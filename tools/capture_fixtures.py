# SPDX-License-Identifier: GPL-3.0-or-later
"""Record live API responses as unit-test fixtures.

Usage (from the repository root):
    python tools/capture_fixtures.py

Writes app/src/test/resources/fixtures/*.json and refreshes the bundled
assets/zfv_uzh_mapping.json. Reads ZFV_API_KEY from local.properties.
Uses Python's standard library only.
"""
import datetime as dt
import json
import urllib.request
from pathlib import Path
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT
OUT = ANDROID / "core" / "src" / "test" / "resources" / "fixtures"
ASSETS = ANDROID / "core" / "src" / "main" / "assets"
UA = "MensaZurich-Android/1.0.0 (+https://github.com/mrghxst/mensa-zurich) fixture-capture"
MAPPING_URL = (
    "https://raw.githubusercontent.com/alexandrereol/ethmensa/refs/heads/main/"
    "ethmensa/Data/API/ZFV/zfv_uzh_mapping.json"
)
ACCEPT_LANGUAGE = {"de": "de-DE;de;q=0.9", "en": "en-EN,en;q=0.9"}


def zfv_key() -> str:
    for line in (ANDROID / "local.properties").read_text(encoding="utf-8").splitlines():
        if line.startswith("ZFV_API_KEY="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("ZFV_API_KEY missing in local.properties")


def fetch(url: str, headers: dict, body: bytes | None = None) -> bytes:
    req = urllib.request.Request(url, data=body, headers={"User-Agent": UA, **headers})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read()


def save(name: str, raw: bytes) -> None:
    data = json.loads(raw)
    (OUT / name).write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"wrote {name} ({len(raw) // 1024} KB)")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    try:
        today = dt.datetime.now(ZoneInfo("Europe/Zurich")).date()
    except Exception:  # Windows Python ships without tzdata; the local date is close enough.
        today = dt.date.today()
    monday = today - dt.timedelta(days=today.weekday())
    before = monday + dt.timedelta(days=14)
    for lang in ("en", "de"):
        headers = {"Accept": "application/json", "Accept-Language": ACCEPT_LANGUAGE[lang]}
        save(f"eth_directory_{lang}.json", fetch(f"https://glyph.ethz.ch/eth-ws/mensas/detail?lang={lang}", headers))
        rotas = (
            "https://idapps.ethz.ch/cookpit-pub-services/v1/weeklyrotas?client-id=ethz-wcms"
            f"&lang={lang}&rs-first=0&rs-size=50&valid-after={monday}&valid-before={before}"
        )
        save(f"eth_rotas_{lang}.json", fetch(rotas, headers))

    query = (ASSETS / "zfv_query.graphql").read_text(encoding="utf-8")
    body = json.dumps({"query": query}).encode()
    for lang in ("en", "de"):
        headers = {"Content-Type": "application/json", "api-key": zfv_key(), "locale": lang}
        save(f"zfv_{lang}.json", fetch("https://api.zfv.ch/graphql", headers, body))

    mapping = fetch(MAPPING_URL, {})
    save("zfv_uzh_mapping.json", mapping)
    (ASSETS / "zfv_uzh_mapping.json").write_bytes(mapping)
    (OUT / "capture_date.txt").write_text(f"{today.isoformat()}\n", encoding="utf-8")
    print(f"captured on {today} (Monday {monday})")


if __name__ == "__main__":
    main()
