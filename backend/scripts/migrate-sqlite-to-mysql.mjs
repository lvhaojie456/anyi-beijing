import "dotenv/config";
import Database from "better-sqlite3";
import mysql from "mysql2/promise";
import { existsSync, readFileSync, readdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const backendRoot = path.resolve(__dirname, "..");

const importPlan = [
  table("users", ["id", "username", "password_hash", "display_name", "role", "avatar_url", "created_at", "deleted_at", "wechat_openid", "wechat_unionid", "wechat_nickname"], ["id"]),
  table("talisman_products", ["id", "name", "description", "price_cents", "image_url", "active", "created_at"], ["id"]),
  table("memorials", ["id", "owner_id", "name", "image_url", "flower_until_json", "candle_until", "candle_until_json", "fruit_offerings_json", "incense_until", "created_at", "updated_at"], ["id"], {
    defaults: { flower_until_json: "[]", candle_until: 0, candle_until_json: "[]", fruit_offerings_json: "[]", incense_until: 0 }
  }),
  table("ritual_orders", ["id", "user_id", "deceased_name", "relation", "service_date", "address", "plan_id", "plan_name", "amount_cents", "status", "note", "acceptance_image_urls", "publisher_name", "needs_clothes", "clothes_count", "created_at", "updated_at"], ["id"], {
    defaults: { acceptance_image_urls: "[]", needs_clothes: 0, clothes_count: 0 }
  }),
  table("order_messages", ["id", "order_id", "sender_id", "sender_role", "content", "created_at"], ["id"]),
  table("talisman_orders", ["id", "user_id", "product_id", "amount_cents", "status", "created_at", "updated_at"], ["id"]),
  table("assets", ["id", "owner_id", "asset_key", "url", "mime_type", "size_bytes", "created_at"], ["id"], { aliases: { asset_key: ["asset_key", "r2_key"] } }),
  table("payment_events", ["id", "order_type", "order_id", "provider", "provider_trade_no", "amount_cents", "status", "raw_json", "created_at"], ["id"]),
  table("ai_profiles", ["user_id", "gender", "relation", "avatar_url", "smile_avatar_url", "avatar_motion_json", "paid_unlocked", "photo_count", "voice_count", "moment_count", "generated", "updated_at"], ["user_id"], {
    defaults: { smile_avatar_url: null, avatar_motion_json: "{}" }
  }),
  table("ai_companions", ["id", "user_id", "display_name", "gender", "relation", "avatar_url", "smile_avatar_url", "avatar_motion_json", "paid_unlocked", "photo_count", "voice_count", "moment_count", "generated", "avatar_style_json", "kernel_json", "is_default", "created_at", "updated_at"], ["id"], {
    defaults: { smile_avatar_url: null, avatar_motion_json: "{}", avatar_style_json: "{}", kernel_json: "{}" }
  }),
  table("ai_chat_messages", ["id", "user_id", "companion_id", "sender", "content", "created_at"], ["id"], { defaults: { companion_id: null } }),
  table("feature_unlocks", ["user_id", "feature", "created_at"], ["user_id", "feature"]),
  table("account_deletion_requests", ["id", "username", "contact", "reason", "status", "created_at", "updated_at"], ["id"], { defaults: { status: "pending" } }),
  table("rate_limits", ["bucket_key", "route_key", "window_start", "count", "updated_at"], ["bucket_key", "route_key", "window_start"]),
  table("audit_logs", ["id", "actor_id", "actor_role", "action", "target_type", "target_id", "ip", "user_agent", "metadata_json", "created_at"], ["id"], { defaults: { metadata_json: "{}" } }),
  table("upload_reviews", ["id", "asset_id", "owner_id", "asset_key", "mime_type", "size_bytes", "status", "reason", "created_at", "reviewed_at", "reviewed_by"], ["id"], {
    aliases: { asset_key: ["asset_key", "r2_key"] },
    defaults: { status: "pending" }
  }),
  table("asset_delete_queue", ["id", "owner_id", "asset_key", "reason", "status", "created_at", "processed_at", "error_message"], ["id"], {
    sourceTables: ["asset_delete_queue", "r2_delete_queue"],
    aliases: { asset_key: ["asset_key", "r2_key"] },
    defaults: { status: "pending" }
  }),
  table("crash_reports", ["id", "user_id", "platform", "app_version", "device_model", "os_version", "error_type", "message", "stack_trace", "created_at"], ["id"]),
  table("community_posts", ["id", "user_id", "content", "image_urls", "created_at", "updated_at"], ["id"], { defaults: { image_urls: "[]" } }),
  table("community_post_likes", ["post_id", "user_id", "created_at"], ["post_id", "user_id"]),
  table("community_volunteer_posts", ["id", "admin_id", "title", "body", "contact", "image_url", "created_at"], ["id"], { defaults: { image_url: null } }),
  table("community_volunteer_applications", ["id", "volunteer_post_id", "volunteer_title", "user_id", "name", "phone", "note", "status", "reviewer_id", "reviewed_at", "created_at", "updated_at"], ["id"], {
    defaults: { status: "pending", reviewer_id: null, reviewed_at: null }
  }),
  table("community_post_comments", ["id", "post_id", "user_id", "content", "created_at", "updated_at"], ["id"])
];

const args = parseArgs(process.argv.slice(2));

if (args.help) {
  printHelp();
  process.exit(0);
}

const sqlitePath = path.resolve(args.sqlite || process.env.SQLITE_DB_PATH || process.env.ANYI_DB_PATH || path.join(process.env.ANYI_DATA_DIR || path.join(backendRoot, "data"), "anyi.sqlite"));
const migrationsDir = path.resolve(args["migrations-dir"] || process.env.ANYI_MYSQL_MIGRATIONS_DIR || path.join(backendRoot, "migrations-mysql"));
const batchSize = Number(args["batch-size"] || process.env.MYSQL_MIGRATION_BATCH_SIZE || "500");
const dryRun = Boolean(args["dry-run"]);
const resetTarget = Boolean(args["reset-target"]);
const allowNonemptyTarget = Boolean(args["allow-nonempty-target"]);
const yes = Boolean(args.yes);

if (!Number.isInteger(batchSize) || batchSize < 1) {
  throw new Error("--batch-size must be a positive integer");
}

if (!existsSync(sqlitePath)) {
  throw new Error(`SQLite database not found: ${sqlitePath}`);
}

const sqlite = new Database(sqlitePath, { readonly: true, fileMustExist: true });
sqlite.pragma("query_only = ON");

try {
  const sourcePlan = buildSourcePlan(sqlite, importPlan);
  const sourceSummary = readSourceSummary(sqlite, sourcePlan);

  if (dryRun) {
    console.log(JSON.stringify({
      mode: "dry-run",
      sqlitePath,
      migrationsDir,
      source: sourceSummary
    }, null, 2));
    process.exit(0);
  }

  if (resetTarget && !yes) {
    throw new Error("--reset-target requires --yes because it deletes target MySQL table contents");
  }

  const pool = await openMySqlPool();
  try {
    await applyMySqlMigrations(pool, migrationsDir);

    if (resetTarget) {
      await resetTargetTables(pool, importPlan);
    } else if (!allowNonemptyTarget) {
      await assertTargetIsEmptyEnough(pool, importPlan);
    }

    const imported = [];
    for (const config of sourcePlan) {
      const result = await importTable(sqlite, pool, config, batchSize);
      imported.push(result);
      console.log(`${config.target}: source=${result.sourceRows} imported=${result.importedRows}`);
    }

    const verification = await verifyCounts(pool, sourceSummary);
    const failed = verification.filter((row) => row.status !== "ok");
    console.log(JSON.stringify({ imported, verification }, null, 2));

    if (failed.length > 0) {
      throw new Error(`count verification failed for: ${failed.map((row) => row.table).join(", ")}`);
    }
  } finally {
    await pool.end();
  }
} finally {
  sqlite.close();
}

function table(target, columns, primaryKey, options = {}) {
  return {
    target,
    sourceTables: options.sourceTables || [target],
    columns,
    primaryKey,
    aliases: options.aliases || {},
    defaults: options.defaults || {}
  };
}

function parseArgs(argv) {
  const result = {};
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (!arg.startsWith("--")) {
      throw new Error(`Unexpected argument: ${arg}`);
    }
    const trimmed = arg.slice(2);
    const eq = trimmed.indexOf("=");
    if (eq >= 0) {
      result[trimmed.slice(0, eq)] = trimmed.slice(eq + 1);
      continue;
    }
    const next = argv[index + 1];
    if (!next || next.startsWith("--")) {
      result[trimmed] = true;
      continue;
    }
    result[trimmed] = next;
    index += 1;
  }
  return result;
}

function printHelp() {
  console.log(`Usage:
  node scripts/migrate-sqlite-to-mysql.mjs --dry-run
  node scripts/migrate-sqlite-to-mysql.mjs --sqlite /var/lib/anyi-memorial-api/anyi.sqlite

Environment:
  MYSQL_HOST, MYSQL_PORT, MYSQL_USER, MYSQL_PASSWORD, MYSQL_DATABASE

Options:
  --sqlite PATH                 Source SQLite file. Defaults to ANYI_DB_PATH or ANYI_DATA_DIR/anyi.sqlite.
  --migrations-dir PATH         MySQL migrations directory. Defaults to backend/migrations-mysql.
  --batch-size N                Rows per bulk insert. Default: 500.
  --dry-run                     Read SQLite and print source mapping/counts only.
  --allow-nonempty-target       Allow importing into a target that already has rows.
  --reset-target --yes          Truncate target business tables before import.
`);
}

function buildSourcePlan(db, plan) {
  const sqliteTables = new Set(db.prepare("SELECT name FROM sqlite_master WHERE type = 'table'").all().map((row) => row.name));

  return plan.map((config) => {
    const sourceTable = config.sourceTables.find((name) => sqliteTables.has(name));
    if (!sourceTable) {
      return { ...config, sourceTable: null, sourceColumns: new Set(), selectExpressions: [] };
    }

    const sourceColumns = new Set(db.prepare(`PRAGMA table_info(${quoteSqliteIdentifier(sourceTable)})`).all().map((row) => row.name));
    const selectExpressions = config.columns.map((column) => {
      const sourceColumn = resolveSourceColumn(column, sourceColumns, config.aliases);
      if (sourceColumn) {
        return `${quoteSqliteIdentifier(sourceColumn)} AS ${quoteSqliteIdentifier(column)}`;
      }
      if (Object.prototype.hasOwnProperty.call(config.defaults, column)) {
        return `${sqliteLiteral(config.defaults[column])} AS ${quoteSqliteIdentifier(column)}`;
      }
      return `NULL AS ${quoteSqliteIdentifier(column)}`;
    });

    return { ...config, sourceTable, sourceColumns, selectExpressions };
  });
}

function resolveSourceColumn(column, sourceColumns, aliases) {
  const candidates = aliases[column] || [column];
  return candidates.find((candidate) => sourceColumns.has(candidate)) || null;
}

function readSourceSummary(db, plan) {
  return plan.map((config) => ({
    table: config.target,
    sourceTable: config.sourceTable,
    sourceRows: config.sourceTable
      ? Number(db.prepare(`SELECT COUNT(*) AS count FROM ${quoteSqliteIdentifier(config.sourceTable)}`).get().count)
      : 0
  }));
}

async function openMySqlPool() {
  const host = process.env.MYSQL_HOST || "127.0.0.1";
  const port = Number(process.env.MYSQL_PORT || "3306");
  const user = process.env.MYSQL_USER;
  const password = process.env.MYSQL_PASSWORD;
  const database = process.env.MYSQL_DATABASE;

  if (!user || !database) {
    throw new Error("MYSQL_USER and MYSQL_DATABASE are required unless --dry-run is used");
  }

  const pool = mysql.createPool({
    host,
    port,
    user,
    password,
    database,
    waitForConnections: true,
    connectionLimit: Number(process.env.MYSQL_CONNECTION_LIMIT || "5"),
    charset: "utf8mb4",
    timezone: "Z",
    supportBigNumbers: true,
    bigNumberStrings: true,
    multipleStatements: false
  });

  await pool.query("SELECT 1");
  return pool;
}

async function applyMySqlMigrations(pool, dir) {
  if (!existsSync(dir)) {
    throw new Error(`MySQL migrations directory not found: ${dir}`);
  }

  await pool.query(`
    CREATE TABLE IF NOT EXISTS _node_migrations (
      name VARCHAR(191) NOT NULL,
      applied_at VARCHAR(32) NOT NULL,
      PRIMARY KEY (name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  `);

  const files = readdirSync(dir)
    .filter((file) => file.endsWith(".sql"))
    .sort();

  for (const file of files) {
    const [rows] = await pool.execute("SELECT name FROM _node_migrations WHERE name = ?", [file]);
    if (rows.length > 0) continue;

    const sql = readFileSync(path.join(dir, file), "utf8");
    for (const statement of splitSqlStatements(sql)) {
      await pool.query(statement);
    }
    await pool.execute("INSERT INTO _node_migrations (name, applied_at) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = name", [file, new Date().toISOString()]);
  }
}

function splitSqlStatements(sql) {
  return sql
    .split(";")
    .map((statement) => statement.trim())
    .filter(Boolean);
}

async function resetTargetTables(pool, plan) {
  await pool.query("SET FOREIGN_KEY_CHECKS = 0");
  try {
    for (const config of [...plan].reverse()) {
      await pool.query(`TRUNCATE TABLE ${quoteMySqlIdentifier(config.target)}`);
    }
  } finally {
    await pool.query("SET FOREIGN_KEY_CHECKS = 1");
  }
}

async function assertTargetIsEmptyEnough(pool, plan) {
  const nonempty = [];
  for (const config of plan) {
    const [rows] = await pool.query(`SELECT COUNT(*) AS count FROM ${quoteMySqlIdentifier(config.target)}`);
    const count = Number(rows[0]?.count || 0);
    if (count > 0 && config.target !== "talisman_products") {
      nonempty.push({ table: config.target, rows: count });
    }
  }

  if (nonempty.length > 0) {
    throw new Error(`target MySQL tables are not empty: ${nonempty.map((row) => `${row.table}=${row.rows}`).join(", ")}. Use --allow-nonempty-target or --reset-target --yes.`);
  }
}

async function importTable(sqliteDb, pool, config, batchSize) {
  if (!config.sourceTable) {
    return { table: config.target, sourceRows: 0, importedRows: 0 };
  }

  const sourceRows = Number(sqliteDb.prepare(`SELECT COUNT(*) AS count FROM ${quoteSqliteIdentifier(config.sourceTable)}`).get().count);
  if (sourceRows === 0) {
    return { table: config.target, sourceRows, importedRows: 0 };
  }

  const selectSql = `SELECT ${config.selectExpressions.join(", ")} FROM ${quoteSqliteIdentifier(config.sourceTable)} LIMIT ? OFFSET ?`;
  const select = sqliteDb.prepare(selectSql);
  const insertSql = buildInsertSql(config);
  let importedRows = 0;

  for (let offset = 0; offset < sourceRows; offset += batchSize) {
    const rows = select.all(batchSize, offset);
    if (rows.length === 0) break;

    const values = rows.map((row) => config.columns.map((column) => normalizeValue(row[column])));
    await pool.query(insertSql, [values]);
    importedRows += rows.length;
  }

  return { table: config.target, sourceRows, importedRows };
}

function buildInsertSql(config) {
  const columnsSql = config.columns.map(quoteMySqlIdentifier).join(", ");
  const updateColumns = config.columns.filter((column) => !config.primaryKey.includes(column));
  const updateSql = updateColumns.length > 0
    ? ` ON DUPLICATE KEY UPDATE ${updateColumns.map((column) => `${quoteMySqlIdentifier(column)} = VALUES(${quoteMySqlIdentifier(column)})`).join(", ")}`
    : "";
  return `INSERT INTO ${quoteMySqlIdentifier(config.target)} (${columnsSql}) VALUES ?${updateSql}`;
}

async function verifyCounts(pool, sourceSummary) {
  const verification = [];
  for (const source of sourceSummary) {
    const [rows] = await pool.query(`SELECT COUNT(*) AS count FROM ${quoteMySqlIdentifier(source.table)}`);
    const targetRows = Number(rows[0]?.count || 0);
    verification.push({
      table: source.table,
      sourceRows: source.sourceRows,
      targetRows,
      status: source.sourceRows === targetRows ? "ok" : "mismatch"
    });
  }
  return verification;
}

function normalizeValue(value) {
  if (value === undefined) return null;
  if (typeof value === "boolean") return value ? 1 : 0;
  if (value instanceof Uint8Array) return Buffer.from(value);
  return value;
}

function sqliteLiteral(value) {
  if (value === null || value === undefined) return "NULL";
  if (typeof value === "number" || typeof value === "bigint") return String(value);
  return `'${String(value).replace(/'/g, "''")}'`;
}

function quoteSqliteIdentifier(value) {
  return `"${String(value).replace(/"/g, '""')}"`;
}

function quoteMySqlIdentifier(value) {
  return `\`${String(value).replace(/`/g, "``")}\``;
}
