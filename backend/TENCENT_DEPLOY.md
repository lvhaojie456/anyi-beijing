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
npm install
npm run server:build
```

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
PAYMENT_ENABLED=false
```

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
