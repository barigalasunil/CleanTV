#!/usr/bin/env python3
"""Takes a picture of the TV screen and saves it as a PNG.

The picture is taken on the TV and pulled back as a file: `adb exec-out screencap -p` does not work
on every TV, because some of them translate the line endings on the way out and the picture is then
damaged. The TV also prints a line or two of system noise ("Init wrapper sys mutex successful"),
which does no harm here.

Usage:
    tools/venv/Scripts/python.exe tools/screenshot.py 192.168.1.50:5555 backup/screenshots/before/home.png
"""

import os
import subprocess
import sys
import tempfile

REMOTE = "/data/local/tmp/screenshot.png"


def adb(serial, *args, **kwargs):
    cmd = ["adb"]
    if serial:
        cmd += ["-s", serial]
    cmd += list(args)
    return subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, **kwargs)


def capture(serial):
    """The screen as PNG bytes, or None when the screen could not be read."""
    shot = adb(serial, "shell", "screencap", "-p", REMOTE)
    if shot.returncode != 0:
        print(f"screencap failed: {shot.stderr.decode('utf-8', 'replace').strip()}", file=sys.stderr)
        return None
    # The picture is pulled as a file: reading it back over the shell damages it.
    local = os.path.join(tempfile.gettempdir(), "tvhome-screenshot.png")
    pull = adb(serial, "pull", REMOTE, local)
    if pull.returncode != 0 or not os.path.exists(local):
        print(f"pull failed: {pull.stderr.decode('utf-8', 'replace').strip()}", file=sys.stderr)
        return None
    with open(local, "rb") as handle:
        data = handle.read()
    os.remove(local)
    if not data.startswith(b"\x89PNG"):
        print("the picture came back damaged", file=sys.stderr)
        return None
    return data


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    serial = sys.argv[1]
    target = sys.argv[2] if len(sys.argv) > 2 else "screenshot.png"
    data = capture(serial)
    if data is None:
        return 1
    folder = os.path.dirname(os.path.abspath(target))
    if folder:
        os.makedirs(folder, exist_ok=True)
    with open(target, "wb") as handle:
        handle.write(data)
    print(f"{target}  ({len(data) // 1024} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
