-- Test seed data (matches athlete diary Excel structure)

INSERT INTO roles (id, name) VALUES
    ('11111111-1111-1111-1111-111111111101', 'admin'),
    ('11111111-1111-1111-1111-111111111102', 'coach'),
    ('11111111-1111-1111-1111-111111111103', 'athlete'),
    ('11111111-1111-1111-1111-111111111104', 'doctor'),
    ('11111111-1111-1111-1111-111111111105', 'nutritionist');

INSERT INTO organizations (id, name, type, country) VALUES
    ('22222222-2222-2222-2222-222222222201', 'СШОР №1', 'school', 'RU');

-- password: test123
INSERT INTO users (id, email, password_hash, role_id) VALUES
    ('33333333-3333-3333-3333-333333333301', 'coach@test.local',
     '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '11111111-1111-1111-1111-111111111102'),
    ('33333333-3333-3333-3333-333333333302', 'athlete@test.local',
     '$2b$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '11111111-1111-1111-1111-111111111103');

INSERT INTO coach_profiles (id, user_id, specialization, experience_years) VALUES
    ('44444444-4444-4444-4444-444444444401', '33333333-3333-3333-3333-333333333301', 'лёгкая атлетика', 12);

INSERT INTO athlete_profiles (id, user_id, first_name, last_name, birth_date, gender, sport, qualification, height_cm, weight_kg, organization_id, primary_coach_id) VALUES
    ('55555555-5555-5555-5555-555555555501', '33333333-3333-3333-3333-333333333302',
     'Иван', 'Петров', '2006-03-15', 'male', 'лёгкая атлетика', 'КМС', 178, 72.5,
     '22222222-2222-2222-2222-222222222201', '44444444-4444-4444-4444-444444444401');

INSERT INTO coach_athletes (coach_id, athlete_id) VALUES
    ('44444444-4444-4444-4444-444444444401', '55555555-5555-5555-5555-555555555501');

INSERT INTO metric_categories (id, name) VALUES
    ('66666666-6666-6666-6666-666666666601', 'recovery'),
    ('66666666-6666-6666-6666-666666666602', 'sleep'),
    ('66666666-6666-6666-6666-666666666603', 'wellness'),
    ('66666666-6666-6666-6666-666666666604', 'cardio'),
    ('66666666-6666-6666-6666-666666666605', 'body'),
    ('66666666-6666-6666-6666-666666666606', 'fatigue');

INSERT INTO metric_types (id, category_id, name, unit, min_value, max_value) VALUES
    ('77777777-7777-7777-7777-777777777701', '66666666-6666-6666-6666-666666666602', 'sleep_duration', 'hours', 0, 24),
    ('77777777-7777-7777-7777-777777777702', '66666666-6666-6666-6666-666666666602', 'sleep_quality', 'score', 1, 10),
    ('77777777-7777-7777-7777-777777777703', '66666666-6666-6666-6666-666666666603', 'motivation', 'score', 1, 10),
    ('77777777-7777-7777-7777-777777777704', '66666666-6666-6666-6666-666666666606', 'fatigue', 'score', 1, 10),
    ('77777777-7777-7777-7777-777777777705', '66666666-6666-6666-6666-666666666604', 'resting_hr', 'bpm', 30, 120),
    ('77777777-7777-7777-7777-777777777706', '66666666-6666-6666-6666-666666666604', 'standing_hr', 'bpm', 40, 150),
    ('77777777-7777-7777-7777-777777777707', '66666666-6666-6666-6666-666666666605', 'body_weight', 'kg', 30, 200),
    ('77777777-7777-7777-7777-777777777708', '66666666-6666-6666-6666-666666666605', 'water_intake', 'ml', 0, 10000);

-- Sample metrics (last 7 days)
INSERT INTO athlete_metrics (athlete_id, metric_type_id, value, measured_at) VALUES
    ('55555555-5555-5555-5555-555555555501', '77777777-7777-7777-7777-777777777701', 8.5, NOW() - INTERVAL '1 day'),
    ('55555555-5555-5555-5555-555555555501', '77777777-7777-7777-7777-777777777702', 7, NOW() - INTERVAL '1 day'),
    ('55555555-5555-5555-5555-555555555501', '77777777-7777-7777-7777-777777777703', 8, NOW() - INTERVAL '1 day'),
    ('55555555-5555-5555-5555-555555555501', '77777777-7777-7777-7777-777777777704', 3, NOW() - INTERVAL '1 day'),
    ('55555555-5555-5555-5555-555555555501', '77777777-7777-7777-7777-777777777705', 52, NOW() - INTERVAL '1 day'),
    ('55555555-5555-5555-5555-555555555501', '77777777-7777-7777-7777-777777777707', 72.3, NOW() - INTERVAL '1 day');

INSERT INTO daily_reports (athlete_id, report_date, sleep_time, wake_time, notes) VALUES
    ('55555555-5555-5555-5555-555555555501', CURRENT_DATE - 1, '23:00', '07:30', 'Хорошее самочувствие'),
    ('55555555-5555-5555-5555-555555555501', CURRENT_DATE, '23:30', '07:00', NULL);

INSERT INTO trainings (id, athlete_id, coach_id, date, start_time, end_time, duration_minutes, volume, intensity, planned_hr, actual_hr, readiness_score, fatigue_score) VALUES
    ('88888888-8888-8888-8888-888888888801', '55555555-5555-5555-5555-555555555501', '44444444-4444-4444-4444-444444444401',
     CURRENT_DATE - 1, '09:00', '11:30', 150, 12.5, 7.5, 145, 152, 8.2, 4.1);

INSERT INTO training_blocks (id, training_id, type, title, duration_minutes) VALUES
    ('99999999-9999-9999-9999-999999999901', '88888888-8888-8888-8888-888888888801', 'warmup', 'Разминка', 20),
    ('99999999-9999-9999-9999-999999999902', '88888888-8888-8888-8888-888888888801', 'main', 'Интервалы 400м', 90),
    ('99999999-9999-9999-9999-999999999903', '88888888-8888-8888-8888-888888888801', 'cooldown', 'Заминка', 40);

INSERT INTO training_exercises (training_block_id, name, sets, reps, distance, intensity, hr_after, notes) VALUES
    ('99999999-9999-9999-9999-999999999902', 'Бег 400м', 6, 1, 400, 8.5, 165, 'Отдых 3 мин');

INSERT INTO nutrition_logs (id, athlete_id, meal_type, consumed_at, appetite) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', '55555555-5555-5555-5555-555555555501', 'breakfast', NOW() - INTERVAL '6 hours', TRUE);

INSERT INTO nutrition_items (nutrition_log_id, product_name, grams, protein, fat, carbs, calories) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'Овсянка', 80, 10, 5, 54, 320),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01', 'Банан', 120, 1.5, 0.4, 27, 105);

INSERT INTO water_intake (athlete_id, amount_ml, recorded_at) VALUES
    ('55555555-5555-5555-5555-555555555501', 500, NOW() - INTERVAL '2 hours'),
    ('55555555-5555-5555-5555-555555555501', 750, NOW() - INTERVAL '30 minutes');

INSERT INTO test_types (id, category, name, unit) VALUES
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 'general_fitness', 'Бег 3 км', 'min'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb02', 'anthropometry', 'ИМТ', 'kg/m2');

INSERT INTO athlete_tests (athlete_id, test_type_id, value, measured_at) VALUES
    ('55555555-5555-5555-5555-555555555501', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb01', 11.45, NOW() - INTERVAL '30 days');

INSERT INTO competitions (id, name, location, start_date, end_date) VALUES
    ('cccccccc-cccc-cccc-cccc-cccccccccc01', 'Чемпионат региона', 'Москва', CURRENT_DATE + 14, CURRENT_DATE + 16);

INSERT INTO athlete_competitions (athlete_id, competition_id, event_date, location, planned_result, actual_result, notes) VALUES
    ('55555555-5555-5555-5555-555555555501', 'cccccccc-cccc-cccc-cccc-cccccccccc01',
     CURRENT_DATE + 14, 'Москва', '11.2 сек 100м', NULL, 'Чемпионат региона');

INSERT INTO training_camps (id, athlete_id, location, start_date, end_date, goals, notes) VALUES
    ('dddddddd-dddd-dddd-dddd-dddddddddd01', '55555555-5555-5555-5555-555555555501',
     'Сочи', CURRENT_DATE + 30, CURRENT_DATE + 45, 'Сборная подготовка к сезону', NULL);

INSERT INTO medical_records (athlete_id, type, examination_date, institution, methods, recommendations) VALUES
    ('55555555-5555-5555-5555-555555555501', 'mandatory', CURRENT_DATE - 60, 'Спортивная клиника', 'ЭКГ, УЗИ сердца', 'Допуск без ограничений');

INSERT INTO analytics_snapshots (athlete_id, readiness_score, recovery_score, fatigue_score, injury_risk_score) VALUES
    ('55555555-5555-5555-5555-555555555501', 8.2, 7.8, 3.1, 2.4);
