import assert from "node:assert/strict";
import { mkdtemp, rm } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import app from "../dist-node/src/index.js";
import {
  migrateSqlite,
  openSqliteDatabase,
  SqliteDatabaseAdapter
} from "../dist-node/server/sqlite-db.js";

const authSecret = "ai-avatar-upload-test-secret-at-least-32-characters";
const png = new Uint8Array(Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=",
  "base64"
));
const jpeg = new Uint8Array(Buffer.from(
  "/9j/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAADAAIDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAj/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFAEBAAAAAAAAAAAAAAAAAAAAAf/EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAMAwEAAhEDEQA/AJXAIf/Z",
  "base64"
));
const webp = new Uint8Array(Buffer.from(
  "UklGRiQAAABXRUJQVlA4IBgAAAAwAQCdASoCAAMAAUAmJaQAA3AA/v0gUAA=",
  "base64"
));

test("direct companion avatar upload binds a private pending asset and is idempotent", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-ai-avatar-"));
  const rawDatabase = openSqliteDatabase(path.join(root, "anyi.sqlite"));
  migrateSqlite(rawDatabase, path.resolve("migrations"));
  const database = new SqliteDatabaseAdapter(rawDatabase);
  const storedAssets = new Map();
  const env = {
    DB: database,
    ASSETS: {
      async put(key, value, options) {
        storedAssets.set(key, { body: new Uint8Array(value), httpMetadata: options?.httpMetadata });
      },
      async get(key) {
        return storedAssets.get(key) || null;
      },
      async delete(key) {
        storedAssets.delete(key);
      }
    },
    AUTH_SECRET: authSecret,
    RATE_LIMIT_ENABLED: "false",
    PUBLIC_ASSET_BASE_URL: "https://api.anyibj.cn",
    ALLOWED_ORIGINS: "https://api.anyibj.cn"
  };

  async function request(pathname, options = {}, token = "") {
    const headers = new Headers(options.headers || {});
    if (token) headers.set("Authorization", `Bearer ${token}`);
    const response = await app.fetch(new Request(`https://api.anyibj.cn${pathname}`, {
      ...options,
      headers
    }), env);
    const bodyBytes = new Uint8Array(await response.arrayBuffer());
    const text = new TextDecoder().decode(bodyBytes);
    const isJson = response.headers.get("Content-Type")?.includes("application/json");
    return {
      response,
      data: text && isJson ? JSON.parse(text) : text || null,
      bodyBytes
    };
  }

  async function register(username) {
    const form = new FormData();
    form.append("username", username);
    form.append("password", "AiAvatarUpload2026");
    form.append("displayName", username);
    form.append("gender", "男");
    form.append("acceptedTerms", "true");
    form.append("acceptedPrivacy", "true");
    form.append("file", new Blob([png], { type: "image/png" }), "profile.png");
    const result = await request("/auth/register", { method: "POST", body: form });
    assert.equal(result.response.status, 201);
    return result.data;
  }

  try {
    const registered = await register("avatar_owner");
    const ownerToken = registered.token;
    const created = await request("/ai/companions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName: "我的猫", relation: "宠物" })
    }, ownerToken);
    assert.equal(created.response.status, 201);
    const companionId = created.data.companion.id;
    const requestId = "4f4e0f3b-2da8-4d90-97ea-5b8d62f27c62";

    const truncatedForm = new FormData();
    truncatedForm.append(
      "file",
      new Blob([new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0])], { type: "image/png" }),
      "truncated.png"
    );
    const truncated = await request(`/ai/companions/${companionId}/avatar`, {
      method: "POST",
      body: truncatedForm
    }, ownerToken);
    assert.equal(truncated.response.status, 415);
    assert.equal(truncated.data.error, "file_signature_mismatch");

    const form = new FormData();
    form.append("uploadRequestId", requestId);
    form.append("file", new Blob([png], { type: "image/png" }), "cat.png");
    const uploaded = await request(`/ai/companions/${companionId}/avatar`, {
      method: "POST",
      headers: { "Idempotency-Key": requestId },
      body: form
    }, ownerToken);
    assert.equal(uploaded.response.status, 201);
    assert.equal(uploaded.data.asset.reviewStatus, "pending");
    assert.equal(uploaded.data.companion.avatarUrl, uploaded.data.asset.url);

    const repeatedForm = new FormData();
    repeatedForm.append("uploadRequestId", requestId);
    repeatedForm.append("file", new Blob([png], { type: "image/png" }), "retry.png");
    const repeated = await request(`/ai/companions/${companionId}/avatar`, {
      method: "POST",
      headers: { "Idempotency-Key": requestId },
      body: repeatedForm
    }, ownerToken);
    assert.equal(repeated.response.status, 201);
    assert.equal(repeated.data.asset.id, uploaded.data.asset.id);
    assert.equal(repeated.data.companion.avatarUrl, uploaded.data.companion.avatarUrl);

    const secondCreated = await request("/ai/companions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ displayName: "另一只猫", relation: "宠物" })
    }, ownerToken);
    assert.equal(secondCreated.response.status, 201);
    const secondCompanionId = secondCreated.data.companion.id;
    const crossCompanionForm = new FormData();
    crossCompanionForm.append("uploadRequestId", requestId);
    crossCompanionForm.append("file", new Blob([png], { type: "image/png" }), "other-cat.png");
    const crossCompanion = await request(`/ai/companions/${secondCompanionId}/avatar`, {
      method: "POST",
      headers: { "Idempotency-Key": requestId },
      body: crossCompanionForm
    }, ownerToken);
    assert.equal(crossCompanion.response.status, 409);
    assert.equal(crossCompanion.data.error, "upload_request_id_conflict");
    assert.equal(
      rawDatabase.prepare("SELECT avatar_url FROM ai_companions WHERE id = ?")
        .get(secondCompanionId).avatar_url,
      null
    );

    const concurrentRequests = [
      "6f6f0f3b-2da8-4d90-97ea-5b8d62f27c62",
      "7f7f0f3b-2da8-4d90-97ea-5b8d62f27c62"
    ];
    const concurrentFiles = [
      { bytes: jpeg, mimeType: "image/jpeg", fileName: "concurrent.jpg" },
      { bytes: webp, mimeType: "image/webp", fileName: "concurrent.webp" }
    ];
    const concurrent = await Promise.all(concurrentRequests.map((concurrentRequestId, index) => {
      const concurrentForm = new FormData();
      concurrentForm.append("uploadRequestId", concurrentRequestId);
      const file = concurrentFiles[index];
      concurrentForm.append("file", new Blob([file.bytes], { type: file.mimeType }), file.fileName);
      return request(`/ai/companions/${companionId}/avatar`, {
        method: "POST",
        headers: { "Idempotency-Key": concurrentRequestId },
        body: concurrentForm
      }, ownerToken);
    }));
    assert.deepEqual(concurrent.map((result) => result.response.status), [201, 201]);
    const finalCompanion = rawDatabase.prepare(
      "SELECT avatar_url FROM ai_companions WHERE id = ?"
    ).get(companionId);
    assert.ok(concurrent.some((result) => result.data.companion.avatarUrl === finalCompanion.avatar_url));
    assert.equal(
      rawDatabase.prepare("SELECT COUNT(*) AS count FROM asset_delete_queue WHERE owner_id = ?")
        .get(registered.user.id).count,
      2
    );

    const assetPath = new URL(uploaded.data.companion.avatarUrl).pathname;
    const ownerRead = await request(assetPath, {}, ownerToken);
    assert.equal(ownerRead.response.status, 200);
    assert.deepEqual(ownerRead.bodyBytes, png);

    const other = await register("avatar_other");
    const forbiddenForm = new FormData();
    forbiddenForm.append("file", new Blob([png], { type: "image/png" }), "cat.png");
    const forbidden = await request(`/ai/companions/${companionId}/avatar`, {
      method: "POST",
      body: forbiddenForm
    }, other.token);
    assert.equal(forbidden.response.status, 404);

    const row = rawDatabase.prepare(
      "SELECT avatar_url FROM ai_companions WHERE id = ?"
    ).get(companionId);
    assert.equal(row.avatar_url, finalCompanion.avatar_url);
    assert.equal(
      rawDatabase.prepare("SELECT COUNT(*) AS count FROM assets WHERE owner_id = ? AND asset_key LIKE '%ai/avatar/%'")
        .get(registered.user.id).count,
      3
    );

    rawDatabase.prepare("UPDATE users SET role = 'admin' WHERE id = ?").run(registered.user.id);
    const finalUploadIndex = concurrent.findIndex((result) => result.data.companion.avatarUrl === finalCompanion.avatar_url);
    const finalUpload = concurrent[finalUploadIndex];
    assert.ok(finalUpload);
    const rejected = await request(`/admin/upload-reviews/${finalUpload.data.asset.reviewId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: "rejected", reason: "test rejection" })
    }, ownerToken);
    assert.equal(rejected.response.status, 200);
    const rejectedCompanion = await request(`/ai/companions/${companionId}`, {}, ownerToken);
    assert.equal(rejectedCompanion.response.status, 200);
    assert.equal(rejectedCompanion.data.companion.avatarUrl, null);
    const rejectedAsset = await request(new URL(finalCompanion.avatar_url).pathname, {}, ownerToken);
    assert.equal(rejectedAsset.response.status, 404);
    const rejectedRetryForm = new FormData();
    rejectedRetryForm.append("uploadRequestId", concurrentRequests[finalUploadIndex]);
    rejectedRetryForm.append("file", new Blob([png], { type: "image/png" }), "rejected-retry.png");
    const rejectedRetry = await request(`/ai/companions/${companionId}/avatar`, {
      method: "POST",
      headers: { "Idempotency-Key": concurrentRequests[finalUploadIndex] },
      body: rejectedRetryForm
    }, ownerToken);
    assert.equal(rejectedRetry.response.status, 422);
    assert.equal(rejectedRetry.data.error, "avatar_upload_rejected");
  } finally {
    rawDatabase.close();
    await rm(root, { recursive: true, force: true });
  }
});
