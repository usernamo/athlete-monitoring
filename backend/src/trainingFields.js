export const PREPARATION_PERIODS = [
  { value: "general", label: "Обще-подготовительный" },
  { value: "special", label: "Специально-подготовительный" },
  { value: "pre_competition", label: "Предсоревновательный" },
  { value: "competition", label: "Соревновательный" },
  { value: "transitional", label: "Переходный" },
];

const PERIOD_VALUES = new Set(PREPARATION_PERIODS.map((p) => p.value));

export function normalizePeriod(v) {
  if (!v) return null;
  const s = String(v).trim().toLowerCase();
  const map = {
    general: "general",
    "обще-подготовительный": "general",
    special: "special",
    "специально-подготовительный": "special",
    pre_competition: "pre_competition",
    "предсоревновательный": "pre_competition",
    competition: "competition",
    соревновательный: "competition",
    transitional: "transitional",
    переходный: "transitional",
  };
  const norm = map[s] || s;
  return PERIOD_VALUES.has(norm) ? norm : null;
}

function int1to10(v) {
  if (v == null || v === "") return null;
  const n = parseInt(v, 10);
  if (Number.isNaN(n) || n < 1 || n > 10) return null;
  return n;
}

function intHr(v) {
  if (v == null || v === "") return null;
  const n = parseInt(v, 10);
  return Number.isNaN(n) ? null : n;
}

function text(v) {
  if (v == null) return null;
  const s = String(v).trim();
  return s || null;
}

function timeVal(v) {
  if (!v) return null;
  const s = String(v).trim();
  const hm = s.match(/^(\d{1,2}):(\d{2})/);
  if (hm) {
    const h = String(hm[1]).padStart(2, "0");
    return `${h}:${hm[2]}:00`;
  }
  if (/^\d{4}$/.test(s)) {
    return `${s.slice(0, 2)}:${s.slice(2)}:00`;
  }
  return s.length >= 4 ? s.slice(0, 8) : null;
}

/** Парсинг объекта training из тела запроса дневника */
export function parseTrainingPayload(raw = {}) {
  return {
    preparation_period: normalizePeriod(raw.preparation_period),
    desire_to_train: int1to10(raw.desire_to_train),
    wellbeing_morning: int1to10(raw.wellbeing_morning ?? raw.wellbeing),
    start_time: timeVal(raw.start_time),
    end_time: timeVal(raw.end_time),
    part_warmup: text(raw.part_warmup),
    part_main: text(raw.part_main),
    part_cooldown: text(raw.part_cooldown),
    planned_hr_before: intHr(raw.planned_hr_before),
    planned_hr_after: intHr(raw.planned_hr_after),
    actual_hr_before: intHr(raw.actual_hr_before),
    actual_hr_after: intHr(raw.actual_hr_after),
    duration_minutes: intHr(raw.duration_minutes),
    work_capacity: int1to10(raw.work_capacity),
    fatigue_training: int1to10(raw.fatigue_training),
    fatigue_score: int1to10(raw.fatigue_score),
    volume: raw.volume != null && raw.volume !== "" ? Number(raw.volume) : null,
    intensity: raw.intensity != null && raw.intensity !== "" ? Number(raw.intensity) : null,
    readiness_score:
      raw.readiness_score != null && raw.readiness_score !== ""
        ? Number(raw.readiness_score)
        : null,
  };
}

export function trainingHasData(t) {
  return Object.values(t).some((v) => v != null && v !== "");
}

/** Поля из формы тренера (название упражнения и т.д.) */
export function parseCoachTrainingPayload(raw = {}) {
  const t = parseTrainingPayload(raw);
  const exercise = text(raw.exercise_name);
  const coachNotes = text(raw.notes);
  return {
    ...t,
    part_main: t.part_main || exercise,
    part_warmup: t.part_warmup || coachNotes,
    duration_minutes: t.duration_minutes ?? intHr(raw.duration_minutes) ?? 60,
    start_time: t.start_time || timeVal(raw.start_time) || "09:00",
    end_time: t.end_time || timeVal(raw.end_time) || "11:00",
    volume:
      t.volume ??
      (raw.volume != null && raw.volume !== "" ? Number(raw.volume) : null),
    intensity:
      t.intensity ??
      (raw.intensity != null && raw.intensity !== "" ? Number(raw.intensity) : null),
  };
}

export function coachTrainingRequestHasData(raw = {}) {
  if (text(raw.exercise_name) || raw.duration_minutes != null) return true;
  return trainingHasData(parseTrainingPayload(raw));
}

export async function upsertCoachTraining(query, athleteId, reportDate, raw, coachId) {
  if (!coachTrainingRequestHasData(raw)) return null;
  const payload = { ...raw, ...parseCoachTrainingPayload(raw) };
  return upsertTrainingForDay(query, athleteId, reportDate, payload, coachId);
}

export const TRAINING_SELECT_BASE = `SELECT
  preparation_period, desire_to_train, wellbeing_morning,
  start_time, end_time, duration_minutes,
  part_warmup, part_main, part_cooldown,
  planned_hr_before, planned_hr_after, actual_hr_before, actual_hr_after,
  work_capacity, fatigue_training,
  volume, intensity, readiness_score, fatigue_score
FROM trainings WHERE athlete_id = $1 AND date = $2::date`;

/** @deprecated use TRAINING_SELECT_ATHLETE or TRAINING_SELECT_COACH */
export const TRAINING_SELECT = `${TRAINING_SELECT_BASE} LIMIT 1`;

export const TRAINING_SELECT_ATHLETE = `${TRAINING_SELECT_BASE} AND coach_id IS NULL LIMIT 1`;
export const TRAINING_SELECT_COACH = `${TRAINING_SELECT_BASE} AND coach_id IS NOT NULL LIMIT 1`;

export function validateAthleteTrainingRequired(t) {
  const checks = [
    ["start_time", t.start_time],
    ["end_time", t.end_time],
    ["part_warmup", t.part_warmup],
    ["part_main", t.part_main],
    ["part_cooldown", t.part_cooldown],
    ["planned_hr_before", t.planned_hr_before],
    ["planned_hr_after", t.planned_hr_after],
  ];
  return checks.filter(([, v]) => v == null || v === "").map(([k]) => k);
}

function computeDurationMinutes(start, end) {
  if (!start || !end) return null;
  const parse = (s) => {
    const [h, m] = String(s).slice(0, 5).split(":").map(Number);
    return h * 60 + (m || 0);
  };
  let mins = parse(end) - parse(start);
  if (mins <= 0) mins += 24 * 60;
  return mins > 0 ? mins : null;
}

function formatTimeField(v) {
  if (v == null) return null;
  if (v instanceof Date) return v.toTimeString().slice(0, 5);
  const s = String(v);
  return s.length >= 5 ? s.slice(0, 5) : s;
}

export function mapTrainingRow(row) {
  if (!row) return null;
  return {
    preparation_period: row.preparation_period,
    desire_to_train: row.desire_to_train,
    wellbeing_morning: row.wellbeing_morning,
    start_time: formatTimeField(row.start_time),
    end_time: formatTimeField(row.end_time),
    part_warmup: row.part_warmup,
    part_main: row.part_main,
    part_cooldown: row.part_cooldown,
    planned_hr_before: row.planned_hr_before,
    planned_hr_after: row.planned_hr_after,
    actual_hr_before: row.actual_hr_before,
    actual_hr_after: row.actual_hr_after,
    duration_minutes: row.duration_minutes,
    work_capacity: row.work_capacity,
    fatigue_training: row.fatigue_training,
    volume: row.volume != null ? Number(row.volume) : null,
    intensity: row.intensity != null ? Number(row.intensity) : null,
    readiness_score: row.readiness_score != null ? Number(row.readiness_score) : null,
    fatigue_score: row.fatigue_score != null ? Number(row.fatigue_score) : null,
  };
}

function trainingDeleteSql(coachId) {
  if (coachId) {
    return `DELETE FROM trainings WHERE athlete_id = $1 AND date = $2::date AND coach_id = $3`;
  }
  return `DELETE FROM trainings WHERE athlete_id = $1 AND date = $2::date AND coach_id IS NULL`;
}

const ATHLETE_TRAINING_UPSERT_PARAMS = (t, duration) => [
  t.start_time,
  t.end_time,
  duration,
  t.preparation_period,
  t.desire_to_train,
  t.wellbeing_morning,
  t.part_warmup,
  t.part_main,
  t.part_cooldown,
  t.planned_hr_before,
  t.planned_hr_after,
  t.actual_hr_before,
  t.actual_hr_after,
  t.work_capacity,
  t.fatigue_training,
  t.volume,
  t.intensity,
  t.readiness_score,
  t.fatigue_score,
];

export async function upsertAthleteTrainingForDay(query, athleteId, reportDate, raw) {
  const t = parseTrainingPayload(raw);
  const missing = validateAthleteTrainingRequired(t);
  if (missing.length) {
    return { error: "fill_all_fields", message: "заполните все поля", missing };
  }

  const duration =
    t.duration_minutes ?? computeDurationMinutes(t.start_time, t.end_time) ?? null;

  const { rows: existing } = await query(
    `SELECT id FROM trainings
     WHERE athlete_id = $1 AND date = $2::date AND coach_id IS NULL
     LIMIT 1`,
    [athleteId, reportDate]
  );

  let rows;
  if (existing.length) {
    ({ rows } = await query(
      `UPDATE trainings SET
         start_time = $3, end_time = $4, duration_minutes = $5,
         preparation_period = $6, desire_to_train = $7, wellbeing_morning = $8,
         part_warmup = $9, part_main = $10, part_cooldown = $11,
         planned_hr_before = $12, planned_hr_after = $13,
         actual_hr_before = $14, actual_hr_after = $15,
         work_capacity = $16, fatigue_training = $17,
         volume = $18, intensity = $19, readiness_score = $20, fatigue_score = $21,
         planned_hr = $13, actual_hr = $15
       WHERE id = $2
       RETURNING *`,
      [athleteId, existing[0].id, ...ATHLETE_TRAINING_UPSERT_PARAMS(t, duration)]
    ));
  } else {
    ({ rows } = await query(
      `INSERT INTO trainings (
         athlete_id, coach_id, date, start_time, end_time, duration_minutes,
         preparation_period, desire_to_train, wellbeing_morning,
         part_warmup, part_main, part_cooldown,
         planned_hr_before, planned_hr_after, actual_hr_before, actual_hr_after,
         work_capacity, fatigue_training, volume, intensity, readiness_score, fatigue_score,
         planned_hr, actual_hr
       ) VALUES (
         $1, NULL, $2::date, $3, $4, $5,
         $6, $7, $8,
         $9, $10, $11,
         $12, $13, $14, $15,
         $16, $17, $18, $19, $20, $21,
         $13, $15
       ) RETURNING *`,
      [athleteId, reportDate, ...ATHLETE_TRAINING_UPSERT_PARAMS(t, duration)]
    ));
  }

  return { training: mapTrainingRow(rows[0]) };
}

export async function upsertTrainingForDay(query, athleteId, reportDate, training, coachId = null) {
  const t = parseTrainingPayload(training);
  if (!trainingHasData(t)) {
    await query(trainingDeleteSql(coachId), coachId ? [athleteId, reportDate, coachId] : [athleteId, reportDate]);
    return null;
  }

  await query(trainingDeleteSql(coachId), coachId ? [athleteId, reportDate, coachId] : [athleteId, reportDate]);

  const duration =
    t.duration_minutes ?? computeDurationMinutes(t.start_time, t.end_time) ?? null;

  const { rows } = await query(
    `INSERT INTO trainings (
       athlete_id, coach_id, date, start_time, end_time, duration_minutes,
       preparation_period, desire_to_train, wellbeing_morning,
       part_warmup, part_main, part_cooldown,
       planned_hr_before, planned_hr_after, actual_hr_before, actual_hr_after,
       work_capacity, fatigue_training, volume, intensity, readiness_score, fatigue_score,
       planned_hr, actual_hr
     ) VALUES (
       $1, $2, $3::date, $4, $5, $6,
       $7, $8, $9,
       $10, $11, $12,
       $13, $14, $15, $16,
       $17, $18, $19, $20, $21, $22,
       $14, $16
     ) RETURNING *`,
    [
      athleteId,
      coachId,
      reportDate,
      t.start_time,
      t.end_time,
      duration,
      t.preparation_period,
      t.desire_to_train,
      t.wellbeing_morning,
      t.part_warmup,
      t.part_main,
      t.part_cooldown,
      t.planned_hr_before,
      t.planned_hr_after,
      t.actual_hr_before,
      t.actual_hr_after,
      t.work_capacity,
      t.fatigue_training,
      t.volume,
      t.intensity,
      t.readiness_score,
      t.fatigue_score,
    ]
  );
  return mapTrainingRow(rows[0]);
}
