#!/usr/bin/env python3
"""Puts the wallpapers and the splash picture into the launcher's assets.

Every picture is scaled down to at most 1920x1080 and written as a JPEG, so the APK stays small and
the TV does not have to decode a 4K picture for its background. The pictures get fixed names
(wall-01.jpg, wall-02.jpg, ... and splash.jpg), because the launcher and the docs refer to those.

Usage:
    tools/venv/Scripts/python.exe tools/prepare-images.py --wallpapers personal-assets/wallpapers
    tools/venv/Scripts/python.exe tools/prepare-images.py --wallpapers personal-assets/wallpapers \
        --splash personal-assets/splash/whatever.jpg

The originals are never changed.
"""

import argparse
import os
import sys

try:
    from PIL import Image
except ImportError:
    print("Pillow is not installed. Run: tools/venv/Scripts/python.exe -m pip install -r tools/requirements.txt")
    sys.exit(1)

MAX_WIDTH = 1920
MAX_HEIGHT = 1080
QUALITY = 80
PICTURES = (".jpg", ".jpeg", ".png", ".webp", ".bmp")

CREDITS_HEADER = """# Wallpaper credits

Every picture in this folder is part of the launcher and goes out with the app, so it ends up in
the APK. Only use pictures you have the right to share: do not use personal photos, and check what
the terms of the site you took a picture from say.

Write one line per picture: the file name, where the picture came from, and its licence
(for example "own photo, no licence needed" or "example.com, CC BY 4.0, author name").

| File | Source | Licence |
| ---- | ------ | ------- |
"""


def pictures_in(folder):
    """The picture files in a folder, sorted by name."""
    if not os.path.isdir(folder):
        return []
    names = [n for n in sorted(os.listdir(folder))
             if os.path.isfile(os.path.join(folder, n)) and n.lower().endswith(PICTURES)]
    return names


def fit(image, width=MAX_WIDTH, height=MAX_HEIGHT):
    """Crops to the screen's shape and scales down: the launcher shows the picture full screen."""
    w, h = image.size
    if w <= 0 or h <= 0:
        raise ValueError("the picture has no size")
    scale = max(width / w, height / h)
    new_w = max(width, round(w * scale))
    new_h = max(height, round(h * scale))
    image = image.resize((new_w, new_h), Image.LANCZOS)
    left = (new_w - width) // 2
    top = (new_h - height) // 2
    return image.crop((left, top, left + width, top + height))


def save_jpeg(image, path):
    image.convert("RGB").save(path, "JPEG", quality=QUALITY, optimize=True)
    return os.path.getsize(path)


def main():
    ap = argparse.ArgumentParser(description="prepare the launcher pictures")
    ap.add_argument("--wallpapers", help="folder with the wallpaper pictures")
    ap.add_argument("--splash", help="the picture for the welcome splash")
    ap.add_argument("--assets", default=os.path.join("launcher", "app", "src", "main", "assets"),
                    help="the launcher's assets folder")
    ap.add_argument("--clean", action="store_true", help="remove the wallpapers that are there first")
    args = ap.parse_args()

    wall_dir = os.path.join(args.assets, "wallpapers")
    splash_dir = os.path.join(args.assets, "splash")
    os.makedirs(wall_dir, exist_ok=True)
    os.makedirs(splash_dir, exist_ok=True)

    if args.clean and os.path.isdir(wall_dir):
        for name in os.listdir(wall_dir):
            if name.lower().endswith(PICTURES):
                os.remove(os.path.join(wall_dir, name))

    lines = []
    count = 0
    if args.wallpapers:
        names = pictures_in(args.wallpapers)
        if not names:
            print(f"no pictures in {args.wallpapers}")
        for name in names:
            source = os.path.join(args.wallpapers, name)
            target = os.path.join(wall_dir, f"wall-{count + 1:02d}.jpg")
            try:
                with Image.open(source) as image:
                    size = save_jpeg(fit(image), target)
            except Exception as error:      # a broken picture must not stop the rest
                print(f"skipped {name}: {error}")
                continue
            count += 1
            lines.append(f"wall-{count:02d}.jpg | {name} | TO FILL IN")
            print(f"{name} -> wall-{count:02d}.jpg  ({size // 1024} KB)")

    credits = os.path.join(wall_dir, "CREDITS.md")
    if not os.path.exists(credits):
        with open(credits, "w", encoding="utf-8") as handle:
            handle.write(CREDITS_HEADER)
            for line in lines:
                handle.write(f"| {line} |\n")
        print(f"wrote {credits} with a row per picture; fill in the sources and licences")

    if args.splash:
        try:
            with Image.open(args.splash) as image:
                size = save_jpeg(fit(image), os.path.join(splash_dir, "splash.jpg"))
            print(f"{os.path.basename(args.splash)} -> splash.jpg  ({size // 1024} KB)")
        except Exception as error:
            print(f"could not read {args.splash}: {error}")
            return 1

    print(f"{count} wallpaper(s) in {wall_dir}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
