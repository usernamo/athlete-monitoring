-- Athlete Monitoring Platform — PostgreSQL schema
-- Flexible metrics: metric_type + value (no hard-coded wellness columns)

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. ROLES
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE
);

-- 2. USERS
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id UUID NOT NULL REFERENCES roles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 3. ORGANIZATIONS
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100),
    country VARCHAR(100)
);

-- 4. ATHLETE_PROFILES
CREATE TABLE athlete_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE,
    gender VARCHAR(20),
    sport VARCHAR(100),
    qualification VARCHAR(100),
    height_cm NUMERIC(5, 2),
    weight_kg NUMERIC(5, 2),
    organization_id UUID REFERENCES organizations(id),
    primary_coach_id UUID
);

-- 5. COACH_PROFILES
CREATE TABLE coach_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    specialization VARCHAR(150),
    experience_years INTEGER
);

ALTER TABLE athlete_profiles
    ADD CONSTRAINT fk_athlete_primary_coach
    FOREIGN KEY (primary_coach_id) REFERENCES coach_profiles(id);

-- 6. COACH_ATHLETES
CREATE TABLE coach_athletes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coach_id UUID NOT NULL REFERENCES coach_profiles(id) ON DELETE CASCADE,
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (coach_id, athlete_id)
);

-- 7. METRIC_CATEGORIES
CREATE TABLE metric_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE
);

-- 8. METRIC_TYPES
CREATE TABLE metric_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES metric_categories(id),
    name VARCHAR(100) NOT NULL,
    unit VARCHAR(50),
    min_value NUMERIC,
    max_value NUMERIC,
    UNIQUE (category_id, name)
);

-- 9. ATHLETE_METRICS (time-series)
CREATE TABLE athlete_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    metric_type_id UUID NOT NULL REFERENCES metric_types(id),
    value NUMERIC NOT NULL,
    measured_at TIMESTAMPTZ NOT NULL,
    notes TEXT
);

CREATE INDEX idx_athlete_metrics_athlete_time
    ON athlete_metrics (athlete_id, measured_at DESC);

CREATE INDEX idx_athlete_metrics_type_time
    ON athlete_metrics (metric_type_id, measured_at DESC);

-- 10. DAILY_REPORTS
CREATE TABLE daily_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    report_date DATE NOT NULL,
    sleep_time TIME,
    wake_time TIME,
    notes TEXT,
    UNIQUE (athlete_id, report_date)
);

-- 11. TRAININGS
CREATE TABLE trainings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    coach_id UUID REFERENCES coach_profiles(id),
    date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    duration_minutes INTEGER,
    volume NUMERIC,
    intensity NUMERIC,
    planned_hr INTEGER,
    actual_hr INTEGER,
    readiness_score NUMERIC,
    fatigue_score NUMERIC
);

CREATE INDEX idx_trainings_athlete_date ON trainings (athlete_id, date DESC);

-- 12. TRAINING_BLOCKS
CREATE TABLE training_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    training_id UUID NOT NULL REFERENCES trainings(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200),
    duration_minutes INTEGER
);

-- 13. TRAINING_EXERCISES
CREATE TABLE training_exercises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    training_block_id UUID NOT NULL REFERENCES training_blocks(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    sets INTEGER,
    reps INTEGER,
    weight NUMERIC,
    distance NUMERIC,
    duration_seconds INTEGER,
    intensity NUMERIC,
    hr_before INTEGER,
    hr_after INTEGER,
    notes TEXT
);

-- 14. NUTRITION_LOGS
CREATE TABLE nutrition_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    meal_type VARCHAR(50) NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL,
    appetite BOOLEAN
);

-- 15. NUTRITION_ITEMS
CREATE TABLE nutrition_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nutrition_log_id UUID NOT NULL REFERENCES nutrition_logs(id) ON DELETE CASCADE,
    product_name VARCHAR(200) NOT NULL,
    grams NUMERIC,
    protein NUMERIC,
    fat NUMERIC,
    carbs NUMERIC,
    calories NUMERIC
);

-- 16. WATER_INTAKE
CREATE TABLE water_intake (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    amount_ml INTEGER NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL
);

-- 17. SUPPLEMENTS
CREATE TABLE supplements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    dosage VARCHAR(100),
    consumed_at TIMESTAMPTZ NOT NULL
);

-- 18. COMPETITIONS
CREATE TABLE competitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    start_date DATE,
    end_date DATE
);

-- 19. ATHLETE_COMPETITIONS
CREATE TABLE athlete_competitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    competition_id UUID NOT NULL REFERENCES competitions(id) ON DELETE CASCADE,
    planned_result VARCHAR(200),
    actual_result VARCHAR(200),
    notes TEXT
);

-- 20. TRAINING_CAMPS
CREATE TABLE training_camps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location VARCHAR(255),
    start_date DATE,
    end_date DATE,
    goals TEXT
);

-- 21. TEST_TYPES
CREATE TABLE test_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    unit VARCHAR(50),
    UNIQUE (category, name)
);

-- 22. ATHLETE_TESTS
CREATE TABLE athlete_tests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    test_type_id UUID NOT NULL REFERENCES test_types(id),
    value NUMERIC NOT NULL,
    measured_at TIMESTAMPTZ NOT NULL
);

-- 23. MEDICAL_RECORDS
CREATE TABLE medical_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    type VARCHAR(100),
    institution VARCHAR(255),
    methods TEXT,
    recommendations TEXT,
    examination_date DATE
);

-- 24. FILES
CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    uploaded_by UUID REFERENCES users(id),
    type VARCHAR(50),
    path VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 25. NOTIFICATIONS
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    body TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 26. CHAT_MESSAGES
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 27. ANALYTICS_SNAPSHOTS
CREATE TABLE analytics_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    readiness_score NUMERIC,
    recovery_score NUMERIC,
    fatigue_score NUMERIC,
    injury_risk_score NUMERIC,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_analytics_athlete_time
    ON analytics_snapshots (athlete_id, generated_at DESC);
