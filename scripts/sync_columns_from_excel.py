# -*- coding: utf-8 -*-
"""
Читает заголовки вашего Excel и печатает mapping для excelForm.js / import_excel.py

  python sync_columns_from_excel.py "C:\path\to\дневник спортсмена.xlsx"
"""
import os
import re
import sys

import pandas as pd

from import_excel import METRIC_ALIASES, norm

def main():
    path = sys.argv[1] if len(sys.argv) > 1 else r"c:\Users\PC\Downloads\Telegram Desktop\дневник спортсмена.xlsx"
    if not os.path.isfile(path):
        print(f"File not found: {path}")
        sys.exit(1)

    xl = pd.ExcelFile(path, engine="openpyxl")
    print(f"File: {path}\nSheets: {xl.sheet_names}\n")

    for sheet in xl.sheet_names:
        df = pd.read_excel(path, sheet_name=sheet, header=None, engine="openpyxl")
        print(f"=== {sheet} ===")
        for i in range(min(10, len(df))):
            cells = [norm(c) for c in df.iloc[i].tolist() if pd.notna(c) and str(c).strip()]
            if len(cells) >= 3:
                print(f"  row {i}: {cells}")
                mapped = []
                for c in cells:
                    key = METRIC_ALIASES.get(c)
                    if not key:
                        for alias, name in METRIC_ALIASES.items():
                            if alias in c:
                                key = name
                                break
                    mapped.append(f"{c} -> {key or '?'}")
                print("  mapping:", mapped[:15])
        print()


if __name__ == "__main__":
    main()
