import dotenv from "dotenv";
import { dirname, join } from "path";
import { fileURLToPath } from "url";
import { checkConnection } from "../src/db.js";
import { bootstrap } from "../src/bootstrap.js";
import { seedSlaves } from "../src/seed50Slaves.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: join(__dirname, "../.env") });

const url =
  process.env.DATABASE_URL ||
  process.env.POSTGRES_URL ||
  process.env.POSTGRES_URL_NON_POOLING;

if (!url) {
  console.error("Set DATABASE_URL or POSTGRES_URL (from Vercel → Storage → Postgres)");
  process.exit(1);
}

process.env.DATABASE_URL = url;
process.env.DATABASE_SSL = "true";
process.env.DATABASE_DIR = process.env.DATABASE_DIR || join(__dirname, "../../database");

try {
  await checkConnection();
  console.log("Connected to remote database");
} catch (e) {
  console.error("Connection failed:", e.message);
  process.exit(1);
}

await bootstrap();

if (process.argv.includes("--slaves")) {
  await seedSlaves({ withActivity: true, activityDays: 14 });
}

console.log("Remote database setup complete.");
