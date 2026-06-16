import pg from "pg";
import dotenv from "dotenv";

dotenv.config();

/**
 * Render Internal URL часто даёт хост вида dpg-xxxxx-a без домена.
 * Он резолвится только в той же сети/регионе. Расширяем до полного имени.
 */
function normalizeDatabaseUrl(raw) {
  if (!raw) return raw;
  const trimmed = raw.trim();
  try {
    const parsed = new URL(trimmed);
    const host = parsed.hostname;

    if (/^dpg-[a-z0-9]+-a$/i.test(host)) {
      const region =
        process.env.PG_REGION ||
        process.env.RENDER_REGION ||
        "oregon";
      parsed.hostname = `${host}.${region}-postgres.render.com`;
      console.log(`[db] Render DB host expanded to ${parsed.hostname}`);
    }

    return parsed.toString();
  } catch {
    return trimmed;
  }
}

const rawUrl =
  process.env.DATABASE_URL ||
  process.env.POSTGRES_URL ||
  process.env.POSTGRES_URL_NON_POOLING ||
  "postgresql://athlete:athlete_secret@localhost:5432/athlete_monitoring";

const connectionString = normalizeDatabaseUrl(rawUrl);

const useSsl =
  process.env.DATABASE_SSL === "true" ||
  /render\.com|neon\.tech|supabase\.co|amazonaws\.com|vercel-storage\.com/i.test(
    connectionString
  );

const pool = new pg.Pool({
  connectionString,
  ssl: useSsl ? { rejectUnauthorized: false } : undefined,
  connectionTimeoutMillis: 15000,
  idleTimeoutMillis: 30000,
  max: 10,
});

pool.on("error", (err) => {
  console.error("[db] Unexpected pool error:", err.message);
});

export async function checkConnection() {
  const client = await pool.connect();
  try {
    await client.query("SELECT 1");
  } finally {
    client.release();
  }
}

export async function query(text, params) {
  return pool.query(text, params);
}

/** Для логов — без пароля */
export function safeConnectionLabel(url) {
  try {
    const u = new URL(url);
    return `postgresql://${u.username}:***@${u.hostname}${u.pathname}`;
  } catch {
    return "postgresql://***";
  }
}

export { connectionString };
export default pool;
