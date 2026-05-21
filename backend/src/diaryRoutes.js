import { query } from "./db.js";
import { DIARY_SECTIONS, EXCEL_COLUMN_ALIASES, flattenDiaryFields } from "./excelForm.js";
import {
  mapTrainingRow,
  trainingHasData,
  parseTrainingPayload,
  upsertTrainingForDay,
  TRAINING_SELECT_ATHLETE,
  TRAINING_SELECT_COACH,
} from "./trainingFields.js";

async function loadMetricTypesMap() {
  const { rows } = await query(
    `SELECT id, name, unit, min_value, max_value FROM metric_types`
  );
  return Object.fromEntries(rows.map((r) => [r.name, r]));
}

function int1to10(v) {
  if (v == null || v === "") return null;
  const n = parseInt(v, 10);
  if (Number.isNaN(n) || n < 1 || n > 10) return null;
  return n;
}

export function registerDiaryRoutes(app) {
  app.get("/api/diary/form-schema", (_req, res) => {
    res.json({ sections: DIARY_SECTIONS, excelAliases: EXCEL_COLUMN_ALIASES });
  });

  app.get("/api/athletes/:id/diary/:date", async (req, res) => {
    const athleteId = req.params.id;
    const date = req.params.date;
    try {
      const [daily, metrics, water, athleteTraining, coachTraining] = await Promise.all([
        query(
          `SELECT report_date, sleep_time, wake_time, notes,
                  daily_activity, wellbeing_evening, fatigue_daily, bedtime
           FROM daily_reports
           WHERE athlete_id = $1 AND report_date = $2::date`,
          [athleteId, date]
        ),
        query(
          `SELECT mt.name, am.value, am.notes
           FROM athlete_metrics am
           JOIN metric_types mt ON mt.id = am.metric_type_id
           WHERE am.athlete_id = $1 AND am.measured_at::date = $2::date`,
          [athleteId, date]
        ),
        query(
          `SELECT COALESCE(SUM(amount_ml), 0) AS total FROM water_intake
           WHERE athlete_id = $1 AND recorded_at::date = $2::date`,
          [athleteId, date]
        ),
        query(TRAINING_SELECT_ATHLETE, [athleteId, date]),
        query(TRAINING_SELECT_COACH, [athleteId, date]),
      ]);

      const d = daily.rows[0];
      const metricsMap = Object.fromEntries(metrics.rows.map((m) => [m.name, m.value]));
      res.json({
        report_date: date,
        sleep_time: d?.sleep_time ?? null,
        wake_time: d?.wake_time ?? null,
        notes: d?.notes ?? null,
        bedtime: d?.bedtime ?? null,
        daily_activity: d?.daily_activity ?? null,
        wellbeing_evening: d?.wellbeing_evening ?? null,
        fatigue_daily: d?.fatigue_daily ?? null,
        water_ml: Number(water.rows[0]?.total ?? 0),
        metrics: metricsMap,
        training: mapTrainingRow(athleteTraining.rows[0]),
        coach_training: mapTrainingRow(coachTraining.rows[0]),
      });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.get("/api/athletes/:id/diary-history", async (req, res) => {
    const athleteId = req.params.id;
    const days = Math.min(Number(req.query.days) || 30, 365);
    try {
      const { rows: dates } = await query(
        `SELECT DISTINCT d::date AS report_date FROM (
           SELECT measured_at::date AS d FROM athlete_metrics WHERE athlete_id = $1
           UNION SELECT report_date FROM daily_reports WHERE athlete_id = $1
           UNION SELECT date FROM trainings WHERE athlete_id = $1
         ) t ORDER BY report_date DESC LIMIT $2`,
        [athleteId, days]
      );

      const history = [];
      for (const { report_date } of dates.rows) {
        const day = report_date.toISOString().slice(0, 10);
        const { rows: m } = await query(
          `SELECT mt.name, am.value FROM athlete_metrics am
           JOIN metric_types mt ON mt.id = am.metric_type_id
           WHERE am.athlete_id = $1 AND am.measured_at::date = $2::date`,
          [athleteId, day]
        );
        const metrics = Object.fromEntries(m.map((x) => [x.name, Number(x.value)]));
        history.push({ report_date: day, metrics });
      }
      res.json({ days: history.length, history });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.post("/api/athletes/:id/diary-entry", async (req, res) => {
    const athleteId = req.params.id;
    const body = req.body || {};
    const reportDate = body.report_date || new Date().toISOString().slice(0, 10);
    const measuredAt = `${reportDate}T12:00:00.000Z`;

    try {
      const typeMap = await loadMetricTypesMap();

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
          reportDate,
          body.sleep_time,
          body.wake_time,
          body.notes,
          int1to10(body.daily_activity),
          int1to10(body.wellbeing_evening),
          int1to10(body.fatigue_daily),
          body.bedtime || null,
        ]
      );

      const metrics = body.metrics || {};
      for (const [name, raw] of Object.entries(metrics)) {
        if (raw == null || raw === "") continue;
        const mt = typeMap[name];
        if (!mt) continue;
        const value = Number(raw);
        if (Number.isNaN(value)) continue;

        await query(
          `DELETE FROM athlete_metrics
           WHERE athlete_id = $1 AND metric_type_id = $2 AND measured_at::date = $3::date`,
          [athleteId, mt.id, reportDate]
        );
        await query(
          `INSERT INTO athlete_metrics (athlete_id, metric_type_id, value, measured_at)
           VALUES ($1, $2, $3, $4::timestamptz)`,
          [athleteId, mt.id, value, measuredAt]
        );
      }

      if (body.water_ml != null && body.water_ml !== "") {
        const ml = parseInt(body.water_ml, 10);
        if (!Number.isNaN(ml) && ml > 0) {
          await query(
            `DELETE FROM water_intake WHERE athlete_id = $1 AND recorded_at::date = $2::date`,
            [athleteId, reportDate]
          );
          await query(
            `INSERT INTO water_intake (athlete_id, amount_ml, recorded_at)
             VALUES ($1, $2, $3::timestamptz)`,
            [athleteId, ml, measuredAt]
          );
        }
      }

      // Тренировка спортсмена сохраняется отдельным API; не трогаем её при сохранении дневника
      if (body.training != null && trainingHasData(parseTrainingPayload(body.training))) {
        await upsertTrainingForDay(query, athleteId, reportDate, body.training);
      }

      res.status(201).json({ ok: true, report_date: reportDate });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.get("/api/athletes/:id/diary-export", async (req, res) => {
    res.json({
      message: "Для Excel-файла запустите: python scripts/export_excel.py",
      script: "scripts/export_excel.py",
      athleteId: req.params.id,
    });
  });
}
