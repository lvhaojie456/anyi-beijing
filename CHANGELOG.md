# 安忆更新日志

本文件是安忆项目唯一的变更总账。凡是进入项目、测试环境或生产环境的变更，都必须在同一个 Pull Request 中记录：新增功能、行为修改、缺陷修复、配置和数据库迁移、文档和资源变更，以及删除或迁移的内容。

## 记录规则

- 每次变更先在 `当前发布` 或 `未发布` 下记录，再提交代码；不要只写“优化若干问题”。
- 明确写出影响范围和删除内容。删除功能、接口、表、迁移、页面、资源或配置时，必须放在 `移除` 小节；重命名或迁移同时写明新位置。
- 数据库迁移、环境变量、生产部署和回滚注意事项单独写在 `运维/部署` 小节，禁止记录密钥值。
- 破坏性变更必须写清数据影响、备份要求和回滚方式。
- 每次发布完成后，将 `未发布` 内容移到日期或版本标题下，并补充验证结果、APK/AAB 版本或后端部署范围。
- 没有某类变更时写“无”，避免读者误以为遗漏。

## 变更分类

- `新增`：新功能、接口、页面、配置或测试。
- `修改`：已有行为、界面、性能、依赖或配置的变化。
- `修复`：错误、回归、安全或数据一致性问题。
- `移除`：删除或停止支持的内容。
- `运维/部署`：数据库迁移、服务发布、下载包、回滚和验证。

## 未发布

### 新增

- 新增 `docs/codex-handover.md`，记录当前 Android、后端、腾讯云生产边界，以及照片数字人视频通话的实施路线和接手步骤。
- Android 新增 Live2D 动态形象与沉浸式聊天模式。APK 内置 6 个 Live2D Cubism 4 官方样例模型（Kei、Izumi、Haru、Hiyori、Tororo 白猫、Hijiki 黑猫，约 17 MB，已剔除示例语音与编辑器源文件），用户在聊天页"更多"菜单里为陪伴对象选择一个形象并绑定，进入"沉浸模式"后形象显示在聊天上方，AI 回复到达时播放说话动画，下方沿用同一条文字聊天流。形象与陪伴对象一一绑定，切换对象自动切换形象。
- 新增 `Live2dAvatarView.kt`：用 Android `WebView` + `WebViewAssetLoader` 从 APK 资产渲染 Live2D（pixi.js + pixi-live2d-display + Cubism Core，全部本地打包，不访问网络；禁用文件与内容访问，拒绝跳离资产域）。新增 `Live2dChatScreen.kt` 沉浸式聊天页，复用 `AiCompanionScreen` 现有的消息列表、草稿与按对象 FIFO 发送队列，不新增任何会话状态。
- 后端新增 `PATCH /ai/companions/:id/live2d`，请求体 `live2dModel` 为白名单内的模型 id 或 `null`（解绑）；非法 id 返回 `400 live2d_model_not_supported`。`GET /ai/companions` 与单个对象响应新增 `live2dModel` 字段。服务端只保存 id，不托管任何模型文件。
- 新增 `androidx.webkit:webkit:1.14.0` 依赖（`WebViewAssetLoader`），这是本次唯一新增的第三方依赖。
- 新增后端测试 `companion Live2D avatar binding accepts only bundled model ids`（绑定、非法 id 拒绝、解绑三种情况）和 Android 单元测试 `updateAiCompanionLive2dModel_*`（PATCH 方法、JSON 字段、`null` 解绑）。
- 沉浸模式页面固定显示"AI 生成形象"标识，形象选择弹窗注明"所有形象均为 AI 生成，不代表任何真实人物"。

### 修改

- 根目录 README 增加 Codex 项目交接文档入口。
- `AiCompanion` 数据类新增 `live2dModel: String?`；`AiCompanionRow` 与 `serializeAiCompanion` 同步新增 `live2d_model` / `live2dModel`。
- 聊天页"更多"菜单新增"选择动态形象"（未绑定时）/ "沉浸模式" + "更换动态形象"（已绑定时）两项。

### 修复

- 修复 Live2D 绑定的两个 Android 单元测试在本机 JVM 上失败（`ProtocolException: Invalid HTTP method: PATCH`）。原因是桌面 JDK 的 `HttpURLConnection` 用一个私有静态白名单校验请求方法，该白名单不含 `PATCH`；Android 的 `HttpURLConnection` 由 OkHttp 实现，可正常发送 `PATCH`。即客户端代码在真机上正确，只有本机测试 JVM 无法表达该方法。已新增测试专用辅助 `app/src/test/java/com/anyi/memorial/network/HttpMethods.kt`，通过反射把白名单中未使用的 `TRACE` 槽位替换为 `PATCH`（不改变数组长度和其他槽位），并在 `app/build.gradle.kts` 的 `testOptions` 为单元测试加上 `--add-opens java.base/java.net=ALL-UNNAMED`。生产代码与 `AnyiApiClient` 未做任何修改。此前 8 个 `PATCH` 调用点从未被单元测试用真实 socket 覆盖，所以该限制一直没有暴露。

### 移除

- 清理 `app/src/main/java/com/anyi/memorial/MainActivity.kt` 中 5 个未使用的 import：`rememberLazyListState`、`Icons.AutoMirrored.Rounded.Send`、`Checkbox`、`Switch`、`graphicsLayer`。
- 移除已退役数字人前端残留在 `isCacheableCloudResource` 中的缓存放行项：`/vtuber/`、`/live2d-models/` 两个路径前缀，以及 `.moc3`、`.wasm`、`.atlas`、`.skel` 四个扩展名。App 中没有任何代码构造这些 URL，后端也已将 `/vtuber` 与 `/vtuber/` 返回 404；该函数只是磁盘缓存白名单，删除后仅不再缓存这些类型，不影响任何正常加载。
- 删除根目录两个与官网资源完全重复的图片：`source_app_icon.png`（1254×1254，1.4 MB）与 `AnyiMemorial-download-qr.png`。二者分别与 `website/assets/app-icon.png`、`website/assets/download-qr.png` 的 SHA-256 完全一致，且没有任何构建脚本、Gradle 配置或代码引用根目录副本。官网仍使用 `website/assets/` 下的同名文件，视觉无变化。
- 移除鸿蒙端整个 AI 陪伴页：`huawei-harmonyos/entry/src/main/ets/pages/tabs/CompanionPage.ets`（485 行）。该页面调用的 5 个接口 `/app/digital-human/chat|messages|memories|memory-settings` 后端已不存在，生产实测全部返回 404；页面还从 `/app/config` 读取后端已不再返回的 `digitalHuman` 字段，导致功能开关恒为关闭。鸿蒙端标签页由 4 个减为 3 个（纪念馆、人文社区、个人中心），`Index.ets` 的 `activeTab` 索引同步由 0/1/2/3 改为 0/1/2。
- 移除鸿蒙端随上述页面一并失效的内容：`ApiClient.ets` 的 5 个 digital-human 方法和 `DigitalHumanPayload` 接口、`Constants.ets` 的 `DEFAULT_DIGITAL_HUMAN_URL`、`Models.ets` 的 7 个死类型（`DigitalHumanMessage`、`DigitalHumanMessagesResponse`、`DigitalHumanChatResponse`、`DigitalHumanConfig`、`AiMemory`、`AiMemoriesResponse`、`AiMemorySettingsResponse`），以及三张不再被引用的图片 `anyi_digital_grandma.png`（1.2 MB）、`anyi_digital_grandpa.png`（1.1 MB）、`anyi_ai_page_bg.png`（319 KB）。
- 修正 `AppConfigResponse` 类型：原先只声明 `digitalHuman` 字段，与后端实际返回的 `wechat`/`payments`/`ai` 结构不符，已按生产响应重建为 `WechatConfig`/`PaymentsConfig`/`AiConfig`。
- 更新 `huawei-harmonyos/README.md`：删除 "ArkWeb：现有 2D 数字人页面" 与功能表里的 AI 陪伴行，并说明该页被移除的原因。

### 运维/部署

- 新增数据库迁移：MySQL `0012_ai_companion_live2d.sql`、SQLite `0030_ai_companion_live2d.sql`，为 `ai_companions` 增加可空列 `live2d_model`。为纯加列、可空、无默认值，不影响现有数据；服务启动时自动应用。回滚只需 `ALTER TABLE ai_companions DROP COLUMN live2d_model`，客户端对缺失字段按未绑定处理。
- 本次不涉及环境变量或密钥变更。生产尚未部署，需先在本地完成 Android 构建与真机验收再发布。
- 授权说明：6 个模型均受 Live2D《免费素材许可协议》v1.6 约束，"一般用户/小规模企业可用于任何营利或非营利目的"，Cubism Core 文件头标注为 Redistributable Code，当前个人使用合规。若未来年销售额达到 1000 万日元或以商业形式发行，需另行取得 Live2D 出版许可并复核形象授权，详见项目记忆 `anyi-live2d-licensing`。
- 补充说明：本次仅删除鸿蒙端代码与资源。生产服务器上的 `open-llm-vtuber.service` 仍为 inactive/disabled 状态，nginx 对 `/vtuber`、`/live2d-models/`、`/tts-ws` 等的 404 拦截保持不变，本次未改动服务器。

## 当前发布（2026-09-08）

### 新增

- 后端增加腾讯云一句话识别 `SentenceRecognition` 适配，使用 TC3-HMAC-SHA256 服务端签名，支持 Android 发送的 `m4a` 与测试用 `wav` 音频。
- 增加腾讯云 ASR 请求签名、音频参数、转写响应和配置脱敏测试。

### 修改

- 生产 ASR 供应商配置改为 `ASR_PROVIDER=tencent`，普通中文使用 `16k_zh` 引擎；语音仍只向 AI 提交转写文字。

### 修复

- 无。

### 移除

- 无。

### 运维/部署

- 腾讯云 ASR 凭证仅写入生产服务器权限为 `600` 的 `.env`，未进入 Git、APK、日志或本更新日志。
- 真实腾讯云语音测试成功，端到端转写链路验证通过；生产 `AI_VOICE_ENABLED=true`，公开 `/app/config` 返回 `asrConfigured=true`。

## 当前发布（2026-09-01）

### 新增

- AI 陪伴支持按住说话、松开发送、上滑取消、语音气泡播放和“转文字”；AI 只接收转写文字并以文字回复。
- 语音请求增加数据库级处理占位和租约，跨进程或重启重试时避免重复调用 ASR/AI。
- 手动记忆增加主体解析上下文，明确区分用户与当前陪伴对象；新增“手动事实优先于旧 AI 回复”的回归测试。
- 增加根目录更新日志和 Pull Request 变更记录检查清单。

### 修改

- 主 AI 对话模型切换为 `gpt-5.6-luna`；自动记忆模型仍由 `AI_MEMORY_MODEL` 独立配置。
- AI 事实型问答温度调整为 `0.28`，手动记忆提示放在对话历史之后，并标记为权威用户事实。
- Android 发布包版本为 `1.0.17`，使用现有 release v2 签名；生产下载地址指向该包。

### 修复

- 修复“我喜欢吃苹果，儿子喜欢吃梨”被 AI 错误理解为“儿子喜欢吃桃”的主体混淆问题。
- 修复旧 assistant 回复覆盖手动记忆的问题；只有用户明确纠正时才允许覆盖已确认事实。
- 修复语音录音临时文件在账号切换、页面退出和自动停止场景下的生命周期问题。

### 移除

- 无新的用户功能移除。

### 运维/部署

- 腾讯云生产 `anyi-memorial-api` 已切换 `AI_MODEL=gpt-5.6-luna`，服务重启后健康检查正常。
- 生产密钥未修改；`AI_MEMORY_MODEL=gpt-5.5` 和语音默认关闭状态保持不变。
- 已应用语音相关 MySQL 迁移 `0010_ai_voice_messages.sql`、`0011_ai_voice_processing_claim.sql`。
- Release APK 已上传至 `https://api.anyibj.cn/downloads/anyi-memorial-latest.apk`；本地与服务器 SHA-256 已核对一致。
- 本地后端全量测试 `45/45` 通过；Android 单元测试、Lint 和 Release 构建通过。

## 2026-08-31

### 新增

- 重做 AI 陪伴对象管理，支持多对象独立聊天、单向关系、手动记忆、头像上传和头像创作。
- 增加陪伴对象列表背景、单对象聊天背景，以及对象头像审核和私有资源访问控制。
- 增加社区素材审核、义工招募媒体幂等、删除队列和事务回滚保护。

### 修改

- Android 个人设置和 AI 陪伴界面重新整理，聊天对象状态按对象隔离，连续消息按 FIFO 处理。
- 隐私政策、用户协议、AI 说明和上线清单同步加入 AI 陪伴数据处理说明。

### 修复

- 修复不同 AI 会话互相阻塞、连续发送消息丢失，以及头像上传后审核状态不同步的问题。

### 移除

- 移除旧的根目录 `harmonyos/` 工程路径，华为版本迁移到独立目录 `huawei-harmonyos/`。
- 移除已退役的数字人前端说明、入口和相关旧资源；当前项目只保留 AI 陪伴文字链路。

### 运维/部署

- 腾讯云 MySQL 生产路线、备份、Nginx、systemd 和发布交接文档完成对齐。

## 2026-08-28

### 新增

- 增加 AI 陪伴持久化记忆、记忆设置、查看、单条删除和账号隔离。
- 增加腾讯云对象存储兼容的图片资源处理和部署交接文档。

### 修改

- AI 陪伴提示词增加关系方向、用户性别和记忆上下文约束。

### 移除

- 无。

## 2026-07-02 至 2026-07-04

### 新增

- 完成云端纪念馆、人文社区、义工招募、素材上传审核、微信登录和基础合规页面。
- 增加数字人集成的历史实验入口和运维检查。

### 移除

- 历史数字人入口已在后续版本退役，详见 2026-08-31 的移除记录。

## 2026-06-18

### 新增

- 创建安忆 Android、后端和官网基础工程。

### 修改

- 无。

### 修复

- 无。

### 移除

- 无。
