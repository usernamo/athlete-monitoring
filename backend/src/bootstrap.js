import { checkConnection, safeConnectionLabel, connectionString, query } from "./db.js";
import { ensureTestData } from "./ensureTestData.js";
import { runMigrations } from "./migrate.js";
import { ensureMetricCatalog } from "./ensureMetricCatalog.js";
import { ensureSchema } from "./ensureSchema.js";
import { seedSlaves } from "./seed50Slaves.js";

let bootstrapPromise = null;

async function isDatabaseInitialized() {
  const { rows } = await query(
    `SELECT to_regclass('public.users')::text AS users_table`
  );
  return !!rows[0]?.users_table;
}

async function bootstrapFull() {
  await runMigrations();
  await ensureSchema();
  await ensureMetricCatalog();
  await ensureTestData();
  if (process.env.SEED_50_SLAVES === "1" || process.env.SEED_56_SLAVES === "1") {
    await seedSlaves({ withActivity: true, activityDays: 14 });
  }
}

/** Быстрая проверка для /health — без миграций и сидов */
export async function pingDatabase() {
  await checkConnection();
}

/**
 * На Vercel после первой инициализации БД — только проверка соединения.
 * Полный bootstrap (миграции, сиды) — при пустой БД или локально.
 */
export async function bootstrap() {
  if (bootstrapPromise) return bootstrapPromise;

  bootstrapPromise = (async () => {
    console.log("[bootstrap] starting...");
    console.log(`[bootstrap] database: ${safeConnectionLabel(connectionString)}`);
    await checkConnection();

    const initialized = await isDatabaseInitialized();

    if (!initialized) {
      console.log("[bootstrap] empty database — full setup");
      await bootstrapFull();
    } else if (process.env.VERCEL === "1") {
      console.log("[bootstrap] Vercel — DB ready, skipping migrations on cold start");
    } else {
      console.log("[bootstrap] local — incremental migrations");
      await bootstrapFull();
    }

    console.log("[bootstrap] ready");
  })().catch((e) => {
    bootstrapPromise = null;
    throw e;
  });

  return bootstrapPromise;
}
