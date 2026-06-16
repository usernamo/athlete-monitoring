import dotenv from "dotenv";
import { dirname, join } from "path";
import { fileURLToPath } from "url";
import { checkConnection } from "../src/db.js";
import { ensureTestData } from "../src/ensureTestData.js";
import { seed50Slaves } from "../src/seed50Slaves.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: join(__dirname, "../.env") });

try {
  await checkConnection();
} catch (e) {
  console.error("Database unavailable:", e.message);
  console.error("Run: docker compose up -d");
  process.exit(1);
}

await ensureTestData();
await seed50Slaves();
console.log("Done.");
