# 安忆后端

这是安忆线上 API，统一按腾讯云 CVM 部署设计。运行方式是 Node.js + Hono，数据落在本机 SQLite，上传文件落在本机目录。

## 主要接口

- `GET /health`：健康检查
- `POST /auth/register`：账号注册
- `POST /auth/login`：账号登录并返回 Bearer token
- `POST /auth/wechat`：微信移动应用登录
- `GET /me`、`PATCH /me`、`DELETE /me`：当前用户资料与账号注销
- `GET /memorials`、`POST /memorials`、`PATCH /memorials/:id`：纪念馆资料
- `POST /memorials/:id/flowers`：献花
- `POST /memorials/:id/candle`：点蜡烛
- `POST /memorials/:id/incense`：上香
- `POST /memorials/:id/fruits`：供品
- `POST /assets`：上传图片、语音或文本素材到服务器本地目录
- `GET /assets/*`：读取公开素材
- `GET /community/posts`：读取所有公开人文社区帖子
- `POST /community/posts`：发布人文社区帖子
- `GET /community/volunteer`：读取义工招募信息
- `GET /ai/profile`、`PATCH /ai/profile`、`POST /ai/unlock`：AI 陪伴档案
- `GET /ai/companions`、`POST /ai/companions`、`PATCH /ai/companions/:id`：AI 陪伴对象
- `POST /ai/companions/:id/assets`：AI 素材上传
- `GET /ai/companions/:id/messages`、`POST /ai/companions/:id/messages`：AI 聊天
- `POST /crash-reports`：App 崩溃日志上报
- `GET /admin`：Web 管理后台
- `GET /admin/audit-logs`：审计日志
- `GET /admin/upload-reviews`、`PATCH /admin/upload-reviews/:id`：上传内容审核
- `GET /admin/asset-delete-queue`、`POST /admin/asset-delete-queue/process`：本地文件删除队列
- `GET /admin/crash-reports`：崩溃日志
- `GET /admin/account-deletion-requests`、`PATCH /admin/account-deletion-requests/:id`：账号注销申请

## 本地开发

```bash
cd backend
npm install
cp .env.server.example .env
npm run check
npm run server:build
npm run server:start
```

`.env` 至少需要修改：

```text
AUTH_SECRET=replace-with-a-long-random-secret
ADMIN_USERNAMES=admin
ANYI_DATA_DIR=./data
PUBLIC_ASSET_BASE_URL=http://127.0.0.1:8787
ALLOWED_ORIGINS=http://127.0.0.1:8787,http://localhost:8787,https://api.anyibj.cn
```

## 腾讯云部署

完整步骤见 [TENCENT_DEPLOY.md](TENCENT_DEPLOY.md)。

核心路径：

```text
/opt/anyiapp2/backend
/var/lib/anyi-memorial-api/anyi.sqlite
/var/lib/anyi-memorial-api/uploads
```

常用命令：

```bash
cd /opt/anyiapp2/backend
npm install
npm run server:build
npm run server:start
```

systemd 服务文件在 [examples/anyi-memorial-api.service](examples/anyi-memorial-api.service)。

Nginx 配置在：

- [examples/tencent-nginx-node-api-http.conf](examples/tencent-nginx-node-api-http.conf)
- [examples/tencent-nginx-node-api.conf](examples/tencent-nginx-node-api.conf)

## 数据备份

生产数据都在 `/var/lib/anyi-memorial-api`：

```bash
sudo tar -czf /opt/anyi-backup-$(date +%F).tar.gz /var/lib/anyi-memorial-api
```

建议每天备份，并同步到独立存储位置。

## 人文社区说明

当前代码的主入口为人文社区：

- 登录用户可以查看所有社区帖子，效果接近公开朋友圈/论坛流。
- 登录用户可以发布文字动态；图片字段已预留为 `imageUrls`。
- 义工招募信息由 `/community/volunteer` 提供，App 侧通过小按钮弹出展示。
- 管理后台可查看社区内容，并继续承担上传审核、账号注销、崩溃日志和审计职责。
