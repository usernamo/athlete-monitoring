import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { query } from "./db.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DATABASE_DIR =
  process.env.DATABASE_DIR || path.resolve(__dirname, "../../database");

export async function runMigrations() {
  const { rows } = await query(
    `SELECT to_regclass('public.users')::text AS users_table`
  );
  if (rows[0]?.users_table) {
    console.log("[migrate] schema already present");
    return;
  }

  if (!fs.existsSync(DATABASE_DIR)) {
    throw new Error(`database folder not found: ${DATABASE_DIR}`);
  }

  const files = fs
    .readdirSync(DATABASE_DIR)
    .filter((f) => f.endsWith(".sql"))
    .sort();

  console.log(`[migrate] applying ${files.length} SQL files...`);
  for (const file of files) {
    const sql = fs.readFileSync(path.join(DATABASE_DIR, file), "utf8");
    console.log(`[migrate] ${file}`);
    await query(sql);
  }
  console.log("[migrate] done");
}
