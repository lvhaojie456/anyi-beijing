# 安忆 Android

安忆是一个 Android Compose 应用，已实现云端纪念馆、AI 陪伴、人文社区、素材上传审核、合规页面和 Web 管理后台；远程礼祭、祈愿护符商城和支付能力已预留数据模型与上线规划，尚未完成完整接口闭环。

面试讲法见 [小红书产品工程师面试项目讲法](docs/xhs-product-engineer-interview.md)。

当前项目已统一切到腾讯云部署路线：

- Android 包名：`com.anyi.memorial`
- App 默认连接：`https://api.anyibj.cn`
- 后端：腾讯云 CVM 上运行 Node.js + Hono 服务
- 数据库：服务器本机 MySQL，数据库名默认 `anyi_memorial`；SQLite 文件仅保留作迁移回滚备份
- 上传文件：服务器本机目录，默认路径 `/var/lib/anyi-memorial-api/uploads`
- Web 管理后台：`https://api.anyibj.cn/admin`
- 2D 数字人：默认由 `GET /app/config` 下发，当前指向 `https://api.anyibj.cn/vtuber/`；服务状态可查 `GET /app/digital-human/status`
- AI 长期记忆：默认关闭；用户开启后按角色保存聊天历史和明确表达的偏好/重要信息，可在 App 内查看或清空
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

## 后端

```powershell
cd D:\Desktop\anyiapp2\backend
npm install
npm run check
npm run server:build
```

腾讯云部署步骤见 [backend/TENCENT_DEPLOY.md](D:/Desktop/anyiapp2/backend/TENCENT_DEPLOY.md)。

## 上线材料

- [上线清单](D:/Desktop/anyiapp2/docs/launch-checklist.md)
- [安全与运营](D:/Desktop/anyiapp2/docs/security-operations.md)
- [2D 数字人运维](D:/Desktop/anyiapp2/docs/digital-human-operations.md)
- [Web 管理后台说明](D:/Desktop/anyiapp2/docs/admin-web-backend.md)
- [隐私政策模板](D:/Desktop/anyiapp2/docs/privacy-policy-template.md)
- [用户协议模板](D:/Desktop/anyiapp2/docs/user-agreement-template.md)
- [AI 陪伴免责声明模板](D:/Desktop/anyiapp2/docs/ai-disclaimer-template.md)

上线前仍需准备域名备案、HTTPS 证书、支付商户号、客服入口、隐私政策 URL、用户协议 URL、商店截图和测试账号。
