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

把本地 `D:\Desktop\anyiapp2` 上传到服务器 `/opt/anyiapp2`。

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
AI_MODEL=gpt-5.5
AI_MEMORY_MODEL=gpt-5.5
PAYMENT_ENABLED=false
```

`APEXIN_API_KEY` 只能保存在服务器 `.env`，不得写入 Android、Git、日志或接口响应。聊天与头像生成会直接调用 Apexin；未配置密钥时相关接口返回 `503 ai_provider_not_configured`。systemd 单元不会覆盖 `.env` 中的 Apexin 配置。

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
sudo cp /opt/anyiapp2/AnyiMemorial-test-v1.0.7-code9-image-upload-fix-20260528-210844.apk /var/www/anyi-downloads/anyi-memorial-latest.apk
sudo chown -R www-data:www-data /var/www/anyi-downloads
```

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

## 7. 打包 App

后端健康检查通过后，使用腾讯云域名构建 App：

```powershell
$env:JAVA_HOME='D:\0\android studio\jbr'
.\gradlew.bat :app:assembleDebug -PANYI_API_BASE_URL=https://api.anyibj.cn
```

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
