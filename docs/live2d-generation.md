# Live2D 生成接入

## App 工作流

Android：AI 陪伴 → 进入对象聊天 → 更多 → 选择/更换动态形象 → 创建我的动态形象。

用户选择提示词或图片后提交任务；页面轮询进度，离开页面不会取消制作。任务成功后可以预览、通过系统文件选择器下载精修 ZIP，或“使用此形象”进入同一对象的沉浸聊天。失败与取消任务可以重试；同一对象同时只能有一个生成任务。内置模型选择和解绑仍然保留。

当前 HarmonyOS 没有 AI 陪伴页，本次只接 Android 与共享后端。

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
| POST /ai/live2d/jobs/:id/retry | 重试失败或取消任务 |
| POST /ai/live2d/jobs/:id/activate | 绑定成功产物 |
| GET /ai/live2d/jobs/:id/files/* | 本人读取运行资产、预览或 project.zip |
| POST /internal/live2d/jobs/claim | 制作端领取任务 |
| POST /internal/live2d/jobs/:id/heartbeat | 续约及进度 |
| GET /internal/live2d/jobs/:id/input | 领取者下载源图 |
| POST /internal/live2d/jobs/:id/artifacts?name=… | 单文件 multipart，带 X-Content-SHA256 |
| POST /internal/live2d/jobs/:id/complete | 校验齐全后发布 |
| POST /internal/live2d/jobs/:id/fail | 标记失败，不暴露供应商响应 |

内部接口必须携带 `Authorization: Bearer <LIVE2D_WORKER_TOKEN>`；领取后的操作还需 `X-Live2d-Lease`。租约 120 秒、15 秒续约、最多 3 次自动领取。前端显示真实阶段进度而非预计完成时间。每用户每日新建最多 10 项任务，源图最多 8 MB，单个运行资产最多 32 MB、精修 ZIP 最多 240 MB，总产物最多 350 MB。

## 部署

1. 备份数据库，部署后端并运行迁移：SQLite `0031_live2d_generation.sql` / MySQL `0013_live2d_generation.sql`。只增加可空绑定列和两个任务表，不清空既有数据。
2. 服务端配置 `LIVE2D_WORKER_TOKEN` 为至少 32 字节随机密钥，制作端配置同值 `ANYI_WORKER_TOKEN`。密钥不进入 Android、Git 或 API 响应。
3. 制作端按 [安装说明](../tools/live2d-worker/README.md) 配置 Python、JDK、独立 Cubism Core、锁定的 psd2live 和远程 4090，然后启动 worker。
4. Nginx 的 `/internal/live2d/` 反向代理路径需要 `client_max_body_size 256m` 和足够的上传超时（建议 300s）；保留 Authorization 与 X-Live2d-Lease、X-Content-SHA256 请求头。继续使用 HTTPS。大文件按文件上传，API 在缓冲前检查制作端凭证；生产服务需预留相应内存。
5. 启用 `LIVE2D_ENABLED=true`，发布经过测试的 Android APK。开关默认 false，未配置时客户端显示“生成服务未开启”。无需公开访问 Mac 或 4090。

应用账号删除沿用资产清理机制；对象删除会取消任务并排队清理其输入和产物。失败/取消的服务端资产和制作主机检查点仍保留供运维重试，需配置保留期清理。制作端离线时任务保持排队；部署方需要持续运行 worker。

回滚：先设 `LIVE2D_ENABLED=false`，停止制作端，回滚 API/App。不要立即删除任务表，保留工程文件和历史；新增字段为可空列。

本次为本地代码接入，没有部署生产服务或发布商店版本。真实生成对任意照片的成功率以及 `.cmo3` 在编辑器中的完整兼容性仍待继续完善；UI 明确将结果标为待精修初版。

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
- 使用 App 原始 HTML 和 SDK 的 Playwright 验证：390×700 与 1200×800 均能载入模型；WebGL 有效像素分别超过 8 万、11 万；截图非空、运动帧发生变化，无页面脚本错误。截图与报告位于本机 `.tmp/live2d-smoke-VHORS5/`。
- 另一次 `--generate` 真实新任务成功创建并领取，在 Astra 规划阶段遭遇供应商 Apexin 504。续约期间状态保持 running，异常后 worker 回传 failed / generation_failed。该轮未进入后续 GPU 阶段，不能算作全新生成成功。本地诊断位于 `.tmp/live2d-smoke-X2LjAx/`。
- 当前未执行 MySQL 实库迁移、Android 真机操作、生产部署或商店发布；新增 MySQL SQL 使用与既有项目一致的字段/外键风格，上线前应在预发布库应用并验收。
