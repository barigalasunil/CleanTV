#!/usr/bin/env python3
"""Builds the pushed theme folder from the owner's own pictures.

The originals are never changed. Wallpapers are scaled down to fit 1920x1080 and saved as
wallpapers/w-01.jpg, thumbs are cropped to 320x180 and saved as thumbs/t-01.jpg, and wallpapers.json
records the name and mood shown by the wallpaper picker. The splash and avatar are optional.

Usage:
    tools/venv/Scripts/python.exe tools/wallpapers.py
    tools/venv/Scripts/python.exe tools/wallpapers.py --wallpapers personal-assets/wallpapers \
        --splash personal-assets/splash/one.jpg --avatar personal-assets/avatar.jpg
    tools/venv/Scripts/python.exe tools/wallpapers.py --no-prompt

With no arguments it reads personal-assets/wallpapers, personal-assets/splash, an avatar named
personal-assets/avatar.* and personal-assets/config.json, and writes personal-assets/theme.
"""

import argparse
import glob
import json
import os
import re
import sys

try:
    from PIL import Image, ImageOps
except ImportError:
    print("Pillow is not installed. Run: tools/venv/Scripts/python.exe -m pip install -r tools/requirements.txt")
    sys.exit(1)

MAX_WIDTH = 1920
MAX_HEIGHT = 1080
THUMB_WIDTH = 320
THUMB_HEIGHT = 180
WALLPAPER_QUALITY = 80
SPLASH_QUALITY = 85
AVATAR_SIZE = 512
PICTURES = (".jpg", ".jpeg", ".png", ".webp", ".bmp")


def pictures(folder):
    if not os.path.isdir(folder):
        return []
    return [os.path.join(folder, name) for name in sorted(os.listdir(folder))
            if os.path.isfile(os.path.join(folder, name))
            and name.lower().endswith(PICTURES) and not name.startswith(".")]


def open_picture(path):
    image = Image.open(path)
    image = ImageOps.exif_transpose(image)
    return image.convert("RGB")


def save_jpeg(image, path, quality=WALLPAPER_QUALITY):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path, "JPEG", quality=quality, optimize=True)
    return os.path.getsize(path)


def fit_screen(image):
    image = image.copy()
    image.thumbnail((MAX_WIDTH, MAX_HEIGHT), Image.LANCZOS)
    return image


def fit_thumb(image):
    return ImageOps.fit(image, (THUMB_WIDTH, THUMB_HEIGHT), Image.LANCZOS)


def fit_cover(image, width, height):
    return ImageOps.fit(image, (width, height), Image.LANCZOS)


def clean(folder):
    if not os.path.isdir(folder):
        return
    for name in os.listdir(folder):
        if name.lower().endswith(PICTURES):
            os.remove(os.path.join(folder, name))


def read_index(paths):
    for path in paths:
        if not os.path.isfile(path):
            continue
        try:
            with open(path, encoding="utf-8-sig") as handle:
                data = json.load(handle)
            items = data.get("wallpapers") if isinstance(data, dict) else data
            if isinstance(items, list):
                return [item for item in items if isinstance(item, dict)]
        except Exception as error:
            print(f"ignoring {path}: {error}")
    return []


def default_name(path):
    name = os.path.splitext(os.path.basename(path))[0]
    name = re.sub(r"[_-]+", " ", name).strip()
    return name or "Wallpaper"


def ask(prompt, default):
    if not sys.stdin.isatty():
        return default
    answer = input(f"{prompt} [{default}]: ").strip()
    return answer or default


def find_avatar(value, wallpapers):
    if value:
        return value if os.path.isfile(value) else None
    root = os.path.dirname(os.path.abspath(wallpapers))
    for pattern in ("avatar.jpg", "avatar.jpeg", "avatar.png", "avatar.webp", "avatar.bmp"):
        path = os.path.join(root, pattern)
        if os.path.isfile(path):
            return path
    found = sorted(glob.glob(os.path.join(root, "avatar.*")))
    return found[0] if found else None


def find_splash(value, config):
    if value:
        return value if os.path.isfile(value) else None
    root = os.path.join(os.path.dirname(os.path.abspath(config)), "splash")
    found = pictures(root)
    return found[0] if found else None


def main():
    parser = argparse.ArgumentParser(description="build the pushed theme folder")
    parser.add_argument("--wallpapers", default=os.path.join("personal-assets", "wallpapers"),
                        help="folder with the original wallpaper pictures")
    parser.add_argument("--splash", help="splash picture (default: first in personal-assets/splash)")
    parser.add_argument("--avatar", help="avatar picture (default: personal-assets/avatar.*)")
    parser.add_argument("--config", default=os.path.join("personal-assets", "config.json"),
                        help="the personal config.json to copy")
    parser.add_argument("--output", default=os.path.join("personal-assets", "theme"),
                        help="theme folder to write (default: personal-assets/theme)")
    parser.add_argument("--no-prompt", action="store_true",
                        help="use file-name defaults instead of asking for names and moods")
    args = parser.parse_args()

    sources = pictures(args.wallpapers)
    if not sources:
        print(f"no pictures in {args.wallpapers}", file=sys.stderr)
        return 1
    if not os.path.isfile(args.config):
        print(f"no config.json at {args.config}", file=sys.stderr)
        return 1

    with open(args.config, encoding="utf-8-sig") as handle:
        config = json.load(handle)
    if not isinstance(config, dict):
        print(f"{args.config} must hold a JSON object", file=sys.stderr)
        return 1

    output = os.path.abspath(args.output)
    wall_dir = os.path.join(output, "wallpapers")
    thumb_dir = os.path.join(output, "thumbs")
    os.makedirs(wall_dir, exist_ok=True)
    os.makedirs(thumb_dir, exist_ok=True)
    clean(wall_dir)
    clean(thumb_dir)

    old = read_index([os.path.join(output, "wallpapers.json"),
                      os.path.join(os.path.dirname(os.path.abspath(args.wallpapers)), "wallpapers.json")])
    known = {}
    for item in old:
        source = item.get("source")
        if isinstance(source, str):
            known[os.path.basename(source)] = item

    entries = []
    count = 0
    for source in sources:
        name = os.path.basename(source)
        try:
            with Image.open(source) as original:
                image = ImageOps.exif_transpose(original).convert("RGB")
                target = os.path.join(wall_dir, f"w-{count + 1:02d}.jpg")
                thumb = os.path.join(thumb_dir, f"t-{count + 1:02d}.jpg")
                full_size = save_jpeg(fit_screen(image), target)
                save_jpeg(fit_thumb(image), thumb)
        except Exception as error:
            print(f"skipped {name}: {error}")
            continue

        count += 1
        old_item = known.get(name, {})
        default = old_item.get("name") if isinstance(old_item.get("name"), str) and old_item.get("name").strip() else default_name(name)
        mood = old_item.get("mood") if isinstance(old_item.get("mood"), str) else ""
        if not args.no_prompt:
            default = ask(f"Name for {name}", default)
            mood = ask(f"Mood for {name} (optional)", mood)
        entries.append({
            "file": f"w-{count:02d}.jpg",
            "thumb": f"t-{count:02d}.jpg",
            "name": default.strip() or default_name(name),
            "mood": mood.strip(),
            "source": name,
        })
        print(f"{name} -> w-{count:02d}.jpg + t-{count:02d}.jpg  ({full_size // 1024} KB)")

    config_target = os.path.join(output, "config.json")
    with open(config_target, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(config, handle, ensure_ascii=False, indent=2)
        handle.write("\n")

    index = os.path.join(output, "wallpapers.json")
    with open(index, "w", encoding="utf-8", newline="\n") as handle:
        json.dump({"wallpapers": entries}, handle, ensure_ascii=False, indent=2)
        handle.write("\n")

    splash = find_splash(args.splash, args.config)
    if splash:
        try:
            with Image.open(splash) as original:
                image = ImageOps.exif_transpose(original).convert("RGB")
                size = save_jpeg(fit_cover(image, MAX_WIDTH, MAX_HEIGHT),
                                 os.path.join(output, "splash.jpg"), SPLASH_QUALITY)
            print(f"{os.path.basename(splash)} -> splash.jpg  ({size // 1024} KB)")
        except Exception as error:
            print(f"could not read the splash {splash}: {error}")

    avatar = find_avatar(args.avatar, args.wallpapers)
    if avatar:
        try:
            with Image.open(avatar) as original:
                image = ImageOps.exif_transpose(original).convert("RGB")
                size = save_jpeg(fit_cover(image, AVATAR_SIZE, AVATAR_SIZE),
                                 os.path.join(output, "avatar.jpg"), 88)
            print(f"{os.path.basename(avatar)} -> avatar.jpg  ({size // 1024} KB)")
        except Exception as error:
            print(f"could not read the avatar {avatar}: {error}")

    print(f"{count} wallpaper(s) in {output}")
    print(f"index: {index}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
