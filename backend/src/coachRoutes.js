import { query } from "./db.js";
import {
  mapTrainingRow,
  upsertCoachTraining,
  TRAINING_SELECT_ATHLETE,
  TRAINING_SELECT_COACH,
} from "./trainingFields.js";

function normalizeOutcome(raw, actualResult) {
  const o = (raw || "").toLowerCase();
  if (o === "win" || o === "loss" || o === "pending") return o;
  return actualResult ? "win" : "pending";
}

async function getCoachProfile(coachId) {
  const { rows } = await query(
    `SELECT id, user_id, specialization, experience_years FROM coach_profiles WHERE id = $1`,
    [coachId]
  );
  return rows[0] || null;
}

async function coachHasAthlete(coachId, athleteId) {
  const { rows } = await query(
    `SELECT 1 FROM coach_athletes WHERE coach_id = $1 AND athlete_id = $2`,
    [coachId, athleteId]
  );
  return rows.length > 0;
}

function mapAthlete(row, assigned = false) {
  const fullName =
    (row.full_name && String(row.full_name).trim()) ||
    `${row.first_name} ${row.last_name}`.trim();
  return {
    id: row.id,
    first_name: row.first_name,
    last_name: row.last_name,
    full_name: fullName,
    sport: row.sport,
    age_years: row.age_years,
    gender: row.gender,
    assigned,
  };
}

async function teamAthleteIds(coachId) {
  const { rows } = await query(
    `SELECT athlete_id FROM coach_athletes WHERE coach_id = $1`,
    [coachId]
  );
  return rows.map((r) => r.athlete_id);
}

export function registerCoachRoutes(app) {
  app.get("/api/coach/by-user/:userId", async (req, res) => {
    try {
      const { rows } = await query(
        `SELECT cp.id, cp.specialization, cp.experience_years, u.email
         FROM coach_profiles cp
         JOIN users u ON u.id = cp.user_id
         WHERE cp.user_id = $1`,
        [req.params.userId]
      );
      if (!rows.length) return res.status(404).json({ error: "coach profile not found" });
      res.json({ coach: rows[0] });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.get("/api/coach/:coachId/athletes/all", async (req, res) => {
    const coachId = req.params.coachId;
    try {
      if (!(await getCoachProfile(coachId))) {
        return res.status(404).json({ error: "coach not found" });
      }
      const { rows } = await query(
        `SELECT ap.id, ap.first_name, ap.last_name, ap.full_name, ap.sport, ap.age_years, ap.gender,
                (ca.athlete_id IS NOT NULL) AS assigned
         FROM athlete_profiles ap
         LEFT JOIN coach_athletes ca ON ca.athlete_id = ap.id AND ca.coach_id = $1
         ORDER BY ap.last_name, ap.first_name`,
        [coachId]
      );
      res.json({
        athletes: rows.map((r) =>
          mapAthlete(r, r.assigned === true || r.assigned === "t")
        ),
      });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.get("/api/coach/:coachId/athletes", async (req, res) => {
    const coachId = req.params.coachId;
    try {
      const { rows } = await query(
        `SELECT ap.id, ap.first_name, ap.last_name, ap.full_name, ap.sport, ap.age_years, ap.gender
         FROM athlete_profiles ap
         JOIN coach_athletes ca ON ca.athlete_id = ap.id AND ca.coach_id = $1
         ORDER BY ap.last_name, ap.first_name`,
        [coachId]
      );
      res.json({ athletes: rows.map((r) => mapAthlete(r, true)) });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.put("/api/coach/:coachId/athletes", async (req, res) => {
    const coachId = req.params.coachId;
    const ids = Array.isArray(req.body?.athlete_ids) ? req.body.athlete_ids : [];
    try {
      if (!(await getCoachProfile(coachId))) {
        return res.status(404).json({ error: "coach not found" });
      }
      await query(`DELETE FROM coach_athletes WHERE coach_id = $1`, [coachId]);
      for (const athleteId of ids) {
        await query(
          `INSERT INTO coach_athletes (coach_id, athlete_id) VALUES ($1, $2)
           ON CONFLICT (coach_id, athlete_id) DO NOTHING`,
          [coachId, athleteId]
        );
      }
      const { rows } = await query(
        `SELECT ap.id, ap.first_name, ap.last_name, ap.full_name, ap.sport, ap.age_years, ap.gender
         FROM athlete_profiles ap
         JOIN coach_athletes ca ON ca.athlete_id = ap.id AND ca.coach_id = $1
         ORDER BY ap.last_name`,
        [coachId]
      );
      res.json({ athletes: rows.map((r) => mapAthlete(r, true)) });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.post("/api/coach/:coachId/athletes/:athleteId", async (req, res) => {
    const { coachId, athleteId } = req.params;
    try {
      await query(
        `INSERT INTO coach_athletes (coach_id, athlete_id) VALUES ($1, $2)
         ON CONFLICT (coach_id, athlete_id) DO NOTHING`,
        [coachId, athleteId]
      );
      res.status(201).json({ ok: true });
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  app.delete("/api/coach/:coachId/athletes/:athleteId", async (req, res) => {
    try {
      await query(`DELETE FROM coach_athletes WHERE coach_id = $1 AND athlete_id = $2`, [
        req.params.coachId,
        req.params.athleteId,
      ]);
      res.json({ ok: true });
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  app.get("/api/coach/:coachId/overview", async (req, res) => {
    const coachId = req.params.coachId;
    try {
      const { rows: athletes } = await query(
        `SELECT ap.id, ap.first_name, ap.last_name, ap.full_name, ap.sport, ap.age_years, ap.gender
         FROM athlete_profiles ap
         JOIN coach_athletes ca ON ca.athlete_id = ap.id AND ca.coach_id = $1
         ORDER BY ap.last_name`,
        [coachId]
      );

      const overview = [];
      for (const a of athletes) {
        const [analytics, training, recs] = await Promise.all([
          query(
            `SELECT readiness_score, recovery_score, fatigue_score, injury_risk_score
             FROM analytics_snapshots WHERE athlete_id = $1 ORDER BY generated_at DESC LIMIT 1`,
            [a.id]
          ),
          query(
            `SELECT id, date, duration_minutes, readiness_score, fatigue_score
             FROM trainings WHERE athlete_id = $1 ORDER BY date DESC LIMIT 3`,
            [a.id]
          ),
          query(
            `SELECT id, recommendation_text, category, created_at
             FROM coach_recommendations WHERE athlete_id = $1 AND coach_id = $2
             ORDER BY created_at DESC LIMIT 3`,
            [a.id, coachId]
          ),
        ]);
        overview.push({
          athlete: mapAthlete(a, true),
          analytics: analytics.rows[0] || null,
          recent_trainings: training.rows,
          recent_recommendations: recs.rows,
        });
      }
      res.json({ overview });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.get("/api/athletes/:athleteId/recommendations", async (req, res) => {
    try {
      const { rows } = await query(
        `SELECT cr.id, cr.recommendation_text, cr.category, cr.created_at,
                cp.specialization AS coach_name
         FROM coach_recommendations cr
         JOIN coach_profiles cp ON cp.id = cr.coach_id
         WHERE cr.athlete_id = $1
         ORDER BY cr.created_at DESC`,
        [req.params.athleteId]
      );
      res.json(rows);
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  app.post("/api/coach/:coachId/athletes/:athleteId/recommendations", async (req, res) => {
    const { coachId, athleteId } = req.params;
    const text = (req.body?.recommendation_text || req.body?.text || "").trim();
    if (!text) return res.status(400).json({ error: "recommendation_text required" });

    try {
      if (!(await coachHasAthlete(coachId, athleteId))) {
        return res.status(403).json({ error: "athlete not in your team" });
      }
      const { rows } = await query(
        `INSERT INTO coach_recommendations (coach_id, athlete_id, recommendation_text, category)
         VALUES ($1, $2, $3, $4) RETURNING id, recommendation_text, category, created_at`,
        [coachId, athleteId, text, req.body?.category || "general"]
      );
      res.status(201).json(rows[0]);
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.post("/api/coach/:coachId/athletes/:athleteId/trainings", async (req, res) => {
    const { coachId, athleteId } = req.params;
    const b = req.body || {};
    if (!b.date) return res.status(400).json({ error: "date required" });
    if (!b.exercise_name?.trim() && b.duration_minutes == null) {
      return res.status(400).json({ error: "exercise_name or duration_minutes required" });
    }

    try {
      if (!(await coachHasAthlete(coachId, athleteId))) {
        return res.status(403).json({ error: "athlete not in your team" });
      }
      const training = await upsertCoachTraining(query, athleteId, b.date, b, coachId);
      if (!training) {
        return res.status(400).json({ error: "could not save training" });
      }
      res.status(201).json(training);
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.get("/api/coach/:coachId/calendar-month/:yearMonth", async (req, res) => {
    const coachId = req.params.coachId;
    const ym = req.params.yearMonth;
    const m = /^(\d{4})-(\d{2})$/.exec(ym);
    if (!m) return res.status(400).json({ error: "yearMonth format YYYY-MM required" });
    const year = Number(m[1]);
    const month = Number(m[2]);
    const start = `${year}-${String(month).padStart(2, "0")}-01`;
    const endMonth = month === 12 ? 1 : month + 1;
    const endYear = month === 12 ? year + 1 : year;
    const end = `${endYear}-${String(endMonth).padStart(2, "0")}-01`;

    try {
      const ids = await teamAthleteIds(coachId);
      if (!ids.length) {
        return res.json({
          year_month: ym,
          diary_dates: [],
          training_dates: [],
          competitions: [],
        });
      }

      const [diary, trainings, comps] = await Promise.all([
        query(
          `SELECT DISTINCT report_date::text AS d FROM daily_reports
           WHERE athlete_id = ANY($1::uuid[]) AND report_date >= $2::date AND report_date < $3::date`,
          [ids, start, end]
        ),
        query(
          `SELECT DISTINCT date::text AS d FROM trainings
           WHERE athlete_id = ANY($1::uuid[]) AND date >= $2::date AND date < $3::date`,
          [ids, start, end]
        ),
        query(
          `SELECT ac.id, c.name, ap.id AS athlete_id, ap.first_name, ap.last_name, ap.full_name,
                  COALESCE(ac.event_date, c.start_date)::text AS event_date,
                  ac.outcome, ac.actual_result
           FROM athlete_competitions ac
           JOIN competitions c ON c.id = ac.competition_id
           JOIN athlete_profiles ap ON ap.id = ac.athlete_id
           WHERE ac.athlete_id = ANY($1::uuid[])
             AND COALESCE(ac.event_date, c.start_date) >= $2::date
             AND COALESCE(ac.event_date, c.start_date) < $3::date`,
          [ids, start, end]
        ),
      ]);

      res.json({
        year_month: ym,
        diary_dates: diary.rows.map((r) => r.d.slice(0, 10)),
        training_dates: trainings.rows.map((r) => r.d.slice(0, 10)),
        competitions: comps.rows.map((r) => ({
          id: r.id,
          name: r.name,
          event_date: r.event_date?.slice(0, 10),
          athlete_id: r.athlete_id,
          athlete_name:
            (r.full_name && String(r.full_name).trim()) ||
            `${r.first_name} ${r.last_name}`.trim(),
          outcome: normalizeOutcome(r.outcome, r.actual_result),
        })),
      });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.get("/api/coach/:coachId/day/:date", async (req, res) => {
    const coachId = req.params.coachId;
    const date = req.params.date;

    try {
      const { rows: athletes } = await query(
        `SELECT ap.id, ap.first_name, ap.last_name, ap.full_name, ap.sport, ap.age_years, ap.gender
         FROM athlete_profiles ap
         JOIN coach_athletes ca ON ca.athlete_id = ap.id AND ca.coach_id = $1
         ORDER BY ap.last_name, ap.first_name`,
        [coachId]
      );

      const list = [];
      for (const a of athletes) {
        const [diary, training, comp, nutrition] = await Promise.all([
          query(
            `SELECT 1 FROM daily_reports WHERE athlete_id = $1 AND report_date = $2::date LIMIT 1`,
            [a.id, date]
          ),
          query(`SELECT 1 FROM trainings WHERE athlete_id = $1 AND date = $2::date LIMIT 1`, [
            a.id,
            date,
          ]),
          query(
            `SELECT ac.outcome, ac.actual_result, c.name
             FROM athlete_competitions ac
             JOIN competitions c ON c.id = ac.competition_id
             WHERE ac.athlete_id = $1 AND COALESCE(ac.event_date, c.start_date) = $2::date
             LIMIT 1`,
            [a.id, date]
          ),
          query(
            `SELECT COUNT(*)::int AS n FROM nutrition_logs
             WHERE athlete_id = $1 AND consumed_at::date = $2::date`,
            [a.id, date]
          ),
        ]);

        const compRow = comp.rows[0];
        list.push({
          athlete: mapAthlete(a, true),
          has_diary: diary.rows.length > 0,
          has_training: training.rows.length > 0,
          has_nutrition: Number(nutrition.rows[0]?.n ?? 0) > 0,
          competition: compRow
            ? {
                name: compRow.name,
                outcome: normalizeOutcome(compRow.outcome, compRow.actual_result),
              }
            : null,
        });
      }

      res.json({ date, athletes: list });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.get("/api/coach/:coachId/athletes/:athleteId/day/:date", async (req, res) => {
    const { coachId, athleteId, date } = req.params;

    try {
      if (!(await coachHasAthlete(coachId, athleteId))) {
        return res.status(403).json({ error: "athlete not in your team" });
      }

      const { rows: ap } = await query(
        `SELECT id, first_name, last_name, full_name, sport, age_years, gender
         FROM athlete_profiles WHERE id = $1`,
        [athleteId]
      );
      if (!ap.length) return res.status(404).json({ error: "athlete not found" });

      const [daily, metrics, water, athleteTraining, coachTraining, comp, analytics, nutritionCount] =
        await Promise.all([
          query(
            `SELECT report_date, sleep_time, wake_time, notes, bedtime,
                    daily_activity, wellbeing_evening, fatigue_daily
             FROM daily_reports WHERE athlete_id = $1 AND report_date = $2::date`,
            [athleteId, date]
          ),
          query(
            `SELECT mt.name, am.value FROM athlete_metrics am
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
          query(
            `SELECT ac.id, c.name, ac.location, ac.planned_result, ac.actual_result,
                    ac.notes, ac.outcome
             FROM athlete_competitions ac
             JOIN competitions c ON c.id = ac.competition_id
             WHERE ac.athlete_id = $1 AND COALESCE(ac.event_date, c.start_date) = $2::date
             LIMIT 1`,
            [athleteId, date]
          ),
          query(
            `SELECT readiness_score, recovery_score, fatigue_score, injury_risk_score, generated_at
             FROM analytics_snapshots
             WHERE athlete_id = $1 AND generated_at::date <= $2::date
             ORDER BY generated_at DESC LIMIT 1`,
            [athleteId, date]
          ),
          query(
            `SELECT COUNT(*)::int AS meals FROM nutrition_logs
             WHERE athlete_id = $1 AND consumed_at::date = $2::date`,
            [athleteId, date]
          ),
        ]);

      const d = daily.rows[0];
      const compRow = comp.rows[0];

      res.json({
        athlete: mapAthlete(ap[0], true),
        date,
        diary: d
          ? {
              sleep_time: d.sleep_time,
              wake_time: d.wake_time,
              bedtime: d.bedtime,
              notes: d.notes,
              daily_activity: d.daily_activity,
              wellbeing_evening: d.wellbeing_evening,
              fatigue_daily: d.fatigue_daily,
              water_ml: Number(water.rows[0]?.total ?? 0),
              metrics: Object.fromEntries(metrics.rows.map((m) => [m.name, Number(m.value)])),
              training:
                mapTrainingRow(athleteTraining.rows[0]) ??
                mapTrainingRow(coachTraining.rows[0]),
              athlete_training: mapTrainingRow(athleteTraining.rows[0]),
              coach_training: mapTrainingRow(coachTraining.rows[0]),
            }
          : null,
        nutrition_meals: Number(nutritionCount.rows[0]?.meals ?? 0),
        competition: compRow
          ? {
              id: compRow.id,
              name: compRow.name,
              event_date: date,
              location: compRow.location,
              planned_result: compRow.planned_result,
              actual_result: compRow.actual_result,
              notes: compRow.notes,
              outcome: normalizeOutcome(compRow.outcome, compRow.actual_result),
            }
          : null,
        analytics: analytics.rows[0] || null,
      });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.post("/api/coach/:coachId/athletes/:athleteId/competitions", async (req, res) => {
    const { coachId, athleteId } = req.params;
    const { name, event_date, location, planned_result, actual_result, notes } = req.body || {};
    if (!name || !event_date) {
      return res.status(400).json({ error: "name and event_date required" });
    }
    try {
      if (!(await coachHasAthlete(coachId, athleteId))) {
        return res.status(403).json({ error: "athlete not in your team" });
      }
      const { rows: comp } = await query(
        `INSERT INTO competitions (name, location, start_date, end_date)
         VALUES ($1, $2, $3::date, $3::date) RETURNING id`,
        [name, location, event_date]
      );
      const { rows } = await query(
        `INSERT INTO athlete_competitions (
           athlete_id, competition_id, event_date, location,
           planned_result, actual_result, notes
         ) VALUES ($1, $2, $3::date, $4, $5, $6, $7) RETURNING id`,
        [athleteId, comp[0].id, event_date, location, planned_result, actual_result, notes]
      );
      res.status(201).json({
        id: rows[0].id,
        name,
        event_date,
        location,
        planned_result,
        actual_result,
        notes,
      });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });
}
