# 安忆安全与运营说明

## 已实现

- 接口限流：Node 服务使用数据库里的 `rate_limits` 表按 IP 和接口类别限流。
- CORS 限制：浏览器请求只允许 `ALLOWED_ORIGINS` 中的域名；Android App 不受浏览器 CORS 影响。
- 日志审计：管理员订单操作、验收上传、上传审核、文件删除队列处理会写入 `audit_logs`。
- 管理员操作记录：`GET /admin/audit-logs` 可查看最近 200 条。
- 上传内容审核：所有上传都会写入 `upload_reviews`；文本做关键词初筛，图片和音频进入人工待审。
- 文件删除策略：账号注销或审核拒绝会写入 `asset_delete_queue`，管理员可统一处理本地上传文件删除。
- 崩溃日志：App 未捕获异常会上报 `/crash-reports`，管理员可查 `/admin/crash-reports`。
- 账号注销申请：网页表单写入 `account_deletion_requests`，管理员可查 `/admin/account-deletion-requests`。
- AI 长期记忆：默认关闭，按用户和陪伴角色隔离；仅保存经过敏感信息过滤的候选事实，并提供单条删除和全部清空。

## 数据备份

生产数据在腾讯云服务器：

```text
MySQL: 127.0.0.1:3306/anyi_memorial
/var/lib/anyi-memorial-api/uploads
```

服务器使用 `anyi-mysql-backup.timer` 每天自动备份 MySQL dump 和 uploads：

```bash
sudo systemctl list-timers anyi-mysql-backup.timer --no-pager
sudo systemctl start anyi-mysql-backup.service
```

备份目录默认是 `/home/ubuntu/anyi-db-backups`，保留 14 天。备份文件建议同步到腾讯云 COS 或另一台服务器。备份目录不要提交到代码仓库。

## 处理文件删除队列

先用管理员账号登录后台，取得当前 Bearer token，设置环境变量：

```powershell
$env:ANYI_ADMIN_TOKEN='your-admin-token'
cd D:\Desktop\anyiapp2\backend
npm run asset:delete:process
```

这会调用：

```text
POST /admin/asset-delete-queue/process
```

用于处理账号注销、上传审核拒绝或隔离产生的本地文件删除任务。

## 上线前建议

- 把 `ALLOWED_ORIGINS` 改成正式域名。
- 保持 `LEGAL_CONTACT_EMAIL` 和 `LEGAL_CONTACT_PHONE` 为真实客服信息。
- 支付回调必须接入真实微信支付或支付宝官方验签。
- 后续接入图片和语音内容安全服务，把 `upload_reviews` 从人工待审升级为自动审核加人工复核。
- AI 记忆上线前应定期检查提取准确率、误记忆、忘记指令和跨账号隔离；记忆表随 MySQL 备份一起恢复。
- 管理后台建议继续强化：权限分级、登录失败锁定、双因素验证、操作导出。
