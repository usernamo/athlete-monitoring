import { query } from "./db.js";

/** Дозаполнение схемы на Render, если миграции SQL не применились полностью */
export async function ensureSchema() {
  await query(`
    ALTER TABLE nutrition_logs
      ADD COLUMN IF NOT EXISTS meal_number INTEGER,
      ADD COLUMN IF NOT EXISTS is_snack BOOLEAN NOT NULL DEFAULT FALSE
  `);

  await query(`
    ALTER TABLE nutrition_items
      ADD COLUMN IF NOT EXISTS quantity_ml NUMERIC
  `);

  await query(`
    CREATE TABLE IF NOT EXISTS nutrition_daily_notes (
      athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
      note_date DATE NOT NULL,
      sports_nutrition TEXT,
      pharmacology TEXT,
      PRIMARY KEY (athlete_id, note_date)
    )
  `);

  await query(`
    ALTER TABLE training_camps
      ADD COLUMN IF NOT EXISTS goals TEXT,
      ADD COLUMN IF NOT EXISTS notes TEXT
  `);

  await query(`
    CREATE INDEX IF NOT EXISTS idx_training_camps_athlete
      ON training_camps (athlete_id, start_date DESC)
  `);

  await query(`
    CREATE TABLE IF NOT EXISTS coach_recommendations (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      coach_id UUID NOT NULL REFERENCES coach_profiles(id) ON DELETE CASCADE,
      athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
      recommendation_text TEXT NOT NULL,
      category VARCHAR(50) NOT NULL DEFAULT 'general',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    )
  `);

  await query(`
    CREATE INDEX IF NOT EXISTS idx_coach_recommendations_athlete
      ON coach_recommendations (athlete_id, created_at DESC)
  `);

  await query(`
    CREATE INDEX IF NOT EXISTS idx_coach_recommendations_coach
      ON coach_recommendations (coach_id, created_at DESC)
  `);
}
