-- Календарь соревнований, сборы, медобследования (расширение схемы)

ALTER TABLE athlete_competitions
    ADD COLUMN IF NOT EXISTS event_date DATE,
    ADD COLUMN IF NOT EXISTS location VARCHAR(255);

ALTER TABLE training_camps
    ADD COLUMN IF NOT EXISTS athlete_id UUID REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS notes TEXT;

-- type: mandatory (обязательное) | extraordinary (внеочередное)
COMMENT ON COLUMN medical_records.type IS 'mandatory | extraordinary';

CREATE INDEX IF NOT EXISTS idx_athlete_competitions_athlete_date
    ON athlete_competitions (athlete_id, event_date DESC);

CREATE INDEX IF NOT EXISTS idx_training_camps_athlete
    ON training_camps (athlete_id, start_date DESC);

CREATE INDEX IF NOT EXISTS idx_medical_records_athlete_date
    ON medical_records (athlete_id, examination_date DESC);
