import { checkConnection, safeConnectionLabel, connectionString } from "./db.js";
import { ensureTestData } from "./ensureTestData.js";
import { runMigrations } from "./migrate.js";
import { ensureMetricCatalog } from "./ensureMetricCatalog.js";
import { ensureSchema } from "./ensureSchema.js";
import { seedSlaves } from "./seed50Slaves.js";

let bootstrapPromise = null;

export async function bootstrap() {
  if (bootstrapPromise) return bootstrapPromise;

  bootstrapPromise = (async () => {
    console.log("[bootstrap] starting...");
    console.log(`[bootstrap] database: ${safeConnectionLabel(connectionString)}`);
    await checkConnection();
    await runMigrations();
    await ensureSchema();
    await ensureMetricCatalog();
    await ensureTestData();
    if (process.env.SEED_50_SLAVES === "1" || process.env.SEED_56_SLAVES === "1") {
      await seedSlaves({ withActivity: true, activityDays: 14 });
    }
    console.log("[bootstrap] ready");
  })().catch((e) => {
    bootstrapPromise = null;
    throw e;
  });

  return bootstrapPromise;
}
