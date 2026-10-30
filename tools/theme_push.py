#!/usr/bin/env python3
"""Pushes a theme folder to the launcher's own external files directory.

The launcher reads it from:

    /sdcard/Android/data/com.gathin.tvhome/files/theme/

Nothing is built into the APK. A theme folder holds config.json, optional avatar.jpg and splash.jpg,
wallpapers/ (w-01.jpg, ...), thumbs/ (t-01.jpg, ...) and optional wallpapers.json. The folder is
uploaded to a temporary name, checked file by file, then swapped into place, so a failed upload
leaves the theme that was already on the TV untouched.

Usage:
    tools/venv/Scripts/python.exe tools/theme_push.py --source personal-assets/theme
    tools/venv/Scripts/python.exe tools/theme_push.py --source themes/aurora
    tools/venv/Scripts/python.exe tools/theme_push.py --reset

Set TV_HOST (or pass --host) to the TV IP. --no-restart uploads without restarting Home.
"""

import argparse
import json
import os
import shlex
import sys

PACKAGE = "com.gathin.tvhome"
REMOTE_BASE = f"/sdcard/Android/data/{PACKAGE}/files"
REMOTE_THEME = f"{REMOTE_BASE}/theme"
PICTURES = (".jpg", ".jpeg", ".png", ".webp", ".bmp")
MAX_CONFIG = 128 * 1024
MAX_INDEX = 256 * 1024


def fail(message):
    print(message, file=sys.stderr)
    return 1


def pictures(folder):
    if not os.path.isdir(folder):
        return []
    return [name for name in sorted(os.listdir(folder))
            if os.path.isfile(os.path.join(folder, name))
            and name.lower().endswith(PICTURES) and not name.startswith(".")]


def read_json(path, limit):
    if not os.path.isfile(path):
        return None
    if os.path.getsize(path) > limit:
        raise ValueError(f"{path} is larger than {limit // 1024} KB")
    with open(path, encoding="utf-8-sig") as handle:
        return json.load(handle)


def validate(source):
    config = os.path.join(source, "config.json")
    if not os.path.isfile(config):
        raise ValueError(f"no config.json in {source}")
    if not isinstance(read_json(config, MAX_CONFIG), dict):
        raise ValueError(f"{config} must hold a JSON object")

    index = os.path.join(source, "wallpapers.json")
    if os.path.isfile(index):
        data = read_json(index, MAX_INDEX)
        items = data.get("wallpapers") if isinstance(data, dict) else data
        if not isinstance(items, list):
            raise ValueError(f"{index} needs a wallpapers list (or be a list itself)")
        seen = set()
        for item in items:
            if not isinstance(item, dict):
                raise ValueError(f"{index} has an entry that is not an object")
            for key in ("file", "thumb"):
                value = item.get(key)
                if value is None:
                    continue
                if not isinstance(value, str) or not value.strip():
                    raise ValueError(f"{index}: {key} must be a file name")
                name = value.replace("\\", "/")
                if "/" in name or ".." in name or name.startswith("."):
                    raise ValueError(f"{index}: {key} must be a plain file name: {value}")
            name = item.get("file")
            if not isinstance(name, str) or not name:
                raise ValueError(f"{index}: every entry needs a file")
            if name.lower() in seen:
                raise ValueError(f"{index}: {name} is listed more than once")
            seen.add(name.lower())


def shell_quote(path):
    return shlex.quote(path)


def remote_size(dev, path):
    out = dev.shell(f"stat -c %s {shell_quote(path)} 2>/dev/null || true", read_timeout_s=30.0)
    try:
        return int(out.strip().splitlines()[-1])
    except (ValueError, IndexError):
        return -1


def main():
    parser = argparse.ArgumentParser(description="push a theme folder to the TV")
    parser.add_argument("--source", default=os.path.join("personal-assets", "theme"),
                        help="theme folder (default: personal-assets/theme)")
    parser.add_argument("--host", help="TV IP (default: $TV_HOST)")
    parser.add_argument("--reset", action="store_true", help="remove the pushed theme and restart Home")
    parser.add_argument("--no-restart", action="store_true", help="upload without restarting Home")
    args = parser.parse_args()

    host = args.host or os.environ.get("TV_HOST")
    if not host:
        return fail("set TV_HOST or pass --host <tv ip>")
    os.environ["TV_HOST"] = host
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import tv

    try:
        dev = tv.connect()
    except Exception as error:
        return fail(f"could not connect to {host}: {error}")

    try:
        if args.reset:
            dev.shell(f"rm -rf {REMOTE_THEME}; rm -f {REMOTE_BASE}/config.json {REMOTE_BASE}/wallpaper.jpg",
                      read_timeout_s=60.0)
            print("removed the pushed theme (the stock pictures and config.xml stay)")
            if not args.no_restart:
                restart(dev)
            return 0

        source = os.path.abspath(args.source)
        if not os.path.isdir(source):
            return fail(f"no such theme folder: {source}")
        validate(source)

        staged = REMOTE_THEME + ".new"
        old = REMOTE_THEME + ".old"
        dev.shell(f"rm -rf {staged} {old}; mkdir -p {staged}/wallpapers {staged}/thumbs",
                  read_timeout_s=60.0)

        uploads = []
        uploads.append(("config.json", os.path.join(source, "config.json")))
        for name in ("avatar.jpg", "splash.jpg", "wallpapers.json"):
            path = os.path.join(source, name)
            if os.path.isfile(path):
                uploads.append((name, path))
        for name in pictures(os.path.join(source, "wallpapers")):
            uploads.append((f"wallpapers/{name}", os.path.join(source, "wallpapers", name)))
        for name in pictures(os.path.join(source, "thumbs")):
            uploads.append((f"thumbs/{name}", os.path.join(source, "thumbs", name)))

        total = 0
        for relative, local in uploads:
            remote = f"{staged}/{relative}"
            dev.push(local, remote, transport_timeout_s=300.0, read_timeout_s=300.0)
            expected = os.path.getsize(local)
            actual = remote_size(dev, remote)
            if actual != expected:
                dev.shell(f"rm -rf {staged}", read_timeout_s=30.0)
                return fail(f"upload check failed for {relative}: local {expected} bytes, TV {actual}")
            total += expected

        dev.shell(f"if [ -d {REMOTE_THEME} ]; then mv {REMOTE_THEME} {old}; fi; "
                  f"mv {staged} {REMOTE_THEME}; rm -rf {old}", read_timeout_s=60.0)
        print(f"pushed {len(uploads)} file(s), {total // 1024} KB, to {REMOTE_THEME}")
        print(f"wallpapers: {len(pictures(os.path.join(source, 'wallpapers')))}, "
              f"thumbs: {len(pictures(os.path.join(source, 'thumbs')))}")
        if not args.no_restart:
            restart(dev)
        return 0
    finally:
        dev.close()


def restart(dev):
    dev.shell(f"am force-stop {PACKAGE}; am start -n {PACKAGE}/.MainActivity >/dev/null; echo applied",
              read_timeout_s=60.0)
    print("Home restarted")


if __name__ == "__main__":
    sys.exit(main())
