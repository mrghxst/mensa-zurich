# SPDX-License-Identifier: GPL-3.0-or-later
"""One-off: geocode every mensa address into app/src/main/assets/mensa_coordinates.json.

Usage (from the repository root, after capture_fixtures.py):
    python android/tools/geocode_mensas.py

Uses OpenStreetMap Nominatim under its usage policy: at most 1 request per second and a
descriptive User-Agent. Reads the addresses from the recorded fixtures (ETH directory, ZFV
outlets, ZFV mapping file). Points outside Zürich are reported; Basel addresses are expected.
"""
import json
import time
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FIXTURES = ROOT / "android" / "app" / "src" / "test" / "resources" / "fixtures"
OUT = ROOT / "android" / "app" / "src" / "main" / "assets" / "mensa_coordinates.json"
UA = "MensaZurich-Android/1.0.0 one-off geocoding (+https://github.com/mrghxst/ethmensa)"
ZURICH_BOX = (47.32, 47.45, 8.44, 8.63)  # lat min, lat max, lng min, lng max

# Hand fixes for addresses Nominatim can't resolve as delivered.
OVERRIDES = {
    "Vladimir-Prelog-Weg 10 1, 8049 Zürich": "Vladimir-Prelog-Weg 10, 8049 Zürich",
    "Binzmühlestrasse 130, 8050 Oerlikon": "Binzmühlestrasse 130, 8050 Zürich",
    # OpenStreetMap spells it "Universitätstrasse".
    "Universitätsstrasse 6, 8092 Zürich": "Universitätstrasse 6, 8092 Zürich",
    "Universitätsstrasse 16, 8092 Zürich": "Universitätstrasse 16, 8092 Zürich",
}


def clean(address: str) -> str:
    lines = [line.replace("​", "").strip() for line in address.split("\n") if line.strip()]
    return ", ".join(lines)


def eth_addresses() -> dict[str, str]:
    out = {}
    for entry in json.loads((FIXTURES / "eth_directory_en.json").read_text(encoding="utf-8")):
        lines = [line for line in (entry.get("address") or "").split("\n") if line.strip()]
        if len(lines) >= 2:
            # The first line is a building code such as "MM B79".
            out[f"eth/{entry['mensaId']}"] = clean("\n".join(lines[1:]))
    return out


def uzh_addresses() -> dict[str, str]:
    mapping = json.loads((FIXTURES / "zfv_uzh_mapping.json").read_text(encoding="utf-8"))
    outlets = json.loads((FIXTURES / "zfv_en.json").read_text(encoding="utf-8"))["data"]["outlets"]
    out = {}
    for outlet in outlets:
        info = mapping.get(outlet["externalId"], {})
        key = f"uzh/{info.get('uzhID') or outlet['externalId']}"
        address = ((outlet.get("location") or {}).get("address")) or {}
        street = address.get("addressLine1")
        city = " ".join(p for p in (address.get("zipCode"), address.get("city")) if p)
        if street:
            out[key] = f"{street}, {city}".strip(", ")
        elif info.get("address"):
            out[key] = clean(info["address"])
    for outlet_id, info in mapping.items():
        key = f"uzh/{info.get('uzhID') or outlet_id}"
        if key not in out and info.get("address"):
            out[key] = clean(info["address"])
    return out


def geocode(query: str) -> tuple[float, float] | None:
    params = urllib.parse.urlencode({"q": query, "format": "jsonv2", "limit": 1, "countrycodes": "ch"})
    req = urllib.request.Request(f"https://nominatim.openstreetmap.org/search?{params}", headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=30) as resp:
        results = json.loads(resp.read())
    if not results:
        return None
    return float(results[0]["lat"]), float(results[0]["lon"])


def main() -> None:
    addresses = {**eth_addresses(), **uzh_addresses()}
    coordinates: dict[str, dict[str, float]] = {}
    cache: dict[str, tuple[float, float] | None] = {}
    for key, address in sorted(addresses.items()):
        query = OVERRIDES.get(address, address)
        if query not in cache:
            cache[query] = geocode(query)
            time.sleep(1.1)
        point = cache[query]
        if point is None:
            print(f"!! {key}: no result for {query!r}")
            continue
        lat, lng = point
        inside = ZURICH_BOX[0] <= lat <= ZURICH_BOX[1] and ZURICH_BOX[2] <= lng <= ZURICH_BOX[3]
        flag = "" if inside else ("  (Basel, expected)" if "Basel" in query else "  !! outside Zürich")
        print(f"{key:8} {lat:.6f},{lng:.6f}  {query}{flag}")
        coordinates[key] = {"lat": round(lat, 6), "lng": round(lng, 6)}
    OUT.write_text(json.dumps(dict(sorted(coordinates.items())), indent=1) + "\n", encoding="utf-8")
    print(f"wrote {OUT.relative_to(ROOT)} ({len(coordinates)} mensas)")


if __name__ == "__main__":
    main()
