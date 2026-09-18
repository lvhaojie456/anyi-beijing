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

- 无。

### 修改

- 无。

### 修复

- 无。

### 移除

- 无。

### 运维/部署

- 无。

## 当前发布（2026-09-18 16:00）

### 新增

- 后端新增语音合成接口 `POST /ai/companions/:id/speech`：腾讯云 `TextToVoice`（TC3 v3 签名，服务名 `tts`，`Codec=mp3`、`SampleRate=16000`，文本上限 150 字），按 `用户 + 音色 + 采样率 + 文本` 的哈希缓存为私有资产，命中直接返回 `X-Anyi-Speech-Cache: hit` 且不计费。新增 `PATCH /ai/companions/:id/voice` 与 `voiceId` 字段（MySQL `0015` / SQLite `0033`，含 `ai_speech_usage` 每日用量表），每日合成字符上限 `TTS_DAILY_CHAR_LIMIT`（默认 20000），缓存保留 `TTS_CACHE_DAYS`（默认 30 天）并在下一次合成时回收未被消息引用的音频。腾讯云签名函数改为接受 `service/host/action`，ASR 行为与签名结果不变。
- 后端新增 `PATCH /ai/companions/:id/messages/:messageId/audio`：首句播完后把合成音频挂到该条回复（`message_type=voice`、`audio_url`、`duration_ms`），只能挂本人 `ai/speech/` 下的资产，语音条因此可以回放。
- Android 新增 `AiSpeechQueue.kt`：把回复按标点分句（首句 ≤ 40 字优先出声、其余 ≤ 120 字、超长硬切、尾句过短并入前句），逐句请求合成、边播边预取下一句，用 `MediaPlayer` 顺序播放；播放期间以播放包络驱动 `setLipSync(0..1)`，形象口型随语音开合、停顿闭嘴。新增音色选择入口（"更多"菜单里仅在绑定形象后出现的"说话声音"），白名单三项：沉稳男声、知性女声、温柔女声，另有"跟随默认"。未绑定形象或未选音色时完全不合成，与旧行为一致；合成失败只提示一次，文字回复不受影响。

### 修改

- Android 版本提升为 `1.0.21`（versionCode 23），沿用 release-v2 签名；在 `0013d31` 之上构建，`fc04951` 仅改服务端文件、不影响安装包。
- `AiCompanion` 新增 `voiceId`；`Live2dAvatarView` 新增 `mouthOpenness` 参数，置空即回落到原来按文本长度估算的说话动画。
- `backend/.env.server.example`、`docs/live2d-generation.md`、`backend/README.md` 补充 `TTS_*` / `TENCENT_TTS_*` 配置与计费、缓存、配额说明。

### 修复

- 后端 `ai_speech_usage` 每日用量的写入改为按数据库方言生成 SQL：MySQL 用 `ON DUPLICATE KEY UPDATE`，SQLite 用 `ON CONFLICT ... DO UPDATE`。PR #13 只写了 SQLite 语法，线上 MySQL 第一次合成就会因 SQL 错误回滚并删除刚生成的音频；本地测试跑在 SQLite 上没有暴露。部署前复查发现，未上线。
- 后端 `server/server.ts` 把 `TTS_ENABLED`、`TENCENT_TTS_*`、`TTS_*` 加入传给应用的环境白名单。此前该文件按键名逐个转发 `process.env`，PR #13 新增的键没有列入，线上写了 `TTS_ENABLED=true` 后 `/app/config` 的 `speech.enabled` 仍为 false，语音合成接口返回 503；本地测试直接构造 env 对象所以没暴露。部署时发现。

### 移除

- 无。

### 运维/部署

- 部署（2026-09-18 15:39–15:56 CST，用户授权）：腾讯云已开通语音合成并领取"超自然大模型音色免费资源包"（2 万字符、3 个月，覆盖三个白名单音色）。线上后端由 `d1611fa` 依次切换到 `0013d31`（迁移 `0015` 自动应用，`_node_migrations` 14 → 15 行）与 `fc04951`；备份 `anyi-mysql-anyi_memorial-20260918-153926.sql.gz` / `anyi-uploads-20260918-153926.tar.gz`；服务器构建哈希 `b123ed7edd3f` / `21ee21ce3c25` 与本机一致；`.env` 追加 `TTS_ENABLED=true`、`TENCENT_TTS_VOICE_DEFAULT=uncle`，密钥复用 `TENCENT_ASR_*`；回滚副本 `before-main-20260918-{0013d31,fc04951}` 与对应 `dist-node.pre-*`。Mac 制作端代码无变化，未切换。Android 1.0.21 已构建发布：APK SHA-256 `059ad5423ce63a5a702147ac1f40e3e35fde6afda29094cf5f411d893f593e61`，证书与线上一致，两个 latest 链接已切换并公网回读一致。
- 线上端到端验收（临时账号，验收后 `DELETE /me` 清理）：`/app/config` 返回 `speech.enabled=true` 与三个音色；建对象默认 `voiceId=null`；`PATCH /voice` 设为 `gentle` 成功、非法 id 400；`POST /speech` 首次 `X-Anyi-Speech-Cache: miss` 返回 `audio/mpeg`（8,742 字节，16 kHz 单声道 mp3），同句再合成 `hit` 且字节完全一致；发文字消息得到 AI 回复后 `PATCH /messages/:id/audio` 使其变为 `voice`，`audioUrl` 带鉴权可回放、未登录 401。三个音色在部署前各真实合成一次（约 12–13 KB、3.1 秒）均成功。详见 `docs/releases/2026-09-18-voice-replies-1.0.21.md`。
- 新增环境变量：`TTS_ENABLED`（默认 false）、`TENCENT_TTS_SECRET_ID/KEY`（默认复用 `TENCENT_ASR_*`）、`TENCENT_TTS_REGION`、`TENCENT_TTS_ENDPOINT`、`TENCENT_TTS_VOICE_DEFAULT`（默认 `uncle`）、`TENCENT_TTS_SAMPLE_RATE`、`TTS_TIMEOUT_MS`、`TTS_DAILY_CHAR_LIMIT`、`TTS_CACHE_DAYS`。不开启则不产生任何合成调用与费用。
- 迁移 MySQL `0015_ai_companion_voice.sql` / SQLite `0033_ai_companion_voice.sql`：`ai_companions` 加可空列 `voice_id`，新建 `ai_speech_usage`。回滚 `DROP COLUMN voice_id` 与 `DROP TABLE ai_speech_usage` 即可。
- 隐私：合成音频含 AI 回复原文，作为私有资产只对该用户可读，账号注销与对象删除随现有资产删除队列清理，服务端缓存 30 天自动回收；`docs/security-operations.md` 已同步。
- 计费与授权：腾讯云按合成字符计费，具体单价以官网定价页为准，本记录不写数字；音色均为腾讯云合成音色，不涉及真人声音克隆，个人自用无额外授权要求，商业发行前需复核。
- 验证：后端 `npm test` 53 项通过（新增 3 项 TTS 用例）、Android `:app:testDebugUnitTest` 23 项通过（新增 4 项分句用例）、制作端 34 项通过。本次未部署；线上需开通语音合成并设置 `TTS_ENABLED=true` 才生效。

## 当前发布（2026-09-18 12:00）

### 新增

- 无。

### 修改

- Android 版本提升为 `1.0.20`（versionCode 22），沿用 release-v2 签名；除版本号外与 `main` `d1611fa` 无差异。

### 修复

- 制作端修复合手/单手形象无法绑定的问题。See-through 对双手交握（或另一只手被遮挡）的图只给一个合并的 `handwear` 图层，而绑定要求 `arm-l`、`arm-r`、`hand-l`、`hand-r` 四个图层，此前 `make_recipe` 只在存在 `handwear-l` / `handwear-r` 时才生成手臂与手的分割，精修包因此缺少这四个图层，`build_body_motion` 以 `Incomplete or duplicate authoring layers` 失败。现在单个 `handwear` 会先按画布中线分成左右，再各自按原有手腕比例切出手和手臂，与腿部 `legwear` / `footwear` 的处理方式一致；拆层本就给出左右分层时行为不变。触发场景：提示词"一个真实的慈祥的老奶奶"生成双手交握的写实立绘，任务在绑定阶段失败。
- 制作端监督器单次模型调用超时由 60 秒提到 90 秒（`SUPERVISOR_TIMEOUT_SECONDS` 可调），超时或返回非法 JSON 时再试一次，缩略图由 512 px 降到 384 px。此前一次真实运行中拆层关卡在 60.2 秒 `APITimeoutError`，该关卡等于没有执行。

### 移除

- 无。

### 运维/部署

- 部署（2026-09-18 11:58–12:05 CST，用户授权）：线上后端由 `16cfa9c` 切换到 `main` 的 `d1611fa`（无新迁移，`_node_migrations` 保持 14 行）；部署前备份 `anyi-mysql-anyi_memorial-20260918-115836.sql.gz` / `anyi-uploads-20260918-115836.tar.gz`；服务器构建哈希 `c09e0744c7ed` 与本机一致；回滚副本 `/opt/anyi-releases/before-main-20260918-d1611fa`、`dist-node.pre-main-20260918-d1611fa`。Mac 制作端切换到 `releases/d1611fa`（plist 备份 `.bak-16cfa9c`）。Android 1.0.20 已构建并发布：APK SHA-256 `5761c02788160951abe2ad6fd3fde2b30fef553b432322c294f8d655956e47cc`，证书与线上一致，`anyi-memorial-latest.apk` / `anyi-memorial-release-latest.apk` 已切换，公网回读哈希一致；AAB 保留本地未上传。详见 `docs/releases/2026-09-18-handwear-fix-1.0.20.md`。
- 验证：制作端 `unittest discover` 34 项通过（新增 2 项：合并手层拆分、审查超时重试）。用失败任务的产物回放（复用规划、拆层 PSD 与表情）修复后一次通过，`arm-l`/`arm-r`/`hand-l`/`hand-r` 正常生成，脚底位移 0.0003 px；随后用同一提示词"一个真实的慈祥的老奶奶"做全新生成（影子模式，真实调用 Astra）也完整通过：生成立绘、规划、4090 拆层（无背景泄漏、未触发裁剪）、表情、精修 31 层、绑定与校验全部通过，脚底位移 0.0002 px，视觉评分 0.83，规划 / 拆层 / 表情三次审查分别 7.6 / 14.0 / 14.3 秒。

## 当前发布（2026-09-18）

### 新增

- 新增 `docs/live2d-supervisor-plan.md`：Live2D 生成监督方案（规则恢复 + 大模型诊断与处置）。内容为三层架构、关卡与失败包、动作菜单与预算、前景遮罩与动作幅度回退等规则层能力、后端诊断字段与迁移 0014/0032 草案、App 文案与确认流程、隐私边界、测试验收与 PR 拆分。仅为方案文档，未改动任何代码。
- 制作端新增规则恢复层 `tools/live2d-worker/scripts/foreground.py`：提示词生成默认请求透明背景（`IMAGE_BACKGROUND=transparent`，Apexin `gpt-image-2.5-sunburst` 实测 33 秒返回真 RGBA，供应商不支持时自动回落普通生成）；进拆层前 `neutralize_background` 把透明或近白（角落近白且与边框连通）背景填成中性灰 (210,210,210)，因为 See-through 会把纯白背景当作人物并入衣服图层，灰底则分得干净（同一张图灰底重拆实测 `topwear` 从占画布 79% 回到 10%）；拆层后 `clip_background` 按外接框占比 > 60%、逐层深度图饱和像素 > 30% 或遮罩外像素 > 30% 判定泄漏，只对泄漏图层按 `depth < 250 ∧ 遮罩` 重建并写出 `decomposition/input_clipped.psd`，其余图层逐字节不变；报告与遮罩写入 `foreground/`。
- 制作端动作幅度回退：`motion_recipe(package, scale)` 支持整体缩小倾斜、呼吸、胸腔扩张、手臂与衣摆幅度；校验只因脚底位移 ≥ 0.25 px 或翻转三角形失败（顶点有限、无退化三角形）时按 1.0 → 0.66 → 0.33 重新绑定复检，最多两次，失败目录保留为 `body-motion.failed-scaleNNN`，`validation.json` 记录 `motionScale`。
- 制作端新增监督层 `tools/live2d-worker/scripts/supervisor.py`：`LIVE2D_SUPERVISOR_MODE` = `off` / `shadow`（默认）/ `act`；用非流式 JSON 请求（≤ 512 px 缩略图、温度 0、60 秒超时、失败不重试不阻断）在规划后审查脸眼嘴矩形、拆层后审查图层表、新生成表情后审查闭眼张嘴、嘴部三档阈值都失败时定位嘴部矩形、校验通过后给初版打分（写入 `validation.visualReview`），最终失败时输出白名单诊断码、菜单动作与一句面向用户的中文。`act` 模式另执行免费动作：覆盖修正后的矩形、用更严格提示重做表情一次、把下次重试改为重新规划。每单预算跨尝试保存在任务目录 `supervisor-state.json`：模型调用 8、重规划 2、重绑定 2、重做表情 1、重生成图片 1；付费动作只变成建议。失败包只含阶段、异常类名、去掉路径的短消息与数值指标；全部审查记录留在任务目录 `supervisor/`。
- 后端新增 MySQL `0014_live2d_diagnosis.sql` / SQLite `0032_live2d_diagnosis.sql`：`live2d_jobs` 增加可空列 `diagnosis_code`、`suggestion`、`retry_hint`、`supervisor_summary`。`POST /internal/live2d/jobs/:id/fail` 接受白名单 `diagnosisCode`（`provider_unavailable` / `background_leak` / `face_not_located` / `expression_failed` / `rig_unstable` / `budget_exhausted`）、`suggestion`（`retry` / `regenerate_image` / `new_input`）与 ≤ 200 字、去控制字符的 `summary`，非法值 400，空请求体兼容旧制作端；`POST /ai/live2d/jobs/:id/retry` 接受 `{"hint":"regenerate_image"}` 并清空上次诊断；`claim` 响应新增 `retryHint`，只在第一次领取时下发并随即清空，租约丢失后的重领不会重复付费动作；任务响应新增 `diagnosisCode`、`suggestion`、`summary`，成功完成时清空。
- Android 生成记录里失败任务显示服务端诊断摘要与建议：`suggestion=regenerate_image` 且为提示词任务时显示"换背景重新生成"（注明将再消耗一次图片生成，调用 retry 带 `hint`），图片任务改为提示更换图片；无诊断时沿用"生成失败 / 重新生成"。`AnyiApiClient.live2dJobAction` 新增可选 `hint`，只允许 `retry` + `regenerate_image`。新增 `Live2dStudioSupport.kt`（纯函数，便于测试）。
- 新增测试：制作端 `test_foreground.py`（透明 / 近白 / 杂色背景处理、信箱坐标映射、泄漏检测与裁剪、干净拆层不改动）、`test_supervisor.py`（off 模式不调用、矩形回算与拒绝、预算跨状态文件、诊断白名单与降级、嘴部定位、摘要清洗），`test_pipeline` 新增幅度缩放与校验失败分类，`test_anyi_worker` 新增诊断载荷白名单与 `validation.json` 扩展字段；后端 Live2D 集成测试新增诊断上报、非法值 400、摘要截断、重试提示单次下发与旧制作端空请求体兼容；Android 新增 `Live2dFailureUiTest` 与 `retryWithHintSendsWhitelistedJsonBodyOnly`。

### 修改

- Android 版本提升为 `1.0.19`（versionCode 21），沿用 release-v2 签名；除版本号外与 `main` `16cfa9c` 无差异。
- `tools/live2d-worker/scripts/auto_build.py` 的 `run()` 重构为带恢复动作的阶段流程：阶段名与进度值不变（后端阶段白名单未改），重跑阶段时旧目录改名保留；`--reuse-decomposition` 现同时复制同目录的 `input/` 深度图；新增 CLI 参数 `--background`、`--supervisor-state`；`edit_face` 新增 `strict` 提示；`regions()` 允许在拆层前调用。`01_input_white.png` 文件名保留作为制作端检查点标记，但其背景现为中性灰而不是白。
- 制作端 `anyi_worker.py`：领取时读取 `retryHint`，`regenerate_image` 时放弃旧图片的全部检查点；构建子进程带 `--supervisor-state <任务目录>/supervisor-state.json`；上报失败时附带尝试目录 `supervisor/diagnosis.json` 里经白名单过滤的诊断；`validation.json` 增加 `motionScale`、`backgroundClipped`、`visualReview`。
- 文档：`docs/live2d-generation.md` 新增"规则恢复与监督"一节并更新接口表；`docs/security-operations.md` 补充自动质检的图片范围与留存说明；`tools/live2d-worker/README.md` 与 `.env.example` 补充 `IMAGE_BACKGROUND`、`LIVE2D_SUPERVISOR_MODE`、`SUPERVISOR_MODEL`、`SUPERVISOR_REASONING_EFFORT`。
- 与方案文档的差异：重试提示白名单只保留 `regenerate_image`（背景裁剪已由规则层自动完成，不再需要 `clip_background` 提示）；没有新增阶段名；`face_not_located` 目前只会由模型诊断给出。

### 修复

- 无。

### 移除

- 无。

### 运维/部署

- 数据库迁移 MySQL `0014_live2d_diagnosis.sql` / SQLite `0032_live2d_diagnosis.sql`：纯加列、可空、无默认值，服务启动时自动应用，不影响现有数据；回滚执行 `ALTER TABLE live2d_jobs DROP COLUMN diagnosis_code, DROP COLUMN suggestion, DROP COLUMN retry_hint, DROP COLUMN supervisor_summary`，客户端对缺失字段按无诊断处理。
- 新环境变量都有默认值，线上 `worker.env` 与服务端 `.env` 无需改动：`IMAGE_BACKGROUND`（默认 transparent）、`LIVE2D_SUPERVISOR_MODE`（默认 shadow）、`SUPERVISOR_MODEL`（默认同 `ASTRA_MODEL`）、`SUPERVISOR_REASONING_EFFORT`（默认 low）。不涉及密钥变更。
- 部署顺序：先部署后端（旧制作端发空请求体仍能标记失败），再在 Mac 发布新的 `releases/<sha>` 并切换 launchd；Android 改动需要随下一个 App 版本发布，本次未提升 versionCode。均需用户授权后执行。
- 部署（2026-09-18 01:16–01:22 CST，用户授权）：线上后端由 `7f8938f` 切换到 `main` 的 `16cfa9c`，启动时自动应用迁移 `0014_live2d_diagnosis.sql`（`_node_migrations` 13 → 14 行，四个新列已验证）；部署前备份 `anyi-mysql-anyi_memorial-20260918-011620.sql.gz` / `anyi-uploads-20260918-011620.tar.gz`；服务器构建哈希 `c09e0744c7ed` 与本机一致；回滚副本 `/opt/anyi-releases/before-main-20260918-16cfa9c`、`dist-node.pre-main-20260918-16cfa9c`。Mac 制作端切换到 `releases/16cfa9c`（plist 备份 `.bak-7f8938f`），监督器按默认 `shadow` 运行。Android 1.0.19 已构建、签名验证与 1.0.18 同证书，APK SHA-256 `6ed6ae4e5a6f00bc8a40620e306c0f87ae7862303619a6874af0c44b87949528`，上传为 `anyi-memorial-1.0.19-release-v2.apk` 并切换 `anyi-memorial-latest.apk` / `anyi-memorial-release-latest.apk`，公网回读哈希一致；AAB 保留本地未上传。详见 `docs/releases/2026-09-18-supervisor-1.0.19.md`。
- 验证：制作端 `unittest discover` 32 项通过（新增 11 项），后端 `npm test` 50 项通过，Android `:app:testDebugUnitTest` 19 项通过（新增 3 项）。用线上任务 `5462ebc4` 第二次尝试的产物回放新流程（复用规划、拆层 PSD 与表情图）：自动判定 `topwear` 泄漏并裁剪（1,014,498 → 146,729 像素，外接框 488,166–794,711，与同图灰底重拆结果一致），校验一次通过，脚底位移 0.0002 px，27 秒。影子模式再回放一次并真实调用 Astra：规划、拆层、收尾三次审查各 11.1 / 9.2 / 13.3 秒，规划关卡判定矩形正确，收尾评分 0.82 并列出闭眼与张嘴细节问题；预算文件正确记录 3 次调用；日志与审查记录中无密钥。

## 当前发布（2026-09-17）

### 2026-09-17 线上发布准备

- Android 版本更新为 `1.0.18`（versionCode 20），沿用现有 release-v2 签名。
- Nginx 增加仅制作端路径的 256 MB 上传配置，通用上传限制仍为 60 MB；制作端支持外部私有环境文件与 launchd 常驻。
- Astra 拆层规划改用流式响应，默认 low 推理强度，避免长非流式响应等待；异常或不完整输出不会发布为成功。
- 部署前已运行 MySQL 和 uploads 备份；只新增 0013 迁移，历史迁移哈希与线上一致。
- 线上 API、0013 迁移、Nginx 制作端上传配置与 Mac launchd worker 已部署；正式签名 APK 默认下载链接已切换到 1.0.18。部署来自 PR #3 的 e1c8e3a，未合并 main，备份和回滚路径见 `docs/releases/2026-09-17-live2d.md`。
- 线上全新图片任务已由常驻制作端自动完成：31 图层、21 参数、363 动作采样通过，私有 ZIP 下载、绑定及手机/桌面 WebGL 渲染通过。公网 APK 哈希与本地签名构建一致，测试账号验收后删除。

### 新增

- Android 陪伴对象新增“创建我的动态形象”，支持提示词/图片提交、后台任务进度、取消/重试、预览、精修 ZIP 下载及绑定到沉浸聊天。
- 后端新增 Live2D 私有生成任务、租约制作协议、哈希上传、完整性校验和模型文件读取；新增 SQLite 0031 / MySQL 0013 非破坏性迁移。
- `tools/live2d-worker/` 纳入已实测的图生 Live2D 流水线及出站轮询适配器；`docs/live2d-generation.md` 记录安装、部署和验证范围。
- 新增任务所有权、幂等、取消与租约、路径限制、Android 请求/下载及真实产物 WebGL 联调测试。
- 新增 `docs/codex-handover.md`，记录当前 Android、后端、腾讯云生产边界，以及照片数字人视频通话的实施路线和接手步骤。
- Android 新增 Live2D 动态形象与沉浸式聊天模式。APK 内置 6 个 Live2D Cubism 4 官方样例模型（Kei、Izumi、Haru、Hiyori、Tororo 白猫、Hijiki 黑猫，约 17 MB，已剔除示例语音与编辑器源文件），用户在聊天页"更多"菜单里为陪伴对象选择一个形象并绑定，进入"沉浸模式"后形象显示在聊天上方，AI 回复到达时播放说话动画，下方沿用同一条文字聊天流。形象与陪伴对象一一绑定，切换对象自动切换形象。
- 新增 `Live2dAvatarView.kt`：用 Android `WebView` + `WebViewAssetLoader` 从 APK 资产渲染 Live2D（pixi.js + pixi-live2d-display + Cubism Core，全部本地打包，不访问网络；禁用文件与内容访问，拒绝跳离资产域）。新增 `Live2dChatScreen.kt` 沉浸式聊天页，复用 `AiCompanionScreen` 现有的消息列表、草稿与按对象 FIFO 发送队列，不新增任何会话状态。
- 后端新增 `PATCH /ai/companions/:id/live2d`，请求体 `live2dModel` 为白名单内的模型 id 或 `null`（解绑）；非法 id 返回 `400 live2d_model_not_supported`。`GET /ai/companions` 与单个对象响应新增 `live2dModel` 字段。服务端只保存 id，不托管任何模型文件。
- 新增 `androidx.webkit:webkit:1.14.0` 依赖（`WebViewAssetLoader`），这是本次唯一新增的第三方依赖。
- 新增后端测试 `companion Live2D avatar binding accepts only bundled model ids`（绑定、非法 id 拒绝、解绑三种情况）和 Android 单元测试 `updateAiCompanionLive2dModel_*`（PATCH 方法、JSON 字段、`null` 解绑）。
- 沉浸模式页面固定显示"AI 生成形象"标识，形象选择弹窗注明"所有形象均为 AI 生成，不代表任何真实人物"。

### 修改

- 动态形象支持 `generated:<job-id>`，运行文件经 Android 原生鉴权代理加载；已生成模型按实际人物边界取景，待机口型不覆盖聊天口型。
- 根目录 README 增加 Codex 项目交接文档入口。
- `AiCompanion` 数据类新增 `live2dModel: String?`；`AiCompanionRow` 与 `serializeAiCompanion` 同步新增 `live2d_model` / `live2dModel`。
- 聊天页"更多"菜单新增"选择动态形象"（未绑定时）/ "沉浸模式" + "更换动态形象"（已绑定时）两项。
- 根目录 `README.md` 与 `backend/README.md` 中 9 处指向 `D:/Desktop/anyiapp2/...` 的 Windows 绝对路径链接改为仓库相对路径（腾讯云部署文档、上线清单、安全与运营、后台说明、三个法务模板、两个 Nginx 配置示例）。此前这些链接在原开发机以外的任何环境都无法打开。
- `docs/live2d-generation.md` 删除两处指向本机临时目录 `.tmp/live2d-smoke-VHORS5/`、`.tmp/live2d-smoke-X2LjAx/` 的定位说明。该目录是本地冒烟产物且已清理；验收结论本身保留在该文档与 `docs/releases/2026-09-17-live2d.md` 中。
- `docs/security-operations.md` 的资产删除队列处理步骤改为通过管理后台页面按钮或 `curl` 直接调用 `POST /admin/asset-delete-queue/process`，不再依赖已删除的 PowerShell 脚本。
- 根 `.gitignore` 与 `backend/.gitignore` 移除已无对应文件的 `.dev.vars`、`.wrangler/` 忽略规则。
- 后端把 SQLite 与 MySQL 迁移运行器逐字重复的 `splitSqlStatements()` / `checksumSql()`（约 80 行）抽到新文件 `backend/server/migration-sql.ts`，`sqlite-db.ts` 与 `mysql-db.ts` 改为引用同一实现；迁移语句切分与校验和行为不变。

### 修复

- 制作端嘴部测量 `tools/live2d-worker/scripts/auto_expression.py` 的口腔暗区阈值由固定的灰度 145 改为按 145、120、100 依次回退，第一个不触及搜索框边界的结果生效，并在 recipe 的 `mouth.dark_threshold` 记录实际使用的阈值。此前写实风格图片里白胡须阴影和嘴周皮肤暗部都低于 145，与口腔连成一片撑满搜索框，触发 `Mouth measurement hit the search boundary` 自检，线上任务 `5462ebc4` 在精修阶段因此失败；插画风格图片在 145 即通过，输出与修改前逐字节一致。所有阈值都失败时仍抛出原错误。
- 制作端 Astra 拆层规划 `tools/live2d-worker/live2d_pipeline.py` 在流式响应被对端中途关闭（`httpx.TransportError`，如 `peer closed connection without sending complete message body`）、连接失败或网关 5xx 时，按 `ASTRA_STREAM_RETRIES`（默认 1 次）重新发起同一请求，间隔 5 秒递增。OpenAI SDK 的 `max_retries` 只覆盖响应开始前的失败，流中断此前会直接让整个任务失败并浪费已生成的图片；`finish_reason` 不是 `stop` 的完整响应仍按原逻辑报错、不重试。
- 制作端新增空输出目录与阶段事件，失败任务仅复用同一任务的输入检查点；旧租约不能覆盖或发布新任务结果。
- 修复 Live2D 绑定的两个 Android 单元测试在本机 JVM 上失败（`ProtocolException: Invalid HTTP method: PATCH`）。原因是桌面 JDK 的 `HttpURLConnection` 用一个私有静态白名单校验请求方法，该白名单不含 `PATCH`；Android 的 `HttpURLConnection` 由 OkHttp 实现，可正常发送 `PATCH`。即客户端代码在真机上正确，只有本机测试 JVM 无法表达该方法。已新增测试专用辅助 `app/src/test/java/com/anyi/memorial/network/HttpMethods.kt`，通过反射把白名单中未使用的 `TRACE` 槽位替换为 `PATCH`（不改变数组长度和其他槽位），并在 `app/build.gradle.kts` 的 `testOptions` 为单元测试加上 `--add-opens java.base/java.net=ALL-UNNAMED`。生产代码与 `AnyiApiClient` 未做任何修改。此前 8 个 `PATCH` 调用点从未被单元测试用真实 socket 覆盖，所以该限制一直没有暴露。

### 移除

- 清理 `app/src/main/java/com/anyi/memorial/MainActivity.kt` 中 5 个未使用的 import：`rememberLazyListState`、`Icons.AutoMirrored.Rounded.Send`、`Checkbox`、`Switch`、`graphicsLayer`。
- 移除已退役数字人前端残留在 `isCacheableCloudResource` 中的缓存放行项：`/vtuber/`、`/live2d-models/` 两个路径前缀，以及 `.moc3`、`.wasm`、`.atlas`、`.skel` 四个扩展名。App 中没有任何代码构造这些 URL，后端也已将 `/vtuber` 与 `/vtuber/` 返回 404；该函数只是磁盘缓存白名单，删除后仅不再缓存这些类型，不影响任何正常加载。
- 删除根目录两个与官网资源完全重复的图片：`source_app_icon.png`（1254×1254，1.4 MB）与 `AnyiMemorial-download-qr.png`。二者分别与 `website/assets/app-icon.png`、`website/assets/download-qr.png` 的 SHA-256 完全一致，且没有任何构建脚本、Gradle 配置或代码引用根目录副本。官网仍使用 `website/assets/` 下的同名文件，视觉无变化。
- 移除鸿蒙端整个 AI 陪伴页：`huawei-harmonyos/entry/src/main/ets/pages/tabs/CompanionPage.ets`（485 行）。该页面调用的 5 个接口 `/app/digital-human/chat|messages|memories|memory-settings` 后端已不存在，生产实测全部返回 404；页面还从 `/app/config` 读取后端已不再返回的 `digitalHuman` 字段，导致功能开关恒为关闭。鸿蒙端标签页由 4 个减为 3 个（纪念馆、人文社区、个人中心），`Index.ets` 的 `activeTab` 索引同步由 0/1/2/3 改为 0/1/2。
- 移除鸿蒙端随上述页面一并失效的内容：`ApiClient.ets` 的 5 个 digital-human 方法和 `DigitalHumanPayload` 接口、`Constants.ets` 的 `DEFAULT_DIGITAL_HUMAN_URL`、`Models.ets` 的 7 个死类型（`DigitalHumanMessage`、`DigitalHumanMessagesResponse`、`DigitalHumanChatResponse`、`DigitalHumanConfig`、`AiMemory`、`AiMemoriesResponse`、`AiMemorySettingsResponse`），以及三张不再被引用的图片 `anyi_digital_grandma.png`（1.2 MB）、`anyi_digital_grandpa.png`（1.1 MB）、`anyi_ai_page_bg.png`（319 KB）。
- 修正 `AppConfigResponse` 类型：原先只声明 `digitalHuman` 字段，与后端实际返回的 `wechat`/`payments`/`ai` 结构不符，已按生产响应重建为 `WechatConfig`/`PaymentsConfig`/`AiConfig`。
- 更新 `huawei-harmonyos/README.md`：删除 "ArkWeb：现有 2D 数字人页面" 与功能表里的 AI 陪伴行，并说明该页被移除的原因。
- 删除 `docs/xhs-product-engineer-interview.md`（131 行）及 `README.md` 中的“面试讲法”链接。该文件是个人面试准备材料，不属于产品文档；`.gitignore` 已将简历等个人文件排除在仓库外，此文件属漏入。
- 删除 `backend/.dev.vars.example`。它是 Cloudflare Workers 时期的环境变量模板：仓库没有 `wrangler.toml`，后端代码与文档没有任何地方读取 `.dev.vars`；其 27 个键中 26 个已由 `backend/.env.server.example` 覆盖，唯一独有的 `ADMIN_USERNAMES` 后端并不读取。服务器环境变量模板统一为 `backend/.env.server.example`。
- 删除 `backend/scripts/process-asset-delete-queue.ps1` 及 `backend/package.json` 中的 `asset:delete:process` 脚本。该脚本只能在 PowerShell 下运行，Mac / Ubuntu 环境均不可用；它调用的接口 `POST /admin/asset-delete-queue/process` 保留不变，管理后台页面已提供同一“处理资产删除队列”操作。
- 删除华为 HarmonyOS 客户端整个目录 `huawei-harmonyos/`（36 个文件，约 3,700 行 ArkTS / JSON5 与 6 张图片）。该客户端没有 AI 陪伴页、未随近期功能迭代，用户于 2026-09-17 确认删除；需要时可从提交 `5be7ea1` 完整恢复。同步删除根 `README.md` 的华为小节与目录说明、`.editorconfig` 的 `ets` / `json5` 规则和 `.gitignore` 注释中的引用；`docs/codex-handover.md` 改为记录删除事实与恢复方式，`docs/live2d-generation.md` 去掉 HarmonyOS 说明。
- 后端删除 `assetDeleteQueueKeyColumn()` 及其 `r2_key` 回落分支：所有能由仓库迁移文件重建的数据库（SQLite `0004` 建表、MySQL `0001` 建表）该列都叫 `asset_key`，生产 MySQL 亦然；此前每次入队或处理删除队列都要多做 1～2 次 `INFORMATION_SCHEMA` / `PRAGMA` 探测。`GET /admin/asset-delete-queue` 不再做 `asset_key` 别名映射，直接返回原始行。同时删除纯别名函数 `loadAiCompanion()`（3 处调用改为 `loadAiCompanionRow()`）和 `publicRequestOrigin()` 中已被前一行正则排除、不可达的空 host 分支。
- Android 删除从未被触发的付费路径：`MemorialHallScreen` 的 `showPayment` / `showDurianPayment` 两个“付费功能暂未开放”对话框、`paidUnlocked` / `durianUnlocked` 状态、`offerFruit(unlockDurian)` 参数、榴莲供品渲染（`FruitIcon`、`hasDurian`、`activeDurians` 等）与 1.4 MB 图片 `drawable-nodpi/anyi_hall_durian_real.png`，以及 `AnyiApiClient.featureUnlocked()` / `unlockFeature()`。灵台每次刷新由 3 个请求减为 1 个（不再请求 `GET /feature-unlocks/hall_more` 与 `offering_durian`；后端接口本身保留）。`userFriendlyMessage` 去掉 3 条对应的榴莲 / 支付文案。
- Android 删除 `PaperBurningAnimation`（约 145 行 Canvas 烧纸动画）及 `MemorialStage` 的 `burnPaperAnimationKey` 参数：该 key 从未递增，动画在任何路径下都不会渲染；连带移除 6 个仅供其使用的 import。
- Android 删除与当前服务端不对应的兼容分支：`parseCommunityPost` / `Comment` / `Volunteer` / `Application` 的 snake_case 键回退（`display_name`、`user_id`、`created_at` 等，服务端只输出 camelCase）、`parseCompanionResponse` 对 `profile` / `imageUrl` / `asset` / `avatar` 等五种响应形状的猜测、`listAiImageModels` 的 `imageModels` 键、`AiImageModel.description`、`companionError` 的 `voice_too_long` 码、`readTimeoutFor` 的 `/acceptance` 路径、`absoluteAssetUrl` 对旧 IP `101.42.1.45` 的改写（迁移 `0021` 已在库内改为正式域名），以及 `isCacheableCloudResource()` 匿名磁盘缓存白名单（含退役数字人时代的 `.js` / `.css` / `.wav` 等扩展名）。图片磁盘缓存现在只对 API 域名 `/assets/` 下的鉴权资源生效，其它外链仍走内存缓存与直接请求，与此前实际行为一致。
- 制作端 `tools/live2d-worker/scripts/auto_build.py` 删除 `make_recipe()` 中随后被 `measure_mouth()` 与 `preserve_texture` 整体覆盖的 `poly()` 多边形和肤色采样点计算，输出的 recipe 内容不变。

### 运维/部署

- 新增默认关闭的 `LIVE2D_ENABLED` 与服务端 `LIVE2D_WORKER_TOKEN`；Mac 制作端使用 `ANYI_API_URL`、`ANYI_WORKER_TOKEN` 和独立供应商/SSH 配置，详情见接入文档。本次仅本地修改和验证，生产未部署。
- 验证：后端 50 项、Android 16 项和制作端 15 项测试通过；Debug APK 构建成功。真实老爷爷产物通过私有上传、下载、绑定及 390×700 / 1200×800 WebGL 非空像素与运动检查。另一次全新图片任务在 Astra 阶段遭遇 Apexin 504，任务已正确回传失败状态；不将此轮记为全新生成成功。
- 新增数据库迁移：MySQL `0012_ai_companion_live2d.sql`、SQLite `0030_ai_companion_live2d.sql`，为 `ai_companions` 增加可空列 `live2d_model`。为纯加列、可空、无默认值，不影响现有数据；服务启动时自动应用。回滚只需 `ALTER TABLE ai_companions DROP COLUMN live2d_model`，客户端对缺失字段按未绑定处理。
- 本次不涉及环境变量或密钥变更。生产尚未部署，需先在本地完成 Android 构建与真机验收再发布。
- 授权说明：6 个模型均受 Live2D《免费素材许可协议》v1.6 约束，"一般用户/小规模企业可用于任何营利或非营利目的"，Cubism Core 文件头标注为 Redistributable Code，当前个人使用合规。若未来年销售额达到 1000 万日元或以商业形式发行，需另行取得 Live2D 出版许可并复核形象授权，详见项目记忆 `anyi-live2d-licensing`。
- 补充说明：本次仅删除鸿蒙端代码与资源。生产服务器上的 `open-llm-vtuber.service` 仍为 inactive/disabled 状态，nginx 对 `/vtuber`、`/live2d-models/`、`/tts-ws` 等的 404 拦截保持不变，本次未改动服务器。
- 验证：本次清理后后端 `npm test` 50 项通过；Android `:app:compileDebugKotlin` 与 `:app:testDebugUnitTest` 在本机 SDK（build-tools 36.1.0、JDK 21）通过，16 项单元测试全部通过；制作端 `unittest discover` 16 项通过。未构建 release 包，未部署。
- 验证：制作端 `unittest discover` 21 项通过（新增 5 项）。用线上任务 `5462ebc4` 第二次尝试的真实产物在本机 dry-run（复用规划、拆层 PSD 与表情图，不调用供应商）：精修、绑定阶段通过，嘴部按阈值 120 测出；但校验阶段以 `feet max displacement=0.32 px`（阈值 0.25）失败。原因是 4090 See-through 把这张白底写实图的整个背景并入 `topwear` 图层（bbox 覆盖 213–1067 × 0–1280 整幅画布，约 101 万像素，正常应约 12 万），呼吸变形器因此固定到画布底部并带动脚部；该问题与本次修复无关，即使修复发布后重试此任务仍会在校验阶段失败，需要另行处理（重新生成非白底图片或在拆层后按前景轮廓裁掉背景）。
- 制作端新增可选环境变量 `ASTRA_STREAM_RETRIES`（默认 1，`.env.example` 已补充），无需改动线上 `worker.env`。本次修复只涉及 Mac 制作端代码，线上 API、数据库与 Android 均无变化；生效需要在 Mac 上以合并后的提交发布新的 `releases/<sha>` 目录并切换 launchd，未经授权前不部署。
- 部署（2026-09-17 23:22–23:29 CST，用户授权）：线上后端由 PR #3 分支的 `e1c8e3a` 切换到 `main` 的 `7f8938f`（含 PR #4、#5、#6），Mac 制作端 launchd 切换到 `releases/7f8938f`。部署前已运行 `anyi-mysql-backup.service`（`anyi-mysql-anyi_memorial-20260917-232532.sql.gz` / `anyi-uploads-20260917-232532.tar.gz`）。服务器用同一 `tsc` 先重建 `e1c8e3a` 得到与线上一致的哈希 `e404f79ea1e3`，再构建 `7f8938f` 得到 `df3f9db09af7`（与本机构建一致）。无新迁移，`_node_migrations` 保持 13 行；`.env`、`node_modules`、Nginx 未改动。回滚副本：`/opt/anyi-releases/before-main-20260917-7f8938f`、`/opt/anyiapp2/backend/dist-node.pre-main-20260917-7f8938f`、`~/Library/LaunchAgents/cn.anyibj.live2d-worker.plist.bak-e1c8e3a`。验证：三项服务 active，本地与公网 `/health` 均 ok，重启后 journal 无错误、Nginx 全部 200、无凭据访问改动路径均 401；制作端 claim 轮询 200、`worker.err` 无新增。详见 `docs/releases/2026-09-17-main-7f8938f.md`。

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
