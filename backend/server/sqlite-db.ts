import Database from "better-sqlite3";
import { existsSync, mkdirSync, readFileSync, readdirSync } from "node:fs";
import path from "node:path";

type BindValue = string | number | bigint | boolean | null | Uint8Array;

function normalizeBindValue(value: unknown): BindValue {
  if (value === undefined) return null;
  if (value instanceof Uint8Array) return value;
  if (typeof value === "string" || typeof value === "number" || typeof value === "bigint" || typeof value === "boolean" || value === null) {
    return value;
  }
  return String(value);
}

export class SqlitePreparedStatement {
  private params: BindValue[] = [];

  constructor(
    private readonly db: Database.Database,
    private readonly query: string
  ) {}

  bind(...values: unknown[]) {
    this.params = values.map(normalizeBindValue);
    return this;
  }

  async first<T = unknown>(): Promise<T | null> {
    const row = this.db.prepare(this.query).get(...this.params);
    return (row ?? null) as T | null;
  }

  async all<T = unknown>(): Promise<{ results: T[]; success: boolean; meta: Record<string, unknown> }> {
    const rows = this.db.prepare(this.query).all(...this.params) as T[];
    return { results: rows, success: true, meta: {} };
  }

  async run(): Promise<{ success: boolean; meta: { changes: number; last_row_id: number | bigint } }> {
    const result = this.db.prepare(this.query).run(...this.params);
    return {
      success: true,
      meta: {
        changes: result.changes,
        last_row_id: result.lastInsertRowid
      }
    };
  }

  executeForBatch() {
    const prepared = this.db.prepare(this.query);
    if (/^\s*select\b/i.test(this.query)) {
      return prepared.all(...this.params);
    }
    return prepared.run(...this.params);
  }
}

export class SqliteDatabaseAdapter {
  readonly dialect = "sqlite" as const;

  constructor(private readonly db: Database.Database) {}

  prepare(query: string) {
    return new SqlitePreparedStatement(this.db, query);
  }

  async batch(statements: SqlitePreparedStatement[]) {
    const transaction = this.db.transaction(() => statements.map((statement) => statement.executeForBatch()));
    return transaction();
  }

  async hasColumn(tableName: string, columnName: string) {
    const rows = this.db.prepare(`PRAGMA table_info(${quoteIdentifier(tableName)})`).all() as { name: string }[];
    return rows.some((row) => row.name === columnName);
  }

  close() {
    this.db.close();
  }
}

function quoteIdentifier(value: string) {
  return `"${value.replace(/"/g, '""')}"`;
}

export function openSqliteDatabase(dbPath: string) {
  mkdirSync(path.dirname(dbPath), { recursive: true });
  const db = new Database(dbPath);
  db.pragma("journal_mode = WAL");
  db.pragma("foreign_keys = ON");
  return db;
}

export function migrateSqlite(db: Database.Database, migrationsDir: string) {
  if (!existsSync(migrationsDir)) {
    throw new Error(`Migrations directory not found: ${migrationsDir}`);
  }

  db.exec(`
    CREATE TABLE IF NOT EXISTS _node_migrations (
      name TEXT PRIMARY KEY,
      applied_at TEXT NOT NULL
    )
  `);

  const files = readdirSync(migrationsDir)
    .filter((file) => file.endsWith(".sql"))
    .sort();

  for (const file of files) {
    const applied = db.prepare("SELECT name FROM _node_migrations WHERE name = ?").get(file);
    if (applied) continue;

    const sql = readFileSync(path.join(migrationsDir, file), "utf8");
    const statements = splitSqlStatements(sql);
    const transaction = db.transaction(() => {
      for (const statement of statements) {
        try {
          db.exec(statement);
        } catch (error) {
          if (isIgnorableMigrationError(error)) {
            continue;
          }
          throw error;
        }
      }
      db.prepare("INSERT INTO _node_migrations (name, applied_at) VALUES (?, ?)").run(file, new Date().toISOString());
    });
    transaction();
  }
}

function splitSqlStatements(sql: string) {
  return sql
    .split(";")
    .map((statement) => statement.trim())
    .filter(Boolean);
}

function isIgnorableMigrationError(error: unknown) {
  const message = error instanceof Error ? error.message.toLowerCase() : String(error).toLowerCase();
  return (
    message.includes("duplicate column name") ||
    message.includes("no such column") ||
    message.includes("no such table") ||
    message.includes("already exists")
  );
}
