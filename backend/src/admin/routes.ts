import { createHash } from "node:crypto";
import type { Context, Hono, MiddlewareHandler } from "hono";

import { ApiError } from "../errors.js";
import type {
  AppEnv,
  AuthUser,
  Bindings,
  CommunityCommentRow,
  CommunityPostRow,
  CommunityReportRow,
  CommunityVolunteerRow,
  Role
} from "../index.js";
import { adminPageHtml } from "./page.js";
import { adminScript } from "./ui-script.js";
import { adminStyles } from "./ui-styles.js";

// The admin console talks to these endpoints only. Everything that mutates
// data goes through the helpers injected from index.ts so the moderation
// side effects (asset visibility, delete queue, audit trail) stay identical to
// what the user-facing routes do.
export type AdminDeps = {
  requireAuth: MiddlewareHandler<AppEnv>;
  requireAdmin(c: Context<AppEnv>): AuthUser;
  parseJson(c: Context<AppEnv>): Promise<Record<string, unknown>>;
  readString(
    body: Record<string, unknown>,
    key: string,
    options?: { required?: boolean; max?: number }
  ): string;
  writeAudit(
    c: Context<AppEnv>,
    input: { action: string; targetType: string; targetId?: string; metadata?: Record<string, unknown> }
  ): Promise<void>;
  setAssetVisibility(
    c: Context<AppEnv>,
    assetKey: string,
    visibility: "private" | "public",
    ownerId?: string
  ): Promise<void>;
  clearRejectedAiCompanionAvatarReferences(c: Context<AppEnv>, assetKey: string, ownerId: string): Promise<void>;
  clearRejectedAiVoiceReferences(c: Context<AppEnv>, assetKey: string, ownerId: string): Promise<void>;
  queueAssetDelete(c: Context<AppEnv>, assetKey: string, ownerId: string | null, reason: string): Promise<void>;
  assetDeletionAllowed(c: Context<AppEnv>, assetKey: string, reason: string): Promise<boolean>;
  assetKeyFromUrl(value: string): string | null;
  parseStringArray(raw: string): string[];
  serializeCommunityPost(row: CommunityPostRow): Record<string, unknown>;
  serializeCommunityComment(row: CommunityCommentRow): Record<string, unknown>;
  serializeCommunityVolunteer(row: CommunityVolunteerRow): Record<string, unknown>;
  moderateCommunityTargetPost(
    c: Context<AppEnv>,
    admin: AuthUser,
    post: { id: string; user_id: string; image_urls: string; status: string },
    status: "approved" | "rejected" | "blocked",
    reason: string | null
  ): Promise<void>;
  moderateCommunityTargetComment(
    c: Context<AppEnv>,
    admin: AuthUser,
    comment: { id: string; user_id: string; status: string },
    status: "approved" | "rejected" | "blocked",
    reason: string | null
  ): Promise<void>;
  setUserModeration(
    c: Context<AppEnv>,
    admin: AuthUser,
    userId: string,
    status: "blocked" | "banned",
    reason: string | null,
    expiresAt: string | null
  ): Promise<void>;
  deleteUserAccount(
    c: Context<AppEnv>,
    target: { id: string; role: Role },
    audit: { action: string; actor: AuthUser; metadata?: Record<string, unknown> }
  ): Promise<void>;
  readEnvBoolean(value: string | undefined, fallback: boolean): boolean;
  speechEnabled(env: Bindings): boolean;
  aiVoiceAsrConfigured(env: Bindings): boolean;
  speechDailyCharLimit(env: Bindings): number;
  live2dEnabled(c: Context<AppEnv>): boolean;
};

type ReviewStatus = "approved" | "rejected" | "quarantined";

type UploadReviewRow = {
  id: string;
  asset_id: string;
  owner_id: string;
  asset_key: string;
  mime_type: string;
  size_bytes: number;
  status: string;
  reason: string | null;
  created_at: string;
  reviewed_at: string | null;
  reviewed_by: string | null;
  username?: string | null;
  display_name?: string | null;
  reviewer_username?: string | null;
};

type CountRow = { count: number | string };

// Hours added to UTC timestamps before bucketing activity by calendar day.
// The product and its administrators operate on Beijing time.
const activityUtcOffsetHours = 8;
const activityDays = 14;

const contentSecurityPolicy = [
  "default-src 'none'",
  "script-src 'self'",
  "style-src 'self'",
  "img-src 'self' blob: data:",
  "media-src 'self' blob:",
  "connect-src 'self'",
  "font-src 'self'",
  "base-uri 'none'",
  "form-action 'none'",
  "frame-ancestors 'none'"
].join("; ");

function etagFor(value: string) {
  return `"${createHash("sha256").update(value).digest("hex").slice(0, 32)}"`;
}

const staticAssets = {
  html: { body: adminPageHtml, type: "text/html; charset=utf-8", etag: etagFor(adminPageHtml) },
  script: { body: adminScript, type: "application/javascript; charset=utf-8", etag: etagFor(adminScript) },
  styles: { body: adminStyles, type: "text/css; charset=utf-8", etag: etagFor(adminStyles) }
};

function serveStatic(c: Context<AppEnv>, asset: { body: string; type: string; etag: string }) {
  const headers: Record<string, string> = {
    "Content-Type": asset.type,
    "Cache-Control": "no-cache",
    ETag: asset.etag
  };
  if (asset === staticAssets.html) {
    headers["Content-Security-Policy"] = contentSecurityPolicy;
  }
  if (c.req.header("If-None-Match") === asset.etag) {
    return new Response(null, { status: 304, headers });
  }
  return c.body(asset.body, 200, headers);
}

function toCount(row: CountRow | null | undefined) {
  return Number(row?.count || 0);
}

function placeholders(count: number) {
  return new Array(count).fill("?").join(", ");
}

function clampLimit(value: string | undefined, fallback: number, max: number) {
  const parsed = Number.parseInt(value || "", 10);
  if (!Number.isFinite(parsed) || parsed <= 0) return fallback;
  return Math.min(parsed, max);
}

function escapeLike(value: string) {
  return value.replace(/[!%_]/g, (match) => `!${match}`);
}

function dayKey(date: Date) {
  return date.toISOString().slice(0, 10);
}

// Buckets ISO timestamps into local calendar days from UTC hour buckets. The
// hour grouping works on both SQLite and MySQL with plain substr().
async function activitySeries(c: Context<AppEnv>, table: string, column: string, since: string, days: string[]) {
  const rows = await c.env.DB.prepare(
    `SELECT substr(${column}, 1, 13) AS bucket, COUNT(*) AS count
     FROM ${table}
     WHERE ${column} >= ?
     GROUP BY substr(${column}, 1, 13)`
  )
    .bind(since)
    .all<{ bucket: string; count: number | string }>();
  const totals = new Map(days.map((day) => [day, 0]));
  for (const row of rows.results) {
    const parsed = Date.parse(`${row.bucket}:00:00Z`);
    if (Number.isNaN(parsed)) continue;
    const local = new Date(parsed + activityUtcOffsetHours * 3_600_000);
    const key = dayKey(local);
    if (totals.has(key)) {
      totals.set(key, (totals.get(key) || 0) + Number(row.count || 0));
    }
  }
  return days.map((day) => totals.get(day) || 0);
}

function localDayList(count: number) {
  const now = new Date(Date.now() + activityUtcOffsetHours * 3_600_000);
  const days: string[] = [];
  for (let offset = count - 1; offset >= 0; offset -= 1) {
    const day = new Date(now.getTime() - offset * 86_400_000);
    days.push(dayKey(day));
  }
  return days;
}

export function registerAdminRoutes(app: Hono<AppEnv>, deps: AdminDeps) {
  const { requireAuth, requireAdmin, parseJson, readString, writeAudit } = deps;

  async function count(c: Context<AppEnv>, sql: string, params: unknown[] = []) {
    const statement = c.env.DB.prepare(sql);
    const row = params.length > 0
      ? await statement.bind(...params).first<CountRow>()
      : await statement.first<CountRow>();
    return toCount(row);
  }

  app.get("/admin", (c) => serveStatic(c, staticAssets.html));
  app.get("/admin/", (c) => serveStatic(c, staticAssets.html));
  app.get("/admin/app.js", (c) => serveStatic(c, staticAssets.script));
  app.get("/admin/app.css", (c) => serveStatic(c, staticAssets.styles));

  // Admin data is never cacheable: a shared browser must not be able to
  // replay a queue listing after the administrator has logged out.
  app.use("/admin/*", async (c, next) => {
    await next();
    if (c.res.headers.get("Content-Type")?.startsWith("application/json")) {
      c.res.headers.set("Cache-Control", "no-store");
    }
  });

  app.get("/admin/overview", requireAuth, async (c) => {
    requireAdmin(c);
    const now = new Date().toISOString();
    const days = localDayList(activityDays);
    const since = new Date(Date.now() - (activityDays + 1) * 86_400_000).toISOString();

    const [
      pendingUploads,
      pendingPosts,
      pendingComments,
      pendingReports,
      pendingApplications,
      pendingDeletionRequests,
      processingDeletionRequests,
      pendingAssetDeletes,
      failedAssetDeletes,
      moderatedUsers,
      totalUsers,
      totalMemorials,
      totalCompanions,
      totalPosts
    ] = await Promise.all([
      count(c, "SELECT COUNT(*) AS count FROM upload_reviews WHERE status = 'pending'"),
      count(c, "SELECT COUNT(*) AS count FROM community_posts WHERE status = 'pending'"),
      count(c, "SELECT COUNT(*) AS count FROM community_post_comments WHERE status = 'pending'"),
      count(c, "SELECT COUNT(*) AS count FROM community_reports WHERE status = 'pending'"),
      count(c, "SELECT COUNT(*) AS count FROM community_volunteer_applications WHERE status = 'pending'"),
      count(c, "SELECT COUNT(*) AS count FROM account_deletion_requests WHERE status = 'pending'"),
      count(c, "SELECT COUNT(*) AS count FROM account_deletion_requests WHERE status = 'processing'"),
      count(c, "SELECT COUNT(*) AS count FROM asset_delete_queue WHERE status = 'pending'"),
      count(c, "SELECT COUNT(*) AS count FROM asset_delete_queue WHERE status = 'failed'"),
      count(c, "SELECT COUNT(*) AS count FROM user_moderation WHERE expires_at IS NULL OR expires_at > ?", [now]),
      count(c, "SELECT COUNT(*) AS count FROM users WHERE deleted_at IS NULL"),
      count(c, "SELECT COUNT(*) AS count FROM memorials"),
      count(c, "SELECT COUNT(*) AS count FROM ai_companions"),
      count(c, "SELECT COUNT(*) AS count FROM community_posts WHERE status = 'approved'")
    ]);

    const live2dReady = await c.env.DB.hasColumn("live2d_jobs", "status");
    const [live2dQueued, live2dRunning, live2dFailed] = live2dReady
      ? await Promise.all([
        count(c, "SELECT COUNT(*) AS count FROM live2d_jobs WHERE status = 'queued'"),
        count(c, "SELECT COUNT(*) AS count FROM live2d_jobs WHERE status = 'running'"),
        count(c, "SELECT COUNT(*) AS count FROM live2d_jobs WHERE status = 'failed' AND updated_at >= ?", [since])
      ])
      : [0, 0, 0];

    const [users, posts, aiMessages, crashes] = await Promise.all([
      activitySeries(c, "users", "created_at", since, days),
      activitySeries(c, "community_posts", "created_at", since, days),
      activitySeries(c, "ai_chat_messages", "created_at", since, days),
      activitySeries(c, "crash_reports", "created_at", since, days)
    ]);

    const speechReady = await c.env.DB.hasColumn("ai_speech_usage", "characters");
    const today = days[days.length - 1];
    const speechToday = speechReady
      ? await c.env.DB.prepare(
        "SELECT COALESCE(SUM(characters), 0) AS characters, COUNT(*) AS users FROM ai_speech_usage WHERE day = ?"
      )
        .bind(new Date().toISOString().slice(0, 10))
        .first<{ characters: number | string; users: number | string }>()
      : null;

    const recentAudit = await c.env.DB.prepare(
      `SELECT l.id, l.action, l.target_type, l.target_id, l.actor_role, l.created_at, l.metadata_json,
              u.username AS actor_username, u.display_name AS actor_display_name
       FROM audit_logs l
       LEFT JOIN users u ON u.id = l.actor_id
       ORDER BY l.created_at DESC
       LIMIT 12`
    ).all<Record<string, unknown>>();

    return c.json({
      generatedAt: now,
      queues: {
        uploads: pendingUploads,
        community: pendingPosts + pendingComments,
        communityPosts: pendingPosts,
        communityComments: pendingComments,
        reports: pendingReports,
        volunteerApplications: pendingApplications,
        deletionRequests: pendingDeletionRequests,
        deletionRequestsProcessing: processingDeletionRequests,
        assetDeletes: pendingAssetDeletes,
        assetDeletesFailed: failedAssetDeletes,
        moderatedUsers,
        live2dActive: live2dQueued + live2dRunning,
        live2dQueued,
        live2dRunning,
        live2dFailedRecently: live2dFailed
      },
      totals: {
        users: totalUsers,
        memorials: totalMemorials,
        companions: totalCompanions,
        posts: totalPosts
      },
      activity: { days, today, users, posts, aiMessages, crashes },
      services: {
        database: c.env.DB.dialect,
        voiceRecognition: deps.readEnvBoolean(c.env.AI_VOICE_ENABLED, false) && deps.aiVoiceAsrConfigured(c.env),
        speech: deps.speechEnabled(c.env),
        live2d: deps.live2dEnabled(c),
        wechatLogin: Boolean(c.env.WECHAT_APP_ID?.trim() && c.env.WECHAT_APP_SECRET?.trim()),
        chatModel: c.env.AI_MODEL?.trim() || "gpt-5.6-luna",
        memoryModel: c.env.AI_MEMORY_MODEL?.trim() || "gpt-5.5",
        speechToday: {
          characters: Number(speechToday?.characters || 0),
          users: Number(speechToday?.users || 0),
          limitPerUser: deps.speechDailyCharLimit(c.env)
        }
      },
      recentAudit: recentAudit.results
    });
  });

  app.get("/admin/audit-logs", requireAuth, async (c) => {
    requireAdmin(c);
    const action = (c.req.query("action") || "").trim().slice(0, 120);
    const actor = (c.req.query("actor") || "").trim().toLowerCase().slice(0, 64);
    const targetId = (c.req.query("targetId") || "").trim().slice(0, 80);
    const limit = clampLimit(c.req.query("limit"), 200, 500);
    const clauses: string[] = [];
    const params: unknown[] = [];
    if (action) {
      clauses.push("l.action LIKE ? ESCAPE '!'");
      params.push(`${escapeLike(action)}%`);
    }
    if (actor) {
      clauses.push("(u.username = ? OR l.actor_id = ?)");
      params.push(actor, actor);
    }
    if (targetId) {
      clauses.push("(l.target_id = ? OR l.actor_id = ?)");
      params.push(targetId, targetId);
    }
    const sql = `SELECT l.*, u.username AS actor_username, u.display_name AS actor_display_name
      FROM audit_logs l
      LEFT JOIN users u ON u.id = l.actor_id
      ${clauses.length > 0 ? `WHERE ${clauses.join(" AND ")}` : ""}
      ORDER BY l.created_at DESC LIMIT ${limit}`;
    const statement = c.env.DB.prepare(sql);
    const rows = params.length > 0
      ? await statement.bind(...params).all<Record<string, unknown>>()
      : await statement.all<Record<string, unknown>>();
    return c.json({ logs: rows.results });
  });

  app.get("/admin/upload-reviews", requireAuth, async (c) => {
    requireAdmin(c);
    const status = c.req.query("status") || "pending";
    if (!["pending", "approved", "rejected", "quarantined"].includes(status)) {
      throw new ApiError(400, "invalid_review_status");
    }
    const limit = clampLimit(c.req.query("limit"), 200, 500);
    const rows = await c.env.DB.prepare(
      `SELECT ur.*, u.username, u.display_name, r.username AS reviewer_username
       FROM upload_reviews ur
       LEFT JOIN users u ON u.id = ur.owner_id
       LEFT JOIN users r ON r.id = ur.reviewed_by
       WHERE ur.status = ?
       ORDER BY ur.created_at DESC LIMIT ${limit}`
    )
      .bind(status)
      .all<UploadReviewRow>();
    const counts = await c.env.DB.prepare(
      "SELECT status, COUNT(*) AS count FROM upload_reviews GROUP BY status"
    ).all<{ status: string; count: number | string }>();
    return c.json({
      reviews: rows.results,
      counts: Object.fromEntries(counts.results.map((row) => [row.status, Number(row.count || 0)]))
    });
  });

  async function applyUploadReview(
    c: Context<AppEnv>,
    admin: AuthUser,
    reviewId: string,
    status: ReviewStatus,
    reason: string | null
  ) {
    const review = await c.env.DB.prepare("SELECT * FROM upload_reviews WHERE id = ?")
      .bind(reviewId)
      .first<{ id: string; asset_key: string; owner_id: string; status: string }>();
    if (!review) {
      throw new ApiError(404, "review_not_found");
    }
    if (review.status !== "pending") {
      if (review.status === status) {
        return { changed: false };
      }
      throw new ApiError(409, "review_status_conflict", {
        currentStatus: review.status,
        requestedStatus: status
      });
    }

    const update = await c.env.DB.prepare(
      `UPDATE upload_reviews
       SET status = ?, reason = ?, reviewed_at = ?, reviewed_by = ?
       WHERE id = ? AND status = 'pending'`
    )
      .bind(status, reason, new Date().toISOString(), admin.id, review.id)
      .run();
    if (Number(update.meta.changes || 0) !== 1) {
      const current = await c.env.DB.prepare("SELECT status FROM upload_reviews WHERE id = ?")
        .bind(review.id)
        .first<{ status: string }>();
      if (current?.status === status) {
        return { changed: false };
      }
      throw new ApiError(409, "review_status_conflict", {
        currentStatus: current?.status || "unknown",
        requestedStatus: status
      });
    }

    if (status === "rejected" || status === "quarantined") {
      await deps.setAssetVisibility(c, review.asset_key, "private", review.owner_id);
      await deps.clearRejectedAiCompanionAvatarReferences(c, review.asset_key, review.owner_id);
      await deps.clearRejectedAiVoiceReferences(c, review.asset_key, review.owner_id);
      await deps.queueAssetDelete(c, review.asset_key, review.owner_id, `upload_review_${status}`);
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
        await deps.setAssetVisibility(c, review.asset_key, "public", profileReference.id);
      }
    }

    await writeAudit(c, {
      action: "admin.upload.review",
      targetType: "upload_review",
      targetId: review.id,
      metadata: { status, reason }
    });
    return { changed: true };
  }

  function readReviewStatus(body: Record<string, unknown>): ReviewStatus {
    const status = readString(body, "status", { required: true, max: 20 });
    if (!["approved", "rejected", "quarantined"].includes(status)) {
      throw new ApiError(400, "invalid_review_status");
    }
    return status as ReviewStatus;
  }

  app.patch("/admin/upload-reviews/:id", requireAuth, async (c) => {
    const admin = requireAdmin(c);
    const body = await parseJson(c);
    const status = readReviewStatus(body);
    const reason = readString(body, "reason", { max: 300 }) || null;
    await applyUploadReview(c, admin, c.req.param("id"), status, reason);
    return c.json({ ok: true });
  });

  app.post("/admin/upload-reviews/batch", requireAuth, async (c) => {
    const admin = requireAdmin(c);
    const body = await parseJson(c);
    const status = readReviewStatus(body);
    const reason = readString(body, "reason", { max: 300 }) || null;
    const ids = Array.isArray(body.ids)
      ? body.ids.filter((value): value is string => typeof value === "string" && value.length > 0 && value.length <= 80)
      : [];
    if (ids.length === 0 || ids.length > 100) {
      throw new ApiError(400, "invalid_review_batch");
    }
    const results: Array<{ id: string; ok: boolean; error?: string }> = [];
    for (const id of new Set(ids)) {
      try {
        await applyUploadReview(c, admin, id, status, reason);
        results.push({ id, ok: true });
      } catch (error) {
        if (error instanceof ApiError) {
          results.push({ id, ok: false, error: error.code });
        } else {
          throw error;
        }
      }
    }
    return c.json({ results, applied: results.filter((item) => item.ok).length });
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
    const counts = await c.env.DB.prepare(
      `SELECT status, SUM(count) AS count FROM (
         SELECT status, COUNT(*) AS count FROM community_posts GROUP BY status
         UNION ALL
         SELECT status, COUNT(*) AS count FROM community_post_comments GROUP BY status
       ) AS combined GROUP BY status`
    ).all<{ status: string; count: number | string }>();

    return c.json({
      posts: posts.results.map(deps.serializeCommunityPost),
      comments: comments.results.map(deps.serializeCommunityComment),
      counts: Object.fromEntries(counts.results.map((row) => [row.status, Number(row.count || 0)]))
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
      for (const url of deps.parseStringArray(post.image_urls || "[]")) {
        const key = deps.assetKeyFromUrl(url);
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

    const imageUrls = deps.parseStringArray(post.image_urls || "[]");
    if (status === "approved") {
      await setCommunityAssetVisibility(c, imageUrls, "public", post.user_id);
    } else if (status === "rejected" || status === "blocked") {
      await setCommunityAssetVisibility(c, imageUrls, "private", post.user_id);
      if (post.status !== status) {
        for (const url of imageUrls) {
          const key = deps.assetKeyFromUrl(url);
          if (key) await deps.queueAssetDelete(c, key, post.user_id, `community_post_${status}`);
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

  async function setCommunityAssetVisibility(
    c: Context<AppEnv>,
    imageUrls: string[],
    visibility: "private" | "public",
    ownerId: string
  ) {
    for (const url of imageUrls) {
      const key = deps.assetKeyFromUrl(url);
      if (key) {
        await deps.setAssetVisibility(c, key, visibility, ownerId);
      }
    }
  }

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

    // Attach the reported content so the reviewer never has to leave the queue.
    const postIds = [...new Set(rows.results.filter((row) => row.target_type === "post").map((row) => row.target_id))];
    const commentIds = [...new Set(rows.results.filter((row) => row.target_type === "comment").map((row) => row.target_id))];
    type TargetRow = {
      id: string;
      user_id: string;
      content: string;
      image_urls?: string | null;
      status: string;
      username: string;
      display_name: string;
    };
    const posts = postIds.length > 0
      ? await c.env.DB.prepare(
        `SELECT p.id, p.user_id, p.content, p.image_urls, p.status, u.username, u.display_name
         FROM community_posts p JOIN users u ON u.id = p.user_id
         WHERE p.id IN (${placeholders(postIds.length)})`
      ).bind(...postIds).all<TargetRow>()
      : { results: [] as TargetRow[] };
    const comments = commentIds.length > 0
      ? await c.env.DB.prepare(
        `SELECT cc.id, cc.user_id, cc.content, cc.status, u.username, u.display_name
         FROM community_post_comments cc JOIN users u ON u.id = cc.user_id
         WHERE cc.id IN (${placeholders(commentIds.length)})`
      ).bind(...commentIds).all<TargetRow>()
      : { results: [] as TargetRow[] };
    const targets = new Map<string, TargetRow>();
    for (const row of posts.results) targets.set(`post:${row.id}`, row);
    for (const row of comments.results) targets.set(`comment:${row.id}`, row);

    const reports = rows.results.map((row) => {
      const target = targets.get(`${row.target_type}:${row.target_id}`);
      return {
        ...row,
        target: target
          ? {
            exists: true,
            authorId: target.user_id,
            authorName: target.display_name || target.username,
            authorUsername: target.username,
            content: target.content,
            imageUrls: deps.parseStringArray(target.image_urls || "[]"),
            status: target.status
          }
          : { exists: false }
      };
    });
    const counts = await c.env.DB.prepare(
      "SELECT status, COUNT(*) AS count FROM community_reports GROUP BY status"
    ).all<{ status: string; count: number | string }>();
    return c.json({
      reports,
      counts: Object.fromEntries(counts.results.map((row) => [row.status, Number(row.count || 0)]))
    });
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
        await deps.moderateCommunityTargetPost(c, admin, target, action === "approve" ? "approved" : "rejected", reason);
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
        await deps.moderateCommunityTargetComment(c, admin, target, action === "approve" ? "approved" : "rejected", reason);
      }
    }

    if ((action === "block_user" || action === "ban_user") && targetUserId) {
      await deps.setUserModeration(c, admin, targetUserId, action === "ban_user" ? "banned" : "blocked", reason, null);
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

  app.get("/admin/users", requireAuth, async (c) => {
    requireAdmin(c);
    const query = (c.req.query("q") || "").trim().toLowerCase().slice(0, 64);
    const filter = c.req.query("filter") || "all";
    if (!["all", "moderated", "admins"].includes(filter)) {
      throw new ApiError(400, "invalid_user_filter");
    }
    const limit = clampLimit(c.req.query("limit"), 50, 200);
    const clauses = ["u.deleted_at IS NULL"];
    const params: unknown[] = [];
    if (query) {
      clauses.push("(u.username LIKE ? ESCAPE '!' OR LOWER(u.display_name) LIKE ? ESCAPE '!' OR u.id = ?)");
      const pattern = `%${escapeLike(query)}%`;
      params.push(pattern, pattern, query);
    }
    if (filter === "moderated") {
      clauses.push("m.user_id IS NOT NULL");
    } else if (filter === "admins") {
      clauses.push("u.role = 'admin'");
    }
    const rows = await c.env.DB.prepare(
      `SELECT u.id, u.username, u.display_name, u.role, u.avatar_url, u.gender, u.created_at,
              CASE WHEN u.wechat_openid IS NULL THEN 0 ELSE 1 END AS wechat_bound,
              m.status AS moderation_status, m.reason AS moderation_reason,
              m.expires_at AS moderation_expires_at, m.updated_at AS moderation_updated_at
       FROM users u
       LEFT JOIN user_moderation m ON m.user_id = u.id
       WHERE ${clauses.join(" AND ")}
       ORDER BY ${filter === "moderated" ? "m.updated_at" : "u.created_at"} DESC
       LIMIT ${limit}`
    )
      .bind(...params)
      .all<Record<string, unknown>>();
    return c.json({ users: rows.results });
  });

  app.get("/admin/users/:id", requireAuth, async (c) => {
    requireAdmin(c);
    const id = c.req.param("id");
    const user = await c.env.DB.prepare(
      `SELECT u.id, u.username, u.display_name, u.role, u.avatar_url, u.gender, u.created_at,
              u.terms_accepted_at, u.privacy_accepted_at, u.wechat_nickname,
              CASE WHEN u.wechat_openid IS NULL THEN 0 ELSE 1 END AS wechat_bound,
              m.status AS moderation_status, m.reason AS moderation_reason,
              m.expires_at AS moderation_expires_at, m.updated_at AS moderation_updated_at,
              m.updated_by AS moderation_updated_by
       FROM users u
       LEFT JOIN user_moderation m ON m.user_id = u.id
       WHERE u.id = ? AND u.deleted_at IS NULL`
    )
      .bind(id)
      .first<Record<string, unknown>>();
    if (!user) throw new ApiError(404, "user_not_found");

    const [memorials, posts, comments, companions, assets, pendingUploads, crashes, reportsFiled, reportsAgainst] = await Promise.all([
      count(c, "SELECT COUNT(*) AS count FROM memorials WHERE owner_id = ?", [id]),
      count(c, "SELECT COUNT(*) AS count FROM community_posts WHERE user_id = ?", [id]),
      count(c, "SELECT COUNT(*) AS count FROM community_post_comments WHERE user_id = ?", [id]),
      count(c, "SELECT COUNT(*) AS count FROM ai_companions WHERE user_id = ?", [id]),
      count(c, "SELECT COUNT(*) AS count FROM assets WHERE owner_id = ?", [id]),
      count(c, "SELECT COUNT(*) AS count FROM upload_reviews WHERE owner_id = ? AND status = 'pending'", [id]),
      count(c, "SELECT COUNT(*) AS count FROM crash_reports WHERE user_id = ?", [id]),
      count(c, "SELECT COUNT(*) AS count FROM community_reports WHERE reporter_id = ?", [id]),
      count(
        c,
        `SELECT COUNT(*) AS count FROM community_reports r
         WHERE (r.target_type = 'post' AND r.target_id IN (SELECT id FROM community_posts WHERE user_id = ?))
            OR (r.target_type = 'comment' AND r.target_id IN (SELECT id FROM community_post_comments WHERE user_id = ?))`,
        [id, id]
      )
    ]);
    const companionRows = await c.env.DB.prepare(
      "SELECT id, display_name, relation, created_at FROM ai_companions WHERE user_id = ? ORDER BY created_at DESC LIMIT 20"
    ).bind(id).all<Record<string, unknown>>();
    const memorialRows = await c.env.DB.prepare(
      "SELECT id, name, created_at FROM memorials WHERE owner_id = ? ORDER BY created_at DESC LIMIT 20"
    ).bind(id).all<Record<string, unknown>>();
    const audit = await c.env.DB.prepare(
      `SELECT l.id, l.action, l.target_type, l.target_id, l.actor_id, l.actor_role, l.created_at, l.metadata_json,
              a.username AS actor_username
       FROM audit_logs l
       LEFT JOIN users a ON a.id = l.actor_id
       WHERE l.actor_id = ? OR l.target_id = ?
       ORDER BY l.created_at DESC LIMIT 30`
    ).bind(id, id).all<Record<string, unknown>>();

    return c.json({
      user,
      counts: { memorials, posts, comments, companions, assets, pendingUploads, crashes, reportsFiled, reportsAgainst },
      companions: companionRows.results,
      memorials: memorialRows.results,
      audit: audit.results
    });
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
      await deps.setUserModeration(c, admin, target.id, status as "blocked" | "banned", reason, expiresAt);
    }
    await writeAudit(c, {
      action: "admin.user.moderation",
      targetType: "user",
      targetId: target.id,
      metadata: { status, reason, expiresAt }
    });
    return c.json({ ok: true, status });
  });

  app.delete("/admin/users/:id", requireAuth, async (c) => {
    const admin = requireAdmin(c);
    const body = await parseJson(c);
    const confirmUsername = readString(body, "confirmUsername", { required: true, max: 64 }).toLowerCase();
    const reason = readString(body, "reason", { max: 300 }) || null;
    const deletionRequestId = readString(body, "deletionRequestId", { max: 80 }) || null;
    const target = await c.env.DB.prepare(
      "SELECT id, username, role FROM users WHERE id = ? AND deleted_at IS NULL"
    )
      .bind(c.req.param("id"))
      .first<{ id: string; username: string; role: Role }>();
    if (!target) throw new ApiError(404, "user_not_found");
    if (target.role === "admin" || target.id === admin.id) {
      throw new ApiError(403, "admin_account_deletion_forbidden");
    }
    if (target.username !== confirmUsername) {
      throw new ApiError(400, "confirm_username_mismatch");
    }

    await deps.deleteUserAccount(c, target, {
      action: "admin.user.account.delete",
      actor: admin,
      metadata: { hardDelete: true, reason, deletionRequestId, username: target.username }
    });
    if (deletionRequestId) {
      await c.env.DB.prepare(
        "UPDATE account_deletion_requests SET status = 'completed', updated_at = ? WHERE id = ? AND status <> 'completed'"
      )
        .bind(new Date().toISOString(), deletionRequestId)
        .run();
    }
    return c.json({ ok: true });
  });

  app.get("/admin/crash-reports", requireAuth, async (c) => {
    requireAdmin(c);
    const limit = clampLimit(c.req.query("limit"), 200, 500);
    const rows = await c.env.DB.prepare(
      `SELECT r.*, u.username, u.display_name
       FROM crash_reports r
       LEFT JOIN users u ON u.id = r.user_id
       ORDER BY r.created_at DESC LIMIT ${limit}`
    ).all<Record<string, unknown>>();
    return c.json({ reports: rows.results });
  });

  app.get("/admin/asset-delete-queue", requireAuth, async (c) => {
    requireAdmin(c);
    const status = c.req.query("status") || "all";
    if (!["pending", "deleted", "failed", "all"].includes(status)) {
      throw new ApiError(400, "invalid_asset_delete_status");
    }
    const sql = `SELECT * FROM asset_delete_queue
      ${status === "all" ? "" : "WHERE status = ?"}
      ORDER BY created_at DESC LIMIT 200`;
    const rows = status === "all"
      ? await c.env.DB.prepare(sql).all<Record<string, unknown>>()
      : await c.env.DB.prepare(sql).bind(status).all<Record<string, unknown>>();
    const counts = await c.env.DB.prepare(
      "SELECT status, COUNT(*) AS count FROM asset_delete_queue GROUP BY status"
    ).all<{ status: string; count: number | string }>();
    return c.json({
      items: rows.results,
      counts: Object.fromEntries(counts.results.map((row) => [row.status, Number(row.count || 0)]))
    });
  });

  app.post("/admin/asset-delete-queue/process", requireAuth, async (c) => {
    requireAdmin(c);
    const rows = await c.env.DB.prepare(
      `SELECT id, asset_key, reason
       FROM asset_delete_queue
       WHERE status = 'pending'
       ORDER BY created_at ASC LIMIT 50`
    ).all<{ id: string; asset_key: string; reason: string }>();

    let deleted = 0;
    for (const row of rows.results) {
      try {
        if (!await deps.assetDeletionAllowed(c, row.asset_key, row.reason)) {
          await c.env.DB.prepare(
            "UPDATE asset_delete_queue SET status = 'failed', processed_at = ?, error_message = ? WHERE id = ?"
          )
            .bind(new Date().toISOString(), "asset_delete_no_longer_allowed", row.id)
            .run();
          continue;
        }
        await c.env.ASSETS.delete(row.asset_key);
        await c.env.DB.batch([
          c.env.DB.prepare("DELETE FROM upload_reviews WHERE asset_key = ?").bind(row.asset_key),
          c.env.DB.prepare("DELETE FROM assets WHERE asset_key = ?").bind(row.asset_key),
          c.env.DB.prepare(
            "UPDATE asset_delete_queue SET status = 'deleted', processed_at = ?, error_message = NULL WHERE id = ?"
          ).bind(new Date().toISOString(), row.id)
        ]);
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

  app.post("/admin/asset-delete-queue/:id/retry", requireAuth, async (c) => {
    requireAdmin(c);
    const id = c.req.param("id");
    const item = await c.env.DB.prepare("SELECT id, asset_key, status FROM asset_delete_queue WHERE id = ?")
      .bind(id)
      .first<{ id: string; asset_key: string; status: string }>();
    if (!item) throw new ApiError(404, "asset_delete_not_found");
    if (item.status !== "failed") {
      throw new ApiError(409, "asset_delete_not_failed", { currentStatus: item.status });
    }
    await c.env.DB.prepare(
      "UPDATE asset_delete_queue SET status = 'pending', processed_at = NULL, error_message = NULL WHERE id = ? AND status = 'failed'"
    ).bind(item.id).run();
    await writeAudit(c, {
      action: "admin.asset_delete_queue.retry",
      targetType: "asset_delete_queue",
      targetId: item.id,
      metadata: { assetKey: item.asset_key }
    });
    return c.json({ ok: true });
  });

  app.get("/admin/account-deletion-requests", requireAuth, async (c) => {
    requireAdmin(c);
    const status = c.req.query("status") || "all";
    if (!["pending", "processing", "completed", "rejected", "all"].includes(status)) {
      throw new ApiError(400, "invalid_deletion_request_status");
    }
    const sql = `SELECT r.*, u.id AS user_id, u.display_name AS user_display_name, u.role AS user_role
      FROM account_deletion_requests r
      LEFT JOIN users u ON u.username = LOWER(r.username) AND u.deleted_at IS NULL
      ${status === "all" ? "" : "WHERE r.status = ?"}
      ORDER BY r.created_at DESC LIMIT 200`;
    const rows = status === "all"
      ? await c.env.DB.prepare(sql).all<Record<string, unknown>>()
      : await c.env.DB.prepare(sql).bind(status).all<Record<string, unknown>>();
    const counts = await c.env.DB.prepare(
      "SELECT status, COUNT(*) AS count FROM account_deletion_requests GROUP BY status"
    ).all<{ status: string; count: number | string }>();
    return c.json({
      requests: rows.results,
      counts: Object.fromEntries(counts.results.map((row) => [row.status, Number(row.count || 0)]))
    });
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

  app.get("/admin/volunteer/posts", requireAuth, async (c) => {
    requireAdmin(c);
    const rows = await c.env.DB.prepare(
      `SELECT p.id, p.title, p.body, p.contact, p.image_url, p.status, p.deadline_at, p.created_at,
              u.username AS admin_username,
              (SELECT COUNT(*) FROM community_volunteer_applications a
                WHERE a.volunteer_post_id = p.id AND a.status = 'pending') AS pending_applications,
              (SELECT COUNT(*) FROM community_volunteer_applications a
                WHERE a.volunteer_post_id = p.id AND a.status = 'approved') AS approved_applications
       FROM community_volunteer_posts p
       LEFT JOIN users u ON u.id = p.admin_id
       ORDER BY p.created_at DESC LIMIT 100`
    ).all<CommunityVolunteerRow & {
      admin_username: string | null;
      pending_applications: number | string;
      approved_applications: number | string;
    }>();
    return c.json({
      posts: rows.results.map((row) => ({
        ...deps.serializeCommunityVolunteer(row),
        adminUsername: row.admin_username,
        pendingApplications: Number(row.pending_applications || 0),
        approvedApplications: Number(row.approved_applications || 0)
      }))
    });
  });

  app.get("/admin/live2d/jobs", requireAuth, async (c) => {
    requireAdmin(c);
    const filter = c.req.query("status") || "active";
    if (!["active", "succeeded", "failed", "all"].includes(filter)) {
      throw new ApiError(400, "invalid_live2d_job_filter");
    }
    if (!await c.env.DB.hasColumn("live2d_jobs", "status")) {
      return c.json({ jobs: [], counts: {} });
    }
    const where = filter === "active"
      ? "WHERE j.status IN ('queued', 'running')"
      : filter === "succeeded"
        ? "WHERE j.status = 'succeeded'"
        : filter === "failed"
          ? "WHERE j.status IN ('failed', 'cancelled')"
          : "";
    const diagnosisReady = await c.env.DB.hasColumn("live2d_jobs", "diagnosis_code");
    const diagnosisColumns = diagnosisReady
      ? "j.diagnosis_code, j.suggestion, j.retry_hint, j.supervisor_summary,"
      : "NULL AS diagnosis_code, NULL AS suggestion, NULL AS retry_hint, NULL AS supervisor_summary,";
    const rows = await c.env.DB.prepare(
      `SELECT j.id, j.user_id, j.companion_id, j.status, j.stage, j.progress, j.error_code, j.attempts,
              j.prompt, j.lease_until, j.created_at, j.updated_at, ${diagnosisColumns}
              u.username, u.display_name, comp.display_name AS companion_name
       FROM live2d_jobs j
       LEFT JOIN users u ON u.id = j.user_id
       LEFT JOIN ai_companions comp ON comp.id = j.companion_id
       ${where}
       ORDER BY j.updated_at DESC LIMIT 200`
    ).all<Record<string, unknown>>();
    const counts = await c.env.DB.prepare(
      "SELECT status, COUNT(*) AS count FROM live2d_jobs GROUP BY status"
    ).all<{ status: string; count: number | string }>();
    return c.json({
      jobs: rows.results,
      counts: Object.fromEntries(counts.results.map((row) => [row.status, Number(row.count || 0)]))
    });
  });
}
