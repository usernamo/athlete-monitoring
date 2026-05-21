-- Одна собственная тренировка спортсмена на день (отдельно от записей тренера)
CREATE UNIQUE INDEX IF NOT EXISTS uq_trainings_athlete_date_self
    ON trainings (athlete_id, date)
    WHERE coach_id IS NULL;
