#!/usr/bin/env bash
# Puts the pictures from personal-assets into the launcher, ready to be built into the APK.
#
# Usage: tools/set-images.sh [wallpaper folder] [splash picture]
#
# With no arguments it uses personal-assets/wallpapers and the first picture in
# personal-assets/splash. Every picture is scaled down to at most 1920x1080 and written as a JPEG
# into launcher/app/src/main/assets; the originals are never changed. The launcher then shows the
# wallpapers one after the other, and the splash picture on the welcome screen.
set -euo pipefail
cd "$(dirname "$0")/.."
PY=tools/venv/bin/python
[ -x "$PY" ] || PY=tools/venv/Scripts/python.exe
[ -x "$PY" ] || { echo "run: python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt" >&2; exit 1; }

WALL_DIR="${1:-personal-assets/wallpapers}"
SPLASH="${2:-}"
if [ -z "$SPLASH" ]; then
  # The first picture in personal-assets/splash, sorted by name.
  SPLASH=$(find personal-assets/splash -maxdepth 1 -type f \
    \( -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.png' -o -iname '*.webp' \) 2>/dev/null | sort | head -n 1)
fi

[ -d "$WALL_DIR" ] || { echo "no such folder: $WALL_DIR" >&2; exit 1; }

ARGS=(--wallpapers "$WALL_DIR" --clean)
[ -n "$SPLASH" ] && ARGS+=(--splash "$SPLASH")
"$PY" -I tools/prepare-images.py "${ARGS[@]}"
echo "Now build and install: launcher/build-and-install.sh"
