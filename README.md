# 安忆

安忆包含 Android、华为 HarmonyOS、云端 API 和官网四个独立部分。当前 Android 版本已实现云端纪念馆、人文社区、义工招募、素材上传审核、合规页面，以及按陪伴对象组织的 AI 消息、记忆和头像管理。陪伴对象可以是人物、宠物、地点、物品或其他有意义的存在。远程礼祭、祈愿护符商城和支付能力已预留数据模型与上线规划，尚未完成完整接口闭环。

## 工程目录

```text
app/                       Android Compose 客户端
huawei-harmonyos/          华为 HarmonyOS 原生客户端，可独立用 DevEco Studio 打开
backend/                   Node.js + Hono 云端 API
website/                   官网静态文件
docs/                      上线、合规和运维文档
```

华为版本的打开、签名和发布说明见 [huawei-harmonyos/README.md](huawei-harmonyos/README.md)。

每次功能、代码、配置、数据库、文档或资源变更都记录在 [更新日志](CHANGELOG.md)；提交 Pull Request 前必须同步填写，删除内容也要明确写在“移除”小节。

Android 已接入提示词/图片生成 Live2D 初版的异步工作流，制作端源码位于 `tools/live2d-worker/`，启用和部署见 [Live2D 生成接入](docs/live2d-generation.md)。生成服务默认关闭，需要部署后台和运行制作端。

当前项目已统一切到腾讯云部署路线：

- Android 包名：`com.anyi.memorial`
- App 默认连接：`https://api.anyibj.cn`
- 后端：腾讯云 CVM 上运行 Node.js + Hono 服务
- 数据库：服务器本机 MySQL，数据库名默认 `anyi_memorial`；SQLite 文件仅保留作迁移回滚备份
- 上传文件：服务器本机目录，默认路径 `/var/lib/anyi-memorial-api/uploads`
- Web 管理后台：`https://api.anyibj.cn/admin`
- AI 陪伴：使用对象消息列表、独立聊天、手动记忆和头像管理；对象通过“对方是我的”单向关系定义，聊天会结合用户资料中的性别判断双方身份
- AI 语音消息：支持按住说话、语音气泡播放和“转文字”；转写内容进入现有文字 AI 对话，AI 只返回文字。语音功能需在服务器单独配置并默认关闭
- AI 服务：聊天默认使用 `gpt-5.6-luna`；GPT 与 Gemini 图片模型分别适配 Apexin 的两个接口，密钥只保存在服务器环境变量中
- AI 历史数据：本次彻底重做包含一次性重置迁移，会清空旧人物、消息和记忆；执行部署前应单独备份需要留存的旧 AI 数据
- 官网：`website/` 静态文件，可部署到腾讯云 CVM Nginx 或腾讯云静态网站托管

## Android 调试

```powershell
$env:JAVA_HOME='D:\0\android studio\jbr'
.\gradlew.bat assembleDebug
```

Debug APK 输出：

```text
D:\Desktop\anyiapp2\app\build\outputs\apk\debug\app-debug.apk
```

## Android 发布配置

发布前准备 release keystore，并通过环境变量传入签名信息：

```powershell
$env:JAVA_HOME='D:\0\android studio\jbr'
$env:ANYI_RELEASE_STORE_FILE='D:\secure\anyi-release.jks'
$env:ANYI_RELEASE_STORE_PASSWORD='replace-with-password'
$env:ANYI_RELEASE_KEY_ALIAS='anyi'
$env:ANYI_RELEASE_KEY_PASSWORD='replace-with-password'
.\gradlew.bat :app:bundleRelease -PANYI_API_BASE_URL=https://api.anyibj.cn
```

Release AAB 通常输出到：

```text
D:\Desktop\anyiapp2\app\build\outputs\bundle\release\app-release.aab
```

## 华为 HarmonyOS

华为版本已经整理为独立 Stage 模型工程：

```text
huawei-harmonyos/
```

在 DevEco Studio 中直接打开该目录，不需要从 Android 工程导入。它使用 ArkTS、ArkUI 和 HarmonyOS 系统 Kit，共用同一套腾讯云 API 和用户数据。

## 后端

```powershell
cd D:\Desktop\anyiapp2\backend
npm ci
npm run check
npm run server:build
```

腾讯云部署步骤见 [backend/TENCENT_DEPLOY.md](backend/TENCENT_DEPLOY.md)。

## 上线材料

- [更新日志](CHANGELOG.md)
- [Codex 项目交接文档](docs/codex-handover.md)
- [上线清单](docs/launch-checklist.md)
- [安全与运营](docs/security-operations.md)
- [Web 管理后台说明](docs/admin-web-backend.md)
- [AI 陪伴界面参考](docs/ai-companion-ui-references.md)
- [隐私政策模板](docs/privacy-policy-template.md)
- [用户协议模板](docs/user-agreement-template.md)
- [AI 陪伴免责声明模板](docs/ai-disclaimer-template.md)

上线前仍需准备域名备案、HTTPS 证书、支付商户号、客服入口、隐私政策 URL、用户协议 URL、商店截图和测试账号。
