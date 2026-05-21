-- Исход соревнования для подсветки календаря: pending | win | loss

ALTER TABLE athlete_competitions
    ADD COLUMN IF NOT EXISTS outcome VARCHAR(10) DEFAULT 'pending';

COMMENT ON COLUMN athlete_competitions.outcome IS 'pending — запланировано, win — выигрыш, loss — проигрыш';
