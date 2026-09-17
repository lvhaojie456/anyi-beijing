import { createHash } from "node:crypto";

// Shared by the SQLite and MySQL migration runners so both dialects split and
// checksum migration files identically.
export function splitSqlStatements(sql: string) {
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

export function checksumSql(sql: string) {
  return createHash("sha256").update(sql).digest("hex");
}
