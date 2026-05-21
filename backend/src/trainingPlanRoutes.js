import { query } from "./db.js";
import { upsertCoachTraining } from "./trainingFields.js";

function mapPlanItem(row) {
  return {
    id: row.id,
    scheduled_date:
      row.scheduled_date instanceof Date
        ? row.scheduled_date.toISOString().slice(0, 10)
        : String(row.scheduled_date).slice(0, 10),
    title: row.title,
    preparation_period: row.preparation_period,
    part_warmup: row.part_warmup,
    part_main: row.part_main,
    part_cooldown: row.part_cooldown,
    start_time: row.start_time,
    end_time: row.end_time,
    duration_minutes: row.duration_minutes,
    coach_notes: row.coach_notes,
    sort_order: row.sort_order,
  };
}

async function getPlanForAthlete(athleteId) {
  const { rows } = await query(
    `SELECT tp.id, tp.title, tp.notes, tp.updated_at, tp.coach_id,
            cp.specialization AS coach_name
     FROM training_plans tp
     LEFT JOIN coach_profiles cp ON cp.id = tp.coach_id
     WHERE tp.athlete_id = $1`,
    [athleteId]
  );
  if (!rows.length) return null;
  const plan = rows[0];
  const { rows: items } = await query(
    `SELECT id, scheduled_date, title, preparation_period,
            part_warmup, part_main, part_cooldown,
            start_time, end_time, duration_minutes, coach_notes, sort_order
     FROM training_plan_items
     WHERE plan_id = $1
     ORDER BY scheduled_date, sort_order`,
    [plan.id]
  );
  return {
    id: plan.id,
    title: plan.title,
    notes: plan.notes,
    updated_at: plan.updated_at,
    coach_name: plan.coach_name,
    items: items.map(mapPlanItem),
  };
}

async function syncPlanToTrainings(queryFn, athleteId, coachId, items) {
  await queryFn(`DELETE FROM trainings WHERE athlete_id = $1 AND coach_id = $2`, [
    athleteId,
    coachId,
  ]);
  for (const item of items) {
    if (!item.scheduled_date) continue;
    await upsertCoachTraining(queryFn, athleteId, item.scheduled_date, {
      date: item.scheduled_date,
      exercise_name: item.part_main || item.title || "Тренировка",
      duration_minutes: item.duration_minutes ?? 90,
      start_time: item.start_time || "09:00",
      end_time: item.end_time || "11:00",
      notes: item.coach_notes,
      part_warmup: item.part_warmup,
      part_main: item.part_main || item.title,
      part_cooldown: item.part_cooldown,
      preparation_period: item.preparation_period,
    }, coachId);
  }
}

export function registerTrainingPlanRoutes(app) {
  app.get("/api/athletes/:id/training-plan", async (req, res) => {
    try {
      const plan = await getPlanForAthlete(req.params.id);
      res.json({ plan });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.get("/api/coach/:coachId/athletes/:athleteId/training-plan", async (req, res) => {
    const { coachId, athleteId } = req.params;
    try {
      const { rows } = await query(
        `SELECT 1 FROM coach_athletes WHERE coach_id = $1 AND athlete_id = $2`,
        [coachId, athleteId]
      );
      if (!rows.length) return res.status(403).json({ error: "athlete not in your team" });
      const plan = await getPlanForAthlete(athleteId);
      res.json({ plan });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.put("/api/coach/:coachId/athletes/:athleteId/training-plan", async (req, res) => {
    const { coachId, athleteId } = req.params;
    const body = req.body || {};
    const items = Array.isArray(body.items) ? body.items : [];
    const title = (body.title || "План тренировок").trim();
    const notes = body.notes?.trim() || null;

    try {
      const { rows: link } = await query(
        `SELECT 1 FROM coach_athletes WHERE coach_id = $1 AND athlete_id = $2`,
        [coachId, athleteId]
      );
      if (!link.length) return res.status(403).json({ error: "athlete not in your team" });

      const { rows: existing } = await query(
        `SELECT id FROM training_plans WHERE athlete_id = $1`,
        [athleteId]
      );

      let planId;
      if (existing.length) {
        planId = existing[0].id;
        await query(
          `UPDATE training_plans SET title = $2, notes = $3, coach_id = $4, updated_at = NOW()
           WHERE id = $1`,
          [planId, title, notes, coachId]
        );
        await query(`DELETE FROM training_plan_items WHERE plan_id = $1`, [planId]);
      } else {
        const { rows: ins } = await query(
          `INSERT INTO training_plans (athlete_id, coach_id, title, notes)
           VALUES ($1, $2, $3, $4) RETURNING id`,
          [athleteId, coachId, title, notes]
        );
        planId = ins[0].id;
      }

      const normalized = [];
      for (let i = 0; i < items.length; i++) {
        const it = items[i];
        const date = it.scheduled_date;
        if (!date) continue;
        const partMain = it.part_main?.trim() || it.title?.trim() || null;
        if (!partMain && !it.part_warmup && !it.duration_minutes) continue;

        const { rows: itemRows } = await query(
          `INSERT INTO training_plan_items (
             plan_id, scheduled_date, title, preparation_period,
             part_warmup, part_main, part_cooldown,
             start_time, end_time, duration_minutes, coach_notes, sort_order
           ) VALUES ($1, $2::date, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
           RETURNING id, scheduled_date, title, preparation_period,
                     part_warmup, part_main, part_cooldown,
                     start_time, end_time, duration_minutes, coach_notes, sort_order`,
          [
            planId,
            date,
            it.title?.trim() || partMain,
            it.preparation_period || null,
            it.part_warmup?.trim() || null,
            partMain,
            it.part_cooldown?.trim() || null,
            it.start_time || null,
            it.end_time || null,
            it.duration_minutes ?? null,
            it.coach_notes?.trim() || null,
            it.sort_order ?? i,
          ]
        );
        normalized.push(mapPlanItem(itemRows[0]));
      }

      await syncPlanToTrainings(query, athleteId, coachId, normalized);

      const plan = await getPlanForAthlete(athleteId);
      res.json({ plan });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });
}
