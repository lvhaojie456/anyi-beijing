import assert from "node:assert/strict";
import { createHmac } from "node:crypto";
import test from "node:test";

import app from "../dist-node/src/index.js";

const authSecret = "test-only-auth-secret-with-at-least-32-characters";

function testEnv(overrides = {}) {
  return {
    DB: {
      dialect: "sqlite",
      prepare(query) {
        return {
          bind() { return this; },
          async first() {
            if (query.includes("FROM users WHERE id = ?")) {
              return {
                id: "user-1",
                username: "tester",
                display_name: "Tester",
                gender: "男",
                avatar_url: null,
                ai_companion_list_background_url: null,
                role: "user"
              };
            }
            return null;
          },
          async all() { return { results: [], success: true, meta: {} }; },
          async run() { return { success: true, meta: { changes: 1 } }; }
        };
      },
      async batch() { return []; },
      async hasColumn() { return true; }
    },
    ASSETS: {
      async put() {},
      async get() { return null; },
      async delete() {}
    },
    AUTH_SECRET: authSecret,
    RATE_LIMIT_ENABLED: "false",
    PUBLIC_ASSET_BASE_URL: "https://api.anyibj.cn",
    ALLOWED_ORIGINS: "https://api.anyibj.cn",
    ...overrides
  };
}

function signedToken(payloadText) {
  const data = Buffer.from(payloadText).toString("base64url");
  const signature = createHmac("sha256", authSecret).update(data).digest("base64url");
  return `${data}.${signature}`;
}

function userToken() {
  return signedToken(JSON.stringify({ sub: "user-1", exp: Math.floor(Date.now() / 1000) + 60 }));
}

function registrationForm(overrides = {}) {
  const values = {
    username: "new_user",
    password: "strong-password",
    displayName: "新用户",
    gender: "男",
    acceptedTerms: "true",
    acceptedPrivacy: "true",
    avatar: true,
    ...overrides
  };
  const form = new FormData();
  for (const key of ["username", "password", "displayName", "gender", "acceptedTerms", "acceptedPrivacy"]) {
    if (values[key] !== null && values[key] !== undefined) form.append(key, String(values[key]));
  }
  if (values.avatar) {
    form.append(
      "file",
      new File([Buffer.from("iVBORw0KGgo=", "base64")], "avatar.png", { type: "image/png" })
    );
  }
  return form;
}

function aiCompanionDatabase({ memory = null, companionOverrides = {} } = {}) {
  let companion = {
    id: "companion-1",
    user_id: "user-1",
    display_name: "阿安",
    gender: "女",
    relation: "朋友",
    avatar_url: null,
    chat_background_url: null,
    smile_avatar_url: null,
    avatar_motion_json: "{}",
    paid_unlocked: 0,
    photo_count: 0,
    voice_count: 0,
    moment_count: 0,
    generated: 0,
    avatar_style_json: "{}",
    kernel_json: "{}",
    is_default: 0,
    created_at: "2026-08-30T01:00:00.000Z",
    updated_at: "2026-08-30T01:00:00.000Z",
    ...companionOverrides
  };
  return {
    dialect: "sqlite",
    prepare(query) {
      let values = [];
      return {
        bind(...nextValues) {
          values = nextValues;
          return this;
        },
        async first() {
          if (query.includes("FROM users WHERE id = ?")) {
            return {
              id: "user-1", username: "tester", display_name: "Tester", gender: "男", avatar_url: null,
              ai_companion_list_background_url: null, role: "user"
            };
          }
          if (query.includes("FROM ai_companions WHERE id = ? AND user_id = ?")) {
            return values[0] === companion.id && values[1] === companion.user_id ? companion : null;
          }
          if (query.includes("FROM ai_memory_items")) {
            return memory;
          }
          return null;
        },
        async all() { return { results: [], success: true, meta: {} }; },
        async run() {
          if (query.startsWith("INSERT INTO ai_companions")) {
            companion = {
              ...companion,
              id: values[0],
              user_id: values[1],
              display_name: values[2],
              gender: values[3],
              relation: values[4],
              avatar_url: values[5],
              smile_avatar_url: values[6],
              avatar_motion_json: values[7],
              paid_unlocked: values[8],
              photo_count: values[9],
              voice_count: values[10],
              moment_count: values[11],
              generated: values[12],
              avatar_style_json: values[13],
              kernel_json: values[14],
              is_default: values[15],
              created_at: values[16],
              updated_at: values[17]
            };
          }
          return { success: true, meta: { changes: 1 } };
        }
      };
    },
    async batch() { return []; },
    async hasColumn() { return true; }
  };
}

function aiBackgroundDatabase({ assetOwner = "user-1", assetMimeType = "image/png" } = {}) {
  const listUrl = "https://api.anyibj.cn/assets/user-1%2Fai%2Fbackgrounds%2Flist.png";
  const chatUrl = "https://api.anyibj.cn/assets/user-1%2Fai%2Fbackgrounds%2Fchat.png";
  let user = {
    id: "user-1", username: "tester", display_name: "Tester", gender: "男",
    avatar_url: null, ai_companion_list_background_url: null, role: "user"
  };
  let companion = {
    id: "companion-1", user_id: "user-1", display_name: "阿安", gender: "女", relation: "朋友",
    chat_background_url: null, avatar_url: null, smile_avatar_url: null, avatar_motion_json: "{}",
    paid_unlocked: 0, photo_count: 0, voice_count: 0, moment_count: 0, generated: 0,
    avatar_style_json: "{}", kernel_json: "{}", is_default: 0,
    created_at: "2026-08-30T01:00:00.000Z", updated_at: "2026-08-30T01:00:00.000Z"
  };
  const queuedDeletes = [];
  let cleanupBatches = 0;
  return {
    dialect: "sqlite",
    prepare(query) {
      let values = [];
      return {
        bind(...nextValues) { values = nextValues; return this; },
        async first() {
          if (query.includes("FROM users WHERE ai_companion_list_background_url = ?")) {
            return { count: user.ai_companion_list_background_url === values[0] ? 1 : 0 };
          }
          if (query.includes("FROM ai_companions WHERE chat_background_url = ?")) {
            return { count: companion.chat_background_url === values[0] ? 1 : 0 };
          }
          if (query.includes("FROM users WHERE id = ?")) return values[0] === user.id ? user : null;
          if (query.includes("FROM ai_companions WHERE id = ? AND user_id = ?")) {
            return values[0] === companion.id && values[1] === companion.user_id ? companion : null;
          }
          if (query.includes("FROM assets a WHERE a.asset_key = ?")) {
            const key = values[0];
            const url = String(key).endsWith("list.png") ? listUrl : chatUrl;
            return { url, owner_id: assetOwner, mime_type: assetMimeType, review_status: "approved" };
          }
          return null;
        },
        async all() { return { results: [], success: true, meta: {} }; },
        async run() {
          if (query.startsWith("UPDATE users SET ai_companion_list_background_url")) {
            user = { ...user, ai_companion_list_background_url: values[0] };
          }
          if (query.startsWith("UPDATE ai_companions SET chat_background_url")) {
            companion = { ...companion, chat_background_url: values[0], updated_at: values[1] };
          }
          if (query.startsWith("UPDATE ai_companions SET live2d_model")) {
            companion = { ...companion, live2d_model: values[0], updated_at: values[1] };
          }
          if (query.startsWith("INSERT INTO asset_delete_queue")) queuedDeletes.push(values[2]);
          return { success: true, meta: { changes: 1 } };
        }
      };
    },
    async batch() { cleanupBatches += 1; return []; },
    async hasColumn() { return true; },
    urls: { listUrl, chatUrl },
    queuedDeletes,
    get cleanupBatches() { return cleanupBatches; }
  };
}

test("invalid and oversized JSON bodies are rejected", async () => {
  const invalid = await app.fetch(new Request("https://api.anyibj.cn/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: "{"
  }), testEnv());
  assert.equal(invalid.status, 400);
  assert.equal((await invalid.json()).error, "invalid_json");

  const oversized = await app.fetch(new Request("https://api.anyibj.cn/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ padding: "x".repeat(300 * 1024) })
  }), testEnv());
  assert.equal(oversized.status, 413);
});

test("registration requires a display name, binary user gender, and avatar", async () => {
  const legacyJson = await app.fetch(new Request("https://api.anyibj.cn/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: "legacy", password: "password123" })
  }), testEnv());
  assert.equal(legacyJson.status, 400);
  assert.equal((await legacyJson.json()).error, "registration_profile_required");

  const missingName = await app.fetch(new Request("https://api.anyibj.cn/auth/register", {
    method: "POST",
    body: registrationForm({ displayName: "" })
  }), testEnv());
  assert.equal(missingName.status, 400);
  assert.equal((await missingName.json()).error, "displayName_required");

  const invalidGender = await app.fetch(new Request("https://api.anyibj.cn/auth/register", {
    method: "POST",
    body: registrationForm({ gender: "未知" })
  }), testEnv());
  assert.equal(invalidGender.status, 400);
  assert.equal((await invalidGender.json()).error, "gender_invalid");

  const missingAvatar = await app.fetch(new Request("https://api.anyibj.cn/auth/register", {
    method: "POST",
    body: registrationForm({ avatar: false })
  }), testEnv());
  assert.equal(missingAvatar.status, 400);
  assert.equal((await missingAvatar.json()).error, "avatar_required");

  const response = await app.fetch(new Request("https://api.anyibj.cn/auth/register", {
    method: "POST",
    body: registrationForm()
  }), testEnv());
  assert.equal(response.status, 201);
  const payload = await response.json();
  assert.equal(payload.user.displayName, "新用户");
  assert.equal(payload.user.gender, "男");
  assert.match(decodeURIComponent(payload.user.avatarUrl), /\/assets\/.*\/profiles\//);
  assert.ok(payload.token);
});

test("registration rolls back a newly inserted user when avatar storage fails", async () => {
  let inserted = false;
  let rollbackQueries = [];
  const db = {
    dialect: "sqlite",
    prepare(query) {
      return {
        query,
        bind() { return this; },
        async first() { return null; },
        async all() { return { results: [], success: true, meta: {} }; },
        async run() {
          if (query.startsWith("INSERT INTO users")) inserted = true;
          return { success: true, meta: { changes: 1 } };
        }
      };
    },
    async batch(statements) {
      rollbackQueries = statements.map((statement) => statement.query);
      return [];
    },
    async hasColumn() { return true; }
  };
  const response = await app.fetch(new Request("https://api.anyibj.cn/auth/register", {
    method: "POST",
    body: registrationForm({ username: "rollback_user" })
  }), testEnv({
    DB: db,
    ASSETS: {
      async put() { throw new Error("forced avatar storage failure"); },
      async get() { return null; },
      async delete() {}
    }
  }));
  assert.equal(response.status, 500);
  assert.equal(inserted, true);
  assert.ok(rollbackQueries.some((query) => query === "DELETE FROM users WHERE id = ?"));
});

test("profile updates reject user genders outside male and female", async () => {
  const response = await app.fetch(new Request("https://api.anyibj.cn/me", {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${userToken()}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ displayName: "Tester", gender: "未知" })
  }), testEnv());
  assert.equal(response.status, 400);
  assert.equal((await response.json()).error, "gender_invalid");
});

test("AI list and chat backgrounds are independent private owned images", async () => {
  const db = aiBackgroundDatabase();
  const listResponse = await app.fetch(new Request("https://api.anyibj.cn/me/ai-companion-background", {
    method: "PATCH",
    headers: { Authorization: `Bearer ${userToken()}`, "Content-Type": "application/json" },
    body: JSON.stringify({ backgroundUrl: db.urls.listUrl })
  }), testEnv({ DB: db }));
  assert.equal(listResponse.status, 200);
  assert.equal((await listResponse.json()).user.aiCompanionListBackgroundUrl, db.urls.listUrl);

  const chatResponse = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1/background",
    {
      method: "PATCH",
      headers: { Authorization: `Bearer ${userToken()}`, "Content-Type": "application/json" },
      body: JSON.stringify({ backgroundUrl: db.urls.chatUrl })
    }
  ), testEnv({ DB: db }));
  assert.equal(chatResponse.status, 200);
  const companion = (await chatResponse.json()).companion;
  assert.equal(companion.chatBackgroundUrl, db.urls.chatUrl);
  assert.notEqual(companion.chatBackgroundUrl, db.urls.listUrl);

  const cleared = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1/background",
    {
      method: "PATCH",
      headers: { Authorization: `Bearer ${userToken()}`, "Content-Type": "application/json" },
      body: JSON.stringify({ backgroundUrl: null })
    }
  ), testEnv({ DB: db }));
  assert.equal(cleared.status, 200);
  assert.equal((await cleared.json()).companion.chatBackgroundUrl, null);
});

test("companion Live2D avatar binding accepts only bundled model ids", async () => {
  const db = aiBackgroundDatabase();
  const bound = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1/live2d",
    {
      method: "PATCH",
      headers: { Authorization: `Bearer ${userToken()}`, "Content-Type": "application/json" },
      body: JSON.stringify({ live2dModel: "Kei" })
    }
  ), testEnv({ DB: db }));
  assert.equal(bound.status, 200);
  assert.equal((await bound.json()).companion.live2dModel, "kei");

  const rejected = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1/live2d",
    {
      method: "PATCH",
      headers: { Authorization: `Bearer ${userToken()}`, "Content-Type": "application/json" },
      body: JSON.stringify({ live2dModel: "../etc/passwd" })
    }
  ), testEnv({ DB: db }));
  assert.equal(rejected.status, 400);
  assert.equal((await rejected.json()).error, "live2d_model_not_supported");

  const cleared = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1/live2d",
    {
      method: "PATCH",
      headers: { Authorization: `Bearer ${userToken()}`, "Content-Type": "application/json" },
      body: JSON.stringify({ live2dModel: null })
    }
  ), testEnv({ DB: db }));
  assert.equal(cleared.status, 200);
  assert.equal((await cleared.json()).companion.live2dModel, null);
});

test("clearing a chat background never queues an asset still used by the list", async () => {
  const db = aiBackgroundDatabase();
  const headers = {
    Authorization: `Bearer ${userToken()}`,
    "Content-Type": "application/json"
  };
  const listResponse = await app.fetch(new Request(
    "https://api.anyibj.cn/me/ai-companion-background",
    { method: "PATCH", headers, body: JSON.stringify({ backgroundUrl: db.urls.listUrl }) }
  ), testEnv({ DB: db }));
  assert.equal(listResponse.status, 200);

  const chatResponse = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1/background",
    { method: "PATCH", headers, body: JSON.stringify({ backgroundUrl: db.urls.listUrl }) }
  ), testEnv({ DB: db }));
  assert.equal(chatResponse.status, 200);

  const cleared = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1/background",
    { method: "PATCH", headers, body: JSON.stringify({ backgroundUrl: null }) }
  ), testEnv({ DB: db }));
  assert.equal(cleared.status, 200);
  assert.equal(db.queuedDeletes.length, 0);
  assert.equal(db.cleanupBatches, 0);
  assert.equal((await listResponse.json()).user.aiCompanionListBackgroundUrl, db.urls.listUrl);
});

test("background upload removes the object and metadata when assignment fails", async () => {
  const base = testEnv();
  const stored = new Set();
  const deleted = [];
  let batchCalls = 0;
  const database = {
    ...base.DB,
    prepare(query) {
      if (query.startsWith("UPDATE users SET ai_companion_list_background_url")) {
        return {
          bind() { return this; },
          async run() { throw new Error("forced background assignment failure"); }
        };
      }
      return base.DB.prepare(query);
    },
    async batch() {
      batchCalls += 1;
      return [];
    }
  };
  const form = new FormData();
  form.append(
    "file",
    new File([Buffer.from("iVBORw0KGgo=", "base64")], "background.png", { type: "image/png" })
  );
  const response = await app.fetch(new Request(
    "https://api.anyibj.cn/me/ai-companion-background",
    { method: "POST", headers: { Authorization: `Bearer ${userToken()}` }, body: form }
  ), testEnv({
    DB: database,
    ASSETS: {
      async put(key) { stored.add(key); },
      async get() { return null; },
      async delete(key) { stored.delete(key); deleted.push(key); }
    }
  }));
  assert.equal(response.status, 500);
  assert.equal(stored.size, 0);
  assert.equal(deleted.length, 1);
  assert.equal(batchCalls, 2);
});

test("delete queue rechecks background references immediately before object deletion", async () => {
  const assetUrl = "https://api.anyibj.cn/assets/user-1%2Fai%2Fbackground%2Fkept.png";
  let failedReason = null;
  let objectDeletes = 0;
  const database = {
    dialect: "sqlite",
    prepare(query) {
      let values = [];
      return {
        bind(...nextValues) { values = nextValues; return this; },
        async first() {
          if (query.includes("FROM users WHERE id = ?")) {
            return {
              id: "user-1", username: "admin", display_name: "Admin", gender: "男",
              avatar_url: null, ai_companion_list_background_url: assetUrl, role: "admin"
            };
          }
          if (query.includes("FROM assets a") && query.includes("a.visibility")) {
            return { visibility: "private", url: assetUrl, review_status: "pending", owner_id: "user-1" };
          }
          if (query.includes("FROM users WHERE ai_companion_list_background_url = ?")) {
            return { count: values[0] === assetUrl ? 1 : 0 };
          }
          return { count: 0 };
        },
        async all() {
          if (query.includes("FROM asset_delete_queue") && query.includes("status = 'pending'")) {
            return {
              results: [{ id: "delete-1", asset_key: "user-1/ai/background/kept.png", reason: "ai_list_background_replaced" }],
              success: true,
              meta: {}
            };
          }
          return { results: [], success: true, meta: {} };
        },
        async run() {
          if (query.startsWith("UPDATE asset_delete_queue SET status = 'failed'")) {
            failedReason = values[1];
          }
          return { success: true, meta: { changes: 1 } };
        }
      };
    },
    async batch() { return []; },
    async hasColumn(_table, column) { return column === "asset_key"; }
  };
  const response = await app.fetch(new Request(
    "https://api.anyibj.cn/admin/asset-delete-queue/process",
    { method: "POST", headers: { Authorization: `Bearer ${userToken()}` } }
  ), testEnv({
    DB: database,
    ASSETS: {
      async put() {},
      async get() { return null; },
      async delete() { objectDeletes += 1; }
    }
  }));
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { attempted: 1, deleted: 0 });
  assert.equal(failedReason, "asset_delete_no_longer_allowed");
  assert.equal(objectDeletes, 0);
});

test("AI backgrounds reject assets owned by another user or non-images", async () => {
  for (const [database, expectedStatus, expectedError] of [
    [aiBackgroundDatabase({ assetOwner: "user-2" }), 403, "background_asset_not_owned"],
    [aiBackgroundDatabase({ assetMimeType: "audio/mpeg" }), 415, "background_asset_type_invalid"]
  ]) {
    const response = await app.fetch(new Request("https://api.anyibj.cn/me/ai-companion-background", {
      method: "PATCH",
      headers: { Authorization: `Bearer ${userToken()}`, "Content-Type": "application/json" },
      body: JSON.stringify({ backgroundUrl: database.urls.listUrl })
    }), testEnv({ DB: database }));
    assert.equal(response.status, expectedStatus);
    assert.equal((await response.json()).error, expectedError);
  }
});

test("application config only exposes the supported AI voice capability", async () => {
  const response = await app.fetch(new Request("http://127.0.0.1/app/config", {
    headers: {
      Host: "api.anyibj.cn",
      "X-Forwarded-Host": "attacker.example",
      "X-Forwarded-Proto": "https"
    }
  }), testEnv({ TRUST_PROXY: "true" }));
  assert.equal(response.status, 200);
  const payload = await response.json();
  assert.deepEqual(payload.ai, { voice: { enabled: false, asrConfigured: false } });
  assert.equal("digitalHuman" in payload, false);
});

test("malformed signed tokens return 401 instead of 500", async () => {
  const response = await app.fetch(new Request("https://api.anyibj.cn/me", {
    headers: { Authorization: `Bearer ${signedToken("{")}` }
  }), testEnv());
  assert.equal(response.status, 401);
  assert.equal((await response.json()).error, "invalid_token");
});

test("crash reports require an authenticated session", async () => {
  const response = await app.fetch(new Request("https://api.anyibj.cn/crash-reports", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ errorType: "test" })
  }), testEnv());
  assert.equal(response.status, 401);
});

test("companion avatars reject arbitrary external URLs", async () => {
  const response = await app.fetch(new Request("https://api.anyibj.cn/ai/companions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${userToken()}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      displayName: "Test",
      gender: "女",
      relation: "朋友",
      avatarUrl: "http://169.254.169.254/latest/meta-data"
    })
  }), testEnv());
  assert.equal(response.status, 422);
  assert.equal((await response.json()).error, "avatar_asset_invalid");
});

test("companion create and patch require a directional relation without a gender field", async () => {
  const created = await app.fetch(new Request("https://api.anyibj.cn/ai/companions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${userToken()}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ displayName: "旧相机", relation: "旧物" })
  }), testEnv({ DB: aiCompanionDatabase() }));
  assert.equal(created.status, 201);
  const createdCompanion = (await created.json()).companion;
  assert.equal(createdCompanion.relation, "旧物");
  assert.equal("gender" in createdCompanion, false);

  const missingRelation = await app.fetch(new Request("https://api.anyibj.cn/ai/companions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${userToken()}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ displayName: "对象" })
  }), testEnv({ DB: aiCompanionDatabase() }));
  assert.equal(missingRelation.status, 400);
  assert.equal((await missingRelation.json()).error, "relation_required");

  const ambiguousRelation = await app.fetch(new Request("https://api.anyibj.cn/ai/companions", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${userToken()}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ displayName: "孩子", relation: "父子" })
  }), testEnv({ DB: aiCompanionDatabase() }));
  assert.equal(ambiguousRelation.status, 400);
  assert.equal((await ambiguousRelation.json()).error, "relation_direction_required");

  const patchWithoutName = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1",
    {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${userToken()}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ relation: "朋友" })
    }
  ), testEnv({ DB: aiCompanionDatabase() }));
  assert.equal(patchWithoutName.status, 400);
  assert.equal((await patchWithoutName.json()).error, "displayName_required");

  const patched = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1",
    {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${userToken()}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ displayName: "小狗", relation: "宠物" })
    }
  ), testEnv({ DB: aiCompanionDatabase() }));
  assert.equal(patched.status, 200);
  const patchedCompanion = (await patched.json()).companion;
  assert.equal(patchedCompanion.relation, "宠物");
  assert.equal("gender" in patchedCompanion, false);
});

test("companion inbox does not auto-create a legacy profile and includes latest message metadata", async () => {
  let listQuery = "";
  const db = {
    ...testEnv().DB,
    prepare(query) {
      return {
        bind() { return this; },
        async first() {
          if (query.includes("FROM users WHERE id = ?")) {
            return {
                id: "user-1", username: "tester", display_name: "Tester", gender: "男", avatar_url: null,
                ai_companion_list_background_url: null, role: "user"
            };
          }
          return null;
        },
        async all() {
          if (query.includes("FROM ai_companions c")) {
            listQuery = query;
            return {
              results: [{
                id: "companion-1",
                user_id: "user-1",
                display_name: "阿安",
                gender: "女",
                relation: "朋友",
                avatar_url: null,
                smile_avatar_url: null,
                avatar_motion_json: "{}",
                paid_unlocked: 0,
                photo_count: 0,
                voice_count: 0,
                moment_count: 0,
                generated: 0,
                avatar_style_json: "{}",
                kernel_json: "{}",
                is_default: 0,
                created_at: "2026-08-30T01:00:00.000Z",
                updated_at: "2026-08-30T02:00:00.000Z",
                latest_message: "晚安",
                latest_message_at: "2026-08-30T03:00:00.000Z"
              }],
              success: true,
              meta: {}
            };
          }
          return { results: [], success: true, meta: {} };
        },
        async run() { return { success: true, meta: { changes: 1 } }; }
      };
    }
  };
  const response = await app.fetch(new Request("https://api.anyibj.cn/ai/companions", {
    headers: { Authorization: `Bearer ${userToken()}` }
  }), testEnv({ DB: db }));
  assert.equal(response.status, 200);
  const payload = await response.json();
  assert.match(listQuery, /latest_message/);
  assert.doesNotMatch(listQuery, /ai_profiles/);
  assert.equal(payload.companions[0].latestMessage, "晚安");
  assert.equal(payload.companions[0].latestMessageAt, Date.parse("2026-08-30T03:00:00.000Z"));
  assert.equal(payload.companions[0].updatedAt, Date.parse("2026-08-30T02:00:00.000Z"));
});

test("AI provider calls fail clearly when the server key is missing", async () => {
  const response = await app.fetch(new Request("https://api.anyibj.cn/ai/companions/companion-1/messages", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${userToken()}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ content: "hello" })
  }), testEnv({ DB: aiCompanionDatabase() }));
  assert.equal(response.status, 503);
  assert.equal((await response.json()).error, "ai_provider_not_configured");

  const avatarForm = new FormData();
  avatarForm.append("model", "gpt-image-2");
  avatarForm.append("prompt", "人物头像");
  const avatar = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1/avatar/studio",
    {
      method: "POST",
      headers: { Authorization: `Bearer ${userToken()}` },
      body: avatarForm
    }
  ), testEnv({ DB: aiCompanionDatabase() }));
  assert.equal(avatar.status, 503);
  assert.equal((await avatar.json()).error, "ai_provider_not_configured");

  const config = await app.fetch(new Request("https://api.anyibj.cn/app/config"), testEnv());
  const payload = await config.json();
  assert.deepEqual(payload.ai, { voice: { enabled: false, asrConfigured: false } });
  assert.equal("digitalHuman" in payload, false);
});

test("image model discovery has no AI disable compatibility field", async () => {
  const response = await app.fetch(new Request("https://api.anyibj.cn/ai/image-models", {
    headers: { Authorization: `Bearer ${userToken()}` }
  }), testEnv());
  assert.equal(response.status, 200);
  const payload = await response.json();
  assert.deepEqual(payload.models.map((item) => item.id), [
    "gpt-image-2",
    "gemini-3-pro-image-1k",
    "gemini-3-pro-image-2k",
    "gemini-3-pro-image-4k",
    "gemini-3.1-flash-image-1k",
    "gemini-3.1-flash-image-2k",
    "gemini-3.1-flash-image-4k"
  ]);
  for (const model of payload.models) {
    assert.equal("enabled" in model, false);
    assert.equal("disabled" in model, false);
    assert.equal("configured" in model, false);
  }
});

test("avatar studio sends the original prompt unchanged for GPT generation", async () => {
  const db = aiCompanionDatabase();
  const previousFetch = globalThis.fetch;
  let upstreamUrl = "";
  let upstreamBody = null;
  const originalPrompt = "  水墨人物，保留这一行原文  ";
  globalThis.fetch = async (input, init) => {
    upstreamUrl = String(input);
    upstreamBody = JSON.parse(String(init.body));
    return new Response(JSON.stringify({
      data: [{ b64_json: "iVBORw0KGgo=", mime_type: "image/png" }]
    }), { status: 200, headers: { "Content-Type": "application/json" } });
  };
  try {
    const form = new FormData();
    form.append("model", "gpt-image-2");
    form.append("prompt", originalPrompt);
    const response = await app.fetch(new Request(
      "https://api.anyibj.cn/ai/companions/companion-1/avatar/studio",
      {
        method: "POST",
        headers: { Authorization: `Bearer ${userToken()}` },
        body: form
      }
    ), testEnv({
      DB: db,
      APEXIN_BASE_URL: "https://api.apexin.test/v1",
      APEXIN_API_KEY: "test-secret"
    }));
    assert.equal(response.status, 200);
    assert.equal(upstreamUrl, "https://api.apexin.test/v1/images/generations");
    assert.equal(upstreamBody.model, "gpt-image-2");
    assert.equal(upstreamBody.prompt, originalPrompt);
    assert.equal(upstreamBody.response_format, "b64_json");
    const payload = await response.json();
    assert.equal(payload.model, "gpt-image-2");
    assert.equal(payload.provider, "gpt");
    assert.equal(payload.mode, "generate");
    assert.match(payload.companion.avatarUrl, /\/assets\/user-1%2Fai%2Favatar-generated%2F/);
  } finally {
    globalThis.fetch = previousFetch;
  }
});

test("avatar studio uses GPT generations JSON with a data URL when an image is provided", async () => {
  const previousFetch = globalThis.fetch;
  let upstreamUrl = "";
  let upstreamBody = null;
  globalThis.fetch = async (input, init) => {
    upstreamUrl = String(input);
    upstreamBody = JSON.parse(String(init.body));
    return new Response(JSON.stringify({
      data: [{ b64_json: "iVBORw0KGgo=", mime_type: "image/png" }]
    }), { status: 200, headers: { "Content-Type": "application/json" } });
  };
  try {
    const originalPrompt = "只修改衣服颜色";
    const form = new FormData();
    form.append("model", "gpt-image-2");
    form.append("prompt", originalPrompt);
    form.append(
      "file",
      new Blob([Buffer.from("iVBORw0KGgo=", "base64")], { type: "image/png" }),
      "source.png"
    );
    const response = await app.fetch(new Request(
      "https://api.anyibj.cn/ai/companions/companion-1/avatar/studio",
      {
        method: "POST",
        headers: { Authorization: `Bearer ${userToken()}` },
        body: form
      }
    ), testEnv({
      DB: aiCompanionDatabase(),
      APEXIN_BASE_URL: "https://api.apexin.test/v1",
      APEXIN_API_KEY: "test-secret"
    }));
    assert.equal(response.status, 200);
    assert.equal(upstreamUrl, "https://api.apexin.test/v1/images/generations");
    assert.equal(upstreamBody.model, "gpt-image-2");
    assert.equal(upstreamBody.prompt, originalPrompt);
    assert.equal(upstreamBody.response_format, "b64_json");
    assert.deepEqual(upstreamBody.images, [{
      image_url: "data:image/png;base64,iVBORw0KGgo="
    }]);
    assert.equal((await response.json()).mode, "edit");
  } finally {
    globalThis.fetch = previousFetch;
  }
});

test("avatar studio can edit the tenant-owned current avatar", async () => {
  const currentUrl = "https://api.anyibj.cn/assets/user-1%2Fai%2Favatar%2Fcurrent.png";
  const baseDb = aiCompanionDatabase({ companionOverrides: { avatar_url: currentUrl } });
  const db = {
    ...baseDb,
    prepare(query) {
      if (query.includes("SELECT owner_id, mime_type FROM assets WHERE asset_key = ?")) {
        return {
          bind() { return this; },
          async first() { return { owner_id: "user-1", mime_type: "image/png" }; },
          async all() { return { results: [], success: true, meta: {} }; },
          async run() { return { success: true, meta: { changes: 1 } }; }
        };
      }
      return baseDb.prepare(query);
    }
  };
  const previousFetch = globalThis.fetch;
  let upstreamBody = null;
  globalThis.fetch = async (_input, init) => {
    upstreamBody = JSON.parse(String(init.body));
    return new Response(JSON.stringify({
      data: [{ b64_json: "iVBORw0KGgo=", mime_type: "image/png" }]
    }), { status: 200, headers: { "Content-Type": "application/json" } });
  };
  try {
    const form = new FormData();
    form.append("model", "gpt-image-2");
    form.append("prompt", "保留人物，换成蓝色背景");
    form.append("useCurrentAvatar", "true");
    const response = await app.fetch(new Request(
      "https://api.anyibj.cn/ai/companions/companion-1/avatar/studio",
      {
        method: "POST",
        headers: { Authorization: `Bearer ${userToken()}` },
        body: form
      }
    ), testEnv({
      DB: db,
      ASSETS: {
        async put() {},
        async get() {
          return {
            body: Buffer.from("iVBORw0KGgo=", "base64"),
            httpMetadata: { contentType: "image/png" }
          };
        },
        async delete() {}
      },
      APEXIN_BASE_URL: "https://api.apexin.test/v1",
      APEXIN_API_KEY: "test-secret"
    }));
    assert.equal(response.status, 200);
    assert.deepEqual(upstreamBody.images, [{
      image_url: "data:image/png;base64,iVBORw0KGgo="
    }]);
    assert.equal((await response.json()).mode, "edit");
  } finally {
    globalThis.fetch = previousFetch;
  }
});

test("avatar studio rejects unsupported and oversized source images", async () => {
  const unsupported = new FormData();
  unsupported.append("model", "gpt-image-2");
  unsupported.append("prompt", "编辑头像");
  unsupported.append("file", new Blob(["plain text"], { type: "text/plain" }), "source.txt");
  const unsupportedResponse = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1/avatar/studio",
    {
      method: "POST",
      headers: { Authorization: `Bearer ${userToken()}` },
      body: unsupported
    }
  ), testEnv({ DB: aiCompanionDatabase(), APEXIN_API_KEY: "test-secret" }));
  assert.equal(unsupportedResponse.status, 415);
  assert.equal((await unsupportedResponse.json()).error, "unsupported_avatar_type");

  const oversized = new FormData();
  oversized.append("model", "gpt-image-2");
  oversized.append("prompt", "编辑头像");
  oversized.append(
    "file",
    new Blob([new Uint8Array(10 * 1024 * 1024 + 1)], { type: "image/png" }),
    "large.png"
  );
  const oversizedResponse = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1/avatar/studio",
    {
      method: "POST",
      headers: { Authorization: `Bearer ${userToken()}` },
      body: oversized
    }
  ), testEnv({ DB: aiCompanionDatabase(), APEXIN_API_KEY: "test-secret" }));
  assert.equal(oversizedResponse.status, 413);
  assert.equal((await oversizedResponse.json()).error, "avatar_image_size_invalid");
});

test("avatar studio sends uploaded image as Gemini inlineData", async () => {
  const db = aiCompanionDatabase();
  const previousFetch = globalThis.fetch;
  let upstreamUrl = "";
  let upstreamBody = null;
  globalThis.fetch = async (input, init) => {
    upstreamUrl = String(input);
    upstreamBody = JSON.parse(String(init.body));
    return new Response(JSON.stringify({
      candidates: [{
        content: {
          parts: [{ text: "done" }, { inlineData: { mimeType: "image/png", data: "iVBORw0KGgo=" } }]
        }
      }]
    }), { status: 200, headers: { "Content-Type": "application/json" } });
  };
  try {
    const model = "gemini-3.1-flash-image-2k";
    const prompt = "把背景换成海边";
    const sourceBytes = Buffer.from("iVBORw0KGgo=", "base64");
    const form = new FormData();
    form.append("model", model);
    form.append("prompt", prompt);
    form.append("file", new Blob([sourceBytes], { type: "image/png" }), "source.png");
    const response = await app.fetch(new Request(
      "https://api.anyibj.cn/ai/companions/companion-1/avatar/studio",
      {
        method: "POST",
        headers: { Authorization: `Bearer ${userToken()}` },
        body: form
      }
    ), testEnv({
      DB: db,
      APEXIN_BASE_URL: "https://api.apexin.test/v1",
      APEXIN_API_KEY: "test-secret"
    }));
    assert.equal(response.status, 200);
    assert.equal(
      upstreamUrl,
      `https://api.apexin.test/v1beta/models/${model}:generateContent`
    );
    assert.deepEqual(upstreamBody.generationConfig.responseModalities, ["TEXT", "IMAGE"]);
    assert.equal(upstreamBody.contents[0].parts[0].text, prompt);
    assert.deepEqual(upstreamBody.contents[0].parts[1], {
      inlineData: { mimeType: "image/png", data: sourceBytes.toString("base64") }
    });
    const payload = await response.json();
    assert.equal(payload.model, model);
    assert.equal(payload.provider, "gemini");
    assert.equal(payload.mode, "edit");
  } finally {
    globalThis.fetch = previousFetch;
  }
});

test("companion chat defaults to gpt-5.6-luna through the shared Apexin secret", async () => {
  const db = aiCompanionDatabase();
  const previousFetch = globalThis.fetch;
  let upstreamUrl = "";
  let upstreamBody = null;
  globalThis.fetch = async (input, init) => {
    upstreamUrl = String(input);
    upstreamBody = JSON.parse(String(init.body));
    return new Response(JSON.stringify({ choices: [{ message: { content: "我在这里。" } }] }), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    });
  };
  try {
    const response = await app.fetch(new Request(
      "https://api.anyibj.cn/ai/companions/companion-1/messages",
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${userToken()}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ content: "你在吗" })
      }
    ), testEnv({
      DB: db,
      APEXIN_BASE_URL: "https://api.apexin.test/v1",
      APEXIN_API_KEY: "test-secret"
    }));
    assert.equal(response.status, 201);
    assert.equal(upstreamUrl, "https://api.apexin.test/v1/chat/completions");
    assert.equal(upstreamBody.model, "gpt-5.6-luna");
    assert.equal((await response.json()).messages[1].content, "我在这里。");
  } finally {
    globalThis.fetch = previousFetch;
  }
});

test("same companion chat requests are processed one at a time", async () => {
  const previousFetch = globalThis.fetch;
  const releases = [];
  let upstreamCalls = 0;
  let activeCalls = 0;
  let maxActiveCalls = 0;
  globalThis.fetch = async () => {
    upstreamCalls += 1;
    activeCalls += 1;
    maxActiveCalls = Math.max(maxActiveCalls, activeCalls);
    await new Promise((resolve) => releases.push(resolve));
    activeCalls -= 1;
    return new Response(
      JSON.stringify({ choices: [{ message: { content: `回复${upstreamCalls}` } }] }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  };

  const request = (content) => app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1/messages",
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${userToken()}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ content })
    }
  ), testEnv({
    DB: aiCompanionDatabase(),
    APEXIN_BASE_URL: "https://api.apexin.test/v1",
    APEXIN_API_KEY: "test-secret"
  }));

  try {
    const first = request("第一条");
    for (let attempt = 0; attempt < 100 && upstreamCalls < 1; attempt += 1) {
      await new Promise((resolve) => setTimeout(resolve, 1));
    }
    assert.equal(upstreamCalls, 1);

    const second = request("第二条");
    await new Promise((resolve) => setTimeout(resolve, 10));
    assert.equal(upstreamCalls, 1);
    releases.shift()();

    const firstResponse = await first;
    assert.equal(firstResponse.status, 201);
    for (let attempt = 0; attempt < 100 && upstreamCalls < 2; attempt += 1) {
      await new Promise((resolve) => setTimeout(resolve, 1));
    }
    assert.equal(upstreamCalls, 2);
    releases.shift()();

    const secondResponse = await second;
    assert.equal(secondResponse.status, 201);
    assert.equal(maxActiveCalls, 1);
  } finally {
    releases.splice(0).forEach((release) => release());
    globalThis.fetch = previousFetch;
  }
});

test("chat sends directional relation, user gender, reciprocal role, and all manual memories", async () => {
  const manualMemories = [{
    id: "memory-1",
    user_id: "user-1",
    companion_key: "companion-1",
    memory_type: "fact",
    memory_key: "fact:shared_trip",
    content: "我们曾一起去海边看日出",
    source_message_id: null,
    confidence: 1,
    importance: 70,
    last_used_at: null,
    created_at: "2026-08-30T00:00:00.000Z",
    updated_at: "2026-08-30T00:00:00.000Z",
    expires_at: null
  }, {
    id: "memory-2",
    user_id: "user-1",
    companion_key: "companion-1",
    memory_type: "preference",
    memory_key: "preference:tea",
    content: "用户喜欢茉莉花茶",
    source_message_id: null,
    confidence: 1,
    importance: 80,
    last_used_at: null,
    created_at: "2026-08-30T00:01:00.000Z",
    updated_at: "2026-08-30T00:01:00.000Z",
    expires_at: null
  }];
  const baseDb = aiCompanionDatabase({ companionOverrides: { relation: "儿子", gender: "男" } });
  const memoryDb = {
    ...baseDb,
    prepare(query) {
      if (query.includes("FROM ai_memory_items") && query.includes("companion_key = ?")) {
        return {
          bind() { return this; },
          async first() { return null; },
          async all() { return { results: manualMemories, success: true, meta: {} }; },
          async run() { return { success: true, meta: { changes: 1 } }; }
        };
      }
      return baseDb.prepare(query);
    }
  };
  const previousFetch = globalThis.fetch;
  let upstreamBody = null;
  globalThis.fetch = async (_input, init) => {
    upstreamBody = JSON.parse(String(init.body));
    return new Response(JSON.stringify({ choices: [{ message: { content: "我记得。" } }] }), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    });
  };
  try {
    const response = await app.fetch(new Request(
      "https://api.anyibj.cn/ai/companions/companion-1/messages",
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${userToken()}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ content: "你还记得海边吗" })
      }
    ), testEnv({
      DB: memoryDb,
      APEXIN_BASE_URL: "https://api.apexin.test/v1",
      APEXIN_API_KEY: "test-secret"
    }));
    assert.equal(response.status, 201);
    const prompt = upstreamBody.messages[1].content;
    const systemPrompt = upstreamBody.messages[0].content;
    assert.match(systemPrompt, /Trusted role contract \(higher priority than conversation data\)/);
    assert.match(systemPrompt, /companion is the user's "儿子"/);
    assert.match(systemPrompt, /user is the companion's "爸爸"/);
    assert.match(systemPrompt, /title must be "爸爸"/);
    assert.match(prompt, /all_manual_memory_base64_lines:/);
    assert.ok(prompt.includes(Buffer.from("阿安").toString("base64")));
    assert.ok(prompt.includes(Buffer.from("儿子").toString("base64")));
    assert.ok(prompt.includes(Buffer.from("男").toString("base64")));
    assert.ok(prompt.includes(Buffer.from("爸爸").toString("base64")));
    assert.match(prompt, /companion_relation_to_user_base64=/);
    assert.match(prompt, /user_gender_base64=/);
    assert.match(prompt, /inferred_user_relation_to_companion_base64=/);
    assert.equal(prompt.includes("companion_gender_base64="), false);
    for (const memory of manualMemories) {
      assert.ok(prompt.includes(Buffer.from(memory.content).toString("base64")));
    }
  } finally {
    globalThis.fetch = previousFetch;
  }
});

test("manual companion facts override a conflicting assistant history", async () => {
  const manualMemories = [{
    id: "memory-fruit",
    user_id: "user-1",
    companion_key: "companion-1",
    memory_type: "fact",
    memory_key: "fact:fruit_preferences",
    content: "我喜欢吃苹果，儿子喜欢吃梨",
    source_message_id: null,
    confidence: 1,
    importance: 100,
    last_used_at: null,
    created_at: "2026-08-30T00:00:00.000Z",
    updated_at: "2026-08-30T00:00:00.000Z",
    expires_at: null
  }];
  const history = [{
    id: "assistant-old",
    companion_id: "companion-1",
    sender: "ai",
    content: "我喜欢吃桃",
    created_at: "2026-08-30T00:01:00.000Z",
    message_type: "text",
    duration_ms: null,
    audio_mime_type: null,
    audio_url: null,
    audio_asset_id: null
  }];
  const baseDb = aiCompanionDatabase({ companionOverrides: { relation: "儿子", gender: "男" } });
  const db = {
    ...baseDb,
    prepare(query) {
      if (query.includes("FROM ai_chat_messages") && query.includes("LIMIT 20")) {
        return {
          bind() { return this; },
          async first() { return null; },
          async all() { return { results: history, success: true, meta: {} }; },
          async run() { return { success: true, meta: { changes: 1 } }; }
        };
      }
      if (query.includes("FROM ai_memory_items") && query.includes("companion_key = ?")) {
        return {
          bind() { return this; },
          async first() { return null; },
          async all() { return { results: manualMemories, success: true, meta: {} }; },
          async run() { return { success: true, meta: { changes: 1 } }; }
        };
      }
      return baseDb.prepare(query);
    }
  };
  const previousFetch = globalThis.fetch;
  let upstreamBody = null;
  globalThis.fetch = async (_input, init) => {
    upstreamBody = JSON.parse(String(init.body));
    return new Response(JSON.stringify({ choices: [{ message: { content: "应该是梨。" } }] }), {
      status: 200,
      headers: { "Content-Type": "application/json" }
    });
  };
  try {
    const response = await app.fetch(new Request(
      "https://api.anyibj.cn/ai/companions/companion-1/messages",
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${userToken()}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ content: "你喜欢吃什么水果" })
      }
    ), testEnv({
      DB: db,
      APEXIN_BASE_URL: "https://api.apexin.test/v1",
      APEXIN_API_KEY: "test-secret"
    }));
    assert.equal(response.status, 201);
    const systemPrompt = upstreamBody.messages[0].content;
    const prompt = upstreamBody.messages[1].content;
    assert.equal(upstreamBody.temperature, 0.28);
    assert.match(systemPrompt, /Memory grounding has the highest priority/);
    assert.match(systemPrompt, /MUST NOT override a manual fact/);
    assert.match(systemPrompt, /我\/我的 normally refers to the user/);
    assert.match(systemPrompt, /儿子.*COMPANION likes 梨/);
    assert.ok(prompt.indexOf("conversation_history_base64_lines:") < prompt.indexOf("all_manual_memory_base64_lines:"));
    assert.ok(prompt.includes(Buffer.from("我喜欢吃桃").toString("base64")));
    assert.ok(prompt.includes(Buffer.from("我喜欢吃苹果，儿子喜欢吃梨").toString("base64")));
    assert.match(prompt, /source=user_curated; priority=authoritative/);
    assert.match(prompt, /resolved_manual_memory_subject_base64_lines:/);
    assert.ok(prompt.includes(`subject=USER; fact_base64=${Buffer.from("我喜欢吃苹果").toString("base64")}`));
    assert.ok(prompt.includes(`subject=COMPANION; fact_base64=${Buffer.from("儿子喜欢吃梨").toString("base64")}`));
  } finally {
    globalThis.fetch = previousFetch;
  }
});


test("companion and memory resources are tenant scoped", async () => {
  const bindings = [];
  const db = {
    dialect: "sqlite",
    prepare(query) {
      return {
        bind(...values) {
          bindings.push({ query, values });
          return this;
        },
        async first() {
          if (query.includes("FROM users WHERE id = ?")) {
            return {
              id: "user-1", username: "tester", display_name: "Tester", avatar_url: null, role: "user"
            };
          }
          return null;
        },
        async all() { return { results: [], success: true, meta: {} }; },
        async run() { return { success: true, meta: { changes: 0 } }; }
      };
    },
    async batch() { return []; },
    async hasColumn() { return true; }
  };
  const response = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/not-owned/memories",
    { headers: { Authorization: `Bearer ${userToken()}` } }
  ), testEnv({ DB: db }));
  assert.equal(response.status, 404);
  const lookup = bindings.find((item) => item.query.includes("FROM ai_companions WHERE id = ?"));
  assert.deepEqual(lookup.values, ["not-owned", "user-1"]);

  const studioForm = new FormData();
  studioForm.append("model", "gpt-image-2");
  studioForm.append("prompt", "人物头像");
  const studioResponse = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/not-owned/avatar/studio",
    {
      method: "POST",
      headers: { Authorization: `Bearer ${userToken()}` },
      body: studioForm
    }
  ), testEnv({ DB: db, APEXIN_API_KEY: "test-secret" }));
  assert.equal(studioResponse.status, 404);

  const crossCompanionMemory = {
    id: "memory-2",
    user_id: "user-1",
    companion_key: "companion-2",
    memory_type: "fact",
    memory_key: "fact:other",
    content: "other",
    source_message_id: null,
    confidence: 1,
    importance: 50,
    last_used_at: null,
    created_at: "2026-08-30T00:00:00.000Z",
    updated_at: "2026-08-30T00:00:00.000Z",
    expires_at: null
  };
  const memoryResponse = await app.fetch(new Request(
    "https://api.anyibj.cn/ai/companions/companion-1/memories/memory-2",
    {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${userToken()}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ content: "overwrite" })
    }
  ), testEnv({
    DB: aiCompanionDatabase({ memory: crossCompanionMemory })
  }));
  assert.equal(memoryResponse.status, 404);
});

test("uploaded media must match its declared content type", async () => {
  const form = new FormData();
  form.append("scope", "profile/avatar");
  form.append("file", new Blob(["not a png"], { type: "image/png" }), "avatar.png");
  const response = await app.fetch(new Request("https://api.anyibj.cn/assets", {
    method: "POST",
    headers: { Authorization: `Bearer ${userToken()}` },
    body: form
  }), testEnv());
  assert.equal(response.status, 415);
  assert.equal((await response.json()).error, "file_signature_mismatch");
});

test("regular users only receive their own volunteer applications", async () => {
  let applicationQuery = "";
  let applicationBindings = [];
  const db = {
    dialect: "sqlite",
    prepare(query) {
      let bindings = [];
      return {
        bind(...values) {
          bindings = values;
          return this;
        },
        async first() {
          if (query.includes("FROM users WHERE id = ?")) {
            return {
              id: "user-1",
              username: "tester",
              display_name: "Tester",
              avatar_url: null,
              role: "user"
            };
          }
          return null;
        },
        async all() {
          if (query.includes("FROM community_volunteer_applications a")) {
            applicationQuery = query;
            applicationBindings = bindings;
          }
          return { results: [], success: true, meta: {} };
        },
        async run() { return { success: true, meta: { changes: 1 } }; }
      };
    },
    async batch() { return []; },
    async hasColumn() { return true; }
  };

  const response = await app.fetch(new Request(
    "https://api.anyibj.cn/community/volunteer/applications?status=all",
    { headers: { Authorization: `Bearer ${userToken()}` } }
  ), testEnv({ DB: db }));
  assert.equal(response.status, 200);
  assert.match(applicationQuery, /WHERE a\.user_id = \?/);
  assert.deepEqual(applicationBindings, ["user-1"]);
});

test("closed volunteer recruitment rejects new applications", async () => {
  const db = volunteerFlowDatabase({ volunteerStatus: "closed" });
  const response = await app.fetch(new Request(
    "https://api.anyibj.cn/community/volunteer/vol-1/applications",
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${userToken()}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ name: "Tester", phone: "13800000000", note: "weekends" })
    }
  ), testEnv({ DB: db }));
  assert.equal(response.status, 409);
  assert.equal((await response.json()).error, "community_volunteer_closed");
});

test("pending volunteer applications cannot be submitted twice", async () => {
  const db = volunteerFlowDatabase({ volunteerStatus: "open", applicationStatus: "pending" });
  const response = await app.fetch(new Request(
    "https://api.anyibj.cn/community/volunteer/vol-1/applications",
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${userToken()}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ name: "Tester", phone: "13800000000", note: "weekends" })
    }
  ), testEnv({ DB: db }));
  assert.equal(response.status, 409);
  assert.equal((await response.json()).error, "volunteer_application_already_pending");
});

test("volunteer application insert rechecks that recruitment is still open", async () => {
  const db = volunteerFlowDatabase({ volunteerStatus: "open", insertChanges: 0 });
  const response = await app.fetch(new Request(
    "https://api.anyibj.cn/community/volunteer/vol-1/applications",
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${userToken()}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ name: "Tester", phone: "13800000000", note: "weekends" })
    }
  ), testEnv({ DB: db }));
  assert.equal(response.status, 409);
  assert.equal((await response.json()).error, "community_volunteer_closed");
});

test("concurrent first volunteer applications return a duplicate conflict", async () => {
  let lookups = 0;
  const duplicate = Object.assign(new Error("UNIQUE constraint failed"), {
    code: "SQLITE_CONSTRAINT_UNIQUE"
  });
  const db = volunteerFlowDatabase({
    volunteerStatus: "open",
    insertError: duplicate,
    applicationStatusLookup() {
      lookups += 1;
      return lookups === 1 ? null : "pending";
    }
  });
  const response = await app.fetch(new Request(
    "https://api.anyibj.cn/community/volunteer/vol-1/applications",
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${userToken()}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ name: "Tester", phone: "13800000000", note: "weekends" })
    }
  ), testEnv({ DB: db }));
  assert.equal(response.status, 409);
  assert.equal((await response.json()).error, "volunteer_application_already_pending");
});

test("an open volunteer recruitment accepts a first application", async () => {
  let applicationStatus = null;
  const db = volunteerFlowDatabase({
    volunteerStatus: "open",
    applicationStatusLookup() { return applicationStatus; },
    currentApplicationStatus() { return applicationStatus; },
    onInsert() { applicationStatus = "pending"; }
  });
  const response = await app.fetch(new Request(
    "https://api.anyibj.cn/community/volunteer/vol-1/applications",
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${userToken()}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ name: "Tester", phone: "13800000000", note: "weekends" })
    }
  ), testEnv({ DB: db }));
  assert.equal(response.status, 201);
  assert.equal((await response.json()).application.status, "pending");
});

test("users can cancel their own volunteer application", async () => {
  let applicationStatus = "approved";
  const db = volunteerFlowDatabase({
    volunteerStatus: "open",
    applicationStatus,
    onCancel() { applicationStatus = "cancelled"; },
    currentApplicationStatus() { return applicationStatus; }
  });
  const response = await app.fetch(new Request(
    "https://api.anyibj.cn/community/volunteer/applications/app-1",
    {
      method: "DELETE",
      headers: { Authorization: `Bearer ${userToken()}` }
    }
  ), testEnv({ DB: db }));
  assert.equal(response.status, 200);
  assert.equal((await response.json()).application.status, "cancelled");
});

function volunteerFlowDatabase({
  volunteerStatus,
  applicationStatus = null,
  onCancel = () => {},
  currentApplicationStatus = () => applicationStatus,
  applicationStatusLookup = currentApplicationStatus,
  insertChanges = 1,
  insertError = null,
  onInsert = () => {}
}) {
  return {
    dialect: "sqlite",
    prepare(query) {
      return {
        bind() { return this; },
        async first() {
          if (query.includes("FROM users WHERE id = ?")) {
            return {
              id: "user-1",
              username: "tester",
              display_name: "Tester",
              avatar_url: null,
              role: "user"
            };
          }
          if (query.includes("FROM community_volunteer_posts WHERE id = ?")) {
            return {
              id: "vol-1",
              title: "Community help",
              body: "Help the community",
              contact: null,
              image_url: null,
              status: volunteerStatus,
              deadline_at: null,
              created_at: "2026-08-01T00:00:00.000Z"
            };
          }
          if (query.includes("WHERE volunteer_post_id = ? AND user_id = ?")) {
            const current = applicationStatusLookup();
            return current ? { id: "app-1", status: current } : null;
          }
          if (query.includes("SELECT id, user_id, status FROM community_volunteer_applications")) {
            return { id: "app-1", user_id: "user-1", status: currentApplicationStatus() };
          }
          if (query.includes("FROM community_volunteer_applications a")) {
            return {
              id: "app-1",
              volunteer_post_id: "vol-1",
              volunteer_title: "Community help",
              user_id: "user-1",
              username: "tester",
              display_name: "Tester",
              avatar_url: null,
              name: "Tester",
              phone: "13800000000",
              note: "weekends",
              status: currentApplicationStatus(),
              reviewer_id: null,
              reviewed_at: null,
              created_at: "2026-08-01T00:00:00.000Z",
              updated_at: "2026-08-30T00:00:00.000Z"
            };
          }
          return null;
        },
        async all() { return { results: [], success: true, meta: {} }; },
        async run() {
          if (query.includes("SET status = 'cancelled'")) onCancel();
          if (query.includes("INSERT INTO community_volunteer_applications")) {
            if (insertError) throw insertError;
            onInsert();
            return { success: true, meta: { changes: insertChanges } };
          }
          return { success: true, meta: { changes: 1 } };
        }
      };
    },
    async batch() { return []; },
    async hasColumn() { return true; }
  };
}
