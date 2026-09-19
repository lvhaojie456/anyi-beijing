# 腾讯云 CVM 部署后端

本项目后端按腾讯云 CVM 上的 Ubuntu 服务器部署：

- API 逻辑复用 `src/index.ts`
- 运行入口是 `server/server.ts`
- 数据库是本机 MySQL：`127.0.0.1:3306/anyi_memorial`
- SQLite 文件 `/var/lib/anyi-memorial-api/anyi.sqlite` 仅在迁移观察期保留作回滚备份
- 上传文件保存在本机目录：`/var/lib/anyi-memorial-api/uploads`
- Nginx 反向代理到本机 `127.0.0.1:8787`
- APK 下载文件由 Nginx 静态托管：`/var/www/anyi-downloads`

## 1. 上传代码

在服务器准备目录：

```bash
sudo mkdir -p /opt/anyiapp2
sudo chown -R ubuntu:ubuntu /opt/anyiapp2
sudo install -d -o ubuntu -g ubuntu -m 700 /var/lib/anyi-memorial-api
sudo install -d -o ubuntu -g ubuntu -m 700 /var/lib/anyi-memorial-api/uploads
```

首次部署时把仓库的 `backend/` 上传到服务器 `/opt/anyiapp2/backend`（用 `git archive <sha> backend | gzip` 打包再 scp，不要把 `.env`、`node_modules`、`dist-node` 或个人文件一起传）。日常发布不再整目录覆盖，按 [交接文档](../docs/handover.md) 的“发布流程”做暂存、哈希比对、回滚副本与原子替换。

## 2. 安装 Node 和依赖

建议 Node.js 20+：

```bash
node -v
npm -v
```

如果服务器没有 Node：

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs build-essential
```

安装 MySQL：

```bash
sudo apt update
sudo apt install -y mysql-server
sudo systemctl enable --now mysql
```

安装依赖并编译：

```bash
cd /opt/anyiapp2/backend
npm ci
npm run server:build
```

`.nvmrc` 固定生产 Node 版本（当前为 20.19.0）；部署机应使用同一主版本和 npm 10.8.2，避免原生依赖漂移。`npm ci` 会严格按 `package-lock.json` 安装，不要在生产机运行 `npm install` 修改锁文件。

## 3. 配置环境变量

```bash
cd /opt/anyiapp2/backend
cp .env.server.example .env
nano .env
```

至少修改：

```text
HOST=127.0.0.1
AUTH_SECRET=replace-with-a-long-random-secret
DB_DRIVER=mysql
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_USER=anyi_api
MYSQL_PASSWORD=replace-with-mysql-password
MYSQL_DATABASE=anyi_memorial
ANYI_MYSQL_MIGRATIONS_DIR=/opt/anyiapp2/backend/migrations-mysql
ANYI_DATA_DIR=/var/lib/anyi-memorial-api
PUBLIC_ASSET_BASE_URL=https://api.anyibj.cn
ALLOWED_ORIGINS=https://api.anyibj.cn
APEXIN_BASE_URL=https://api.apexin.ai/v1
APEXIN_API_KEY=replace-with-apexin-api-key
AI_MODEL=gpt-5.6-luna
AI_MEMORY_MODEL=gpt-5.5
AI_VOICE_ENABLED=false
AI_VOICE_RETAIN_AUDIO=true
# 腾讯云一句话识别；密钥应来自仅有 ASR 权限的 CAM 子账号
ASR_PROVIDER=tencent
ASR_TIMEOUT_MS=60000
TENCENT_ASR_SECRET_ID=replace-with-tencent-asr-secret-id
TENCENT_ASR_SECRET_KEY=replace-with-tencent-asr-secret-key
TENCENT_ASR_REGION=ap-beijing
TENCENT_ASR_ENGINE_MODEL_TYPE=16k_zh
TENCENT_ASR_ENDPOINT=https://asr.tencentcloudapi.com
PAYMENT_ENABLED=false
```

`APEXIN_API_KEY`、`TENCENT_ASR_SECRET_ID` 和 `TENCENT_ASR_SECRET_KEY` 只能保存在服务器 `.env`，不得写入 Android、Git、日志或接口响应。聊天与头像生成会直接调用 Apexin；语音默认关闭，未配置腾讯云 ASR 时返回 `503 asr_provider_not_configured`。systemd 单元不会覆盖 `.env` 中的供应商配置。

生成随机 `AUTH_SECRET`：

```bash
openssl rand -hex 32
```

## 4. 直接启动测试

```bash
cd /opt/anyiapp2/backend
npm run server:start
```

另开终端测试：

```bash
curl http://127.0.0.1:8787/health
```

返回 `{"ok":true,...}` 说明服务正常。

## 5. 配置 systemd 常驻

```bash
sudo cp /opt/anyiapp2/backend/examples/anyi-memorial-api.service /etc/systemd/system/anyi-memorial-api.service
sudo systemctl daemon-reload
sudo systemctl enable --now anyi-memorial-api
sudo systemctl status anyi-memorial-api --no-pager
```

服务单元启用 `TRUST_PROXY=true`，因此 Nginx 配置必须同时使用仓库中的版本，由 Nginx 覆盖客户端传入的 IP 头。更新代码时同步复制 systemd/Nginx 示例并执行 `daemon-reload`、`nginx -t` 和 reload，不能只重启 Node。

查看日志：

```bash
journalctl -u anyi-memorial-api -f
```

## 6. 配置 Nginx

先准备 APK 下载目录：

```bash
sudo mkdir -p /var/www/anyi-downloads
sudo chown www-data:www-data /var/www/anyi-downloads
```

安装包按版本号命名放进去（`sudo install -o www-data -g www-data -m 644`），`anyi-memorial-latest.apk` 与 `anyi-memorial-release-latest.apk` 是指向当前版本的软链，用 `sudo ln -sfn` 切换。

HTTP 配置只负责跳转到 HTTPS；证书准备好后再对外提供服务：

```bash
sudo cp /opt/anyiapp2/backend/examples/tencent-nginx-node-api-http.conf /etc/nginx/sites-available/anyi-api
sudo ln -sf /etc/nginx/sites-available/anyi-api /etc/nginx/sites-enabled/anyi-api
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
curl -I http://api.anyibj.cn/health
```

证书申请成功后换成 HTTPS 配置：

```bash
sudo cp /opt/anyiapp2/backend/examples/tencent-nginx-node-api.conf /etc/nginx/sites-available/anyi-api
sudo ln -sf /etc/nginx/sites-available/anyi-api /etc/nginx/sites-enabled/anyi-api
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
curl https://api.anyibj.cn/health
```

`sites-enabled/anyi-api` 必须是指向 `sites-available/anyi-api` 的软链。如果它是一个独立文件，更新 `sites-available` 不会生效；用 `sudo nginx -T | grep internal/live2d` 确认制作端的 256 MB 上传段落真的在生效配置里。

## 7. 打包 App

后端健康检查通过后，使用腾讯云域名构建 App：

```bash
./gradlew :app:assembleDebug -PANYI_API_BASE_URL=https://api.anyibj.cn
```

正式包的签名与上传步骤见 [交接文档](../docs/handover.md)。

## 8. 备份

生产主库是 MySQL，上传目录在 `/var/lib/anyi-memorial-api/uploads`。部署备份脚本和定时器：

```bash
sudo cp /opt/anyiapp2/backend/scripts/anyi-mysql-backup.sh /usr/local/bin/anyi-mysql-backup
sudo chmod 750 /usr/local/bin/anyi-mysql-backup
sudo cp /opt/anyiapp2/backend/examples/anyi-mysql-backup.service /etc/systemd/system/anyi-mysql-backup.service
sudo cp /opt/anyiapp2/backend/examples/anyi-mysql-backup.timer /etc/systemd/system/anyi-mysql-backup.timer
sudo systemctl daemon-reload
sudo systemctl enable --now anyi-mysql-backup.timer
sudo systemctl start anyi-mysql-backup.service
```

备份目录默认是 `/home/ubuntu/anyi-db-backups`，保留 14 天。建议再把备份同步到腾讯云 COS 或另一台服务器。

### 恢复演练

先恢复到隔离数据库，不要直接导入生产库：

```bash
sudo systemctl stop anyi-memorial-api
mysql -e 'CREATE DATABASE IF NOT EXISTS anyi_memorial_restore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci'
gunzip -c /home/ubuntu/anyi-db-backups/anyi-mysql-anyi_memorial-YYYYMMDD-HHMMSS.sql.gz \
  | mysql anyi_memorial_restore
sudo mkdir -p /var/lib/anyi-memorial-api-restore
sudo tar -xzf /home/ubuntu/anyi-db-backups/anyi-uploads-YYYYMMDD-HHMMSS.tar.gz \
  -C /var/lib/anyi-memorial-api-restore
mysql -D anyi_memorial_restore -e 'SELECT COUNT(*) AS users FROM users'
sudo systemctl start anyi-memorial-api
curl http://127.0.0.1:8787/health
```

确认数据库行数、`/health` 和 uploads 文件可读后，再按变更窗口制定生产切换方案。备份文件应在本机之外再保留一份。
