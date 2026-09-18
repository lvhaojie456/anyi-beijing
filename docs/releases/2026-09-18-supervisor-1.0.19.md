# 2026-09-18 Live2D 监督上线与安忆 1.0.19 发布记录

## 范围

- 代码版本：`main` `16cfa9c04324e1c98787702a63cde83f31fc4ca9`（PR [#8](https://github.com/lvhaojie456/anyi-beijing/pull/8) 方案文档、PR [#9](https://github.com/lvhaojie456/anyi-beijing/pull/9) 规则恢复 + 模型监督 + 诊断透传）。上一版线上为 `7f8938f`。
- Android：`1.0.19`，versionCode `21`，包名 `com.anyi.memorial`，在 `release/1.0.19` 分支于 `16cfa9c` 之上仅提升版本号后构建；沿用 release-v2 签名。
- 后端：`/opt/anyiapp2/backend` 切换到 `16cfa9c`，启动时自动应用迁移 `0014_live2d_diagnosis.sql`。
- Mac 制作端：`~/Library/Application Support/AnyiLive2D/releases/16cfa9c`，launchd 已切换；`LIVE2D_SUPERVISOR_MODE` 未设置，按默认 `shadow` 运行；`IMAGE_BACKGROUND` 未设置，按默认 `transparent`。
- 环境变量与密钥：无变更。

## 后端（01:16–01:19 CST）

1. 备份：`anyi-mysql-backup.service` → `/home/ubuntu/anyi-db-backups/anyi-mysql-anyi_memorial-20260918-011620.sql.gz`、`anyi-uploads-20260918-011620.tar.gz`。
2. 在服务器用同一 `tsc` 构建 `16cfa9c`，`dist-node` JS 哈希 `c09e0744c7ed`，与本机 `npm run server:build` 一致。迁移目录相对线上只新增 `0014_live2d_diagnosis.sql`。
3. 暂存 `/opt/anyi-releases/main-20260918-16cfa9c`；回滚副本 `/opt/anyi-releases/before-main-20260918-16cfa9c`（旧 `dist-node`、源码、两套迁移目录、`RELEASE.json`、`.env` 副本，700 权限）；旧编译目录 `/opt/anyiapp2/backend/dist-node.pre-main-20260918-16cfa9c`。
4. 切换 `dist-node`、`src`、`server`、`tests`、`scripts`、`examples`、`migrations-mysql`、`migrations` 与配置文件，更新 `RELEASE.json`；`systemctl restart anyi-memorial-api`，停机约 1 秒。
5. 验证：三项服务 active；本地与公网 `/health` ok；`_node_migrations` 由 13 行变为 14 行，最新 `0014_live2d_diagnosis.sql`；`live2d_jobs` 新列 `diagnosis_code`、`suggestion`、`retry_hint`、`supervisor_summary` 存在；重启后 3 分钟 journal 无错误，Nginx 请求全部 200；`/ai/live2d/config`、`/admin/asset-delete-queue`、`/memorials` 无凭据访问 401。切换时任务表只有 1 个 failed 与 1 个 cancelled，没有 running。

## Mac 制作端（01:17 CST）

- 从 `16cfa9c` 用 `git archive` 生成 `releases/16cfa9c`，与仓库逐字节一致；用生产 venv 跑 `unittest discover` 32 项通过；`requirements.txt` 无变化。
- plist 备份为 `cn.anyibj.live2d-worker.plist.bak-7f8938f`，路径改为新目录后 `launchctl bootout` / `bootstrap`。新进程 pid 57983 由 `caffeinate -i` 托管，领取轮询每 10 秒返回 200，`worker.err` 无新增；后端重启瞬间记录 1 条 `URLError`，随后自动恢复。

## Android 1.0.19

- 构建：`./gradlew :app:assembleRelease :app:bundleRelease`，签名口令来自 macOS 钥匙串，密钥库 `anyi-release-v2.jks`。
- APK SHA-256：`6ed6ae4e5a6f00bc8a40620e306c0f87ae7862303619a6874af0c44b87949528`（33,823,462 字节）；AAB SHA-256：`59af1418748a462c28de162a910379622945d8b46844843f94200b9cfce496a2`。APK 比 1.0.18 小约 1.4 MB，来自 PR #5 删除的榴莲图片。
- 签名证书 SHA-256：`40922d059fc1cc448554703e659faa2a1f27c5d24fb5ee652726f0993396c85f`，与线上 1.0.17 / 1.0.18 相同，可覆盖升级；APK 使用 v2 签名方案。
- 发布：上传为 `/var/www/anyi-downloads/anyi-memorial-1.0.19-release-v2.apk`（www-data，644），`anyi-memorial-latest.apk` 与 `anyi-memorial-release-latest.apk` 两个链接切到新文件；三个公网地址回读 SHA-256 均与本机一致。`RELEASE.json` 记为 1.0.19 / 21。
- AAB 未上传服务器，与 APK 一起保留在仓库根目录（`anyi-memorial-1.0.19-release-v2.*`，未纳入版本库）。本次不涉及商店上架。
- 1.0.18 的 APK 仍保留在下载目录，供回滚。

## 回滚

后端：

```bash
cd /opt/anyiapp2/backend
mv dist-node dist-node.main-16cfa9c && cp -a dist-node.pre-main-20260918-16cfa9c dist-node
sudo systemctl restart anyi-memorial-api && curl -fsS http://127.0.0.1:8787/health
```

迁移 0014 为纯加列可空，回滚代码时可以保留新列；确需删除时执行 `ALTER TABLE live2d_jobs DROP COLUMN diagnosis_code, DROP COLUMN suggestion, DROP COLUMN retry_hint, DROP COLUMN supervisor_summary`，并从 `_node_migrations` 删除该行。源码与 `RELEASE.json` 可从 `/opt/anyi-releases/before-main-20260918-16cfa9c/backend` 复制回来。

制作端：把 plist 恢复为 `.bak-7f8938f` 副本后 `launchctl bootout gui/$UID/cn.anyibj.live2d-worker && launchctl bootstrap gui/$UID ~/Library/LaunchAgents/cn.anyibj.live2d-worker.plist`。

安装包：`sudo ln -sfn /var/www/anyi-downloads/anyi-memorial-1.0.18-release-v2.apk /var/www/anyi-downloads/anyi-memorial-latest.apk`（`anyi-memorial-release-latest.apk` 同理）。已安装 1.0.19 的设备不能直接覆盖降级。

## 说明

- 监督器默认影子模式：会调用模型并把审查记录留在任务目录 `supervisor/`，但只执行规则层动作。建议先跑一批任务，对照记录后再决定是否切 `act`。
- 线上任务 `5462ebc4` 现在可以在 App 里重新生成：制作端会复用其规划、拆层 PSD 与表情图，自动裁掉并入上衣的背景，本机回放已通过校验。
