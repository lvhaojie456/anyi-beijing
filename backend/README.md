# 安忆后端

这是安忆线上 API，统一按腾讯云 CVM 部署设计。运行方式是 Node.js + Hono，数据落在本机 MySQL，上传文件落在本机目录。

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
- `GET /assets/*`：登录后按所有权或已审核社区公开状态读取素材
- `GET /community/posts`、`POST /community/posts`：人文社区公开信息流，支持文字和图片动态
- `POST /community/posts/:id/like`：社区帖子点赞/取消点赞
- `POST /community/reports`：举报动态或评论
- `GET /community/posts/:id/comments`、`POST /community/posts/:id/comments`：社区帖子评论列表与留言
- `GET /community/volunteer`：社区义工招募信息列表
- `POST /community/volunteer`：管理员发布义工招募信息
- `POST /community/volunteer/:id/applications`：用户提交义工报名表
- `GET /community/volunteer/applications`：管理员查看义工报名审核列表
- `PATCH /community/volunteer/applications/:id`：管理员通过或拒绝义工报名
- `GET /admin/community/moderation`、`PATCH /admin/community/posts/:id`、`PATCH /admin/community/comments/:id`：社区审核队列
- `GET /admin/community/reports`、`PATCH /admin/community/reports/:id`：举报处理
- `GET /admin/users/moderation`、`PATCH /admin/users/:id/moderation`：用户屏蔽/封禁
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
- `GET /app/config`：App 运行配置，下发 2D 数字人入口地址和开关
- `GET /app/digital-human/status`：检查 2D 数字人页面是否可用
- `POST /app/digital-human/chat`：爷爷/奶奶 2D 数字人聊天，默认模型 `gpt-5.4-mini`

## 本地开发

```powershell
cd D:\Desktop\anyiapp2\backend
npm install
Copy-Item .env.server.example .env
npm run server:build
npm run server:start
```

`.env` 至少需要修改：

```text
HOST=127.0.0.1
AUTH_SECRET=replace-with-a-long-random-secret
DB_DRIVER=mysql
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_USER=anyi_api
MYSQL_PASSWORD=replace-with-mysql-password
MYSQL_DATABASE=anyi_memorial
ANYI_DATA_DIR=D:\Desktop\anyiapp2\backend\data
PUBLIC_ASSET_BASE_URL=https://api.anyibj.cn
ALLOWED_ORIGINS=https://api.anyibj.cn
PAYMENT_ENABLED=false
VTUBER_URL=https://api.anyibj.cn/vtuber/
VTUBER_ENABLED=true
DIGITAL_HUMAN_CHAT_MODEL=gpt-5.4-mini
```

## 腾讯云部署

完整步骤见 [TENCENT_DEPLOY.md](D:/Desktop/anyiapp2/backend/TENCENT_DEPLOY.md)。

核心路径：

```text
/opt/anyiapp2/backend
/var/lib/anyi-memorial-api/anyi.sqlite  # 迁移回滚保留文件，不再作为生产主库
/var/lib/anyi-memorial-api/uploads
MySQL: 127.0.0.1:3306/anyi_memorial
```

常用命令：

```bash
cd /opt/anyiapp2/backend
npm install
npm run server:build
npm run server:start
npm run digital-human:smoke -- https://api.anyibj.cn
```

systemd 服务文件在 [examples/anyi-memorial-api.service](D:/Desktop/anyiapp2/backend/examples/anyi-memorial-api.service)。

Nginx 配置在：

- [examples/tencent-nginx-node-api-http.conf](D:/Desktop/anyiapp2/backend/examples/tencent-nginx-node-api-http.conf)
- [examples/tencent-nginx-node-api.conf](D:/Desktop/anyiapp2/backend/examples/tencent-nginx-node-api.conf)

## 数据备份

生产主库在本机 MySQL，上传文件在 `/var/lib/anyi-memorial-api/uploads`。服务器使用 `anyi-mysql-backup.timer` 每天自动备份 MySQL dump 和 uploads，手动执行：

```bash
sudo systemctl start anyi-mysql-backup.service
sudo systemctl status anyi-mysql-backup.service --no-pager
```

备份目录默认是 `/home/ubuntu/anyi-db-backups`，保留 14 天。建议再同步到腾讯云 COS 或另一台服务器。
