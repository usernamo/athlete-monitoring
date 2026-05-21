import { query } from "./db.js";

function normalizeOutcome(raw, actualResult) {
  const o = (raw || "").toLowerCase();
  if (o === "win" || o === "loss" || o === "pending") return o;
  return actualResult ? "win" : "pending";
}

export function registerCalendarRoutes(app) {
  app.get("/api/athletes/:id/calendar-month/:yearMonth", async (req, res) => {
    const athleteId = req.params.id;
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
      const [diary, trainings, comps] = await Promise.all([
        query(
          `SELECT DISTINCT report_date::text AS d FROM daily_reports
           WHERE athlete_id = $1 AND report_date >= $2::date AND report_date < $3::date`,
          [athleteId, start, end]
        ),
        query(
          `SELECT DISTINCT date::text AS d FROM trainings
           WHERE athlete_id = $1 AND date >= $2::date AND date < $3::date`,
          [athleteId, start, end]
        ),
        query(
          `SELECT ac.id, c.name, COALESCE(ac.event_date, c.start_date)::text AS event_date,
                  ac.planned_result, ac.actual_result, ac.outcome, ac.location, ac.notes
           FROM athlete_competitions ac
           JOIN competitions c ON c.id = ac.competition_id
           WHERE ac.athlete_id = $1
             AND COALESCE(ac.event_date, c.start_date) >= $2::date
             AND COALESCE(ac.event_date, c.start_date) < $3::date`,
          [athleteId, start, end]
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
          location: r.location,
          planned_result: r.planned_result,
          actual_result: r.actual_result,
          notes: r.notes,
          outcome: normalizeOutcome(r.outcome, r.actual_result),
        })),
      });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  // ——— Соревнования ———
  app.get("/api/athletes/:id/competitions", async (req, res) => {
    try {
      const { rows } = await query(
        `SELECT ac.id, c.name, COALESCE(ac.event_date, c.start_date) AS event_date,
                COALESCE(ac.location, c.location) AS location,
                ac.planned_result, ac.actual_result, ac.notes, ac.outcome
         FROM athlete_competitions ac
         JOIN competitions c ON c.id = ac.competition_id
         WHERE ac.athlete_id = $1
         ORDER BY COALESCE(ac.event_date, c.start_date) DESC NULLS LAST`,
        [req.params.id]
      );
      res.json(rows);
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.post("/api/athletes/:id/competitions", async (req, res) => {
    const { name, event_date, location, planned_result, actual_result, notes, outcome } = req.body || {};
    if (!name || !event_date) {
      return res.status(400).json({ error: "name and event_date required" });
    }
    try {
      const { rows: comp } = await query(
        `INSERT INTO competitions (name, location, start_date, end_date)
         VALUES ($1, $2, $3::date, $3::date) RETURNING id`,
        [name, location, event_date]
      );
      const { rows } = await query(
        `INSERT INTO athlete_competitions (
           athlete_id, competition_id, event_date, location,
           planned_result, actual_result, notes, outcome
         ) VALUES ($1, $2, $3::date, $4, $5, $6, $7, $8)
         RETURNING id`,
        [
          req.params.id,
          comp[0].id,
          event_date,
          location,
          planned_result,
          actual_result,
          notes,
          normalizeOutcome(outcome, actual_result),
        ]
      );
      res.status(201).json({
        id: rows[0].id,
        name,
        event_date,
        location,
        planned_result,
        actual_result,
        notes,
        outcome: normalizeOutcome(outcome, actual_result),
      });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.put("/api/athletes/:id/competitions/:entryId", async (req, res) => {
    const b = req.body || {};
    try {
      await query(
        `UPDATE competitions c SET
           name = COALESCE($1, c.name),
           location = COALESCE($2, c.location),
           start_date = COALESCE($3::date, c.start_date),
           end_date = COALESCE($3::date, c.end_date)
         FROM athlete_competitions ac
         WHERE ac.competition_id = c.id AND ac.id = $4 AND ac.athlete_id = $5`,
        [b.name, b.location, b.event_date, req.params.entryId, req.params.id]
      );
      await query(
        `UPDATE athlete_competitions SET
           event_date = COALESCE($1::date, event_date),
           location = COALESCE($2, location),
           planned_result = COALESCE($3, planned_result),
           actual_result = COALESCE($4, actual_result),
           notes = COALESCE($5, notes),
           outcome = COALESCE($6, outcome)
         WHERE id = $7 AND athlete_id = $8`,
        [
          b.event_date,
          b.location,
          b.planned_result,
          b.actual_result,
          b.notes,
          b.outcome,
          req.params.entryId,
          req.params.id,
        ]
      );
      res.json({ ok: true });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.delete("/api/athletes/:id/competitions/:entryId", async (req, res) => {
    try {
      const { rows } = await query(
        `DELETE FROM athlete_competitions WHERE id = $1 AND athlete_id = $2 RETURNING competition_id`,
        [req.params.entryId, req.params.id]
      );
      if (rows[0]) {
        await query(`DELETE FROM competitions WHERE id = $1`, [rows[0].competition_id]);
      }
      res.json({ ok: true });
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  // ——— Учебно-тренировочные сборы ———
  app.get("/api/athletes/:id/training-camps", async (req, res) => {
    try {
      const { rows } = await query(
        `SELECT id, location, start_date, end_date, goals, notes
         FROM training_camps WHERE athlete_id = $1
         ORDER BY start_date DESC NULLS LAST`,
        [req.params.id]
      );
      res.json(rows);
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  app.post("/api/athletes/:id/training-camps", async (req, res) => {
    const { location, start_date, end_date, goals, notes } = req.body || {};
    try {
      const { rows } = await query(
        `INSERT INTO training_camps (athlete_id, location, start_date, end_date, goals, notes)
         VALUES ($1, $2, $3::date, $4::date, $5, $6) RETURNING *`,
        [req.params.id, location, start_date, end_date, goals, notes]
      );
      res.status(201).json(rows[0]);
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  app.put("/api/athletes/:id/training-camps/:campId", async (req, res) => {
    const b = req.body || {};
    try {
      const { rows } = await query(
        `UPDATE training_camps SET
           location = COALESCE($1, location),
           start_date = COALESCE($2::date, start_date),
           end_date = COALESCE($3::date, end_date),
           goals = COALESCE($4, goals),
           notes = COALESCE($5, notes)
         WHERE id = $6 AND athlete_id = $7 RETURNING *`,
        [b.location, b.start_date, b.end_date, b.goals, b.notes, req.params.campId, req.params.id]
      );
      res.json(rows[0] || {});
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  app.delete("/api/athletes/:id/training-camps/:campId", async (req, res) => {
    try {
      await query(`DELETE FROM training_camps WHERE id = $1 AND athlete_id = $2`, [
        req.params.campId,
        req.params.id,
      ]);
      res.json({ ok: true });
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  // ——— Медобследования ———
  app.get("/api/athletes/:id/medical-exams", async (req, res) => {
    try {
      const { rows } = await query(
        `SELECT id, type, examination_date, institution, methods, recommendations
         FROM medical_records WHERE athlete_id = $1
         ORDER BY examination_date DESC NULLS LAST`,
        [req.params.id]
      );
      res.json(rows);
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  app.post("/api/athletes/:id/medical-exams", async (req, res) => {
    const { type, examination_date, institution, methods, recommendations } = req.body || {};
    if (!examination_date) {
      return res.status(400).json({ error: "examination_date required" });
    }
    try {
      const { rows } = await query(
        `INSERT INTO medical_records (athlete_id, type, examination_date, institution, methods, recommendations)
         VALUES ($1, $2, $3::date, $4, $5, $6) RETURNING *`,
        [req.params.id, type || "mandatory", examination_date, institution, methods, recommendations]
      );
      res.status(201).json(rows[0]);
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  app.put("/api/athletes/:id/medical-exams/:examId", async (req, res) => {
    const b = req.body || {};
    try {
      const { rows } = await query(
        `UPDATE medical_records SET
           type = COALESCE($1, type),
           examination_date = COALESCE($2::date, examination_date),
           institution = COALESCE($3, institution),
           methods = COALESCE($4, methods),
           recommendations = COALESCE($5, recommendations)
         WHERE id = $6 AND athlete_id = $7 RETURNING *`,
        [b.type, b.examination_date, b.institution, b.methods, b.recommendations, req.params.examId, req.params.id]
      );
      res.json(rows[0] || {});
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  app.delete("/api/athletes/:id/medical-exams/:examId", async (req, res) => {
    try {
      await query(`DELETE FROM medical_records WHERE id = $1 AND athlete_id = $2`, [
        req.params.examId,
        req.params.id,
      ]);
      res.json({ ok: true });
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });
}
