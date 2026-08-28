import "dotenv/config";
import { serve } from "@hono/node-server";
import { webcrypto } from "node:crypto";
import path from "node:path";
import app from "../src/index.js";
import { LocalAssetBucket } from "./local-assets.js";
import { openMySqlDatabaseFromEnv } from "./mysql-db.js";
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
const hostname = process.env.HOST || "127.0.0.1";
const dbDriver = normalizeDbDriver(process.env.DB_DRIVER || "sqlite");
const dataDir = path.resolve(process.env.ANYI_DATA_DIR || path.join(process.cwd(), "data"));
const dbPath = path.resolve(process.env.ANYI_DB_PATH || path.join(dataDir, "anyi.sqlite"));
const uploadsDir = path.resolve(process.env.ANYI_UPLOADS_DIR || path.join(dataDir, "uploads"));
const migrationsDir = path.resolve(process.env.ANYI_MIGRATIONS_DIR || path.join(process.cwd(), "migrations"));
const mysqlMigrationsDir = path.resolve(process.env.ANYI_MYSQL_MIGRATIONS_DIR || path.join(process.cwd(), "migrations-mysql"));

if (process.env.NODE_ENV === "production") {
  const authSecret = process.env.AUTH_SECRET?.trim() || "";
  if (!authSecret || /replace-with|change-this|your-.*-secret/i.test(authSecret) || authSecret.length < 32) {
    throw new Error("A random AUTH_SECRET of at least 32 characters is required in production");
  }
}

const database = await openAppDatabase(dbDriver);

const env = {
  DB: database,
  ASSETS: new LocalAssetBucket(uploadsDir),
  AUTH_SECRET: process.env.AUTH_SECRET || "change-this-auth-secret-before-production",
  PUBLIC_ASSET_BASE_URL: process.env.PUBLIC_ASSET_BASE_URL || "",
  ALLOWED_ORIGINS:
    process.env.ALLOWED_ORIGINS ||
    `http://localhost:${port},http://127.0.0.1:${port},https://api.anyibj.cn`,
  RATE_LIMIT_ENABLED: process.env.RATE_LIMIT_ENABLED || "true",
  PAYMENT_ENABLED: process.env.PAYMENT_ENABLED || "false",
  PAYMENT_WEBHOOK_SECRET: process.env.PAYMENT_WEBHOOK_SECRET || "",
  AI_BASE_URL: process.env.AI_BASE_URL || "",
  AI_API_KEY: process.env.AI_API_KEY || "",
  AI_MODEL: process.env.AI_MODEL || "",
  AI_IMAGE_BASE_URL: process.env.AI_IMAGE_BASE_URL || "",
  AI_IMAGE_API_KEY: process.env.AI_IMAGE_API_KEY || "",
  AI_IMAGE_MODEL: process.env.AI_IMAGE_MODEL || "",
  AI_VISION_MODEL: process.env.AI_VISION_MODEL || "",
  DIGITAL_HUMAN_CHAT_MODEL: process.env.DIGITAL_HUMAN_CHAT_MODEL || "",
  AI_TIMEOUT_MS: process.env.AI_TIMEOUT_MS || "20000",
  WECHAT_APP_ID: process.env.WECHAT_APP_ID || "",
  WECHAT_APP_SECRET: process.env.WECHAT_APP_SECRET || "",
  LEGAL_OPERATOR_NAME: process.env.LEGAL_OPERATOR_NAME || "安忆",
  LEGAL_CONTACT_EMAIL: process.env.LEGAL_CONTACT_EMAIL || "544908186@qq.com",
  LEGAL_CONTACT_PHONE: process.env.LEGAL_CONTACT_PHONE || "+8619310425540",
  LEGAL_EFFECTIVE_DATE: process.env.LEGAL_EFFECTIVE_DATE || "2026-05-09",
  VTUBER_URL: process.env.VTUBER_URL || "",
  VTUBER_ENABLED: process.env.VTUBER_ENABLED || "true"
};

const server = serve(
  {
    port,
    hostname,
    fetch: (request) => app.fetch(request, env)
  },
  (info) => {
    console.log(`Anyi memorial API listening on http://${hostname}:${info.port}`);
    if (dbDriver === "sqlite") {
      console.log(`SQLite database: ${dbPath}`);
    } else {
      console.log(`MySQL database: ${process.env.MYSQL_HOST || "127.0.0.1"}:${process.env.MYSQL_PORT || "3306"}/${process.env.MYSQL_DATABASE || ""}`);
    }
    console.log(`Uploads directory: ${uploadsDir}`);
  }
);

function shutdown() {
  console.log("Shutting down Anyi memorial API...");
  server.close(async () => {
    await database.close();
    process.exit(0);
  });
}

process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);

function normalizeDbDriver(value: string) {
  const normalized = value.toLowerCase();
  if (normalized === "sqlite" || normalized === "mysql") {
    return normalized;
  }
  throw new Error(`Unsupported DB_DRIVER: ${value}`);
}

async function openAppDatabase(driver: "sqlite" | "mysql") {
  if (driver === "mysql") {
    return openMySqlDatabaseFromEnv(process.env, mysqlMigrationsDir);
  }

  const sqlite = openSqliteDatabase(dbPath);
  migrateSqlite(sqlite, migrationsDir);
  return new SqliteDatabaseAdapter(sqlite);
}
