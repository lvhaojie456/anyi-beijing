import { createPool, type Pool, type PoolConnection, type ResultSetHeader, type RowDataPacket } from "mysql2/promise";
import { existsSync, readFileSync, readdirSync } from "node:fs";
import path from "node:path";

type BindValue = string | number | bigint | null | Buffer;

function normalizeBindValue(value: unknown): BindValue {
  if (value === undefined) return null;
  if (value instanceof Uint8Array) return Buffer.from(value);
  if (typeof value === "boolean") return value ? 1 : 0;
  if (typeof value === "string" || typeof value === "number" || typeof value === "bigint" || value === null) {
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

export async function openMySqlDatabaseFromEnv(env: NodeJS.ProcessEnv = process.env, migrationsDir?: string) {
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

async function migrateMySql(pool: Pool, migrationsDir: string) {
  if (!existsSync(migrationsDir)) {
    throw new Error(`MySQL migrations directory not found: ${migrationsDir}`);
  }

  await pool.query(`
    CREATE TABLE IF NOT EXISTS _node_migrations (
      name VARCHAR(191) NOT NULL,
      applied_at VARCHAR(32) NOT NULL,
      PRIMARY KEY (name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  `);

  const files = readdirSync(migrationsDir)
    .filter((file) => file.endsWith(".sql"))
    .sort();

  for (const file of files) {
    const [rows] = await pool.execute<RowDataPacket[]>(
      "SELECT name FROM _node_migrations WHERE name = ?",
      [file]
    );
    if (rows.length > 0) continue;

    const sql = readFileSync(path.join(migrationsDir, file), "utf8");
    for (const statement of splitSqlStatements(sql)) {
      await pool.query(statement);
    }
    await pool.execute(
      "INSERT INTO _node_migrations (name, applied_at) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = name",
      [file, new Date().toISOString()]
    );
  }
}

function splitSqlStatements(sql: string) {
  return sql
    .split(";")
    .map((statement) => statement.trim())
    .filter(Boolean);
}
