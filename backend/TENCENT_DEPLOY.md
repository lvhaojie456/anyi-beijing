# 腾讯云 CVM 部署后端

本项目后端按腾讯云 CVM 上的 Ubuntu 服务器部署：

- API 逻辑复用 `src/index.ts`
- 运行入口是 `server/server.ts`
- 数据库是本机 SQLite：`/var/lib/anyi-memorial-api/anyi.sqlite`
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
AUTH_SECRET=replace-with-a-long-random-secret
ADMIN_USERNAMES=admin
ANYI_DATA_DIR=/var/lib/anyi-memorial-api
PUBLIC_ASSET_BASE_URL=https://api.anyibj.cn
ALLOWED_ORIGINS=https://api.anyibj.cn,http://api.anyibj.cn
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

如果证书还没准备好，先用 HTTP 配置测试：

```bash
sudo cp /opt/anyiapp2/backend/examples/tencent-nginx-node-api-http.conf /etc/nginx/sites-available/anyi-api
sudo ln -sf /etc/nginx/sites-available/anyi-api /etc/nginx/sites-enabled/anyi-api
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
curl http://api.anyibj.cn/health
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

SQLite 数据库和上传目录都在 `/var/lib/anyi-memorial-api`，建议每天备份：

```bash
sudo tar -czf /opt/anyi-backup-$(date +%F).tar.gz /var/lib/anyi-memorial-api
```

建议再把备份同步到腾讯云 COS 或另一台服务器。
