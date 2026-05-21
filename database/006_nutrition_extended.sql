-- Расширение модуля питания (как в Excel-дневнике)

ALTER TABLE nutrition_logs
    ADD COLUMN IF NOT EXISTS meal_number INTEGER,
    ADD COLUMN IF NOT EXISTS is_snack BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE nutrition_items
    ADD COLUMN IF NOT EXISTS quantity_ml NUMERIC;

CREATE TABLE IF NOT EXISTS nutrition_daily_notes (
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    note_date DATE NOT NULL,
    sports_nutrition TEXT,
    pharmacology TEXT,
    PRIMARY KEY (athlete_id, note_date)
);
