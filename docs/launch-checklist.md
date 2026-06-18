# 安忆上线清单

## 已在项目中完成

- Android 正式包名：`com.anyi.memorial`
- Android 联网权限：已加入 `INTERNET`
- Android 后端地址：通过 `-PANYI_API_BASE_URL=...` 注入 `BuildConfig.API_BASE_URL`
- 默认 API 地址：`https://api.anyibj.cn`
- Release 签名入口：通过 `ANYI_RELEASE_*` 环境变量配置，不提交 keystore
- 后端骨架：账号、管理员、纪念馆、人文社区、义工招募、AI 聊天、上传审核、文件上传
- 数据库迁移：`backend/migrations/*.sql`
- 腾讯云部署文档：`backend/TENCENT_DEPLOY.md`
- 隐私政策、用户协议、AI 免责声明模板

## 还需要准备

- 开发者主体资料：个人或公司主体
- 腾讯云 CVM、域名、备案资料和 HTTPS 证书
- 国内上线所需的 APP 备案资料
- 隐私政策 URL、用户协议 URL、AI 免责声明 URL
- 客服邮箱/电话、投诉入口、社区运营规则
- App 图标、启动图、应用商店截图、应用简介
- 普通用户测试账号、管理员测试账号

## 国内应用市场

如果在中国大陆提供互联网信息服务，通常需要先完成 APP 备案。上线前请确认域名备案、服务器所在地、主体信息和应用市场材料一致。

参考：

- 工信部 APP 备案通知：<https://www.miit.gov.cn/zwgk/zcwj/wjfb/tz/art/2023/art_920db564162e4312916a01bed6540ad8.html>

## Google Play

当前项目 `targetSdk = 36`，方向上符合较新的目标 API 策略，但提交前仍要以 Play Console 当时检查结果为准。

参考：

- 目标 API 要求：<https://developer.android.google.cn/google/play/requirements/target-sdk?hl=zh-cn>
- Play App Signing：<https://support.google.com/googleplay/android-developer/answer/9842756?hl=zh-cn>
- Data safety：<https://support.google.com/googleplay/android-developer/answer/10787469?hl=zh-cn>

## 人文社区流

社区动态：

```text
登录用户 -> 查看所有公开帖子 -> 发布动态 -> 全员可见
```

规则：

- App 主入口展示人文社区帖子流，普通用户可看到所有用户发布的内容。
- 发布内容进入 `community_posts`，图片字段已预留为 `imageUrls`。
- 义工招募信息通过社区页小按钮打开。
- 管理后台用于查看社区内容、上传审核、账号注销和审计日志。

## 发布前测试

- 注册、登录、退出、注销
- 管理员账号是否正确进入 Web 后台
- 普通用户发布社区动态
- 不同账号能看到同一条社区动态
- 社区页义工招募按钮能打开并展示联系方式
- 管理员能在 Web 后台查看社区内容和审核上传素材
- 纪念馆献花、点蜡烛、上香、供品限制
- 图片/语音上传失败、权限拒绝、网络断开
- 文件删除队列处理
- 社区内容过长、空内容、敏感词和接口限流
- 深色模式、不同屏幕尺寸、首次安装和升级安装
- Release AAB 安装、崩溃日志、隐私弹窗、测试账号

## 上线前不可省略

- 后端必须开启 HTTPS 和自有域名。
- 隐私政策必须覆盖头像、纪念照片、语音、聊天、社区帖子和上传素材信息。
- AI 陪伴如果未接真实 AI，商店描述必须明确“演示功能”或“暂未接入真实 AI”。
- SQLite 数据库和上传目录必须配置定时备份。
- 管理后台建议独立成后台域名，App 内仅保留入口。
