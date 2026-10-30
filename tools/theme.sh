#!/usr/bin/env bash
# Push a theme folder to the launcher on the TV and restart Home. A theme is a folder with
# config.json and optionally avatar.jpg, splash.jpg, wallpapers/, thumbs/ and wallpapers.json; see
# themes/README.md for the keys and the layout.
#
# Usage: TV_HOST=192.168.1.50 tools/theme.sh personal-assets/theme
#        TV_HOST=192.168.1.50 tools/theme.sh --reset        # remove the pushed theme
set -euo pipefail
# Resolve the theme folder against the caller's directory before moving into tools/.
THEME_ARG="${1:-}"
case "$THEME_ARG" in --reset|"") ;; /*) ;; *) THEME_ARG="$PWD/$THEME_ARG" ;; esac
cd "$(dirname "$0")"
: "${TV_HOST:?set TV_HOST to the TV IP address, e.g. TV_HOST=192.168.1.50 $0 personal-assets/theme}"
PY=venv/bin/python
[ -x "$PY" ] || PY=venv/Scripts/python.exe
[ -x "$PY" ] || { echo "run: python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt" >&2; exit 1; }
if [ "$THEME_ARG" = --reset ]; then
  PYTHONUTF8=1 "$PY" -I theme_push.py --reset
else
  [ -n "$THEME_ARG" ] || { echo "usage: theme.sh <theme folder> | --reset" >&2; exit 1; }
  PYTHONUTF8=1 "$PY" -I theme_push.py --source "$THEME_ARG"
fi
