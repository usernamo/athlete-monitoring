import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { query } from "./db.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

function resolveDatabaseDir() {
  const raw = process.env.DATABASE_DIR;
  if (!raw) return path.resolve(__dirname, "../../database");
  return path.isAbsolute(raw) ? raw : path.resolve(process.cwd(), raw);
}

const DATABASE_DIR = resolveDatabaseDir();

/** Только полный начальный сид — на уже существующей БД не перезапускаем */
const SKIP_WHEN_DB_EXISTS = new Set(["001_schema.sql", "002_seed.sql"]);

export async function runMigrations() {
  if (!fs.existsSync(DATABASE_DIR)) {
    throw new Error(`database folder not found: ${DATABASE_DIR}`);
  }

  const { rows } = await query(
    `SELECT to_regclass('public.users')::text AS users_table`
  );
  const dbExists = !!rows[0]?.users_table;

  const files = fs
    .readdirSync(DATABASE_DIR)
    .filter((f) => f.endsWith(".sql"))
    .sort();

  if (!dbExists) {
    console.log(`[migrate] fresh database — applying all ${files.length} SQL files`);
  } else {
    console.log(
      `[migrate] existing database — applying incremental migrations (${files.length - SKIP_WHEN_DB_EXISTS.size} files)`
    );
  }

  for (const file of files) {
    if (dbExists && SKIP_WHEN_DB_EXISTS.has(file)) {
      console.log(`[migrate] skip ${file}`);
      continue;
    }
    const sql = fs.readFileSync(path.join(DATABASE_DIR, file), "utf8");
    console.log(`[migrate] apply ${file}`);
    try {
      await query(sql);
    } catch (e) {
      console.error(`[migrate] failed on ${file}:`, e.message);
      throw e;
    }
  }
  console.log("[migrate] done");
}
