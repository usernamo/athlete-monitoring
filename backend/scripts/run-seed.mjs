import dotenv from "dotenv";
import { dirname, join } from "path";
import { fileURLToPath } from "url";
import { checkConnection } from "../src/db.js";
import { ensureTestData } from "../src/ensureTestData.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
dotenv.config({ path: join(__dirname, "../.env") });

const ok = await checkConnection();
if (!ok) {
  console.error("Database unavailable. Run: npm run db:up");
  process.exit(1);
}
await ensureTestData();
console.log("Done.");
