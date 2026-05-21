-- Регистрация: возраст, окружность груди, календарь менструаций

ALTER TABLE athlete_profiles
    ADD COLUMN IF NOT EXISTS age_years INTEGER,
    ADD COLUMN IF NOT EXISTS chest_cm NUMERIC(5, 2);

CREATE TABLE IF NOT EXISTS menstrual_cycles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    cycle_start_date DATE NOT NULL,
    cycle_end_date DATE,
    cycle_length_days INTEGER,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_menstrual_athlete_start
    ON menstrual_cycles (athlete_id, cycle_start_date DESC);
