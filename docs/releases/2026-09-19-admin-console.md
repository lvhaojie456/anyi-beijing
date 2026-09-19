# 2026-09-19 管理后台重做上线记录（main f2a5c1e）

## 范围

- 代码版本：`main` `f2a5c1e7014b62dbd993a9f8b5fa8d99def6747b`（PR [#21](https://github.com/lvhaojie456/anyi-beijing/pull/21) 管理后台重做；同批合并的还有 [#19](https://github.com/lvhaojie456/anyi-beijing/pull/19) 沉浸模式语音输入与形象命名、[#20](https://github.com/lvhaojie456/anyi-beijing/pull/20) 形象本地缓存）。上一版线上代码为 `fc04951`。
- 后端：`/opt/anyiapp2/backend` 的源码与 `dist-node` 切到 `f2a5c1e`。新增 `backend/src/admin/`（页面外壳、样式、前端脚本）、`backend/src/errors.ts`，`/admin/*` 接口全部迁入 `backend/src/admin/routes.ts`。`.env`、`node_modules`、Nginx 均未改动。
- 数据库：`fc04951 → f2a5c1e` 只带来一个迁移 `0016_live2d_job_name.sql`（`live2d_jobs` 加可空列 `name varchar(40)`，来自 PR #19），服务启动时自动应用，`_node_migrations` 15 → 16 行。管理后台自身无迁移。
- Android：无变化，线上仍是 1.0.21（versionCode 23）。
- Mac 制作端：无变化，仍在 `releases/d1611fa`。

## 后端部署步骤（17:19–17:21 CST）

1. 本机用 `git archive origin/main` 导出 `f2a5c1e` 并 `tsc -p tsconfig.node.json`，`dist-node` 的 JS 哈希 `bbcd5098f026`。
2. `scp` 后端源码包到服务器，在 `/opt/anyi-releases/main-20260919-f2a5c1e/app` 用线上 `node_modules`（tsc 5.9.3、Node 20.20.2）构建，哈希同为 `bbcd5098f026`，与本机一致。
3. 备份：`anyi-mysql-anyi_memorial-20260919-172020.sql.gz`、`anyi-uploads-20260919-172020.tar.gz`。
4. 回滚副本：`/opt/anyi-releases/before-main-20260919-f2a5c1e`（权限 700，含 `dist-node`、源码、`RELEASE.json`、`.env.bak`）与 `/opt/anyiapp2/backend/dist-node.pre-main-20260919-fc04951`。
5. 切换 `dist-node`（旧目录保留为 `dist-node.main-fc04951`）、`src`、`server`、`tests`、`scripts`、`migrations`、`migrations-mysql` 与 `package.json` 等文件，更新 `RELEASE.json`（`codeRevision: f2a5c1e`）。
6. `sudo systemctl restart anyi-memorial-api`，停机约 1 秒，启动日志正常连接 MySQL。

## 验证

- 本机：后端 `npm test` 66 项通过（含新增 `tests/admin-console.test.mjs` 10 项；PR 描述里写的 63 项是本分支单独的数字）。
- 线上服务：`anyi-memorial-api` / `nginx` / `mysql` 均 active，`/health` 返回 ok，重启后 10 分钟内 journal 无 error/exception。
- 页面：`https://api.anyibj.cn/admin` 200 且带严格 CSP（`default-src 'none'`、`script-src 'self'`）；`/admin/app.css`、`/admin/app.js` 200；无凭据访问 `/admin/overview` 401。
- 新接口（用临时管理员 `opscheck172211` 验收）：`/admin/overview` 200 且 `Cache-Control: no-store`（队列计数、14 天活跃度、服务能力 `mysql / speech=true / asr=true / live2d=true / gpt-5.6-luna`）；`/admin/users?q=` 命中；`/admin/users/:id` 计数正确；`/admin/upload-reviews` 返回带 `username`/`display_name` 的增强行；`/admin/live2d/jobs`、`/admin/volunteer/posts`、`/admin/account-deletion-requests` 均 200。
- 注销链路：临时管理员的同名账号被 `/admin/users/:id` 正确拒绝（403 `admin_account_deletion_forbidden`）；普通账号 `opsdel172343` 走完整流程 —— 确认名不匹配 400 `confirm_username_mismatch`、以管理员用户名确认同样 400、正确确认后 200，注销申请自动转 `completed`，账号从用户目录消失、旧令牌 401，上传文件进入删除队列。
- 清理：两个验收账号（`opscheck172211`、`opsdel172343`）及其上传文件已删除并写入删除队列，账号行与资产行均已清空。
- 磁盘：`/` 使用 63%。

## 回滚

```bash
cd /opt/anyiapp2/backend
mv dist-node dist-node.main-f2a5c1e && cp -a dist-node.pre-main-20260919-fc04951 dist-node
sudo systemctl restart anyi-memorial-api && curl -fsS http://127.0.0.1:8787/health
```

源码与 `RELEASE.json` 可从 `/opt/anyi-releases/before-main-20260919-f2a5c1e/` 复制回来；`.env` 未改动（备份在 `.env.bak`，无需恢复）；迁移 `0016` 为纯加列，可保留。

## 待办与已知问题

- 删除队列有 50 条 pending：49 条是历史账号注销留下的 `account_deleted` 文件，1 条 `ai_chat_background_replaced`、1 条 `ai_list_background_replaced`。本次未处理，需要时在后台「文件删除」点「处理待删除文件」。
- 生产 `live2d_jobs` 只有 3 条（2 succeeded、1 cancelled），且后台「动态形象」页暂不显示 PR #19 新增的形象名字 `name` 字段（列为空且旧任务无名），需要时再补。
- Nginx `sites-enabled/anyi-api` 仍是 2026-08-31 的独立文件，2026-09-17 那份 Live2D 256 MB 上传配置仍未生效（2026-09-19 已确认，未授权修改）。
