# 安忆项目交接文档

更新时间：2026-09-19。本文以 `main` 与线上核验结果为准；生产服务器状态必须按文末命令重新核验，不要凭文档推断。

## 1. 项目是什么

安忆是一款 Android 纪念与陪伴应用。用户可以为亲人、宠物、地点或物品建立云端纪念馆和 AI 陪伴对象；陪伴对象可以绑定内置或按提示词/照片生成的 Live2D 动态形象，AI 回复会出声、口型跟随，并保存为语音条。

北极星目标仍然是：用户上传一张亲人照片，之后能和基于该照片、人物资料与真实记忆的 AI 形象进行**视频通话**，页面必须明确标注为 AI 生成形象。这条实时链路（流式 STT → 流式 LLM → 流式 TTS → 照片驱动口型 → WebRTC）**还没有做**，现有能力是它的前置台阶。

## 2. 当前状态（2026-09-19 核验）

| 项目 | 当前值 |
| --- | --- |
| `main` | `804a24d`（PR #16 之后）；官网改版 PR #17 与仓库整理 PR 待合并 |
| 线上后端 | `main` `fc04951`，`/opt/anyiapp2/backend/RELEASE.json` 记录 `codeRevision` |
| 数据库 | MySQL `anyi_memorial`，`_node_migrations` 15 行，最新 `0015_ai_companion_voice.sql` |
| Android | 1.0.21（versionCode 23），release-v2 签名，线上 `anyi-memorial-latest.apk` 指向它 |
| Mac 制作端 | `~/Library/Application Support/AnyiLive2D/releases/d1611fa`，launchd `cn.anyibj.live2d-worker` |
| 线上开关 | `LIVE2D_ENABLED=true`、`AI_VOICE_ENABLED=true`、`TTS_ENABLED=true`、`PAYMENT_ENABLED=false`、`RATE_LIMIT_ENABLED=true` |
| AI 模型 | 聊天 `gpt-5.6-luna`，记忆整理 `gpt-5.5`，都经 Apexin；图片生成 `gpt-image-2` 与 6 个 Gemini 型号 |
| 语音 | 输入：腾讯云一句话识别 `16k_zh`；输出：腾讯云 `TextToVoice`，音色白名单 `uncle`/`aunt`/`gentle` |
| 官网 | `https://anyi-memorial-site.pages.dev`（Cloudflare Pages，手工 `wrangler pages deploy`） |
| 服务器 | 腾讯云 CVM `VM-0-8-ubuntu`，Node 20.20.2，tsc 5.9.3，磁盘 62% |

发布历史见 [docs/releases/README.md](releases/README.md)。

## 3. 必须遵守的规则

- **不直接推送或合并 `main`。** 每个改动开 PR，标题用中文。`main` 只靠约定保护，没有分支保护规则。
- **每个 PR 都写 `CHANGELOG.md`**，在 `## 未发布` 下按 `新增 / 修改 / 修复 / 移除 / 运维/部署` 记录；删除的东西写原位置和替代位置。发布后把 `未发布` 归档到带时间的 `当前发布` 标题下。
- **生产部署必须得到用户明确授权**，包括后端、制作端、安装包、Nginx、`.env`。
- **密钥不进仓库、不进聊天、不进日志。** `.env`、SSH 私钥、keystore 密码、供应商 API key 只在服务器 `.env` 和维护者本机的安全目录。文档里只写键名，不写值。
- 不要重建已删除的 `huawei-harmonyos/` 目录（2026-09-17 删除，可从 `5be7ea1` 恢复）。
- 个人材料（简历、面试稿、个人截图）不属于仓库。

## 4. 代码地图

### Android（`app/`，Jetpack Compose，包名 `com.anyi.memorial`）

- `MainActivity.kt`（约 7,000 行）：应用壳、导航、隐私同意门、云端纪念馆、人文社区、义工、个人设置。
- `AiCompanionScreen.kt`：陪伴对象列表、聊天、记忆、头像、音色选择、`speakReply()`。
- `Live2dChatScreen.kt` / `Live2dAvatarView.kt`：沉浸聊天与 WebView 渲染；`window.anyiLive2d.{load,speak,idle,setLipSync}`。
- `Live2dStudioScreen.kt` / `Live2dStudioSupport.kt`：提示词或照片生成形象、任务进度、失败原因与重试。
- `AiSpeechQueue.kt`：回复分句、逐句合成、顺序播放、预取下一句、播放包络驱动口型。
- `network/AnyiApiClient.kt`：`HttpURLConnection` + `org.json`，多 base URL 回退，非 debug 只允许 HTTPS。
- 不变量：所有陪伴状态按 `companionId` 隔离；`PrivacyConsentStore.POLICY_VERSION` 变更会重新弹隐私同意，披露文案变了就要升版本；网络层不接触任何供应商密钥。
- 单元测试：`app/src/test/`，23 项。

### 后端（`backend/`，Node 20 + Hono）

- `src/index.ts`：几乎所有业务（认证、纪念馆、社区、义工、AI 陪伴、记忆、语音输入/输出、管理后台、法律页面）。
- `src/live2d.ts`：Live2D 任务队列的用户接口与制作端内部接口。
- `server/server.ts`：启动入口。**它按键名白名单把 `process.env` 转给应用**，新增环境变量必须同时加进这个白名单，否则线上读不到（2026-09-18 因此吃过一次亏）。
- `server/mysql-db.ts` / `sqlite-db.ts` / `migration-sql.ts`：双方言适配。**本地测试只跑 SQLite**，MySQL 专有 SQL（upsert 等）必须走 `dialect === "mysql"` 分支，参考 `rateLimitUpsertSql`、`speechUsageUpsertSql`。
- 迁移：`migrations/`（SQLite，到 0033）与 `migrations-mysql/`（生产，到 0015）成对新增；服务启动时自动应用未执行的迁移。
- 不变量：陪伴关系是**单向**的（`relation` 表示“对方是我的 ___”），不要反转；手动记忆优先于历史回复；用户内容进提示词前 Base64 封装；同一对象的聊天请求串行、不同对象并行，语音另有数据库级处理租约。
- 测试：`backend/tests/`，6 个文件 53 项，`npm test` 会先 `tsc` 再构建再跑。

### 制作端（`tools/live2d-worker/`，Python 3.12）

- `live2d_pipeline.py`：CLI（generate / plan / remote-decompose / build）。
- `scripts/auto_build.py`：一次构建的编排（生成 → 规划 → 拆层 → 表情 → 精修 → 绑定 → 校验 → 上传）。
- `scripts/anyi_worker.py`：领取 → 心跳 → 构建 → 上传 → 完成/失败。
- `scripts/foreground.py`：规则恢复层（背景中性灰化、拆层后按深度图与遮罩裁掉并入的背景、动作幅度回退）。
- `scripts/supervisor.py`：大模型监督层，模式 `off / shadow / act`，线上目前 `shadow`。
- 测试：`tests/`，34 项。用生产共用的 `~/Library/Application Support/AnyiLive2D/venv` 运行。

## 5. 发布流程（2026-09-17 起每次都这么做，已验证可复现）

一次完整发布分四段，按需取用；每段之前都要用户授权。

**后端**

1. 本机 `git archive <sha> backend | gzip` 打包，scp 到服务器 `/tmp`。
2. 服务器解压到 `/opt/anyi-releases/main-<日期>-<sha>`，把 `/opt/anyiapp2/backend/node_modules` 软链进去，用同一份 `node_modules/.bin/tsc -p tsconfig.node.json` 构建。对比 `dist-node` 下 JS 文件的合并哈希与本机 `npm run server:build` 结果，必须一致。
3. `sudo systemctl start anyi-mysql-backup.service` 做发布前备份。
4. 复制当前目录到 `/opt/anyi-releases/before-main-<日期>-<sha>` 作回滚副本，旧 `dist-node` 另存为 `dist-node.pre-main-<日期>-<sha>`。
5. 替换 `dist-node`、`src`、`server`、`tests`、`scripts`、`examples`、`package.json` 等；**保留** `.env`、`node_modules`、`migrations-mysql` 之外的运行数据。更新 `RELEASE.json`。
6. 需要新环境变量时追加到 `.env`（同时确认 `server/server.ts` 白名单已含该键）。
7. `sudo systemctl restart anyi-memorial-api`（停机约 1 秒），然后核验：三项服务 active、`/health`、`_node_migrations` 行数、journal 无错误、Nginx 状态码正常。

**Mac 制作端**（只有 `tools/live2d-worker/` 变了才需要）

```bash
git archive <sha> tools/live2d-worker | tar -x --strip-components=2 -C "$HOME/Library/Application Support/AnyiLive2D/releases/<sha>"
```

用共用 venv 在新目录跑一遍 `unittest`，改 `~/Library/LaunchAgents/cn.anyibj.live2d-worker.plist` 里的目录，`launchctl bootout gui/$UID/cn.anyibj.live2d-worker && launchctl bootstrap gui/$UID ~/Library/LaunchAgents/cn.anyibj.live2d-worker.plist`，确认 `/internal/live2d/jobs/claim` 每 10 秒返回 200。切换时确认线上没有 running 任务。

**Android 安装包**

1. 从 `main` 开 `release/<版本>` 分支，只改 `app/build.gradle.kts` 的 `versionCode` / `versionName`，走 PR。
2. 导出 `JAVA_HOME`、`ANDROID_HOME` 与四个 `ANYI_RELEASE_*` 变量（密码从钥匙串读，不打印），`./gradlew :app:assembleRelease :app:bundleRelease`。
3. `apksigner verify --print-certs` 确认证书 SHA-256 以 `40922d05` 开头、`c85f` 结尾；`aapt2 dump badging` 确认版本号。
4. 产物复制到本机 `dist/android/`（不入库），APK scp 到服务器，`sudo install -o www-data -g www-data -m 644` 到 `/var/www/anyi-downloads/anyi-memorial-<版本>-release-v2.apk`，`sudo ln -sfn` 切换 `anyi-memorial-latest.apk` 与 `anyi-memorial-release-latest.apk`，公网回读哈希。

**记录**

在 `docs/releases/` 写一份记录，把 `CHANGELOG.md` 的 `未发布` 归档，更新线上 `RELEASE.json`。

回滚方式每份发布记录里都有；最简单的是把 `dist-node` 换回 `dist-node.pre-*` 再重启。迁移都是加列加表，回滚代码时不需要回退迁移。

## 6. 已知问题与遗留（按紧急程度）

1. **Nginx 制作端上传配置未生效。** `/etc/nginx/sites-enabled/anyi-api` 是 2026-08-31 的独立文件，不是指向 `sites-available` 的链接；2026-09-17 更新的 `sites-available/anyi-api`（与仓库 `backend/examples/tencent-nginx-node-api.conf` 逐字节一致，含 `/internal/live2d/` 的 `client_max_body_size 256m` 与 300 秒超时）没有进入生效配置，`nginx -T` 里没有 `internal/live2d`。当前制作端上传实际受通用的 60 MB / 120 秒限制。至今最大的 `project.zip` 约 16 MB，尚未触发 413。修法：`sudo ln -sf /etc/nginx/sites-available/anyi-api /etc/nginx/sites-enabled/anyi-api && sudo nginx -t && sudo systemctl reload nginx`，需要用户授权。
2. **`anyi.anyibj.cn` 没有 DNS 记录**（NXDOMAIN）。官网 `robots.txt` 与 `sitemap.xml` 仍指向它；CVM 上 `/var/www/anyi-memorial-site/` 与 `anyi-site` vhost 是 2026-08-30 的旧版且不可访问。待决定：加回解析、改成 `pages.dev`，或绑自定义域名。
3. **服务器杂物。** `/opt/anyiapp2/` 顶层有 2026-05 的截图、`resume_pdf_page1.png`、`edge_test.png` 和一个 1.0.7 的 APK；`/var/www/anyi-downloads/` 1.1 GB，保留了 2026-07 起的二十多个旧包（1.0.18 之前的都已不可回滚）；`/var/log/journal` 约 1 GB，可设 `SystemMaxUse=200M`；`open-llm-vtuber.service` 单元文件与 `anyi_proxy` 账号是退役项目残留。都需要用户授权后再清理。
4. **官网截图是 2026-08 的**，没有动态形象与语音界面；本机没有连模拟器或真机，需要重拍四张 1080×2424 截图。
5. **口型是播放包络驱动，不是真频谱。** `MediaPlayer` 拿不到实时音量；要做真频谱得把音频搬进 WebView 用 Web Audio 播。
6. **AI 回复不是流式的**，语音在整条回复到达后才开始。视频通话链路需要流式 LLM/TTS，届时一并处理。
7. **监督器仍是 `shadow` 模式**，只记录不动手。跑过一批真实任务确认误判率后再切 `act`（`LIVE2D_SUPERVISOR_MODE=act`）。
8. **腾讯云 TTS 免费包**约 2 万字符、3 个月（2026-09-18 起）。用尽后返回 `UnsupportedOperation.PkgExhausted`，App 自动回落纯文字。续费买“超自然大模型音色”付费包，或把白名单换成“大模型音色”系列。
9. 远程礼祭、护符商城与支付只有数据模型，`PAYMENT_ENABLED=false`。
10. Live2D 生成对任意照片的成功率未系统统计；`.cmo3` 在 Cubism Editor 里另存/重开未完整验收。

## 7. 视频通话：下一步的实施顺序

这是与用户约定的首版范围与顺序，未开工：

1. 用户选择已有陪伴对象和一张已审核照片；服务端创建一次性会话，Android 只拿短期令牌。
2. 首版只开麦克风，不要求前置摄像头。
3. 形象待机有轻微动作，说话时口型同步并显示字幕；支持静音、挂断、插话打断、弱网退回语音/文字聊天。
4. 页面固定显示“AI 生成形象”；用户可删除照片、声音与会话资料。

做法上：先选定并验证一家实时数字人供应商（数据地域、是否支持单张照片、并发与分钟数限制），不要把供应商写死在 Android；后端加供应商无关的会话接口；把现有陪伴资料与手动记忆转成会话系统上下文，保留单向关系与手动事实优先；先做可打断的实时语音会话，再接画面。先做 30–60 秒样机测“停止说话到首次出声”的延迟、口型、断线恢复、单分钟成本与并发上限，再决定套餐。当前 CVM 没有 GPU，不能自托管实时渲染。

## 8. 验证命令

后端（Node 20；本机 Node 不是 20 时先 `npm rebuild better-sqlite3`）：

```bash
cd backend && npm ci && npm test
```

Android：

```bash
export JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21-aarch64-os_x.2/jdk-21.0.7+6/Contents/Home ANDROID_HOME=~/dev-tools/android-sdk && ./gradlew :app:testDebugUnitTest
```

制作端：

```bash
cd tools/live2d-worker && "$HOME/Library/Application Support/AnyiLive2D/venv/bin/python" -m unittest discover -s tests
```

生产只读核验（SSH 密钥路径只记录在维护者本机，不写进仓库）：

```bash
ssh -i "$ANYI_SSH_KEY" -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes -o UserKnownHostsFile="$ANYI_KNOWN_HOSTS" ubuntu@api.anyibj.cn 'systemctl is-active anyi-memorial-api nginx mysql; curl -fsS http://127.0.0.1:8787/health; cat /opt/anyiapp2/backend/RELEASE.json'
```

```bash
curl -fsS https://api.anyibj.cn/health && curl -fsS https://api.anyibj.cn/app/config
```

服务器登录、备份、SSH 安全与救援见 [腾讯云运维](tencent-cloud-handover.md)。
