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

const authSecret = "admin-console-test-secret-at-least-32-characters";

function createHarness(root) {
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
      async get(key) { return storedAssets.get(key) || null; },
      async delete(key) { storedAssets.delete(key); }
    },
    AUTH_SECRET: authSecret,
    RATE_LIMIT_ENABLED: "false",
    PUBLIC_ASSET_BASE_URL: "https://api.anyibj.cn",
    ALLOWED_ORIGINS: "https://api.anyibj.cn"
  };

  async function request(pathname, options = {}, token = "") {
    const headers = new Headers(options.headers || {});
    if (token) headers.set("Authorization", `Bearer ${token}`);
    const response = await app.fetch(new Request(`https://api.anyibj.cn${pathname}`, { ...options, headers }), env);
    const text = await response.text();
    const isJson = response.headers.get("Content-Type")?.includes("application/json");
    let data = null;
    try { data = text && isJson ? JSON.parse(text) : text || null; } catch { data = text; }
    return { response, data, raw: text };
  }

  async function register(username, displayName = username) {
    const form = new FormData();
    form.append("username", username);
    form.append("password", "AdminConsole2026");
    form.append("displayName", displayName);
    form.append("gender", "女");
    form.append("acceptedTerms", "true");
    form.append("acceptedPrivacy", "true");
    form.append("file", new Blob([new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10])], { type: "image/png" }), "avatar.png");
    const result = await request("/auth/register", { method: "POST", body: form });
    assert.equal(result.response.status, 201);
    return result.data;
  }

  async function login(username) {
    const result = await request("/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password: "AdminConsole2026" })
    });
    assert.equal(result.response.status, 200);
    return result.data.token;
  }

  async function uploadImage(token, scope = "community/posts") {
    const form = new FormData();
    form.append("scope", scope);
    form.append("file", new Blob([new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10])], { type: "image/png" }), "photo.png");
    const result = await request("/assets", { method: "POST", body: form }, token);
    assert.equal(result.response.status, 201);
    return result.data.asset;
  }

  async function json(pathname, method, body, token) {
    return request(pathname, {
      method,
      headers: { "Content-Type": "application/json" },
      body: body === undefined ? undefined : JSON.stringify(body)
    }, token);
  }

  return { rawDatabase, request, register, login, uploadImage, json };
}

test("admin console serves its shell, styles and script with a strict CSP", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-admin-console-"));
  const harness = createHarness(root);
  try {
    for (const pathname of ["/admin", "/admin/"]) {
      const page = await harness.request(pathname);
      assert.equal(page.response.status, 200);
      assert.match(page.response.headers.get("Content-Security-Policy") || "", /default-src 'none'/);
      assert.match(page.response.headers.get("Content-Security-Policy") || "", /script-src 'self'/);
      assert.equal(page.response.headers.get("X-Frame-Options"), "DENY");
      assert.match(page.raw, /\/admin\/app\.js/);
      assert.match(page.raw, /安忆后台/);
      assert.doesNotMatch(page.raw, /<script>[^<]/);
      assert.doesNotMatch(page.raw, /onclick=/);
    }

    const script = await harness.request("/admin/app.js");
    assert.equal(script.response.status, 200);
    assert.match(script.response.headers.get("Content-Type") || "", /application\/javascript/);
    const etag = script.response.headers.get("ETag");
    assert.ok(etag);
    // The inline script must stay parseable: the page is unusable if it is not.
    assert.doesNotThrow(() => new Function(script.raw));
    assert.doesNotMatch(script.raw, /\)\s*\.style\s*=\s*'/);
    assert.match(script.raw, /\/admin\/upload-reviews/);

    const notModified = await harness.request("/admin/app.js", { headers: { "If-None-Match": etag } });
    assert.equal(notModified.response.status, 304);

    const styles = await harness.request("/admin/app.css");
    assert.equal(styles.response.status, 200);
    assert.match(styles.response.headers.get("Content-Type") || "", /text\/css/);
    assert.match(styles.raw, /--brass/);
  } finally {
    harness.rawDatabase.close();
    await rm(root, { recursive: true, force: true });
  }
});

test("admin overview, search and queue endpoints require an administrator", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-admin-access-"));
  const harness = createHarness(root);
  try {
    assert.equal((await harness.request("/admin/overview")).response.status, 401);
    const member = await harness.register("member_user", "普通用户");
    const owner = await harness.register("access_admin", "访问管理员");
    harness.rawDatabase.prepare("UPDATE users SET role = 'admin' WHERE id = ?").run(owner.user.id);
    const adminToken = await harness.login("access_admin");
    assert.equal((await harness.request("/admin/overview", {}, member.token)).response.status, 403);
    const overview = await harness.request("/admin/overview", {}, member.token);
    assert.equal(overview.response.status, 403);
    assert.equal(overview.data.error, "admin_required");
    for (const pathname of ["/admin/users", "/admin/upload-reviews", "/admin/audit-logs", "/admin/live2d/jobs", "/admin/volunteer/posts"]) {
      assert.equal((await harness.request(pathname, {}, member.token)).response.status, 403, pathname);
    }
    // "/admin/users/moderation" stays its own route instead of being captured as a user id.
    assert.equal((await harness.request("/admin/users/moderation", {}, member.token)).response.status, 403);
    assert.equal((await harness.request("/admin/users/moderation", {}, adminToken)).response.status, 200);
  } finally {
    harness.rawDatabase.close();
    await rm(root, { recursive: true, force: true });
  }
});

test("admin overview reports queue sizes, activity and service capabilities", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-admin-overview-"));
  const harness = createHarness(root);
  try {
    const admin = await harness.register("console_admin", "控制台管理员");
    harness.rawDatabase.prepare("UPDATE users SET role = 'admin' WHERE id = ?").run(admin.user.id);
    const token = await harness.login("console_admin");
    const member = await harness.register("member_one", "普通会员");

    // Registration avatars are media and therefore land in the review queue.
    await harness.json("/community/posts", "POST", { content: "想给奶奶写一点纪念的话。" }, member.token);
    await harness.json("/community/posts", "POST", { content: "低价出售纪念用品，加微信联系。" }, member.token);
    await harness.json("/legal/account-deletion/request", "POST", undefined, "");

    const overview = await harness.request("/admin/overview", {}, token);
    assert.equal(overview.response.status, 200);
    assert.equal(overview.response.headers.get("Cache-Control"), "no-store");
    const data = overview.data;
    assert.equal(data.queues.uploads, 2);
    assert.equal(data.queues.communityPosts, 1);
    assert.equal(data.queues.communityComments, 0);
    assert.equal(data.queues.community, 1);
    assert.equal(data.totals.users, 2);
    assert.equal(data.activity.days.length, 14);
    assert.equal(data.activity.days[13], data.activity.today);
    assert.equal(data.activity.users[13], 2);
    assert.equal(data.activity.posts[13], 2);
    assert.deepEqual(data.activity.crashes, new Array(14).fill(0));
    assert.equal(data.services.database, "sqlite");
    assert.equal(data.services.voiceRecognition, false);
    assert.equal(data.services.speech, false);
    assert.equal(data.services.live2d, false);
    assert.equal(data.services.speechToday.limitPerUser, 20000);
    assert.ok(Array.isArray(data.recentAudit));
    assert.ok(JSON.stringify(data).indexOf("password") < 0);
  } finally {
    harness.rawDatabase.close();
    await rm(root, { recursive: true, force: true });
  }
});

test("admin upload review listing is enriched and decisions keep their side effects", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-admin-uploads-"));
  const harness = createHarness(root);
  try {
    const admin = await harness.register("upload_admin", "审核员");
    harness.rawDatabase.prepare("UPDATE users SET role = 'admin' WHERE id = ?").run(admin.user.id);
    const token = await harness.login("upload_admin");
    const uploader = await harness.register("upload_member", "上传用户");
    const asset = await harness.uploadImage(uploader.token, "profiles");

    const listing = await harness.request("/admin/upload-reviews?status=pending", {}, token);
    assert.equal(listing.response.status, 200);
    // Registration avatars are media too, so the queue holds more than one row.
    const review = listing.data.reviews.find((row) => row.asset_key === asset.key);
    assert.ok(review);
    assert.equal(review.username, "upload_member");
    assert.equal(review.display_name, "上传用户");
    assert.equal(listing.data.counts.pending, listing.data.reviews.length);
    assert.equal((await harness.request("/admin/upload-reviews?status=nonsense", {}, token)).response.status, 400);

    const reason = await harness.json(`/admin/upload-reviews/${asset.reviewId}`, "PATCH", { status: "rejected" }, token);
    assert.equal(reason.response.status, 200);
    const queue = harness.rawDatabase.prepare("SELECT asset_key, reason FROM asset_delete_queue").all();
    assert.deepEqual(queue, [{ asset_key: asset.key, reason: "upload_review_rejected" }]);
    const repeated = await harness.json(`/admin/upload-reviews/${asset.reviewId}`, "PATCH", { status: "rejected" }, token);
    assert.equal(repeated.response.status, 200);
    const conflict = await harness.json(`/admin/upload-reviews/${asset.reviewId}`, "PATCH", { status: "approved" }, token);
    assert.equal(conflict.response.status, 409);
    assert.equal(conflict.data.error, "review_status_conflict");
    assert.equal(conflict.data.details.currentStatus, "rejected");

    // Batch approval reports per-item outcomes instead of failing the request.
    const second = await harness.uploadImage(uploader.token, "memorials");
    const batch = await harness.json("/admin/upload-reviews/batch", "POST", {
      ids: [second.reviewId, asset.reviewId],
      status: "approved"
    }, token);
    assert.equal(batch.response.status, 200);
    assert.equal(batch.data.applied, 1);
    assert.deepEqual(batch.data.results, [
      { id: second.reviewId, ok: true },
      { id: asset.reviewId, ok: false, error: "review_status_conflict" }
    ]);
    assert.equal((await harness.json("/admin/upload-reviews/batch", "POST", { ids: [], status: "approved" }, token)).response.status, 400);
    assert.equal((await harness.json("/admin/upload-reviews/batch", "POST", { ids: [second.reviewId], status: "wiped" }, token)).response.status, 400);
  } finally {
    harness.rawDatabase.close();
    await rm(root, { recursive: true, force: true });
  }
});

test("admin community moderation carries author, counts and reported content", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-admin-community-"));
  const harness = createHarness(root);
  try {
    const admin = await harness.register("community_admin", "社区管理员");
    harness.rawDatabase.prepare("UPDATE users SET role = 'admin' WHERE id = ?").run(admin.user.id);
    const token = await harness.login("community_admin");
    const author = await harness.register("community_author", "作者");
    const reporter = await harness.register("community_reporter", "举报人");

    const clean = await harness.json("/community/posts", "POST", { content: "今天去看了外婆。" }, author.token);
    const flagged = await harness.json("/community/posts", "POST", { content: "刷单兼职日结，加微信详聊。" }, author.token);
    const comment = await harness.json(`/community/posts/${clean.data.post.id}/comments`, "POST", { content: "抱抱你。" }, reporter.token);

    const listing = await harness.request("/admin/community/moderation?status=pending", {}, token);
    assert.equal(listing.response.status, 200);
    assert.equal(listing.data.posts.length, 1);
    assert.equal(listing.data.posts[0].id, flagged.data.post.id);
    assert.equal(listing.data.posts[0].authorName, "作者");
    assert.equal(listing.data.counts.pending, 1);
    assert.equal((await harness.request("/admin/community/moderation?status=weird", {}, token)).response.status, 400);

    const approved = await harness.json(`/admin/community/posts/${flagged.data.post.id}`, "PATCH", { status: "approved", reason: "" }, token);
    assert.equal(approved.response.status, 200);
    assert.equal(approved.data.status, "approved");
    const afterApproval = await harness.request("/admin/community/moderation?status=approved", {}, token);
    assert.deepEqual(afterApproval.data.posts.map((row) => row.id).sort(), [clean.data.post.id, flagged.data.post.id].sort());
    assert.equal((await harness.json(`/admin/community/posts/${flagged.data.post.id}`, "PATCH", { status: "nope" }, token)).response.status, 400);
    assert.equal((await harness.json("/admin/community/posts/missing-id", "PATCH", { status: "approved" }, token)).response.status, 404);

    const blocked = await harness.json(`/admin/community/comments/${comment.data.comment.id}`, "PATCH", { status: "blocked", reason: "测试屏蔽" }, token);
    assert.equal(blocked.response.status, 200);
    assert.equal(harness.rawDatabase.prepare("SELECT status FROM community_post_comments WHERE id = ?").get(comment.data.comment.id).status, "blocked");

    // Reports include the reported content so the reviewer never has to search for it.
    await harness.json("/community/reports", "POST", { targetType: "post", targetId: clean.data.post.id, reason: "疑似冒用照片。" }, reporter.token);
    const reports = await harness.request("/admin/community/reports?status=pending", {}, token);
    assert.equal(reports.response.status, 200);
    assert.equal(reports.data.reports.length, 1);
    const report = reports.data.reports[0];
    assert.equal(report.target.exists, true);
    assert.equal(report.target.content, "今天去看了外婆。");
    assert.equal(report.target.authorName, "作者");
    assert.equal(report.target.status, "approved");
    assert.equal(report.reporter_username, "community_reporter");
    assert.equal(reports.data.counts.pending, 1);

    const removed = await harness.json(`/admin/community/reports/${report.id}`, "PATCH", { action: "remove", reason: "确认违规" }, token);
    assert.equal(removed.response.status, 200);
    assert.equal(removed.data.status, "actioned");
    assert.equal(harness.rawDatabase.prepare("SELECT status FROM community_posts WHERE id = ?").get(clean.data.post.id).status, "rejected");
    assert.equal((await harness.json(`/admin/community/reports/${report.id}`, "PATCH", { action: "remove" }, token)).response.status, 409);
    assert.equal((await harness.json(`/admin/community/reports/${report.id}`, "PATCH", { action: "erase" }, token)).response.status, 400);
  } finally {
    harness.rawDatabase.close();
    await rm(root, { recursive: true, force: true });
  }
});

test("admin user directory, detail and moderation stay usable and safe", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-admin-users-"));
  const harness = createHarness(root);
  try {
    const admin = await harness.register("user_admin", "用户管理员");
    harness.rawDatabase.prepare("UPDATE users SET role = 'admin' WHERE id = ?").run(admin.user.id);
    const token = await harness.login("user_admin");
    const member = await harness.register("search_target", "被查找的用户");
    await harness.json("/community/posts", "POST", { content: "今天天气很好。" }, member.token);

    const all = await harness.request("/admin/users", {}, token);
    assert.equal(all.response.status, 200);
    assert.equal(all.data.users.length, 2);
    assert.equal((await harness.request("/admin/users?filter=moderated", {}, token)).data.users.length, 0);
    assert.equal((await harness.request("/admin/users?filter=admins", {}, token)).data.users.length, 1);

    const search = await harness.request("/admin/users?q=search_target", {}, token);
    assert.equal(search.data.users.length, 1);
    assert.equal(search.data.users[0].username, "search_target");
    assert.equal(search.data.users[0].moderation_status, null);
    assert.ok(!("password_hash" in search.data.users[0]));
    assert.equal((await harness.request("/admin/users?q=%25", {}, token)).data.users.length, 0);
    assert.equal((await harness.request("/admin/users?filter=broken", {}, token)).response.status, 400);

    const detail = await harness.request(`/admin/users/${member.user.id}`, {}, token);
    assert.equal(detail.response.status, 200);
    assert.equal(detail.data.user.username, "search_target");
    assert.equal(detail.data.counts.posts, 1);
    assert.equal(detail.data.counts.pendingUploads, 1);
    assert.equal(detail.data.counts.companions, 0);
    assert.equal(detail.data.audit.length >= 1, true);
    assert.equal((await harness.request(`/admin/users/missing-user`, {}, token)).response.status, 404);

    const blocked = await harness.json(`/admin/users/${member.user.id}/moderation`, "PATCH", {
      status: "blocked",
      reason: "多次发布广告",
      expiresAt: new Date(Date.now() + 86_400_000).toISOString()
    }, token);
    assert.equal(blocked.response.status, 200);
    assert.equal((await harness.request(`/admin/users/${member.user.id}`, {}, token)).data.user.moderation_status, "blocked");
    assert.equal((await harness.request("/admin/users?filter=moderated", {}, token)).data.users.length, 1);
    assert.equal((await harness.request("/me", {}, member.token)).response.status, 403);
    assert.equal((await harness.json(`/admin/users/${member.user.id}/moderation`, "PATCH", { status: "blocked", expiresAt: "not-a-date" }, token)).response.status, 400);
    assert.equal((await harness.json(`/admin/users/${admin.user.id}/moderation`, "PATCH", { status: "banned" }, token)).response.status, 403);
    assert.equal((await harness.json(`/admin/users/${member.user.id}/moderation`, "PATCH", { status: "active" }, token)).response.status, 200);
    assert.equal((await harness.request("/me", {}, member.token)).response.status, 200);

    const moderationListing = await harness.request("/admin/users/moderation", {}, token);
    assert.equal(moderationListing.response.status, 200);
  } finally {
    harness.rawDatabase.close();
    await rm(root, { recursive: true, force: true });
  }
});

test("admin-initiated account deletion requires confirmation and completes the request", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-admin-delete-"));
  const harness = createHarness(root);
  try {
    const admin = await harness.register("delete_admin", "注销管理员");
    harness.rawDatabase.prepare("UPDATE users SET role = 'admin' WHERE id = ?").run(admin.user.id);
    const token = await harness.login("delete_admin");
    const member = await harness.register("delete_target", "待注销用户");
    await harness.uploadImage(member.token, "community/posts");
    await harness.request("/legal/account-deletion/request", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ username: "delete_target", contact: "user@example.com", reason: "不再使用" }).toString()
    });

    const pending = await harness.request("/admin/account-deletion-requests?status=pending", {}, token);
    assert.equal(pending.response.status, 200);
    assert.equal(pending.data.requests.length, 1);
    const request = pending.data.requests[0];
    assert.equal(request.user_id, member.user.id);
    assert.equal(request.user_display_name, "待注销用户");
    assert.equal(pending.data.counts.pending, 1);
    assert.equal((await harness.request("/admin/account-deletion-requests?status=weird", {}, token)).response.status, 400);

    const mismatch = await harness.json(`/admin/users/${member.user.id}`, "DELETE", { confirmUsername: "someone-else" }, token);
    assert.equal(mismatch.response.status, 400);
    assert.equal(mismatch.data.error, "confirm_username_mismatch");
    assert.equal((await harness.json(`/admin/users/${admin.user.id}`, "DELETE", { confirmUsername: "delete_admin" }, token)).response.status, 403);

    const deleted = await harness.json(`/admin/users/${member.user.id}`, "DELETE", {
      confirmUsername: "delete_target",
      reason: "客服核验通过",
      deletionRequestId: request.id
    }, token);
    assert.equal(deleted.response.status, 200);
    assert.equal(harness.rawDatabase.prepare("SELECT COUNT(*) AS count FROM users WHERE id = ?").get(member.user.id).count, 0);
    assert.equal(harness.rawDatabase.prepare("SELECT status FROM account_deletion_requests WHERE id = ?").get(request.id).status, "completed");
    // Both the registration avatar and the uploaded image belong to the user.
    const queued = harness.rawDatabase.prepare("SELECT asset_key FROM asset_delete_queue WHERE reason = 'account_deleted'").all();
    assert.equal(queued.length, 2);
    const audit = harness.rawDatabase.prepare("SELECT action, actor_id FROM audit_logs WHERE action = 'admin.user.account.delete'").get();
    assert.equal(audit.actor_id, admin.user.id);
    assert.equal((await harness.request("/me", {}, member.token)).response.status, 401);
    assert.equal((await harness.json("/admin/account-deletion-requests/" + request.id, "PATCH", { status: "processing" }, token)).response.status, 200);
    assert.equal((await harness.json("/admin/account-deletion-requests/" + request.id, "PATCH", { status: "unknown" }, token)).response.status, 400);
  } finally {
    harness.rawDatabase.close();
    await rm(root, { recursive: true, force: true });
  }
});

test("admin file delete queue filters, retains referenced files and processes the rest", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-admin-assets-"));
  const harness = createHarness(root);
  try {
    const admin = await harness.register("asset_admin", "文件管理员");
    harness.rawDatabase.prepare("UPDATE users SET role = 'admin' WHERE id = ?").run(admin.user.id);
    const token = await harness.login("asset_admin");
    const member = await harness.register("asset_member", "文件用户");
    const asset = await harness.uploadImage(member.token, "profiles");
    await harness.json(`/admin/upload-reviews/${asset.reviewId}`, "PATCH", { status: "rejected", reason: "不符合要求" }, token);

    const listing = await harness.request("/admin/asset-delete-queue", {}, token);
    assert.equal(listing.response.status, 200);
    // Registration avatars are queued for deletion as soon as they are rejected,
    // so this queue is never empty for a freshly registered member.
    const queuedItem = listing.data.items.find((row) => row.asset_key === asset.key);
    assert.ok(queuedItem);
    assert.equal(queuedItem.status, "pending");
    assert.equal(listing.data.counts.pending, listing.data.items.length);
    assert.equal((await harness.request("/admin/asset-delete-queue?status=broken", {}, token)).response.status, 400);

    // A rejected upload that nothing references any more is deleted outright.
    const processed = await harness.json("/admin/asset-delete-queue/process", "POST", {}, token);
    assert.equal(processed.response.status, 200);
    assert.equal(processed.data.attempted, 1);
    assert.equal(processed.data.deleted, 1);
    assert.equal(harness.rawDatabase.prepare("SELECT status FROM asset_delete_queue WHERE asset_key = ?").get(asset.key).status, "deleted");
    assert.equal(harness.rawDatabase.prepare("SELECT COUNT(*) AS count FROM assets WHERE asset_key = ?").get(asset.key).count, 0);

    // A profile avatar stays referenced by the account, so its deletion is refused
    // and recorded as a failure instead of silently breaking the profile.
    const avatarKey = harness.rawDatabase.prepare("SELECT asset_key FROM assets WHERE owner_id = ? AND asset_key LIKE '%/profiles/%'")
      .get(member.user.id).asset_key;
    const avatarReviewId = harness.rawDatabase.prepare("SELECT id FROM upload_reviews WHERE asset_key = ?").get(avatarKey).id;
    await harness.json(`/admin/upload-reviews/${avatarReviewId}`, "PATCH", { status: "rejected", reason: "测试" }, token);
    const protectedRun = await harness.json("/admin/asset-delete-queue/process", "POST", {}, token);
    assert.equal(protectedRun.data.attempted, 1);
    assert.equal(protectedRun.data.deleted, 0);
    const avatarItem = harness.rawDatabase.prepare("SELECT id, status, error_message FROM asset_delete_queue WHERE asset_key = ?").get(avatarKey);
    assert.equal(avatarItem.status, "failed");
    assert.equal(avatarItem.error_message, "asset_delete_no_longer_allowed");
    assert.equal((await harness.request("/admin/asset-delete-queue?status=failed", {}, token)).data.items.length, 1);

    const retried = await harness.json(`/admin/asset-delete-queue/${avatarItem.id}/retry`, "POST", {}, token);
    assert.equal(retried.response.status, 200);
    assert.equal(harness.rawDatabase.prepare("SELECT status FROM asset_delete_queue WHERE id = ?").get(avatarItem.id).status, "pending");
    assert.equal((await harness.json(`/admin/asset-delete-queue/${avatarItem.id}/retry`, "POST", {}, token)).response.status, 409);
    assert.equal((await harness.json("/admin/asset-delete-queue/does-not-exist/retry", "POST", {}, token)).response.status, 404);
  } finally {
    harness.rawDatabase.close();
    await rm(root, { recursive: true, force: true });
  }
});

test("admin volunteer, crash and audit listings expose what operators need", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-admin-ops-"));
  const harness = createHarness(root);
  try {
    const admin = await harness.register("ops_admin", "运营管理员");
    harness.rawDatabase.prepare("UPDATE users SET role = 'admin' WHERE id = ?").run(admin.user.id);
    const token = await harness.login("ops_admin");
    const member = await harness.register("ops_member", "运营会员");

    const created = await harness.json("/community/volunteer", "POST", {
      title: "故事整理义工",
      body: "协助整理纪念故事。",
      contact: "在社区留言"
    }, token);
    assert.equal(created.response.status, 201);
    const closed = await harness.json(`/community/volunteer/${created.data.volunteer.id}`, "PATCH", { status: "closed" }, token);
    assert.equal(closed.response.status, 200);
    await harness.json(`/community/volunteer/${created.data.volunteer.id}`, "PATCH", { status: "open" }, token);
    const application = await harness.json(`/community/volunteer/${created.data.volunteer.id}/applications`, "POST", {
      name: "报名用户",
      phone: "13800000000",
      note: "周末有空"
    }, member.token);
    assert.equal(application.response.status, 201);

    const posts = await harness.request("/admin/volunteer/posts", {}, token);
    assert.equal(posts.response.status, 200);
    const post = posts.data.posts.find((row) => row.id === created.data.volunteer.id);
    assert.equal(post.title, "故事整理义工");
    assert.equal(post.pendingApplications, 1);
    assert.equal(post.approvedApplications, 0);
    assert.equal(post.adminUsername, "ops_admin");
    assert.equal(post.status, "open");

    const applications = await harness.request("/community/volunteer/applications?status=pending", {}, token);
    assert.equal(applications.data.applications.length, 1);
    const reviewed = await harness.json(`/community/volunteer/applications/${application.data.application.id}`, "PATCH", { status: "approved" }, token);
    assert.equal(reviewed.response.status, 200);
    assert.equal((await harness.request("/admin/volunteer/posts", {}, token)).data.posts.find((row) => row.id === post.id).pendingApplications, 0);

    await harness.json("/crash-reports", "POST", {
      platform: "android",
      appVersion: "1.0.21",
      deviceModel: "Pixel 8",
      osVersion: "15",
      errorType: "java.lang.NullPointerException",
      message: "boom",
      stackTrace: "at MainActivity.kt:1"
    }, member.token);
    const crashes = await harness.request("/admin/crash-reports", {}, token);
    assert.equal(crashes.response.status, 200);
    assert.equal(crashes.data.reports.length, 1);
    assert.equal(crashes.data.reports[0].username, "ops_member");
    assert.equal(crashes.data.reports[0].app_version, "1.0.21");

    const allAudit = await harness.request("/admin/audit-logs", {}, token);
    assert.equal(allAudit.response.status, 200);
    assert.ok(allAudit.data.logs.length >= 3);
    const adminOnly = await harness.request("/admin/audit-logs?action=admin.upload.review", {}, token);
    assert.equal(adminOnly.data.logs.length, 0);
    const communityOnly = await harness.request("/admin/audit-logs?action=community.volunteer.", {}, token);
    assert.ok(communityOnly.data.logs.length >= 2);
    assert.ok(communityOnly.data.logs.every((row) => row.action.startsWith("community.volunteer.")));
    // Volunteer applications are submitted by members, so only the recruitments
    // themselves carry the administrator as the actor.
    assert.ok(communityOnly.data.logs.some((row) => row.action === "community.volunteer.create" && row.actor_username === "ops_admin"));
    const byActor = await harness.request("/admin/audit-logs?actor=ops_admin", {}, token);
    assert.ok(byActor.data.logs.length >= 2);
    assert.ok(byActor.data.logs.every((row) => row.actor_username === "ops_admin"));
    assert.equal((await harness.request(`/admin/audit-logs?targetId=${member.user.id}`, {}, token)).data.logs.length >= 1, true);
  } finally {
    harness.rawDatabase.close();
    await rm(root, { recursive: true, force: true });
  }
});

test("admin live2d queue lists jobs with companion names and rejects bad filters", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-admin-live2d-"));
  const harness = createHarness(root);
  try {
    const admin = await harness.register("live2d_admin", "形象管理员");
    harness.rawDatabase.prepare("UPDATE users SET role = 'admin' WHERE id = ?").run(admin.user.id);
    const token = await harness.login("live2d_admin");
    const member = await harness.register("live2d_member", "形象会员");

    const now = new Date().toISOString();
    const companionId = "companion-1";
    harness.rawDatabase.prepare(
      `INSERT INTO ai_companions (id, user_id, display_name, gender, relation, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?)`
    ).run(companionId, member.user.id, "妈妈", "女性", "母亲", now, now);
    for (const [id, status, diagnosis] of [
      ["job-running", "running", null],
      ["job-done", "succeeded", null],
      ["job-failed", "failed", "background_leak"]
    ]) {
      harness.rawDatabase.prepare(
        `INSERT INTO live2d_jobs (
           id, user_id, companion_id, request_id, request_hash, prompt, status, stage, progress,
           error_code, attempts, created_at, updated_at, diagnosis_code, suggestion, supervisor_summary
         ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
      ).run(id, member.user.id, companionId, `request-${id}`, `hash-${id}`, "白底写实老人", status, status === "running" ? "rigging" : status, 40, null, 1, now, now, diagnosis, diagnosis ? "new_input" : null, diagnosis ? "白底被并入图层。" : null);
    }

    const active = await harness.request("/admin/live2d/jobs?status=active", {}, token);
    assert.equal(active.response.status, 200);
    assert.deepEqual(active.data.jobs.map((row) => row.id), ["job-running"]);
    assert.equal(active.data.jobs[0].companion_name, "妈妈");
    assert.equal(active.data.jobs[0].username, "live2d_member");
    assert.equal(active.data.counts.succeeded, 1);

    const failed = await harness.request("/admin/live2d/jobs?status=failed", {}, token);
    assert.equal(failed.data.jobs.length, 1);
    assert.equal(failed.data.jobs[0].diagnosis_code, "background_leak");
    assert.equal(failed.data.jobs[0].suggestion, "new_input");
    assert.equal(failed.data.jobs[0].supervisor_summary, "白底被并入图层。");
    assert.equal((await harness.request("/admin/live2d/jobs?status=all", {}, token)).data.jobs.length, 3);
    assert.equal((await harness.request("/admin/live2d/jobs?status=nah", {}, token)).response.status, 400);
  } finally {
    harness.rawDatabase.close();
    await rm(root, { recursive: true, force: true });
  }
});
