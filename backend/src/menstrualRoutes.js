import { query } from "./db.js";

async function assertFemaleAthlete(athleteId) {
  const { rows } = await query(
    `SELECT gender FROM athlete_profiles WHERE id = $1`,
    [athleteId]
  );
  if (!rows.length || rows[0].gender !== "female") {
    return false;
  }
  return true;
}

export function registerMenstrualRoutes(app) {
  app.get("/api/athletes/:id/menstrual-cycles", async (req, res) => {
    if (!(await assertFemaleAthlete(req.params.id))) {
      return res.status(403).json({ error: "only for female athletes" });
    }
    try {
      const { rows } = await query(
        `SELECT id, cycle_start_date, cycle_end_date, cycle_length_days, notes
         FROM menstrual_cycles WHERE athlete_id = $1
         ORDER BY cycle_start_date DESC`,
        [req.params.id]
      );
      res.json(rows);
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  app.post("/api/athletes/:id/menstrual-cycles", async (req, res) => {
    if (!(await assertFemaleAthlete(req.params.id))) {
      return res.status(403).json({ error: "only for female athletes" });
    }
    const { cycle_start_date, cycle_end_date, cycle_length_days, notes } = req.body || {};
    if (!cycle_start_date) {
      return res.status(400).json({ error: "cycle_start_date required" });
    }
    try {
      const { rows } = await query(
        `INSERT INTO menstrual_cycles (
           athlete_id, cycle_start_date, cycle_end_date, cycle_length_days, notes
         ) VALUES ($1, $2::date, $3::date, $4, $5)
         RETURNING *`,
        [req.params.id, cycle_start_date, cycle_end_date, cycle_length_days, notes]
      );
      res.status(201).json(rows[0]);
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });

  app.delete("/api/athletes/:id/menstrual-cycles/:cycleId", async (req, res) => {
    try {
      await query(
        `DELETE FROM menstrual_cycles WHERE id = $1 AND athlete_id = $2`,
        [req.params.cycleId, req.params.id]
      );
      res.json({ ok: true });
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });
}
