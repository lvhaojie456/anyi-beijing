# 2026-09-18 合手图层修复与安忆 1.0.20 发布记录

## 范围

- 代码版本：`main` `d1611fa`（PR [#11](https://github.com/lvhaojie456/anyi-beijing/pull/11)）。含 PR #10 的 1.0.19 版本号与发布记录。
- Android：`1.0.20`，versionCode `22`，在 `release/1.0.20` 分支于 `d1611fa` 之上仅提升版本号后构建；沿用 release-v2 签名。
- 后端：`/opt/anyiapp2/backend` 切换到 `d1611fa`，无新迁移（`0014` 已于 09-18 01:17 应用）。
- Mac 制作端：`~/Library/Application Support/AnyiLive2D/releases/d1611fa`，launchd 已切换；监督器仍为默认 `shadow`。
- 环境变量与密钥：无变更。

## 修复内容

提示词"一个真实的慈祥的老奶奶"生成的写实立绘双手交握，See-through 只给出一个合并的 `handwear` 图层，绑定因缺少 `arm-l`/`arm-r`/`hand-l`/`hand-r` 失败。`make_recipe` 现在对单个 `handwear` 先按画布中线分左右、再按手腕比例切出手和手臂；同时监督器单次调用超时 60 → 90 秒、失败重试一次、缩略图 512 → 384 px（此前拆层审查在 60.2 秒 `APITimeoutError`）。

## 验证

- 制作端 `unittest discover` 34 项通过（新增 2 项）。
- 失败任务产物回放（复用规划、拆层 PSD 与表情）：一次通过，四个手臂/手图层生成，脚底位移 0.0003 px。
- 同一提示词全新生成（影子模式，真实调用 Astra）：生成立绘、规划、4090 拆层、表情、精修 31 层、绑定与校验全部通过，脚底位移 0.0002 px，视觉评分 0.83，三次审查 7.6 / 14.0 / 14.3 秒，未触发背景裁剪。

## 后端（11:58–12:00 CST）

1. 备份：`anyi-mysql-anyi_memorial-20260918-115836.sql.gz`、`anyi-uploads-20260918-115836.tar.gz`。
2. 服务器构建哈希 `c09e0744c7ed`，与本机 `npm run server:build` 一致；迁移目录无差异。
3. 暂存 `/opt/anyi-releases/main-20260918-d1611fa`；回滚副本 `/opt/anyi-releases/before-main-20260918-d1611fa` 与 `/opt/anyiapp2/backend/dist-node.pre-main-20260918-d1611fa`。
4. 重启后健康检查正常，重启后 1 分钟 journal 无错误，迁移仍为 14 行。

## Mac 制作端（12:00 CST）

- `releases/d1611fa` 与仓库逐字节一致，生产 venv 下 34 项单测通过；plist 备份 `.bak-16cfa9c` 后切换，新进程 pid 70948 正常领取轮询，`worker.err` 无新增。

## Android 1.0.20

- APK SHA-256：`5761c02788160951abe2ad6fd3fde2b30fef553b432322c294f8d655956e47cc`（33,823,458 字节）；AAB SHA-256：`$(shasum -a 256 anyi-memorial-1.0.20-release-v2.aab | cut -c1-64)`。
- 签名证书 SHA-256 `40922d059fc1cc448554703e659faa2a1f27c5d24fb5ee652726f0993396c85f`，与 1.0.17 / 1.0.18 / 1.0.19 相同，可覆盖升级。
- 上传为 `/var/www/anyi-downloads/anyi-memorial-1.0.20-release-v2.apk`，两个 latest 链接切换；三个公网地址回读哈希与本机一致。AAB 未上传，与 APK 一并保留在仓库根目录；1.0.19 与 1.0.18 的包仍在服务器上供回滚。

## 回滚

后端：

```bash
cd /opt/anyiapp2/backend
mv dist-node dist-node.main-d1611fa && cp -a dist-node.pre-main-20260918-d1611fa dist-node
sudo systemctl restart anyi-memorial-api && curl -fsS http://127.0.0.1:8787/health
```

制作端：plist 恢复为 `.bak-16cfa9c` 后 `launchctl bootout gui/$UID/cn.anyibj.live2d-worker && launchctl bootstrap gui/$UID ~/Library/LaunchAgents/cn.anyibj.live2d-worker.plist`。

安装包：`sudo ln -sfn /var/www/anyi-downloads/anyi-memorial-1.0.19-release-v2.apk /var/www/anyi-downloads/anyi-memorial-latest.apk`（release-latest 同理）。
