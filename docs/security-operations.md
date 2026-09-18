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
- AI 人物记忆：按用户和人物隔离，用户手动新增、查看和删除；敏感信息过滤始终启用，自动整理新记忆默认关闭。
- 语音合成：开启 `TTS_ENABLED` 后，绑定动态形象的对象会把 AI 回复合成语音。音频是含回复原文的私有资产，只对该用户可读；同句按哈希复用缓存，默认保留 30 天后自动回收，被消息引用的音频不删；账号注销与对象删除随现有资产删除队列清理。
- Live2D 生成质检：制作端在规划、拆层、表情和收尾关卡把输入图及其派生图层的 ≤ 512 px 缩略图发给现有图像供应商做自动质检，范围不超过生成本身已发送的图片；失败包不含路径、密钥和供应商原文，审查记录只留在制作主机任务目录，线上只保存白名单诊断码、建议和 ≤ 200 字摘要。

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

用于处理账号注销、上传审核拒绝或隔离产生的本地文件删除任务。有两种方式：

1. 管理后台页面：用管理员账号登录后台，在资产删除队列区域点击“处理待删除”按钮。
2. 命令行：用管理员账号登录取得当前 Bearer token，直接调用接口：

```bash
export ANYI_ADMIN_TOKEN='your-admin-token'
curl -fsS -X POST \
  -H "Authorization: Bearer $ANYI_ADMIN_TOKEN" \
  -H "Content-Type: application/json" -d '{}' \
  https://api.anyibj.cn/admin/asset-delete-queue/process
```

接口为 `POST /admin/asset-delete-queue/process`，返回本次尝试处理（`attempted`）与实际删除（`deleted`）的条数。

## 上线前建议

- 把 `ALLOWED_ORIGINS` 改成正式域名。
- 保持 `LEGAL_CONTACT_EMAIL` 和 `LEGAL_CONTACT_PHONE` 为真实客服信息。
- 支付回调必须接入真实微信支付或支付宝官方验签。
- 后续接入图片和语音内容安全服务，把 `upload_reviews` 从人工待审升级为自动审核加人工复核。
- AI 上线前应验证 Apexin 密钥只存在服务器环境、GPT/Gemini 图片适配器不能混用，并定期检查人物记忆准确性和跨账号隔离；相关表随 MySQL 备份一起恢复。
- 管理后台建议继续强化：权限分级、登录失败锁定、双因素验证、操作导出。
