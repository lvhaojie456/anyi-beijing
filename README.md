# 安忆北京

安忆北京是一套面向纪念、陪伴与社区互助场景的 Android 应用与配套后端服务。项目包含 Android 客户端、Node.js API、本地 SQLite 数据存储、Web 管理后台、官网静态页，以及上线所需的运营与合规模板。

## 功能概览

- 人文社区：公开论坛式信息流，用户发布的内容所有人可见，体验接近朋友圈动态。
- 义工招募：社区入口提供义工招募信息弹窗，便于用户了解参与方式。
- 云端纪念馆：支持纪念馆资料、献花、点蜡烛、上香和供品等互动。
- AI 陪伴：支持陪伴对象档案、素材上传与聊天接口。
- 账号体系：支持账号注册登录、微信登录预留、资料编辑与账号注销流程。
- Web 管理后台：提供内容审核、审计日志、崩溃日志、账号注销申请等运营工具。
- 官网页面：`website/` 下提供静态官网素材与示例 Nginx 配置。

## 项目结构

```text
.
├── app/                 # Android Compose 客户端
├── backend/             # Node.js + Hono API 与管理后台
│   ├── examples/        # systemd / Nginx 部署示例
│   ├── migrations/      # SQLite 数据库迁移脚本
│   ├── server/          # Node 运行时适配层
│   └── src/             # API 主逻辑
├── docs/                # 上线、合规、安全运营文档
├── gradle/              # Gradle Wrapper 依赖
├── store-assets/        # 应用商店与发布素材
├── tools/               # 项目辅助脚本
└── website/             # 官网静态页面
```

## 技术栈

- Android：Kotlin、Jetpack Compose、Gradle
- 后端：Node.js、TypeScript、Hono
- 数据库：SQLite
- 部署：腾讯云 CVM、Nginx、systemd
- 管理后台：后端内置 Web 页面

## 本地开发

### Android 客户端

```bash
./gradlew :app:assembleDebug
```

可通过 Gradle 参数覆盖 API 地址：

```bash
./gradlew :app:assembleDebug -PANYI_API_BASE_URL=https://api.example.com
```

微信登录 AppID 可通过参数传入：

```bash
./gradlew :app:assembleDebug -PANYI_WECHAT_APP_ID=wx_your_app_id
```

### 后端服务

```bash
cd backend
npm install
cp .env.server.example .env
npm run check
npm run server:build
npm run server:start
```

后端默认会读取 `.env`。生产环境必须替换 `AUTH_SECRET`、微信配置、AI 配置等敏感值，真实密钥不要提交到仓库。

## 常用接口

- `GET /health`：健康检查
- `POST /auth/register`：注册
- `POST /auth/login`：登录
- `POST /auth/wechat`：微信登录
- `GET /me`、`PATCH /me`、`DELETE /me`：当前用户资料与注销
- `GET /community/posts`、`POST /community/posts`：人文社区帖子
- `GET /community/volunteer`：义工招募信息
- `GET /memorials`、`POST /memorials`、`PATCH /memorials/:id`：纪念馆
- `POST /assets`、`GET /assets/*`：素材上传与读取
- `GET /admin`：Web 管理后台

更多接口说明见 [backend/README.md](backend/README.md)。

## 部署说明

腾讯云部署参考 [backend/TENCENT_DEPLOY.md](backend/TENCENT_DEPLOY.md)。核心运行方式是：

- Nginx 负责 HTTPS 与反向代理
- Node.js 后端以 systemd 服务运行
- SQLite 与上传文件保存在服务器数据目录
- Android 客户端通过 `ANYI_API_BASE_URL` 指向线上 API

上线前需要完成：

- 域名备案与 HTTPS 证书
- 微信开放平台 / 小程序相关配置
- 隐私政策、用户协议和 AI 免责声明 URL
- 管理员账号与运营审核流程
- 数据备份和日志留存策略

## 文档

- [上线清单](docs/launch-checklist.md)
- [安全与运营](docs/security-operations.md)
- [Web 管理后台说明](docs/admin-web-backend.md)
- [隐私政策模板](docs/privacy-policy-template.md)
- [用户协议模板](docs/user-agreement-template.md)
- [AI 陪伴免责声明模板](docs/ai-disclaimer-template.md)

## 仓库安全

本仓库只应保存源码、配置模板、迁移脚本、文档和项目资源。以下内容不要提交：

- `.env`、真实密钥、Token、证书、签名文件
- `node_modules/`、Gradle/Android 构建产物
- 数据库、备份、日志、浏览器缓存和临时输出
- APK/AAB 成品包和个人文件
