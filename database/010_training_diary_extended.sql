-- Расширенный дневник: этап подготовки, самочувствие, содержание тренировки, ЧСС, вечерние показатели

ALTER TABLE trainings
    ADD COLUMN IF NOT EXISTS preparation_period VARCHAR(30),
    ADD COLUMN IF NOT EXISTS desire_to_train SMALLINT,
    ADD COLUMN IF NOT EXISTS wellbeing_morning SMALLINT,
    ADD COLUMN IF NOT EXISTS part_warmup TEXT,
    ADD COLUMN IF NOT EXISTS part_main TEXT,
    ADD COLUMN IF NOT EXISTS part_cooldown TEXT,
    ADD COLUMN IF NOT EXISTS planned_hr_before INTEGER,
    ADD COLUMN IF NOT EXISTS planned_hr_after INTEGER,
    ADD COLUMN IF NOT EXISTS actual_hr_before INTEGER,
    ADD COLUMN IF NOT EXISTS actual_hr_after INTEGER,
    ADD COLUMN IF NOT EXISTS work_capacity SMALLINT,
    ADD COLUMN IF NOT EXISTS fatigue_training SMALLINT;

ALTER TABLE daily_reports
    ADD COLUMN IF NOT EXISTS daily_activity SMALLINT,
    ADD COLUMN IF NOT EXISTS wellbeing_evening SMALLINT,
    ADD COLUMN IF NOT EXISTS fatigue_daily SMALLINT,
    ADD COLUMN IF NOT EXISTS bedtime TIME;

COMMENT ON COLUMN trainings.preparation_period IS 'general|special|pre_competition|competition|transitional';
