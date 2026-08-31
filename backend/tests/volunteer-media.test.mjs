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

const authSecret = "volunteer-media-test-secret-at-least-32-characters";

test("volunteer cover reuses one approved upload through the full review flow", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-volunteer-media-"));
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
    const text = await response.text();
    const isJson = response.headers.get("Content-Type")?.includes("application/json");
    return {
      response,
      data: text && isJson ? JSON.parse(text) : text || null
    };
  }

  async function register(username) {
    const form = new FormData();
    form.append("username", username);
    form.append("password", "VolunteerMedia2026");
    form.append("displayName", username);
    form.append("gender", "男");
    form.append("acceptedTerms", "true");
    form.append("acceptedPrivacy", "true");
    form.append(
      "file",
      new Blob([new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10])], { type: "image/png" }),
      "avatar.png"
    );
    const result = await request("/auth/register", {
      method: "POST",
      body: form
    });
    assert.equal(result.response.status, 201);
    return result.data.token;
  }

  try {
    await register("media_admin");
    rawDatabase.prepare("UPDATE users SET role = 'admin' WHERE username = ?").run("media_admin");
    const login = await request("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: "media_admin", password: "VolunteerMedia2026" })
    });
    assert.equal(login.response.status, 200);
    const adminToken = login.data.token;

    const png = new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0]);
    const uploadRequestId = "11111111-1111-4111-8111-111111111111";
    const form = new FormData();
    form.append("scope", "community/volunteer");
    form.append("uploadRequestId", uploadRequestId);
    form.append("file", new Blob([png], { type: "image/png" }), "cover.png");
    const uploaded = await request("/assets", {
      method: "POST",
      headers: { "Idempotency-Key": uploadRequestId },
      body: form
    }, adminToken);
    assert.equal(uploaded.response.status, 201);
    assert.equal(uploaded.data.asset.reviewStatus, "pending");
    assert.ok(uploaded.data.asset.reviewId);

    const repeatedForm = new FormData();
    repeatedForm.append("scope", "community/volunteer");
    repeatedForm.append("uploadRequestId", uploadRequestId);
    repeatedForm.append("file", new Blob([png], { type: "image/png" }), "retry.png");
    const repeatedUpload = await request("/assets", {
      method: "POST",
      headers: { "Idempotency-Key": uploadRequestId },
      body: repeatedForm
    }, adminToken);
    assert.equal(repeatedUpload.response.status, 201);
    assert.equal(repeatedUpload.data.asset.id, uploaded.data.asset.id);
    assert.equal(repeatedUpload.data.asset.reviewId, uploaded.data.asset.reviewId);
    assert.equal(
      rawDatabase.prepare("SELECT COUNT(*) AS count FROM assets WHERE client_request_id = ?")
        .get(uploadRequestId).count,
      1
    );

    const volunteerBody = {
      title: "Media review project",
      body: "A volunteer project with a reviewed cover",
      contact: "test",
      imageUrl: uploaded.data.asset.url
    };
    const pendingCreate = await request("/community/volunteer", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(volunteerBody)
    }, adminToken);
    assert.equal(pendingCreate.response.status, 409);
    assert.equal(pendingCreate.data.error, "community_media_not_approved");

    const pendingReview = await request(
      `/asset-reviews/${uploaded.data.asset.id}`,
      {},
      adminToken
    );
    assert.equal(pendingReview.response.status, 200);
    assert.equal(pendingReview.data.review.status, "pending");

    const viewerToken = await register("media_viewer");
    const privateReview = await request(`/asset-reviews/${uploaded.data.asset.id}`, {}, viewerToken);
    assert.equal(privateReview.response.status, 404);

    const approved = await request(`/admin/upload-reviews/${uploaded.data.asset.reviewId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: "approved", reason: "" })
    }, adminToken);
    assert.equal(approved.response.status, 200);

    const repeatedApproval = await request(`/admin/upload-reviews/${uploaded.data.asset.reviewId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: "approved", reason: "ignored retry" })
    }, adminToken);
    assert.equal(repeatedApproval.response.status, 200);

    const reversedApproval = await request(`/admin/upload-reviews/${uploaded.data.asset.reviewId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: "rejected", reason: "late reversal" })
    }, adminToken);
    assert.equal(reversedApproval.response.status, 409);
    assert.equal(reversedApproval.data.error, "review_status_conflict");
    assert.equal(
      rawDatabase.prepare("SELECT COUNT(*) AS count FROM asset_delete_queue WHERE asset_key = ?")
        .get(uploaded.data.asset.key).count,
      0
    );

    const approvedReview = await request(
      `/asset-reviews/${uploaded.data.asset.id}`,
      {},
      adminToken
    );
    assert.equal(approvedReview.response.status, 200);
    assert.equal(approvedReview.data.review.status, "approved");

    const volunteerCreateRequestId = "22222222-2222-4222-8222-222222222222";
    const canonicalVolunteerBody = {
      ...volunteerBody,
      imageUrl: `https://untrusted.example/assets/${encodeURIComponent(uploaded.data.asset.key)}`,
      volunteerCreateRequestId
    };
    const created = await request("/community/volunteer", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": volunteerCreateRequestId
      },
      body: JSON.stringify(canonicalVolunteerBody)
    }, adminToken);
    assert.equal(created.response.status, 201);
    assert.equal(created.data.volunteer.imageUrl, uploaded.data.asset.url);

    const repeatedCreate = await request("/community/volunteer", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": volunteerCreateRequestId
      },
      body: JSON.stringify({ ...canonicalVolunteerBody, title: "retry payload is ignored" })
    }, adminToken);
    assert.equal(repeatedCreate.response.status, 200);
    assert.equal(repeatedCreate.data.volunteer.id, created.data.volunteer.id);
    assert.equal(repeatedCreate.data.volunteer.title, created.data.volunteer.title);
    assert.equal(
      rawDatabase.prepare(
        "SELECT COUNT(*) AS count FROM community_volunteer_posts WHERE client_request_id = ?"
      ).get(volunteerCreateRequestId).count,
      1
    );

    const asset = rawDatabase.prepare(
      "SELECT visibility FROM assets WHERE id = ?"
    ).get(uploaded.data.asset.id);
    assert.equal(asset.visibility, "public");

    const projects = await request("/community/volunteer", {}, viewerToken);
    assert.equal(projects.response.status, 200);
    assert.ok(projects.data.volunteers.some((item) => item.id === created.data.volunteer.id));

    const assetPath = new URL(uploaded.data.asset.url).pathname;
    const publicAsset = await request(assetPath, {}, viewerToken);
    assert.equal(publicAsset.response.status, 200);

    rawDatabase.prepare(
      `INSERT INTO asset_delete_queue (id, owner_id, asset_key, reason, created_at)
       VALUES (?, (SELECT owner_id FROM assets WHERE id = ?), ?, 'upload_review_rejected', ?)`
    ).run("stale-delete", uploaded.data.asset.id, uploaded.data.asset.key, new Date().toISOString());
    const processed = await request("/admin/asset-delete-queue/process", {
      method: "POST"
    }, adminToken);
    assert.equal(processed.response.status, 200);
    assert.equal(processed.data.deleted, 0);
    assert.ok(storedAssets.has(uploaded.data.asset.key));
    assert.deepEqual(
      rawDatabase.prepare(
        "SELECT status, error_message FROM asset_delete_queue WHERE id = 'stale-delete'"
      ).get(),
      { status: "failed", error_message: "asset_delete_no_longer_allowed" }
    );

    const referencedUploadId = "88888888-8888-4888-8888-888888888888";
    const referencedForm = new FormData();
    referencedForm.append("scope", "ai/background/list");
    referencedForm.append("uploadRequestId", referencedUploadId);
    referencedForm.append("file", new Blob([png], { type: "image/png" }), "referenced.png");
    const referencedUpload = await request("/assets", {
      method: "POST",
      headers: { "Idempotency-Key": referencedUploadId },
      body: referencedForm
    }, adminToken);
    assert.equal(referencedUpload.response.status, 201);
    rawDatabase.prepare("UPDATE users SET ai_companion_list_background_url = ? WHERE username = ?")
      .run(referencedUpload.data.asset.url, "media_admin");
    rawDatabase.prepare(
      `INSERT INTO asset_delete_queue (id, owner_id, asset_key, reason, created_at)
       VALUES (?, (SELECT owner_id FROM assets WHERE id = ?), ?, 'ai_list_background_replaced', ?)`
    ).run(
      "referenced-background-delete",
      referencedUpload.data.asset.id,
      referencedUpload.data.asset.key,
      new Date().toISOString()
    );
    const protectedDelete = await request("/admin/asset-delete-queue/process", {
      method: "POST"
    }, adminToken);
    assert.equal(protectedDelete.response.status, 200);
    assert.ok(storedAssets.has(referencedUpload.data.asset.key));
    assert.deepEqual(
      rawDatabase.prepare(
        "SELECT status, error_message FROM asset_delete_queue WHERE id = 'referenced-background-delete'"
      ).get(),
      { status: "failed", error_message: "asset_delete_no_longer_allowed" }
    );

    const concurrentUploadId = "66666666-6666-4666-8666-666666666666";
    const concurrentUpload = () => {
      const concurrentForm = new FormData();
      concurrentForm.append("scope", "community/volunteer");
      concurrentForm.append("uploadRequestId", concurrentUploadId);
      concurrentForm.append("file", new Blob([png], { type: "image/png" }), "concurrent.png");
      return request("/assets", {
        method: "POST",
        headers: { "Idempotency-Key": concurrentUploadId },
        body: concurrentForm
      }, adminToken);
    };
    const concurrentUploads = await Promise.all([concurrentUpload(), concurrentUpload()]);
    assert.deepEqual(concurrentUploads.map((item) => item.response.status), [201, 201]);
    assert.equal(concurrentUploads[0].data.asset.id, concurrentUploads[1].data.asset.id);
    assert.equal(
      rawDatabase.prepare("SELECT COUNT(*) AS count FROM assets WHERE client_request_id = ?")
        .get(concurrentUploadId).count,
      1
    );

    const concurrentCreateId = "77777777-7777-4777-8777-777777777777";
    const concurrentCreate = () => request("/community/volunteer", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": concurrentCreateId
      },
      body: JSON.stringify({
        title: "Concurrent request",
        body: "Only one volunteer project should be created",
        volunteerCreateRequestId: concurrentCreateId
      })
    }, adminToken);
    const concurrentCreates = await Promise.all([concurrentCreate(), concurrentCreate()]);
    assert.ok(concurrentCreates.every((item) => [200, 201].includes(item.response.status)));
    assert.equal(concurrentCreates[0].data.volunteer.id, concurrentCreates[1].data.volunteer.id);
    assert.equal(
      rawDatabase.prepare(
        "SELECT COUNT(*) AS count FROM community_volunteer_posts WHERE client_request_id = ?"
      ).get(concurrentCreateId).count,
      1
    );

    const rollbackUploadId = "33333333-3333-4333-8333-333333333333";
    const rollbackForm = new FormData();
    rollbackForm.append("scope", "community/volunteer");
    rollbackForm.append("uploadRequestId", rollbackUploadId);
    rollbackForm.append("file", new Blob([png], { type: "image/png" }), "rollback.png");
    const rollbackUpload = await request("/assets", {
      method: "POST",
      headers: { "Idempotency-Key": rollbackUploadId },
      body: rollbackForm
    }, adminToken);
    assert.equal(rollbackUpload.response.status, 201);
    await request(`/admin/upload-reviews/${rollbackUpload.data.asset.reviewId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: "approved" })
    }, adminToken);
    rawDatabase.exec(`
      CREATE TRIGGER fail_asset_publish
      BEFORE UPDATE OF visibility ON assets
      WHEN NEW.visibility = 'public' AND NEW.id = '${rollbackUpload.data.asset.id}'
      BEGIN
        SELECT RAISE(ABORT, 'forced publish failure');
      END
    `);
    const rollbackCreateId = "44444444-4444-4444-8444-444444444444";
    const rollbackCreate = await request("/community/volunteer", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": rollbackCreateId
      },
      body: JSON.stringify({
        title: "Must roll back",
        body: "The post insert must roll back when publishing fails",
        imageUrl: rollbackUpload.data.asset.url,
        volunteerCreateRequestId: rollbackCreateId
      })
    }, adminToken);
    assert.equal(rollbackCreate.response.status, 500);
    assert.equal(
      rawDatabase.prepare(
        "SELECT COUNT(*) AS count FROM community_volunteer_posts WHERE client_request_id = ?"
      ).get(rollbackCreateId).count,
      0
    );
    assert.equal(
      rawDatabase.prepare("SELECT visibility FROM assets WHERE id = ?")
        .get(rollbackUpload.data.asset.id).visibility,
      "private"
    );
    rawDatabase.exec("DROP TRIGGER fail_asset_publish");

    const rejectedUploadId = "55555555-5555-4555-8555-555555555555";
    const rejectedForm = new FormData();
    rejectedForm.append("scope", "community/volunteer");
    rejectedForm.append("uploadRequestId", rejectedUploadId);
    rejectedForm.append("file", new Blob([png], { type: "image/png" }), "rejected.png");
    const rejectedUpload = await request("/assets", {
      method: "POST",
      headers: { "Idempotency-Key": rejectedUploadId },
      body: rejectedForm
    }, adminToken);
    const rejected = await request(`/admin/upload-reviews/${rejectedUpload.data.asset.reviewId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: "rejected", reason: "test" })
    }, adminToken);
    assert.equal(rejected.response.status, 200);
    const repeatedRejection = await request(`/admin/upload-reviews/${rejectedUpload.data.asset.reviewId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: "rejected", reason: "retry" })
    }, adminToken);
    assert.equal(repeatedRejection.response.status, 200);
    assert.equal(
      rawDatabase.prepare("SELECT COUNT(*) AS count FROM asset_delete_queue WHERE asset_key = ?")
        .get(rejectedUpload.data.asset.key).count,
      1
    );
    const approveRejected = await request(`/admin/upload-reviews/${rejectedUpload.data.asset.reviewId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status: "approved" })
    }, adminToken);
    assert.equal(approveRejected.response.status, 409);
    assert.equal(approveRejected.data.error, "review_status_conflict");
    assert.equal(
      rawDatabase.prepare("SELECT status FROM upload_reviews WHERE id = ?")
        .get(rejectedUpload.data.asset.reviewId).status,
      "rejected"
    );
  } finally {
    await database.close();
    await rm(root, { recursive: true, force: true });
  }
});
