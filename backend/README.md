# 安忆后端

这是安忆线上 API，统一按腾讯云 CVM 部署设计。运行方式是 Node.js + Hono，数据落在本机 MySQL，上传文件落在本机目录。

## 主要接口

- `GET /health`：健康检查
- `POST /auth/register`：账号注册 multipart 接口，名称、用户性别（男/女）和头像文件必填
- `POST /auth/login`：账号登录并返回 Bearer token
- `POST /auth/wechat`：微信移动应用登录
- `GET /me`、`PATCH /me`、`DELETE /me`：当前用户资料与账号注销；资料支持昵称、用户性别和头像
- `GET /memorials`、`POST /memorials`、`PATCH /memorials/:id`：纪念馆资料
- `POST /memorials/:id/flowers`：献花
- `POST /memorials/:id/candle`：点蜡烛
- `POST /memorials/:id/incense`：上香
- `POST /memorials/:id/fruits`：供品
- `POST /assets`：上传图片、语音或文本素材到服务器本地目录
- `GET /asset-reviews/:assetId`：素材所有者或管理员读取单条审核状态
- `GET /assets/*`：登录后按所有权或已审核社区公开状态读取素材
- `GET /community/posts`、`POST /community/posts`：人文社区公开信息流，支持文字和图片动态
- `POST /community/posts/:id/like`：社区帖子点赞/取消点赞
- `POST /community/reports`：举报动态或评论
- `GET /community/posts/:id/comments`、`POST /community/posts/:id/comments`：社区帖子评论列表与留言
- `GET /community/volunteer`：社区义工招募信息列表
- `POST /community/volunteer`：管理员发布义工招募信息
- `PATCH /community/volunteer/:id`：管理员开放或截止义工招募
- `POST /community/volunteer/:id/applications`：用户提交义工报名表
- `GET /community/volunteer/applications`：普通用户查看自己的报名，管理员查看报名审核列表
- `DELETE /community/volunteer/applications/:id`：用户取消自己的义工报名
- `PATCH /community/volunteer/applications/:id`：管理员通过或拒绝义工报名
- `GET /admin/community/moderation`、`PATCH /admin/community/posts/:id`、`PATCH /admin/community/comments/:id`：社区审核队列
- `GET /admin/community/reports`、`PATCH /admin/community/reports/:id`：举报处理
- `GET /admin/users/moderation`、`PATCH /admin/users/:id/moderation`：用户屏蔽/封禁
- `GET /ai/companions`、`POST /ai/companions`：陪伴对象列表与创建；名称和“对方是我的”单向关系必填，不要求对象性别，支持人物、宠物、地点或物品
- `GET /ai/companions/:id`、`PATCH /ai/companions/:id`、`DELETE /ai/companions/:id`：读取、修改和删除陪伴对象；“父子、母女、兄弟、姐妹、祖孙、夫妻”等双向写法会被拒绝
- `POST /ai/companions/:id/avatar`：上传并设置本人陪伴对象的头像；图片在审核期间保持私有且本人可见
- `GET /ai/companions/:id/messages`、`POST /ai/companions/:id/messages`、`DELETE /ai/companions/:id/messages/:messageId`：陪伴对象聊天记录
- `POST /ai/companions/:id/voice-messages`：上传 60 秒内语音，服务端转写后进入同一聊天队列并返回文字 AI 回复；语音作为私有资源保存，只有本人可读取
- `GET /ai/companions/:id/memories`、`POST /ai/companions/:id/memories`、`PATCH /ai/companions/:id/memories/:memoryId`、`DELETE /ai/companions/:id/memories/:memoryId`：陪伴对象私有记忆 CRUD
- `DELETE /ai/companions/:id/memories`：清空某个陪伴对象的私有记忆
- `GET /ai/memory-settings`、`PUT /ai/memory-settings`：用户级自动记忆开关，默认关闭
- `GET /ai/image-models`：头像生成模型白名单
- `POST /me/ai-companion-background`、`PATCH /me/ai-companion-background`：直接上传、设置或清除 AI 陪伴对象列表背景（仅限本人图片）
- `POST /ai/companions/:id/background`、`PATCH /ai/companions/:id/background`：直接上传、设置或清除指定陪伴对象的聊天背景（与列表背景独立）
- `POST /ai/companions/:id/avatar/studio`：头像创作 multipart 接口；提交 `model`、原文 `prompt`、可选 `useCurrentAvatar` 与图片字段 `file`。无参考图时生图，有上传图或使用当前头像时改图
- `POST /crash-reports`：App 崩溃日志上报
- `GET /admin`：Web 管理后台
- `GET /admin/audit-logs`：审计日志
- `GET /admin/upload-reviews`、`PATCH /admin/upload-reviews/:id`：上传内容审核
- `GET /admin/asset-delete-queue`、`POST /admin/asset-delete-queue/process`：本地文件删除队列
- `GET /admin/crash-reports`：崩溃日志
- `GET /admin/account-deletion-requests`、`PATCH /admin/account-deletion-requests/:id`：账号注销申请
- `GET /app/config`：App 运行配置与微信、支付配置

## 本地开发

```powershell
cd D:\Desktop\anyiapp2\backend
npm ci
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
APEXIN_BASE_URL=https://api.apexin.ai/v1
APEXIN_API_KEY=replace-with-apexin-api-key
AI_MODEL=gpt-5.6-luna
AI_MEMORY_MODEL=gpt-5.5
AI_VOICE_ENABLED=false
AI_VOICE_RETAIN_AUDIO=true
ASR_PROVIDER=tencent
ASR_TIMEOUT_MS=60000
TENCENT_ASR_SECRET_ID=replace-with-tencent-asr-secret-id
TENCENT_ASR_SECRET_KEY=replace-with-tencent-asr-secret-key
TENCENT_ASR_REGION=ap-beijing
TENCENT_ASR_ENGINE_MODEL_TYPE=16k_zh
TENCENT_ASR_ENDPOINT=https://asr.tencentcloudapi.com
```

`APEXIN_API_KEY` 只允许写入服务器部署环境，禁止提交到仓库、下发给客户端或打印到日志。聊天默认使用 `gpt-5.6-luna`；自动记忆模型可由 `AI_MEMORY_MODEL` 单独配置。GPT 图片走 `/v1/images/generations`，Gemini 图片走 `/v1beta/models/{model}:generateContent`，两者共用上述 Apexin 地址和密钥。

用户手动保存的对象记忆始终会用于该对象的对话；手动新增记忆不会自动打开对话记忆提取。`/ai/memory-settings` 只控制是否从后续对话中自动整理新记忆，默认关闭。

聊天与头像生成会直接尝试调用 Apexin；服务器未配置 `APEXIN_API_KEY` 时，相关请求返回 `503 ai_provider_not_configured`。人物、手动记忆、历史消息和素材管理不依赖供应商密钥。

语音消息默认关闭（`AI_VOICE_ENABLED=false`）。生产使用腾讯云一句话识别 `SentenceRecognition`，服务端通过 TC3-HMAC-SHA256 签名调用；Android 不保存或接触腾讯云密钥。开启前需配置 `TENCENT_ASR_SECRET_ID`、`TENCENT_ASR_SECRET_KEY`，建议普通中文使用 `16k_zh`。未配置时接口返回 `503 asr_provider_not_configured`。语音文件使用私有资产路径保存，不会进入 AI 或应用日志；删除消息、陪伴对象或账号时会进入资产删除队列。部署时必须先应用 MySQL `0010_ai_voice_messages.sql`、`0011_ai_voice_processing_claim.sql`（SQLite 对应 `0028`、`0029`），后者为跨进程重试增加持久化处理状态。

迁移 `0025_ai_companion_reset.sql`（MySQL 为 `0007_ai_companion_reset.sql`）是一次性破坏性迁移：会清空旧 AI profile、陪伴人物、聊天记录、记忆和记忆开关。这是本次彻底重做的预期行为，部署前必须确认无需保留旧 AI 数据或已完成独立备份。

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
npm ci
npm run server:build
npm run server:start
```

systemd 服务文件在 [examples/anyi-memorial-api.service](D:/Desktop/anyiapp2/backend/examples/anyi-memorial-api.service)。

该单元启用 `TRUST_PROXY=true`；必须配套部署仓库内的 Nginx 配置，使 Nginx 重建 `X-Real-IP` 和 `X-Forwarded-For` 并清除外部 `CF-Connecting-IP`，否则客户端可伪造来源 IP。

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

恢复前先停止 API，并在隔离的恢复库验证 dump；不要直接覆盖生产库：

```bash
sudo systemctl stop anyi-memorial-api
gunzip -c /home/ubuntu/anyi-db-backups/anyi-mysql-anyi_memorial-YYYYMMDD-HHMMSS.sql.gz \
  | mysql --defaults-extra-file=/etc/anyi/mysql-backup.cnf anyi_memorial_restore
sudo tar -xzf /home/ubuntu/anyi-db-backups/anyi-uploads-YYYYMMDD-HHMMSS.tar.gz -C /var/lib/anyi-memorial-api-restore
sudo systemctl start anyi-memorial-api
```

恢复演练至少应检查 `SELECT COUNT(*) FROM users`、`GET /health`，并确认 uploads 目录权限为 API 运行用户可读写。备份脚本使用临时文件后原子改名，失败时不会留下可误用的半成品；MySQL dump 与 uploads 压缩包需要成对保留。
