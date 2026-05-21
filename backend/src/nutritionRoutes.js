import { query } from "./db.js";

function sumItems(items) {
  return items.reduce(
    (acc, it) => ({
      grams: acc.grams + Number(it.grams || 0),
      ml: acc.ml + Number(it.quantity_ml || 0),
      protein: acc.protein + Number(it.protein || 0),
      fat: acc.fat + Number(it.fat || 0),
      carbs: acc.carbs + Number(it.carbs || 0),
      calories: acc.calories + Number(it.calories || 0),
    }),
    { grams: 0, ml: 0, protein: 0, fat: 0, carbs: 0, calories: 0 }
  );
}

export function registerNutritionRoutes(app) {
  app.get("/api/athletes/:id/nutrition-day/:date", async (req, res) => {
    const { id: athleteId, date } = req.params;
    try {
      const { rows: meals } = await query(
        `SELECT id, meal_number, meal_type, consumed_at, appetite, is_snack
         FROM nutrition_logs
         WHERE athlete_id = $1 AND consumed_at::date = $2::date
         ORDER BY meal_number NULLS LAST, consumed_at`,
        [athleteId, date]
      );

      const mealsWithItems = [];
      for (const m of meals) {
        const { rows: items } = await query(
          `SELECT id, product_name, grams, quantity_ml, protein, fat, carbs, calories
           FROM nutrition_items WHERE nutrition_log_id = $1 ORDER BY product_name`,
          [m.id]
        );
        mealsWithItems.push({ ...m, items, subtotal: sumItems(items) });
      }

      const grand = mealsWithItems.reduce(
        (acc, m) => ({
          grams: acc.grams + m.subtotal.grams,
          ml: acc.ml + m.subtotal.ml,
          protein: acc.protein + m.subtotal.protein,
          fat: acc.fat + m.subtotal.fat,
          carbs: acc.carbs + m.subtotal.carbs,
          calories: acc.calories + m.subtotal.calories,
        }),
        { grams: 0, ml: 0, protein: 0, fat: 0, carbs: 0, calories: 0 }
      );

      const { rows: notes } = await query(
        `SELECT sports_nutrition, pharmacology FROM nutrition_daily_notes
         WHERE athlete_id = $1 AND note_date = $2::date`,
        [athleteId, date]
      );

      const { rows: water } = await query(
        `SELECT COALESCE(SUM(amount_ml), 0) AS total_ml FROM water_intake
         WHERE athlete_id = $1 AND recorded_at::date = $2::date`,
        [athleteId, date]
      );

      res.json({
        date,
        meals: mealsWithItems,
        total: grand,
        sports_nutrition: notes[0]?.sports_nutrition ?? "",
        pharmacology: notes[0]?.pharmacology ?? "",
        water_ml: Number(water[0]?.total_ml ?? 0),
      });
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.post("/api/athletes/:id/nutrition-day", async (req, res) => {
    const athleteId = req.params.id;
    const body = req.body || {};
    const date = body.date || new Date().toISOString().slice(0, 10);

    try {
      if (body.replace_day) {
        await query(
          `DELETE FROM nutrition_items WHERE nutrition_log_id IN (
             SELECT id FROM nutrition_logs WHERE athlete_id = $1 AND consumed_at::date = $2::date
           )`,
          [athleteId, date]
        );
        await query(
          `DELETE FROM nutrition_logs WHERE athlete_id = $1 AND consumed_at::date = $2::date`,
          [athleteId, date]
        );
      }

      for (const meal of body.meals || []) {
        const consumedAt = meal.consumed_at || `${date}T${meal.time || "12:00"}:00`;
        const { rows } = await query(
          `INSERT INTO nutrition_logs (
             athlete_id, meal_number, meal_type, consumed_at, appetite, is_snack
           ) VALUES ($1, $2, $3, $4::timestamptz, $5, $6)
           RETURNING id`,
          [
            athleteId,
            meal.meal_number ?? null,
            meal.meal_type || (meal.is_snack ? "snack" : "meal"),
            consumedAt,
            meal.appetite ?? null,
            meal.is_snack ?? false,
          ]
        );
        const logId = rows[0].id;

        for (const item of meal.items || []) {
          await query(
            `INSERT INTO nutrition_items (
               nutrition_log_id, product_name, grams, quantity_ml,
               protein, fat, carbs, calories
             ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)`,
            [
              logId,
              item.product_name,
              item.grams ?? null,
              item.quantity_ml ?? null,
              item.protein ?? null,
              item.fat ?? null,
              item.carbs ?? null,
              item.calories ?? null,
            ]
          );
        }
      }

      await query(
        `INSERT INTO nutrition_daily_notes (athlete_id, note_date, sports_nutrition, pharmacology)
         VALUES ($1, $2::date, $3, $4)
         ON CONFLICT (athlete_id, note_date) DO UPDATE SET
           sports_nutrition = EXCLUDED.sports_nutrition,
           pharmacology = EXCLUDED.pharmacology`,
        [athleteId, date, body.sports_nutrition ?? "", body.pharmacology ?? ""]
      );

      if (body.water_ml != null && body.water_ml !== "") {
        const ml = parseInt(body.water_ml, 10);
        await query(
          `DELETE FROM water_intake WHERE athlete_id = $1 AND recorded_at::date = $2::date`,
          [athleteId, date]
        );
        if (!Number.isNaN(ml) && ml > 0) {
          await query(
            `INSERT INTO water_intake (athlete_id, amount_ml, recorded_at)
             VALUES ($1, $2, $3::timestamptz)`,
            [athleteId, ml, `${date}T20:00:00`]
          );
        }
      }

      const day = await fetchNutritionDay(athleteId, date);
      res.status(201).json(day);
    } catch (e) {
      console.error(e);
      res.status(500).json({ error: e.message });
    }
  });

  app.delete("/api/athletes/:id/nutrition-meal/:mealId", async (req, res) => {
    try {
      await query(`DELETE FROM nutrition_logs WHERE id = $1 AND athlete_id = $2`, [
        req.params.mealId,
        req.params.id,
      ]);
      res.json({ ok: true });
    } catch (e) {
      res.status(500).json({ error: e.message });
    }
  });
}

async function fetchNutritionDay(athleteId, date) {
  // reuse GET logic inline
  const { rows: meals } = await query(
    `SELECT id, meal_number, meal_type, consumed_at, appetite, is_snack
     FROM nutrition_logs WHERE athlete_id = $1 AND consumed_at::date = $2::date
     ORDER BY meal_number NULLS LAST, consumed_at`,
    [athleteId, date]
  );
  const mealsWithItems = [];
  for (const m of meals) {
    const { rows: items } = await query(
      `SELECT id, product_name, grams, quantity_ml, protein, fat, carbs, calories
       FROM nutrition_items WHERE nutrition_log_id = $1`,
      [m.id]
    );
    mealsWithItems.push({ ...m, items, subtotal: sumItems(items) });
  }
  const grand = mealsWithItems.reduce(
    (acc, m) => ({
      protein: acc.protein + m.subtotal.protein,
      fat: acc.fat + m.subtotal.fat,
      carbs: acc.carbs + m.subtotal.carbs,
      calories: acc.calories + m.subtotal.calories,
      grams: acc.grams + m.subtotal.grams,
      ml: acc.ml + m.subtotal.ml,
    }),
    { grams: 0, ml: 0, protein: 0, fat: 0, carbs: 0, calories: 0 }
  );
  return { date, meals: mealsWithItems, total: grand };
}
