-- Расширенный профиль спортсмена

ALTER TABLE athlete_profiles
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS residence_address TEXT,
    ADD COLUMN IF NOT EXISTS phone VARCHAR(50),
    ADD COLUMN IF NOT EXISTS coach_full_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS coach_phone VARCHAR(50),
    ADD COLUMN IF NOT EXISTS institution VARCHAR(255);

UPDATE athlete_profiles
SET full_name = TRIM(first_name || ' ' || last_name)
WHERE full_name IS NULL OR TRIM(full_name) = '';
