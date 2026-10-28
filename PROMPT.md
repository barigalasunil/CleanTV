# The prompt

Paste this into Claude Code (or another coding assistant with shell access) on a computer that is
on the same Wi-Fi as the TV. Works on Android TV / Google TV sets (Xiaomi, TCL, Hisense, Sony,
OnePlus, Realme, Nvidia Shield and most others). It will not work on Samsung Tizen, LG webOS or
Fire TV. If you cloned this repo, mention that: the assistant can reuse `tools/` and `launcher/`
instead of writing them from scratch, and only needs to edit `launcher/.../values/config.xml`.

## Working rules for the agent

Follow these for the whole session:

- Run every shell command with a timeout, and never wait silently for longer than 60 seconds.
- Before each step print one short status line saying what is about to happen and whether I need to do anything on the TV.
- After I do something on the TV, re-check the device state immediately and continue by yourself, without waiting for me to type "continue".
- If the same step fails twice, stop, show me the command and its output, and ask. Do not retry in a loop.
- For long builds, show only the last 20 lines of output.
- If a command has produced no output for 2 minutes, say so and ask whether to keep waiting.
- Do not start new features or edit unrelated files without asking.

Before you start, on the TV: Settings → Device Preferences → About → press OK on "Build" 7 times,
then go back → Developer options → turn on USB debugging (and Network/ADB debugging if your TV
shows it). Keep the TV on. Some boxes cut power to their USB port while USB debugging is on, so a
USB speaker or drive plugged into it goes dead until you switch debugging off again.

---

I want to debloat my Android TV and replace its launcher with a clean Apple TV style home screen.
My TV model is: <MODEL NAME HERE>. Work on the real device, step by step, and verify everything you
do with screenshots pulled from the TV. Ask me before anything that depends on my preferences.

Connect
- Find the TV on my network (hostname, ARP table, or ask me for the IP) and connect over ADB on port 5555.
- If the stock adb binary fails with "No route to host" on macOS even though the port is open, that is
  macOS Local Network privacy blocking adb. Don't debug it: use the Python `adb-shell` package in a venv
  as the ADB client instead. If its file push hangs on larger files, transfer files as base64 chunks
  through the shell and verify the md5.
- Tell me when the "Allow USB debugging?" prompt will appear on the TV so I can accept it.
- If `adb install` hangs on Android 14 it is waiting for an on-screen confirmation; push the APK and run
  `pm install -r` over the shell instead.

Back up first
- Save the full package list, disabled-package list, getprop output and all settings to a backup folder
  before changing anything, and write a restore script that re-enables everything you disable.
- Ask me what is plugged into the TV's USB ports. Some devices cut their power while USB debugging
  is on, so warn me, and remind me to switch debugging off once you're done.

Debloat
- List installed packages and sort them into: manufacturer telemetry/analytics, manufacturer content
  and ad services (PatchWall, recommendation rows, content providers), background services I probably
  don't need (share/cast duplicates, cleaners), preloaded apps, and streaming apps.
- Disable the first categories with `pm disable-user --user 0`, never uninstall system packages.
- Never disable what the TV needs to work: the TV app and input services that play HDMI and the
  tuner, the input/source menu, remote control and Bluetooth pairing services, the on-screen
  keyboard, Google Play Store and Play Services. Manufacturer package names often don't say what a
  package does: check its activities and services with `dumpsys package` first, and ask me if it's
  still unclear.
- Keep the network path intact: never disable the network stack (`com.android.networkstack*`, `com.google.android.networkstack*` or a vendor equivalent such as `com.mediatek.network`), the captive portal login app (`com.android.captiveportallogin`), the system WebView, Play Services, or the Chromecast built-in receiver (`com.google.android.apps.mediashell`) unless I tell you I don't cast: on most sets casting dies with that receiver, and its loss looks like a Wi-Fi fault to the user.
- Never leave an HTTP proxy configured. Read `settings get global global_http_proxy_host`, `global_http_proxy_port` and `http_proxy` before and after the debloat: a proxy on a dead address (commonly `127.0.0.1:9`) fails the connectivity check, so the network never validates and most apps report that they cannot connect to Wi-Fi while YouTube and Netflix still play. Clear it with `settings delete global global_http_proxy_host`, `settings delete global global_http_proxy_port` and `settings put global http_proxy :0`, then confirm `VALIDATED` in `dumpsys connectivity`. Never "fix" this with `captive_portal_mode 0`: it hides the symptom and stops real captive portals from opening.
- Before touching streaming apps, voice assistant, Chromecast, Miracast or the update service, ask me
  which ones I use with a checklist question and disable only what I don't tick.
- Run `pm` commands one per shell call, chained commands may silently stop after the first.
- Disable in small batches and screenshot between them: a batch can take something load-bearing with
  it and you cannot tell which.

Launcher
- Check that the TV accepts APKs before building anything. Some China-market sets answer every
  sideload with `INSTALL_FAILED_VERIFICATION_FAILURE` — `adb install`, `pm install`, a hand-started
  installer and even the manufacturer's own APKs all fail — and others refuse any package that
  declares `android.intent.category.HOME` unless it carries the manufacturer's signature. If you
  hit either, do the debloat half, say plainly that the launcher half is impossible here, and stop
  retrying installs. The matching `examples/` header says which case your TV is.
- Don't install a third-party launcher that charges for basics like wallpapers. Build a small native
  Android TV launcher instead (Java, AndroidX RecyclerView, minSdk matching the TV, debug build is fine),
  install it over ADB, set it as the default home and disable the stock launcher. If several apps claim
  the HOME intent, list them with `cmd package query-activities -a android.intent.action.MAIN -c
  android.intent.category.HOME` and set the default with `cmd package set-home-activity`.
- Before disabling the stock launcher, check which other apps claim HOME: on Google TV the setup wizard
  (`com.google.android.tungsten.setupwraith`) takes over even after the stock launcher is disabled, and
  the matching `examples/` header says what to do.
- On Android 11 and newer the launcher must declare `<queries>` for `android.intent.category.LEANBACK_LAUNCHER`,
  or package visibility silently hides every app and the grid comes up empty; target a recent SDK, some
  Google TV 14 sets refuse older targets at install.
- Design, Apple TV 4K style: pitch black background; a dock row at the bottom of the screen inside a
  slightly lighter rounded band holding the four apps I pick (ask me which, and the order); the rest of
  the apps in a 4-column grid below that scrolls up when I press Down; one translucent "glass" highlight
  that springs from tile to tile instead of per-tile borders; the focused tile scales up slightly and
  shows its name underneath; above the dock, the focused dock app's name in large light type with an
  ambient glow in a colour sampled from its banner; frosted pill buttons top right for Inputs, Wi-Fi
  (showing the network name) and Settings; a greeting, a large thin clock and the date top left in a
  downloaded open-licence typeface such as Outfit. Inputs opens a compact glass dropdown under the pill
  listing the HDMI ports, AV and Live TV, switching via the TV input framework.
- Personalise it, and ask me before deciding anything:
  - my name, for the greeting ("GOOD NIGHT, <NAME>") and for the welcome splash;
  - my pincode, so the weather widget knows the place. The launcher asks Open-Meteo for the current
    temperature, weather code and whether it is day; look the coordinates up once (OpenStreetMap
    Nominatim is fine) and write them into `theme/config.json` next to the pincode. The TV only ever talks
    to Open-Meteo. Keep the widget hidden while the coordinates are unknown and ask again: after a
    boot Home starts before the network is up, so the first request fails; retry every few seconds
    for the first minute, and once more when the network comes up.
  - whether the home screen shows a wallpaper behind the tiles, and if so whether several of them
    rotate with a slow crossfade. Put the pictures in `personal-assets/wallpapers/` (git-ignored),
    make a 320x180 preview and a name/mood for each one, and push the theme folder to the TV (see
    the theme step below); the APK only ships the stock pictures in `assets/`.
  - "Show home widgets underneath the pills?" (skippable, on by default): one row with today's
    weather over two cells and a RAM and a storage ring gauge, aligned to the pills' left and right
    edges. Ask which ones I want (`widgets_enabled`, `widget_weather_enabled`,
    `widget_ram_enabled`, `widget_storage_enabled`). Display only: no focus, no key presses, and the
    dock and the tile grid keep their place.
  - a screensaver that shows those same pictures one after another with the time, the date and a
    Now Playing line (while music plays) drifting over them every fifteen seconds (a DreamService),
    and point the TV's screensaver at it over ADB
    (`settings put secure screensaver_components <package>/.WallpaperDream; settings put secure
    screensaver_enabled 1`). Keep a plain clock screensaver too, let any key end either one, and ask
    me which should be the default.
  - make the text readable over any picture: read each wallpaper sparsely (a few hundred points
    instead of every pixel) and switch the greeting, the clock, the date, the button labels and the
    screensaver's clock between light and dark type to match what is under them. It has to flip over
    mid-crossfade, not after it.
  - a welcome splash once per boot, with my name in the message ("Welcome back <NAME>"), my picture
    from `theme/splash.jpg` (falling back to the stock `assets/splash/splash.jpg`), and the message near the
    bottom of the screen, lined up left, centre or right. The message fills the screen width, so set its own gravity for that: the layout gravity
    does nothing on a full-width TextView. Once per boot, not once per
    app start: read the boot count from `Settings.Global.BOOT_COUNT` and keep a flag in the app's
    own storage, with `SystemClock.elapsedRealtime()` as the fallback when the setting is missing.
    Any remote key skips it.
- Let me rearrange the grid: hold OK on a tile to pick it up, move it with the D-pad, OK drops it
  there and Back puts it back. The four dock apps stay where they are. Remember the order in the
  app's own storage and keep the order of apps that were not moved.
- Optionally build a small Live TV app next to the launcher, in its own folder with its own APK:
  plain Java, Media3 ExoPlayer, a list of channels grouped by language, and the picture for each
  channel. The playlists come from the public iptv-org lists (`languages/<code>.m3u` per language),
  are cached on disk and refreshed when they get old. It must stay optional: the launcher works
  without it, and the home screen only shows a Live TV tile when the app is installed.
- Make D-pad navigation deterministic: Up/Down move exactly one row, Up from the dock goes to the top
  bar, the ends of the top bar don't fall through to tiles. Remember the last focused tile.
- Keep it light: the TV has about 1 GB of RAM. No blur, no video, no network polling.

Verify
- After each change, inject remote key presses over ADB, take a screenshot from the TV and look at it.
  Fix what you see before moving on.
- When done, give me a restore script, a build-and-install script, and a README that explains how to
  re-enable any app and how to change the dock order or the wallpaper.

Personalise
- Ask me for my name, pincode, avatar, wallpapers and splash picture, and ask for a name and mood for
  each wallpaper. Build the portable theme folder with `tools/wallpapers.py`: `theme/config.json`,
  optional `theme/avatar.jpg` and `theme/splash.jpg`, `theme/wallpapers/w-01.jpg`, 320x180
  `theme/thumbs/t-01.jpg` and `theme/wallpapers.json`.
- Write the personal answers into `personal-assets/theme/config.json` (git-ignored), not into
  `launcher/.../values/config.xml`, the code or the README. Push the folder with
  `tools/theme_push.py` or `tools/theme.sh`; the launcher reads it from its own external files
  directory and needs no storage permission.
- Keep personal pictures and the theme out of the APK: build the release and check that no personal
  file or hash is inside it. Re-push `personal-assets/theme` after every install, and tell me that
  uninstalling `com.gathin.tvhome` deletes the pushed data.
- Keep the whole thing reversible: the launcher, the Live TV app and every package you disabled
  have to be restorable with the scripts you hand me.

---

Expect to answer three or four checklist questions (which apps you use, which four go in the dock,
whether to keep the voice assistant, Chromecast and firmware updates), plus what the home screen
should call you and which pincode the weather is for. Everything is reversible.
