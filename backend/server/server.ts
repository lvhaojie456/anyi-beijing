import "dotenv/config";
import { serve } from "@hono/node-server";
import { webcrypto } from "node:crypto";
import path from "node:path";
import app from "../src/index.js";
import { LocalAssetBucket } from "./local-assets.js";
import { migrateSqlite, openSqliteDatabase, SqliteDatabaseAdapter } from "./sqlite-db.js";

if (!globalThis.crypto) {
  Object.defineProperty(globalThis, "crypto", {
    value: webcrypto,
    configurable: true
  });
}

if (!globalThis.btoa) {
  globalThis.btoa = (value: string) => Buffer.from(value, "binary").toString("base64");
}

if (!globalThis.atob) {
  globalThis.atob = (value: string) => Buffer.from(value, "base64").toString("binary");
}

const port = Number(process.env.PORT || "8787");
const dataDir = path.resolve(process.env.ANYI_DATA_DIR || path.join(process.cwd(), "data"));
const dbPath = path.resolve(process.env.ANYI_DB_PATH || path.join(dataDir, "anyi.sqlite"));
const uploadsDir = path.resolve(process.env.ANYI_UPLOADS_DIR || path.join(dataDir, "uploads"));
const migrationsDir = path.resolve(process.env.ANYI_MIGRATIONS_DIR || path.join(process.cwd(), "migrations"));

const sqlite = openSqliteDatabase(dbPath);
migrateSqlite(sqlite, migrationsDir);

const env = {
  DB: new SqliteDatabaseAdapter(sqlite),
  ASSETS: new LocalAssetBucket(uploadsDir),
  AUTH_SECRET: process.env.AUTH_SECRET || "change-this-auth-secret-before-production",
  ADMIN_USERNAMES: process.env.ADMIN_USERNAMES || "admin",
  PUBLIC_ASSET_BASE_URL: process.env.PUBLIC_ASSET_BASE_URL || "",
  ALLOWED_ORIGINS:
    process.env.ALLOWED_ORIGINS ||
    `http://localhost:${port},http://127.0.0.1:${port},http://api.anyibj.cn,https://api.anyibj.cn`,
  RATE_LIMIT_ENABLED: process.env.RATE_LIMIT_ENABLED || "true",
  AI_BASE_URL: process.env.AI_BASE_URL || "",
  AI_API_KEY: process.env.AI_API_KEY || "",
  AI_MODEL: process.env.AI_MODEL || "",
  AI_IMAGE_BASE_URL: process.env.AI_IMAGE_BASE_URL || "",
  AI_IMAGE_API_KEY: process.env.AI_IMAGE_API_KEY || "",
  AI_IMAGE_MODEL: process.env.AI_IMAGE_MODEL || "",
  AI_VISION_MODEL: process.env.AI_VISION_MODEL || "",
  AI_TIMEOUT_MS: process.env.AI_TIMEOUT_MS || "20000",
  WECHAT_APP_ID: process.env.WECHAT_APP_ID || "",
  WECHAT_APP_SECRET: process.env.WECHAT_APP_SECRET || "",
  LEGAL_OPERATOR_NAME: process.env.LEGAL_OPERATOR_NAME || "安忆",
  LEGAL_CONTACT_EMAIL: process.env.LEGAL_CONTACT_EMAIL || "544908186@qq.com",
  LEGAL_CONTACT_PHONE: process.env.LEGAL_CONTACT_PHONE || "+8619310425540",
  LEGAL_EFFECTIVE_DATE: process.env.LEGAL_EFFECTIVE_DATE || "2026-05-09"
};

const server = serve(
  {
    port,
    fetch: (request) => app.fetch(request, env)
  },
  (info) => {
    console.log(`Anyi memorial API listening on http://127.0.0.1:${info.port}`);
    console.log(`SQLite database: ${dbPath}`);
    console.log(`Uploads directory: ${uploadsDir}`);
  }
);

function shutdown() {
  console.log("Shutting down Anyi memorial API...");
  server.close(() => {
    sqlite.close();
    process.exit(0);
  });
}

process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);
