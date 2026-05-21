# -*- coding: utf-8 -*-
"""
Import athlete diary Excel files into PostgreSQL.
Maps flexible columns to metric_types + athlete_metrics.

Usage:
  pip install -r requirements.txt
  python import_excel.py

Env:
  DATABASE_URL=postgresql://athlete:athlete_secret@localhost:5432/athlete_monitoring
"""
from __future__ import annotations

import os
import re
from datetime import datetime

import pandas as pd
import psycopg2
from psycopg2.extras import execute_values

BASE = r"c:\Users\PC\Downloads\Telegram Desktop"
XLSX_FILES = [
    os.path.join(BASE, "дневник спортсмена.xlsx"),
    os.path.join(BASE, "дневник спортсмена 1.xlsx"),
    os.path.join(BASE, "дневник спортсмена 2.xlsx"),
]

ATHLETE_ID = "55555555-5555-5555-5555-555555555501"

# Excel header (RU) -> metric_types.name
METRIC_ALIASES = {
    "сон (ч)": "sleep_duration",
    "сон, ч": "sleep_duration",
    "продолжительность сна": "sleep_duration",
    "качество сна": "sleep_quality",
    "мотивация": "motivation",
    "усталость": "fatigue",
    "пульс покоя": "resting_hr",
    "пульс стоя": "standing_hr",
    "вес": "body_weight",
    "масса тела": "body_weight",
    "вода": "water_intake",
    "вода, мл": "water_intake",
    "настроение": "mood",
    "самочувствие": "wellbeing",
    "заснул": "sleep_time",
    "проснулся": "wake_time",
}


def norm(s: str) -> str:
    return re.sub(r"\s+", " ", str(s).strip().lower())


def load_metric_map(conn):
    cur = conn.cursor()
    cur.execute("SELECT id, name FROM metric_types")
    rows = cur.fetchall()
    cur.close()
    return {name: mid for mid, name in rows}


def find_header_row(df: pd.DataFrame) -> int | None:
    for i in range(min(15, len(df))):
        row = [norm(x) for x in df.iloc[i].tolist() if pd.notna(x)]
        hits = sum(1 for c in row if c in METRIC_ALIASES or any(k in c for k in METRIC_ALIASES))
        if hits >= 2:
            return i
    return None


def parse_sheet(conn, df: pd.DataFrame, metric_map: dict, sheet_name: str):
    header_idx = find_header_row(df)
    if header_idx is None:
        print(f"  skip sheet {sheet_name!r}: header not found")
        return 0

    headers = [norm(h) if pd.notna(h) else "" for h in df.iloc[header_idx]]
    data = df.iloc[header_idx + 1 :].copy()
    data.columns = headers

    date_col = next((c for c in headers if "дата" in c or c == "date"), None)
    if not date_col:
        print(f"  skip sheet {sheet_name!r}: no date column")
        return 0

    inserts = []
    for _, row in data.iterrows():
        raw_date = row.get(date_col)
        if pd.isna(raw_date):
            continue
        try:
            measured_at = pd.to_datetime(raw_date).to_pydatetime()
        except Exception:
            continue

        for col, val in row.items():
            if col == date_col or pd.isna(val):
                continue
            key = METRIC_ALIASES.get(col)
            if not key:
                for alias, name in METRIC_ALIASES.items():
                    if alias in col:
                        key = name
                        break
            if not key or key not in metric_map:
                continue
            try:
                num = float(val)
            except (TypeError, ValueError):
                continue
            inserts.append((ATHLETE_ID, metric_map[key], num, measured_at))

    if not inserts:
        return 0

    cur = conn.cursor()
    execute_values(
        cur,
        """
        INSERT INTO athlete_metrics (athlete_id, metric_type_id, value, measured_at)
        VALUES %s
        """,
        inserts,
        template="(%s, %s, %s, %s)",
    )
    conn.commit()
    cur.close()
    return len(inserts)


def main():
    dsn = os.environ.get(
        "DATABASE_URL",
        "postgresql://athlete:athlete_secret@localhost:5432/athlete_monitoring",
    )
    conn = psycopg2.connect(dsn)
    metric_map = load_metric_map(conn)
    total = 0

    for path in XLSX_FILES:
        print(f"File: {path}")
        if not os.path.isfile(path):
            print("  NOT FOUND")
            continue
        xl = pd.ExcelFile(path, engine="openpyxl")
        for sheet in xl.sheet_names:
            df = pd.read_excel(path, sheet_name=sheet, header=None, engine="openpyxl")
            n = parse_sheet(conn, df, metric_map, sheet)
            print(f"  {sheet}: imported {n} metrics")
            total += n
        xl.close()

    conn.close()
    print(f"Done. Total metrics imported: {total}")


if __name__ == "__main__":
    main()
