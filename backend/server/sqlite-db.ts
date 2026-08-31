import { createHash } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, readdirSync } from "node:fs";
import path from "node:path";

import Database from "better-sqlite3";

type BindValue = string | number | bigint | boolean | null | Uint8Array;

function normalizeBindValue(value: unknown): BindValue {
  if (value === undefined) return null;
  if (value instanceof Uint8Array) return value;
  if (
    typeof value === "string" ||
    typeof value === "number" ||
    typeof value === "bigint" ||
    typeof value === "boolean" ||
    value === null
  ) {
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
    const transaction = this.db.transaction(() =>
      statements.map((statement) => statement.executeForBatch())
    );
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

  const metadataTransaction = db.transaction(() => ensureMigrationTable(db));
  metadataTransaction.immediate();

  const files = readdirSync(migrationsDir)
    .filter((file) => /^[0-9][A-Za-z0-9._-]*\.sql$/.test(file))
    .sort();

  // One immediate transaction serializes startup migrations and prevents a
  // process from observing a partially applied schema.
  const transaction = db.transaction(() => {
    for (const file of files) {
      const sql = readFileSync(path.join(migrationsDir, file), "utf8");
      const checksum = checksumSql(sql);
      const applied = db
        .prepare("SELECT name, checksum FROM _node_migrations WHERE name = ?")
        .get(file) as MigrationRecord | undefined;

      if (applied) {
        if (applied.checksum && applied.checksum !== checksum) {
          throw new Error(`Migration ${file} has changed since it was applied`);
        }
        // Older databases predate checksums. Record a baseline once, while
        // retaining compatibility with their existing migration history.
        if (!applied.checksum) {
          db.prepare("UPDATE _node_migrations SET checksum = ? WHERE name = ?").run(checksum, file);
        }
        continue;
      }

      for (const statement of splitSqlStatements(sql)) {
        executeMigrationStatement(db, statement, file);
      }
      db.prepare(
        "INSERT INTO _node_migrations (name, applied_at, checksum) VALUES (?, ?, ?)"
      ).run(file, new Date().toISOString(), checksum);
    }
  });
  transaction.immediate();
}

function splitSqlStatements(sql: string) {
  const statements: string[] = [];
  let statement = "";
  let quote: "'" | '"' | "`" | null = null;
  let lineComment = false;
  let blockComment = false;

  for (let index = 0; index < sql.length; index += 1) {
    const character = sql[index];
    const next = sql[index + 1];

    if (lineComment) {
      statement += character;
      if (character === "\n") lineComment = false;
      continue;
    }
    if (blockComment) {
      statement += character;
      if (character === "*" && next === "/") {
        statement += next;
        index += 1;
        blockComment = false;
      }
      continue;
    }
    if (quote) {
      statement += character;
      if (character === "\\" && next) {
        statement += next;
        index += 1;
        continue;
      }
      if (character === quote) {
        if (sql[index + 1] === quote) {
          statement += sql[index + 1];
          index += 1;
        } else {
          quote = null;
        }
      }
      continue;
    }
    if (character === "-" && next === "-") {
      statement += character + next;
      index += 1;
      lineComment = true;
      continue;
    }
    if (character === "#") {
      statement += character;
      lineComment = true;
      continue;
    }
    if (character === "/" && next === "*") {
      statement += character + next;
      index += 1;
      blockComment = true;
      continue;
    }
    if (character === "'" || character === '"' || character === "`") {
      quote = character;
      statement += character;
      continue;
    }
    if (character === ";") {
      if (statement.trim()) statements.push(statement.trim());
      statement = "";
      continue;
    }
    statement += character;
  }

  if (statement.trim()) statements.push(statement.trim());
  return statements;
}

type MigrationRecord = { name: string; checksum: string | null };

function ensureMigrationTable(db: Database.Database) {
  db.exec(`
    CREATE TABLE IF NOT EXISTS _node_migrations (
      name TEXT PRIMARY KEY,
      applied_at TEXT NOT NULL,
      checksum TEXT
    )
  `);
  const columns = db.prepare('PRAGMA table_info("_node_migrations")').all() as { name: string }[];
  const names = new Set(columns.map((column) => column.name));
  if (!names.has("name") || !names.has("applied_at")) {
    throw new Error("_node_migrations has an incompatible schema");
  }
  if (!names.has("checksum")) {
    db.exec('ALTER TABLE "_node_migrations" ADD COLUMN checksum TEXT');
  }
}

function checksumSql(sql: string) {
  return createHash("sha256").update(sql).digest("hex");
}

function executeMigrationStatement(db: Database.Database, statement: string, file: string) {
  try {
    db.exec(statement);
  } catch (error) {
    if (isCompatibleAlterAlreadyApplied(db, statement, error)) return;
    const message = error instanceof Error ? error.message : String(error);
    throw new Error(`Migration ${file} failed: ${message}`, { cause: error });
  }
}

function isCompatibleAlterAlreadyApplied(db: Database.Database, statement: string, error: unknown) {
  const message = error instanceof Error ? error.message : String(error);
  const addColumn = /^\s*ALTER\s+TABLE\s+([\w"`]+)\s+ADD\s+COLUMN\s+([\w"`]+)/i.exec(statement);
  if (addColumn) {
    if (!/duplicate column name/i.test(message)) return false;
    const table = unquoteIdentifier(addColumn[1]);
    const column = unquoteIdentifier(addColumn[2]);
    return hasTable(db, table) && hasColumn(db, table, column);
  }

  const renameColumn = /^\s*ALTER\s+TABLE\s+([\w"`]+)\s+RENAME\s+COLUMN\s+([\w"`]+)\s+TO\s+([\w"`]+)/i.exec(statement);
  if (renameColumn) {
    if (!/(no such column|duplicate column name)/i.test(message)) return false;
    const table = unquoteIdentifier(renameColumn[1]);
    const oldColumn = unquoteIdentifier(renameColumn[2]);
    const newColumn = unquoteIdentifier(renameColumn[3]);
    return hasTable(db, table) && !hasColumn(db, table, oldColumn) && hasColumn(db, table, newColumn);
  }

  const renameTable = /^\s*ALTER\s+TABLE\s+([\w"`]+)\s+RENAME\s+TO\s+([\w"`]+)/i.exec(statement);
  if (renameTable) {
    if (!/(no such table|already exists)/i.test(message)) return false;
    const oldTable = unquoteIdentifier(renameTable[1]);
    const newTable = unquoteIdentifier(renameTable[2]);
    return !hasTable(db, oldTable) && hasTable(db, newTable);
  }
  return false;
}

function hasTable(db: Database.Database, tableName: string) {
  const row = db
    .prepare("SELECT 1 AS present FROM sqlite_master WHERE type = 'table' AND name = ?")
    .get(tableName);
  return Boolean(row);
}

function hasColumn(db: Database.Database, tableName: string, columnName: string) {
  const rows = db.prepare(`PRAGMA table_info(${quoteIdentifier(tableName)})`).all() as { name: string }[];
  return rows.some((row) => row.name === columnName);
}

function unquoteIdentifier(value: string) {
  if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("`") && value.endsWith("`"))) {
    return value.slice(1, -1).replace(/(["`])\1/g, "$1");
  }
  return value;
}
