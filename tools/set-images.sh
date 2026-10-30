#!/usr/bin/env bash
# Builds the pushed theme from the owner's pictures and, when TV_HOST is set, uploads it.
#
# Usage: tools/set-images.sh [wallpaper folder] [splash picture]
#        TV_HOST=192.168.1.50 tools/set-images.sh
#        PREPARE_ONLY=1 tools/set-images.sh
#
# With no arguments it reads personal-assets/wallpapers, personal-assets/splash and
# personal-assets/avatar.*, writes personal-assets/theme, and uploads it when TV_HOST is set.
# The originals are never changed and nothing is built into the APK.
set -euo pipefail
cd "$(dirname "$0")/.."
PY=tools/venv/bin/python
[ -x "$PY" ] || PY=tools/venv/Scripts/python.exe
[ -x "$PY" ] || { echo "run: python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt" >&2; exit 1; }

WALL_DIR="${1:-personal-assets/wallpapers}"
SPLASH="${2:-}"
if [ -z "$SPLASH" ]; then
  SPLASH=$(find personal-assets/splash -maxdepth 1 -type f \
    \( -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.png' -o -iname '*.webp' -o -iname '*.bmp' \) 2>/dev/null | sort | head -n 1 || true)
fi

ARGS=(--wallpapers "$WALL_DIR")
[ -n "$SPLASH" ] && ARGS+=(--splash "$SPLASH")
"$PY" -I tools/wallpapers.py "${ARGS[@]}"

if [ "${PREPARE_ONLY:-0}" = 1 ] || [ -z "${TV_HOST:-}" ]; then
  echo "Prepared personal-assets/theme. Set TV_HOST and run again, or: tools/theme.sh personal-assets/theme"
  exit 0
fi
"$PY" -I tools/theme_push.py --source personal-assets/theme
echo "Pushed. Uninstalling com.gathin.tvhome deletes the pushed theme; keep personal-assets/theme as the master copy."
