#!/usr/bin/env python3
"""
summarize_catalog.py

Parses src/main/resources/data.sql for this project and prints a clean
Markdown table of every row in the work_spec_catalog seed data.

This is meant to be run BY a skill (see provisioning-catalog-summarizer
SKILL.md), not edited by hand each time the catalog changes - it always
reflects whatever is actually in data.sql, so the output can't drift out
of sync with the real seed data the way a hand-maintained doc could.

Usage:
    python3 scripts/summarize_catalog.py
    python3 scripts/summarize_catalog.py --path /custom/path/to/data.sql
"""

import argparse
import re
import sys
from pathlib import Path

# Matches INSERT statements shaped like the ones in this project's data.sql, e.g.:
# INSERT INTO work_spec_catalog (service_code, network_type, work_spec_code,
#   description, duration_minutes, required_skill)
# VALUES ('INTERNET', 'FIBER', 'INSTALL_DROP', 'Run fiber drop...', 60, 'OUTSIDE_PLANT');
INSERT_PATTERN = re.compile(
    r"INSERT INTO work_spec_catalog\s*\([^)]*\)\s*VALUES\s*\(([^;]*)\)\s*;",
    re.IGNORECASE | re.DOTALL,
)

COLUMNS = [
    "service_code",
    "network_type",
    "work_spec_code",
    "description",
    "duration_minutes",
    "required_skill",
]


def parse_values(raw_values: str) -> list[str]:
    """
    Splits a VALUES(...) tuple into individual field strings, respecting
    single-quoted strings so commas inside a description don't break the split.
    """
    fields = []
    current = ""
    in_quotes = False
    for ch in raw_values:
        if ch == "'" and not in_quotes:
            in_quotes = True
            continue
        if ch == "'" and in_quotes:
            in_quotes = False
            continue
        if ch == "," and not in_quotes:
            fields.append(current.strip())
            current = ""
            continue
        current += ch
    fields.append(current.strip())
    return fields


def load_rows(sql_path: Path) -> list[dict]:
    if not sql_path.exists():
        print(f"ERROR: {sql_path} not found", file=sys.stderr)
        sys.exit(1)

    text = sql_path.read_text(encoding="utf-8")
    rows = []
    for match in INSERT_PATTERN.finditer(text):
        values = parse_values(match.group(1))
        if len(values) != len(COLUMNS):
            print(
                f"WARNING: skipping malformed row (expected {len(COLUMNS)} "
                f"fields, got {len(values)}): {match.group(0)[:80]}...",
                file=sys.stderr,
            )
            continue
        rows.append(dict(zip(COLUMNS, values)))
    return rows


def to_markdown_table(rows: list[dict]) -> str:
    if not rows:
        return "_No work_spec_catalog rows found._"

    header = "| Service | Network | Work Spec | Description | Duration (min) | Skill |"
    separator = "|---|---|---|---|---|---|"
    lines = [header, separator]

    # Sorted for stable, diffable output across runs
    for row in sorted(rows, key=lambda r: (r["service_code"], r["network_type"], r["work_spec_code"])):
        lines.append(
            f"| {row['service_code']} | {row['network_type']} | {row['work_spec_code']} | "
            f"{row['description']} | {row['duration_minutes']} | {row['required_skill']} |"
        )
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--path",
        default="src/main/resources/data.sql",
        help="Path to data.sql (default: src/main/resources/data.sql, relative to cwd)",
    )
    args = parser.parse_args()

    rows = load_rows(Path(args.path))
    print(to_markdown_table(rows))
    print(f"\n_{len(rows)} work spec(s) currently in the catalog._", file=sys.stderr)


if __name__ == "__main__":
    main()