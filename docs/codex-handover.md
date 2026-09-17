# 安忆项目 Claude code 交接文档

更新时间：2026-09-13  
用途：交给下一位 Claude code 继续维护和开发。本文中的“当前”以本地工作树为准；生产服务器状态必须按文末命令重新核验。

## 1. 项目目标

安忆是一个 Android 纪念与陪伴应用，包含云端纪念馆、人文社区、义工招募、素材审核和 AI 陪伴。

当前产品重点是 AI 陪伴对象：用户可以为亲人、宠物、地点或其他有意义的对象上传头像，填写关系和记忆，然后进行独立聊天。

用户最终目标是：用户上传一张亲人照片后，能够和一个基于该照片、人物资料和真实记忆的 AI 形象进行视频通话。它应明确标注为 AI 生成形象，不能把 AI 表述成真实亲人本人。

## 2. 当前代码状态

- 工作目录：`/Users/lv/Documents/ChatGPT/anyibeijing`
- 当前分支：`main`
- 最新已提交基线：`dc10657 docs: align launch materials with current Android product`
- 当前工作树有大量未提交变更，包含 Android、后端、测试、文档和目录迁移。不要使用 `git reset --hard` 或丢弃用户改动。
- 当前项目只讨论 Android 和腾讯云生产路线。华为 HarmonyOS 客户端（原 `huawei-harmonyos/` 目录）已于 2026-09-17 经用户确认删除，需要时可从提交 `5be7ea1` 恢复。
- GitHub 操作要求：不要直接合并到 `main`，应创建 Pull Request，PR 标题使用中文。
- 每次新增、修改、修复、删除、迁移、部署都必须同步写入根目录 `CHANGELOG.md`。

## 3. 已有能力

### Android

- 包名：`com.anyi.memorial`
- 当前版本配置：`versionCode 19`、`versionName 1.0.17`
- 默认 API：`https://api.anyibj.cn`
- Jetpack Compose AI 陪伴界面，支持多个陪伴对象独立聊天。
- 陪伴对象头像：直接上传、审核状态、私有资源访问和头像创作。
- 支持按住说话、松开发送、上滑取消、1 至 60 秒录音、语音气泡播放和转文字。
- 语音消息按陪伴对象进入 FIFO 队列；切换对象、退出页面、退到后台时会停止或取消录音。
- 首次使用麦克风有单独授权说明和本地语音同意记录。
- 文字与语音目前都由后端处理，AI 只收到语音转写后的文字，返回仍是文字。
- 首次隐私同意前不连接云端、不初始化微信、不上报崩溃信息。

### 后端

- Node.js 20+、Hono、MySQL，服务入口为 `backend/server/server.ts`，业务主要在 `backend/src/index.ts`。
- 主要 API：
  - `GET /app/config`
  - `GET/POST /ai/companions`
  - `GET/PATCH/DELETE /ai/companions/:id`
  - `POST /ai/companions/:id/avatar`
  - `GET/POST /ai/companions/:id/messages`
  - `POST /ai/companions/:id/voice-messages`
  - `GET/POST/PATCH/DELETE /ai/companions/:id/memories...`
- 默认聊天模型是 `gpt-5.6-luna`；自动记忆模型由 `AI_MEMORY_MODEL` 单独配置，当前默认 `gpt-5.5`。
- 手动记忆会区分用户和陪伴对象主体，手动事实优先于旧 assistant 回复，避免“喜欢吃梨”被历史回答覆盖成“喜欢吃桃”。
- 同一陪伴对象的聊天请求按顺序处理，不同陪伴对象可以并行；这是消息一致性保护，不能误认为已经实现视频通话并发扩容。
- 腾讯云 ASR 已接入 `SentenceRecognition`，生产使用 `ASR_PROVIDER=tencent` 和 `16k_zh`；语音功能由 `AI_VOICE_ENABLED` 控制。
- 语音相关 MySQL 迁移：`backend/migrations-mysql/0010_ai_voice_messages.sql`、`0011_ai_voice_processing_claim.sql`；SQLite 对应 `0028`、`0029`。

## 4. 生产和密钥边界

腾讯云生产 API 的已知信息：

- 域名：`api.anyibj.cn`
- CVM 项目目录：`/opt/anyiapp2/backend`
- systemd：`anyi-memorial-api.service`
- API 本机监听：`127.0.0.1:8787`
- 数据库：本机 MySQL `anyi_memorial`
- 上传目录：`/var/lib/anyi-memorial-api/uploads`
- 管理后台：`https://api.anyibj.cn/admin`

服务器运维细节见 [`docs/tencent-cloud-handover.md`](tencent-cloud-handover.md)。登录私钥、`known_hosts`、数据库密码、Apexin 密钥、腾讯云 ASR 密钥和微信密钥只能从本机安全目录或腾讯云控制台取得，绝不能写入本文件、Git、APK、日志或聊天消息。不要根据聊天记录重新复制任何密钥；如果密钥曾经在聊天中明文发送，应在供应商后台轮换。

生产上一次已知发布包含 `gpt-5.6-luna` 和腾讯云 ASR，但本地现在有未提交变更，所以不能直接说服务器已经等于当前工作树。发布前必须构建、检查 diff、备份数据库、应用迁移、原子替换并做健康检查。

## 5. 目标功能：照片视频通话

这不是在现有聊天页加一个视频播放器，而是一条新的实时链路：

```text
用户麦克风
  -> 实时语音识别（流式 STT）
  -> 现有陪伴对象资料 + 手动记忆 + 聊天上下文
  -> gpt-5.6-luna（流式回复）
  -> 文字转语音（流式 TTS）
  -> 照片驱动的数字人嘴型和动作
  -> WebRTC 视频和声音回传 Android
```

现有语音接口是“录完文件后上传的一句话识别”，不能直接当作实时通话 STT。现有聊天也等待完整 AI 回复后再显示，不能直接当作低延迟视频对话。实时版本必须增加：流式 STT、流式 LLM/TTS、VAD 说话结束检测、用户插话打断、WebRTC 会话、断线重连和通话超时。

### 推荐的首版范围

1. 用户选择一个已有陪伴对象，上传或使用一张已审核照片。
2. 服务端创建一次性视频会话，Android 只拿短期会话令牌，不拿供应商主密钥。
3. 用户首版只开麦克风，不要求打开前置摄像头。
4. AI 形象显示轻微待机动作，说话时做嘴型同步，同时显示字幕。
5. 支持静音、挂断、用户插话立即停止旧回答、网络异常退回普通语音或文字聊天。
6. 页面固定显示“AI 生成形象”或类似提示；用户可以删除照片、声音和会话资料。

先做一个 30 至 60 秒内部样机，测量“用户停止说话到 AI 首次出声”、嘴型同步、断线恢复、单分钟成本和同时在线人数，再决定正式供应商和套餐。

## 6. 服务选型注意事项

- 现成数字人实时 API：最适合先验证效果，通常会同时提供 STT、TTS、数字人渲染和 WebRTC。接入前必须确认是否支持“单张历史照片作为实时形象”，是否允许中国大陆用户和数据地域，是否能提供自定义 LLM 或服务端转发。
- 自建 GPU：可以控制数据和渲染，但需要自己维护实时推流、ASR、LLM、TTS、GPU 并发和监控。MuseTalk 等开源项目主要解决音频驱动嘴型，不能单独构成完整视频通话产品，也不能假设当前普通腾讯云 CVM 能承担实时 GPU 并发。
- 个人或亲人声音需要单独的授权和删除机制。照片本身不能还原声音；首版建议先用平台普通音色。
- 视频通话的容量不只受同一个 AI Key 影响，还受 ASR、TTS、数字人会话数、WebRTC/TURN 和 GPU 或供应商分钟数限制。上线前必须做真实并发压测。

## 7. 下一位 Claude code 的实施顺序

1. 阅读本文、`docs/tencent-cloud-handover.md`、`backend/README.md`、`docs/launch-checklist.md` 和 `CHANGELOG.md`，先确认工作树，不要覆盖用户已有修改。
2. 先选定并验证一家实时数字人供应商，拿到测试凭证、数据地域说明、照片要求、WebRTC/SDK 文档和并发限制；不要先把某个供应商写死在 Android。
3. 在后端增加供应商无关的会话接口，例如创建会话、结束会话、短期令牌和能力配置；供应商密钥只放服务器环境变量。
4. 把当前 AI 陪伴资料和记忆转换为实时会话的系统上下文，保留主体关系约束和手动事实优先规则。
5. 先完成可打断的实时语音会话，再接照片数字人画面；不要先做静态视频轮播冒充实时通话。
6. Android 增加视频会话页面、权限、字幕、静音、挂断、重连和降级入口；所有状态按 `companionId` 隔离。
7. 更新隐私政策、AI 免责声明、商店数据安全清单和 `CHANGELOG.md`，明确照片、声音、语音、转写、聊天记录和第三方处理方。
8. 完成本地测试、模拟器或真机测试、弱网测试、删除测试和并发测试，再由用户明确授权后创建 PR；不要直接推送或合并到 `main`，也不要未经授权部署腾讯云生产。

## 8. 验证命令

后端：

```bash
cd /Users/lv/Documents/ChatGPT/anyibeijing/backend
npm ci
npm run check
npm run server:build
npm test
```

Android：

```bash
cd /Users/lv/Documents/ChatGPT/anyibeijing
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

生产只做只读核验时：

```bash
curl -fsS https://api.anyibj.cn/health
curl -fsS https://api.anyibj.cn/app/config
```

生产发布、迁移、回滚和服务重启按照 `docs/tencent-cloud-handover.md` 执行；发布后必须记录版本、迁移、健康检查和回滚点。

## 9. 已知未完成事项

- 没有实时视频数字人会话接口和 Android WebRTC 页面。
- 没有流式 ASR、流式 TTS、流式 AI 回复和完整打断链路。
- 没有经过选择和合规核验的照片数字人供应商。
- 没有个性化亲人声音方案。
- 没有真实 Android 设备上的视频通话手工验收。
- 当前工作树尚未形成新的提交或 PR；不要把未提交状态误认为已部署生产。
