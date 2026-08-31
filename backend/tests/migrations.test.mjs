import assert from "node:assert/strict";
import { copyFile, mkdtemp, mkdir, readdir, rm, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import test from "node:test";

import { migrateSqlite, openSqliteDatabase } from "../dist-node/server/sqlite-db.js";

test("SQLite migrations are atomic, checksummed, and safely rerunnable", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-migration-test-"));
  const migrationsDir = path.join(root, "migrations");
  const dbPath = path.join(root, "anyi.sqlite");
  await mkdir(migrationsDir);
  try {
    await writeFile(
      path.join(migrationsDir, "0001_initial.sql"),
      "CREATE TABLE notes (id INTEGER PRIMARY KEY, body TEXT NOT NULL); INSERT INTO notes (body) VALUES ('a;b');\n"
    );
    await writeFile(path.join(migrationsDir, "._0001_initial.sql"), Buffer.from([0, 5, 22, 7]));
    const db = openSqliteDatabase(dbPath);
    migrateSqlite(db, migrationsDir);
    assert.equal(db.prepare("SELECT body FROM notes").get().body, "a;b");
    assert.match(db.prepare("SELECT checksum FROM _node_migrations WHERE name = ?").get("0001_initial.sql").checksum, /^[a-f0-9]{64}$/);

    const originalMigration = "CREATE TABLE notes (id INTEGER PRIMARY KEY, body TEXT NOT NULL); INSERT INTO notes (body) VALUES ('a;b');\n";
    await assert.rejects(
      async () => {
        await writeFile(path.join(migrationsDir, "0001_initial.sql"), "CREATE TABLE notes (id INTEGER PRIMARY KEY, body TEXT NOT NULL);\n");
        migrateSqlite(db, migrationsDir);
      },
      /has changed since it was applied/
    );
    await writeFile(path.join(migrationsDir, "0001_initial.sql"), originalMigration);

    await writeFile(path.join(migrationsDir, "0002_broken.sql"), "CREATE TABLE broken (id INTEGER); THIS IS NOT SQL;\n");
    assert.throws(() => migrateSqlite(db, migrationsDir), /Migration 0002_broken.sql failed/);
    assert.equal(db.prepare("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?").get("broken"), undefined);
    assert.equal(db.prepare("SELECT name FROM _node_migrations WHERE name = ?").get("0002_broken.sql"), undefined);

    await writeFile(path.join(migrationsDir, "0002_broken.sql"), "ALTER TABLE notes ADD COLUMN label TEXT;\n");
    migrateSqlite(db, migrationsDir);
    assert.equal(db.prepare("SELECT name FROM _node_migrations WHERE name = ?").get("0002_broken.sql").name, "0002_broken.sql");
    db.close();
  } finally {
    await rm(root, { recursive: true, force: true });
  }
});

test("SQLite preserves compatibility with an already-applied ADD COLUMN", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-migration-compat-"));
  const migrationsDir = path.join(root, "migrations");
  const dbPath = path.join(root, "anyi.sqlite");
  await mkdir(migrationsDir);
  try {
    const db = openSqliteDatabase(dbPath);
    db.exec("CREATE TABLE memorials (id TEXT PRIMARY KEY, candle_until_json TEXT NOT NULL DEFAULT '[]')");
    await writeFile(path.join(migrationsDir, "0005_dual_memorial_candles.sql"), "ALTER TABLE memorials ADD COLUMN candle_until_json TEXT NOT NULL DEFAULT '[]';\n");
    migrateSqlite(db, migrationsDir);
    assert.equal(db.prepare("SELECT name FROM _node_migrations").get().name, "0005_dual_memorial_candles.sql");
    db.close();
  } finally {
    await rm(root, { recursive: true, force: true });
  }
});

test("volunteer recruitment migration preserves applications and seeds manageable projects", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-volunteer-migration-"));
  const migrationsDir = path.join(root, "migrations");
  const dbPath = path.join(root, "anyi.sqlite");
  const sourceMigrationsDir = path.resolve("migrations");
  const volunteerMigration = "0023_community_volunteer_recruitment_flow.sql";
  await mkdir(migrationsDir);

  try {
    const baseMigrations = (await readdir(sourceMigrationsDir))
      .filter((file) => /^[0-9].*\.sql$/.test(file) && file < volunteerMigration)
      .sort();
    for (const file of baseMigrations) {
      await copyFile(path.join(sourceMigrationsDir, file), path.join(migrationsDir, file));
    }

    const db = openSqliteDatabase(dbPath);
    migrateSqlite(db, migrationsDir);
    db.prepare(
      `INSERT INTO users (id, username, password_hash, display_name, role, created_at)
       VALUES (?, ?, ?, ?, ?, ?)`
    ).run("admin-1", "migration_admin", "hash", "Admin", "admin", "2026-01-01T00:00:00.000Z");
    db.prepare(
      `INSERT INTO users (id, username, password_hash, display_name, role, created_at)
       VALUES (?, ?, ?, ?, ?, ?)`
    ).run("user-1", "migration_user", "hash", "User", "user", "2026-01-02T00:00:00.000Z");
    db.prepare(
      `INSERT INTO community_volunteer_applications (
         id, volunteer_post_id, volunteer_title, user_id, name, phone, note,
         status, reviewer_id, reviewed_at, created_at, updated_at
       ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).run(
      "application-1",
      "default-story",
      "故事整理义工",
      "user-1",
      "User",
      "13800000000",
      "weekends",
      "approved",
      "admin-1",
      "2026-02-01T00:00:00.000Z",
      "2026-01-10T00:00:00.000Z",
      "2026-02-01T00:00:00.000Z"
    );

    await copyFile(
      path.join(sourceMigrationsDir, volunteerMigration),
      path.join(migrationsDir, volunteerMigration)
    );
    migrateSqlite(db, migrationsDir);

    assert.equal(
      db.prepare("SELECT COUNT(*) AS count FROM community_volunteer_posts").get().count,
      3
    );
    assert.equal(
      db.prepare("SELECT admin_id FROM community_volunteer_posts WHERE id = ?").get("default-story").admin_id,
      "admin-1"
    );
    assert.equal(
      db.prepare("SELECT status FROM community_volunteer_applications WHERE id = ?").get("application-1").status,
      "approved"
    );
    db.prepare("UPDATE community_volunteer_applications SET status = 'cancelled' WHERE id = ?")
      .run("application-1");
    assert.equal(
      db.prepare("SELECT status FROM community_volunteer_applications WHERE id = ?").get("application-1").status,
      "cancelled"
    );
    migrateSqlite(db, migrationsDir);
    db.close();
  } finally {
    await rm(root, { recursive: true, force: true });
  }
});

test("AI companion reset migration intentionally clears every legacy AI data table", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-ai-reset-migration-"));
  const migrationsDir = path.join(root, "migrations");
  const dbPath = path.join(root, "anyi.sqlite");
  const sourceMigrationsDir = path.resolve("migrations");
  const resetMigration = "0025_ai_companion_reset.sql";
  await mkdir(migrationsDir);
  try {
    const baseMigrations = (await readdir(sourceMigrationsDir))
      .filter((file) => /^[0-9].*\.sql$/.test(file) && file < resetMigration)
      .sort();
    for (const file of baseMigrations) {
      await copyFile(path.join(sourceMigrationsDir, file), path.join(migrationsDir, file));
    }
    const db = openSqliteDatabase(dbPath);
    migrateSqlite(db, migrationsDir);
    db.prepare(
      `INSERT INTO users (id, username, password_hash, display_name, role, created_at)
       VALUES (?, ?, ?, ?, ?, ?)`
    ).run("user-ai", "legacy_ai_user", "hash", "Legacy", "user", "2026-01-01T00:00:00.000Z");
    db.prepare("INSERT INTO ai_profiles (user_id, updated_at) VALUES (?, ?)")
      .run("user-ai", "2026-01-01T00:00:00.000Z");
    db.prepare(
      `INSERT INTO ai_companions (
         id, user_id, display_name, gender, relation, avatar_motion_json,
         avatar_style_json, kernel_json, created_at, updated_at
       ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).run(
      "legacy-companion", "user-ai", "Legacy", "不限定", "亲人", "{}", "{}", "{}",
      "2026-01-01T00:00:00.000Z", "2026-01-01T00:00:00.000Z"
    );
    db.prepare(
      "INSERT INTO ai_chat_messages (id, user_id, companion_id, sender, content, created_at) VALUES (?, ?, ?, ?, ?, ?)"
    ).run("legacy-message", "user-ai", "legacy-companion", "user", "hello", "2026-01-01T00:00:00.000Z");
    db.prepare(
      `INSERT INTO ai_memory_items (
         id, user_id, companion_key, memory_type, memory_key, content, confidence,
         importance, created_at, updated_at
       ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).run(
      "legacy-memory", "user-ai", "legacy-companion", "fact", "fact:legacy", "legacy", 1, 50,
      "2026-01-01T00:00:00.000Z", "2026-01-01T00:00:00.000Z"
    );
    db.prepare("INSERT INTO ai_memory_settings (user_id, enabled, updated_at) VALUES (?, ?, ?)")
      .run("user-ai", 1, "2026-01-01T00:00:00.000Z");

    await copyFile(path.join(sourceMigrationsDir, resetMigration), path.join(migrationsDir, resetMigration));
    migrateSqlite(db, migrationsDir);
    for (const table of [
      "ai_memory_items", "ai_memory_settings", "ai_chat_messages", "ai_companions", "ai_profiles"
    ]) {
      assert.equal(db.prepare(`SELECT COUNT(*) AS count FROM ${table}`).get().count, 0, table);
    }
    db.close();
  } finally {
    await rm(root, { recursive: true, force: true });
  }
});

test("user gender migration preserves existing accounts and allows only male or female", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-user-gender-migration-"));
  const migrationsDir = path.join(root, "migrations");
  const dbPath = path.join(root, "anyi.sqlite");
  const sourceMigrationsDir = path.resolve("migrations");
  const genderMigration = "0026_user_gender.sql";
  await mkdir(migrationsDir);
  try {
    const baseMigrations = (await readdir(sourceMigrationsDir))
      .filter((file) => /^[0-9].*\.sql$/.test(file) && file < genderMigration)
      .sort();
    for (const file of baseMigrations) {
      await copyFile(path.join(sourceMigrationsDir, file), path.join(migrationsDir, file));
    }

    const db = openSqliteDatabase(dbPath);
    migrateSqlite(db, migrationsDir);
    db.prepare(
      `INSERT INTO users (id, username, password_hash, display_name, role, created_at)
       VALUES (?, ?, ?, ?, ?, ?)`
    ).run("legacy-user", "legacy_user", "hash", "Legacy", "user", "2026-01-01T00:00:00.000Z");

    await copyFile(path.join(sourceMigrationsDir, genderMigration), path.join(migrationsDir, genderMigration));
    migrateSqlite(db, migrationsDir);
    assert.equal(db.prepare("SELECT gender FROM users WHERE id = ?").get("legacy-user").gender, null);
    db.prepare("UPDATE users SET gender = ? WHERE id = ?").run("男", "legacy-user");
    assert.equal(db.prepare("SELECT gender FROM users WHERE id = ?").get("legacy-user").gender, "男");
    assert.throws(
      () => db.prepare("UPDATE users SET gender = ? WHERE id = ?").run("未知", "legacy-user"),
      /CHECK constraint failed/
    );
    migrateSqlite(db, migrationsDir);
    db.close();
  } finally {
    await rm(root, { recursive: true, force: true });
  }
});

test("AI background migration keeps list and per-companion backgrounds independent", async () => {
  const root = await mkdtemp(path.join(os.tmpdir(), "anyi-ai-background-migration-"));
  const migrationsDir = path.join(root, "migrations");
  const dbPath = path.join(root, "anyi.sqlite");
  const sourceMigrationsDir = path.resolve("migrations");
  const backgroundMigration = "0027_ai_companion_backgrounds.sql";
  await mkdir(migrationsDir);
  try {
    const baseMigrations = (await readdir(sourceMigrationsDir))
      .filter((file) => /^[0-9].*\.sql$/.test(file) && file < backgroundMigration)
      .sort();
    for (const file of baseMigrations) {
      await copyFile(path.join(sourceMigrationsDir, file), path.join(migrationsDir, file));
    }
    const db = openSqliteDatabase(dbPath);
    migrateSqlite(db, migrationsDir);
    db.prepare(
      `INSERT INTO users (id, username, password_hash, display_name, role, created_at)
       VALUES (?, ?, ?, ?, ?, ?)`
    ).run("background-user", "background_user", "hash", "Background", "user", "2026-01-01T00:00:00.000Z");
    db.prepare(
      `INSERT INTO ai_companions (
         id, user_id, display_name, gender, relation, avatar_motion_json,
         avatar_style_json, kernel_json, created_at, updated_at
       ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    ).run(
      "background-companion", "background-user", "旧相机", "未指定", "旧物", "{}", "{}", "{}",
      "2026-01-01T00:00:00.000Z", "2026-01-01T00:00:00.000Z"
    );

    await copyFile(path.join(sourceMigrationsDir, backgroundMigration), path.join(migrationsDir, backgroundMigration));
    migrateSqlite(db, migrationsDir);
    assert.equal(
      db.prepare("SELECT ai_companion_list_background_url FROM users WHERE id = ?").get("background-user")
        .ai_companion_list_background_url,
      null
    );
    assert.equal(
      db.prepare("SELECT chat_background_url FROM ai_companions WHERE id = ?").get("background-companion")
        .chat_background_url,
      null
    );
    db.prepare("UPDATE users SET ai_companion_list_background_url = ? WHERE id = ?")
      .run("/assets/list.png", "background-user");
    db.prepare("UPDATE ai_companions SET chat_background_url = ? WHERE id = ?")
      .run("/assets/chat.png", "background-companion");
    assert.deepEqual(
      db.prepare(
        `SELECT u.ai_companion_list_background_url AS list_background,
                c.chat_background_url AS chat_background
         FROM users u JOIN ai_companions c ON c.user_id = u.id
         WHERE u.id = ?`
      ).get("background-user"),
      { list_background: "/assets/list.png", chat_background: "/assets/chat.png" }
    );
    migrateSqlite(db, migrationsDir);
    db.close();
  } finally {
    await rm(root, { recursive: true, force: true });
  }
});
