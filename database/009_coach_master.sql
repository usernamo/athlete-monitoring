-- Master (тренер) → slave (спортсмены): рекомендации тренера

CREATE TABLE IF NOT EXISTS coach_recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coach_id UUID NOT NULL REFERENCES coach_profiles(id) ON DELETE CASCADE,
    athlete_id UUID NOT NULL REFERENCES athlete_profiles(id) ON DELETE CASCADE,
    recommendation_text TEXT NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'general',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_coach_recommendations_athlete
    ON coach_recommendations (athlete_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_coach_recommendations_coach
    ON coach_recommendations (coach_id, created_at DESC);
