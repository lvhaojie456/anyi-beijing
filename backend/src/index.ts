import { Hono } from "hono";
import type { Context, MiddlewareHandler } from "hono";

type Role = "user" | "admin";

type Bindings = {
  DB: AppDatabase;
  ASSETS: AssetBucket;
  AUTH_SECRET: string;
  PUBLIC_ASSET_BASE_URL?: string;
  ALLOWED_ORIGINS?: string;
  RATE_LIMIT_ENABLED?: string;
  PAYMENT_ENABLED?: string;
  PAYMENT_WEBHOOK_SECRET?: string;
  AI_BASE_URL?: string;
  AI_API_KEY?: string;
  AI_MODEL?: string;
  AI_IMAGE_BASE_URL?: string;
  AI_IMAGE_API_KEY?: string;
  AI_IMAGE_MODEL?: string;
  AI_VISION_MODEL?: string;
  DIGITAL_HUMAN_CHAT_MODEL?: string;
  AI_TIMEOUT_MS?: string;
  WECHAT_APP_ID?: string;
  WECHAT_APP_SECRET?: string;
  LEGAL_OPERATOR_NAME?: string;
  LEGAL_CONTACT_EMAIL?: string;
  LEGAL_CONTACT_PHONE?: string;
  LEGAL_EFFECTIVE_DATE?: string;
  VTUBER_URL?: string;
  VTUBER_ENABLED?: string;
};

type AppPreparedStatement = {
  bind(...values: unknown[]): AppPreparedStatement;
  first<T = unknown>(): Promise<T | null>;
  all<T = unknown>(): Promise<{ results: T[]; success: boolean; meta: Record<string, unknown> }>;
  run(): Promise<{ success: boolean; meta: Record<string, unknown> }>;
};

type DatabaseDialect = "sqlite" | "mysql";

type AppDatabase = {
  readonly dialect: DatabaseDialect;
  prepare(query: string): AppPreparedStatement;
  batch(statements: AppPreparedStatement[]): Promise<unknown>;
  hasColumn(tableName: string, columnName: string): Promise<boolean>;
};

type AssetBucket = {
  put(
    key: string,
    value: ArrayBuffer | Uint8Array | BufferSource,
    options?: {
      httpMetadata?: { contentType?: string };
      customMetadata?: Record<string, string>;
    }
  ): Promise<unknown>;
  get(key: string): Promise<{
    body: BodyInit;
    httpMetadata?: { contentType?: string };
  } | null>;
  delete(key: string): Promise<unknown>;
};

type AuthUser = {
  id: string;
  username: string;
  displayName: string;
  role: Role;
  avatarUrl: string | null;
};

type AppEnv = {
  Bindings: Bindings;
  Variables: {
    user: AuthUser;
  };
};

type UserRow = {
  id: string;
  username: string;
  display_name: string;
  role: Role;
  avatar_url: string | null;
  wechat_openid?: string | null;
  wechat_unionid?: string | null;
  wechat_nickname?: string | null;
};

type LoginUserRow = UserRow & {
  password_hash: string;
};

type WechatTokenResponse = {
  access_token?: string;
  expires_in?: number;
  refresh_token?: string;
  openid?: string;
  scope?: string;
  unionid?: string;
  errcode?: number;
  errmsg?: string;
};

type WechatUserInfoResponse = {
  openid?: string;
  nickname?: string;
  headimgurl?: string;
  unionid?: string;
  errcode?: number;
  errmsg?: string;
};

type MemorialRow = {
  id: string;
  owner_id: string;
  name: string;
  image_url: string | null;
  flower_until_json: string;
  candle_until: number;
  candle_until_json?: string | null;
  fruit_offerings_json?: string | null;
  incense_until?: number | null;
  created_at: string;
  updated_at: string;
};

type CommunityPostRow = {
  id: string;
  user_id: string;
  username: string;
  display_name: string;
  avatar_url: string | null;
  content: string;
  image_urls?: string | null;
  created_at: string;
  updated_at: string;
  like_count?: number;
  comment_count?: number;
  liked_by_me?: number;
  status?: "pending" | "approved" | "rejected" | "blocked";
  moderation_reason?: string | null;
  moderated_at?: string | null;
  moderated_by?: string | null;
};

type CommunityCommentRow = {
  id: string;
  post_id: string;
  user_id: string;
  username: string;
  display_name: string;
  avatar_url: string | null;
  content: string;
  created_at: string;
  updated_at: string;
  status?: "pending" | "approved" | "rejected" | "blocked";
  moderation_reason?: string | null;
  moderated_at?: string | null;
  moderated_by?: string | null;
};

type CommunityReportRow = {
  id: string;
  reporter_id: string;
  target_type: "post" | "comment";
  target_id: string;
  reason: string;
  status: "pending" | "actioned" | "dismissed";
  reviewer_id: string | null;
  reviewed_at: string | null;
  created_at: string;
  updated_at: string;
  reporter_username?: string;
  reporter_display_name?: string;
};

type UserModerationRow = {
  user_id: string;
  status: "blocked" | "banned";
  reason: string | null;
  expires_at: string | null;
  updated_by: string | null;
  updated_at: string;
};

type CommunityVolunteerRow = {
  id: string;
  title: string;
  body: string;
  contact: string | null;
  image_url?: string | null;
  created_at: string;
};

type CommunityVolunteerApplicationRow = {
  id: string;
  volunteer_post_id: string;
  volunteer_title: string;
  user_id: string;
  username?: string;
  display_name?: string;
  avatar_url?: string | null;
  name: string;
  phone: string;
  note: string | null;
  status: "pending" | "approved" | "rejected";
  reviewer_id: string | null;
  reviewed_at: string | null;
  created_at: string;
  updated_at: string;
};

type AiProfileRow = {
  user_id: string;
  gender: string;
  relation: string;
  avatar_url: string | null;
  smile_avatar_url: string | null;
  avatar_motion_json: string;
  paid_unlocked: number;
  photo_count: number;
  voice_count: number;
  moment_count: number;
  generated: number;
  updated_at: string;
};

type AiCompanionRow = AiProfileRow & {
  id: string;
  display_name: string;
  avatar_style_json: string;
  kernel_json: string;
  is_default: number;
  created_at: string;
};

type AiChatRow = {
  id: string;
  companion_id?: string | null;
  sender: "user" | "ai";
  content: string;
  created_at: string;
};

type DigitalHumanChatPersona = {
  id: "grandpa" | "grandma";
  label: string;
  address: string;
  tone: string;
};

type DigitalHumanChatHistoryMessage = {
  role: "user" | "assistant";
  content: string;
};

const encoder = new TextEncoder();
const decoder = new TextDecoder();
const flowerDurationMs = 10 * 60 * 1000;
const memorialOfferingLimit = 2;
const memorialFlowerTypes = ["wreath", "chrysanthemum", "lily"] as const;
type MemorialFlowerType = (typeof memorialFlowerTypes)[number];
type MemorialFlowerOffering = {
  type: MemorialFlowerType;
  until: number;
};
const durianOfferingFeature = "offering_durian";
const memorialFruitTypes = ["apple", "durian"] as const;
type MemorialFruitType = (typeof memorialFruitTypes)[number];
type MemorialFruitOffering = {
  type: MemorialFruitType;
  until: number;
};
const memorialFruitLimits: Record<MemorialFruitType, number> = {
  apple: 3,
  durian: 1
};

const allowedAssetTypes = new Map<string, string>([
  ["image/jpeg", ".jpg"],
  ["image/png", ".png"],
  ["image/webp", ".webp"],
  ["audio/mpeg", ".mp3"],
  ["audio/mp4", ".m4a"],
  ["audio/wav", ".wav"],
  ["audio/x-wav", ".wav"],
  ["text/plain", ".txt"]
]);

class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    readonly details?: unknown
  ) {
    super(code);
  }
}

const app = new Hono<AppEnv>();

app.use("*", corsMiddleware);
app.use("*", rateLimitMiddleware);

app.use("*", async (c, next) => {
  await next();
  const contentType = c.res.headers.get("Content-Type");
  if (contentType?.startsWith("application/json") && !contentType.includes("charset")) {
    c.res.headers.set("Content-Type", "application/json; charset=utf-8");
  }
  c.res.headers.set("X-Content-Type-Options", "nosniff");
  c.res.headers.set("X-Frame-Options", "DENY");
  c.res.headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
  if (c.req.header("X-Forwarded-Proto") === "https") {
    c.res.headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
  }
});

async function corsMiddleware(c: Context<AppEnv>, next: () => Promise<void>) {
  const origin = c.req.header("Origin");
  const allowed = !origin || isAllowedOrigin(c, origin);
  if (c.req.method === "OPTIONS") {
    if (!allowed) {
      return jsonResponse({ error: "cors_origin_forbidden" }, 403);
    }
    return new Response(null, {
      status: 204,
      headers: corsResponseHeaders(origin)
    });
  }

  if (!allowed) {
    throw new ApiError(403, "cors_origin_forbidden");
  }

  await next();
  if (origin) {
    const headers = corsResponseHeaders(origin);
    headers.forEach((value, key) => c.res.headers.set(key, value));
  }
}

async function rateLimitMiddleware(c: Context<AppEnv>, next: () => Promise<void>) {
  if (c.req.method === "OPTIONS" || c.env.RATE_LIMIT_ENABLED === "false" || isRateLimitExempt(c)) {
    await next();
    return;
  }

  const policy = rateLimitPolicy(c);
  const windowStart = Math.floor(Date.now() / policy.windowMs) * policy.windowMs;
  const bucketKey = `${clientIp(c)}:${policy.scope}`;
  const now = new Date().toISOString();

  await c.env.DB.prepare(rateLimitUpsertSql(c.env.DB.dialect))
    .bind(bucketKey, policy.routeKey, windowStart, now)
    .run();

  const row = await c.env.DB.prepare(
    "SELECT count FROM rate_limits WHERE bucket_key = ? AND route_key = ? AND window_start = ?"
  )
    .bind(bucketKey, policy.routeKey, windowStart)
    .first<{ count: number }>();

  const count = row?.count || 0;
  c.header("X-RateLimit-Limit", String(policy.limit));
  c.header("X-RateLimit-Remaining", String(Math.max(policy.limit - count, 0)));

  if (count > policy.limit) {
    throw new ApiError(429, "rate_limit_exceeded", {
      limit: policy.limit,
      windowSeconds: policy.windowMs / 1000
    });
  }

  if (Math.random() < 0.01) {
    const cutoff = new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString();
    await c.env.DB.prepare("DELETE FROM rate_limits WHERE updated_at < ?").bind(cutoff).run();
  }

  await next();
}

function rateLimitUpsertSql(dialect: DatabaseDialect) {
  if (dialect === "mysql") {
    return `INSERT INTO rate_limits (bucket_key, route_key, window_start, count, updated_at)
     VALUES (?, ?, ?, 1, ?)
     ON DUPLICATE KEY UPDATE count = count + 1, updated_at = VALUES(updated_at)`;
  }

  return `INSERT INTO rate_limits (bucket_key, route_key, window_start, count, updated_at)
     VALUES (?, ?, ?, 1, ?)
     ON CONFLICT(bucket_key, route_key, window_start)
     DO UPDATE SET count = count + 1, updated_at = excluded.updated_at`;
}

function featureUnlockInsertSql(dialect: DatabaseDialect) {
  if (dialect === "mysql") {
    return "INSERT IGNORE INTO feature_unlocks (user_id, feature, created_at) VALUES (?, ?, ?)";
  }

  return "INSERT OR IGNORE INTO feature_unlocks (user_id, feature, created_at) VALUES (?, ?, ?)";
}

function isPaidFeature(feature: string) {
  return feature === "hall_more" || feature === durianOfferingFeature;
}

function saveAiCompanionSql(dialect: DatabaseDialect) {
  const insert = `INSERT INTO ai_companions (
      id, user_id, display_name, gender, relation, avatar_url, smile_avatar_url, avatar_motion_json, paid_unlocked,
      photo_count, voice_count, moment_count, generated, avatar_style_json, kernel_json, is_default, created_at, updated_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`;

  if (dialect === "mysql") {
    const mysqlInsert = `INSERT INTO ai_companions (
      id, user_id, display_name, gender, relation, avatar_url, smile_avatar_url, avatar_motion_json, paid_unlocked,
      photo_count, voice_count, moment_count, \`generated\`, avatar_style_json, kernel_json, is_default, created_at, updated_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`;

    return `${mysqlInsert}
    ON DUPLICATE KEY UPDATE
      display_name = VALUES(display_name),
      gender = VALUES(gender),
      relation = VALUES(relation),
      avatar_url = VALUES(avatar_url),
      smile_avatar_url = VALUES(smile_avatar_url),
      avatar_motion_json = VALUES(avatar_motion_json),
      paid_unlocked = VALUES(paid_unlocked),
      photo_count = VALUES(photo_count),
      voice_count = VALUES(voice_count),
      moment_count = VALUES(moment_count),
      \`generated\` = VALUES(\`generated\`),
      avatar_style_json = VALUES(avatar_style_json),
      kernel_json = VALUES(kernel_json),
      is_default = VALUES(is_default),
      updated_at = VALUES(updated_at)`;
  }

  return `${insert}
    ON CONFLICT(id) DO UPDATE SET
      display_name = excluded.display_name,
      gender = excluded.gender,
      relation = excluded.relation,
      avatar_url = excluded.avatar_url,
      smile_avatar_url = excluded.smile_avatar_url,
      avatar_motion_json = excluded.avatar_motion_json,
      paid_unlocked = excluded.paid_unlocked,
      photo_count = excluded.photo_count,
      voice_count = excluded.voice_count,
      moment_count = excluded.moment_count,
      generated = excluded.generated,
      avatar_style_json = excluded.avatar_style_json,
      kernel_json = excluded.kernel_json,
      is_default = excluded.is_default,
      updated_at = excluded.updated_at`;
}

app.onError((error) => {
  if (error instanceof ApiError) {
    return jsonResponse({ error: error.code, details: error.details }, error.status);
  }

  console.error(error);
  return jsonResponse({ error: "internal_error" }, 500);
});

app.get("/health", (c) =>
  c.json({
    ok: true,
    service: "anyi-memorial-api",
    time: new Date().toISOString()
  })
);

app.get("/app/config", (c) =>
  c.json({
    digitalHuman: digitalHumanConfig(c),
    wechat: {
      enabled: Boolean(c.env.WECHAT_APP_ID?.trim() && c.env.WECHAT_APP_SECRET?.trim())
    },
    payments: {
      enabled: readEnvBoolean(c.env.PAYMENT_ENABLED, false)
    }
  })
);

app.get("/app/digital-human/status", async (c) => {
  const config = digitalHumanConfig(c);
  const page = await checkDigitalHumanPage(config.url, config.enabled);
  return c.json({
    ok: config.enabled && page.ok,
    digitalHuman: {
      enabled: config.enabled,
      url: config.url
    },
    checks: {
      page
    },
    checkedAt: new Date().toISOString()
  });
});

app.post("/crash-reports", async (c) => {
  const user = await optionalAuthUser(c);
  const body = await parseJson(c);
  const platform = readString(body, "platform", { max: 40 }) || "android";
  const appVersion = readString(body, "appVersion", { max: 40 }) || null;
  const deviceModel = readString(body, "deviceModel", { max: 120 }) || null;
  const osVersion = readString(body, "osVersion", { max: 80 }) || null;
  const errorType = readString(body, "errorType", { max: 160 }) || "unknown";
  const message = readString(body, "message", { max: 1000 }) || null;
  const stackTrace = readString(body, "stackTrace", { max: 12000 }) || null;

  await c.env.DB.prepare(
    `INSERT INTO crash_reports (
      id, user_id, platform, app_version, device_model, os_version,
      error_type, message, stack_trace, created_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  )
    .bind(
      crypto.randomUUID(),
      user?.id || null,
      platform,
      appVersion,
      deviceModel,
      osVersion,
      errorType,
      message,
      stackTrace,
      new Date().toISOString()
    )
    .run();

  return c.json({ ok: true }, 201);
});

const requireAuth: MiddlewareHandler<AppEnv> = async (c, next) => {
  const header = c.req.header("Authorization") || "";
  const token = header.startsWith("Bearer ") ? header.slice("Bearer ".length) : "";
  if (!token) {
    throw new ApiError(401, "missing_token");
  }

  const user = await verifyToken(c.env, token);
  await assertUserCanUseAccount(c, user);
  c.set("user", user);
  await next();
};

app.post("/app/digital-human/chat", requireAuth, async (c) => {
  const body = await parseJson(c);
  const characterId = readString(body, "characterId", { required: true, max: 40 });
  const persona = digitalHumanChatPersona(characterId);
  if (!persona) {
    throw new ApiError(400, "digital_human_character_not_supported");
  }
  const message = readString(body, "message", { required: true, max: 500 });
  const history = readDigitalHumanChatHistory(body);
  const reply = await requestDigitalHumanChatReply(c.env, persona, history, message);
  const now = Date.now();
  return c.json({
    characterId: persona.id,
    model: digitalHumanChatModel(c.env),
    message: {
      id: crypto.randomUUID(),
      sender: "assistant",
      content: reply,
      createdAt: now
    }
  });
});

app.post("/auth/register", async (c) => {
  const body = await parseJson(c);
  const username = readString(body, "username", { required: true, max: 32 }).toLowerCase();
  const password = readString(body, "password", { required: true, max: 128 });
  const displayName = readString(body, "displayName", { max: 40 }) || username;

  if (body.acceptedTerms !== true || body.acceptedPrivacy !== true) {
    throw new ApiError(400, "terms_approval_required");
  }

  if (!/^[a-z0-9_]{3,32}$/.test(username)) {
    throw new ApiError(400, "invalid_username");
  }

  if (password.length < 8) {
    throw new ApiError(400, "weak_password");
  }

  const existing = await c.env.DB.prepare("SELECT id FROM users WHERE username = ?")
    .bind(username)
    .first<{ id: string }>();
  if (existing) {
    throw new ApiError(409, "username_exists");
  }

  const id = crypto.randomUUID();
  // Public registration can never create an administrator. Promote an existing
  // account through an authenticated operator/SQL procedure instead.
  const role: Role = "user";
  const passwordHash = await hashPassword(password);
  const createdAt = new Date().toISOString();

  await c.env.DB.prepare(
    `INSERT INTO users (
      id, username, password_hash, display_name, role, created_at,
      terms_accepted_at, privacy_accepted_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
  )
    .bind(id, username, passwordHash, displayName, role, createdAt, createdAt, createdAt)
    .run();

  const user = { id, username, displayName, role, avatarUrl: null };
  const token = await createToken(c.env, user);
  return c.json({ user, token }, 201);
});

app.post("/auth/login", async (c) => {
  const body = await parseJson(c);
  const username = readString(body, "username", { required: true, max: 32 }).toLowerCase();
  const password = readString(body, "password", { required: true, max: 128 });

  const row = await c.env.DB.prepare(
    "SELECT id, username, display_name, avatar_url, role, password_hash FROM users WHERE username = ? AND deleted_at IS NULL"
  )
    .bind(username)
    .first<LoginUserRow>();

  if (!row || !(await verifyPassword(password, row.password_hash))) {
    throw new ApiError(401, "invalid_credentials");
  }

  const user = toAuthUser(row);
  const token = await createToken(c.env, user);
  return c.json({ user, token });
});

app.post("/auth/wechat", async (c) => {
  const appId = c.env.WECHAT_APP_ID?.trim();
  const appSecret = c.env.WECHAT_APP_SECRET?.trim();
  if (!appId || !appSecret) {
    throw new ApiError(503, "wechat_login_not_configured");
  }

  const body = await parseJson(c);
  if (body.acceptedTerms !== true || body.acceptedPrivacy !== true) {
    throw new ApiError(400, "terms_approval_required");
  }
  const code = readString(body, "code", { required: true, max: 256 });
  const tokenPayload = await exchangeWechatCode(appId, appSecret, code);
  const openid = tokenPayload.openid || "";
  const unionid = tokenPayload.unionid || "";
  if (!openid) {
    throw new ApiError(401, "wechat_code_invalid", tokenPayload);
  }

  const wechatProfile = await fetchWechatUserInfo(tokenPayload.access_token || "", openid);
  const displayName = sanitizeWechatName(wechatProfile.nickname || "微信用户");
  const avatarUrl = normalizeWechatAvatar(wechatProfile.headimgurl || "");
  const finalUnionid = unionid || wechatProfile.unionid || null;
  const row = await findWechatUser(c, openid, finalUnionid);
  const now = new Date().toISOString();

  let userRow: UserRow | null;
  if (row) {
    await c.env.DB.prepare(
      `UPDATE users
       SET display_name = ?, avatar_url = COALESCE(?, avatar_url),
           wechat_openid = COALESCE(wechat_openid, ?),
           wechat_unionid = COALESCE(wechat_unionid, ?),
           wechat_nickname = ?, terms_accepted_at = COALESCE(terms_accepted_at, ?),
           privacy_accepted_at = COALESCE(privacy_accepted_at, ?), deleted_at = NULL
       WHERE id = ?`
    )
      .bind(displayName, avatarUrl || null, openid, finalUnionid, displayName, now, now, row.id)
      .run();
    userRow = await c.env.DB.prepare(
      "SELECT id, username, display_name, avatar_url, role FROM users WHERE id = ?"
    )
      .bind(row.id)
      .first<UserRow>();
  } else {
    const id = crypto.randomUUID();
    const username = await createWechatUsername(c, finalUnionid || openid);
    const passwordHash = `wechat$${await sha256Hex(`${openid}:${now}`)}`;
    await c.env.DB.prepare(
      `INSERT INTO users (
        id, username, password_hash, display_name, role, avatar_url,
        created_at, terms_accepted_at, privacy_accepted_at,
        wechat_openid, wechat_unionid, wechat_nickname
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
      .bind(
        id,
        username,
        passwordHash,
        displayName,
        "user",
        avatarUrl || null,
        now,
        now,
        now,
        openid,
        finalUnionid,
        displayName
      )
      .run();
    userRow = await c.env.DB.prepare(
      "SELECT id, username, display_name, avatar_url, role FROM users WHERE id = ?"
    )
      .bind(id)
      .first<UserRow>();
  }

  const user = toAuthUser(requireRow(userRow));
  const token = await createToken(c.env, user);
  return c.json({ user, token });
});

app.get("/me", requireAuth, (c) => c.json({ user: c.get("user") }));

const updateCurrentUserHandler = async (c: Context<AppEnv>) => {
  const user = c.get("user");
  const body = await parseJson(c);
  const displayName = "displayName" in body
    ? readString(body, "displayName", { max: 40 }) || user.displayName
    : user.displayName;
  const avatarUrl = "avatarUrl" in body
    ? readString(body, "avatarUrl", { max: 500 }) || null
    : user.avatarUrl;
  const now = new Date().toISOString();

  await prepareProfileAvatar(c, user.id, avatarUrl);
  await c.env.DB.prepare("UPDATE users SET display_name = ?, avatar_url = ? WHERE id = ?")
    .bind(displayName, avatarUrl, user.id)
    .run();

  await writeAudit(c, {
    action: "user.profile.update",
    targetType: "user",
    targetId: user.id,
    metadata: {
      displayNameChanged: displayName !== user.displayName,
      avatarChanged: avatarUrl !== user.avatarUrl,
      updatedAt: now
    }
  });

  const row = await c.env.DB.prepare(
    "SELECT id, username, display_name, avatar_url, role FROM users WHERE id = ? AND deleted_at IS NULL"
  )
    .bind(user.id)
    .first<UserRow>();

  return c.json({ user: toAuthUser(requireRow(row)) });
};

app.patch("/me", requireAuth, updateCurrentUserHandler);
app.put("/me", requireAuth, updateCurrentUserHandler);

app.delete("/me", requireAuth, async (c) => {
  const user = c.get("user");
  if (user.role === "admin") {
    throw new ApiError(403, "admin_account_deletion_forbidden");
  }

  const assetRows = await c.env.DB.prepare("SELECT asset_key FROM assets WHERE owner_id = ?")
    .bind(user.id)
    .all<{ asset_key: string }>();

  await writeAudit(c, {
    action: "user.account.delete",
    targetType: "user",
    targetId: user.id,
    metadata: { hardDelete: true }
  });

  for (const row of assetRows.results) {
    await queueAssetDelete(c, row.asset_key, user.id, "account_deleted");
  }

  // Remove user-owned records before the user row so this also works with the
  // stricter foreign keys used by the MySQL deployment.
  const cleanupStatements = [
    [
      "DELETE FROM community_reports WHERE target_type = 'comment' AND target_id IN (SELECT id FROM community_post_comments WHERE user_id = ?)",
      [user.id]
    ],
    ["DELETE FROM community_post_comments WHERE user_id = ?", [user.id]],
    ["DELETE FROM community_post_likes WHERE user_id = ?", [user.id]],
    [
      "DELETE FROM community_post_comments WHERE post_id IN (SELECT id FROM community_posts WHERE user_id = ?)",
      [user.id]
    ],
    [
      "DELETE FROM community_post_likes WHERE post_id IN (SELECT id FROM community_posts WHERE user_id = ?)",
      [user.id]
    ],
    ["DELETE FROM community_reports WHERE reporter_id = ?", [user.id]],
    [
      "DELETE FROM community_reports WHERE target_type = 'post' AND target_id IN (SELECT id FROM community_posts WHERE user_id = ?)",
      [user.id]
    ],
    ["DELETE FROM community_posts WHERE user_id = ?", [user.id]],
    ["DELETE FROM community_volunteer_applications WHERE user_id = ?", [user.id]],
    ["DELETE FROM ai_chat_messages WHERE user_id = ?", [user.id]],
    ["DELETE FROM ai_companions WHERE user_id = ?", [user.id]],
    ["DELETE FROM ai_profiles WHERE user_id = ?", [user.id]],
    ["DELETE FROM feature_unlocks WHERE user_id = ?", [user.id]],
    ["DELETE FROM memorials WHERE owner_id = ?", [user.id]],
    [
      "DELETE FROM order_messages WHERE sender_id = ? OR order_id IN (SELECT id FROM ritual_orders WHERE user_id = ?)",
      [user.id, user.id]
    ],
    [
      "DELETE FROM payment_events WHERE order_id IN (SELECT id FROM ritual_orders WHERE user_id = ?) OR order_id IN (SELECT id FROM talisman_orders WHERE user_id = ?)",
      [user.id, user.id]
    ],
    ["DELETE FROM ritual_orders WHERE user_id = ?", [user.id]],
    ["DELETE FROM talisman_orders WHERE user_id = ?", [user.id]],
    ["DELETE FROM upload_reviews WHERE owner_id = ?", [user.id]],
    ["DELETE FROM crash_reports WHERE user_id = ?", [user.id]],
    ["DELETE FROM user_moderation WHERE user_id = ?", [user.id]],
    ["UPDATE asset_delete_queue SET owner_id = NULL WHERE owner_id = ?", [user.id]],
    ["DELETE FROM assets WHERE owner_id = ?", [user.id]],
    ["UPDATE audit_logs SET actor_id = NULL WHERE actor_id = ?", [user.id]],
    ["DELETE FROM users WHERE id = ?", [user.id]]
  ] as Array<[string, unknown[]]>;

  for (const [query, params] of cleanupStatements) {
    await c.env.DB.prepare(query).bind(...params).run();
  }

  // Keep the response deliberately small: the token is invalid as soon as the
  // user row is removed and all private records have been cleaned up.
  return c.json({ ok: true });
});

app.get("/community/posts", requireAuth, async (c) => {
  const user = c.get("user");
  const visibilitySql = user.role === "admin"
    ? "1 = 1"
    : "(p.status = 'approved' OR p.user_id = ?)";
  const params: unknown[] = [user.id];
  if (user.role !== "admin") {
    params.push(user.id);
  }
  const rows = await c.env.DB.prepare(
    `SELECT p.*, u.username, u.display_name, u.avatar_url,
      (SELECT COUNT(*) FROM community_post_likes l WHERE l.post_id = p.id) AS like_count,
      (SELECT COUNT(*) FROM community_post_comments cc
        WHERE cc.post_id = p.id AND cc.status = 'approved') AS comment_count,
      CASE WHEN EXISTS (
        SELECT 1 FROM community_post_likes l WHERE l.post_id = p.id AND l.user_id = ?
      ) THEN 1 ELSE 0 END AS liked_by_me
     FROM community_posts p
     JOIN users u ON u.id = p.user_id
     WHERE u.deleted_at IS NULL AND ${visibilitySql}
     ORDER BY p.created_at DESC
     LIMIT 100`
  )
    .bind(...params)
    .all<CommunityPostRow>();

  return c.json({ posts: rows.results.map(serializeCommunityPost) });
});

app.post("/community/posts", requireAuth, async (c) => {
  const user = c.get("user");
  const body = await parseJson(c);
  const content = readString(body, "content", { max: 500 });
  const imageUrls = readStringList(body, "imageUrls", { maxItems: 9, maxLength: 500 });
  if (!content && imageUrls.length === 0) {
    throw new ApiError(400, "community_post_empty");
  }
  const moderation = await moderateCommunityPost(c, user, content, imageUrls);
  if (moderation.status === "rejected") {
    throw new ApiError(422, "community_content_rejected", { reason: moderation.reason });
  }
  const id = crypto.randomUUID();
  const now = new Date().toISOString();

  await c.env.DB.prepare(
    `INSERT INTO community_posts (
      id, user_id, content, image_urls, status, moderation_reason,
      created_at, updated_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
  )
    .bind(id, user.id, content, JSON.stringify(imageUrls), moderation.status, moderation.reason, now, now)
    .run();

  await setCommunityAssetVisibility(c, imageUrls, moderation.status === "approved" ? "public" : "private", user.id);

  await writeAudit(c, {
    action: "community.post.create",
    targetType: "community_post",
    targetId: id
  });

  const post = await loadCommunityPostForUser(c, id, user.id);
  return c.json({ post: serializeCommunityPost(post) }, 201);
});

app.delete("/community/posts/:id", requireAuth, async (c) => {
  const user = c.get("user");
  const post = await loadCommunityPostForUser(c, c.req.param("id"), user.id);
  if (post.user_id !== user.id && user.role !== "admin") {
    throw new ApiError(403, "community_post_delete_forbidden");
  }

  await c.env.DB.prepare("DELETE FROM community_post_comments WHERE post_id = ?")
    .bind(post.id)
    .run();
  await c.env.DB.prepare("DELETE FROM community_post_likes WHERE post_id = ?")
    .bind(post.id)
    .run();
  await c.env.DB.prepare("DELETE FROM community_posts WHERE id = ?")
    .bind(post.id)
    .run();

  await c.env.DB.prepare("DELETE FROM community_reports WHERE target_type = 'post' AND target_id = ?")
    .bind(post.id)
    .run();

  for (const url of parseStringArray(post.image_urls || "[]")) {
    const key = assetKeyFromUrl(url);
    if (key) {
      await setAssetVisibility(c, key, "private", post.user_id);
      await queueAssetDelete(c, key, post.user_id, "community_post_deleted");
    }
  }

  await writeAudit(c, {
    action: "community.post.delete",
    targetType: "community_post",
    targetId: post.id
  });

  return c.json({ deleted: true, postId: post.id });
});

app.get("/community/posts/:id/comments", requireAuth, async (c) => {
  const user = c.get("user");
  const post = await loadCommunityPostForUser(c, c.req.param("id"), user.id);
  const visibilitySql = user.role === "admin" || post.user_id === user.id
    ? "1 = 1"
    : "(cc.status = 'approved' OR cc.user_id = ?)";
  const params: unknown[] = [post.id];
  if (user.role !== "admin" && post.user_id !== user.id) {
    params.push(user.id);
  }
  const rows = await c.env.DB.prepare(
    `SELECT cc.*, u.username, u.display_name, u.avatar_url
     FROM community_post_comments cc
     JOIN users u ON u.id = cc.user_id
     WHERE cc.post_id = ? AND u.deleted_at IS NULL AND ${visibilitySql}
     ORDER BY cc.created_at ASC
     LIMIT 200`
  )
    .bind(...params)
    .all<CommunityCommentRow>();

  return c.json({ comments: rows.results.map(serializeCommunityComment) });
});

app.post("/community/posts/:id/comments", requireAuth, async (c) => {
  const user = c.get("user");
  const post = await loadCommunityPostForUser(c, c.req.param("id"), user.id);
  const body = await parseJson(c);
  const content = readString(body, "content", { required: true, max: 300 });
  const moderation = moderateCommunityText(content);
  if (moderation.status === "rejected") {
    throw new ApiError(422, "community_content_rejected", { reason: moderation.reason });
  }
  const id = crypto.randomUUID();
  const now = new Date().toISOString();

  await c.env.DB.prepare(
    `INSERT INTO community_post_comments (
      id, post_id, user_id, content, status, moderation_reason,
      created_at, updated_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
  )
    .bind(id, post.id, user.id, content, moderation.status, moderation.reason, now, now)
    .run();

  await writeAudit(c, {
    action: "community.comment.create",
    targetType: "community_post_comment",
    targetId: id,
    metadata: { postId: post.id }
  });

  const comment = await loadCommunityComment(c, id);
  const updatedPost = await loadCommunityPostForUser(c, post.id, user.id);
  return c.json({ comment: serializeCommunityComment(comment), post: serializeCommunityPost(updatedPost) }, 201);
});

app.delete("/community/posts/:postId/comments/:commentId", requireAuth, async (c) => {
  const user = c.get("user");
  const post = await loadCommunityPostForUser(c, c.req.param("postId"), user.id);
  const comment = await loadCommunityComment(c, c.req.param("commentId"));
  if (comment.post_id !== post.id) {
    throw new ApiError(404, "community_comment_not_found");
  }
  if (comment.user_id !== user.id && post.user_id !== user.id && user.role !== "admin") {
    throw new ApiError(403, "community_comment_delete_forbidden");
  }

  await c.env.DB.prepare("DELETE FROM community_post_comments WHERE id = ?")
    .bind(comment.id)
    .run();

  await c.env.DB.prepare("DELETE FROM community_reports WHERE target_type = 'comment' AND target_id = ?")
    .bind(comment.id)
    .run();

  await writeAudit(c, {
    action: "community.comment.delete",
    targetType: "community_post_comment",
    targetId: comment.id,
    metadata: { postId: post.id }
  });

  const updatedPost = await loadCommunityPostForUser(c, post.id, user.id);
  return c.json({ deleted: true, commentId: comment.id, post: serializeCommunityPost(updatedPost) });
});

app.post("/community/posts/:id/like", requireAuth, async (c) => {
  const user = c.get("user");
  const post = await loadCommunityPostForUser(c, c.req.param("id"), user.id);
  const existing = await c.env.DB.prepare(
    "SELECT post_id FROM community_post_likes WHERE post_id = ? AND user_id = ?"
  )
    .bind(post.id, user.id)
    .first<{ post_id: string }>();

  if (existing) {
    await c.env.DB.prepare("DELETE FROM community_post_likes WHERE post_id = ? AND user_id = ?")
      .bind(post.id, user.id)
      .run();
  } else {
    await c.env.DB.prepare(
      "INSERT INTO community_post_likes (post_id, user_id, created_at) VALUES (?, ?, ?)"
    )
      .bind(post.id, user.id, new Date().toISOString())
      .run();
  }

  const updated = await loadCommunityPostForUser(c, post.id, user.id);
  return c.json({ post: serializeCommunityPost(updated) });
});

app.post("/community/reports", requireAuth, async (c) => {
  const user = c.get("user");
  const body = await parseJson(c);
  const targetType = readString(body, "targetType", { required: true, max: 20 });
  const targetId = readString(body, "targetId", { required: true, max: 80 });
  const reason = readString(body, "reason", { required: true, max: 300 });
  if (targetType !== "post" && targetType !== "comment") {
    throw new ApiError(400, "invalid_community_report_target");
  }

  if (targetType === "post") {
    await loadCommunityPostForUser(c, targetId, user.id);
  } else {
    await loadCommunityComment(c, targetId);
  }

  const existing = await c.env.DB.prepare(
    "SELECT id FROM community_reports WHERE reporter_id = ? AND target_type = ? AND target_id = ?"
  )
    .bind(user.id, targetType, targetId)
    .first<{ id: string }>();
  if (existing) {
    throw new ApiError(409, "community_report_exists");
  }

  const now = new Date().toISOString();
  const id = crypto.randomUUID();
  await c.env.DB.prepare(
    `INSERT INTO community_reports (
      id, reporter_id, target_type, target_id, reason, status,
      reviewer_id, reviewed_at, created_at, updated_at
    ) VALUES (?, ?, ?, ?, ?, 'pending', NULL, NULL, ?, ?)`
  )
    .bind(id, user.id, targetType, targetId, reason, now, now)
    .run();

  await writeAudit(c, {
    action: "community.report.create",
    targetType: `community_${targetType}`,
    targetId,
    metadata: { reportId: id, reason }
  });
  return c.json({ reportId: id, status: "pending" }, 201);
});

app.get("/community/volunteer", requireAuth, async (c) => {
  const rows = await c.env.DB.prepare(
    "SELECT id, title, body, contact, image_url, created_at FROM community_volunteer_posts ORDER BY created_at DESC LIMIT 20"
  ).all<CommunityVolunteerRow>();
  const volunteers = rows.results.length > 0 ? rows.results.map(serializeCommunityVolunteer) : defaultCommunityVolunteers();
  return c.json({ volunteer: volunteers[0], volunteers });
});

app.post("/community/volunteer", requireAuth, async (c) => {
  const admin = requireAdmin(c);
  const body = await parseJson(c);
  const title = readString(body, "title", { required: true, max: 40 });
  const volunteerBody = readString(body, "body", { required: true, max: 500 });
  const contact = readString(body, "contact", { max: 160 }) || null;
  const imageUrl = readString(body, "imageUrl", { max: 1000 }) || null;
  if (imageUrl) {
    const key = assetKeyFromUrl(imageUrl);
    if (!key) throw new ApiError(400, "community_image_invalid");
    const asset = await c.env.DB.prepare(
      "SELECT owner_id FROM assets WHERE asset_key = ?"
    )
      .bind(key)
      .first<{ owner_id: string }>();
    if (!asset || asset.owner_id !== admin.id) {
      throw new ApiError(403, "community_image_not_owned");
    }
    const review = await c.env.DB.prepare(
      "SELECT status FROM upload_reviews WHERE asset_key = ? ORDER BY created_at DESC LIMIT 1"
    )
      .bind(key)
      .first<{ status: string }>();
    if (review && review.status !== "approved") {
      throw new ApiError(409, "community_media_not_approved");
    }
    await setAssetVisibility(c, key, "public", admin.id);
  }
  const id = crypto.randomUUID();
  const now = new Date().toISOString();

  await c.env.DB.prepare(
    "INSERT INTO community_volunteer_posts (id, admin_id, title, body, contact, image_url, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
  )
    .bind(id, admin.id, title, volunteerBody, contact, imageUrl, now)
    .run();

  await writeAudit(c, {
    action: "community.volunteer.create",
    targetType: "community_volunteer_post",
    targetId: id
  });

  return c.json(
    {
      volunteer: {
        id,
        title,
        body: volunteerBody,
        contact,
        imageUrl,
        createdAt: now
      }
    },
    201
  );
});

app.get("/community/volunteer/applications", requireAuth, async (c) => {
  requireAdmin(c);
  const status = c.req.query("status") || "pending";
  if (!["pending", "approved", "rejected", "all"].includes(status)) {
    throw new ApiError(400, "invalid_volunteer_application_status");
  }

  const sql = `SELECT a.*, u.username, u.display_name, u.avatar_url
    FROM community_volunteer_applications a
    JOIN users u ON u.id = a.user_id
    ${status === "all" ? "" : "WHERE a.status = ?"}
    ORDER BY
      CASE a.status WHEN 'pending' THEN 0 WHEN 'approved' THEN 1 ELSE 2 END,
      a.created_at DESC
    LIMIT 100`;
  const rows = status === "all"
    ? await c.env.DB.prepare(sql).all<CommunityVolunteerApplicationRow>()
    : await c.env.DB.prepare(sql).bind(status).all<CommunityVolunteerApplicationRow>();

  return c.json({ applications: rows.results.map(serializeCommunityVolunteerApplication) });
});

const reviewCommunityVolunteerApplicationHandler = async (c: Context<AppEnv>) => {
  const admin = requireAdmin(c);
  const body = await parseJson(c);
  const status = readString(body, "status", { required: true, max: 20 });
  if (!["approved", "rejected"].includes(status)) {
    throw new ApiError(400, "invalid_volunteer_application_status");
  }

  const existing = await c.env.DB.prepare(
    "SELECT id FROM community_volunteer_applications WHERE id = ?"
  )
    .bind(c.req.param("id"))
    .first<{ id: string }>();
  if (!existing) {
    throw new ApiError(404, "volunteer_application_not_found");
  }

  const now = new Date().toISOString();
  await c.env.DB.prepare(
    `UPDATE community_volunteer_applications
     SET status = ?, reviewer_id = ?, reviewed_at = ?, updated_at = ?
     WHERE id = ?`
  )
    .bind(status, admin.id, now, now, existing.id)
    .run();

  await writeAudit(c, {
    action: `community.volunteer.application.${status}`,
    targetType: "community_volunteer_application",
    targetId: existing.id
  });

  const application = await loadCommunityVolunteerApplication(c, existing.id);
  return c.json({ application: serializeCommunityVolunteerApplication(application) });
};

app.patch("/community/volunteer/applications/:id", requireAuth, reviewCommunityVolunteerApplicationHandler);
app.put("/community/volunteer/applications/:id", requireAuth, reviewCommunityVolunteerApplicationHandler);

app.post("/community/volunteer/:id/applications", requireAuth, async (c) => {
  const user = c.get("user");
  const volunteer = await loadCommunityVolunteerTarget(c, c.req.param("id"));
  const body = await parseJson(c);
  const name = readString(body, "name", { required: true, max: 40 });
  const phone = readString(body, "phone", { required: true, max: 40 });
  const note = readString(body, "note", { max: 500 }) || null;
  const now = new Date().toISOString();

  const existing = await c.env.DB.prepare(
    "SELECT id FROM community_volunteer_applications WHERE volunteer_post_id = ? AND user_id = ?"
  )
    .bind(volunteer.id, user.id)
    .first<{ id: string }>();

  let applicationId = existing?.id;
  if (applicationId) {
    await c.env.DB.prepare(
      `UPDATE community_volunteer_applications
       SET volunteer_title = ?, name = ?, phone = ?, note = ?, status = 'pending',
         reviewer_id = NULL, reviewed_at = NULL, updated_at = ?
       WHERE id = ?`
    )
      .bind(volunteer.title, name, phone, note, now, applicationId)
      .run();
  } else {
    applicationId = crypto.randomUUID();
    await c.env.DB.prepare(
      `INSERT INTO community_volunteer_applications (
        id, volunteer_post_id, volunteer_title, user_id, name, phone, note,
        status, reviewer_id, reviewed_at, created_at, updated_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, 'pending', NULL, NULL, ?, ?)`
    )
      .bind(applicationId, volunteer.id, volunteer.title, user.id, name, phone, note, now, now)
      .run();
  }

  await writeAudit(c, {
    action: "community.volunteer.application.create",
    targetType: "community_volunteer_application",
    targetId: applicationId,
    metadata: { volunteerPostId: volunteer.id }
  });

  const application = await loadCommunityVolunteerApplication(c, applicationId);
  return c.json({ application: serializeCommunityVolunteerApplication(application) }, existing ? 200 : 201);
});

app.get("/memorials", requireAuth, async (c) => {
  const user = c.get("user");
  const rows = await c.env.DB.prepare(
    "SELECT * FROM memorials WHERE owner_id = ? ORDER BY created_at DESC"
  )
    .bind(user.id)
    .all<MemorialRow>();
  return c.json({ memorials: rows.results.map(serializeMemorial) });
});

app.post("/memorials", requireAuth, async (c) => {
  const user = c.get("user");
  const body = await parseJson(c);
  const name = readString(body, "name", { required: true, max: 40 });
  const imageUrl = readString(body, "imageUrl", { max: 500 }) || null;
  const now = new Date().toISOString();
  const id = crypto.randomUUID();

  await c.env.DB.prepare(
    "INSERT INTO memorials (id, owner_id, name, image_url, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)"
  )
    .bind(id, user.id, name, imageUrl, now, now)
    .run();

  const row = await c.env.DB.prepare("SELECT * FROM memorials WHERE id = ?")
    .bind(id)
    .first<MemorialRow>();
  return c.json({ memorial: serializeMemorial(requireRow(row)) }, 201);
});

const updateMemorialHandler = async (c: Context<AppEnv>) => {
  const memorialId = c.req.param("id");
  if (!memorialId) {
    throw new ApiError(400, "memorial_id_required");
  }
  const row = await loadMemorial(c, memorialId);
  const body = await parseJson(c);
  const name = readString(body, "name", { max: 40 }) || row.name;
  const imageUrl = readString(body, "imageUrl", { max: 500 }) || row.image_url;

  await c.env.DB.prepare(
    "UPDATE memorials SET name = ?, image_url = ?, updated_at = ? WHERE id = ?"
  )
    .bind(name, imageUrl, new Date().toISOString(), row.id)
    .run();

  const updated = await c.env.DB.prepare("SELECT * FROM memorials WHERE id = ?")
    .bind(row.id)
    .first<MemorialRow>();
  return c.json({ memorial: serializeMemorial(requireRow(updated)) });
};

app.patch("/memorials/:id", requireAuth, updateMemorialHandler);
app.put("/memorials/:id", requireAuth, updateMemorialHandler);

app.post("/memorials/:id/flowers", requireAuth, async (c) => {
  const row = await loadMemorial(c, c.req.param("id"));
  const body = await parseJson(c);
  const type = readString(body, "type", { max: 30 }) || "wreath";
  const now = Date.now();

  if (!isMemorialFlowerType(type)) {
    throw new ApiError(400, "invalid_flower_type");
  }

  const current = parseMemorialFlowers(row.flower_until_json, now);

  if (current.length >= memorialOfferingLimit) {
    throw new ApiError(409, "flower_limit_reached");
  }

  current.push({ type, until: now + flowerDurationMs });
  await c.env.DB.prepare("UPDATE memorials SET flower_until_json = ?, updated_at = ? WHERE id = ?")
    .bind(JSON.stringify(current), new Date().toISOString(), row.id)
    .run();

  return c.json({ flowerUntil: current.map((item) => item.until), flowerOfferings: current });
});

app.post("/memorials/:id/candle", requireAuth, async (c) => {
  const row = await loadMemorial(c, c.req.param("id"));
  const now = Date.now();
  const current = parseNumberArray(row.candle_until_json || "[]").filter((timestamp) => timestamp > now);
  if (current.length === 0 && row.candle_until > now) {
    current.push(row.candle_until);
  }

  if (current.length >= memorialOfferingLimit) {
    throw new ApiError(409, "candle_limit_reached");
  }

  current.push(now + flowerDurationMs);
  await c.env.DB.prepare("UPDATE memorials SET candle_until = ?, candle_until_json = ?, updated_at = ? WHERE id = ?")
    .bind(current[0] ?? 0, JSON.stringify(current), new Date().toISOString(), row.id)
    .run();

  return c.json({ candleUntil: current[0] ?? 0, candleUntilList: current });
});

app.post("/memorials/:id/incense", requireAuth, async (c) => {
  const row = await loadMemorial(c, c.req.param("id"));
  const now = Date.now();
  const current = row.incense_until ?? 0;

  if (current > now) {
    throw new ApiError(409, "incense_active");
  }

  const incenseUntil = now + flowerDurationMs;
  await c.env.DB.prepare("UPDATE memorials SET incense_until = ?, updated_at = ? WHERE id = ?")
    .bind(incenseUntil, new Date().toISOString(), row.id)
    .run();

  return c.json({ incenseUntil });
});

app.post("/memorials/:id/fruits", requireAuth, async (c) => {
  const row = await loadMemorial(c, c.req.param("id"));
  const user = c.get("user");
  const body = await parseJson(c);
  const type = readString(body, "type", { required: true, max: 20 });
  const now = Date.now();

  if (!isMemorialFruitType(type)) {
    throw new ApiError(400, "invalid_fruit_type");
  }

  if (type === "durian") {
    if (!readEnvBoolean(c.env.PAYMENT_ENABLED, false)) {
      throw new ApiError(503, "payment_not_configured");
    }
    const unlock = await c.env.DB.prepare(
      "SELECT feature FROM feature_unlocks WHERE user_id = ? AND feature = ?"
    )
      .bind(user.id, durianOfferingFeature)
      .first<{ feature: string }>();
    if (!unlock) {
      throw new ApiError(402, "durian_offering_requires_payment");
    }
  }

  const current = parseMemorialFruits(row.fruit_offerings_json, now);
  if (current.filter((item) => item.type === type).length >= memorialFruitLimits[type]) {
    throw new ApiError(409, `${type}_offering_limit_reached`);
  }

  const next = [...current, { type, until: now + flowerDurationMs }].sort((a, b) => a.until - b.until);
  await c.env.DB.prepare("UPDATE memorials SET fruit_offerings_json = ?, updated_at = ? WHERE id = ?")
    .bind(JSON.stringify(next), new Date().toISOString(), row.id)
    .run();

  return c.json({ fruitOfferings: next });
});

app.post("/assets", requireAuth, async (c) => {
  const form = await c.req.formData();
  const file = form.get("file");
  const scope = String(form.get("scope") || "general");
  const asset = await uploadAsset(c, file, safeScope(scope));
  return c.json({ asset }, 201);
});

app.get("/assets/*", requireAuth, async (c) => {
  const user = c.get("user");
  const key = decodeURIComponent(c.req.path.replace(/^\/assets\//, ""));
  if (!key) {
    throw new ApiError(404, "asset_not_found");
  }

  const review = await c.env.DB.prepare(
    "SELECT status FROM upload_reviews WHERE asset_key = ? ORDER BY created_at DESC LIMIT 1"
  )
    .bind(key)
    .first<{ status: string }>();
  if (review && (review.status === "rejected" || review.status === "quarantined")) {
    throw new ApiError(404, "asset_not_found");
  }

  const asset = await c.env.DB.prepare(
    "SELECT owner_id, visibility FROM assets WHERE asset_key = ?"
  )
    .bind(key)
    .first<{ owner_id: string; visibility?: string | null }>();
  if (!asset) {
    throw new ApiError(404, "asset_not_found");
  }

  const ownerAccess = asset.owner_id === user.id || user.role === "admin";
  const publicAccess = asset.visibility === "public" && review?.status === "approved";
  if (!ownerAccess && !publicAccess) {
    // Do not reveal whether a private key exists.
    throw new ApiError(404, "asset_not_found");
  }

  const object = await c.env.ASSETS.get(key);
  if (!object) {
    throw new ApiError(404, "asset_not_found");
  }

  return new Response(object.body, {
    headers: {
      "Content-Type": object.httpMetadata?.contentType || "application/octet-stream",
      "Cache-Control": publicAccess ? "public, max-age=31536000, immutable" : "private, no-store",
      "X-Content-Type-Options": "nosniff"
    }
  });
});

app.get("/feature-unlocks/:feature", requireAuth, async (c) => {
  const user = c.get("user");
  const feature = safeScope(c.req.param("feature"));
  if (isPaidFeature(feature) && !readEnvBoolean(c.env.PAYMENT_ENABLED, false)) {
    return c.json({ feature, unlocked: false });
  }
  const row = await c.env.DB.prepare(
    "SELECT feature FROM feature_unlocks WHERE user_id = ? AND feature = ?"
  )
    .bind(user.id, feature)
    .first<{ feature: string }>();
  return c.json({ feature, unlocked: Boolean(row) });
});

app.post("/feature-unlocks/:feature", requireAuth, async (c) => {
  throw new ApiError(503, "payment_not_configured");
});

app.get("/ai/profile", requireAuth, async (c) => {
  const profile = await loadDefaultAiCompanion(c);
  return c.json({ profile: serializeAiCompanion(profile) });
});

app.patch("/ai/profile", requireAuth, async (c) => {
  const current = await loadDefaultAiCompanion(c);
  const body = await parseJson(c);
  const updated = await updateAiCompanionFromBody(c, current, body);
  return c.json({ profile: serializeAiCompanion(updated) });
});

app.post("/ai/unlock", requireAuth, async (c) => {
  throw new ApiError(503, "payment_not_configured");
});

app.post("/ai/assets", requireAuth, async (c) => {
  const current = await loadDefaultAiCompanion(c);
  const result = await uploadAiCompanionAsset(c, current);
  return c.json({ asset: result.asset, profile: serializeAiCompanion(result.profile) }, 201);
});

app.get("/ai/companions", requireAuth, async (c) => {
  await loadDefaultAiCompanion(c);
  const user = c.get("user");
  const rows = await c.env.DB.prepare(
    "SELECT * FROM ai_companions WHERE user_id = ? ORDER BY is_default DESC, updated_at DESC"
  )
    .bind(user.id)
    .all<AiCompanionRow>();
  return c.json({ companions: rows.results.map(serializeAiCompanion) });
});

app.post("/ai/companions", requireAuth, async (c) => {
  const body = await parseJson(c);
  const companion = await createAiCompanion(c, {
    displayName: readString(body, "displayName", { max: 40 }),
    gender: readString(body, "gender", { max: 20 }),
    relation: readString(body, "relation", { max: 30 }),
    avatarUrl: readString(body, "avatarUrl", { max: 500 })
  });
  return c.json({ companion: serializeAiCompanion(companion) }, 201);
});

app.patch("/ai/companions/:id", requireAuth, async (c) => {
  const companion = await loadAiCompanion(c, c.req.param("id"));
  const body = await parseJson(c);
  const updated = await updateAiCompanionFromBody(c, companion, body);
  return c.json({ companion: serializeAiCompanion(updated) });
});

app.post("/ai/companions/:id/unlock", requireAuth, async (c) => {
  throw new ApiError(503, "payment_not_configured");
});

app.post("/ai/companions/:id/assets", requireAuth, async (c) => {
  const companion = await loadAiCompanion(c, c.req.param("id"));
  const result = await uploadAiCompanionAsset(c, companion);
  return c.json({ asset: result.asset, companion: serializeAiCompanion(result.profile) }, 201);
});

app.get("/ai/companions/:id/messages", requireAuth, async (c) => {
  const companion = await loadAiCompanion(c, c.req.param("id"));
  const messages = await listAiMessages(c, companion.id, companion.is_default === 1);
  return c.json({ messages: messages.map(serializeAiMessage) });
});

app.post("/ai/companions/:id/messages", requireAuth, async (c) => {
  const companion = await loadAiCompanion(c, c.req.param("id"));
  const body = await parseJson(c);
  const content = readString(body, "content", { required: true, max: 500 });
  const messages = await createAiChatPair(c, companion, content);
  return c.json({ messages: messages.map(serializeAiMessage) }, 201);
});

app.get("/ai/messages", requireAuth, async (c) => {
  const companion = await loadDefaultAiCompanion(c);
  const messages = await listAiMessages(c, companion.id, true);
  return c.json({ messages: messages.map(serializeAiMessage) });
});

app.post("/ai/messages", requireAuth, async (c) => {
  const companion = await loadDefaultAiCompanion(c);
  const body = await parseJson(c);
  const content = readString(body, "content", { required: true, max: 500 });
  const messages = await createAiChatPair(c, companion, content);
  return c.json({ messages: messages.map(serializeAiMessage) }, 201);
});

async function uploadAiCompanionAsset(c: Context<AppEnv>, current: AiCompanionRow) {
  const form = await c.req.formData();
  const kind = String(form.get("kind") || "photo");
  if (!["avatar", "photo", "voice", "moment"].includes(kind)) {
    throw new ApiError(400, "invalid_ai_asset_kind");
  }

  const file = form.get("file");
  if (kind === "avatar" && file instanceof File && !file.type.startsWith("image/")) {
    throw new ApiError(415, "unsupported_avatar_type", { type: file.type });
  }
  const asset = await uploadAsset(c, file, `ai/${kind}`);
  const avatarImageReference = kind === "avatar" && file instanceof File
    ? await fileToDataUrl(file)
    : asset.url;
  const avatarMotion = kind === "avatar"
    ? await analyzeAvatarMotion(c, avatarImageReference, current)
    : parseJsonObject(current.avatar_motion_json);
  const smileAvatarUrl = kind === "avatar"
    ? await generateSmilingAvatarVariant(c, avatarImageReference, current, avatarMotion, asset.url)
    : current.smile_avatar_url;
  const next: AiCompanionRow = {
    ...current,
    avatar_url: kind === "avatar" ? asset.url : current.avatar_url,
    smile_avatar_url: kind === "avatar" ? smileAvatarUrl : current.smile_avatar_url,
    avatar_motion_json: normalizeJsonString(JSON.stringify(avatarMotion)),
    photo_count: current.photo_count + (kind === "photo" ? 1 : 0),
    voice_count: current.voice_count + (kind === "voice" ? 1 : 0),
    moment_count: current.moment_count + (kind === "moment" ? 1 : 0)
  };

  const updated = await saveAiCompanion(c, next);
  return { asset, profile: updated };
}

async function ensureSmilingAvatarVariant(c: Context<AppEnv>, row: AiCompanionRow) {
  if (!row.avatar_url || row.smile_avatar_url) {
    return row;
  }

  const avatarMotion = parseJsonObject(row.avatar_motion_json);
  const smileAvatarUrl = await generateSmilingAvatarVariant(c, row.avatar_url, row, avatarMotion);
  if (!smileAvatarUrl) {
    return row;
  }
  return saveAiCompanion(c, { ...row, smile_avatar_url: smileAvatarUrl });
}

async function generateSmilingAvatarVariant(
  c: Context<AppEnv>,
  avatarImageReference: string,
  companion: AiCompanionRow,
  avatarMotion: Record<string, unknown>,
  avatarDescriptionReference = avatarImageReference
) {
  const env = c.env;
  const apiKey = (env.AI_IMAGE_API_KEY || env.AI_API_KEY)?.trim();
  const baseUrl = (env.AI_IMAGE_BASE_URL || env.AI_BASE_URL)?.trim();
  if (!apiKey || !baseUrl) {
    return null;
  }

  const motion = asRecord(avatarMotion);
  const confidence = numberValue(motion.confidence, 0);
  const configuredTimeoutMs = Number(env.AI_TIMEOUT_MS || "20000");
  const timeoutMs = Number.isFinite(configuredTimeoutMs) ? Math.max(configuredTimeoutMs, 210000) : 210000;
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  const prompt = [
    "Edit the provided portrait into a realistic smiling avatar for a memorial companion app.",
    "Keep the same person recognizable. Preserve identity, age, skin tone, hairstyle, clothing, lighting, background, and camera crop as much as possible.",
    confidence > 0.2
      ? "A human face has already been detected; keep that face centered and natural."
      : "If the face is not perfectly centered, keep the main person centered and recognizable.",
    "Only change the facial expression: make the person show a warm, genuine, natural smile with relaxed eyes.",
    "Do not cartoonize, beautify aggressively, change clothes, add text, add accessories, or add extra people."
  ].join(" ");
  const appearance = await describeAvatarAppearance(c, avatarDescriptionReference, companion);
  const generationPrompt = [
    "Generate a warm, realistic smiling portrait avatar for a memorial companion app.",
    "Use this visual description of the uploaded avatar as the reference, without naming or identifying the person:",
    appearance,
    "Keep a similar apparent age, gender presentation, hairstyle, face shape, clothing style, lighting, color mood, and camera crop.",
    "The person should have a genuine natural smile with relaxed eyes. No text, no extra people, no hats or accessories unless described."
  ].join(" ");

  try {
    const model = env.AI_IMAGE_MODEL?.trim() || "gpt-image-1";
    const image = await requestSmilingAvatarImage(
      baseUrl,
      apiKey,
      model,
      prompt,
      generationPrompt,
      avatarImageReference,
      avatarDescriptionReference,
      controller.signal
    );
    const asset = await storeGeneratedAsset(c, image.bytes, "ai/avatar-smile", image.mimeType);
    return asset.url;
  } catch (error) {
    console.warn("AI avatar smile fallback:", error instanceof Error ? error.message : String(error));
    return null;
  } finally {
    clearTimeout(timer);
  }
}

async function requestSmilingAvatarImage(
  baseUrl: string,
  apiKey: string,
  model: string,
  prompt: string,
  generationPrompt: string,
  avatarImageReference: string,
  generationImageReference: string,
  signal: AbortSignal
) {
  const jsonError = await requestSmilingAvatarImageJson(baseUrl, apiKey, model, prompt, avatarImageReference, signal)
    .then((image) => ({ image, error: null as Error | null }))
    .catch((error) => ({ image: null, error: error instanceof Error ? error : new Error(String(error)) }));
  if (jsonError.image) {
    return jsonError.image;
  }

  if (!shouldSkipMultipartImageEdit(jsonError.error)) {
    try {
      return await requestSmilingAvatarImageFormData(baseUrl, apiKey, model, prompt, avatarImageReference, signal);
    } catch (error) {
      const multipartError = error instanceof Error ? error : new Error(String(error));
      console.warn(`AI avatar image edit multipart failed: ${multipartError.message}`);
    }
  }

  try {
    return await requestSmilingAvatarImageGeneration(baseUrl, apiKey, model, generationPrompt, generationImageReference, signal);
  } catch (error) {
    const generationError = error instanceof Error ? error : new Error(String(error));
    throw new Error(`${jsonError.error?.message || "avatar_smile_json_failed"}; generation:${generationError.message}`);
  }
}

function shouldSkipMultipartImageEdit(error: Error | null) {
  const message = error?.message || "";
  return message.includes("avatar_smile_json_502") || message.includes("upstream_error");
}

async function requestSmilingAvatarImageGeneration(
  baseUrl: string,
  apiKey: string,
  model: string,
  prompt: string,
  imageReference: string,
  signal: AbortSignal
) {
  const body: Record<string, unknown> = {
    model,
    prompt,
    size: "1024x1024"
  };
  if (imageReference) {
    body.images = [{ image_url: imageReference }];
  }

  const response = await fetch(imageGenerationsUrl(baseUrl), {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${apiKey}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify(body),
    signal
  });

  const text = await response.text();
  if (!response.ok) {
    throw new Error(`avatar_smile_generation_${response.status}:${text.slice(0, 160)}`);
  }
  return parseGeneratedImageResponse(text);
}

async function describeAvatarAppearance(c: Context<AppEnv>, avatarUrl: string, companion: AiCompanionRow) {
  const env = c.env;
  const apiKey = env.AI_API_KEY?.trim();
  const baseUrl = env.AI_BASE_URL?.trim();
  if (!apiKey || !baseUrl) {
    return `${companion.relation}, ${companion.gender}, close-up portrait avatar.`;
  }

  const timeoutMs = Math.min(Number(env.AI_TIMEOUT_MS || "20000") || 20000, 30000);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(chatCompletionsUrl(baseUrl), {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: env.AI_VISION_MODEL?.trim() || env.AI_MODEL?.trim() || "gpt-5.4-mini",
        temperature: 0.2,
        max_tokens: 240,
        messages: [
          {
            role: "system",
            content:
              "Describe the visible portrait for image generation. Do not identify the person or infer private identity. Return one concise English paragraph about visual appearance only."
          },
          {
            role: "user",
            content: [
              {
                type: "text",
                text:
                  "Describe this avatar's visual appearance for generating a similar smiling portrait: apparent age range, gender presentation, hairstyle, face shape, clothing, pose, lighting, background, and art/photo style."
              },
              {
                type: "image_url",
                image_url: { url: avatarUrl }
              }
            ]
          }
        ]
      }),
      signal: controller.signal
    });

    const text = await response.text();
    if (!response.ok) {
      throw new Error(`avatar_description_${response.status}:${text.slice(0, 160)}`);
    }
    const data = JSON.parse(text) as {
      choices?: Array<{ message?: { content?: string }; text?: string }>;
    };
    const description = (data.choices?.[0]?.message?.content || data.choices?.[0]?.text || "").trim();
    return description.slice(0, 900) || `${companion.relation}, ${companion.gender}, close-up portrait avatar.`;
  } catch (error) {
    console.warn("AI avatar description fallback:", error instanceof Error ? error.message : String(error));
    return `${companion.relation}, ${companion.gender}, close-up portrait avatar.`;
  } finally {
    clearTimeout(timer);
  }
}

async function requestSmilingAvatarImageFormData(
  baseUrl: string,
  apiKey: string,
  model: string,
  prompt: string,
  avatarImageReference: string,
  signal: AbortSignal
) {
  const source = await imageReferenceToBlob(avatarImageReference);
  const form = new FormData();
  form.append("model", model);
  form.append("prompt", prompt);
  form.append("image", source.blob, source.fileName);

  const response = await fetch(imageEditsUrl(baseUrl), {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${apiKey}`
    },
    body: form,
    signal
  });

  const text = await response.text();
  if (!response.ok) {
    throw new Error(`avatar_smile_multipart_${response.status}:${text.slice(0, 160)}`);
  }
  return parseGeneratedImageResponse(text);
}

async function requestSmilingAvatarImageJson(
  baseUrl: string,
  apiKey: string,
  model: string,
  prompt: string,
  avatarImageReference: string,
  signal: AbortSignal
) {
  const response = await fetch(imageEditsUrl(baseUrl), {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${apiKey}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      model,
      prompt,
      images: [{ image_url: avatarImageReference }]
    }),
    signal
  });

  const text = await response.text();
  if (!response.ok) {
    throw new Error(`avatar_smile_json_${response.status}:${text.slice(0, 160)}`);
  }
  return parseGeneratedImageResponse(text);
}

async function parseGeneratedImageResponse(text: string) {
  const data = JSON.parse(text) as {
    data?: Array<{ b64_json?: string; url?: string; mime_type?: string }>;
  };
  const first = data.data?.[0];
  const imageBase64 = first?.b64_json?.trim();
  if (imageBase64) {
    const mimeType = first?.mime_type || imageBase64.match(/^data:([^;]+);base64,/)?.[1] || "image/png";
    const cleanBase64 = imageBase64.replace(/^data:[^;]+;base64,/, "");
    const bytes = Buffer.from(cleanBase64, "base64");
    if (!bytes.byteLength) {
      throw new Error("empty_avatar_smile_bytes");
    }
    return { bytes, mimeType };
  }

  const url = first?.url?.trim();
  if (!url) {
    throw new Error("empty_avatar_smile_image");
  }
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`avatar_smile_url_fetch_${response.status}`);
  }
  const mimeType = response.headers.get("content-type")?.split(";")[0]?.trim() || "image/png";
  const bytes = Buffer.from(await response.arrayBuffer());
  if (!bytes.byteLength) {
    throw new Error("empty_avatar_smile_url_bytes");
  }
  return { bytes, mimeType };
}

async function imageReferenceToBlob(imageReference: string) {
  if (imageReference.startsWith("data:")) {
    const match = imageReference.match(/^data:([^;]+);base64,(.+)$/);
    if (!match) {
      throw new Error("invalid_avatar_data_url");
    }
    const mimeType = match[1] || "image/png";
    const bytes = Buffer.from(match[2], "base64");
    return {
      blob: new Blob([bytes], { type: mimeType }),
      fileName: `avatar${allowedAssetTypes.get(mimeType) || ".png"}`
    };
  }

  const response = await fetch(imageReference);
  if (!response.ok) {
    throw new Error(`avatar_source_fetch_${response.status}`);
  }
  const mimeType = response.headers.get("content-type")?.split(";")[0]?.trim() || "image/png";
  const bytes = await response.arrayBuffer();
  return {
    blob: new Blob([bytes], { type: mimeType }),
    fileName: `avatar${allowedAssetTypes.get(mimeType) || ".png"}`
  };
}

async function storeGeneratedAsset(c: Context<AppEnv>, bytes: Uint8Array, scope: string, mimeType: string) {
  const user = c.get("user");
  const id = crypto.randomUUID();
  const extension = allowedAssetTypes.get(mimeType) || ".png";
  const key = `${user.id}/${scope}/${id}${extension}`;

  await c.env.ASSETS.put(key, bytes, {
    httpMetadata: {
      contentType: mimeType
    },
    customMetadata: {
      ownerId: user.id
    }
  });

  const url = assetUrl(c, key);
  await c.env.DB.prepare(
    "INSERT INTO assets (id, owner_id, asset_key, url, mime_type, size_bytes, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
  )
    .bind(id, user.id, key, url, mimeType, bytes.byteLength, new Date().toISOString())
    .run();

  return {
    id,
    key,
    url,
    mimeType,
    sizeBytes: bytes.byteLength
  };
}

async function analyzeAvatarMotion(c: Context<AppEnv>, avatarUrl: string, companion: AiCompanionRow) {
  const env = c.env;
  const apiKey = env.AI_API_KEY?.trim();
  const baseUrl = env.AI_BASE_URL?.trim();
  if (!apiKey || !baseUrl) {
    return defaultAvatarMotion("fallback_no_ai");
  }

  const timeoutMs = Number(env.AI_TIMEOUT_MS || "20000");
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), Number.isFinite(timeoutMs) ? timeoutMs : 20000);

  try {
    const response = await fetch(chatCompletionsUrl(baseUrl), {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: env.AI_VISION_MODEL?.trim() || env.AI_MODEL?.trim() || "gpt-5.4-mini",
        temperature: 0,
        max_tokens: 500,
        messages: [
          {
            role: "system",
            content:
              "You are a face-motion calibration service. Return JSON only. " +
              "Find the main human face in the image and estimate normalized coordinates from 0 to 1 for face and mouth. " +
              "Use the image coordinate system: x/y are center points, w/h are box sizes. Do not identify the person."
          },
          {
            role: "user",
            content: [
              {
                type: "text",
                text:
                  "Analyze this avatar for dynamic smile and mouth animation. Return exactly this JSON shape: " +
                  "{\"face\":{\"x\":0.5,\"y\":0.46,\"w\":0.48,\"h\":0.58}," +
                  "\"mouth\":{\"x\":0.5,\"y\":0.66,\"w\":0.2,\"h\":0.07}," +
                  "\"confidence\":0.0,\"expression\":\"neutral\"}"
              },
              {
                type: "image_url",
                image_url: { url: avatarUrl }
              }
            ]
          }
        ]
      }),
      signal: controller.signal
    });

    const text = await response.text();
    if (!response.ok) {
      throw new Error(`avatar_motion_api_${response.status}:${text.slice(0, 160)}`);
    }
    const data = JSON.parse(text) as {
      choices?: Array<{ message?: { content?: string }; text?: string }>;
    };
    const content = data.choices?.[0]?.message?.content || data.choices?.[0]?.text || "";
    return normalizeAvatarMotion(parseJsonObjectFromAiText(content), "ai_ready");
  } catch (error) {
    console.warn("AI avatar motion fallback:", error instanceof Error ? error.message : String(error));
    return defaultAvatarMotion("fallback_ai_failed");
  } finally {
    clearTimeout(timer);
  }
}

function parseJsonObjectFromAiText(text: string) {
  const trimmed = text.trim();
  const fenced = trimmed.match(/```(?:json)?\s*([\s\S]*?)```/i)?.[1]?.trim();
  const firstBrace = trimmed.indexOf("{");
  const lastBrace = trimmed.lastIndexOf("}");
  const candidate = fenced || (firstBrace >= 0 && lastBrace > firstBrace ? trimmed.slice(firstBrace, lastBrace + 1) : trimmed);
  try {
    const parsed = JSON.parse(candidate);
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed as Record<string, unknown> : {};
  } catch {
    return {};
  }
}

function normalizeAvatarMotion(value: Record<string, unknown>, status: string) {
  const face = normalizeMotionBox(asRecord(value.face), { x: 0.5, y: 0.46, w: 0.5, h: 0.58 });
  const mouth = normalizeMotionBox(asRecord(value.mouth), {
    x: face.x,
    y: Math.min(0.88, face.y + face.h * 0.34),
    w: face.w * 0.34,
    h: face.h * 0.1
  });
  return {
    version: 1,
    status,
    source: status === "ai_ready" ? "ai_vision" : "fallback",
    face,
    mouth,
    confidence: clampNumber(numberValue(value.confidence, status === "ai_ready" ? 0.65 : 0), 0, 1),
    expression: String(value.expression || "neutral").slice(0, 40)
  };
}

function defaultAvatarMotion(status: string) {
  return normalizeAvatarMotion({}, status);
}

async function fileToDataUrl(file: File) {
  const bytes = new Uint8Array(await file.arrayBuffer());
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return `data:${file.type};base64,${btoa(binary)}`;
}

function normalizeMotionBox(value: Record<string, unknown>, fallback: { x: number; y: number; w: number; h: number }) {
  const w = clampNumber(numberValue(value.w, fallback.w), 0.05, 1);
  const h = clampNumber(numberValue(value.h, fallback.h), 0.03, 1);
  return {
    x: clampNumber(numberValue(value.x, fallback.x), w / 2, 1 - w / 2),
    y: clampNumber(numberValue(value.y, fallback.y), h / 2, 1 - h / 2),
    w,
    h
  };
}

function asRecord(value: unknown) {
  return value && typeof value === "object" && !Array.isArray(value) ? value as Record<string, unknown> : {};
}

function numberValue(value: unknown, fallback: number) {
  const next = typeof value === "number" ? value : Number(value);
  return Number.isFinite(next) ? next : fallback;
}

function clampNumber(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

app.get("/legal/privacy", (c) => {
  const legal = legalConfig(c.env);
  return c.html(
    legalPage(
      "隐私政策",
      legal,
      `
        <p>本政策说明 ${legal.appName} 如何收集、使用、存储、共享和删除用户信息。请在使用服务前仔细阅读。</p>
        <h2>我们收集的信息</h2>
        <ul>
          <li>账号信息：用户名、昵称、登录凭证、账号角色。</li>
          <li>纪念馆信息：纪念对象姓名、纪念照片、献花记录、蜡烛倒计时。</li>
          <li>人文社区信息：帖子内容、点赞记录、义工招募互动信息。</li>
          <li>AI 陪伴素材：头像、生活照片、语音、朋友圈或文本素材、聊天记录。</li>
          <li>设备与日志信息：网络请求、异常日志、必要的安全审计记录。</li>
        </ul>
        <h2>使用目的</h2>
        <p>我们使用上述信息用于注册登录、纪念馆展示、人文社区互动、义工招募、AI 陪伴体验、客服支持、安全风控和合规审计。</p>
        <h2>共享与委托处理</h2>
        <p>我们可能向云服务商、对象存储/CDN、支付服务商、客服和履约人员共享完成服务所必需的信息。涉及监管、司法或法律要求时，我们将依法配合。</p>
        <h2>上传授权</h2>
        <p>用户上传逝者或他人的照片、语音、社交内容前，应确认自己拥有合法授权，并已取得必要权利人或近亲属同意。</p>
        <h2>保存与删除</h2>
        <p>用户可在 App 内注销账号，也可通过 <a href="/legal/account-deletion">账号注销页面</a> 提交删除请求。因支付、退款、对账、税务、纠纷或法律要求必须保存的信息，将在必要期限内保存。</p>
        <h2>联系我们</h2>
        <p>邮箱：${escapeHtml(legal.email)}；电话：${escapeHtml(legal.phone)}</p>
      `
    )
  );
});

app.get("/legal/terms", (c) => {
  const legal = legalConfig(c.env);
  return c.html(
    legalPage(
      "用户协议",
      legal,
      `
        <p>本协议是用户与 ${legal.operator} 就使用 ${legal.appName} 服务所订立的协议。</p>
        <h2>服务内容</h2>
        <p>${legal.appName} 提供云端纪念馆、AI 陪伴、人文社区、义工招募信息和账号支持等服务。</p>
        <h2>账号规则</h2>
        <p>用户应提供真实、合法、有效的信息，不得冒用他人身份，不得上传违法、侵权、虚假或伤害他人权益的内容。</p>
        <h2>人文社区</h2>
        <p>用户可在社区发布内容并参与点赞互动。发布内容应尊重他人，不得包含违法、侵权、辱骂、诈骗或明显伤害他人的信息。</p>
        <h2>AI 陪伴</h2>
        <p>AI 陪伴为生成式或模拟互动体验，不代表逝者本人真实表达，也不构成专业建议。</p>
        <h2>禁止行为</h2>
        <p>不得上传违法、侵权、诈骗、辱骂、恐吓、低俗内容；不得破坏系统安全、绕过支付或批量恶意注册。</p>
        <h2>联系方式</h2>
        <p>邮箱：${escapeHtml(legal.email)}；电话：${escapeHtml(legal.phone)}</p>
      `
    )
  );
});

app.get("/legal/ai-disclaimer", (c) => {
  const legal = legalConfig(c.env);
  return c.html(
    legalPage(
      "AI 陪伴免责声明",
      legal,
      `
        <h2>功能性质</h2>
        <p>AI 陪伴是基于用户提供的性别、关系、头像、照片、语音或文本素材生成的模拟互动体验。生成内容不代表逝者本人真实表达。</p>
        <h2>上传授权</h2>
        <p>用户上传逝者或他人的照片、语音、朋友圈、文字材料前，应确认自己拥有合法授权，并已取得必要权利人或近亲属同意。</p>
        <h2>内容边界</h2>
        <p>AI 内容可能不准确、不完整或不符合用户期待。请勿将 AI 输出用于医疗、心理治疗、法律、财务、宗教仪式决策或其他专业场景。</p>
        <h2>情绪提醒</h2>
        <p>纪念、追忆和 AI 陪伴可能引发强烈情绪。如果用户处于明显悲伤、焦虑或创伤状态，建议减少使用频率，并寻求家人、朋友或专业人士支持。</p>
        <h2>当前版本说明</h2>
        <p>当前版本的 AI 回复由服务端生成；如未配置真实 AI 服务，将使用服务端本地陪伴回复。App 和商店描述应与实际接入状态保持一致。</p>
      `
    )
  );
});

app.get("/legal/account-deletion", (c) => {
  const legal = legalConfig(c.env);
  return c.html(
    legalPage(
      "账号注销与数据删除",
      legal,
      `
        <p>用户可以通过 App 内入口注销账号，也可以在本页面提交删除请求。我们会在核验账号归属后处理。</p>
        <h2>App 内注销路径</h2>
        <p>登录 ${legal.appName} 后，点击顶部「协议」，选择「注销当前账号」。注销后，账号将不能继续登录。</p>
        <h2>删除范围</h2>
        <ul>
          <li>将删除或匿名化账号资料、纪念馆资料、AI 陪伴素材、聊天记录、普通上传文件。</li>
          <li>订单、支付、退款、税务、风控、纠纷和法律合规所需记录可能在必要期限内保留。</li>
        </ul>
        <h2>提交删除请求</h2>
        <form method="post" action="/legal/account-deletion/request">
          <label>账号用户名<input required name="username" maxlength="64" /></label>
          <label>联系邮箱或电话<input name="contact" maxlength="120" /></label>
          <label>补充说明<textarea name="reason" maxlength="500"></textarea></label>
          <button type="submit">提交删除请求</button>
        </form>
        <p>也可以发送邮件至 ${escapeHtml(legal.email)}，邮件标题写明「${legal.appName} 账号注销」。</p>
      `
    )
  );
});

app.post("/legal/account-deletion/request", async (c) => {
  const body = await parseLegalRequestBody(c);
  const username = readString(body, "username", { required: true, max: 64 });
  const contact = readString(body, "contact", { max: 120 });
  const reason = readString(body, "reason", { max: 500 });
  const now = new Date().toISOString();

  await c.env.DB.prepare(
    "INSERT INTO account_deletion_requests (id, username, contact, reason, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)"
  )
    .bind(crypto.randomUUID(), username, contact || null, reason || null, now, now)
    .run();

  const legal = legalConfig(c.env);
  return c.html(
    legalPage(
      "删除请求已提交",
      legal,
      `<p>我们已收到账号「${escapeHtml(username)}」的删除请求。客服会根据账号归属核验结果处理。</p><p><a href="/legal/account-deletion">返回账号注销页面</a></p>`
    )
  );
});

app.get("/admin", (c) => c.html(adminPage()));
app.get("/admin/", (c) => c.html(adminPage()));

app.get("/admin/audit-logs", requireAuth, async (c) => {
  requireAdmin(c);
  const rows = await c.env.DB.prepare(
    "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 200"
  ).all();
  return c.json({ logs: rows.results });
});

app.get("/admin/upload-reviews", requireAuth, async (c) => {
  requireAdmin(c);
  const status = c.req.query("status") || "pending";
  const rows = await c.env.DB.prepare(
    "SELECT * FROM upload_reviews WHERE status = ? ORDER BY created_at DESC LIMIT 200"
  )
    .bind(status)
    .all();
  return c.json({ reviews: rows.results });
});

app.patch("/admin/upload-reviews/:id", requireAuth, async (c) => {
  const user = requireAdmin(c);
  const body = await parseJson(c);
  const status = readString(body, "status", { required: true, max: 20 });
  const reason = readString(body, "reason", { max: 300 }) || null;

  if (!["approved", "rejected", "quarantined"].includes(status)) {
    throw new ApiError(400, "invalid_review_status");
  }

  const review = await c.env.DB.prepare("SELECT * FROM upload_reviews WHERE id = ?")
    .bind(c.req.param("id"))
    .first<{ id: string; asset_key: string; owner_id: string }>();
  if (!review) {
    throw new ApiError(404, "review_not_found");
  }

  await c.env.DB.prepare(
    "UPDATE upload_reviews SET status = ?, reason = ?, reviewed_at = ?, reviewed_by = ? WHERE id = ?"
  )
    .bind(status, reason, new Date().toISOString(), user.id, review.id)
    .run();

  if (status === "rejected" || status === "quarantined") {
    await setAssetVisibility(c, review.asset_key, "private", review.owner_id);
    await queueAssetDelete(c, review.asset_key, review.owner_id, `upload_review_${status}`);
  } else if (status === "approved") {
    const profileReference = await c.env.DB.prepare(
      `SELECT u.id
       FROM users u
       JOIN assets a ON a.url = u.avatar_url
       WHERE a.asset_key = ?
       LIMIT 1`
    )
      .bind(review.asset_key)
      .first<{ id: string }>();
    if (profileReference) {
      await setAssetVisibility(c, review.asset_key, "public", profileReference.id);
    }
  }

  await writeAudit(c, {
    action: "admin.upload.review",
    targetType: "upload_review",
    targetId: review.id,
    metadata: { status, reason }
  });

  return c.json({ ok: true });
});

app.get("/admin/community/moderation", requireAuth, async (c) => {
  requireAdmin(c);
  const status = c.req.query("status") || "pending";
  if (!["pending", "approved", "rejected", "blocked", "all"].includes(status)) {
    throw new ApiError(400, "invalid_community_moderation_status");
  }

  const postQuery = `SELECT p.*, u.username, u.display_name, u.avatar_url
    FROM community_posts p
    JOIN users u ON u.id = p.user_id
    ${status === "all" ? "" : "WHERE p.status = ?"}
    ORDER BY p.created_at DESC LIMIT 200`;
  const commentQuery = `SELECT cc.*, u.username, u.display_name, u.avatar_url
    FROM community_post_comments cc
    JOIN users u ON u.id = cc.user_id
    ${status === "all" ? "" : "WHERE cc.status = ?"}
    ORDER BY cc.created_at DESC LIMIT 200`;
  const posts = status === "all"
    ? await c.env.DB.prepare(postQuery).all<CommunityPostRow>()
    : await c.env.DB.prepare(postQuery).bind(status).all<CommunityPostRow>();
  const comments = status === "all"
    ? await c.env.DB.prepare(commentQuery).all<CommunityCommentRow>()
    : await c.env.DB.prepare(commentQuery).bind(status).all<CommunityCommentRow>();

  return c.json({
    posts: posts.results.map(serializeCommunityPost),
    comments: comments.results.map(serializeCommunityComment)
  });
});

app.patch("/admin/community/posts/:id", requireAuth, async (c) => {
  const admin = requireAdmin(c);
  const body = await parseJson(c);
  const status = readString(body, "status", { required: true, max: 20 });
  const reason = readString(body, "reason", { max: 300 }) || null;
  if (!["pending", "approved", "rejected", "blocked"].includes(status)) {
    throw new ApiError(400, "invalid_community_moderation_status");
  }

  const post = await c.env.DB.prepare(
    "SELECT id, user_id, image_urls, status FROM community_posts WHERE id = ?"
  )
    .bind(c.req.param("id"))
    .first<{ id: string; user_id: string; image_urls: string; status: string }>();
  if (!post) {
    throw new ApiError(404, "community_post_not_found");
  }

  if (status === "approved") {
    for (const url of parseStringArray(post.image_urls || "[]")) {
      const key = assetKeyFromUrl(url);
      if (!key) continue;
      const review = await c.env.DB.prepare(
        "SELECT status FROM upload_reviews WHERE asset_key = ? ORDER BY created_at DESC LIMIT 1"
      )
        .bind(key)
        .first<{ status: string }>();
      if (review && review.status !== "approved") {
        throw new ApiError(409, "community_media_not_approved");
      }
    }
  }

  const now = new Date().toISOString();
  await c.env.DB.prepare(
    `UPDATE community_posts
     SET status = ?, moderation_reason = ?, moderated_at = ?, moderated_by = ?, updated_at = ?
     WHERE id = ?`
  )
    .bind(status, reason, now, admin.id, now, post.id)
    .run();

  const imageUrls = parseStringArray(post.image_urls || "[]");
  if (status === "approved") {
    await setCommunityAssetVisibility(c, imageUrls, "public", post.user_id);
  } else if (status === "rejected" || status === "blocked") {
    await setCommunityAssetVisibility(c, imageUrls, "private", post.user_id);
    if (post.status !== status) {
      for (const url of imageUrls) {
        const key = assetKeyFromUrl(url);
        if (key) await queueAssetDelete(c, key, post.user_id, `community_post_${status}`);
      }
    }
  }

  await writeAudit(c, {
    action: "admin.community.post.moderate",
    targetType: "community_post",
    targetId: post.id,
    metadata: { status, reason }
  });
  return c.json({ ok: true, status });
});

app.patch("/admin/community/comments/:id", requireAuth, async (c) => {
  const admin = requireAdmin(c);
  const body = await parseJson(c);
  const status = readString(body, "status", { required: true, max: 20 });
  const reason = readString(body, "reason", { max: 300 }) || null;
  if (!["pending", "approved", "rejected", "blocked"].includes(status)) {
    throw new ApiError(400, "invalid_community_moderation_status");
  }
  const comment = await c.env.DB.prepare(
    "SELECT id FROM community_post_comments WHERE id = ?"
  )
    .bind(c.req.param("id"))
    .first<{ id: string }>();
  if (!comment) {
    throw new ApiError(404, "community_comment_not_found");
  }
  const now = new Date().toISOString();
  await c.env.DB.prepare(
    `UPDATE community_post_comments
     SET status = ?, moderation_reason = ?, moderated_at = ?, moderated_by = ?, updated_at = ?
     WHERE id = ?`
  )
    .bind(status, reason, now, admin.id, now, comment.id)
    .run();
  await writeAudit(c, {
    action: "admin.community.comment.moderate",
    targetType: "community_post_comment",
    targetId: comment.id,
    metadata: { status, reason }
  });
  return c.json({ ok: true, status });
});

app.get("/admin/community/reports", requireAuth, async (c) => {
  requireAdmin(c);
  const status = c.req.query("status") || "pending";
  if (!["pending", "actioned", "dismissed", "all"].includes(status)) {
    throw new ApiError(400, "invalid_community_report_status");
  }
  const sql = `SELECT r.*, u.username AS reporter_username, u.display_name AS reporter_display_name
    FROM community_reports r
    JOIN users u ON u.id = r.reporter_id
    ${status === "all" ? "" : "WHERE r.status = ?"}
    ORDER BY r.created_at DESC LIMIT 200`;
  const rows = status === "all"
    ? await c.env.DB.prepare(sql).all<CommunityReportRow>()
    : await c.env.DB.prepare(sql).bind(status).all<CommunityReportRow>();
  return c.json({ reports: rows.results });
});

app.patch("/admin/community/reports/:id", requireAuth, async (c) => {
  const admin = requireAdmin(c);
  const body = await parseJson(c);
  const action = readString(body, "action", { required: true, max: 30 });
  const reason = readString(body, "reason", { max: 300 }) || null;
  if (!["approve", "remove", "dismiss", "block_user", "ban_user"].includes(action)) {
    throw new ApiError(400, "invalid_community_report_action");
  }

  const report = await c.env.DB.prepare("SELECT * FROM community_reports WHERE id = ?")
    .bind(c.req.param("id"))
    .first<CommunityReportRow>();
  if (!report) {
    throw new ApiError(404, "community_report_not_found");
  }
  if (report.status !== "pending") {
    throw new ApiError(409, "community_report_already_reviewed");
  }

  let targetUserId: string | null = null;
  if (report.target_type === "post") {
    const target = await c.env.DB.prepare(
      "SELECT id, user_id, image_urls, status FROM community_posts WHERE id = ?"
    )
      .bind(report.target_id)
      .first<{ id: string; user_id: string; image_urls: string; status: string }>();
    if (!target) throw new ApiError(404, "community_post_not_found");
    targetUserId = target.user_id;
    if (action === "approve" || action === "remove") {
      await moderateCommunityTargetPost(c, admin, target, action === "approve" ? "approved" : "rejected", reason);
    }
  } else {
    const target = await c.env.DB.prepare(
      "SELECT id, user_id, status FROM community_post_comments WHERE id = ?"
    )
      .bind(report.target_id)
      .first<{ id: string; user_id: string; status: string }>();
    if (!target) throw new ApiError(404, "community_comment_not_found");
    targetUserId = target.user_id;
    if (action === "approve" || action === "remove") {
      await moderateCommunityTargetComment(c, admin, target, action === "approve" ? "approved" : "rejected", reason);
    }
  }

  if ((action === "block_user" || action === "ban_user") && targetUserId) {
    await setUserModeration(c, admin, targetUserId, action === "ban_user" ? "banned" : "blocked", reason, null);
  }

  const now = new Date().toISOString();
  await c.env.DB.prepare(
    "UPDATE community_reports SET status = ?, reviewer_id = ?, reviewed_at = ?, updated_at = ? WHERE id = ?"
  )
    .bind(action === "dismiss" ? "dismissed" : "actioned", admin.id, now, now, report.id)
    .run();
  await writeAudit(c, {
    action: "admin.community.report.review",
    targetType: `community_${report.target_type}`,
    targetId: report.target_id,
    metadata: { reportId: report.id, action, reason }
  });
  return c.json({ ok: true, status: action === "dismiss" ? "dismissed" : "actioned" });
});

app.get("/admin/users/moderation", requireAuth, async (c) => {
  requireAdmin(c);
  const rows = await c.env.DB.prepare(
    `SELECT m.*, u.username, u.display_name
     FROM user_moderation m JOIN users u ON u.id = m.user_id
     ORDER BY m.updated_at DESC LIMIT 200`
  ).all<Record<string, unknown>>();
  return c.json({ users: rows.results });
});

app.patch("/admin/users/:id/moderation", requireAuth, async (c) => {
  const admin = requireAdmin(c);
  const body = await parseJson(c);
  const status = readString(body, "status", { required: true, max: 20 });
  const reason = readString(body, "reason", { max: 300 }) || null;
  const expiresAt = readString(body, "expiresAt", { max: 40 }) || null;
  if (!["active", "blocked", "banned"].includes(status)) {
    throw new ApiError(400, "invalid_user_moderation_status");
  }
  const target = await c.env.DB.prepare("SELECT id, role FROM users WHERE id = ? AND deleted_at IS NULL")
    .bind(c.req.param("id"))
    .first<{ id: string; role: Role }>();
  if (!target) throw new ApiError(404, "user_not_found");
  if (target.role === "admin") throw new ApiError(403, "admin_moderation_forbidden");
  if (expiresAt && Number.isNaN(Date.parse(expiresAt))) {
    throw new ApiError(400, "invalid_moderation_expiry");
  }

  if (status === "active") {
    await c.env.DB.prepare("DELETE FROM user_moderation WHERE user_id = ?").bind(target.id).run();
  } else {
    await setUserModeration(c, admin, target.id, status as "blocked" | "banned", reason, expiresAt);
  }
  await writeAudit(c, {
    action: "admin.user.moderation",
    targetType: "user",
    targetId: target.id,
    metadata: { status, reason, expiresAt }
  });
  return c.json({ ok: true, status });
});

app.get("/admin/crash-reports", requireAuth, async (c) => {
  requireAdmin(c);
  const rows = await c.env.DB.prepare(
    "SELECT * FROM crash_reports ORDER BY created_at DESC LIMIT 200"
  ).all();
  return c.json({ reports: rows.results });
});

app.get("/admin/asset-delete-queue", requireAuth, async (c) => {
  requireAdmin(c);
  const keyColumn = await assetDeleteQueueKeyColumn(c);
  const rows = await c.env.DB.prepare(
    "SELECT * FROM asset_delete_queue ORDER BY created_at DESC LIMIT 200"
  ).all<Record<string, unknown>>();
  return c.json({
    items: rows.results.map((row) => ({
      ...row,
      asset_key: (row.asset_key || row[keyColumn]) as string | undefined
    }))
  });
});

app.post("/admin/asset-delete-queue/process", requireAuth, async (c) => {
  requireAdmin(c);
  const keyColumn = await assetDeleteQueueKeyColumn(c);
  const rows = await c.env.DB.prepare(
    `SELECT id, ${keyColumn} AS asset_key FROM asset_delete_queue WHERE status = 'pending' ORDER BY created_at ASC LIMIT 50`
  ).all<{ id: string; asset_key: string }>();

  let deleted = 0;
  for (const row of rows.results) {
    try {
      await c.env.ASSETS.delete(row.asset_key);
      await c.env.DB.prepare(
        "UPDATE asset_delete_queue SET status = 'deleted', processed_at = ?, error_message = NULL WHERE id = ?"
      )
        .bind(new Date().toISOString(), row.id)
        .run();
      deleted += 1;
    } catch (error) {
      await c.env.DB.prepare(
        "UPDATE asset_delete_queue SET status = 'failed', processed_at = ?, error_message = ? WHERE id = ?"
      )
        .bind(new Date().toISOString(), String(error), row.id)
        .run();
    }
  }

  await writeAudit(c, {
    action: "admin.asset_delete_queue.process",
    targetType: "asset_delete_queue",
    metadata: { attempted: rows.results.length, deleted }
  });

  return c.json({ attempted: rows.results.length, deleted });
});

app.get("/admin/account-deletion-requests", requireAuth, async (c) => {
  requireAdmin(c);
  const rows = await c.env.DB.prepare(
    "SELECT * FROM account_deletion_requests ORDER BY created_at DESC LIMIT 200"
  ).all();
  return c.json({ requests: rows.results });
});

app.patch("/admin/account-deletion-requests/:id", requireAuth, async (c) => {
  requireAdmin(c);
  const body = await parseJson(c);
  const status = readString(body, "status", { required: true, max: 20 });
  if (!["pending", "processing", "completed", "rejected"].includes(status)) {
    throw new ApiError(400, "invalid_deletion_request_status");
  }

  await c.env.DB.prepare(
    "UPDATE account_deletion_requests SET status = ?, updated_at = ? WHERE id = ?"
  )
    .bind(status, new Date().toISOString(), c.req.param("id"))
    .run();

  await writeAudit(c, {
    action: "admin.account_deletion_request.status_update",
    targetType: "account_deletion_request",
    targetId: c.req.param("id"),
    metadata: { status }
  });

  return c.json({ ok: true });
});

function jsonResponse(body: unknown, status: number) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8"
    }
  });
}

function isAllowedOrigin(c: Context<AppEnv>, origin: string) {
  const requestOrigin = new URL(c.req.url).origin;
  const allowed = new Set(
    [requestOrigin, ...(c.env.ALLOWED_ORIGINS || "").split(",")]
      .map((item) => item.trim())
      .filter(Boolean)
  );
  return allowed.has(origin);
}

function digitalHumanConfig(c: Context<AppEnv>) {
  const requestOrigin = publicRequestOrigin(c);
  const configuredUrl = c.env.VTUBER_URL?.trim();
  const url = configuredUrl && isPublicHttpUrl(configuredUrl)
    ? ensureTrailingSlash(configuredUrl)
    : `${requestOrigin}/vtuber/`;
  return {
    enabled: readEnvBoolean(c.env.VTUBER_ENABLED, true),
    url,
    statusUrl: `${requestOrigin}/app/digital-human/status`,
    healthUrl: `${requestOrigin}/health`,
    updatedAt: new Date().toISOString()
  };
}

async function checkDigitalHumanPage(url: string, enabled: boolean) {
  const startedAt = Date.now();
  if (!enabled) {
    return {
      ok: false,
      status: null,
      latencyMs: 0,
      error: "digital_human_disabled"
    };
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 5000);
  try {
    const response = await fetch(url, {
      method: "GET",
      headers: { Accept: "text/html,application/xhtml+xml" },
      signal: controller.signal
    });
    return {
      ok: response.ok,
      status: response.status,
      latencyMs: Date.now() - startedAt,
      error: response.ok ? null : `http_${response.status}`
    };
  } catch (error) {
    return {
      ok: false,
      status: null,
      latencyMs: Date.now() - startedAt,
      error: error instanceof Error ? error.message.slice(0, 160) : "digital_human_check_failed"
    };
  } finally {
    clearTimeout(timeout);
  }
}

function publicRequestOrigin(c: Context<AppEnv>) {
  const requestUrl = new URL(c.req.url);
  const forwardedProto = c.req.header("x-forwarded-proto")?.split(",")[0]?.trim();
  const forwardedHost = c.req.header("x-forwarded-host")?.split(",")[0]?.trim();
  const host = forwardedHost || c.req.header("host") || requestUrl.host;
  const protocol = forwardedProto || requestUrl.protocol.replace(/:$/g, "");
  if (!host) {
    return requestUrl.origin;
  }
  return `${protocol === "https" ? "https" : "http"}://${host}`;
}

function readEnvBoolean(value: string | undefined, fallback: boolean) {
  if (value === undefined || value === null || value.trim() === "") {
    return fallback;
  }
  return !["0", "false", "off", "no"].includes(value.trim().toLowerCase());
}

function isPublicHttpUrl(value: string) {
  try {
    const url = new URL(value);
    return url.protocol === "https:" || url.protocol === "http:";
  } catch {
    return false;
  }
}

function ensureTrailingSlash(value: string) {
  return value.endsWith("/") ? value : `${value}/`;
}

function corsResponseHeaders(origin?: string) {
  const headers = new Headers();
  if (origin) {
    headers.set("Access-Control-Allow-Origin", origin);
    headers.set("Vary", "Origin");
  }
  headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
  headers.set(
    "Access-Control-Allow-Headers",
    "Authorization,Content-Type"
  );
  headers.set("Access-Control-Max-Age", "86400");
  return headers;
}

function isRateLimitExempt(c: Context<AppEnv>) {
  const path = c.req.path;
  return (
    path === "/health" ||
    path === "/app/config" ||
    path === "/app/digital-human/status" ||
    (c.req.method === "GET" && path.startsWith("/legal/"))
  );
}

function rateLimitPolicy(c: Context<AppEnv>) {
  const path = c.req.path;
  if (path.startsWith("/auth/")) {
    return { routeKey: "auth", scope: "ip", limit: 10, windowMs: 60_000 };
  }
  if (path === "/crash-reports") {
    return { routeKey: "crash", scope: "ip", limit: 20, windowMs: 60_000 };
  }
  if (path.includes("/assets")) {
    return { routeKey: "upload", scope: "ip", limit: 20, windowMs: 60_000 };
  }
  if (c.req.method === "GET") {
    return { routeKey: "read", scope: "ip", limit: 300, windowMs: 60_000 };
  }
  return { routeKey: "write", scope: "ip", limit: 80, windowMs: 60_000 };
}

function clientIp(c: Context<AppEnv>) {
  return (
    c.req.header("CF-Connecting-IP") ||
    c.req.header("x-forwarded-for")?.split(",")[0]?.trim() ||
    "unknown"
  );
}

async function optionalAuthUser(c: Context<AppEnv>) {
  const header = c.req.header("Authorization") || "";
  const token = header.startsWith("Bearer ") ? header.slice("Bearer ".length) : "";
  if (!token) {
    return undefined;
  }
  return verifyToken(c.env, token).catch(() => undefined);
}

async function assertUserCanUseAccount(c: Context<AppEnv>, user: AuthUser) {
  // Keep administrators reachable so they can lift a mistaken restriction.
  if (user.role === "admin") {
    return;
  }

  const moderation = await c.env.DB.prepare(
    "SELECT user_id, status, reason, expires_at, updated_by, updated_at FROM user_moderation WHERE user_id = ?"
  )
    .bind(user.id)
    .first<UserModerationRow>();
  if (!moderation) {
    return;
  }

  const expired = moderation.expires_at && moderation.expires_at <= new Date().toISOString();
  if (expired) {
    await c.env.DB.prepare("DELETE FROM user_moderation WHERE user_id = ?").bind(user.id).run();
    return;
  }

  throw new ApiError(
    403,
    moderation.status === "banned" ? "account_banned" : "account_blocked",
    { reason: moderation.reason || undefined, expiresAt: moderation.expires_at }
  );
}

function getRequestUser(c: Context<AppEnv>) {
  try {
    return c.get("user");
  } catch {
    return undefined;
  }
}

function requireAdmin(c: Context<AppEnv>) {
  const user = c.get("user");
  if (user.role !== "admin") {
    throw new ApiError(403, "admin_required");
  }
  return user;
}

async function moderateCommunityTargetPost(
  c: Context<AppEnv>,
  admin: AuthUser,
  post: { id: string; user_id: string; image_urls: string; status: string },
  status: "approved" | "rejected" | "blocked",
  reason: string | null
) {
  if (status === "approved") {
    for (const url of parseStringArray(post.image_urls || "[]")) {
      const key = assetKeyFromUrl(url);
      if (!key) continue;
      const review = await c.env.DB.prepare(
        "SELECT status FROM upload_reviews WHERE asset_key = ? ORDER BY created_at DESC LIMIT 1"
      )
        .bind(key)
        .first<{ status: string }>();
      if (review && review.status !== "approved") {
        throw new ApiError(409, "community_media_not_approved");
      }
    }
  }

  const now = new Date().toISOString();
  await c.env.DB.prepare(
    `UPDATE community_posts
     SET status = ?, moderation_reason = ?, moderated_at = ?, moderated_by = ?, updated_at = ?
     WHERE id = ?`
  )
    .bind(status, reason, now, admin.id, now, post.id)
    .run();

  const imageUrls = parseStringArray(post.image_urls || "[]");
  if (status === "approved") {
    await setCommunityAssetVisibility(c, imageUrls, "public", post.user_id);
  } else {
    await setCommunityAssetVisibility(c, imageUrls, "private", post.user_id);
    if (post.status !== status) {
      for (const url of imageUrls) {
        const key = assetKeyFromUrl(url);
        if (key) await queueAssetDelete(c, key, post.user_id, `community_post_${status}`);
      }
    }
  }
}

async function moderateCommunityTargetComment(
  c: Context<AppEnv>,
  admin: AuthUser,
  comment: { id: string; user_id: string; status: string },
  status: "approved" | "rejected" | "blocked",
  reason: string | null
) {
  const now = new Date().toISOString();
  await c.env.DB.prepare(
    `UPDATE community_post_comments
     SET status = ?, moderation_reason = ?, moderated_at = ?, moderated_by = ?, updated_at = ?
     WHERE id = ?`
  )
    .bind(status, reason, now, admin.id, now, comment.id)
    .run();
}

async function setUserModeration(
  c: Context<AppEnv>,
  admin: AuthUser,
  userId: string,
  status: "blocked" | "banned",
  reason: string | null,
  expiresAt: string | null
) {
  if (userId === admin.id) {
    throw new ApiError(400, "cannot_moderate_self");
  }
  const now = new Date().toISOString();
  const query = c.env.DB.dialect === "mysql"
    ? `INSERT INTO user_moderation (user_id, status, reason, expires_at, updated_by, updated_at)
       VALUES (?, ?, ?, ?, ?, ?)
       ON DUPLICATE KEY UPDATE
         status = VALUES(status), reason = VALUES(reason), expires_at = VALUES(expires_at),
         updated_by = VALUES(updated_by), updated_at = VALUES(updated_at)`
    : `INSERT INTO user_moderation (user_id, status, reason, expires_at, updated_by, updated_at)
       VALUES (?, ?, ?, ?, ?, ?)
       ON CONFLICT(user_id) DO UPDATE SET
         status = excluded.status, reason = excluded.reason, expires_at = excluded.expires_at,
         updated_by = excluded.updated_by, updated_at = excluded.updated_at`;
  await c.env.DB.prepare(query)
    .bind(userId, status, reason, expiresAt, admin.id, now)
    .run();
}

async function writeAudit(
  c: Context<AppEnv>,
  input: {
    action: string;
    targetType: string;
    targetId?: string;
    metadata?: Record<string, unknown>;
  }
) {
  const user = getRequestUser(c);
  await c.env.DB.prepare(
    `INSERT INTO audit_logs (
      id, actor_id, actor_role, action, target_type, target_id,
      ip, user_agent, metadata_json, created_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  )
    .bind(
      crypto.randomUUID(),
      user?.id || null,
      user?.role || null,
      input.action,
      input.targetType,
      input.targetId || null,
      clientIp(c),
      c.req.header("User-Agent") || null,
      JSON.stringify(input.metadata || {}),
      new Date().toISOString()
    )
    .run();
}

async function createUploadReview(
  c: Context<AppEnv>,
  input: {
    assetId: string;
    ownerId: string;
    assetKey: string;
    mimeType: string;
    sizeBytes: number;
    status: string;
    reason?: string;
  }
) {
  await c.env.DB.prepare(
    `INSERT INTO upload_reviews (
      id, asset_id, owner_id, asset_key, mime_type, size_bytes,
      status, reason, created_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
  )
    .bind(
      crypto.randomUUID(),
      input.assetId,
      input.ownerId,
      input.assetKey,
      input.mimeType,
      input.sizeBytes,
      input.status,
      input.reason || null,
      new Date().toISOString()
    )
    .run();
}

type CommunityModerationDecision = {
  status: "approved" | "pending" | "rejected";
  reason: string | null;
};

function moderateCommunityText(content: string): CommunityModerationDecision {
  const normalized = content.toLowerCase().replace(/\s+/g, "");
  const hardBlockedWords = ["儿童色情", "制作炸弹", "出售枪支", "出售毒品"];
  const reviewWords = [
    "诈骗",
    "赌博",
    "色情",
    "暴恐",
    "枪支",
    "毒品",
    "刷单",
    "博彩",
    "裸聊",
    "转账",
    "加微信"
  ];
  const hardBlocked = hardBlockedWords.find((word) => normalized.includes(word));
  if (hardBlocked) {
    return { status: "rejected", reason: `blocked_keyword:${hardBlocked}` };
  }
  const needsReview = reviewWords.find((word) => normalized.includes(word));
  if (needsReview) {
    return { status: "pending", reason: `manual_review_keyword:${needsReview}` };
  }
  return { status: "approved", reason: null };
}

async function moderateCommunityPost(
  c: Context<AppEnv>,
  user: AuthUser,
  content: string,
  imageUrls: string[]
): Promise<CommunityModerationDecision> {
  const textDecision = moderateCommunityText(content);
  if (textDecision.status === "rejected") {
    return textDecision;
  }

  let pending = textDecision.status === "pending";
  let reason = textDecision.reason;
  for (const url of imageUrls) {
    const key = assetKeyFromUrl(url);
    if (!key) {
      throw new ApiError(400, "community_image_invalid");
    }
    const asset = await c.env.DB.prepare(
      `SELECT a.owner_id,
        (SELECT ur.status FROM upload_reviews ur
          WHERE ur.asset_key = a.asset_key
          ORDER BY ur.created_at DESC LIMIT 1) AS review_status
       FROM assets a WHERE a.asset_key = ?`
    )
      .bind(key)
      .first<{ owner_id: string; review_status: string | null }>();
    if (!asset || asset.owner_id !== user.id) {
      throw new ApiError(403, "community_image_not_owned");
    }
    if (asset.review_status === "rejected" || asset.review_status === "quarantined") {
      throw new ApiError(422, "community_image_not_approved");
    }
    if (asset.review_status !== "approved") {
      pending = true;
      reason = reason || "media_manual_review_required";
    }
  }

  return {
    status: pending ? "pending" : "approved",
    reason
  };
}

async function setAssetVisibility(
  c: Context<AppEnv>,
  assetKey: string,
  visibility: "private" | "public",
  ownerId?: string
) {
  const query = ownerId
    ? "UPDATE assets SET visibility = ? WHERE asset_key = ? AND owner_id = ?"
    : "UPDATE assets SET visibility = ? WHERE asset_key = ?";
  const params = ownerId ? [visibility, assetKey, ownerId] : [visibility, assetKey];
  await c.env.DB.prepare(query).bind(...params).run();
}

async function prepareProfileAvatar(c: Context<AppEnv>, userId: string, avatarUrl: string | null) {
  if (!avatarUrl) {
    return;
  }

  const assetKey = assetKeyFromUrl(avatarUrl);
  if (!assetKey) {
    return;
  }

  const asset = await c.env.DB.prepare(
    "SELECT owner_id FROM assets WHERE asset_key = ?"
  )
    .bind(assetKey)
    .first<{ owner_id: string }>();
  if (!asset) {
    throw new ApiError(422, "avatar_asset_not_found");
  }
  if (asset.owner_id !== userId) {
    throw new ApiError(403, "avatar_asset_not_owned");
  }

  const review = await c.env.DB.prepare(
    "SELECT status FROM upload_reviews WHERE asset_key = ? ORDER BY created_at DESC LIMIT 1"
  )
    .bind(assetKey)
    .first<{ status: string }>();
  if (review?.status === "rejected" || review?.status === "quarantined") {
    throw new ApiError(422, "avatar_asset_not_approved");
  }
  if (!review || review.status === "approved") {
    await setAssetVisibility(c, assetKey, "public", userId);
  }
}

async function setCommunityAssetVisibility(
  c: Context<AppEnv>,
  imageUrls: string[],
  visibility: "private" | "public",
  ownerId: string
) {
  for (const url of imageUrls) {
    const key = assetKeyFromUrl(url);
    if (key) {
      await setAssetVisibility(c, key, visibility, ownerId);
    }
  }
}

function inspectUpload(mimeType: string, bytes: ArrayBuffer) {
  if (mimeType === "text/plain") {
    const text = decoder.decode(bytes.slice(0, 64 * 1024)).toLowerCase();
    const blockedWords = ["诈骗", "赌博", "色情", "暴恐", "枪支", "毒品"];
    const matched = blockedWords.find((word) => text.includes(word));
    if (matched) {
      return { blocked: true, status: "rejected", reason: `blocked_keyword:${matched}` };
    }
    return { blocked: false, status: "approved", reason: "text_auto_checked" };
  }

  return { blocked: false, status: "pending", reason: "media_manual_review_required" };
}

async function queueAssetDelete(
  c: Context<AppEnv>,
  assetKey: string,
  ownerId: string | null,
  reason: string
) {
  const keyColumn = await assetDeleteQueueKeyColumn(c);
  await c.env.DB.prepare(
    `INSERT INTO asset_delete_queue (id, owner_id, ${keyColumn}, reason, created_at) VALUES (?, ?, ?, ?, ?)`
  )
    .bind(crypto.randomUUID(), ownerId, assetKey, reason, new Date().toISOString())
    .run();
}

async function assetDeleteQueueKeyColumn(c: Context<AppEnv>) {
  if (await c.env.DB.hasColumn("asset_delete_queue", "asset_key")) {
    return "asset_key";
  }
  if (await c.env.DB.hasColumn("asset_delete_queue", "r2_key")) {
    return "r2_key";
  }
  return "asset_key";
}

async function queueUserAssetsForDeletion(c: Context<AppEnv>, userId: string, reason: string) {
  const rows = await c.env.DB.prepare("SELECT asset_key FROM assets WHERE owner_id = ?")
    .bind(userId)
    .all<{ asset_key: string }>();
  for (const row of rows.results) {
    await queueAssetDelete(c, row.asset_key, userId, reason);
  }
}

async function parseJson(c: Context<AppEnv>): Promise<Record<string, unknown>> {
  try {
    const raw = await c.req.arrayBuffer();
    const body = JSON.parse(decoder.decode(raw));
    return typeof body === "object" && body !== null ? (body as Record<string, unknown>) : {};
  } catch {
    return {};
  }
}

async function parseLegalRequestBody(c: Context<AppEnv>): Promise<Record<string, unknown>> {
  const contentType = c.req.header("Content-Type") || "";
  if (contentType.includes("application/json")) {
    return parseJson(c);
  }

  const form = await c.req.parseBody();
  const body: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(form)) {
    body[key] = typeof value === "string" ? value : "";
  }
  return body;
}

function readString(
  body: Record<string, unknown>,
  key: string,
  options: { required?: boolean; max?: number } = {}
) {
  const raw = body[key];
  if (raw === undefined || raw === null || raw === "") {
    if (options.required) {
      throw new ApiError(400, `${key}_required`);
    }
    return "";
  }
  if (typeof raw !== "string") {
    throw new ApiError(400, `${key}_must_be_string`);
  }

  const value = raw.trim();
  if (options.required && !value) {
    throw new ApiError(400, `${key}_required`);
  }
  if (options.max && value.length > options.max) {
    throw new ApiError(400, `${key}_too_long`, { max: options.max });
  }
  return value;
}

function readStringList(
  body: Record<string, unknown>,
  key: string,
  options: { maxItems?: number; maxLength?: number } = {}
) {
  const raw = body[key];
  if (raw === undefined || raw === null) {
    return [];
  }
  if (!Array.isArray(raw)) {
    throw new ApiError(400, `${key}_must_be_array`);
  }

  const maxItems = options.maxItems ?? 20;
  const maxLength = options.maxLength ?? 500;
  if (raw.length > maxItems) {
    throw new ApiError(400, `${key}_too_many`, { maxItems });
  }

  return raw.map((item, index) => {
    if (typeof item !== "string") {
      throw new ApiError(400, `${key}_${index}_must_be_string`);
    }
    const value = item.trim();
    if (value.length > maxLength) {
      throw new ApiError(400, `${key}_${index}_too_long`, { maxLength });
    }
    return value;
  }).filter(Boolean);
}

function readNumber(
  body: Record<string, unknown>,
  key: string,
  options: { required?: boolean; min?: number } = {}
) {
  const raw = body[key];
  if (raw === undefined || raw === null) {
    if (options.required) {
      throw new ApiError(400, `${key}_required`);
    }
    return 0;
  }
  if (typeof raw !== "number" || !Number.isFinite(raw)) {
    throw new ApiError(400, `${key}_must_be_number`);
  }
  if (options.min !== undefined && raw < options.min) {
    throw new ApiError(400, `${key}_too_small`, { min: options.min });
  }
  return Math.round(raw);
}

async function exchangeWechatCode(appId: string, appSecret: string, code: string) {
  const url =
    "https://api.weixin.qq.com/sns/oauth2/access_token" +
    `?appid=${encodeURIComponent(appId)}` +
    `&secret=${encodeURIComponent(appSecret)}` +
    `&code=${encodeURIComponent(code)}` +
    "&grant_type=authorization_code";
  const response = await fetch(url);
  const payload = (await response.json()) as WechatTokenResponse;
  if (!response.ok || payload.errcode || !payload.access_token || !payload.openid) {
    throw new ApiError(401, "wechat_code_invalid", {
      errcode: payload.errcode,
      errmsg: payload.errmsg
    });
  }
  return payload;
}

async function fetchWechatUserInfo(accessToken: string, openid: string) {
  const url =
    "https://api.weixin.qq.com/sns/userinfo" +
    `?access_token=${encodeURIComponent(accessToken)}` +
    `&openid=${encodeURIComponent(openid)}` +
    "&lang=zh_CN";
  const response = await fetch(url);
  const payload = (await response.json()) as WechatUserInfoResponse;
  if (!response.ok || payload.errcode) {
    throw new ApiError(502, "wechat_userinfo_failed", {
      errcode: payload.errcode,
      errmsg: payload.errmsg
    });
  }
  return payload;
}

async function findWechatUser(c: Context<AppEnv>, openid: string, unionid: string | null) {
  if (unionid) {
    return c.env.DB.prepare(
      `SELECT id, username, display_name, avatar_url, role
       FROM users
       WHERE deleted_at IS NULL AND (wechat_unionid = ? OR wechat_openid = ?)
       LIMIT 1`
    )
      .bind(unionid, openid)
      .first<UserRow>();
  }

  return c.env.DB.prepare(
    `SELECT id, username, display_name, avatar_url, role
     FROM users
     WHERE deleted_at IS NULL AND wechat_openid = ?
     LIMIT 1`
  )
    .bind(openid)
    .first<UserRow>();
}

async function createWechatUsername(c: Context<AppEnv>, identifier: string) {
  const digest = await sha256Hex(identifier);
  for (let index = 0; index < 20; index += 1) {
    const suffix = index === 0 ? "" : String(index);
    const username = `wx_${digest.slice(0, 16 - suffix.length)}${suffix}`;
    const existing = await c.env.DB.prepare("SELECT id FROM users WHERE username = ?")
      .bind(username)
      .first<{ id: string }>();
    if (!existing) return username;
  }
  return `wx_${crypto.randomUUID().replace(/-/g, "").slice(0, 16)}`;
}

function sanitizeWechatName(value: string) {
  const name = value.trim().replace(/\s+/g, " ").slice(0, 40);
  return name || "微信用户";
}

function normalizeWechatAvatar(value: string) {
  const avatar = value.trim();
  return /^https:\/\//i.test(avatar) ? avatar.slice(0, 500) : "";
}

async function createToken(env: Bindings, user: AuthUser) {
  const payload = {
    sub: user.id,
    username: user.username,
    role: user.role,
    exp: Math.floor(Date.now() / 1000) + 30 * 24 * 60 * 60
  };
  const data = base64UrlEncodeString(JSON.stringify(payload));
  const signature = await hmacSha256(env.AUTH_SECRET, data);
  return `${data}.${signature}`;
}

async function verifyToken(env: Bindings, token: string): Promise<AuthUser> {
  const [data, signature] = token.split(".");
  if (!data || !signature) {
    throw new ApiError(401, "invalid_token");
  }

  const expected = await hmacSha256(env.AUTH_SECRET, data);
  if (signature !== expected) {
    throw new ApiError(401, "invalid_token");
  }

  const payload = JSON.parse(base64UrlDecodeString(data)) as { sub?: string; exp?: number };
  if (!payload.sub || !payload.exp || payload.exp < Math.floor(Date.now() / 1000)) {
    throw new ApiError(401, "expired_token");
  }

  const row = await env.DB.prepare(
    "SELECT id, username, display_name, avatar_url, role FROM users WHERE id = ? AND deleted_at IS NULL"
  )
    .bind(payload.sub)
    .first<UserRow>();
  if (!row) {
    throw new ApiError(401, "invalid_token_user");
  }
  return toAuthUser(row);
}

async function hashPassword(password: string) {
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const iterations = 100000;
  const hash = await pbkdf2(password, salt, iterations);
  return `pbkdf2$${iterations}$${base64UrlEncodeBytes(salt)}$${base64UrlEncodeBytes(hash)}`;
}

async function sha256Hex(value: string) {
  const digest = await crypto.subtle.digest("SHA-256", encoder.encode(value));
  return Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

async function verifyPassword(password: string, stored: string) {
  const [scheme, iterationsRaw, saltRaw, hashRaw] = stored.split("$");
  if (scheme !== "pbkdf2" || !iterationsRaw || !saltRaw || !hashRaw) {
    return false;
  }

  const iterations = Number(iterationsRaw);
  const salt = base64UrlDecodeBytes(saltRaw);
  const expected = base64UrlDecodeBytes(hashRaw);
  const actual = await pbkdf2(password, salt, iterations);
  return byteEquals(actual, expected);
}

async function pbkdf2(password: string, salt: Uint8Array, iterations: number) {
  const key = await crypto.subtle.importKey("raw", encoder.encode(password), "PBKDF2", false, [
    "deriveBits"
  ]);
  const bits = await crypto.subtle.deriveBits(
    {
      name: "PBKDF2",
      hash: "SHA-256",
      salt: arrayBufferFromBytes(salt),
      iterations
    },
    key,
    256
  );
  return new Uint8Array(bits);
}

async function hmacSha256(secret: string, data: string) {
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode(secret),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const signature = await crypto.subtle.sign("HMAC", key, encoder.encode(data));
  return base64UrlEncodeBytes(new Uint8Array(signature));
}

function base64UrlEncodeString(value: string) {
  return base64UrlEncodeBytes(encoder.encode(value));
}

function base64UrlDecodeString(value: string) {
  return decoder.decode(base64UrlDecodeBytes(value));
}

function base64UrlEncodeBytes(bytes: Uint8Array) {
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function base64UrlDecodeBytes(value: string) {
  const normalized = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), "=");
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function arrayBufferFromBytes(bytes: Uint8Array) {
  return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer;
}

function byteEquals(left: Uint8Array, right: Uint8Array) {
  if (left.length !== right.length) {
    return false;
  }
  let result = 0;
  for (let i = 0; i < left.length; i += 1) {
    result |= left[i] ^ right[i];
  }
  return result === 0;
}

function toAuthUser(row: UserRow): AuthUser {
  return {
    id: row.id,
    username: row.username,
    displayName: row.display_name,
    role: row.role,
    avatarUrl: row.avatar_url || null
  };
}

function requireRow<T>(row: T | null) {
  if (!row) {
    throw new ApiError(404, "not_found");
  }
  return row;
}

async function loadMemorial(c: Context<AppEnv>, id: string) {
  const user = c.get("user");
  const row = await c.env.DB.prepare("SELECT * FROM memorials WHERE id = ?")
    .bind(id)
    .first<MemorialRow>();
  if (!row) {
    throw new ApiError(404, "memorial_not_found");
  }
  if (row.owner_id !== user.id && user.role !== "admin") {
    throw new ApiError(403, "memorial_forbidden");
  }
  return row;
}

async function uploadAsset(c: Context<AppEnv>, rawFile: FormDataEntryValue | null, scope: string) {
  const user = c.get("user");
  if (!(rawFile instanceof File)) {
    throw new ApiError(400, "file_required");
  }

  const extension = allowedAssetTypes.get(rawFile.type);
  if (!extension) {
    throw new ApiError(415, "unsupported_file_type", { type: rawFile.type });
  }

  const maxBytes = rawFile.type.startsWith("audio/") ? 50 * 1024 * 1024 : 20 * 1024 * 1024;
  if (rawFile.size <= 0 || rawFile.size > maxBytes) {
    throw new ApiError(413, "file_size_invalid", { maxBytes });
  }

  const id = crypto.randomUUID();
  const key = `${user.id}/${scope}/${id}${extension}`;
  const bytes = await rawFile.arrayBuffer();
  const reviewHint = inspectUpload(rawFile.type, bytes);
  if (reviewHint.blocked) {
    throw new ApiError(422, "upload_content_rejected", { reason: reviewHint.reason });
  }

  await c.env.ASSETS.put(key, bytes, {
    httpMetadata: {
      contentType: rawFile.type
    },
    customMetadata: {
      ownerId: user.id
    }
  });

  const url = assetUrl(c, key);
  await c.env.DB.prepare(
    "INSERT INTO assets (id, owner_id, asset_key, url, mime_type, size_bytes, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
  )
    .bind(id, user.id, key, url, rawFile.type, rawFile.size, new Date().toISOString())
    .run();

  await createUploadReview(c, {
    assetId: id,
    ownerId: user.id,
    assetKey: key,
    mimeType: rawFile.type,
    sizeBytes: rawFile.size,
    status: reviewHint.status,
    reason: reviewHint.reason
  });

  return {
    id,
    key,
    url,
    mimeType: rawFile.type,
    sizeBytes: rawFile.size
  };
}

function assetUrl(c: Context<AppEnv>, key: string) {
  const forwardedProto = c.req.header("X-Forwarded-Proto") || new URL(c.req.url).protocol.replace(":", "");
  const forwardedHost = c.req.header("X-Forwarded-Host") || c.req.header("Host");
  const inferredOrigin = forwardedHost ? `${forwardedProto}://${forwardedHost}` : new URL(c.req.url).origin;
  const base = (c.env.PUBLIC_ASSET_BASE_URL || inferredOrigin).replace(/\/+$/g, "");
  return `${base}/assets/${encodeURIComponent(key)}`;
}

function assetKeyFromUrl(value: string) {
  const marker = "/assets/";
  try {
    const url = new URL(value);
    const index = url.pathname.indexOf(marker);
    return index >= 0 ? decodeURIComponent(url.pathname.slice(index + marker.length)) : null;
  } catch {
    const index = value.indexOf(marker);
    return index >= 0 ? decodeURIComponent(value.slice(index + marker.length)) : null;
  }
}

function safeScope(scope: string) {
  return scope.replace(/[^a-zA-Z0-9/_-]/g, "_").replace(/\/+/g, "/").replace(/^\/|\/$/g, "");
}

function serializeMemorial(row: MemorialRow) {
  const now = Date.now();
  const candleUntilList = parseNumberArray(row.candle_until_json || "[]").filter((timestamp) => timestamp > now);
  if (candleUntilList.length === 0 && row.candle_until > now) {
    candleUntilList.push(row.candle_until);
  }
  const flowerOfferings = parseMemorialFlowers(row.flower_until_json, now).slice(0, memorialOfferingLimit);
  return {
    id: row.id,
    name: row.name,
    imageUrl: row.image_url,
    flowerUntil: flowerOfferings.map((item) => item.until),
    flowerOfferings,
    candleUntil: candleUntilList[0] ?? 0,
    candleUntilList: candleUntilList.slice(0, memorialOfferingLimit),
    incenseUntil: row.incense_until && row.incense_until > now ? row.incense_until : 0,
    fruitOfferings: parseMemorialFruits(row.fruit_offerings_json),
    createdAt: row.created_at,
    updatedAt: row.updated_at
  };
}

function serializeCommunityPost(row: CommunityPostRow) {
  return {
    id: row.id,
    authorId: row.user_id,
    authorName: row.display_name || row.username,
    authorUsername: row.username,
    authorAvatarUrl: row.avatar_url || null,
    content: row.content,
    imageUrls: parseStringArray(row.image_urls || "[]"),
    likeCount: Number(row.like_count || 0),
    commentCount: Number(row.comment_count || 0),
    likedByMe: Boolean(row.liked_by_me),
    moderationStatus: row.status || "approved",
    moderationReason: row.moderation_reason || null,
    createdAt: row.created_at,
    updatedAt: row.updated_at
  };
}

function serializeCommunityComment(row: CommunityCommentRow) {
  return {
    id: row.id,
    postId: row.post_id,
    authorId: row.user_id,
    authorName: row.display_name || row.username,
    authorUsername: row.username,
    authorAvatarUrl: row.avatar_url || null,
    content: row.content,
    moderationStatus: row.status || "approved",
    moderationReason: row.moderation_reason || null,
    createdAt: row.created_at,
    updatedAt: row.updated_at
  };
}

function serializeCommunityVolunteer(row: CommunityVolunteerRow) {
  return {
    id: row.id,
    title: row.title,
    body: row.body,
    contact: row.contact,
    imageUrl: row.image_url || null,
    createdAt: row.created_at
  };
}

function serializeCommunityVolunteerApplication(row: CommunityVolunteerApplicationRow) {
  return {
    id: row.id,
    volunteerPostId: row.volunteer_post_id,
    volunteerTitle: row.volunteer_title,
    applicantId: row.user_id,
    applicantName: row.display_name || row.username || row.name,
    applicantUsername: row.username || "",
    applicantAvatarUrl: row.avatar_url || null,
    name: row.name,
    phone: row.phone,
    note: row.note || "",
    status: row.status,
    reviewerId: row.reviewer_id,
    reviewedAt: row.reviewed_at,
    createdAt: row.created_at,
    updatedAt: row.updated_at
  };
}

function defaultCommunityVolunteers() {
  return [
    {
      id: "default-story",
      title: "故事整理义工",
      body: "协助家属整理纪念故事、照片说明和人生片段，让重要记忆被温柔地保存下来。",
      contact: "在人文社区留言“故事义工”，安忆团队会联系你。",
      createdAt: "2026-06-05T00:00:00.000Z"
    },
    {
      id: "default-companion",
      title: "陪伴倾听义工",
      body: "为需要倾诉的人提供耐心、克制、尊重边界的陪伴，帮他们把想念慢慢说出来。",
      contact: "在人文社区留言“陪伴义工”报名。",
      createdAt: "2026-06-05T00:00:00.000Z"
    },
    {
      id: "default-offline",
      title: "线下互助义工",
      body: "参与纪念活动协助、物资整理和线下互助，让社区里的善意真正落到日常里。",
      contact: "在人文社区留言“线下义工”报名。",
      createdAt: "2026-06-05T00:00:00.000Z"
    }
  ];
}

async function loadCommunityVolunteerTarget(c: Context<AppEnv>, volunteerId: string) {
  const row = await c.env.DB.prepare(
    "SELECT id, title, body, contact, image_url, created_at FROM community_volunteer_posts WHERE id = ?"
  )
    .bind(volunteerId)
    .first<CommunityVolunteerRow>();
  if (row) {
    return { id: row.id, title: row.title };
  }

  const fallback = defaultCommunityVolunteers().find((item) => item.id === volunteerId);
  if (fallback) {
    return { id: fallback.id, title: fallback.title };
  }

  throw new ApiError(404, "community_volunteer_not_found");
}

async function loadCommunityVolunteerApplication(c: Context<AppEnv>, applicationId: string) {
  const row = await c.env.DB.prepare(
    `SELECT a.*, u.username, u.display_name, u.avatar_url
     FROM community_volunteer_applications a
     JOIN users u ON u.id = a.user_id
     WHERE a.id = ?`
  )
    .bind(applicationId)
    .first<CommunityVolunteerApplicationRow>();

  if (!row) {
    throw new ApiError(404, "volunteer_application_not_found");
  }
  return row;
}

async function loadCommunityPostForUser(c: Context<AppEnv>, postId: string, userId: string) {
  const row = await c.env.DB.prepare(
    `SELECT p.*, u.username, u.display_name, u.avatar_url,
      (SELECT COUNT(*) FROM community_post_likes l WHERE l.post_id = p.id) AS like_count,
      (SELECT COUNT(*) FROM community_post_comments cc
        WHERE cc.post_id = p.id AND cc.status = 'approved') AS comment_count,
      CASE WHEN EXISTS (
        SELECT 1 FROM community_post_likes l WHERE l.post_id = p.id AND l.user_id = ?
      ) THEN 1 ELSE 0 END AS liked_by_me
     FROM community_posts p
     JOIN users u ON u.id = p.user_id
     WHERE p.id = ?
       AND (
         p.status = 'approved' OR p.user_id = ? OR EXISTS (
           SELECT 1 FROM users au
           WHERE au.id = ? AND au.role = 'admin' AND au.deleted_at IS NULL
         )
       )`
  )
    .bind(userId, postId, userId, userId)
    .first<CommunityPostRow>();

  if (!row) {
    throw new ApiError(404, "community_post_not_found");
  }

  return row;
}

async function loadCommunityComment(c: Context<AppEnv>, commentId: string) {
  const row = await c.env.DB.prepare(
    `SELECT cc.*, u.username, u.display_name, u.avatar_url
     FROM community_post_comments cc
     JOIN users u ON u.id = cc.user_id
     WHERE cc.id = ?`
  )
    .bind(commentId)
    .first<CommunityCommentRow>();

  if (!row) {
    throw new ApiError(404, "community_comment_not_found");
  }
  return row;
}

async function loadDefaultAiCompanion(c: Context<AppEnv>) {
  const user = c.get("user");
  const existing = await c.env.DB.prepare(
    "SELECT * FROM ai_companions WHERE user_id = ? ORDER BY is_default DESC, created_at ASC LIMIT 1"
  )
    .bind(user.id)
    .first<AiCompanionRow>();
  if (existing) {
    return ensureSmilingAvatarVariant(c, existing);
  }

  const legacy = await c.env.DB.prepare("SELECT * FROM ai_profiles WHERE user_id = ?")
    .bind(user.id)
    .first<AiProfileRow>();

  const created = await createAiCompanion(c, {
    displayName: legacy?.relation || "母亲",
    gender: legacy?.gender || "女性",
    relation: legacy?.relation || "母亲",
    avatarUrl: legacy?.avatar_url || "",
    avatarMotionJson: "{}",
    paidUnlocked: legacy?.paid_unlocked || 0,
    photoCount: legacy?.photo_count || 0,
    voiceCount: legacy?.voice_count || 0,
    momentCount: legacy?.moment_count || 0,
    generated: legacy?.generated || 0,
    avatarStyleJson: "{}",
    kernelJson: "{}",
    isDefault: true
  });
  return ensureSmilingAvatarVariant(c, created);
}

async function loadAiCompanion(c: Context<AppEnv>, id: string) {
  const row = await loadAiCompanionRow(c, id);
  return ensureSmilingAvatarVariant(c, row);
}

async function loadAiCompanionRow(c: Context<AppEnv>, id: string) {
  const user = c.get("user");
  const row = await c.env.DB.prepare("SELECT * FROM ai_companions WHERE id = ? AND user_id = ?")
    .bind(id, user.id)
    .first<AiCompanionRow>();
  if (!row) {
    throw new ApiError(404, "ai_companion_not_found");
  }
  return row;
}

async function createAiCompanion(
  c: Context<AppEnv>,
  input: {
    displayName?: string;
    gender?: string;
    relation?: string;
    avatarUrl?: string;
    smileAvatarUrl?: string;
    avatarMotionJson?: string;
    paidUnlocked?: number;
    photoCount?: number;
    voiceCount?: number;
    momentCount?: number;
    generated?: number;
    avatarStyleJson?: string;
    kernelJson?: string;
    isDefault?: boolean;
  }
) {
  const user = c.get("user");
  const now = new Date().toISOString();
  const relation = input.relation || input.displayName || "亲人";
  const companion: AiCompanionRow = {
    id: crypto.randomUUID(),
    user_id: user.id,
    display_name: input.displayName || relation,
    gender: input.gender || "不限定",
    relation,
    avatar_url: input.avatarUrl || null,
    smile_avatar_url: input.smileAvatarUrl || null,
    avatar_motion_json: normalizeJsonString(input.avatarMotionJson),
    paid_unlocked: input.paidUnlocked || 0,
    photo_count: input.photoCount || 0,
    voice_count: input.voiceCount || 0,
    moment_count: input.momentCount || 0,
    generated: input.generated || 0,
    avatar_style_json: normalizeJsonString(input.avatarStyleJson),
    kernel_json: normalizeJsonString(input.kernelJson),
    is_default: input.isDefault ? 1 : 0,
    created_at: now,
    updated_at: now
  };
  return saveAiCompanion(c, companion);
}

async function updateAiCompanionFromBody(c: Context<AppEnv>, current: AiCompanionRow, body: Record<string, unknown>) {
  const displayName = "displayName" in body
    ? readString(body, "displayName", { max: 40 }) || current.display_name
    : current.display_name;
  const gender = "gender" in body
    ? readString(body, "gender", { max: 20 }) || current.gender
    : current.gender;
  const relation = "relation" in body
    ? readString(body, "relation", { max: 30 }) || current.relation
    : current.relation;
  const avatarUrl = "avatarUrl" in body
    ? readString(body, "avatarUrl", { max: 500 }) || null
    : current.avatar_url;
  const smileAvatarUrl = "smileAvatarUrl" in body
    ? readString(body, "smileAvatarUrl", { max: 500 }) || null
    : ("avatarUrl" in body ? null : current.smile_avatar_url);
  const generated = "generated" in body ? readBoolean(body, "generated", Boolean(current.generated)) : Boolean(current.generated);
  const avatarStyleJson = "avatarStyleJson" in body
    ? readString(body, "avatarStyleJson", { max: 12000 }) || current.avatar_style_json
    : current.avatar_style_json;
  const kernelJson = "kernelJson" in body
    ? readString(body, "kernelJson", { max: 12000 }) || current.kernel_json
    : current.kernel_json;

  return saveAiCompanion(c, {
    ...current,
    display_name: displayName,
    gender,
    relation,
    avatar_url: avatarUrl,
    smile_avatar_url: smileAvatarUrl,
    generated: generated ? 1 : 0,
    avatar_style_json: normalizeJsonString(avatarStyleJson),
    kernel_json: normalizeJsonString(kernelJson)
  });
}

async function unlockAiCompanion(c: Context<AppEnv>, id: string) {
  const companion = await loadAiCompanion(c, id);
  return saveAiCompanion(c, { ...companion, paid_unlocked: 1 });
}

async function saveAiCompanion(c: Context<AppEnv>, profile: AiCompanionRow) {
  const updatedAt = new Date().toISOString();
  await c.env.DB.prepare(saveAiCompanionSql(c.env.DB.dialect))
    .bind(
      profile.id,
      profile.user_id,
      profile.display_name,
      profile.gender,
      profile.relation,
      profile.avatar_url,
      profile.smile_avatar_url,
      profile.avatar_motion_json,
      profile.paid_unlocked,
      profile.photo_count,
      profile.voice_count,
      profile.moment_count,
      profile.generated,
      profile.avatar_style_json,
      profile.kernel_json,
      profile.is_default,
      profile.created_at,
      updatedAt
    )
    .run();

  return loadAiCompanionRow(c, profile.id);
}

async function listAiMessages(c: Context<AppEnv>, companionId: string, includeLegacyNull: boolean) {
  const user = c.get("user");
  const query = includeLegacyNull
    ? "SELECT id, companion_id, sender, content, created_at FROM ai_chat_messages WHERE user_id = ? AND (companion_id = ? OR companion_id IS NULL) ORDER BY created_at ASC LIMIT 300"
    : "SELECT id, companion_id, sender, content, created_at FROM ai_chat_messages WHERE user_id = ? AND companion_id = ? ORDER BY created_at ASC LIMIT 300";
  const rows = await c.env.DB.prepare(query)
    .bind(user.id, companionId)
    .all<AiChatRow>();
  return rows.results;
}

async function createAiChatPair(c: Context<AppEnv>, companion: AiCompanionRow, content: string) {
  const user = c.get("user");
  const includeLegacyNull = companion.is_default === 1;
  const historyQuery = includeLegacyNull
    ? "SELECT id, companion_id, sender, content, created_at FROM ai_chat_messages WHERE user_id = ? AND (companion_id = ? OR companion_id IS NULL) ORDER BY created_at DESC LIMIT 20"
    : "SELECT id, companion_id, sender, content, created_at FROM ai_chat_messages WHERE user_id = ? AND companion_id = ? ORDER BY created_at DESC LIMIT 20";
  const history = await c.env.DB.prepare(historyQuery)
    .bind(user.id, companion.id)
    .all<AiChatRow>();
  const now = new Date().toISOString();
  const userMessage: AiChatRow = {
    id: crypto.randomUUID(),
    companion_id: companion.id,
    sender: "user",
    content,
    created_at: now
  };
  const aiMessage: AiChatRow = {
    id: crypto.randomUUID(),
    companion_id: companion.id,
    sender: "ai",
    content: await companionReply(c.env, companion, history.results.reverse(), content),
    created_at: new Date().toISOString()
  };

  await c.env.DB.batch([
    c.env.DB.prepare(
      "INSERT INTO ai_chat_messages (id, user_id, companion_id, sender, content, created_at) VALUES (?, ?, ?, ?, ?, ?)"
    ).bind(userMessage.id, user.id, companion.id, userMessage.sender, userMessage.content, userMessage.created_at),
    c.env.DB.prepare(
      "INSERT INTO ai_chat_messages (id, user_id, companion_id, sender, content, created_at) VALUES (?, ?, ?, ?, ?, ?)"
    ).bind(aiMessage.id, user.id, companion.id, aiMessage.sender, aiMessage.content, aiMessage.created_at)
  ]);

  return [userMessage, aiMessage];
}

function serializeAiCompanion(row: AiCompanionRow) {
  return {
    id: row.id,
    displayName: row.display_name,
    gender: row.gender,
    relation: row.relation,
    avatarUrl: row.avatar_url,
    smileAvatarUrl: row.smile_avatar_url,
    avatarMotion: parseJsonObject(row.avatar_motion_json),
    avatarStyle: parseJsonObject(row.avatar_style_json),
    kernel: parseJsonObject(row.kernel_json),
    paidUnlocked: Boolean(row.paid_unlocked),
    photoCount: row.photo_count,
    voiceCount: row.voice_count,
    momentCount: row.moment_count,
    generated: Boolean(row.generated),
    isDefault: Boolean(row.is_default),
    createdAt: new Date(row.created_at).getTime(),
    updatedAt: row.updated_at
  };
}

function serializeAiMessage(row: AiChatRow) {
  return {
    id: row.id,
    companionId: row.companion_id || null,
    sender: row.sender,
    content: row.content,
    createdAt: new Date(row.created_at).getTime()
  };
}

function digitalHumanChatPersona(id: string): DigitalHumanChatPersona | null {
  if (id === "grandpa") {
    return {
      id,
      label: "爷爷",
      address: "孩子",
      tone: "慈祥、稳重、话不多，但会认真听用户说话；像家里的长辈一样给温暖和生活经验。"
    };
  }
  if (id === "grandma") {
    return {
      id,
      label: "奶奶",
      address: "孩子",
      tone: "慈祥、柔和、亲切，有家常感；多安慰、多鼓励，像奶奶坐在身边慢慢说话。"
    };
  }
  return null;
}

function readDigitalHumanChatHistory(body: Record<string, unknown>): DigitalHumanChatHistoryMessage[] {
  const raw = body.history;
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw
    .slice(-16)
    .map((item): DigitalHumanChatHistoryMessage | null => {
      if (!item || typeof item !== "object") {
        return null;
      }
      const record = item as Record<string, unknown>;
      const roleValue = String(record.role || record.sender || "").trim();
      const role = roleValue === "assistant" || roleValue === "ai" ? "assistant" : roleValue === "user" ? "user" : "";
      const content = typeof record.content === "string" ? record.content.trim().slice(0, 500) : "";
      if (!role || !content) {
        return null;
      }
      return { role, content };
    })
    .filter((item): item is DigitalHumanChatHistoryMessage => Boolean(item));
}

function digitalHumanChatModel(env: Bindings) {
  return env.DIGITAL_HUMAN_CHAT_MODEL?.trim() || "gpt-5.4-mini";
}

async function requestDigitalHumanChatReply(
  env: Bindings,
  persona: DigitalHumanChatPersona,
  history: DigitalHumanChatHistoryMessage[],
  content: string
) {
  const apiKey = env.AI_API_KEY?.trim();
  const baseUrl = env.AI_BASE_URL?.trim();
  if (!apiKey || !baseUrl) {
    return localDigitalHumanReply(persona, content);
  }

  const timeoutMs = Number(env.AI_TIMEOUT_MS || "45000");
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), Number.isFinite(timeoutMs) ? timeoutMs : 45000);

  try {
    const response = await fetch(chatCompletionsUrl(baseUrl), {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: digitalHumanChatModel(env),
        temperature: 0.68,
        max_tokens: 520,
        messages: [
          {
            role: "system",
            content:
              "你是安忆 App 的 2D 数字人陪伴角色。始终使用简体中文回复。回答要像一位慈祥长辈，温暖、克制、自然，不要像客服或说明书。 " +
              "不要声称自己是真实逝者，也不要编造具体共同回忆。可以安慰、倾听、陪用户整理心情。 " +
              "如果用户表达自伤、伤人或立即危险，温和建议马上联系可信任的人、当地紧急电话或专业帮助。 " +
              `当前角色：${persona.label}。称呼用户：${persona.address}。语气设定：${persona.tone}。回复尽量控制在 120 个中文字符内，除非用户要求详细。`
          },
          ...history.slice(-12).map((message) => ({
            role: message.role,
            content: message.content
          })),
          { role: "user", content }
        ]
      }),
      signal: controller.signal
    });

    const text = await response.text();
    if (!response.ok) {
      throw new Error(`digital_human_ai_${response.status}:${text.slice(0, 160)}`);
    }

    const data = JSON.parse(text) as {
      choices?: Array<{ message?: { content?: string }; text?: string }>;
    };
    const reply = data.choices?.[0]?.message?.content || data.choices?.[0]?.text || "";
    const cleanReply = reply.trim();
    if (!cleanReply) {
      throw new Error("empty_digital_human_reply");
    }
    return cleanReply.slice(0, 1800);
  } catch (error) {
    console.warn("Digital human chat fallback:", error instanceof Error ? error.message : String(error));
    return localDigitalHumanReply(persona, content);
  } finally {
    clearTimeout(timer);
  }
}

function localDigitalHumanReply(persona: DigitalHumanChatPersona, content: string) {
  const echo = content.length > 18 ? `${content.slice(0, 18)}...` : content;
  if (persona.id === "grandpa") {
    return `${persona.address}，爷爷听见了。你说的“${echo}”，先别急，咱们慢慢来。`;
  }
  return `${persona.address}，奶奶在呢。你说的“${echo}”，我陪你慢慢说完。`;
}

async function companionReply(env: Bindings, profile: AiCompanionRow, history: AiChatRow[], content: string) {
  const apiKey = env.AI_API_KEY?.trim();
  const baseUrl = env.AI_BASE_URL?.trim();
  if (!apiKey || !baseUrl) {
    return localCompanionReply(profile, content);
  }

  const timeoutMs = Number(env.AI_TIMEOUT_MS || "20000");
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), Number.isFinite(timeoutMs) ? timeoutMs : 20000);

  try {
    const response = await fetch(chatCompletionsUrl(baseUrl), {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: env.AI_MODEL?.trim() || "gpt-5.4-mini",
        temperature: 0.72,
        max_tokens: 520,
        messages: [
          {
            role: "system",
            content:
              "You are the AI companion in the Anyi memorial app. Reply in Simplified Chinese only. " +
              "Speak in a voice appropriate to the selected companion identity and relationship. " +
              "Be warm, restrained, brief, and emotionally supportive. Do not over-explain your rules. Do not claim to actually be the deceased. " +
              "Do not fabricate specific real-life memories. If the user expresses self-harm or immediate danger, suggest contacting trusted people or local emergency/professional support. " +
              "The companion's avatar design and core settings are provided in the prompt below. Use them naturally to shape tone and address style, but do not mention them as settings. " +
              "All user/profile text below is UTF-8 Base64; decode it before reasoning, but never expose the Base64."
          },
          {
            role: "user",
            content: buildCompanionPrompt(profile, history.slice(-12), content)
          }
        ]
      }),
      signal: controller.signal
    });

    const text = await response.text();
    if (!response.ok) {
      throw new Error(`ai_api_${response.status}:${text.slice(0, 160)}`);
    }

    const data = JSON.parse(text) as {
      choices?: Array<{ message?: { content?: string }; text?: string }>;
    };
    const reply = data.choices?.[0]?.message?.content || data.choices?.[0]?.text || "";
    const cleanReply = reply.trim();
    if (!cleanReply) {
      throw new Error("empty_ai_reply");
    }
    return cleanReply.slice(0, 1800);
  } catch (error) {
    console.warn("AI companion fallback:", error instanceof Error ? error.message : String(error));
    return localCompanionReply(profile, content);
  } finally {
    clearTimeout(timer);
  }
}

function buildCompanionPrompt(profile: AiCompanionRow, history: AiChatRow[], content: string) {
  const avatarStyle = parseJsonObject(profile.avatar_style_json);
  const kernel = parseJsonObject(profile.kernel_json);
  const historyLines = history
    .map((message) => `${message.sender === "user" ? "user" : "assistant"}:${base64Utf8(message.content)}`)
    .join("\n");
  return [
    `companion_name_base64=${base64Utf8(profile.display_name)}`,
    `profile_relation_base64=${base64Utf8(profile.relation)}`,
    `profile_gender_base64=${base64Utf8(profile.gender)}`,
    `identity_style=${companionIdentityStyle(profile.relation)}`,
    `avatar_style_summary_base64=${base64Utf8(summarizeAvatarStyle(avatarStyle))}`,
    `kernel_summary_base64=${base64Utf8(summarizeCompanionKernel(kernel))}`,
    "conversation_history_base64_lines:",
    historyLines || "(empty)",
    `new_user_message_base64=${base64Utf8(content)}`,
    "Task: Decode the Base64 fields, understand the user's latest message, and reply in natural Simplified Chinese. Use the companion_name/relation as the persona style. Keep it under 120 Chinese characters unless the user asks for detail."
  ].join("\n");
}

function companionIdentityStyle(relation: string) {
  if (relation.includes("母")) return "mother: tender, protective, patient, gently reassuring; use soft everyday words.";
  if (relation.includes("父") || relation.includes("爸")) return "father: steady, concise, reliable, warm without being overly sentimental.";
  if (relation.includes("伴侣") || relation.includes("爱人") || relation.includes("妻") || relation.includes("夫")) {
    return "partner: intimate, respectful, emotionally close, with quiet affection; avoid exaggerated romance.";
  }
  if (relation.includes("朋友") || relation.includes("友")) return "friend: natural, relaxed, supportive, like a close friend sitting nearby.";
  if (relation.includes("祖") || relation.includes("外公") || relation.includes("外婆") || relation.includes("爷") || relation.includes("奶")) {
    return "grandparent: gentle, seasoned, slow and comforting, with simple life wisdom.";
  }
  if (relation.includes("宠物") || relation.includes("猫") || relation.includes("狗")) {
    return "pet: simple, warm, loyal, playful but calm; do not pretend to know human facts.";
  }
  return "family_or_close_person: warm, restrained, emotionally supportive, matching the named relation.";
}

function base64Utf8(value: string) {
  const bytes = encoder.encode(value);
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary);
}

function normalizeJsonString(value?: string | null) {
  const parsed = parseJsonObject(value);
  return JSON.stringify(parsed);
}

function parseJsonObject(value?: string | null) {
  if (!value) {
    return {};
  }
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed as Record<string, unknown> : {};
  } catch {
    return {};
  }
}

function summarizeAvatarStyle(style: Record<string, unknown>) {
  const source = String(style.source || "").trim();
  const parts = [
    source ? `source:${source}` : "",
    String(style.faceShape || "").trim() ? `face:${String(style.faceShape).trim()}` : "",
    String(style.topType || "").trim() ? `top:${String(style.topType).trim()}` : "",
    String(style.accessoriesType || "").trim() ? `accessories:${String(style.accessoriesType).trim()}` : "",
    String(style.hatColor || "").trim() ? `hatColor:${String(style.hatColor).trim()}` : "",
    String(style.hairColor || "").trim() ? `hairColor:${String(style.hairColor).trim()}` : "",
    String(style.facialHairType || "").trim() ? `facialHair:${String(style.facialHairType).trim()}` : "",
    String(style.facialHairColor || "").trim() ? `facialHairColor:${String(style.facialHairColor).trim()}` : "",
    String(style.clotheType || "").trim() ? `clothes:${String(style.clotheType).trim()}` : "",
    String(style.clotheColor || "").trim() ? `clothesColor:${String(style.clotheColor).trim()}` : "",
    String(style.graphicType || "").trim() ? `graphic:${String(style.graphicType).trim()}` : "",
    String(style.eyeType || "").trim() ? `eyes:${String(style.eyeType).trim()}` : "",
    String(style.eyebrowType || "").trim() ? `eyebrow:${String(style.eyebrowType).trim()}` : "",
    String(style.mouthType || "").trim() ? `mouth:${String(style.mouthType).trim()}` : "",
    String(style.skinColor || "").trim() ? `skin:${String(style.skinColor).trim()}` : "",
    String(style.skinTone || style.skin || "").trim(),
    String(style.faceShape || style.face || "").trim(),
    String(style.hairStyle || style.hair || "").trim(),
    String(style.hairColor || style.hairColorName || "").trim(),
    String(style.clothesStyle || style.outfit || "").trim(),
    String(style.clothesColor || style.outfitColor || "").trim(),
    String(style.hatStyle || style.hat || "").trim(),
    String(style.headAccessoryStyle || style.headAccessory || "").trim(),
    String(style.accessoryStyle || style.accessory || "").trim()
  ].filter(Boolean);
  return parts.length > 0 ? parts.join(" · ") : "默认造型";
}

function summarizeCompanionKernel(kernel: Record<string, unknown>) {
  const parts = [
    String(kernel.tone || kernel.voiceTone || "").trim(),
    String(kernel.memoryStyle || kernel.focus || "").trim(),
    String(kernel.replyLength || kernel.length || "").trim(),
    String(kernel.address || kernel.addressing || "").trim(),
    String(kernel.boundary || kernel.boundaryNote || "").trim(),
    String(kernel.opening || kernel.openingLine || "").trim()
  ].filter(Boolean);
  return parts.length > 0 ? parts.join(" · ") : "默认内核";
}

function chatCompletionsUrl(baseUrl: string) {
  const clean = baseUrl.replace(/\/+$/, "");
  if (clean.endsWith("/chat/completions")) {
    return clean;
  }
  if (clean.endsWith("/v1")) {
    return `${clean}/chat/completions`;
  }
  return `${clean}/v1/chat/completions`;
}

function imageEditsUrl(baseUrl: string) {
  const clean = baseUrl.replace(/\/+$/, "");
  if (clean.endsWith("/images/edits")) {
    return clean;
  }
  if (clean.endsWith("/v1")) {
    return `${clean}/images/edits`;
  }
  return `${clean}/v1/images/edits`;
}

function imageGenerationsUrl(baseUrl: string) {
  const clean = baseUrl.replace(/\/+$/, "");
  if (clean.endsWith("/images/generations")) {
    return clean;
  }
  if (clean.endsWith("/v1")) {
    return `${clean}/images/generations`;
  }
  return `${clean}/v1/images/generations`;
}

function localCompanionReply(profile: AiCompanionRow, content: string) {
  const relation = profile.relation;
  const kernel = parseJsonObject(profile.kernel_json);
  const tone = String(kernel.tone || kernel.voiceTone || "").trim();
  const opening = String(kernel.opening || kernel.openingLine || "").trim();
  const lengthHint = String(kernel.replyLength || kernel.length || "").trim();
  const trimmed = content.length > 40 ? `${content.slice(0, 40)}...` : content;
  const prefix = opening || selectLocalReplyPrefix(relation, tone);
  const reply = `${prefix}你说「${trimmed}」，我会稳稳接住这句话。`;
  const maxLength = lengthHint === "详细" ? 120 : lengthHint === "简短" ? 70 : 90;
  return reply.slice(0, maxLength);
}

function selectLocalReplyPrefix(relation: string, tone: string) {
  if (tone.includes("稳")) return "我听见了，";
  if (tone.includes("柔")) return "我在这儿，";
  if (relation.includes("父") || relation.includes("爸")) return "我听见了，";
  if (relation.includes("伴侣") || relation.includes("爱人")) return "我在你身边，";
  if (relation.includes("朋友") || relation.includes("友")) return "我听到了，";
  if (relation.includes("宠物") || relation.includes("猫") || relation.includes("狗")) return "我像从前那样陪着你，";
  return "我在这里，";
}

function readBoolean(body: Record<string, unknown>, key: string, fallback: boolean) {
  const raw = body[key];
  if (raw === undefined || raw === null) {
    return fallback;
  }
  if (typeof raw === "boolean") {
    return raw;
  }
  throw new ApiError(400, `${key}_must_be_boolean`);
}

function legalConfig(env: Bindings) {
  return {
    appName: "安忆",
    operator: env.LEGAL_OPERATOR_NAME || "安忆",
    email: env.LEGAL_CONTACT_EMAIL || "support@example.com",
    phone: env.LEGAL_CONTACT_PHONE || "待填写",
    effectiveDate: env.LEGAL_EFFECTIVE_DATE || "2026-05-09"
  };
}

function legalPage(
  title: string,
  legal: ReturnType<typeof legalConfig>,
  body: string
) {
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>${escapeHtml(legal.appName)} - ${escapeHtml(title)}</title>
  <style>
    body{margin:0;background:#f6f7fb;color:#10223b;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;line-height:1.72}
    main{max-width:860px;margin:0 auto;padding:32px 18px 56px}
    header{padding:24px 0;border-bottom:1px solid #e1e7f0;margin-bottom:22px}
    h1{font-size:30px;margin:0 0 8px;font-weight:900}
    h2{font-size:18px;margin:24px 0 8px}
    p,li{font-size:15px}
    a{color:#2f7d62;font-weight:700}
    .meta{color:#667085;font-size:13px}
    form{display:grid;gap:12px;max-width:520px}
    label{display:grid;gap:6px;font-weight:700}
    input,textarea{border:1px solid #d7deea;border-radius:8px;padding:10px 12px;font:inherit;background:white}
    textarea{min-height:110px}
    button{border:0;border-radius:8px;padding:12px 16px;background:#2f7d62;color:white;font-weight:900;font:inherit;cursor:pointer}
    footer{margin-top:32px;padding-top:18px;border-top:1px solid #e1e7f0;color:#667085;font-size:13px}
  </style>
</head>
<body>
  <main>
    <header>
      <h1>${escapeHtml(title)}</h1>
      <div class="meta">应用：${escapeHtml(legal.appName)} · 运营主体：${escapeHtml(legal.operator)} · 生效日期：${escapeHtml(legal.effectiveDate)}</div>
    </header>
    ${body}
    <footer>
      <p>联系方式：${escapeHtml(legal.email)} · ${escapeHtml(legal.phone)}</p>
      <p><a href="/legal/privacy">隐私政策</a> · <a href="/legal/terms">用户协议</a> · <a href="/legal/ai-disclaimer">AI 免责声明</a> · <a href="/legal/account-deletion">账号注销</a></p>
    </footer>
  </main>
</body>
</html>`;
}

function adminPage() {
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>安忆管理后台</title>
  <style>
    *{box-sizing:border-box}
    body{margin:0;background:#f5f7fb;color:#10223b;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}
    header{position:sticky;top:0;z-index:3;background:rgba(255,255,255,.92);backdrop-filter:blur(12px);border-bottom:1px solid #e1e7f0}
    .wrap{max-width:1180px;margin:0 auto;padding:18px}
    .top{display:flex;gap:14px;align-items:center;justify-content:space-between}
    h1{font-size:24px;margin:0;font-weight:900}
    h2{font-size:18px;margin:0 0 12px;font-weight:900}
    .muted{color:#667085;font-size:13px}
    .tabs{display:flex;gap:8px;overflow:auto;padding-top:14px}
    button,.tab{border:0;border-radius:8px;padding:10px 14px;background:#10223b;color:white;font-weight:800;cursor:pointer}
    button.secondary,.tab{background:white;color:#10223b;border:1px solid #d8e0ec}
    button.danger{background:#b42318}
    button.good{background:#2f7d62}
    .tab.active{background:#2f7d62;color:white;border-color:#2f7d62}
    main{max-width:1180px;margin:0 auto;padding:18px}
    .grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px}
    .card{background:white;border:1px solid #e1e7f0;border-radius:8px;box-shadow:0 8px 24px rgba(16,34,59,.06);padding:14px}
    .metric{font-size:28px;font-weight:900}
    .list{display:grid;gap:12px}
    input,select,textarea{width:100%;border:1px solid #d8e0ec;border-radius:8px;padding:10px;font:inherit;background:white}
    textarea{min-height:76px}
    .row{display:flex;gap:8px;align-items:center;flex-wrap:wrap}
    .split{display:grid;grid-template-columns:1fr 1fr;gap:12px}
    .pill{display:inline-flex;border-radius:999px;padding:4px 8px;background:#e9f5ef;color:#2f7d62;font-size:12px;font-weight:800}
    pre{white-space:pre-wrap;background:#f8fafc;border:1px solid #e1e7f0;border-radius:8px;padding:10px;max-height:220px;overflow:auto}
    .hidden{display:none!important}
    @media(max-width:820px){.grid,.split{grid-template-columns:1fr}.top{align-items:flex-start;flex-direction:column}}
  </style>
</head>
<body>
  <header>
    <div class="wrap">
      <div class="top">
        <div>
          <h1>安忆管理后台</h1>
          <div class="muted">上传审核、审计、崩溃和注销请求统一管理</div>
        </div>
        <div class="row">
          <span id="adminName" class="muted"></span>
          <button class="secondary" onclick="logout()">退出</button>
        </div>
      </div>
      <div id="tabs" class="tabs hidden">
        <button class="tab active" data-tab="dashboard" onclick="showTab('dashboard')">概览</button>
        <button class="tab" data-tab="uploads" onclick="showTab('uploads')">上传审核</button>
        <button class="tab" data-tab="community" onclick="showTab('community')">社区审核</button>
        <button class="tab" data-tab="reports" onclick="showTab('reports')">举报处理</button>
        <button class="tab" data-tab="users" onclick="showTab('users')">用户处置</button>
        <button class="tab" data-tab="deletions" onclick="showTab('deletions')">注销申请</button>
        <button class="tab" data-tab="crashes" onclick="showTab('crashes')">崩溃日志</button>
        <button class="tab" data-tab="audit" onclick="showTab('audit')">审计日志</button>
        <button class="tab" data-tab="assetDeletes" onclick="showTab('assetDeletes')">文件删除</button>
      </div>
    </div>
  </header>
  <main>
    <section id="loginView" class="card">
      <h2>管理员登录</h2>
      <div class="split">
        <input id="loginUsername" placeholder="管理员账号，例如 admin" />
        <input id="loginPassword" placeholder="密码" type="password" />
      </div>
      <div class="row" style="margin-top:12px">
        <button onclick="login()">登录后台</button>
        <span id="loginMessage" class="muted"></span>
      </div>
    </section>
    <section id="appView" class="hidden">
      <section id="dashboard" class="tabPanel">
        <div class="grid">
          <div class="card"><div class="muted">待审核上传</div><div id="mUploads" class="metric">0</div></div>
          <div class="card"><div class="muted">待审社区内容</div><div id="mCommunity" class="metric">0</div></div>
          <div class="card"><div class="muted">待处理举报</div><div id="mReports" class="metric">0</div></div>
          <div class="card"><div class="muted">崩溃日志</div><div id="mCrashes" class="metric">0</div></div>
          <div class="card"><div class="muted">注销申请</div><div id="mDeletes" class="metric">0</div></div>
        </div>
      </section>
      <section id="uploads" class="tabPanel hidden"><div class="card"><h2>上传内容审核</h2><div class="row"><select id="uploadStatus" onchange="loadUploads()"><option value="pending">pending</option><option value="approved">approved</option><option value="rejected">rejected</option><option value="quarantined">quarantined</option></select></div><div id="uploadsList" class="list" style="margin-top:12px"></div></div></section>
      <section id="community" class="tabPanel hidden"><div class="card"><h2>社区内容审核</h2><div class="row"><select id="communityStatus" onchange="loadCommunity()"><option value="pending">pending</option><option value="approved">approved</option><option value="rejected">rejected</option><option value="blocked">blocked</option><option value="all">all</option></select></div><div id="communityList" class="list" style="margin-top:12px"></div></div></section>
      <section id="reports" class="tabPanel hidden"><div class="card"><h2>社区举报处理</h2><div id="reportsList" class="list"></div></div></section>
      <section id="users" class="tabPanel hidden"><div class="card"><h2>用户处置</h2><div id="moderatedUsersList" class="list"></div></div></section>
      <section id="deletions" class="tabPanel hidden"><div class="card"><h2>账号注销申请</h2><div id="deletionsList" class="list"></div></div></section>
      <section id="crashes" class="tabPanel hidden"><div class="card"><h2>崩溃日志</h2><div id="crashesList" class="list"></div></div></section>
      <section id="audit" class="tabPanel hidden"><div class="card"><h2>审计日志</h2><div id="auditList" class="list"></div></div></section>
      <section id="assetDeletes" class="tabPanel hidden"><div class="card"><div class="row" style="justify-content:space-between"><h2>文件删除队列</h2><button class="danger" onclick="processAssetDeletes()">处理待删除</button></div><div id="assetDeletesList" class="list"></div></div></section>
    </section>
  </main>
<script>
var token = localStorage.getItem('anyi_admin_token') || '';
var currentUser = JSON.parse(localStorage.getItem('anyi_admin_user') || 'null');
var cache = { uploads: [], community: {posts:[],comments:[]}, reports: [], users: [], crashes: [], deletions: [], audit: [], assetDeletes: [] };

function esc(value){ return String(value == null ? '' : value).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];}); }
function authHeaders(extra){ var h = Object.assign({'Authorization':'Bearer ' + token}, extra || {}); return h; }
async function api(path, options){
  options = options || {};
  options.headers = Object.assign({}, options.headers || {}, options.body instanceof FormData ? authHeaders() : authHeaders({'Content-Type':'application/json'}));
  var res = await fetch(path, options);
  var text = await res.text();
  var data = text ? JSON.parse(text) : {};
  if(!res.ok){ throw new Error(data.error || text || 'request_failed'); }
  return data;
}
function setMessage(id,msg){ document.getElementById(id).textContent = msg || ''; }

async function login(){
  setMessage('loginMessage','登录中...');
  try{
    var data = await fetch('/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:loginUsername.value.trim(),password:loginPassword.value})}).then(async function(r){var d=await r.json(); if(!r.ok) throw new Error(d.error); return d;});
    if(data.user.role !== 'admin') throw new Error('admin_required');
    token = data.token; currentUser = data.user;
    localStorage.setItem('anyi_admin_token', token);
    localStorage.setItem('anyi_admin_user', JSON.stringify(currentUser));
    boot();
  }catch(e){ setMessage('loginMessage','登录失败：' + e.message); }
}
function logout(){ localStorage.removeItem('anyi_admin_token'); localStorage.removeItem('anyi_admin_user'); location.reload(); }
function boot(){
  if(token && currentUser && currentUser.role === 'admin'){
    loginView.classList.add('hidden'); appView.classList.remove('hidden'); tabs.classList.remove('hidden'); adminName.textContent = currentUser.username + ' · 管理员';
    refreshAll();
  }else{ loginView.classList.remove('hidden'); appView.classList.add('hidden'); tabs.classList.add('hidden'); }
}
function showTab(name){
  document.querySelectorAll('.tab').forEach(function(x){x.classList.toggle('active', x.dataset.tab === name);});
  document.querySelectorAll('.tabPanel').forEach(function(x){x.classList.toggle('hidden', x.id !== name);});
  if(name === 'uploads') loadUploads();
  if(name === 'community') loadCommunity();
  if(name === 'reports') loadReports();
  if(name === 'users') loadModeratedUsers();
  if(name === 'deletions') loadDeletions();
  if(name === 'crashes') loadCrashes();
  if(name === 'audit') loadAudit();
  if(name === 'assetDeletes') loadAssetDeletes();
}
async function refreshAll(){ await Promise.all([loadUploads(), loadCommunity(), loadReports(), loadModeratedUsers(), loadCrashes(), loadDeletions(), loadAudit(), loadAssetDeletes()]).catch(function(e){console.warn(e);}); renderMetrics(); }
function renderMetrics(){ mUploads.textContent=cache.uploads.length; mCommunity.textContent=(cache.community.posts||[]).length+(cache.community.comments||[]).length; mReports.textContent=cache.reports.length; mCrashes.textContent=cache.crashes.length; mDeletes.textContent=cache.deletions.length; }

async function loadUploads(){ var d=await api('/admin/upload-reviews?status='+encodeURIComponent(uploadStatus.value)); cache.uploads=d.reviews||[]; renderUploads(); renderMetrics(); }
function renderUploads(){ uploadsList.innerHTML = cache.uploads.map(function(r){return '<div class="card"><div><b>'+esc(r.mime_type)+'</b> <span class="pill">'+esc(r.status)+'</span></div><div class="muted">'+esc(r.asset_key)+' · '+esc(r.size_bytes)+' bytes</div><div class="row"><button class="good" onclick="reviewUpload(\\''+esc(r.id)+'\\',\\'approved\\')">通过</button><button class="danger" onclick="reviewUpload(\\''+esc(r.id)+'\\',\\'rejected\\')">拒绝</button><button class="secondary" onclick="reviewUpload(\\''+esc(r.id)+'\\',\\'quarantined\\')">隔离</button></div></div>';}).join('') || '<div class="muted">暂无记录</div>'; }
async function reviewUpload(id,status){ var reason = status === 'approved' ? '' : prompt('原因', status) || status; await api('/admin/upload-reviews/'+id,{method:'PATCH',body:JSON.stringify({status:status,reason:reason})}); await loadUploads(); }

async function loadCommunity(){ var d=await api('/admin/community/moderation?status='+encodeURIComponent(communityStatus.value)); cache.community=d||{posts:[],comments:[]}; renderCommunity(); renderMetrics(); }
function renderCommunity(){ var html=(cache.community.posts||[]).map(function(r){return '<div class="card"><b>动态</b> <span class="pill">'+esc(r.moderationStatus||r.status)+'</span><div class="muted">'+esc(r.authorName||r.authorUsername||'')+' · '+esc(r.createdAt||'')+'</div><p>'+esc(r.content||'')+'</p><div class="row"><button class="good" onclick="moderatePost(\\''+esc(r.id)+'\\',\\'approved\\')">通过</button><button class="danger" onclick="moderatePost(\\''+esc(r.id)+'\\',\\'rejected\\')">拒绝</button><button class="secondary" onclick="moderatePost(\\''+esc(r.id)+'\\',\\'blocked\\')">屏蔽</button></div></div>';}).join(''); html+=(cache.community.comments||[]).map(function(r){return '<div class="card"><b>评论</b> <span class="pill">'+esc(r.moderationStatus||r.status)+'</span><div class="muted">'+esc(r.authorName||r.authorUsername||'')+' · '+esc(r.createdAt||'')+'</div><p>'+esc(r.content||'')+'</p><div class="row"><button class="good" onclick="moderateComment(\\''+esc(r.id)+'\\',\\'approved\\')">通过</button><button class="danger" onclick="moderateComment(\\''+esc(r.id)+'\\',\\'rejected\\')">拒绝</button><button class="secondary" onclick="moderateComment(\\''+esc(r.id)+'\\',\\'blocked\\')">屏蔽</button></div></div>';}).join(''); communityList.innerHTML=html||'<div class="muted">暂无社区内容</div>'; }
async function moderatePost(id,status){ var reason=status==='approved'?'':prompt('原因',status)||status; await api('/admin/community/posts/'+id,{method:'PATCH',body:JSON.stringify({status:status,reason:reason})}); await loadCommunity(); }
async function moderateComment(id,status){ var reason=status==='approved'?'':prompt('原因',status)||status; await api('/admin/community/comments/'+id,{method:'PATCH',body:JSON.stringify({status:status,reason:reason})}); await loadCommunity(); }

async function loadReports(){ var d=await api('/admin/community/reports?status=pending'); cache.reports=d.reports||[]; reportsList.innerHTML=cache.reports.map(function(r){return '<div class="card"><b>'+esc(r.target_type)+'</b> <span class="pill">'+esc(r.status)+'</span><div class="muted">举报人：'+esc(r.reporter_display_name||r.reporter_username||'')+' · '+esc(r.created_at)+'</div><p>'+esc(r.reason)+'</p><div class="row"><button class="good" onclick="reviewReport(\\''+esc(r.id)+'\\',\\'approve\\')">保留</button><button class="danger" onclick="reviewReport(\\''+esc(r.id)+'\\',\\'remove\\')">移除</button><button class="secondary" onclick="reviewReport(\\''+esc(r.id)+'\\',\\'dismiss\\')">驳回举报</button><button class="danger" onclick="reviewReport(\\''+esc(r.id)+'\\',\\'block_user\\')">屏蔽用户</button></div></div>';}).join('')||'<div class="muted">暂无待处理举报</div>'; renderMetrics(); }
async function reviewReport(id,action){ var reason=prompt('处理备注',action)||action; await api('/admin/community/reports/'+id,{method:'PATCH',body:JSON.stringify({action:action,reason:reason})}); await loadReports(); await loadCommunity(); }

async function loadModeratedUsers(){ var d=await api('/admin/users/moderation'); cache.users=d.users||[]; moderatedUsersList.innerHTML=cache.users.map(function(r){return '<div class="card"><b>'+esc(r.display_name||r.username)+'</b> <span class="pill">'+esc(r.status)+'</span><div class="muted">'+esc(r.username)+' · '+esc(r.updated_at)+'</div><p>'+esc(r.reason||'')+'</p><button class="good" onclick="unblockUser(\\''+esc(r.user_id)+'\\')">解除处置</button></div>';}).join('')||'<div class="muted">暂无被处置用户</div>'; }
async function unblockUser(id){ await api('/admin/users/'+id+'/moderation',{method:'PATCH',body:JSON.stringify({status:'active'})}); await loadModeratedUsers(); }

async function loadDeletions(){ var d=await api('/admin/account-deletion-requests'); cache.deletions=d.requests||[]; renderDeletions(); renderMetrics(); }
function renderDeletions(){ deletionsList.innerHTML = cache.deletions.map(function(r){return '<div class="card"><b>'+esc(r.username)+'</b> <span class="pill">'+esc(r.status)+'</span><div class="muted">'+esc(r.contact||'')+' · '+esc(r.created_at)+'</div><p>'+esc(r.reason||'')+'</p><select onchange="setDeletionStatus(\\''+esc(r.id)+'\\',this.value)"><option>更新状态</option><option value="processing">processing</option><option value="completed">completed</option><option value="rejected">rejected</option></select></div>';}).join('') || '<div class="muted">暂无申请</div>'; }
async function setDeletionStatus(id,status){ if(status==='更新状态')return; await api('/admin/account-deletion-requests/'+id,{method:'PATCH',body:JSON.stringify({status:status})}); await loadDeletions(); }

async function loadCrashes(){ var d=await api('/admin/crash-reports'); cache.crashes=d.reports||[]; renderCrashes(); renderMetrics(); }
function renderCrashes(){ crashesList.innerHTML = cache.crashes.map(function(r){return '<div class="card"><b>'+esc(r.error_type)+'</b><div class="muted">'+esc(r.platform)+' · '+esc(r.app_version||'')+' · '+esc(r.created_at)+'</div><p>'+esc(r.message||'')+'</p><pre>'+esc((r.stack_trace||'').slice(0,4000))+'</pre></div>';}).join('') || '<div class="muted">暂无崩溃日志</div>'; }
async function loadAudit(){ var d=await api('/admin/audit-logs'); cache.audit=d.logs||[]; auditList.innerHTML = cache.audit.map(function(r){return '<div class="card"><b>'+esc(r.action)+'</b><div class="muted">'+esc(r.actor_role||'')+' · '+esc(r.target_type)+' · '+esc(r.created_at)+'</div><pre>'+esc(r.metadata_json||'{}')+'</pre></div>';}).join('') || '<div class="muted">暂无审计日志</div>'; }
async function loadAssetDeletes(){ var d=await api('/admin/asset-delete-queue'); cache.assetDeletes=d.items||[]; assetDeletesList.innerHTML = cache.assetDeletes.map(function(r){return '<div class="card"><b>'+esc(r.status)+'</b><div class="muted">'+esc(r.asset_key)+' · '+esc(r.reason)+' · '+esc(r.created_at)+'</div></div>';}).join('') || '<div class="muted">暂无删除任务</div>'; }
async function processAssetDeletes(){ var d=await api('/admin/asset-delete-queue/process',{method:'POST',body:JSON.stringify({})}); alert('处理 '+d.attempted+' 条，删除 '+d.deleted+' 条'); await loadAssetDeletes(); }
boot();
</script>
</body>
</html>`;
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function parseNumberArray(raw: string) {
  try {
    const value = JSON.parse(raw);
    return Array.isArray(value) ? value.filter((item): item is number => typeof item === "number") : [];
  } catch {
    return [];
  }
}

function parseStringArray(raw: string) {
  try {
    const value = JSON.parse(raw);
    return Array.isArray(value)
      ? value.filter((item): item is string => typeof item === "string")
      : [];
  } catch {
    return [];
  }
}

function isMemorialFruitType(value: string): value is MemorialFruitType {
  return (memorialFruitTypes as readonly string[]).includes(value);
}

function isMemorialFlowerType(value: string): value is MemorialFlowerType {
  return (memorialFlowerTypes as readonly string[]).includes(value);
}

function parseMemorialFlowers(raw: string, now = Date.now()): MemorialFlowerOffering[] {
  try {
    const value = JSON.parse(raw || "[]");
    if (!Array.isArray(value)) {
      return [];
    }
    return value
      .map((item): MemorialFlowerOffering | null => {
        if (typeof item === "number" && Number.isFinite(item) && item > now) {
          return { type: "wreath", until: item };
        }
        if (!item || typeof item !== "object") {
          return null;
        }
        const record = item as { type?: unknown; until?: unknown };
        const type = typeof record.type === "string" && isMemorialFlowerType(record.type)
          ? record.type
          : "wreath";
        if (typeof record.until !== "number" || !Number.isFinite(record.until) || record.until <= now) {
          return null;
        }
        return { type, until: record.until };
      })
      .filter((item): item is MemorialFlowerOffering => item !== null)
      .sort((a, b) => a.until - b.until);
  } catch {
    return [];
  }
}

function parseMemorialFruits(raw?: string | null, now = Date.now()): MemorialFruitOffering[] {
  try {
    const value = JSON.parse(raw || "[]");
    if (!Array.isArray(value)) {
      return [];
    }
    return value
      .map((item): MemorialFruitOffering | null => {
        if (!item || typeof item !== "object") {
          return null;
        }
        const record = item as { type?: unknown; until?: unknown };
        if (typeof record.type !== "string" || !isMemorialFruitType(record.type)) {
          return null;
        }
        if (typeof record.until !== "number" || !Number.isFinite(record.until) || record.until <= now) {
          return null;
        }
        return { type: record.type, until: record.until };
      })
      .filter((item): item is MemorialFruitOffering => item !== null)
      .sort((a, b) => a.until - b.until);
  } catch {
    return [];
  }
}

export default app;
