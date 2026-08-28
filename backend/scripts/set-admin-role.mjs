import "dotenv/config";
import Database from "better-sqlite3";
import { createPool } from "mysql2/promise";

const username = process.argv[2]?.trim().toLowerCase();
if (!username || !/^[a-z0-9_]{3,64}$/.test(username)) {
  console.error("Usage: node scripts/set-admin-role.mjs USERNAME");
  process.exit(2);
}

const driver = (process.env.DB_DRIVER || "sqlite").toLowerCase();
if (driver === "mysql") {
  const pool = createPool({
    host: process.env.MYSQL_HOST || "127.0.0.1",
    port: Number(process.env.MYSQL_PORT || "3306"),
    user: process.env.MYSQL_USER,
    password: process.env.MYSQL_PASSWORD,
    database: process.env.MYSQL_DATABASE,
    charset: "utf8mb4"
  });
  try {
    const [result] = await pool.execute(
      "UPDATE users SET role = 'admin' WHERE username = ? AND deleted_at IS NULL",
      [username]
    );
    if (!result.affectedRows) {
      throw new Error(`Active user not found: ${username}`);
    }
  } finally {
    await pool.end();
  }
} else if (driver === "sqlite") {
  const dbPath = process.env.ANYI_DB_PATH || `${process.env.ANYI_DATA_DIR || "./data"}/anyi.sqlite`;
  const db = new Database(dbPath);
  try {
    const result = db.prepare(
      "UPDATE users SET role = 'admin' WHERE username = ? AND deleted_at IS NULL"
    ).run(username);
    if (!result.changes) {
      throw new Error(`Active user not found: ${username}`);
    }
  } finally {
    db.close();
  }
} else {
  throw new Error(`Unsupported DB_DRIVER: ${driver}`);
}

console.log(`Administrator role granted to ${username}`);
