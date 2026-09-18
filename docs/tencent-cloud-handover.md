# 安忆腾讯云交接文档

本文面向下一位维护者，记录安忆项目当前腾讯云 CVM 的登录、部署、排障和恢复方式。

## 1. 当前环境

| 项目 | 当前值 |
| --- | --- |
| 云厂商 | 腾讯云 CVM |
| API 域名 | `api.anyibj.cn` |
| 系统 | Ubuntu |
| SSH 用户 | `ubuntu` |
| SSH 端口 | `22` |
| API 监听 | `127.0.0.1:8787` |
| 数据库 | 本机 MySQL `127.0.0.1:3306/anyi_memorial` |
| API 服务 | `anyi-memorial-api.service` |
| Nginx | `80/443` |
| 项目目录 | `/opt/anyiapp2` |
| 上传目录 | `/var/lib/anyi-memorial-api/uploads` |
| APK 下载目录 | `/var/www/anyi-downloads` |
| 备份目录 | `/home/ubuntu/anyi-db-backups` |

当前服务器实例主机名为 `VM-0-8-ubuntu`。腾讯云实例元数据可访问，服务器上运行有腾讯云 TAT/YunJing 管理组件。

## 2. SSH 登录

当前部署密钥只保存在维护者本机的安全目录，不提交到 Git，也不要复制到聊天或文档中。下面用两个环境变量代指私钥与配套的 `known_hosts`，实际路径只记录在本机：

```bash
export ANYI_SSH_KEY=/path/to/anyi_deploy_ed25519
export ANYI_KNOWN_HOSTS=/path/to/known_hosts

ssh -i "$ANYI_SSH_KEY" \
  -o IdentitiesOnly=yes \
  -o StrictHostKeyChecking=yes \
  -o UserKnownHostsFile="$ANYI_KNOWN_HOSTS" \
  -o ConnectTimeout=15 \
  ubuntu@api.anyibj.cn
```

登录后常用检查：

```bash
hostname
date -Is
systemctl is-active ssh anyi-memorial-api nginx mysql
curl -fsS http://127.0.0.1:8787/health
curl -fsS https://api.anyibj.cn/health
```

当前 `ubuntu` 授权公钥指纹为：

```text
SHA256:J6vRCONJEboxcI8wO6DyQsnR5sZRrI33OTO6AjWdc+E
```

本地核对公钥指纹：

```bash
ssh-keygen -lf "$ANYI_SSH_KEY.pub"
```

## 3. 上传文件

上传到服务器临时目录，再在服务器上用 `install` 或 `mv` 替换，避免直接覆盖正在使用的文件：

```bash
scp -i "$ANYI_SSH_KEY" \
  -o IdentitiesOnly=yes \
  -o StrictHostKeyChecking=yes \
  -o UserKnownHostsFile="$ANYI_KNOWN_HOSTS" \
  backend/dist-node/src/index.js \
  ubuntu@api.anyibj.cn:/tmp/anyi-index.js
```

以上是单文件热修的做法。常规发布不这么做：按 [交接文档](handover.md) 的“发布流程”整棵 `backend` 树在服务器上重建、比对哈希、留回滚副本后原子替换。

服务器端安装示例：

```bash
sudo cp -p /opt/anyiapp2/backend/dist-node/src/index.js \
  /opt/anyiapp2/backend/dist-node/src/index.js.before-change-$(date +%Y%m%d-%H%M%S)
sudo install -o ubuntu -g ubuntu -m 0644 \
  /tmp/anyi-index.js /opt/anyiapp2/backend/dist-node/src/index.js.tmp
sudo mv -f /opt/anyiapp2/backend/dist-node/src/index.js.tmp \
  /opt/anyiapp2/backend/dist-node/src/index.js
rm -f /tmp/anyi-index.js
sudo systemctl restart anyi-memorial-api
curl -fsS http://127.0.0.1:8787/health
```

完整后端发布前，在本地执行：

```bash
cd backend
npm ci
npm test
```

生产服务器部署目录为 `/opt/anyiapp2/backend`。不要覆盖以下文件或目录：

```text
/opt/anyiapp2/backend/.env
/var/lib/anyi-memorial-api/uploads
/home/ubuntu/anyi-db-backups
```

`.env` 权限应保持为 `600`，其中包含数据库、AI、微信等密钥。发布包不得包含 `.env`、私钥、个人简历、测试账号或本地工具目录。

## 4. 数据库与迁移

生产数据库是 MySQL，不是 SQLite：

```bash
sudo mysql -N -e 'SELECT VERSION();'
sudo mysql -N anyi_memorial -e 'SHOW TABLES;'
```

MySQL 迁移目录：

```text
/opt/anyiapp2/backend/migrations-mysql
```

服务启动时会按迁移记录应用未执行的 MySQL 迁移。执行发布后检查：

```bash
sudo mysql -N anyi_memorial -e \
  'SELECT name, applied_at FROM _node_migrations ORDER BY name;'
```

新版 AI 陪伴使用以下表：

```text
ai_companions
ai_chat_messages
ai_memory_items
ai_memory_settings
```

语音消息使用 `ai_chat_messages` 中的 `message_type`、`duration_ms`、`audio_url` 等字段，对应 MySQL 迁移为 `0010_ai_voice_messages.sql` 和 `0011_ai_voice_processing_claim.sql`（SQLite 为 `0028_ai_voice_messages.sql` 和 `0029_ai_voice_processing_claim.sql`）。第二个迁移增加跨进程幂等处理占位和租约，必须与第一迁移一起应用。生产 ASR 使用腾讯云一句话识别 `SentenceRecognition`；`.env` 配置 `ASR_PROVIDER=tencent`、CAM 子账号密钥、地域和 `16k_zh` 引擎。不要把 ASR 密钥写入 Android、Git、日志或更新日志。

首次部署新版时，MySQL 迁移 `0007_ai_companion_reset.sql` 会一次性清空旧 AI profile、人物、聊天和记忆。部署前必须确认无需保留，或先做独立备份。

备份服务：

```bash
systemctl list-timers anyi-mysql-backup.timer --no-pager
sudo systemctl start anyi-mysql-backup.service
sudo systemctl status anyi-mysql-backup.service --no-pager
```

备份内容包括 MySQL dump 和上传目录，默认保存在 `/home/ubuntu/anyi-db-backups`。不要把备份文件提交到 Git 或公开下载目录。

## 5. 服务运维

查看 API 状态和日志：

```bash
sudo systemctl status anyi-memorial-api --no-pager -l
sudo journalctl -u anyi-memorial-api -n 100 --no-pager
sudo journalctl -u anyi-memorial-api -f
```

重启 API：

```bash
sudo systemctl restart anyi-memorial-api
sudo systemctl is-active anyi-memorial-api
curl -fsS http://127.0.0.1:8787/health
```

Nginx 检查：

```bash
sudo nginx -t
sudo systemctl reload nginx
sudo systemctl status nginx --no-pager
```

当前还存在以下相关单元：

```text
anyi-memorial-api.service   API（enabled）
anyi-mysql-backup.timer     每日备份（enabled）
certbot.timer               证书续期（enabled）
mysql.service / nginx.service
open-llm-vtuber.service     退役项目残留，disabled，单元文件与 anyi_proxy 账号待清理
```

## 6. SSH 安全状态

当前服务器已启用 `/etc/ssh/sshd_config.d/99-anyi-hardening.conf`，有效设置为：

```text
PasswordAuthentication no
KbdInteractiveAuthentication no
ChallengeResponseAuthentication no
PermitRootLogin no
PubkeyAuthentication yes
MaxAuthTries 3
LoginGraceTime 20
```

因此现在只能使用公钥登录 `ubuntu`，不能使用密码或 root 直接登录。修改 SSH 配置后必须先验证：

```bash
sudo sshd -t
sudo sshd -T | grep -E \
  '^(passwordauthentication|permitrootlogin|kbdinteractiveauthentication|pubkeyauthentication|maxauthtries) '
sudo systemctl reload ssh
```

不要在没有备用登录通道的情况下删除现有公钥。当前 SSH 配置原文件备份为：

```text
/etc/ssh/sshd_config.before-anyi-hardening-20260828-233408
```

## 7. 新维护者如何添加自己的 SSH 公钥

因为密码登录已关闭，新维护者需要先通过腾讯云控制台 VNC、在线终端或 TAT 进入服务器，再添加自己的**公钥**。

在本地生成密钥：

```bash
ssh-keygen -t ed25519 -f ~/.ssh/anyi-maintainer-ed25519
```

在服务器控制台中执行，把下面的占位内容替换为新维护者 `.pub` 文件的一整行：

```bash
sudo install -d -o ubuntu -g ubuntu -m 700 /home/ubuntu/.ssh
echo 'ssh-ed25519 AAAA... maintainer' | sudo tee -a /home/ubuntu/.ssh/authorized_keys >/dev/null
sudo chown ubuntu:ubuntu /home/ubuntu/.ssh/authorized_keys
sudo chmod 600 /home/ubuntu/.ssh/authorized_keys
sudo sshd -t
```

然后从新电脑测试：

```bash
ssh -i ~/.ssh/anyi-maintainer-ed25519 -o IdentitiesOnly=yes ubuntu@api.anyibj.cn
```

确认新密钥可用后，再按密钥所有权清理旧密钥；清理前必须确认没有其他维护者仍在使用旧密钥。

## 8. 腾讯云控制台救援登录

SSH 连接完全失败时，不要反复尝试密码。进入腾讯云控制台：

1. 打开云服务器 CVM，选择实例 `VM-0-8-ubuntu`。
2. 点击“登录”，优先使用 VNC 登录、在线终端或 TAT 命令执行。
3. 检查网络和 SSH 服务：

```bash
systemctl status ssh --no-pager -l
ss -lntp | grep ':22'
sudo sshd -t
```

4. 检查腾讯云安全组入站规则，确保 TCP 22 来源包含维护者当前公网 IP；HTTP/HTTPS 需要保留 TCP 80 和 443。
5. 修复后先在控制台执行 `sudo sshd -t`，再执行 `sudo systemctl reload ssh`，最后从维护者电脑测试密钥登录。

如果密钥丢失，通过控制台添加新的公钥，不要重新开启公网密码登录。腾讯云控制台的登录事件、CVM 安全中心和 TAT 审计日志应与服务器 `/var/log/auth.log` 一起核对。

## 9. 常见 SSH 报错

### `Permission denied (publickey)`

通常是用户名、密钥路径或权限不对：

```bash
ssh -vvv -i "$ANYI_SSH_KEY" -o IdentitiesOnly=yes ubuntu@api.anyibj.cn
```

检查：

- 用户必须是 `ubuntu`，不是 `root`。
- `-i` 指向私钥，不是 `.pub` 文件。
- 使用 `-o IdentitiesOnly=yes`，避免 SSH agent 发送其他密钥。
- 服务器上的 `/home/ubuntu/.ssh/authorized_keys` 是否包含对应公钥。
- 公钥文件权限为 `600`，`.ssh` 目录权限为 `700`。

### `Connection timed out`

优先检查：

- 腾讯云安全组是否放行 TCP 22。
- 当前网络、VPN 或公司出口是否阻断 22 端口。
- 域名是否解析到正确的 CVM。

```bash
dig +short api.anyibj.cn
nc -vz api.anyibj.cn 22
```

注意本机 DNS 可能被代理软件劫持成 `198.18.x.x` 一类假地址；拿权威结果用 `curl -s -H 'accept: application/dns-json' 'https://cloudflare-dns.com/dns-query?name=api.anyibj.cn&type=A'`。

### `Connection refused`

通过腾讯云控制台检查：

```bash
sudo systemctl status ssh --no-pager
sudo sshd -t
sudo ss -lntp | grep ':22'
```

### `Host key verification failed`

不要直接执行 `ssh-keygen -R`。先通过腾讯云控制台或可信的维护者确认服务器没有被替换，再核对主机指纹并更新 `known_hosts`。

### `Too many authentication failures`

使用：

```bash
ssh -o IdentitiesOnly=yes -i "$ANYI_SSH_KEY" ubuntu@api.anyibj.cn
```

### `Connection closed`

可能是安全组、腾讯云 YJ 防火墙、SSH 配置或来源 IP 被拦截。通过控制台检查：

```bash
sudo journalctl -u ssh --since '30 minutes ago' --no-pager
sudo tail -n 100 /var/log/auth.log
sudo sshd -T | grep -E \
  '^(passwordauthentication|permitrootlogin|pubkeyauthentication|maxauthtries) '
```

## 10. 安全事件核查

服务器曾收到腾讯云“异常登录”提醒。已核查结果：

- 告警时间段的成功登录均为 `ubuntu` 公钥登录。
- 主要来源是当前管理环境公网 IP，使用已知部署密钥。
- 没有发现未知密钥、成功密码登录或成功 root 登录。
- 同时存在公网 SSH 密码爆破和无效用户名探测，这是互联网暴露 22 端口后的常见扫描。

发现新告警时，先保留证据：

```bash
date -Is
who
w
last -Fai -n 30
sudo lastb -Fai -n 50
sudo journalctl -u ssh --since '24 hours ago' --no-pager
sudo zgrep -hE 'Accepted|Failed password|Invalid user' /var/log/auth.log* 2>/dev/null
```

如果出现不认识的成功登录来源：

1. 先通过腾讯云控制台保留并导出安全中心、登录审计和 TAT 记录。
2. 暂停或限制安全组 TCP 22，只允许可信维护 IP。
3. 添加一把新的维护密钥并测试，再撤销可能泄露的旧密钥。
4. 轮换服务器 `.env` 中的 `AUTH_SECRET`、AI 密钥、微信密钥和数据库凭据。
5. 检查 `sudo`、systemd、cron、`/tmp`、`/var/tmp` 和最近修改的文件。

## 11. 发布验收清单

每次发布后至少执行：

```bash
sudo systemctl is-active anyi-memorial-api nginx mysql
curl -fsS http://127.0.0.1:8787/health
curl -fsS https://api.anyibj.cn/health
sudo journalctl -u anyi-memorial-api -n 80 --no-pager
```

并确认：

- Nginx HTTPS 正常，且 `sudo nginx -T | grep -c internal/live2d` 不为 0（`sites-enabled/anyi-api` 必须是指向 `sites-available` 的软链，2026-09-19 核验时它是独立旧文件，制作端 256 MB 上传段落未生效）。
- MySQL 迁移记录已更新。
- 上传目录和 `.env` 没有被覆盖。
- 人物、聊天和手动记忆账号隔离正常；自动整理新记忆默认关闭。
- `APEXIN_API_KEY` 仅存在服务器 `.env`，聊天使用 `gpt-5.6-luna`，GPT/Gemini 图片模型分别走各自接口。
- 语音功能仍按需保持 `AI_VOICE_ENABLED=false`；若启用，确认 ASR 配置已验证、MySQL `0010` 与 `0011`（或 SQLite `0028` 与 `0029`）已应用，且语音资产可由用户删除。
- 数据库和 uploads 备份任务成功。
- Android 下载地址返回预期 APK，而不是临时 unsigned 构建产物。

GitHub 仓库：

```text
https://github.com/lvhaojie456/anyi-beijing.git
```

交接或发布前必须先审查 `git status` 和待部署 commit，确认破坏性 AI 重置迁移已被明确接受；不要把个人文件、`.env`、密钥或测试账号上传到服务器。
