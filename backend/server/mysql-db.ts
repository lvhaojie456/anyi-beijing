import { createHash } from "node:crypto";
import { existsSync, readFileSync, readdirSync } from "node:fs";
import path from "node:path";

import {
  createPool,
  type Pool,
  type PoolConnection,
  type ResultSetHeader,
  type RowDataPacket
} from "mysql2/promise";

import { checksumSql, splitSqlStatements } from "./migration-sql.js";

type BindValue = string | number | bigint | null | Buffer;

function normalizeBindValue(value: unknown): BindValue {
  if (value === undefined) return null;
  if (value instanceof Uint8Array) return Buffer.from(value);
  if (typeof value === "boolean") return value ? 1 : 0;
  if (
    typeof value === "string" ||
    typeof value === "number" ||
    typeof value === "bigint" ||
    value === null
  ) {
    return value;
  }
  return String(value);
}

export class MySqlPreparedStatement {
  private params: BindValue[] = [];

  constructor(
    private readonly pool: Pool,
    private readonly query: string
  ) {}

  bind(...values: unknown[]) {
    this.params = values.map(normalizeBindValue);
    return this;
  }

  async first<T = unknown>(): Promise<T | null> {
    const [rows] = await this.pool.execute<RowDataPacket[]>(this.query, this.params);
    return ((rows[0] as T | undefined) ?? null) as T | null;
  }

  async all<T = unknown>(): Promise<{ results: T[]; success: boolean; meta: Record<string, unknown> }> {
    const [rows] = await this.pool.execute<RowDataPacket[]>(this.query, this.params);
    return { results: rows as T[], success: true, meta: {} };
  }

  async run(): Promise<{ success: boolean; meta: { changes: number; last_row_id: number } }> {
    const [result] = await this.pool.execute<ResultSetHeader>(this.query, this.params);
    return {
      success: true,
      meta: {
        changes: result.affectedRows,
        last_row_id: result.insertId
      }
    };
  }

  async executeForBatch(connection: PoolConnection) {
    const [result] = await connection.execute(this.query, this.params);
    return result;
  }
}

export class MySqlDatabaseAdapter {
  readonly dialect = "mysql" as const;

  constructor(private readonly pool: Pool) {}

  prepare(query: string) {
    return new MySqlPreparedStatement(this.pool, query);
  }

  async batch(statements: MySqlPreparedStatement[]) {
    const connection = await this.pool.getConnection();
    try {
      await connection.beginTransaction();
      const results = [];
      for (const statement of statements) {
        results.push(await statement.executeForBatch(connection));
      }
      await connection.commit();
      return results;
    } catch (error) {
      await connection.rollback();
      throw error;
    } finally {
      connection.release();
    }
  }

  async hasColumn(tableName: string, columnName: string) {
    const [rows] = await this.pool.execute<RowDataPacket[]>(
      `SELECT COUNT(*) AS count
         FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = ?
          AND COLUMN_NAME = ?`,
      [tableName, columnName]
    );
    return Number(rows[0]?.count || 0) > 0;
  }

  async close() {
    await this.pool.end();
  }
}

export async function openMySqlDatabaseFromEnv(
  env: NodeJS.ProcessEnv = process.env,
  migrationsDir?: string
) {
  const host = env.MYSQL_HOST || "127.0.0.1";
  const port = Number(env.MYSQL_PORT || "3306");
  const user = env.MYSQL_USER;
  const password = env.MYSQL_PASSWORD;
  const database = env.MYSQL_DATABASE;

  if (!user || !database) {
    throw new Error("MYSQL_USER and MYSQL_DATABASE are required when DB_DRIVER=mysql");
  }

  const pool = createPool({
    host,
    port,
    user,
    password,
    database,
    waitForConnections: true,
    connectionLimit: Number(env.MYSQL_CONNECTION_LIMIT || "10"),
    charset: "utf8mb4",
    timezone: "Z",
    supportBigNumbers: true,
    bigNumberStrings: true
  });

  await pool.query("SELECT 1");
  if (migrationsDir) {
    await migrateMySql(pool, migrationsDir);
  }
  return new MySqlDatabaseAdapter(pool);
}

export async function migrateMySql(pool: Pool, migrationsDir: string) {
  if (!existsSync(migrationsDir)) {
    throw new Error(`MySQL migrations directory not found: ${migrationsDir}`);
  }

  const files = readdirSync(migrationsDir)
    .filter((file) => /^[0-9][A-Za-z0-9._-]*\.sql$/.test(file))
    .sort();

  const connection = await pool.getConnection();
  const [databaseRows] = await connection.query<RowDataPacket[]>("SELECT DATABASE() AS name");
  const databaseName = String(databaseRows[0]?.name || "default");
  const lockName = migrationLockName(databaseName);
  try {
    const lockTimeoutSeconds = Number(process.env.MYSQL_MIGRATION_LOCK_TIMEOUT_SECONDS || "60");
    if (!Number.isInteger(lockTimeoutSeconds) || lockTimeoutSeconds < 0) {
      throw new Error("MYSQL_MIGRATION_LOCK_TIMEOUT_SECONDS must be a non-negative integer");
    }
    const [lockRows] = await connection.query<RowDataPacket[]>("SELECT GET_LOCK(?, ?) AS acquired", [
      lockName,
      lockTimeoutSeconds
    ]);
    if (Number(lockRows[0]?.acquired || 0) !== 1) {
      throw new Error(`Timed out waiting for MySQL migration lock ${lockName}`);
    }

    await ensureMigrationTable(connection);
    for (const file of files) {
      const sql = readFileSync(path.join(migrationsDir, file), "utf8");
      const checksum = checksumSql(sql);
      const [rows] = await connection.execute<RowDataPacket[]>(
        "SELECT name, checksum FROM _node_migrations WHERE name = ?",
        [file]
      );
      const applied = rows[0];
      if (applied) {
        if (applied.checksum && applied.checksum !== checksum) {
          throw new Error(`Migration ${file} has changed since it was applied`);
        }
        if (!applied.checksum) {
          await connection.execute("UPDATE _node_migrations SET checksum = ? WHERE name = ?", [checksum, file]);
        }
        continue;
      }

      // InnoDB DML is transactional, while MySQL DDL may implicitly commit.
      // Keep the record insert in the same transaction where possible and only
      // write it after every statement completed successfully.
      await connection.beginTransaction();
      try {
        for (const statement of splitSqlStatements(sql)) {
          await executeMigrationStatement(connection, statement);
        }
        await connection.execute(
          "INSERT INTO _node_migrations (name, applied_at, checksum) VALUES (?, ?, ?)",
          [file, new Date().toISOString(), checksum]
        );
        await connection.commit();
      } catch (error) {
        await connection.rollback();
        const message = error instanceof Error ? error.message : String(error);
        throw new Error(`Migration ${file} failed: ${message}`, { cause: error });
      }
    }
  } finally {
    try {
      await connection.query("SELECT RELEASE_LOCK(?)", [lockName]);
    } finally {
      connection.release();
    }
  }
}

async function executeMigrationStatement(connection: PoolConnection, statement: string) {
  try {
    await connection.query(statement);
  } catch (error) {
    if (await isCompatibleAlterAlreadyApplied(connection, statement, error)) return;
    throw error;
  }
}

async function isCompatibleAlterAlreadyApplied(
  connection: PoolConnection,
  statement: string,
  error: unknown
) {
  const message = error instanceof Error ? error.message : String(error);
  if (!/duplicate (?:column|key|index)|already exists/i.test(message)) return false;
  const tableMatch = /^\s*ALTER\s+TABLE\s+`?([A-Za-z0-9_]+)`?/i.exec(statement);
  if (!tableMatch) return false;

  const columns = [...statement.matchAll(
    /\bADD\s+(?:COLUMN\s+)?(?!KEY\b|INDEX\b|UNIQUE\b|PRIMARY\b|CONSTRAINT\b)`?([A-Za-z0-9_]+)`?/gi
  )].map((match) => match[1]);
  const indexes = [...statement.matchAll(
    /\bADD\s+(?:UNIQUE\s+)?(?:KEY|INDEX)\s+`?([A-Za-z0-9_]+)`?/gi
  )].map((match) => match[1]);
  if (columns.length === 0 && indexes.length === 0) return false;

  const [columnRows] = await connection.execute<RowDataPacket[]>(
    `SELECT COLUMN_NAME AS name FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?`,
    [tableMatch[1]]
  );
  const [indexRows] = await connection.execute<RowDataPacket[]>(
    `SELECT DISTINCT INDEX_NAME AS name FROM INFORMATION_SCHEMA.STATISTICS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?`,
    [tableMatch[1]]
  );
  const existingColumns = new Set(columnRows.map((row) => String(row.name)));
  const existingIndexes = new Set(indexRows.map((row) => String(row.name)));
  return columns.every((name) => existingColumns.has(name)) &&
    indexes.every((name) => existingIndexes.has(name));
}

async function ensureMigrationTable(connection: PoolConnection) {
  await connection.query(`
    CREATE TABLE IF NOT EXISTS _node_migrations (
      name VARCHAR(191) NOT NULL,
      applied_at VARCHAR(32) NOT NULL,
      checksum CHAR(64) NULL,
      PRIMARY KEY (name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  `);
  const [columns] = await connection.query<RowDataPacket[]>(
    `SELECT COLUMN_NAME AS name
       FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = '_node_migrations'`
  );
  if (!columns.some((column) => column.name === "name") || !columns.some((column) => column.name === "applied_at")) {
    throw new Error("_node_migrations has an incompatible schema");
  }
  if (!columns.some((column) => column.name === "checksum")) {
    await connection.query("ALTER TABLE _node_migrations ADD COLUMN checksum CHAR(64) NULL");
  }
}

function migrationLockName(databaseName: string) {
  // GET_LOCK names are limited to 64 bytes. Hash the database name so two
  // databases on one MySQL server cannot accidentally share a truncated lock.
  return `anyi-migrations:${createHash("sha256").update(databaseName).digest("hex").slice(0, 48)}`;
}
