#!/usr/bin/env python3
"""Build an attributed, validated offline South African draw archive.

Fetches year-by-year result tables from an independent public archive at build
time. This is NOT a licensed official API or a guarantee of complete history.
Every output row is a historical observation, not a predictive signal.
"""
from __future__ import annotations

import argparse
import csv
import datetime as dt
from html.parser import HTMLParser
from pathlib import Path
import re
import time
import urllib.request

BASE = "https://za.national-lottery.com"
START = dt.date(2025, 9, 21)
DAYS = "Monday Tuesday Wednesday Thursday Friday Saturday Sunday"
DATE = re.compile(
    rf"(?:{DAYS.replace(' ', '|')})\s*(\d{{1,2}})\s+"
    r"(January|February|March|April|May|June|July|August|September|October|November|December)"
    r"\s+(20\d{2})", re.I
)
MONTH = {m: i for i, m in enumerate(
    "January February March April May June July August September October November December".split(), 1)}
SOURCES = {
    "lotto": ("LOTTO", "LOTTO"),
    "lotto-plus-1": ("PLUS1", "PLUS1"),
    "lotto-5-max": ("PLUS2", "MAX5"),
    "powerball": ("POWERBALL", "POWERBALL"),
    "powerball-xtra": ("POWERPLUS", "XTRA"),
    "daily-lotto": ("DAILY", "DAILY"),
    "daily-lotto-plus": ("DAILYPLUS", None),
}
PICKS = {"LOTTO": 6, "PLUS1": 6, "PLUS2": 6, "MAX5": 6,
         "POWERBALL": 5, "POWERPLUS": 5, "XTRA": 5,
         "DAILY": 5, "DAILYPLUS": 5}
POOL = {"LOTTO": 52, "PLUS1": 52, "PLUS2": 58, "MAX5": 52,
        "POWERBALL": 50, "POWERPLUS": 50, "XTRA": 50,
        "DAILY": 36, "DAILYPLUS": 36}
POWER = {"POWERBALL", "POWERPLUS", "XTRA"}
BONUS = {"LOTTO", "PLUS1", "PLUS2", "MAX5"}


class ArchiveTable(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.in_row = False
        self.in_cell = False
        self.cell = []
        self.row = []
        self.rows = []

    def handle_starttag(self, tag, attrs):
        if tag == "tr":
            self.in_row = True
            self.row = []
        elif tag in ("td", "th") and self.in_row:
            self.in_cell = True
            self.cell = []

    def handle_endtag(self, tag):
        if tag in ("td", "th") and self.in_cell:
            self.row.append(" ".join(self.cell))
            self.in_cell = False
        elif tag == "tr" and self.in_row:
            if self.row:
                self.rows.append(self.row)
            self.in_row = False

    def handle_data(self, value):
        if self.in_row and self.in_cell:
            value = value.strip()
            if value:
                self.cell.append(value)


def parse_page(html: str, game: str, url: str, asof: dt.date):
    parser = ArchiveTable()
    parser.feed(html)
    rows = []
    for cells in parser.rows:
        if len(cells) < 2:
            continue
        match = DATE.search(cells[0])
        if not match:
            continue
        day, month, year = match.groups()
        drawn = dt.date(int(year), MONTH[month.title()], int(day))
        if drawn < START or drawn > asof:
            continue
        name = SOURCES[game][0 if drawn < dt.date(2026, 6, 1) else 1]
        if name is None:
            continue
        tokens = re.findall(r"\b\d{1,2}\b", cells[1])
        length = PICKS[name] + (1 if name in BONUS or name in POWER else 0)
        if len(tokens) != length:
            raise ValueError(f"{url}: {drawn} {name} has {len(tokens)} balls, expected {length}")
        ints = [int(x) for x in tokens]
        main, special = ints[:PICKS[name]], ints[PICKS[name]:]
        max_ball = POOL[name]
        pb_max = 20 if drawn < dt.date(2026, 6, 1) else 16
        if len(set(main)) != len(main) or not all(1 <= x <= max_ball if name not in BONUS or drawn >= dt.date(2026, 6, 1) else 1 <= x <= 58 for x in main):
            raise ValueError(f"{url}: invalid or duplicate main ball on {drawn}")
        if name in BONUS and (special[0] < 1 or special[0] > (58 if drawn < dt.date(2026,6,1) else 52) or special[0] in main):
            raise ValueError(f"{url}: invalid bonus on {drawn}")
        if name in POWER and not (1 <= special[0] <= pb_max):
            raise ValueError(f"{url}: invalid PowerBall on {drawn}")
        rows.append((name, drawn.isoformat(), " ".join(map(str, sorted(main))),
                     special[0] if name in BONUS else "",
                     special[0] if name in POWER else "", url))
    if not rows:
        raise ValueError(f"No supported dated draw rows in {url}; do not generate empty seed.")
    return rows


def fetch(url: str):
    req = urllib.request.Request(url, headers={
        "User-Agent": "Mozilla/5.0 (compatible; LottoLabArchive/0.4; historical fact cache)",
        "Accept": "text/html",
    })
    with urllib.request.urlopen(req, timeout=25) as response:
        final = response.geturl()
        if not final.startswith(BASE + "/"):
            raise ValueError(f"Archive redirected to unexpected site: {final}")
        return response.read(3_000_000).decode("utf-8"), final


def build(output: Path, years=(2025, 2026)):
    asof = dt.datetime.now(dt.timezone(dt.timedelta(hours=2))).date()
    data = {}
    for slug in SOURCES:
        for year in years:
            if year == 2025 and slug in ():
                continue
            url = f"{BASE}/{slug}/results/{year}-archive"
            html, canonical = fetch(url)
            results = parse_page(html, slug, canonical, asof)
            for row in results:
                key = (row[0], row[1])
                if key in data and data[key] != row:
                    raise ValueError(f"Conflicting archive draw for {key}")
                data[key] = row
            print(f"{slug} {year}: {len(results)} supported rows", flush=True)
            time.sleep(0.25)  # modest request rate
    if len(data) < 500:
        raise ValueError(f"Only {len(data)} historical records; refusing a partial offline build.")
    counts = {g: sum(1 for r in data if r[0] == g) for g in PICKS}
    if any(v < 15 for v in counts.values()):
        raise ValueError(f"Missing important game history: {counts}")
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.writer(fh)
        writer.writerow(("game", "date", "numbers", "bonus", "powerball", "source_url", "fetched_at"))
        timestamp = dt.datetime.now(dt.timezone.utc).isoformat().replace("+00:00", "Z")
        for row in sorted(data.values(), key=lambda r: (r[1], r[0])):
            writer.writerow((*row, timestamp))
    print(f"Bundled {len(data)} validated real draw records: {counts}", flush=True)


def fixture_test():
    page = """
    <table><tr><th>Draw Date</th><th>Results</th><th>Jackpot</th></tr>
    <tr><td><a>Wednesday14 October 2026</a></td>
    <td><ul><li>11</li><li>12</li><li>30</li><li>42</li><li>47</li><li>48</li><li>10</li></ul></td>
    <td>R4,434,479.60</td></tr></table>"""
    rows = parse_page(page, "lotto", f"{BASE}/lotto/results/2026-archive", dt.date(2026, 10, 16))
    assert rows[0][:5] == ("LOTTO", "2026-10-14", "11 12 30 42 47 48", 10, "")
    print("HTML parser fixture passed", flush=True)


if __name__ == "__main__":
    cli = argparse.ArgumentParser()
    cli.add_argument("--fixture-only", action="store_true")
    cli.add_argument("--output", default="lottolab-next/app/src/main/assets/seed_draws.csv")
    args = cli.parse_args()
    fixture_test()
    if not args.fixture_only:
        build(Path(args.output))
