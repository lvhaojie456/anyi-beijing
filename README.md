# 安忆

安忆是一款 Android 云端纪念与数字陪伴应用，由三个部分组成：Android 客户端、腾讯云上的 Node.js API，以及一个在 Mac 上常驻运行的 Live2D 制作端。当前线上版本 **1.0.21**。

已上线的能力：

- 云端纪念馆：纪念照片、献花、点烛、上香、供果。
- 人文社区与义工互助：动态、评论、举报、义工招募与报名审核。
- AI 陪伴：按“对方是我的 ___”单向关系定义陪伴对象，独立聊天、手动记忆、头像上传与创作；按住说话由腾讯云一句话识别转写后进入同一对话。
- 动态形象：内置 6 个 Live2D 形象；也可以用提示词或照片生成专属形象，由 Mac 制作端加远程 4090 异步完成，带规则恢复与大模型监督。
- 语音回复：绑定了动态形象的对象，AI 回复会用腾讯云语音合成出声，形象口型跟随，回复保存为可回放的语音条。
- Web 管理后台，以及隐私政策、用户协议、AI 免责声明、账号注销页面。

远程礼祭、祈愿护符商城和支付只预留了数据模型，`PAYMENT_ENABLED=false`，尚未闭环。

## 工程目录

```text
app/                   Android Compose 客户端（包名 com.anyi.memorial）
backend/               Node.js 20 + Hono API；生产 MySQL、测试 SQLite；迁移在 migrations/ 与 migrations-mysql/
tools/live2d-worker/   Mac 制作端：图生 Live2D 流水线、监督器、安忆队列适配器
website/               官网静态文件，发布到 Cloudflare Pages
docs/                  交接、运维、合规文档；docs/releases/ 每次线上发布一份记录
store-assets/          微信开放平台移动应用登记用的 108×108 高清图标与 28×28 水印图
dist/                  （不入库）本机保留的已发布 APK/AAB，按版本号命名
```

每次功能、代码、配置、数据库、文档或资源变更都记录在 [更新日志](CHANGELOG.md)，与代码在同一个 Pull Request 里提交；删除的内容写在“移除”小节。不直接推送 `main`，PR 标题用中文。

## 线上环境

- App 默认连接 `https://api.anyibj.cn`；管理后台 `https://api.anyibj.cn/admin`；安装包 `https://api.anyibj.cn/downloads/anyi-memorial-latest.apk`。
- 后端跑在一台腾讯云 CVM 上：systemd `anyi-memorial-api`、Nginx、本机 MySQL。所有密钥只在服务器 `.env`。
- 官网 `https://anyi-memorial-site.pages.dev`。域名 `anyibj.cn` 在腾讯云注册、由 Cloudflare 做权威解析，只解析不代理。
- 制作端在维护者的 Mac 上用 launchd 常驻，GPU 推理走 SSH 到远程 4090。

维护者从 [交接文档](docs/handover.md) 开始；服务器登录、部署与回滚见 [腾讯云运维](docs/tencent-cloud-handover.md)。

## 本地开发（macOS）

Android SDK 与 JDK 不一定在 PATH 上，先导出（以本机为例）：

```bash
export JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.7+6/Contents/Home
export ANDROID_HOME=~/dev-tools/android-sdk
```

Android 单元测试与调试包：

```bash
./gradlew :app:testDebugUnitTest
```

```bash
./gradlew :app:assembleDebug -PANYI_API_BASE_URL=https://api.anyibj.cn
```

后端（Node 20，见 `backend/.nvmrc`）：

```bash
cd backend && npm ci && npm test
```

本地测试全部跑在 SQLite 上。MySQL 专有 SQL 必须走 `dialect === "mysql"` 分支，否则只有上线才会暴露。本机 Node 不是 20 时，`better-sqlite3` 需要先 `npm rebuild better-sqlite3`。

制作端环境见 [tools/live2d-worker/README.md](tools/live2d-worker/README.md)。

## 发布

签名 keystore 与密码不进仓库，通过环境变量传入：

```bash
export ANYI_RELEASE_STORE_FILE=/path/to/anyi-release-v2.jks
export ANYI_RELEASE_KEY_ALIAS=anyi-release-v2
export ANYI_RELEASE_STORE_PASSWORD=...   # 从钥匙串读取，不要留在 shell 历史里
export ANYI_RELEASE_KEY_PASSWORD=...
./gradlew :app:assembleRelease :app:bundleRelease
```

产物在 `app/build/outputs/apk/release/` 与 `app/build/outputs/bundle/release/`。版本号提升、后端发布、制作端切换、安装包上传与回滚的完整步骤在 [交接文档](docs/handover.md) 的“发布流程”一节；每次发布在 `docs/releases/` 留一份记录。

## 文档索引

- [更新日志](CHANGELOG.md)
- [交接文档](docs/handover.md)：当前状态、规则、代码地图、发布流程、已知问题、下一步
- [腾讯云运维](docs/tencent-cloud-handover.md)：登录、部署、备份、SSH 安全、救援
- [后端说明](backend/README.md)、[腾讯云部署](backend/TENCENT_DEPLOY.md)
- [Live2D 生成接入](docs/live2d-generation.md)、[Live2D 监督方案](docs/live2d-supervisor-plan.md)
- [发布记录索引](docs/releases/README.md)
- [上线清单](docs/launch-checklist.md)、[安全与运营](docs/security-operations.md)、[Web 管理后台](docs/admin-web-backend.md)
- 合规模板：[隐私政策](docs/privacy-policy-template.md)、[用户协议](docs/user-agreement-template.md)、[AI 免责声明](docs/ai-disclaimer-template.md)
- [AI 陪伴界面参考](docs/ai-companion-ui-references.md)
