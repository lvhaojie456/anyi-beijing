# 2026-09-18 AI 回复语音合成上线与安忆 1.0.21 发布记录

## 范围

- 代码版本：`main` `fc04951`（PR [#13](https://github.com/lvhaojie456/anyi-beijing/pull/13) 语音合成与口型、[#14](https://github.com/lvhaojie456/anyi-beijing/pull/14) MySQL upsert 修复、[#15](https://github.com/lvhaojie456/anyi-beijing/pull/15) 服务端环境变量白名单、[#12](https://github.com/lvhaojie456/anyi-beijing/pull/12) 1.0.20 记录）。
- Android：`1.0.21`，versionCode `23`，在 `release/1.0.21` 于 `0013d31` 之上提升版本号后构建；`fc04951` 只改服务端文件，APK 不受影响。沿用 release-v2 签名。
- 后端：先切到 `0013d31`（含迁移 `0015_ai_companion_voice.sql` 自动应用，`_node_migrations` 14 → 15 行），再切到 `fc04951`。`.env` 追加 `TTS_ENABLED=true`、`TENCENT_TTS_VOICE_DEFAULT=uncle`，密钥复用 `TENCENT_ASR_*`。
- Mac 制作端：`d1611fa → fc04951` 的制作端代码无变化，仍在 `releases/d1611fa`，未切换。
- 腾讯云：用户开通"语音合成"并领取"超自然大模型音色免费资源包"（2 万字符、3 个月）。三个白名单音色 603006 / 602005 / 603004 均属于该包。

## 部署过程中发现并修复的两个问题

1. `ai_speech_usage` 的用量写入只写了 SQLite 的 `ON CONFLICT` 语法，线上 MySQL 第一次合成就会报错回滚（PR #14，部署前复查发现）。
2. `server/server.ts` 按白名单转发 `process.env`，PR #13 新增的 `TTS_*` 键未列入，`TTS_ENABLED=true` 写入后 `/app/config` 的 `speech.enabled` 仍为 false（PR #15，部署时发现）。两次都在 CHANGELOG「修复」小节记录。

## 后端（15:39–15:56 CST）

- 备份：`anyi-mysql-anyi_memorial-20260918-153926.sql.gz`（0013d31 部署前）、`anyi-uploads-20260918-153926.tar.gz`；`fc04951` 仅切换编译产物，未再次备份。
- 服务器构建哈希：`0013d31` → `b123ed7edd3f`，`fc04951` → `21ee21ce3c25`，均与本机一致。
- 回滚副本：`/opt/anyi-releases/before-main-20260918-0013d31`、`before-main-20260918-fc04951`，以及 `dist-node.pre-main-20260918-{0013d31,fc04951}`。
- 验证：三项服务 active，`/health` ok，`_node_migrations` 15 行，`ai_companions.voice_id` 与 `ai_speech_usage` 存在，`/app/config` 返回 `speech.enabled=true` 与三个音色，journal 无错误。

## 端到端验收（临时账号，验收后已删除）

见 CHANGELOG 运维小节的记录：注册 → 建对象（`voiceId=null`）→ `PATCH /voice` 设为 `gentle`（非法 id 400）→ `POST /speech` 首次 `miss` 返回 `audio/mpeg`，同句第二次 `hit` 且字节完全一致 → 发文字消息拿到 AI 回复 → `PATCH /messages/:id/audio` 后消息变为 `voice` 且 `audioUrl` 可带鉴权回放、未登录 401 → `DELETE /me` 清理。

## Android 1.0.21

- APK SHA-256：`059ad5423ce63a5a702147ac1f40e3e35fde6afda29094cf5f411d893f593e61`（33,839,842 字节）；AAB SHA-256：`86949251a77caa947f452e84d816927901bd9815ebff680a32b0edee18ab9fe1`。
- 签名证书 SHA-256 `40922d059fc1cc448554703e659faa2a1f27c5d24fb5ee652726f0993396c85f`，与线上一致。
- 上传为 `/var/www/anyi-downloads/anyi-memorial-1.0.21-release-v2.apk`，两个 latest 链接已切换，公网回读哈希一致；AAB 保留本地。1.0.20 / 1.0.19 / 1.0.18 仍在服务器上供回滚。

## 回滚

- 关掉语音而不回退代码：`.env` 里 `TTS_ENABLED=false` 后 `sudo systemctl restart anyi-memorial-api`，客户端自动只回文字。
- 后端代码：`cd /opt/anyiapp2/backend && mv dist-node dist-node.main-fc04951 && cp -a dist-node.pre-main-20260918-0013d31 dist-node && sudo systemctl restart anyi-memorial-api`（回到 d1611fa 的产物；迁移 0015 为纯加列加表，可保留）。
- 安装包：`sudo ln -sfn /var/www/anyi-downloads/anyi-memorial-1.0.20-release-v2.apk /var/www/anyi-downloads/anyi-memorial-latest.apk`（release-latest 同理）。

## 额度提醒

免费包 2 万字符按每条回复 50–100 字约 200–400 条回复；用尽后腾讯云返回 `UnsupportedOperation.PkgExhausted`，服务端映射为 `tts_upstream_failed`，客户端只显示文字。续费方式：购买"超自然大模型音色"付费资源包，或改用免费额度更多的"大模型音色"包（需换音色白名单）。
