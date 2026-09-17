# Live2D 生成监督方案：规则恢复 + 大模型诊断与处置

状态：方案，未实施。日期：2026-09-18。对应讨论：线上任务 `5462ebc4` 两次失败与校验清单。

## 1. 目标与边界

目标：

- 校验失败时不再直接判死，先按规则自动恢复；规则无法覆盖时由大模型看图诊断，在固定菜单里选下一步。
- 用户在 App 看到的是原因和建议，而不是一句“生成失败”。
- 所有自动动作有预算、有日志、可回放。

不做：

- 大模型不写代码、不执行任意命令、不改流水线参数范围以外的东西。
- 不改变现有校验的判定标准（脚底位移 0.25 px、翻转三角形为 0 等）。校验是底线，监督只决定“失败后怎么办”。
- 第一期不做视觉美观评分，只做“能不能过”和“为什么不过”。

## 2. 现状与失败链路

流水线 `auto_build.run` 是线性脚本：生成 5% → 规划 15% → 拆层 25% → 表情 55% → 精修 70% → 绑定 80% → 校验 90% → 上传 96%。任何异常都让子进程退出，`anyi_worker.process()` 回传 `fail`，后端固定写 `generation_failed`。用户重试时制作端复用最近一次尝试的规划、拆层 PSD 与表情图，所以确定性的失败会原样重现。

2026-09-17 的三次失败：

| 失败点 | 根因 | 现状 |
|---|---|---|
| 规划阶段流被对端关闭 | 传输 | 已加 `ASTRA_STREAM_RETRIES` |
| 精修阶段嘴部测量撑满搜索框 | 写实风格暗部阈值 | 已改 145/120/100 回退 |
| 校验阶段脚底位移 0.32 px | 白底被 See-through 当作前景并入 `topwear`，呼吸变形器固定到画布底部 | 未解决 |

第三个问题的关键事实：4090 返回的 `input/src_img.png` 本身 alpha 就覆盖了 66.7% 的画布，说明白底在拆层第一步就被当成了主体。补救必须在制作端加前景遮罩，不能指望拆层模型。

## 3. 总体架构

三层，从便宜到贵，从确定到不确定：

```
校验失败
  │
  ├─ 第一层 规则恢复（制作端，无模型调用）
  │     传输错误重试；图层合理性检查 + 前景遮罩裁剪；动作幅度回退；嘴部阈值回退
  │
  ├─ 第二层 监督（制作端调用大模型，事件驱动）
  │     关卡审查：规划 / 拆层 / 表情 / 校验 四个关卡各审一次
  │     失败诊断：生成“失败包” → 模型输出 诊断码 + 菜单动作 + 一句中文说明
  │     预算：每单最多 重生成图片 1 次、重规划 2 次、重绑定 2 次、模型调用 8 次
  │
  └─ 第三层 用户确认（后端 + App）
        免费动作自动执行；付费动作（重新生成图片）把建议透给 App，用户点一下再执行
```

运行位置：第一、二层全部在 Mac 制作端 `tools/live2d-worker/`，后端只负责传递诊断码与重试提示，App 只负责展示与确认。线上不新增任何对供应商的调用。

## 4. 关卡与失败包

### 4.1 关卡审查（每单 4 次，缩略图 ≤ 512 px）

| 关卡 | 送给模型的材料 | 模型回答 | 不通过时 |
|---|---|---|---|
| 规划后 | 输入图缩略图 + Astra 给出的脸/眼/嘴矩形画在图上 | 矩形是否落在正确部位；若不是，给出修正矩形（图像像素坐标） | 用修正矩形覆盖 `regions`，不重新调用 Astra |
| 拆层后 | 图层表：每个图层的缩略图与 bbox/画布占比 | 是否有背景泄漏、明显错分（如手杖并入衣服）、缺失部位 | 背景泄漏 → 规则层的前景裁剪；错分 → 记录到诊断，不阻断 |
| 表情后 | 闭眼图、张嘴图的脸部裁剪与原图对照 | 是否闭上了眼、张开了嘴、有没有改变身份/风格 | 表情不合格 → 用更严格提示词重做一次表情（免费额度内） |
| 校验后 | 校验失败摘要（位移、翻转数、退化数）+ `neutral.png` + 待机 GIF 首末帧 | 选择：动作幅度回退 / 前景裁剪 / 放弃 | 按菜单执行 |

审查结果写入尝试目录 `supervisor/gate-<stage>.json`，不阻断成功路径；只有失败时才进入处置。

### 4.2 失败包（制作端在异常处组装）

```json
{
  "job_id": "…", "attempt": "attempt-31f3c65656a6", "stage": "verifying",
  "error_class": "CalledProcessError", "error_message": "feet max displacement=0.32; passed=false",
  "metrics": {"feetMaxDisplacementPixels": 0.324, "trianglesFlipped": 0, "degenerate": 0,
              "layer_canvas_share": {"topwear": 0.79, "legwear": 0.10}},
  "history": [{"stage": "planning", "action": "retry_stream", "result": "ok"}],
  "budget_left": {"regenerate_image": 1, "replan": 2, "rerig": 2, "model_calls": 5},
  "images": ["neutral.png", "layer-sheet.png"]
}
```

不放入失败包：本机路径、密钥、供应商原始响应全文、用户其它任务的任何信息。

### 4.3 决策输出（模型必须按此 JSON 回答，temperature 0）

```json
{
  "diagnosis_code": "background_leak",
  "action": "clip_background",
  "params": {},
  "explanation": "白色背景被并入了上衣图层，呼吸动作因此带动了脚部。先按人物轮廓裁掉背景再绑定。",
  "confidence": 0.9
}
```

`diagnosis_code` 与 `action` 都必须来自白名单，否则视为无效回答，按规则层默认动作处理。

## 5. 动作菜单与预算

| 动作 | 成本 | 需用户确认 | 制作端做什么 |
|---|---|---|---|
| `retry_stage` | 时间 | 否 | 同一尝试内重跑当前阶段（传输类错误） |
| `replan_with_hint` | 一次 Astra 调用（约 4–6 分钟） | 否 | 带上关卡反馈重新规划；作废 `layer_plan.json` 检查点 |
| `clip_background` | 无 | 否 | 用前景遮罩裁剪全部图层后重做精修与绑定；作废精修与绑定产物，保留拆层 PSD |
| `tune_motion` | 无 | 否 | 倾斜 3°→2°→1°，呼吸抬升 4 px→2 px，逐级回退后重绑定重校验 |
| `redo_expressions` | 两次图片编辑 | 否（额度 1 次） | 更严格提示词重做闭眼/张嘴 |
| `regenerate_image` | 一次图片生成 | 是 | 作废本单全部检查点，用新提示（如“纯灰色背景”）重新生成 |
| `give_up` | 无 | 否 | 回传失败 + 诊断码 + 建议 |

预算：每单 `regenerate_image` 1 次（且需确认）、`replan_with_hint` 2 次、`tune_motion` 2 次、`redo_expressions` 1 次、模型调用 8 次、总墙钟时间 60 分钟。超预算直接 `give_up`。

顺序：规则层先于模型层。同一失败先看规则表是否有确定动作（例如 `layer_canvas_share > 0.6` 直接 `clip_background`），有则不调模型；没有再调模型。

## 6. 规则层需要新增的能力（第一期，先做）

### 6.1 图层合理性检查与前景遮罩裁剪

- 检查：任一非背景图层的 bbox 面积占画布 > 60%，或 `topwear` 底边低于 `footwear` 底边，判定 `background_leak`。
- 遮罩来源，按优先级：
  1. 生成图自带透明通道。图片生成调用改为请求透明背景（gpt-image 系列支持 `background=transparent`，Apexin 的 `gpt-image-2.5-sunburst` 是否支持需实测），`prepare_input` 已经会把透明合成到白底送去拆层，同时保留原图，正好可作遮罩。
  2. 上传照片没有透明通道时，在 4090 上跑一次抠图（See-through 环境已有分割/深度模型，选型待定），得到 `foreground_mask.png` 随 `result.zip` 返回。
  3. 兜底：近白阈值 + 形态学开运算 + 最大连通域，仅用于白底图。
- 裁剪：拆层后把每个图层的 alpha 与遮罩相交，再进入精修；同时把遮罩写入精修包供人工核对。
- 回放验证：用 `5462ebc4` 的现有产物 dry-run，目标是脚底位移回到 0.25 px 以下（中午成功任务为 0.0002 px）。

### 6.2 动作幅度回退

`motion_recipe()` 增加 `scale` 参数；校验失败且失败项只有脚底位移或翻转三角形时，按 1.0 → 0.66 → 0.33 重绑定重校验，最多两次；成功后在 `validation.json` 记录 `motionScale`。

### 6.3 嘴部定位兜底

三档阈值都失败时，把嘴部裁剪图送模型要一个矩形，用该矩形重测一次；仍失败才报错。这是第二层的第一个落点，实现最小。

## 7. 后端与 App 变化

### 7.1 数据库

MySQL `0014_live2d_diagnosis.sql` / SQLite `0032_live2d_diagnosis.sql`，纯加列、可空：

```sql
ALTER TABLE live2d_jobs ADD COLUMN diagnosis_code VARCHAR(40) NULL;
ALTER TABLE live2d_jobs ADD COLUMN suggestion VARCHAR(40) NULL;
ALTER TABLE live2d_jobs ADD COLUMN retry_hint VARCHAR(40) NULL;
ALTER TABLE live2d_jobs ADD COLUMN supervisor_summary TEXT NULL;
```

回滚：`DROP COLUMN` 四列，客户端对缺失字段按无诊断处理。

### 7.2 接口

- `POST /internal/live2d/jobs/:id/fail`：接受 `{diagnosisCode, suggestion, summary}`，三者都过白名单/长度限制（summary ≤ 200 字，仅制作端生成的中文句子，不含供应商文本）。`error_code` 仍为 `generation_failed`，兼容旧制作端。
- `POST /ai/live2d/jobs/:id/retry`：接受可选 `{hint}`，白名单 `regenerate_image | clip_background | none`，写入 `retry_hint`。
- `POST /internal/live2d/jobs/claim`：响应增加 `retryHint`，制作端据此作废对应检查点。
- `GET …/live2d/jobs`：`serialize()` 增加 `diagnosisCode`、`suggestion`、`summary`。

白名单（首期）：

| diagnosis_code | 中文文案 | 默认 suggestion |
|---|---|---|
| `provider_unavailable` | 生成服务暂时不可用 | `retry` |
| `background_leak` | 背景被并入了人物，已尝试自动裁剪 | `regenerate_image` |
| `face_not_located` | 没有找到清晰的正面脸部 | `new_input` |
| `expression_failed` | 表情生成不合格 | `retry` |
| `rig_unstable` | 动作检查未通过 | `regenerate_image` |
| `budget_exhausted` | 自动修复次数已用完 | `new_input` |

### 7.3 App（`Live2dStudioScreen`）

- 失败卡片显示 `summary`（没有则沿用“生成失败”）。
- 按 `suggestion` 显示按钮：`retry` → “重新生成”；`regenerate_image` → “换背景重新生成（将再消耗一次生成）”，调用 retry 并带 `hint`；`new_input` → “换一张图/提示词”跳回提交。
- 不新增轮询；沿用 8 秒刷新。

## 8. 模型与调用

- 首期走现有 Apexin 网关，模型 `gpt-6-astra`（已能看图，无新供应商、无新密钥）。非流式、`max_tokens` 600、超时 60 秒、`temperature 0`、`response_format` JSON。
- 备选：Claude API（新增供应商与密钥，`worker.env` 加一项）。切换只影响 `supervisor/client.py` 一个文件。
- 开关：`LIVE2D_SUPERVISOR_MODE=off|shadow|act`，默认 `shadow`：调用模型、落盘决策，但只执行规则层动作。
- 成本与延时估算：每单 4 次关卡审查 + 失败时 ≤ 4 次诊断，每次 2–4 张 ≤ 512 px 缩略图，合计约 1–3 万 token，延时每次 10–30 秒。相对一次图片生成可以忽略。
- 今晚 Apexin 流式连接曾被中途关闭，审查调用统一用短的非流式请求，失败一次即视为 `provider_unavailable` 走规则默认动作，不重试模型本身超过 1 次。

## 9. 隐私与安全

- 送出的图片范围不超过现状：输入图已发给 Apexin 生成与规划，关卡审查用的是同一张图及其派生图层的缩略图。
- 失败包不含本机路径、密钥、`worker.env` 内容、供应商原始响应、其它用户数据。
- 决策日志留在任务目录（700 权限）；上传到线上的只有白名单诊断码、建议和 ≤ 200 字摘要。失败任务的产物仍不可被客户端读取。
- `docs/security-operations.md` 与隐私政策模板补一句：生成过程中的中间图会以缩略图形式用于自动质检。

## 10. 测试与验收

- 单测（制作端）：失败包组装不含敏感字段；白名单与预算；无效模型回答的降级；`clip_background` 在合成图上的裁剪；`tune_motion` 的回退序列；嘴部兜底重测。
- 回放：`5462ebc4` 的产物 dry-run 通过校验（规则层）；用录制的模型回答做决策回放（第二层），保证同样输入同样动作。
- 后端：新字段白名单、长度限制、旧制作端不带字段时兼容、`retry_hint` 只在一次领取后清空。
- Android：诊断文案映射与按钮状态的单元测试。
- 验收指标：影子模式跑 20 单，记录“模型建议 = 人工判断”的比例（目标 ≥ 80%）、误报率、平均每单模型成本；达标后切 `act`。

## 11. 分期与 PR 拆分

| 期 | PR | 内容 | 部署 |
|---|---|---|---|
| 1 | A | 规则层：图层合理性检查 + 前景遮罩裁剪（含透明背景生成实测）、动作幅度回退、`5462ebc4` 回放验证 | 仅 Mac 制作端 |
| 1 | B | 后端诊断字段与迁移 0014/0032、`fail`/`retry`/`claim` 扩展、App 文案与按钮 | 线上 API + Android 版本 |
| 2 | C | 失败包、模型客户端、关卡审查、影子模式、决策日志 | 仅 Mac 制作端 |
| 2 | D | 菜单动作执行、预算、付费动作用户确认闭环 | Mac 制作端 |
| 3 | E | 收尾视觉质检评分（不阻断，标记待精修） | Mac 制作端 |

每个 PR 按仓库规则在 `CHANGELOG.md` 记录；B 涉及迁移与 App 发版，需要备份、验收与用户授权后部署。A 可以先做，因为它单独就能救当前失败的任务。

## 12. 待定与待验证

- 付费动作确认策略：建议“需要用户确认”，每单最多 1 次；如果改为全自动，改预算表即可。
- 审查模型供应商：建议先用 Apexin，影子期结束再评估是否切 Claude。
- 待实测：`gpt-image-2.5-sunburst` 是否支持 `background=transparent`；上传照片的抠图模型选型（4090 现有环境优先）。
- 拆层结果包一半是中间 PNG，30 MB 无压缩；若下行仍慢，可让远端只回传 PSD 与遮罩。
