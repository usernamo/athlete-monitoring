import { query } from "./db.js";
import {
  mapTrainingRow,
  TRAINING_SELECT_ATHLETE,
  TRAINING_SELECT_COACH,
  upsertAthleteTrainingForDay,
} from "./trainingFields.js";

export function registerAthleteTrainingRoutes(app) {
  app.get("/api/athletes/:id/trainings/:date", async (req, res) => {
    const { id: athleteId, date } = req.params;
    try {
      const [athleteRows, coachRows] = await Promise.all([
        query(TRAINING_SELECT_ATHLETE, [athleteId, date]),
        query(TRAINING_SELECT_COACH, [athleteId, date]),
      ]);
      res.json({
        training: mapTrainingRow(athleteRows.rows[0]),
        coach_training: mapTrainingRow(coachRows.rows[0]),
      });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  async function saveAthleteTraining(req, res) {
    const { id: athleteId, date } = req.params;
    const body = req.body || {};
    try {
      const { rows: ap } = await query(
        `SELECT id FROM athlete_profiles WHERE id = $1 LIMIT 1`,
        [athleteId]
      );
      if (!ap.length) {
        return res.status(404).json({ error: "athlete not found" });
      }

      const result = await upsertAthleteTrainingForDay(query, athleteId, date, body);
      if (result?.error === "fill_all_fields") {
        return res.status(400).json(result);
      }
      console.log(`[training] saved athlete=${athleteId} date=${date}`);
      res.json({ ok: true, training: result.training, report_date: date });
    } catch (e) {
      console.error("[training] save failed:", e);
      res.status(500).json({ error: e.message });
    }
  }

  app.put("/api/athletes/:id/trainings/:date", saveAthleteTraining);
  app.post("/api/athletes/:id/trainings/:date", saveAthleteTraining);
}
