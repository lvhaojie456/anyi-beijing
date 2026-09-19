# 安忆 Web 管理后台

管理后台独立运行在腾讯云后端服务中，入口是：

```text
https://api.anyibj.cn/admin
```

实现位置：后端 `backend/src/admin/`（`routes.ts` 注册接口，`page.ts` 页面外壳，`ui-styles.ts` / `ui-script.ts` 样式与前端脚本）。页面本身不含数据，接口全部挂在 `/admin/*` 下。

## 登录

使用已由运维人员手动提升为管理员的账号登录。公开注册永远只创建普通用户，注册名为 `admin` 也不会获得管理员权限。

在后端服务器上执行 `npm run admin:promote -- USERNAME` 可提升一个已经注册的账号。该命令只修改数据库角色，不接受或保存密码。

登录凭证默认只保存在当前标签页的 `sessionStorage`，勾选「在这台电脑记住登录」后才写入 `localStorage`；令牌过期或权限被撤销时，页面退回登录页并要求重新登录。

## 页面结构

左侧是队列导航，数字是待处理条数，归零即代表该队列清空。右侧是「列表 + 详情」：点一行在右侧处理，处理完自动跳到下一条。

分组：

- **队列**：上传审核、社区内容、举报处理、义工报名、注销申请、文件删除。
- **记录**：用户、义工招募、动态形象、崩溃日志、审计日志。
- **总览**：所有队列的待处理量、近 14 天活跃度、服务开关与最近操作。

各队列的作用：

- **上传审核**：图片、音频、文本素材。详情里能直接看图和听音频，并显示上传用途（个人头像、陪伴对象头像、社区动态图片、语音消息等）。通过后头像类素材会公开；拒绝或隔离会把文件设为私有、解除头像或语音引用并加入文件删除队列。列表支持多选批量通过或拒绝。
- **社区内容**：动态和评论分开标记。动态里的图片必须先在上传审核通过，否则「通过」会提示先审图片。
- **举报处理**：详情里直接显示被举报的内容和作者，不需要再去搜。保留或移除只作用于内容，屏蔽或封禁只作用于作者账号。
- **义工报名**：通过或拒绝报名表，处理时可以看到账号、电话和备注。
- **注销申请**：更新办理进度；核实账号归属后可在同一页注销账号，注销会自动把申请标记为「已完成」。
- **文件删除**：处理待删除文件（每次最多 50 条）。仍被头像、背景、语音或形象引用的文件会标记为失败并保留，解除引用后可在详情里重新入队。
- **用户**：按用户名或昵称搜索，可按「处置中」「管理员」筛选；详情里给出纪念馆、动态、评论、陪伴对象、上传、举报等计数，以及账号处置入口（屏蔽可设 1/7/30 天或手动解除，封禁不自动解除）。
- **义工招募**：发布、截止、重新开放招募，并直接看到每条招募的待审报名数。封面图以当前管理员身份上传并直接通过审核。
- **动态形象**：只读查看 Live2D 生成任务的阶段、进度、尝试次数、诊断码和制作端摘要。取消和重试由用户在 App 内操作。
- **崩溃日志**：按异常类型筛选，展开可见堆栈。
- **审计日志**：按操作类型或操作者筛选，展开可见结构化元数据。

## 键盘操作

- `J` / `K`：下一条 / 上一条。
- `A`：通过（举报页是「保留内容」）。
- `X`：拒绝（举报页是「移除内容」）。
- `Q`：隔离或屏蔽（上传审核、社区内容）。
- `/`：跳到顶部搜索框。
- `Esc`：关闭详情。

拒绝、隔离、屏蔽、封禁、移除、注销都必须填写原因，原因会写入 `audit_logs`。

## 接口清单

页面使用的接口都在 `/admin/*`，全部要求管理员令牌：

- `GET /admin`：后台页面（`/admin/app.js`、`/admin/app.css` 为样式与脚本，页面带严格 CSP）。
- `GET /admin/overview`：队列计数、近 14 天活跃度、服务能力、最近操作。
- `GET /admin/upload-reviews`、`PATCH /admin/upload-reviews/:id`、`POST /admin/upload-reviews/batch`。
- `GET /admin/community/moderation`、`PATCH /admin/community/posts/:id`、`PATCH /admin/community/comments/:id`。
- `GET /admin/community/reports`、`PATCH /admin/community/reports/:id`。
- `GET /admin/users`、`GET /admin/users/:id`、`GET /admin/users/moderation`、`PATCH /admin/users/:id/moderation`、`DELETE /admin/users/:id`。
- `GET /admin/volunteer/posts`（发布与截止复用 `/community/volunteer`）。
- `GET /admin/live2d/jobs`。
- `GET /admin/crash-reports`、`GET /admin/audit-logs`。
- `GET /admin/asset-delete-queue`、`POST /admin/asset-delete-queue/process`、`POST /admin/asset-delete-queue/:id/retry`。
- `GET /admin/account-deletion-requests`、`PATCH /admin/account-deletion-requests/:id`。

## 删除账号

`DELETE /admin/users/:id` 需要 `confirmUsername` 与账号用户名完全一致（不区分大小写），可选 `reason` 和 `deletionRequestId`。执行后立即永久删除该用户的纪念馆、社区内容、陪伴对象、聊天记录、记忆、报名和账号行，把其上传文件写入删除队列，并把 `audit_logs` 里的 `actor_id` 置空。管理员账号和当前登录账号不能被注销。

## 后续建议

- 正式上线前改成独立后台域名，例如 `https://admin.your-domain.com`。
- 做更细的管理员权限分级，例如客服、审核员、财务、超级管理员。
- 给后台加双因素验证和登录失败锁定。
- 后台操作继续写入 `audit_logs`，保留追责能力。
