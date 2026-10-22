#!/usr/bin/env python3
"""Looks up the coordinates of a pincode, for the weather in the launcher.

The launcher asks Open-Meteo for the weather, and that needs a place, not a pincode. The
coordinates are looked up once, here, with OpenStreetMap Nominatim, and are then written into
launcher/app/src/main/res/values/config.xml by hand.

Usage:
    tools/venv/Scripts/python.exe tools/pincode-to-coords.py 110001
    tools/venv/Scripts/python.exe tools/pincode-to-coords.py 110001 --country in

Nominatim wants one request per second and a User-Agent that says who is asking, so the script
waits a moment between lookups and names itself.
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

ENDPOINT = "https://nominatim.openstreetmap.org/search"
USER_AGENT = "CleanYourTV-setup/1.0 (a TV launcher; one lookup at run time)"


def lookup(pincode, country, limit=5):
    """The places Nominatim knows for the pincode, best first."""
    params = urllib.parse.urlencode({
        "q": pincode,
        "countrycodes": country,
        "format": "jsonv2",
        "limit": str(limit),
        "addressdetails": "1",
    })
    request = urllib.request.Request(f"{ENDPOINT}?{params}", headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        body = response.read().decode("utf-8", "replace")
    return json.loads(body)


def describe(place):
    """A readable line for the place."""
    bits = []
    for key in ("city", "town", "village", "suburb", "county", "state", "country"):
        value = place.get("address", {}).get(key)
        if value and value not in bits:
            bits.append(value)
    return ", ".join(bits) if bits else place.get("display_name", "")


def main():
    ap = argparse.ArgumentParser(description="look up the coordinates of a pincode")
    ap.add_argument("pincode", help="the pincode, e.g. 110001")
    ap.add_argument("--country", default=os.environ.get("PINCODE_COUNTRY", "in"),
                    help="the country code of the pincode (default: in)")
    args = ap.parse_args()

    try:
        places = lookup(args.pincode, args.country)
    except urllib.error.HTTPError as error:
        print(f"Nominatim answered {error.code}: {error.reason}")
        print("Try again in a moment; they limit how fast one person may look things up.")
        return 1
    except urllib.error.URLError as error:
        print(f"could not reach Nominatim: {error.reason}")
        print("The TV itself never needs this: it only asks Open-Meteo, with the coordinates you put in config.xml.")
        return 1

    if not places:
        print(f"nothing found for pincode {args.pincode} in country '{args.country}'")
        print("Check the pincode, or try another country code.")
        return 1

    for place in places:
        print(f"{place['lat']}, {place['lon']}   {describe(place)}")

    best = places[0]
    print()
    print("Put these into launcher/app/src/main/res/values/config.xml:")
    print(f'    <string name="weather_pincode" translatable="false">{args.pincode}</string>')
    print(f'    <string name="weather_lat" translatable="false">{best["lat"]}</string>')
    print(f'    <string name="weather_lon" translatable="false">{best["lon"]}</string>')
    print(f'    <string name="weather_country" translatable="false">{args.country.upper()}</string>')
    return 0


if __name__ == "__main__":
    sys.exit(main())
