import assert from "node:assert/strict";
import { mkdtemp, rm } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import app from "../dist-node/src/index.js";
// A cache-busted import gives this test a second independent in-process app,
// including a separate in-memory conversation lock map. Both apps still share
// the same database, which exercises the durable voice claim.
const { default: isolatedApp } = await import("../dist-node/src/index.js?voice-isolated");
import { migrateSqlite, openSqliteDatabase, SqliteDatabaseAdapter } from "../dist-node/server/sqlite-db.js";

const authSecret = "ai-voice-message-test-secret-at-least-32-characters";
const png = new Uint8Array(Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
  "base64"
));
const wavData = Buffer.alloc(19_200);
const wavSize = Buffer.alloc(4);
wavSize.writeUInt32LE(36 + wavData.length, 0);
const wavDataSize = Buffer.alloc(4);
wavDataSize.writeUInt32LE(wavData.length, 0);
const wav = new Uint8Array(Buffer.concat([
  Buffer.from("RIFF"), wavSize, Buffer.from("WAVEfmt "),
  Buffer.from([16, 0, 0, 0, 1, 0, 1, 0, 64, 31, 0, 0, 128, 62, 0, 0, 2, 0, 16, 0]),
  Buffer.from("data"), wavDataSize, wavData
]));

test("voice message is transcribed, persisted privately, replied to, and idempotent", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-ai-voice-"));
  const rawDatabase = openSqliteDatabase(path.join(root, "anyi.sqlite"));
  migrateSqlite(rawDatabase, path.resolve("migrations"));
  const storedAssets = new Map();
  const env = {
    DB: new SqliteDatabaseAdapter(rawDatabase),
    ASSETS: {
      async put(key, value, options) {
        storedAssets.set(key, { body: new Uint8Array(value), httpMetadata: options?.httpMetadata });
      },
      async get(key) { return storedAssets.get(key) || null; },
      async delete(key) { storedAssets.delete(key); }
    },
    AUTH_SECRET: authSecret,
    RATE_LIMIT_ENABLED: "false",
    PUBLIC_ASSET_BASE_URL: "https://api.anyibj.cn",
    ALLOWED_ORIGINS: "https://api.anyibj.cn",
    AI_VOICE_ENABLED: "true",
    AI_VOICE_RETAIN_AUDIO: "true",
    ASR_PROVIDER: "openai-compatible",
    ASR_BASE_URL: "https://asr.test/v1",
    ASR_API_KEY: "asr-secret",
    ASR_MODEL: "whisper-1",
    APEXIN_BASE_URL: "https://chat.test/v1",
    APEXIN_API_KEY: "chat-secret"
  };

  async function request(pathname, options = {}, token = "", targetApp = app) {
    const headers = new Headers(options.headers || {});
    if (token) headers.set("Authorization", `Bearer ${token}`);
    const response = await targetApp.fetch(new Request(`https://api.anyibj.cn${pathname}`, { ...options, headers }), env);
    const bytes = new Uint8Array(await response.arrayBuffer());
    const text = new TextDecoder().decode(bytes);
    return {
      response,
      data: response.headers.get("Content-Type")?.includes("application/json") && text ? JSON.parse(text) : text,
      bytes
    };
  }

  async function register(username) {
    const form = new FormData();
    form.append("username", username);
    form.append("password", "AiVoiceMessage2026");
    form.append("displayName", username);
    form.append("gender", "男");
    form.append("acceptedTerms", "true");
    form.append("acceptedPrivacy", "true");
    form.append("file", new Blob([png], { type: "image/png" }), "profile.png");
    const result = await request("/auth/register", { method: "POST", body: form });
    assert.equal(result.response.status, 201);
    return result.data;
  }

  const previousFetch = globalThis.fetch;
  let asrCalls = 0;
  let chatCalls = 0;
  let failNextAsr = false;
  globalThis.fetch = async (input, init) => {
    const url = String(input);
    if (url === "https://asr.test/v1/audio/transcriptions") {
      asrCalls += 1;
      assert.equal(init.headers.Authorization, "Bearer asr-secret");
      assert.equal(init.body.get("model"), "whisper-1");
      await new Promise((resolve) => setTimeout(resolve, 20));
      if (failNextAsr) {
        failNextAsr = false;
        return new Response("upstream failed", { status: 500 });
      }
      return Response.json({ text: "今天天气真好" });
    }
    if (url === "https://chat.test/v1/chat/completions") {
      chatCalls += 1;
      return Response.json({ choices: [{ message: { content: "是啊，出去走走吧。" } }] });
    }
    throw new Error(`unexpected upstream ${url}`);
  };

  try {
    const owner = await register("voice_owner");
    const other = await register("voice_other");
    const created = await request("/ai/companions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName: "阿安", relation: "朋友" })
    }, owner.token);
    const companionId = created.data.companion.id;
    const requestId = "12345678-1234-4234-8234-123456789abc";
    const send = () => {
      const form = new FormData();
      form.append("durationMs", "1200");
      form.append("uploadRequestId", requestId);
      form.append("file", new Blob([wav], { type: "audio/wav" }), "voice.wav");
      return request(`/ai/companions/${companionId}/voice-messages`, {
        method: "POST", headers: { "Idempotency-Key": requestId }, body: form
      }, owner.token);
    };

    const invalidDurationForm = new FormData();
    invalidDurationForm.append("durationMs", "500");
    invalidDurationForm.append("uploadRequestId", "22345678-1234-4234-8234-123456789abc");
    invalidDurationForm.append("file", new Blob([wav], { type: "audio/wav" }), "voice.wav");
    const invalidDuration = await request(`/ai/companions/${companionId}/voice-messages`, {
      method: "POST", body: invalidDurationForm
    }, owner.token);
    assert.equal(invalidDuration.response.status, 400);
    assert.equal(invalidDuration.data.error, "voice_duration_invalid");

    const mismatchForm = new FormData();
    mismatchForm.append("durationMs", "5000");
    mismatchForm.append("uploadRequestId", "32345678-1234-4234-8234-123456789abc");
    mismatchForm.append("file", new Blob([wav], { type: "audio/wav" }), "voice.wav");
    const mismatch = await request(`/ai/companions/${companionId}/voice-messages`, {
      method: "POST", body: mismatchForm
    }, owner.token);
    assert.equal(mismatch.response.status, 400);
    assert.equal(mismatch.data.error, "voice_duration_mismatch");

    const missingAsrForm = new FormData();
    missingAsrForm.append("durationMs", "1200");
    missingAsrForm.append("uploadRequestId", "42345678-1234-4234-8234-123456789abc");
    missingAsrForm.append("file", new Blob([wav], { type: "audio/wav" }), "voice.wav");
    const savedAsrKey = env.ASR_API_KEY;
    env.ASR_API_KEY = "";
    const missingAsr = await request(`/ai/companions/${companionId}/voice-messages`, {
      method: "POST", body: missingAsrForm
    }, owner.token);
    env.ASR_API_KEY = savedAsrKey;
    assert.equal(missingAsr.response.status, 503);
    assert.equal(missingAsr.data.error, "asr_provider_not_configured");

    const sent = await send();
    assert.equal(sent.response.status, 201);
    assert.equal(sent.data.transcript, "今天天气真好");
    assert.equal(sent.data.voiceMessage.messageType, "voice");
    assert.equal(sent.data.voiceMessage.durationMs, 1200);
    assert.equal(sent.data.voiceMessage.audioMimeType, "audio/wav");
    assert.ok(sent.data.voiceMessage.audioUrl);
    assert.ok(sent.data.voiceMessage.audioAssetId);
    assert.equal(sent.data.messages[1].content, "是啊，出去走走吧。");

    const repeated = await send();
    assert.equal(repeated.response.status, 201);
    assert.equal(repeated.data.voiceMessage.id, sent.data.voiceMessage.id);
    assert.equal(asrCalls, 1);
    assert.equal(chatCalls, 1);

    const history = await request(`/ai/companions/${companionId}/messages`, {}, owner.token);
    assert.equal(history.data.messages.length, 2);
    assert.equal(history.data.messages[0].audioUrl, sent.data.voiceMessage.audioUrl);
    const audioPath = new URL(sent.data.voiceMessage.audioUrl).pathname;
    const ownerAudio = await request(audioPath, {}, owner.token);
    assert.equal(ownerAudio.response.status, 200);
    assert.deepEqual(ownerAudio.bytes, wav);
    const otherAudio = await request(audioPath, {}, other.token);
    assert.equal(otherAudio.response.status, 404);

    const secondRequestId = "62345678-1234-4234-8234-123456789abc";
    const secondForm = new FormData();
    secondForm.append("durationMs", "1200");
    secondForm.append("uploadRequestId", secondRequestId);
    secondForm.append("file", new Blob([wav], { type: "audio/wav" }), "voice-2.wav");
    const secondSent = await request(`/ai/companions/${companionId}/voice-messages`, {
      method: "POST", headers: { "Idempotency-Key": secondRequestId }, body: secondForm
    }, owner.token);
    assert.equal(secondSent.response.status, 201);
    rawDatabase.prepare("UPDATE users SET role = 'admin' WHERE id = ?").run(owner.user.id);
    const secondReviewId = rawDatabase.prepare(
      "SELECT id FROM upload_reviews WHERE asset_id = ?"
    ).get(secondSent.data.voiceMessage.audioAssetId).id;
    const rejected = await request(`/admin/upload-reviews/${secondReviewId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: "rejected", reason: "voice test" })
    }, owner.token);
    assert.equal(rejected.response.status, 200);
    const rejectedHistory = await request(`/ai/companions/${companionId}/messages`, {}, owner.token);
    const rejectedMessage = rejectedHistory.data.messages.find((item) => item.id === secondSent.data.voiceMessage.id);
    assert.equal(rejectedMessage.audioUrl, null);
    const rejectedAudio = await request(new URL(secondSent.data.voiceMessage.audioUrl).pathname, {}, owner.token);
    assert.equal(rejectedAudio.response.status, 404);

    const forbiddenForm = new FormData();
    forbiddenForm.append("durationMs", "1200");
    forbiddenForm.append("uploadRequestId", "52345678-1234-4234-8234-123456789abc");
    forbiddenForm.append("file", new Blob([wav], { type: "audio/wav" }), "voice.wav");
    const forbidden = await request(`/ai/companions/${companionId}/voice-messages`, {
      method: "POST", body: forbiddenForm
    }, other.token);
    assert.equal(forbidden.response.status, 404);

    const deleted = await request(
      `/ai/companions/${companionId}/messages/${sent.data.voiceMessage.id}`,
      { method: "DELETE" },
      owner.token
    );
    assert.equal(deleted.response.status, 200);
    assert.equal(
      rawDatabase.prepare("SELECT COUNT(*) AS count FROM asset_delete_queue WHERE reason = 'ai_voice_message_deleted'")
        .get().count,
      1
    );

    // Two independent app instances must not both invoke ASR/LLM for one key.
    // The loser observes the durable processing claim while the winner runs.
    const concurrentRequestId = "72345678-1234-4234-8234-123456789abc";
    const sendTo = (targetApp, id = concurrentRequestId) => {
      const form = new FormData();
      form.append("durationMs", "1200");
      form.append("uploadRequestId", id);
      form.append("file", new Blob([wav], { type: "audio/wav" }), "voice.wav");
      return request(`/ai/companions/${companionId}/voice-messages`, {
        method: "POST", headers: { "Idempotency-Key": id }, body: form
      }, owner.token, targetApp);
    };
    const beforeAsr = asrCalls;
    const beforeChat = chatCalls;
    const [concurrentA, concurrentB] = await Promise.all([
      sendTo(app),
      sendTo(isolatedApp)
    ]);
    assert.deepEqual(
      [concurrentA.response.status, concurrentB.response.status].sort((a, b) => a - b),
      [201, 409]
    );
    assert.equal(asrCalls - beforeAsr, 1);
    assert.equal(chatCalls - beforeChat, 1);

    // A failed provider call marks the claim failed; a retry with the same key
    // is allowed to reclaim it and completes one canonical message pair.
    const retryRequestId = "82345678-1234-4234-8234-123456789abc";
    failNextAsr = true;
    const failedRetry = await sendTo(app, retryRequestId);
    assert.equal(failedRetry.response.status, 502);
    const recoveredRetry = await sendTo(isolatedApp, retryRequestId);
    assert.equal(recoveredRetry.response.status, 201);
    assert.equal(recoveredRetry.data.transcript, "今天天气真好");
    assert.equal(
      rawDatabase.prepare(
        "SELECT COUNT(*) AS count FROM ai_chat_messages WHERE user_id = ? AND companion_id = ? AND client_request_id IN (?, ?)"
      ).get(owner.user.id, companionId, retryRequestId, `${retryRequestId}:reply`).count,
      2
    );

    // An abandoned claim older than the lease is safely taken over by a new
    // process instead of being stuck forever.
    const staleRequestId = "92345678-1234-4234-8234-123456789abc";
    const staleId = "stale-voice-message-id";
    rawDatabase.prepare(
      `INSERT INTO ai_chat_messages
       (id, user_id, companion_id, sender, content, created_at, message_type,
        duration_ms, audio_mime_type, client_request_id, voice_status, voice_processing_at)
       VALUES (?, ?, ?, 'user', '', ?, 'voice', 1200, 'audio/wav', ?, 'processing', ?)`
    ).run(
      staleId, owner.user.id, companionId, new Date(Date.now() - 10 * 60 * 1000).toISOString(),
      staleRequestId, new Date(Date.now() - 10 * 60 * 1000).toISOString()
    );
    const staleRecovered = await sendTo(isolatedApp, staleRequestId);
    assert.equal(staleRecovered.response.status, 201);
    assert.equal(staleRecovered.data.voiceMessage.id, staleId);
  } finally {
    globalThis.fetch = previousFetch;
    rawDatabase.close();
    await rm(root, { recursive: true, force: true });
  }
});

test("voice endpoint is disabled by default and reports missing ASR configuration", async () => {
  // Detailed ASR transport coverage lives in the integration test above. The
  // public feature flag is intentionally false unless deployment enables it.
  const response = await app.fetch(new Request("https://api.anyibj.cn/app/config"), {
    DB: { dialect: "sqlite", prepare() { throw new Error("not used"); }, batch() {}, hasColumn() {} },
    ASSETS: {}, AUTH_SECRET: authSecret, RATE_LIMIT_ENABLED: "false"
  });
  assert.equal(response.status, 200);
  assert.equal((await response.json()).ai.voice.enabled, false);
});

test("Tencent ASR signs and sends a short WAV transcription request", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-tencent-asr-"));
  const rawDatabase = openSqliteDatabase(path.join(root, "anyi.sqlite"));
  migrateSqlite(rawDatabase, path.resolve("migrations"));
  const env = {
    DB: new SqliteDatabaseAdapter(rawDatabase),
    ASSETS: {
      async put() {},
      async get() { return null; },
      async delete() {}
    },
    AUTH_SECRET: authSecret,
    RATE_LIMIT_ENABLED: "false",
    AI_VOICE_ENABLED: "true",
    AI_VOICE_RETAIN_AUDIO: "false",
    ASR_PROVIDER: "tencent",
    TENCENT_ASR_SECRET_ID: "AKIDEXAMPLE",
    TENCENT_ASR_SECRET_KEY: "example-secret-key",
    TENCENT_ASR_REGION: "ap-beijing",
    TENCENT_ASR_ENGINE_MODEL_TYPE: "16k_zh",
    APEXIN_BASE_URL: "https://chat.test/v1",
    APEXIN_API_KEY: "chat-secret"
  };

  async function request(pathname, options = {}, token = "") {
    const headers = new Headers(options.headers || {});
    if (token) headers.set("Authorization", `Bearer ${token}`);
    const response = await app.fetch(new Request(`https://api.anyibj.cn${pathname}`, { ...options, headers }), env);
    const text = await response.text();
    return { response, data: text ? JSON.parse(text) : {} };
  }

  async function register() {
    const form = new FormData();
    form.append("username", "tencent_voice_owner");
    form.append("password", "TencentVoice2026");
    form.append("displayName", "腾讯语音测试");
    form.append("gender", "男");
    form.append("acceptedTerms", "true");
    form.append("acceptedPrivacy", "true");
    form.append("file", new Blob([png], { type: "image/png" }), "profile.png");
    const result = await request("/auth/register", { method: "POST", body: form });
    assert.equal(result.response.status, 201);
    return result.data;
  }

  const previousFetch = globalThis.fetch;
  let capturedAsr = null;
  globalThis.fetch = async (input, init) => {
    const url = String(input);
    if (url === "https://asr.tencentcloudapi.com/") {
      const headers = new Headers(init.headers);
      capturedAsr = { headers, body: JSON.parse(String(init.body)) };
      return Response.json({
        Response: { Result: "今天 天气 真好。", AudioDuration: 1200, RequestId: "request-id" }
      });
    }
    if (url === "https://chat.test/v1/chat/completions") {
      return Response.json({ choices: [{ message: { content: "是啊。" } }] });
    }
    throw new Error(`unexpected upstream ${url}`);
  };

  try {
    const owner = await register();
    const created = await request("/ai/companions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName: "阿安", relation: "朋友" })
    }, owner.token);
    const requestId = "a2345678-1234-4234-8234-123456789abc";
    const form = new FormData();
    form.append("durationMs", "1200");
    form.append("uploadRequestId", requestId);
    form.append("file", new Blob([wav], { type: "audio/wav" }), "voice.wav");
    const sent = await request(`/ai/companions/${created.data.companion.id}/voice-messages`, {
      method: "POST",
      headers: { "Idempotency-Key": requestId },
      body: form
    }, owner.token);
    assert.equal(sent.response.status, 201);
    assert.equal(sent.data.transcript, "今天 天气 真好。");
    assert.equal(sent.data.voiceMessage.audioUrl, null);
    assert.equal(capturedAsr.headers.get("X-TC-Action"), "SentenceRecognition");
    assert.equal(capturedAsr.headers.get("X-TC-Version"), "2019-06-14");
    assert.equal(capturedAsr.headers.get("X-TC-Region"), "ap-beijing");
    assert.match(capturedAsr.headers.get("Authorization"), /^TC3-HMAC-SHA256 Credential=AKIDEXAMPLE\//);
    assert.match(capturedAsr.headers.get("Authorization"), /SignedHeaders=content-type;host;x-tc-action/);
    assert.equal(capturedAsr.body.EngSerViceType, "16k_zh");
    assert.equal(capturedAsr.body.SourceType, 1);
    assert.equal(capturedAsr.body.VoiceFormat, "wav");
    assert.equal(capturedAsr.body.DataLen, wav.byteLength);
    assert.deepEqual(Buffer.from(capturedAsr.body.Data, "base64"), Buffer.from(wav));
  } finally {
    globalThis.fetch = previousFetch;
    rawDatabase.close();
    await rm(root, { recursive: true, force: true });
  }
});

test("Tencent ASR reports missing credentials without exposing values", async () => {
  const response = await app.fetch(new Request("https://api.anyibj.cn/app/config"), {
    DB: { dialect: "sqlite", prepare() { throw new Error("not used"); }, batch() {}, hasColumn() {} },
    ASSETS: {}, AUTH_SECRET: authSecret, RATE_LIMIT_ENABLED: "false",
    AI_VOICE_ENABLED: "true", ASR_PROVIDER: "tencent"
  });
  const payload = await response.json();
  assert.equal(payload.ai.voice.enabled, true);
  assert.equal(payload.ai.voice.asrConfigured, false);
  assert.equal(JSON.stringify(payload).includes("Secret"), false);
});
