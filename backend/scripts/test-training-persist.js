/**
 * Проверка сохранения тренировки спортсмена:
 *   node scripts/test-training-persist.js
 */
const BASE = process.env.API_BASE || "http://127.0.0.1:3000";
const ATHLETE_ID = "55555555-5555-5555-5555-555555555501";
const DATE = new Date().toISOString().slice(0, 10);

const payload = {
  start_time: "09:00",
  end_time: "11:00",
  part_warmup: "разминка тест",
  part_main: "основная тест",
  part_cooldown: "заминка тест",
  planned_hr_before: 60,
  planned_hr_after: 150,
  actual_hr_before: 62,
  actual_hr_after: 148,
};

async function req(method, path, body) {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: { "Content-Type": "application/json" },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json;
  try {
    json = JSON.parse(text);
  } catch {
    json = { raw: text };
  }
  return { status: res.status, json };
}

async function main() {
  console.log("POST training", DATE);
  const save = await req("POST", `/api/athletes/${ATHLETE_ID}/trainings/${DATE}`, payload);
  console.log("save:", save.status, save.json);
  if (save.status !== 200) process.exit(1);

  const load = await req("GET", `/api/athletes/${ATHLETE_ID}/trainings/${DATE}`);
  console.log("load:", load.status, load.json);
  const t = load.json?.training;
  if (!t?.part_main || t.part_main !== payload.part_main) {
    console.error("FAIL: training not persisted");
    process.exit(1);
  }
  console.log("OK: training persisted");
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
