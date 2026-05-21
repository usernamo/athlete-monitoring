-- План тренировок от тренера для спортсмена

CREATE TABLE IF NOT EXISTS training_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    coach_id UUID NOT NULL REFERENCES coach_profiles(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL DEFAULT 'План тренировок',
    notes TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (athlete_id)
);

CREATE TABLE IF NOT EXISTS training_plan_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL REFERENCES training_plans(id) ON DELETE CASCADE,
    scheduled_date DATE NOT NULL,
    title VARCHAR(255),
    preparation_period VARCHAR(30),
    part_warmup TEXT,
    part_main TEXT,
    part_cooldown TEXT,
    start_time TIME,
    end_time TIME,
    duration_minutes INTEGER,
    coach_notes TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_training_plan_items_plan_date
    ON training_plan_items (plan_id, scheduled_date);
