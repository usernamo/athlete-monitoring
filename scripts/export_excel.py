# -*- coding: utf-8 -*-
"""
Экспорт дневника из PostgreSQL в Excel (для сравнения с вашей таблицей).

  pip install -r requirements.txt
  python export_excel.py

Файл: exports/дневник_из_приложения.xlsx
"""
from __future__ import annotations

import os
from datetime import date

import pandas as pd
import psycopg2

DSN = os.environ.get(
    "DATABASE_URL",
    "postgresql://athlete:athlete_secret@localhost:5432/athlete_monitoring",
)
ATHLETE_ID = "55555555-5555-5555-5555-555555555501"
OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "exports")

# Порядок колонок как в типичном дневнике Excel
COLUMNS = [
    ("Дата", "report_date"),
    ("Заснул", "sleep_time"),
    ("Проснулся", "wake_time"),
    ("Сон (ч)", "sleep_duration"),
    ("Качество сна", "sleep_quality"),
    ("Мотивация", "motivation"),
    ("Усталость", "fatigue"),
    ("Настроение", "mood"),
    ("Самочувствие", "wellbeing"),
    ("Пульс покоя", "resting_hr"),
    ("Пульс стоя", "standing_hr"),
    ("Масса тела (кг)", "body_weight"),
    ("Вода (мл)", "water_ml"),
    ("Длительность (мин)", "duration_minutes"),
    ("Объём", "volume"),
    ("Интенсивность", "intensity"),
    ("ЧСС факт", "actual_hr"),
    ("Готовность", "readiness_score"),
    ("Заметки", "notes"),
]


def main():
    conn = psycopg2.connect(DSN)
    cur = conn.cursor()

    cur.execute(
        """
        SELECT DISTINCT d::date FROM (
          SELECT measured_at::date AS d FROM athlete_metrics WHERE athlete_id = %s
          UNION SELECT report_date FROM daily_reports WHERE athlete_id = %s
          UNION SELECT date FROM trainings WHERE athlete_id = %s
        ) t ORDER BY d
        """,
        (ATHLETE_ID, ATHLETE_ID, ATHLETE_ID),
    )
    dates = [r[0] for r in cur.fetchall()]

    rows_out = []
    for d in dates:
        day = d.isoformat() if hasattr(d, "isoformat") else str(d)
        row = {"report_date": day}

        cur.execute(
            "SELECT sleep_time, wake_time, notes FROM daily_reports WHERE athlete_id=%s AND report_date=%s",
            (ATHLETE_ID, d),
        )
        dr = cur.fetchone()
        if dr:
            row["sleep_time"] = str(dr[0])[:5] if dr[0] else ""
            row["wake_time"] = str(dr[1])[:5] if dr[1] else ""
            row["notes"] = dr[2] or ""

        cur.execute(
            """
            SELECT mt.name, am.value FROM athlete_metrics am
            JOIN metric_types mt ON mt.id = am.metric_type_id
            WHERE am.athlete_id=%s AND am.measured_at::date=%s
            """,
            (ATHLETE_ID, d),
        )
        for name, val in cur.fetchall():
            row[name] = float(val)

        cur.execute(
            "SELECT COALESCE(SUM(amount_ml),0) FROM water_intake WHERE athlete_id=%s AND recorded_at::date=%s",
            (ATHLETE_ID, d),
        )
        row["water_ml"] = int(cur.fetchone()[0])

        cur.execute(
            """SELECT duration_minutes, volume, intensity, actual_hr, readiness_score, fatigue_score
               FROM trainings WHERE athlete_id=%s AND date=%s LIMIT 1""",
            (ATHLETE_ID, d),
        )
        tr = cur.fetchone()
        if tr:
            keys = ["duration_minutes", "volume", "intensity", "actual_hr", "readiness_score", "fatigue_score"]
            for k, v in zip(keys, tr):
                if v is not None:
                    row[k] = float(v) if k != "duration_minutes" and k != "actual_hr" else int(v)

        rows_out.append(row)

    conn.close()

    os.makedirs(OUT_DIR, exist_ok=True)
    out_path = os.path.join(OUT_DIR, "дневник_из_приложения.xlsx")

    # Excel: заголовки на русском
    excel_rows = []
    for r in rows_out:
        line = {}
        for label, key in COLUMNS:
            line[label] = r.get(key, "")
        excel_rows.append(line)

    df = pd.DataFrame(excel_rows)
    df.to_excel(out_path, index=False, sheet_name="Дневник")
    print(f"Exported {len(df)} rows -> {out_path}")
    print("Сравните с вашим Excel построчно по дате.")


if __name__ == "__main__":
    main()
