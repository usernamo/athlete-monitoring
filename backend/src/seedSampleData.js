import { query } from "./db.js";
import { PREPARATION_PERIODS, upsertTrainingForDay } from "./trainingFields.js";

const COACH_ID = "44444444-4444-4444-4444-444444444401";
const ATHLETE_MALE = "55555555-5555-5555-5555-555555555501";
const ATHLETE_FEMALE = "55555555-5555-5555-5555-555555555502";
const ITERATIONS = 5;

const FOODS = [
  { name: "Овсянка", grams: 80, protein: 10, fat: 5, carbs: 54, calories: 320 },
  { name: "Яйца", grams: 120, protein: 15, fat: 10, carbs: 1, calories: 180 },
  { name: "Курица", grams: 150, protein: 32, fat: 4, carbs: 0, calories: 170 },
  { name: "Рис", grams: 200, protein: 4, fat: 1, carbs: 44, calories: 220 },
  { name: "Творог 5%", grams: 200, protein: 34, fat: 10, carbs: 6, calories: 240 },
  { name: "Гречка", grams: 180, protein: 8, fat: 2, carbs: 38, calories: 210 },
  { name: "Банан", grams: 120, protein: 1.5, fat: 0.4, carbs: 27, calories: 105 },
];

const COMPETITIONS = [
  { name: "Чемпионат региона", location: "Москва", planned: "11.2 сек 100м" },
  { name: "Кубок области", location: "Казань", planned: "23.5 сек 200м" },
  { name: "Открытый стадион", location: "Санкт-Петербург", planned: "52.1 сек 400м" },
  { name: "Мемориал тренера", location: "Сочи", planned: "4:15 мин 1500м" },
  { name: "Первенство СШОР", location: "Нижний Новгород", planned: "1.85 м прыжок" },
];

const PERIOD_VALUES = PREPARATION_PERIODS.map((p) => p.value);

const MAIN_PARTS = [
  "Бег 100 м × 8, отдых 3 мин",
  "Подтягивания 4×10",
  "Приседания со штангой 5×5",
  "Бег 400 м × 6",
  "Планка, скакалка, ОФП 30 мин",
];

const METRIC_NAMES = [
  "sleep_duration",
  "sleep_quality",
  "motivation",
  "fatigue",
  "mood",
  "wellbeing",
  "resting_hr",
  "body_weight",
];

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function rand(min, max) {
  return Math.round((Math.random() * (max - min) + min) * 10) / 10;
}

function randInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function dateStr(daysAgo) {
  const d = new Date();
  d.setDate(d.getDate() - daysAgo);
  return d.toISOString().slice(0, 10);
}

async function getAllAthleteIds() {
  const { rows } = await query(
    `SELECT ap.id FROM athlete_profiles ap
     JOIN users u ON u.id = ap.user_id
     JOIN roles r ON r.id = u.role_id
     WHERE r.name = 'athlete' AND COALESCE(u.is_active, TRUE) = TRUE
     ORDER BY ap.last_name, ap.first_name`
  );
  const ids = rows.map((r) => r.id);
  if (ids.length) return ids;
  return [ATHLETE_MALE, ATHLETE_FEMALE];
}

async function loadMetricTypeMap() {
  const { rows } = await query(`SELECT id, name FROM metric_types`);
  return Object.fromEntries(rows.map((r) => [r.name, r.id]));
}

async function clearRecent(athleteId, iterations = ITERATIONS) {
  const from = dateStr(iterations - 1);

  await query(
    `DELETE FROM athlete_metrics WHERE athlete_id = $1 AND measured_at::date >= $2::date`,
    [athleteId, from]
  );
  await query(`DELETE FROM daily_reports WHERE athlete_id = $1 AND report_date >= $2::date`, [
    athleteId,
    from,
  ]);

  await query(
    `DELETE FROM nutrition_items WHERE nutrition_log_id IN (
       SELECT id FROM nutrition_logs WHERE athlete_id = $1 AND consumed_at::date >= $2::date
     )`,
    [athleteId, from]
  );
  await query(`DELETE FROM nutrition_logs WHERE athlete_id = $1 AND consumed_at::date >= $2::date`, [
    athleteId,
    from,
  ]);
  await query(`DELETE FROM nutrition_daily_notes WHERE athlete_id = $1 AND note_date >= $2::date`, [
    athleteId,
    from,
  ]);
  await query(`DELETE FROM water_intake WHERE athlete_id = $1 AND recorded_at::date >= $2::date`, [
    athleteId,
    from,
  ]);
  await query(
    `DELETE FROM training_exercises WHERE training_block_id IN (
       SELECT tb.id FROM training_blocks tb
       JOIN trainings t ON t.id = tb.training_id
       WHERE t.athlete_id = $1 AND t.date >= $2::date
     )`,
    [athleteId, from]
  );
  await query(
    `DELETE FROM training_blocks WHERE training_id IN (
       SELECT id FROM trainings WHERE athlete_id = $1 AND date >= $2::date
     )`,
    [athleteId, from]
  );
  // Не удаляем тренировки, добавленные самим спортсменом (coach_id IS NULL)
  await query(
    `DELETE FROM trainings WHERE athlete_id = $1 AND date >= $2::date AND coach_id IS NOT NULL`,
    [athleteId, from]
  );
  await query(`DELETE FROM athlete_competitions WHERE athlete_id = $1 AND event_date >= $2::date`, [
    athleteId,
    from,
  ]);
}

async function seedDiaryDay(athleteId, dayOffset, dayIndex, metricMap) {
  const date = dateStr(dayOffset);
  const measuredAt = `${date}T12:00:00.000Z`;
  const duration = randInt(75, 140);
  const hrRest = randInt(58, 68);
  const hrLoad = randInt(138, 168);

  await query(
    `INSERT INTO daily_reports (
       athlete_id, report_date, sleep_time, wake_time, notes,
       daily_activity, wellbeing_evening, fatigue_daily, bedtime
     ) VALUES ($1, $2::date, $3, $4, $5, $6, $7, $8, $9)
     ON CONFLICT (athlete_id, report_date) DO UPDATE SET
       sleep_time = EXCLUDED.sleep_time,
       wake_time = EXCLUDED.wake_time,
       notes = EXCLUDED.notes,
       daily_activity = EXCLUDED.daily_activity,
       wellbeing_evening = EXCLUDED.wellbeing_evening,
       fatigue_daily = EXCLUDED.fatigue_daily,
       bedtime = EXCLUDED.bedtime`,
    [
      athleteId,
      date,
      `23:${randInt(0, 45).toString().padStart(2, "0")}`,
      `07:${randInt(0, 30).toString().padStart(2, "0")}`,
      `Дневник за ${date} (итерация ${dayIndex + 1}/${ITERATIONS})`,
      randInt(3, 8),
      randInt(5, 9),
      randInt(2, 8),
      `22:${randInt(30, 59).toString().padStart(2, "0")}`,
    ]
  );

  const trainingPayload = {
    preparation_period: PERIOD_VALUES[dayIndex % PERIOD_VALUES.length],
    desire_to_train: randInt(5, 10),
    wellbeing_morning: randInt(5, 9),
    start_time: `09:${randInt(0, 30).toString().padStart(2, "0")}`,
    end_time: `11:${randInt(0, 45).toString().padStart(2, "0")}`,
    part_warmup: "Разминка: бег 10 мин, суставная гимнастика, ускорения 4×30 м",
    part_main: MAIN_PARTS[dayIndex % MAIN_PARTS.length],
    part_cooldown: "Заминка: лёгкий бег 8 мин, растяжка 10 мин",
    planned_hr_before: hrRest,
    planned_hr_after: hrLoad,
    actual_hr_before: hrRest + randInt(-3, 3),
    actual_hr_after: hrLoad + randInt(-5, 8),
    duration_minutes: duration,
    work_capacity: randInt(5, 10),
    fatigue_training: randInt(3, 8),
    volume: rand(8, 16),
    intensity: rand(5, 9),
  };

  await upsertTrainingForDay(query, athleteId, date, trainingPayload, COACH_ID);

  const metricValues = {
    sleep_duration: rand(7, 9.5),
    sleep_quality: randInt(6, 10),
    motivation: randInt(6, 10),
    fatigue: randInt(2, 7),
    mood: randInt(6, 10),
    wellbeing: randInt(6, 10),
    resting_hr: randInt(52, 62),
    body_weight: rand(55, 75),
  };

  for (const name of METRIC_NAMES) {
    const typeId = metricMap[name];
    if (!typeId) continue;
    const value = metricValues[name];
    if (value == null) continue;
    await query(
      `INSERT INTO athlete_metrics (athlete_id, metric_type_id, value, measured_at)
       VALUES ($1, $2, $3, $4::timestamptz)`,
      [athleteId, typeId, value, measuredAt]
    );
  }

  await query(
    `INSERT INTO water_intake (athlete_id, amount_ml, recorded_at)
     VALUES ($1, $2, ($3::date + '10:00'::time)::timestamptz),
            ($1, $4, ($3::date + '18:00'::time)::timestamptz)`,
    [athleteId, randInt(450, 750), date, randInt(500, 900)]
  );
}

async function seedDayExtras(athleteId, dayOffset, compIndex) {
  const date = dateStr(dayOffset);
  const readiness = rand(6, 9.5);
  const fatigue = rand(2, 7);

  const mealTimes = ["08:00", "13:00", "19:00"];
  for (let m = 0; m < mealTimes.length; m++) {
    const { rows: logs } = await query(
      `INSERT INTO nutrition_logs (
         athlete_id, meal_number, meal_type, consumed_at, appetite, is_snack
       ) VALUES ($1, $2, 'meal', ($3::date + $4::time)::timestamptz, $5, false)
       RETURNING id`,
      [athleteId, m + 1, date, mealTimes[m], Math.random() > 0.15]
    );
    const food = pick(FOODS);
    await query(
      `INSERT INTO nutrition_items (
         nutrition_log_id, product_name, grams, protein, fat, carbs, calories
       ) VALUES ($1, $2, $3, $4, $5, $6, $7)`,
      [
        logs[0].id,
        food.name,
        food.grams,
        food.protein,
        food.fat,
        food.carbs,
        food.calories,
      ]
    );
  }

  await query(
    `INSERT INTO nutrition_daily_notes (athlete_id, note_date, sports_nutrition, pharmacology)
     VALUES ($1, $2::date, $3, $4)
     ON CONFLICT (athlete_id, note_date) DO UPDATE SET
       sports_nutrition = EXCLUDED.sports_nutrition,
       pharmacology = EXCLUDED.pharmacology`,
    [athleteId, date, pick(["Изотоник", "BCAA", "—"]), pick(["—", "Витамин D", "—"])]
  );

  const comp = COMPETITIONS[compIndex % COMPETITIONS.length];
  const { rows: compRows } = await query(
    `INSERT INTO competitions (name, location, start_date, end_date)
     VALUES ($1, $2, $3::date, ($3::date + 2))
     RETURNING id`,
    [`${comp.name} — ${date}`, comp.location, date]
  );
  const past = dayOffset > 0;
  const outcome = !past ? "pending" : compIndex % 2 === 0 ? "win" : "loss";
  await query(
    `INSERT INTO athlete_competitions (
       athlete_id, competition_id, event_date, location,
       planned_result, actual_result, notes, outcome
     ) VALUES ($1, $2, $3::date, $4, $5, $6, $7, $8)`,
    [
      athleteId,
      compRows[0].id,
      date,
      comp.location,
      comp.planned,
      past ? `${rand(10, 12)}.${randInt(0, 99)}` : null,
      past ? "Завершено" : "Предстоящее",
      outcome,
    ]
  );

  await query(
    `INSERT INTO analytics_snapshots (
       athlete_id, readiness_score, recovery_score, fatigue_score, injury_risk_score, generated_at
     ) VALUES ($1, $2, $3, $4, $5, ($6::date + time '20:00')::timestamptz)`,
    [athleteId, readiness, rand(6, 9), fatigue, rand(1, 4), date]
  );
}

export async function seedActivityForAthletes(athleteIds, { iterations = ITERATIONS, clearFirst = true } = {}) {
  if (!athleteIds.length) return;
  const metricMap = await loadMetricTypeMap();

  if (clearFirst) {
    for (const athleteId of athleteIds) {
      await clearRecent(athleteId, iterations);
    }
  }

  for (const athleteId of athleteIds) {
    for (let i = 0; i < iterations; i++) {
      const dayOffset = iterations - 1 - i;
      await seedDiaryDay(athleteId, dayOffset, i, metricMap);
      await seedDayExtras(athleteId, dayOffset, i);
    }
  }

  console.log(
    `[seed] ${iterations} diary days × ${athleteIds.length} athletes (daily_reports, trainings, nutrition, metrics)`
  );
}

export async function seedSampleData() {
  const athletes = await getAllAthleteIds();
  await seedActivityForAthletes(athletes, { iterations: ITERATIONS, clearFirst: true });
}
