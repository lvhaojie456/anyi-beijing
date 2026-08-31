# 安忆上线清单

## 已在项目中完成

- Android 正式包名：`com.anyi.memorial`
- Android 联网权限：已加入 `INTERNET`
- Android 后端地址：通过 `-PANYI_API_BASE_URL=...` 注入 `BuildConfig.API_BASE_URL`
- 默认 API 地址：`https://api.anyibj.cn`
- 首次启动隐私同意：同意前不连接云端、不初始化微信登录、不启用崩溃上报
- Release 签名入口：通过 `ANYI_RELEASE_*` 环境变量配置，不提交 keystore
- 后端骨架：账号、管理员、纪念馆、订单、聊天、验收图片、护符商城、文件上传；支付接口默认关闭
- 数据库迁移：SQLite 使用 `backend/migrations/*.sql`，MySQL 使用 `backend/migrations-mysql/*.sql`
- 腾讯云部署文档：`backend/TENCENT_DEPLOY.md`
- 隐私政策、用户协议、AI 免责声明模板

## 还需要准备

- 开发者主体资料：个人或公司主体
- 腾讯云 CVM、域名、备案资料和 HTTPS 证书
- 国内上线所需的 APP 备案资料
- 微信支付、支付宝或其他支付商户号
- 隐私政策 URL、用户协议 URL、AI 免责声明 URL
- 客服邮箱/电话、投诉入口、退款规则
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

## 订单状态流

远程礼祭：

```text
pending_payment -> pending_order -> accepted -> in_progress -> pending_acceptance -> completed
```

规则：

- `pending_payment -> pending_order`：由支付回调推进；管理员只用于线下对账补录。
- `pending_order -> accepted -> in_progress`：管理员推进。
- `in_progress -> pending_acceptance`：管理员上传验收图片后推进。
- `pending_acceptance -> completed`：用户最终确认。
- 用户和管理员可在订单内互发消息。

## 发布前测试

- 注册、登录、退出、注销
- 管理员账号是否正确进入 Web 后台
- 普通用户创建远程礼祭订单，并确认未配置真实支付时不会误报支付成功
- 管理员查看所有订单、接单、推进、上传验收图片
- 用户在验收图片后确认完成
- 订单内用户和管理员互发消息
- 纪念馆献花、点蜡烛、上香、供品限制
- 图片/语音上传失败、权限拒绝、网络断开
- 文件删除队列处理
- 接入真实支付后的重复支付、回调延迟、金额不一致
- 深色模式、不同屏幕尺寸、首次安装和升级安装
- Release AAB 安装、崩溃日志、隐私弹窗、测试账号
- 验证人物记忆跨次启动恢复、手动新增/查看/单条删除和敏感信息过滤；如开启自动整理，再单独验证开关与“忘记”指令
- 验证 `gpt-image-2` 走 Image API，六个 Gemini 图片模型走 `generateContent`，并确认客户端拿不到 Apexin 密钥

## 上线前不可省略

- 后端必须开启 HTTPS 和自有域名。
- 支付签名校验必须接真实微信支付或支付宝官方流程。
- 隐私政策必须覆盖头像、纪念照片、语音、聊天、订单、支付信息。
- 隐私政策和商店隐私清单必须明确 Apexin 处理的人物设定、相关记忆、对话和头像提示词。
- MySQL 数据库和上传目录必须配置定时备份。
- AI 人物记忆必须支持用户查看和单条删除，并在删除人物或注销账号时一并删除。
- 管理后台建议独立成后台域名，App 内仅保留入口。
