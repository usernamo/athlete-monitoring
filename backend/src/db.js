import pg from "pg";
import dotenv from "dotenv";

dotenv.config();

const connectionString =
  process.env.DATABASE_URL ||
  "postgresql://athlete:athlete_secret@localhost:5432/athlete_monitoring";

const useSsl =
  process.env.DATABASE_SSL === "true" ||
  /render\.com|neon\.tech|supabase\.co|amazonaws\.com/i.test(connectionString);

const pool = new pg.Pool({
  connectionString,
  ssl: useSsl ? { rejectUnauthorized: false } : undefined,
  connectionTimeoutMillis: 10000,
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

export { connectionString };
export default pool;
