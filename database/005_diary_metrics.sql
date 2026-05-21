-- Доп. показатели дневника (как в Excel) + подписи для формы

INSERT INTO metric_categories (id, name) VALUES
    ('66666666-6666-6666-6666-666666666603', 'wellness')
ON CONFLICT (id) DO NOTHING;

INSERT INTO metric_types (id, category_id, name, unit, min_value, max_value) VALUES
    ('77777777-7777-7777-7777-777777777709', '66666666-6666-6666-6666-666666666603', 'mood', 'score', 1, 10),
    ('77777777-7777-7777-7777-777777777710', '66666666-6666-6666-6666-666666666603', 'wellbeing', 'score', 1, 10)
ON CONFLICT (category_id, name) DO NOTHING;

-- Уникальность: одно значение метрики на спортсмена / тип / день
CREATE UNIQUE INDEX IF NOT EXISTS uq_athlete_metric_per_day
    ON athlete_metrics (athlete_id, metric_type_id, ((measured_at AT TIME ZONE 'UTC')::date));
