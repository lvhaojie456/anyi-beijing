# Live2D 生成接入

## App 工作流

Android：AI 陪伴 → 进入对象聊天 → 更多 → 选择/更换动态形象 → 创建我的动态形象。

用户选择提示词或图片后提交任务；页面轮询进度，离开页面不会取消制作。任务成功后可以预览、通过系统文件选择器下载精修 ZIP，或“使用此形象”进入同一对象的沉浸聊天。失败与取消任务可以重试；同一对象同时只能有一个生成任务。内置模型选择和解绑仍然保留。

本次只接 Android 与共享后端。

## 架构

```text
Android Compose
  → Hono API + MySQL/SQLite 持久化任务
  ← 私有预览、运行资产、精修 ZIP
          ↑ 领取/续约/分文件上传
Mac 制作端（tools/live2d-worker）
  → Image-2.5 / Astra
  → SSH 4090 See-through
  → 本机 psd2live + Cubism Core 验证
```

后端负责鉴权和持久化，不在 HTTP 请求中执行 GPU 推理。模型文件只能由所属用户读取。Android 在原生网络层附带登录凭证，代理到 WebView 的虚拟同源路径，JS 不获得 token；页面加载、脚本和模型请求不能跳到外部域名。后端拒绝模型引用中的绝对路径、外部 URL、父目录和脚本，上传按租约隔离并进行哈希检查，完整性验证后才发布。

## 接口

| 方法与路径 | 用途 |
| --- | --- |
| GET /ai/live2d/config | 生成开关 |
| POST /ai/companions/:id/live2d/jobs | multipart prompt 或 file，必带 UUID Idempotency-Key |
| GET /ai/companions/:id/live2d/jobs | 对象最近 20 项任务 |
| GET /ai/live2d/jobs/:id | 单项状态和进度 |
| POST /ai/live2d/jobs/:id/cancel | 取消未完成任务 |
| POST /ai/live2d/jobs/:id/retry | 重试失败或取消任务；可带 `{"hint":"regenerate_image"}`，制作端据此放弃旧图片重新生成 |
| POST /ai/live2d/jobs/:id/activate | 绑定成功产物 |
| GET /ai/live2d/jobs/:id/files/* | 本人读取运行资产、预览或 project.zip |
| POST /internal/live2d/jobs/claim | 制作端领取任务；响应含 `retryHint`，只在第一次领取时下发 |
| POST /internal/live2d/jobs/:id/heartbeat | 续约及进度 |
| GET /internal/live2d/jobs/:id/input | 领取者下载源图 |
| POST /internal/live2d/jobs/:id/artifacts?name=… | 单文件 multipart，带 X-Content-SHA256 |
| POST /internal/live2d/jobs/:id/complete | 校验齐全后发布 |
| POST /internal/live2d/jobs/:id/fail | 标记失败，不暴露供应商响应；可带白名单 `diagnosisCode`、`suggestion` 与 ≤ 200 字 `summary`，任务响应原样透给本人 |

内部接口必须携带 `Authorization: Bearer <LIVE2D_WORKER_TOKEN>`；领取后的操作还需 `X-Live2d-Lease`。租约 120 秒、15 秒续约、最多 3 次自动领取。前端显示真实阶段进度而非预计完成时间。每用户每日新建最多 10 项任务，源图最多 8 MB，单个运行资产最多 32 MB、精修 ZIP 最多 240 MB，总产物最多 350 MB。

## 规则恢复与监督

2026-09-18 起制作端按 `docs/live2d-supervisor-plan.md` 的三层结构处理失败。

规则层始终生效、不调用模型：

- 提示词生成默认请求透明背景（`IMAGE_BACKGROUND=transparent`，供应商不支持时回落普通生成）。进拆层前 `foreground.neutralize_background` 把透明或近白（角落近白且与边框连通）背景填成中性灰 (210,210,210)：See-through 会把纯白背景当作人物并入衣服图层，灰底则分得干净。
- 拆层后 `foreground.clip_background` 检查每个图层：外接框超过画布 60%，或按逐层深度图 `input/<part>_depth.png` 饱和（≥ 250）像素超过 30%，或落在前景遮罩外的像素超过 30%，判定背景泄漏。只对泄漏图层重建：保留 深度 < 250 且在遮罩内 的像素，形态学开运算、保留 ≥ 最大连通域 2% 的区块并填洞，写出 `decomposition/input_clipped.psd`，其余图层逐字节不变。
- 校验只因脚底位移 ≥ 0.25 px 或翻转三角形未通过（顶点有限、无退化三角形）时，按 `MOTION_SCALES = (1.0, 0.66, 0.33)` 缩小倾斜、呼吸、手臂、衣摆幅度重新绑定并复检，最多两次；`validation.json` 记录 `motionScale`。
- Astra 流中断按 `ASTRA_STREAM_RETRIES` 重试；嘴部暗区阈值 145/120/100 回退。

监督层（`LIVE2D_SUPERVISOR_MODE`：`off` / `shadow`（默认）/ `act`）用非流式 JSON 请求调用 `SUPERVISOR_MODEL`（默认同 `ASTRA_MODEL`），每次带 ≤ 512 px 缩略图，温度 0，超时 60 秒，失败不重试、不阻断：

| 时机 | 材料 | 回答 | shadow | act |
| --- | --- | --- | --- | --- |
| 规划后 | 输入图 + 脸/眼/嘴矩形 | 是否落对，修正矩形 | 记录 | 用修正矩形覆盖 `layer_plan.json` |
| 拆层后 | 图层缩略表 + 规则层报告 | 是否泄漏、错分、缺失 | 记录 | 记录（裁剪由规则层决定） |
| 表情后（仅新生成时） | 原脸 / 闭眼 / 张嘴 | 是否闭眼、张嘴、保持身份 | 记录 | 不合格则用更严格提示重做一次 |
| 嘴部三档阈值都失败 | 张嘴编辑图 | 嘴的矩形 | 用该矩形重测 | 同 |
| 校验通过后 | 表情表 + 姿态表 | 0–1 评分与问题 | 写入 `validation.visualReview`，不阻断 | 同 |
| 最终失败 | 失败包 + 缩略图 | 诊断码、菜单动作、一句中文 | 只用于诊断码与摘要 | 另执行 `replan_with_hint`（删除本次规划检查点，下次重试重新分析） |

每单预算（跨尝试，存于任务目录 `supervisor-state.json`）：模型调用 8 次、重规划 2 次、重绑定 2 次、重做表情 1 次、重生成图片 1 次。付费动作不会自动执行，只作为 `suggestion=regenerate_image` 透给 App，用户点"换背景重新生成"后以 `hint` 重试，制作端在领取时收到 `retryHint` 并放弃旧图片的全部检查点。

诊断码白名单：`provider_unavailable`、`background_leak`、`face_not_located`、`expression_failed`、`rig_unstable`、`budget_exhausted`；建议白名单：`retry`、`regenerate_image`、`new_input`。失败包只含阶段、异常类名、去掉路径的短消息和数值指标；审查记录留在任务目录 `supervisor/`，不上传。

回放验证：线上任务 `5462ebc4` 第二次尝试的产物在新流程下自动判定 `topwear` 泄漏并裁剪（1,014,498 → 146,729 像素，外接框 488,166–794,711，与同图灰底重新拆层的结果一致），校验一次通过，脚底位移 0.0002 px。

## 部署

1. 备份数据库，部署后端并运行迁移：SQLite `0031_live2d_generation.sql` / MySQL `0013_live2d_generation.sql`。只增加可空绑定列和两个任务表，不清空既有数据。
2. 服务端配置 `LIVE2D_WORKER_TOKEN` 为至少 32 字节随机密钥，制作端配置同值 `ANYI_WORKER_TOKEN`。密钥不进入 Android、Git 或 API 响应。
3. 制作端按 [安装说明](../tools/live2d-worker/README.md) 配置 Python、JDK、独立 Cubism Core、锁定的 psd2live 和远程 4090，然后启动 worker。
4. Nginx 的 `/internal/live2d/` 反向代理路径需要 `client_max_body_size 256m` 和足够的上传超时（建议 300s）；保留 Authorization 与 X-Live2d-Lease、X-Content-SHA256 请求头。继续使用 HTTPS。大文件按文件上传，API 在缓冲前检查制作端凭证；生产服务需预留相应内存。
5. 启用 `LIVE2D_ENABLED=true`，发布经过测试的 Android APK。开关默认 false，未配置时客户端显示“生成服务未开启”。无需公开访问 Mac 或 4090。

应用账号删除沿用资产清理机制；对象删除会取消任务并排队清理其输入和产物。失败/取消的服务端资产和制作主机检查点仍保留供运维重试，需配置保留期清理。制作端离线时任务保持排队；部署方需要持续运行 worker。

回滚：先设 `LIVE2D_ENABLED=false`，停止制作端，回滚 API/App。不要立即删除任务表，保留工程文件和历史；新增字段为可空列。

2026-09-17 已授权部署线上 API、MySQL 迁移、Mac 常驻制作端与正式签名 1.0.18 APK 下载，见 [发布记录](releases/2026-09-17-live2d.md)。未执行应用商店发布。真实生成对任意照片的成功率以及 `.cmo3` 在编辑器中的完整兼容性仍待继续完善；UI 明确将结果标为待精修初版。

## 验证

隔离本地开发 API：`cd backend && npm run server:build && node scripts/start-live2d-local.mjs 8789`。
它只监听 `127.0.0.1`，使用 `.tmp/live2d-local` 数据库，随机密钥保存到该目录的 `private-config.json`（权限 600）。制作端连接时使用其中 workerToken。Android 模拟器调试构建可设置 `-PANYI_API_BASE_URL=http://10.0.2.2:8789 -PANYI_API_BASE_URLS=http://10.0.2.2:8789`；真机需本地端口转发。该开发模式仍需另外启动制作端才能消费队列。

```bash
cd backend
npm test
node scripts/smoke-live2d.mjs ../tools/live2d-worker /path/to/verified-output /path/to/python /path/to/playwright
```

联调脚本创建隔离 SQLite 与本地 API，使用真实已验证产物调用制作端打包/上传协议，然后验证任务发布、绑定、私有下载和 App 的同源 WebGL 页面。最后可选运行 Playwright 手机/桌面截图、非空像素与运动检查。这条测试不调用付费生图，不等同于全新角色的一次性生成成功。

附加 `--generate` 可改为由制作端重新执行图片输入的完整生成流程（会调用真实供应商与 GPU），仍使用隔离的本地 API/数据库。该模式需要给制作端传入实际供应商和 SSH 环境变量。

### 2026-09-17 验证结果

- 后端 50 项测试、Android 16 项单元测试、制作端 15 项测试通过；调试 APK 编译通过。
- 真实已验证老爷爷模型经实际制作端协议打包、上传、发布、绑定及私有下载成功。精修 ZIP 约 16 MB，包含 PSD、CMO3、MOC3、纹理和动作。
- 使用 App 原始 HTML 和 SDK 的 Playwright 验证：390×700 与 1200×800 均能载入模型；WebGL 有效像素分别超过 8 万、11 万；截图非空、运动帧发生变化，无页面脚本错误。
- 另一次 `--generate` 真实新任务成功创建并领取，在 Astra 规划阶段遭遇供应商 Apexin 504。续约期间状态保持 running，异常后 worker 回传 failed / generation_failed。该轮未进入后续 GPU 阶段，不能算作全新生成成功。
- 当前未执行 MySQL 实库迁移、Android 真机操作、生产部署或商店发布；新增 MySQL SQL 使用与既有项目一致的字段/外键风格，上线前应在预发布库应用并验收。

上述为接入阶段记录。后续同日线上发布已完成 MySQL 隔离恢复迁移、生产迁移和全新图片任务验收，详见 [线上发布记录](releases/2026-09-17-live2d.md)。尚未执行应用商店上架和 Android 真机手工验收。
