<h1 align="center">CleanTV</h1>

<p align="center">
  <a href="https://github.com/barigalasunil/CleanTV">CleanTV</a> ·
  <a href="PROMPT.md">The prompt</a> ·
  <a href="examples/">30 TV lists</a> ·
  <a href="SHOWCASE.md">Made with it</a>
</p>

<p align="center">
  <a href="https://github.com/barigalasunil/CleanTV"><img src="https://img.shields.io/github/stars/barigalasunil/CleanTV?style=flat&color=cfffcf&labelColor=15181e" alt="Stars"></a>
  <a href="https://github.com/barigalasunil/CleanTV/pulls?q=is%3Amerged"><img src="https://img.shields.io/github/issues-pr-closed/barigalasunil/CleanTV?style=flat&label=merged%20PRs&color=cfffcf&labelColor=15181e" alt="Merged PRs"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/licence-MIT-cfffcf?style=flat&labelColor=15181e" alt="MIT"></a>
</p>

Strip the bloatware from any Android TV or Google TV over Wi-Fi, and replace the ad-filled home
screen with a clean launcher that looks like an Apple TV. **No root, no cable, nothing uninstalled,
and a one-line restore if you change your mind.**

> Vibe coded my parents' TV this afternoon! It was showing a sponsored ad before you could even get
> to Netflix. Gave Claude Code the model number, it found the TV on wifi, removed 25 Xiaomi packages
> and wrote a new launcher. Looks like an Apple TV now lol.
>
> [@gathint, 17 October 2026](https://x.com/gathint/status/2111379569030939102). The replies are where
> most of the TV lists here came from. The repo was called tv-vibehack until the 19th; old links work.

## What the original project gives you

These come from [cleanyourtv (formerly tv-vibehack)](https://github.com/Gathin23/cleanyourtv) by [@Gathin23](https://github.com/Gathin23).

| Feature | What it gives you |
|---|---|
| **ADB debloat over Wi-Fi** | Disables the manufacturer's telemetry, ads and preloaded apps with `pm disable-user`; nothing is uninstalled. |
| **Backup and restore** | The prompt records the package list and settings first; the restore script re-enables every disabled package and puts the stock launcher back. |
| **tvOS-style launcher** | A pitch-black home with a dock of your favourite apps, a top shelf, the app grid and a floating glass highlight. |
| **Glowing focus and frosted surfaces** | An ambient glow in the focused app's colour, plus frosted glass for the top bar and menus. |
| **Clock and clock screensaver** | A thin clock, the date and a drifting clock screensaver in Outfit. |
| **Themes** | Change colours, wallpaper, dock and columns from a `config.json`, without rebuilding. |
| **Module cards** | Other TV apps can offer their own tile, only after you approve it. |
| **Per-TV lists** | Ready-made debloat lists for the models in [`examples/`](examples/), grouped and commented. |
| **A quieter TV** | No sponsored rows, no "Discover" nags and no analytics phoning home. |

## What I added on top

| Feature | What it gives you |
|---|---|
| **Motion system** | One set of durations, easing curves and spring values for every animation, scaled by the TV's own animator-speed setting. |
| **Dock of five** | Five dock slots, set by `columns` (default `5`), with tiles that keep their design size at four columns and shrink to fit from five up. |
| **Personal data pushed to the TV** | Your name, avatar, splash and wallpapers live in the app's external `theme/` folder, never in the APK; the install scripts re-push the local master copy after an install. |
| **Avatar pill and control panel** | One top-right pill with your avatar and optional time opens Inputs, network, Settings and Wallpapers; Back closes it and returns focus to the pill. |
| **Wallpaper picker** | Every pushed wallpaper, grouped by mood, with Rotate all, rotate one mood, or pin one picture; the choice is saved and applied at once. |
| **Personal greeting and welcome splash** | Your name in the greeting and the once-per-boot splash, skipped by any remote key. |
| **Home widgets** | One display-only row under the avatar pill: today's weather over two cells, then RAM and storage ring gauges, on dark glass. |
| **Adaptive text contrast** | The clock, greeting, date and pill text switch between light and dark to stay readable on every picture. |
| **Now Playing card** | In the widget band while something plays: art, title, artist and previous/play/next. |
| **Tile menu and app grid reordering** | Hold OK on a tile for move, hide, unhide and app info; the dock stays fixed. |
| **Wallpaper screensaver** | The same wallpapers in the screensaver, with the time, date and Now Playing line drifting over them. |
| **Live TV app** | An optional Media3 player with channels grouped by language, filtered by the languages you pick. |
| **First-connection helper** | [`tools/connect.sh`](tools/connect.sh) waits for the TV and explains an unauthorised, offline or wrong address instead of hanging. |
| **Image and personalisation tools** | [`tools/set-images.sh`](tools/set-images.sh) builds and pushes the theme, and [`tools/pincode-to-coords.py`](tools/pincode-to-coords.py) looks up the weather coordinates. |
| **Tests** | JVM tests for the parsers, order, motion maths and wallpaper choice, plus [`tools/check-links.py`](tools/check-links.py) for the documentation's local links. |

## How it works

Android TVs have the same debug bridge as Android phones. Switch on USB debugging in the TV's
developer options, allow your computer once, and the rest is standard Android tooling:

1. **Back up** the list of installed packages and settings.
2. **Disable** the manufacturer's telemetry, ad services, content rows and preloaded junk with
   `pm disable-user`. Nothing is uninstalled, so a restore script can undo every step.
3. **Install** a small native launcher and make it the default home screen.

The bloat differs by brand, so the prompt has your coding assistant discover it on your TV and ask
before touching anything you might use.

## Quick start

You need a computer on the same Wi-Fi as the TV and a coding assistant with shell access
(Claude Code or similar). Clone this repository first:

```bash
git clone https://github.com/barigalasunil/CleanTV.git
cd CleanTV
```

| | |
|---|---|
| **1. On the TV** | Settings → Device Preferences → About → press OK on **Build** seven times → back → Developer options → **USB debugging** on. If you see a Network or ADB debugging toggle, turn that on too. Keep the TV on. |
| **2. On your computer** | Paste [`PROMPT.md`](PROMPT.md) into your assistant and fill in your TV model. |
| **3. Follow along** | Accept "Allow USB debugging?" on the TV, then answer the checklist: which apps you use, which five go in the dock, whether to keep the voice assistant, Chromecast and updates. |

The assistant backs up, debloats, builds and installs the launcher, and checks each step with
screenshots pulled from the TV. With this repo as context it takes about fifteen minutes.

## Supported TVs

Anything running **Android TV** or **Google TV**. Samsung (Tizen) and LG (webOS) run different
systems. **Fire TV** takes the debloat half only,
since Fire OS will not hand Home to a third-party launcher. A few China-market TCL sets refuse to
install any launcher at all; their lists say so in the header.

Tested lists from contributors, one folder per model in [`examples/`](examples/):

**Xiaomi** 8 · **TCL** 7 · **Sony** 2 · BenQ · CHiQ · Chromecast with Google TV · Hisense · Insignia Fire TV ·
NVIDIA Shield · OnePlus · Philips · RCA · SANYO · Toshiba Fire TV · Stellamore and XGIMI projectors

<details>
<summary><strong>The full list</strong></summary>

| TV | OS |
|---|---|
| BenQ E55-735 (Changhong AI PONT, DVB96BQ) | Google TV 14 |
| CHiQ AI PONT (DVB93CQ) | Android TV 11 |
| Chromecast with Google TV 4K (sabrina) | Google TV 14 |
| Hisense SmartTV 4K FFM (huangshan, MediaTek) | Android TV 10 |
| Insignia 55" F30 4K Fire TV (NS-55F301NA25, AFTDCT31, duckie) | Fire OS 7 / Android 9 |
| NVIDIA SHIELD TV Pro 2019 (P2897, mdarcy) | Android TV 11 |
| OnePlus TV (D00, Dubai) | Google TV 14 |
| Philips Smart 4K TV (Skyworth, eleonas, MediaTek), India | Google TV 14 |
| RCA AI PONT SA (Changhong, redwood, ISDB92RC; also sold as realme 40-inch) | Android 14 |
| SANYO ATV R2 (SCBC, Realtek RTD285O) | Android TV 9 |
| Sony BRAVIA 4K UR2 (BRAVIA_UR2_4K, MediaTek) | Android TV 10 |
| Sony BRAVIA XBR-65X900E (2017, BRAVIA_ATV2) | Android TV 9 |
| Stellamore T950S projector (Droidlogic be30a3, Amlogic txhd2) | Android 13 |
| TCL 55" 4K Google TV, 2023 (G10, G10_4K_US_NF) | Google TV 12 |
| TCL 65C725 (Smart TV Pro, BeyondTV4) | Android TV 11 |
| TCL 55Q10G Pro (China, tcl_mt5879_cn, MediaTek MT9952) | Android TV 11 |
| TCL ak30a5 (China, Amlogic) | Android TV 9 |
| TCL BeyondTV (India) | Android TV 9 |
| TCL QM6K Pro, 2025 (G08, MediaTek tcl9618) | Google TV 14 |
| TCL Smart TV Pro (G05, G05_4K_US, MediaTek MT5889) | Google TV 11 |
| Toshiba Fire TV (AFTHA004, hazel) | Fire OS 7 / Android 9 |
| XGIMI Horizon S Pro projector (XK03H, xgimigalileo, MediaTek) | Android TV 11 |
| Xiaomi Mi Box S (MIBOX4, oneday), streaming box | Android TV 9 |
| Xiaomi Mi TV (MiTV-MOOQ0, croods) | Android TV 10 |
| Xiaomi Mi TV 4A (MiTV-MOOQ1) | Android TV 10 |
| Xiaomi MiTV-AESP0 stick (aquaman, 1 GB) | Android TV 9 |
| Xiaomi MiTV-AXSO0 (dangal; Mi TV 4C Pro 2019 / 4A Pro) | Android TV 9 |
| Xiaomi MiTV-MOOR2 (venom, MediaTek) | Google TV 11 |
| Xiaomi MiTV-MSSP3 (machuca, MediaTek) | Android TV 11 |
| Xiaomi TV A Pro 75" 2026 (MiTV-MZTU0, river, MediaTek) | Google TV 14 |

</details>

Your TV not here? The prompt discovers the bloat itself. Add your list afterwards and the next
person with your model skips that step: [CONTRIBUTING.md](CONTRIBUTING.md).

## Doing it by hand

Everything the assistant does, you can run yourself. The tools need Python 3 and nothing else.

```bash
python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt
export TV_HOST=192.168.1.50                                   # your TV's IP address

tools/connect.sh                                              # wait for the TV, with a clear error
tools/venv/bin/python -I tools/tv.py --probe                 # model, Android version, firmware
tools/venv/bin/python -I tools/netcheck.py                   # read-only: does the network validate?
tools/venv/bin/python -I tools/tv.py 'pm list packages'      # one shell command on the TV
tools/disable.sh examples/xiaomi-mi-tv-4a/debloat.txt        # apply a debloat list
tools/restore.sh examples/xiaomi-mi-tv-4a/debloat.txt        # undo it, stock launcher back
TV_HOST=192.168.1.50 launcher/build-and-install.sh           # build the launcher and make it Home
LIVE_TV=1 TV_HOST=192.168.1.50 launcher/build-and-install.sh # ... and the Live TV app with it
tools/theme.sh themes/aurora                                 # push a theme; --reset removes it
tools/set-images.sh                                           # build personal-assets/theme and push it
tools/venv/bin/python -I tools/pincode-to-coords.py 110001   # coordinates for the weather pill
tools/venv/bin/python -I tools/screenshot.py 192.168.1.50:5555 shot.png
```

| Tool | What it does |
|---|---|
| [`tools/tv.py`](tools/tv.py) | Pure-Python ADB client: shell, batch, push, pull. Works where the stock `adb` binary is blocked. |
| [`tools/b64push.py`](tools/b64push.py) | Uploads large files such as APKs in base64 chunks and verifies the checksum. |
| [`tools/connect.sh`](tools/connect.sh) | Waits for the TV to answer on the network, up to a minute, and says what is wrong (unauthorised, offline, wrong IP) instead of hanging. |
| [`tools/disable.sh`](tools/disable.sh), [`restore.sh`](tools/restore.sh) | Apply or undo a debloat list. |
| [`tools/theme.sh`](tools/theme.sh) | Push a theme folder to the TV and restart Home; `--reset` removes the pushed theme. |
| [`tools/set-images.sh`](tools/set-images.sh) | Builds `personal-assets/theme` from your pictures (1920x1080 wallpapers, 320x180 previews) and uploads it when `TV_HOST` is set. |
| [`tools/pincode-to-coords.py`](tools/pincode-to-coords.py) | Turns a pincode into the coordinates the weather pill needs. |
| [`tools/screenshot.py`](tools/screenshot.py) | Takes a picture of the TV screen; some TVs damage the picture when it comes over the shell. |
| [`tools/netcheck.py`](tools/netcheck.py) | Read-only network health check: reports the network's `VALIDATED` state, captive-portal state, any configured HTTP proxy and disabled network packages. Exit 0 healthy, 1 a problem, 2 no connection. |
| [`launcher/build-and-install.sh`](launcher/build-and-install.sh) | Builds a shrunk release (about 2.2 MB with the bundled stock pictures), installs it, switches Home, and rolls back if Home does not land on it. |

Every script has a PowerShell twin next to it. On Windows with Android Studio installed nothing
else is needed:

```powershell
python -m venv tools\venv; tools\venv\Scripts\python.exe -m pip install -r tools\requirements.txt
powershell -ExecutionPolicy Bypass -File launcher\build-and-install.ps1 -TvHost 192.168.1.50
powershell -ExecutionPolicy Bypass -File tools\disable.ps1 -TvHost 192.168.1.50 examples\xiaomi-mi-tv-4a\debloat.txt
```

The install scripts ask the TV which launcher owns Home and only disable one they recognise
(`tvlauncher` or `launcherx`); pass `STOCK_LAUNCHER=<package>` (`-StockLauncher`) to override, or
`none` to leave it alone.

## The launcher

A small native Android TV app (`com.gathin.tvhome`, about 2.2 MB installed, mostly the wallpapers and
the splash picture) in plain Java with
AndroidX RecyclerView and nothing heavier. Built for sets with 1 GB of RAM: no blur, no video, and
the weather is its only network request. Background work is limited to the screensavers, the opt-in
media listener and the optional remote-key services. It needs JDK 17 and Android SDK platform 34 to
build; on macOS,
`brew install openjdk@17 android-commandlinetools` and `sdkmanager "platforms;android-34" "build-tools;34.0.0"`.

- **Configure** in [`config.xml`](launcher/app/src/main/res/values/config.xml): dock order, hidden apps, the Live TV package, columns (tiles shrink from 5 up), rows under the dock, an optional VPN app for the network pill.
- **Personalise** through the pushed theme: `theme/config.json` holds your name, the place the weather is for and the splash message; `theme/avatar.jpg`, `theme/splash.jpg` and `theme/wallpapers/` hold your pictures. The personal answers live in the git-ignored `personal-assets/` folder and on the TV, not in `config.xml` or the APK.
- **Themes** set the same keys plus a background colour and their own pictures from a folder pushed to the TV, no rebuild. Three ship in [`themes/`](themes/), documented there.
- **Language** follows the TV: English, German, Spanish, Catalan, French, Russian, Simplified Chinese, Thai. Android 13+ can set it for the launcher alone: `tools/tv.py 'cmd locale set-app-locales com.gathin.tvhome --locales ca'`.
- **Module cards** let any TV app declare its own tile (label, caption, glow). The launcher asks you once per app, Keep hidden by default, and remembers the answer for that app's signing key and card name. Hold OK on Settings to change your mind. An app declares a card with a `com.gathin.tvhome.category.DOCK_CARD` filter and a `com.gathin.tvhome.card` XML resource (`spec="1"`, `label`, optional `caption` and `accent="#RRGGBB"`) on the activity that already has the LEANBACK_LAUNCHER filter.
- **Screensavers**: a drifting clock (`ClockDream`), or the wallpapers one after another with the time and the date drifting over them (`WallpaperDream`). Pick one in the TV's screensaver settings, or over ADB:
  `tools/tv.py 'settings put secure screensaver_components com.gathin.tvhome/.WallpaperDream; settings put secure screensaver_enabled 1'`
  (swap in `.ClockDream` for the clock). Any remote key ends it.
- **Remote keys**: an optional accessibility service keeps the remote's Input button working on Google TV (`INPUT_KEY_SERVICE=1` at install), another fixes sticks whose Home key sends ALL_APPS (`HOME_KEY_SERVICE=1`).
- **Navigation** is deliberate: Up and Down move one row, the avatar pill never falls through to tiles, the last focused app is remembered, and installing or removing an app refreshes the tiles while Home is open.

### Personalisation

Defaults live in [`res/values/config.xml`](launcher/app/src/main/res/values/config.xml). A pushed
`theme/config.json` may override the same keys; every key is optional, and a missing or badly typed
one keeps the `config.xml` value.

| Key | Default | Set in | What it does |
|---|---|---|---|
| `dock_apps` | Netflix, Prime Video, YouTube, Hotstar | `config.xml`, `theme/config.json` | Dock order; the first `columns` apps fill the dock, missing packages are skipped. |
| `hidden_apps` | `com.mediatek.wwtv.tvcenter`, `com.mitv.gallery` | `config.xml`, `theme/config.json` | Packages never shown as tiles. |
| `columns` | `5` | `config.xml`, `theme/config.json` | Tiles per row and dock slots, `1`-`8`. |
| `rows_below_dock` | `0` | `config.xml`, `theme/config.json` | Grid rows visible under the dock, `0`-`3`. |
| `module_cards` | `true` | `config.xml`, `theme/config.json` | Look for apps that declare a module card and ask once. |
| `show_home_settings_shortcut` | `false` | `config.xml`, `theme/config.json` | Add a "Default home" tile when the TV has a home-screen picker. |
| `greeting_name` | empty | `config.xml`, `theme/config.json` | Name in the greeting and splash. |
| `pill_show_time` | `true` | `config.xml`, `theme/config.json` | Show the time beside the avatar; `false` leaves the avatar alone. |
| `use_wallpaper` | `true` | `config.xml`, `theme/config.json` | Draw a wallpaper behind the tiles. |
| `wallpaper_interval_seconds` | `30` | `config.xml`, `theme/config.json` | Seconds per home wallpaper, `5`-`86400`. |
| `screensaver_enabled` | `true` | `config.xml`, `theme/config.json` | Let the wallpaper screensaver show the pictures. |
| `screensaver_interval_seconds` | `30` | `config.xml`, `theme/config.json` | Seconds per screensaver picture, `5`-`3600`. |
| `splash_enabled` | `true` | `config.xml`, `theme/config.json` | Show the welcome splash once per boot. |
| `splash_message` | `Welcome back %s` | `config.xml`, `theme/config.json` | Splash text; `%s` is `greeting_name`. |
| `splash_duration_ms` | `4000` | `config.xml`, `theme/config.json` | Hold time before it fades, `500`-`30000`; any key skips it. |
| `splash_text_bottom_percent` | `8` | `config.xml`, `theme/config.json` | Distance from the bottom edge, `0`-`60`. |
| `splash_text_align` | `center` | `config.xml`, `theme/config.json` | `center`, `left` or `right`. |
| `widgets_enabled` | `true` | `config.xml`, `theme/config.json` | The whole home widget row. |
| `widget_weather_enabled` | `true` | `config.xml`, `theme/config.json` | Today's weather; hidden on its own when no coordinates are set. |
| `widget_ram_enabled` | `true` | `config.xml`, `theme/config.json` | RAM ring gauge. |
| `widget_storage_enabled` | `true` | `config.xml`, `theme/config.json` | Storage ring gauge. |
| `weather_pincode` | empty | `config.xml` | Kept for reference; the TV never reads or sends it. |
| `weather_lat`, `weather_lon` | empty | `config.xml`, `theme/config.json` | Coordinates sent to Open-Meteo. |
| `weather_country` | `IN` | `config.xml` | Country code used once by the pincode lookup. |
| `vpn_app` | empty | `config.xml`, `theme/config.json` | App the network pill opens; empty opens Wi-Fi settings. |
| `shamsi_date` | `false` | `config.xml`, `theme/config.json` | Show the Solar Hijri date above the Gregorian one. |
| `live_tv_enabled` | `true` | `config.xml`, `theme/config.json` | Pin the optional Live TV app as the first grid tile when installed. |
| `live_tv_app_package` | `com.gathin.livetv` | `config.xml`, `theme/config.json` | Package of that app. |
| `live_tv_languages` | `tel`, `hin`, `eng` | `config.xml`, `theme/config.json` | Playlists the Live TV app loads. |
| `live_tv_package` | `com.mediatek.wwtv.tvcenter` | `config.xml`, `theme/config.json` | The TV's own tuner app, offered in Inputs; empty omits it. |
| `live_tv_action` | empty | `config.xml`, `theme/config.json` | Action sent to that tuner app when it has no launch intent. |
| `source_panel_service` | empty | `config.xml`, `theme/config.json` | Fallback TV source panel service, `component` first. |
| `source_panel_action` | empty | `config.xml`, `theme/config.json` | Action for that fallback panel. |
| `input_action` | empty | `config.xml`, `theme/config.json` | Action sent with the input URI when no app answers `ACTION_VIEW`. |
| `background_color` | `#000000` | `theme/config.json` | Opaque background colour behind everything. |

The RAM widget shows memory in use against the total, and **memory in use includes caches** (it is
the same reading the TV's own settings show). The gauges turn amber from 75% and red from 90%.

The pictures live in `personal-assets/wallpapers/`, `personal-assets/splash/` and an optional
`personal-assets/avatar.*` (all git-ignored). `tools/set-images.sh` builds the pushed theme at
`personal-assets/theme/` (1920x1080 JPEG wallpapers, 320x180 previews, `wallpapers.json` with names
and moods) and uploads it when `TV_HOST` is set. The APK only ever ships the stock pictures in
`launcher/app/src/main/assets/`; the theme is the app's own external data and needs no storage
permission. Uninstalling the launcher deletes that data, so keep `personal-assets/theme` as the
master copy.

The splash shows **once per boot**, not once per app start: the launcher compares the boot count
from `Settings.Global.BOOT_COUNT` with the one it remembers, and falls back to
`SystemClock.elapsedRealtime()` when the setting is missing. `splash_text_align` puts the message
where you want it, and any remote key skips the splash altogether.

Each wallpaper is read as a sparse grid of a few hundred points when it comes in, and the
brightness under a piece of text picks its colour: white on a dark picture, near-black on a bright
one, with a shade only where the picture is dark. The clock, the greeting, the date, the buttons and
the screensaver's clock all follow the same rule, and the colour crossfades with the picture.

The weather widget comes from [Open-Meteo](https://open-meteo.com), which needs no key and no
account. One request asks for the current temperature, the weather code and whether it is day, and
the answer is cached and refreshed every half an hour; offline, the last reading stays up and a
failure is silent. `tools/pincode-to-coords.py` turns your pincode into the coordinates it needs,
once, on your computer.

The widget is hidden while the coordinates are unknown: at boot Home starts before the TV's network
is up, so the first request fails and the launcher retries every ten seconds for the first minute,
and once more when the network comes up. If the widget stays hidden, check the coordinates: a wrong
or no internet is the usual cause.

### Control panel and avatar pill

The top right is one frosted pill, not a row of buttons. Its circle shows `theme/avatar.jpg` when
the pushed theme has one, otherwise the first letter of `greeting_name`, otherwise a person glyph.
The time beside it is hidden when `pill_show_time` is `false`.

Pressing OK on the pill opens the control panel under it:

| Row | What it does |
|---|---|
| **Inputs** | Opens the HDMI/AV list. The row only appears when the TV exposes an input, a Live TV app or a source panel. |
| **Network** | Shows Wi-Fi, Ethernet, offline, or the VPN state. OK opens the configured `vpn_app` when it is installed, otherwise Wi-Fi settings; hold for Wi-Fi settings. |
| **Settings** | Opens Android settings. Hold it for the Module cards list when `module_cards` is on. |
| **Wallpapers** | Opens the wallpaper picker. |

Back closes the panel with the same fade-and-shrink motion every menu uses, then puts focus back on
the pill. The widget row hangs under the pill's right edge in its own band: four 81 dp cells with
three 10 dp gaps, the weather taking two cells, RAM and storage one each. The Now Playing card
shares that band, so showing it moves nothing else.

### Wallpaper picker

Choose **Wallpapers** in the control panel. The picker lists every picture in `theme/wallpapers/`
that `wallpapers.json` names, or every picture in that folder when the JSON file is missing. It
groups them by mood, puts pictures without a mood under **Other**, and offers three modes:

- **Rotate all** - cycle through every picture.
- **Rotate one mood** - cycle through the pictures with that mood.
- **Pin one picture** - keep that picture on screen.

The saved choice lives in the launcher's own `wallpapers` preferences (`mode`, `mood`, `key`), so
it survives a restart; an old single-picture choice is read as a pin. The picker and the rotation
never list more than **20** pictures. `wallpapers.json` accepts a bare list or a
`{"wallpapers": [...]}` wrapper; `file` is required, and `thumb`, `name` and `mood` are optional:

```json
{"wallpapers": [
  {"file": "w-01.jpg", "thumb": "t-01.jpg", "name": "Morning", "mood": "Calm"},
  {"file": "w-02.jpg", "thumb": "t-02.jpg", "name": "City", "mood": "Moody"}
]}
```

With no pushed pictures the picker falls back to the stock pictures bundled in the APK. If there are
none at all, it shows "No wallpapers in the theme" and changes nothing.

### Personal data pushed to the TV

Your personal files are not in the APK. They are pushed to the launcher's own external folder:

```text
/sdcard/Android/data/com.gathin.tvhome/files/theme/
  config.json          # optional overrides for the keys in config.xml
  avatar.jpg            # optional, shown in the control pill
  splash.jpg            # optional welcome splash picture
  wallpapers.json       # optional names, moods and previews
  wallpapers/           # w-01.jpg, w-02.jpg, ...
  thumbs/               # t-01.jpg, t-02.jpg, ...
```

Build the folder from your originals, then push it:

```powershell
tools\set-images.ps1 -TvHost 192.168.1.50
```

```bash
TV_HOST=192.168.1.50 tools/set-images.sh
```

`set-images` reads `personal-assets/wallpapers/`, `personal-assets/splash/`, an optional
`personal-assets/avatar.*` and `personal-assets/config.json`, writes `personal-assets/theme/`, and
uploads it when a host is given. To push an already built folder, use `tools\theme.ps1` or
`tools/theme.sh`; `-Reset` (`--reset` on macOS/Linux) removes it and returns the launcher to the
stock pictures.

The APK always ships only the stock pictures in `launcher/app/src/main/assets/`, so a build with no
`personal-assets/theme` is a stock-only build: the stock splash stays, with no name, avatar or personal
wallpaper.
`launcher/build-and-install.*` re-pushes `personal-assets/theme` automatically after an install when
that folder exists, and leaves the stock set in place when it does not. Uninstalling
`com.gathin.tvhome` deletes the app's external folder, so keep `personal-assets/theme` as your master
copy and re-push it after a reinstall.

### Dock of five

The dock has as many slots as there are columns. `columns` is `5` by default, so the first row holds
five apps; `dock_apps` sets their order and is read from `res/values/config.xml` or from
`theme/config.json`. The stock list names four apps (Netflix, Prime Video, YouTube, Hotstar), so the
fifth slot is filled by the next app in the grid order (Live TV when installed, then module cards,
then alphabetical). Missing packages are skipped. The dock has no labels and is fixed: hold OK on a
dock tile does nothing.

Tiles are designed at 180x101 dp. Four columns use that full size; from five columns up they shrink
to fit the cell, about 144 dp at five and 117 dp at six on a 1080p TV, with a floor of 80 dp.

### Motion system

Every animation takes its timing from `Motion.java`:

- **FAST 160 ms** for a colour or alpha change and a highlight landing.
- **MEDIUM 260 ms** for a tile taking focus and a panel opening.
- **SLOW 420 ms** for something arriving on screen.
- **Ease-out** (`DecelerateInterpolator(1.8f)`) for starting and settling, a gentle
  **overshoot** (`OvershootInterpolator(1.15f)`) only for focus landing, and an accelerating curve
  for menus closing.
- One spring for the highlight and previews: stiffness `260`, damping ratio `0.60`.

The TV's **Developer options -> Animator duration scale** is read at `onResume` and when the
screensaver starts, and is never written. At `0.5` the designed duration is divided by `0.5`, which
keeps the designed speed; at `0` animations are off and every change jumps to its end state; a value
above `1` is treated as `1`, so the TV is never told to run faster than designed. A missing or
unreadable setting behaves like `1`.

## The Live TV app

An optional second app in [`apps/live-tv`](apps/live-tv): a small plain-Java Android TV player
built on Media3 ExoPlayer, with thousands of channels grouped by language, a picture for each
channel and a settings screen for the languages.

It stays optional: the launcher works without it, and the home screen only shows the Live TV tile
when the app is installed. Build and install it with the launcher in one go:

```bash
LIVE_TV=1 TV_HOST=192.168.1.50 launcher/build-and-install.sh     # Linux and macOS
powershell -ExecutionPolicy Bypass -File launcher\build-and-install.ps1 -TvHost 192.168.1.50 -LiveTv
```

The channels come from the public [iptv-org](https://iptv-org.github.io) lists, one file per
language (`languages/tel.m3u`, `languages/hin.m3u`, `languages/eng.m3u` and so on). The app downloads
them on the TV, keeps them in its own storage and refreshes them when they get old. Which languages
it loads is set in `config.xml` under `live_tv_languages`.

## Tests

The plain JVM tests in `launcher/app/src/test/java/com/gathin/tvhome/` cover the saved app order,
the module-card validators and guard, the config overlay, the animator-scale maths, the wallpaper
index, the three wallpaper modes and the weather-code mapping. They do not need a TV or an emulator.

Windows:

```powershell
launcher\gradlew.bat -p launcher test
tools\venv\Scripts\python.exe tools\check-links.py
```

macOS and Linux:

```bash
launcher/gradlew -p launcher test
tools/venv/bin/python tools/check-links.py
```

`tools/check-links.py` checks every local file and image target in the root Markdown, `themes/*.md`
and `examples/*.md`; it exits `0` when every target resolves and `1` with one line per broken target.

## Restoring your TV

Keep the list you applied. The restore script re-enables exactly those packages, puts the stock
launcher back as Home and resets the animation scales:

```bash
TV_HOST=192.168.1.50 tools/restore.sh examples/<your tv>/debloat.txt                                    # Android TV
TV_HOST=192.168.1.50 tools/restore.sh examples/<your tv>/debloat.txt com.google.android.apps.tv.launcherx  # Google TV
```

It leaves the new launcher installed (remove it from the TV's Settings → Apps if you want it gone)
and does not know about packages you disabled outside the list. The same goes for the optional Live
TV app: it stays until you uninstall it. To switch back later, run the
install script again; it works out which launcher to disable even when Home has fallen back to
Android's chooser. If the TV is unreachable, its own
Settings → Apps page can re-enable packages too.

## When something goes wrong

The ones that bit us most:

| Symptom | Fix |
|---|---|
| **Images or the name are missing after a reinstall** | Uninstalling deletes `/sdcard/Android/data/com.gathin.tvhome/files/theme/`. Re-run `tools\set-images.ps1 -TvHost <tv ip>` (or `TV_HOST=<tv ip> tools/set-images.sh`); the local master copy is `personal-assets/theme/`. |
| **The wallpaper picker is empty** | The TV has no theme pictures and no stock fallback, or the pushed folder is gone. Push a theme with at least one picture in `theme/wallpapers/` and check that `wallpapers.json`, if present, lists it by file name. |
| **The control panel does not open** | Focus the top-right avatar pill and press OK. If focus is not on it, press Up from the dock or use the D-pad to reach it; Back closes an open panel. |
| **The dock shows only four apps** | `columns` is still `4`, not the default `5`. Set `columns` to `5` in `config.xml` or `theme/config.json`; the fifth app is the next one in grid order unless `dock_apps` names a fifth package. |

- **macOS blocks `adb connect`** ("No route to host") under Local Network privacy. `tools/tv.py` avoids the binary.
- **Disable the stock launcher only after the new one is installed and running**, or Home lands on a factory test app. The install script orders this for you.
- **On Google TV the setup wizard** (`com.google.android.tungsten.setupwraith`) can take Home and crash-loop the system after the stock launcher is disabled. Disable it too, and list it in your `debloat.txt` so restore brings it back. Stuck already? `tools/tv.py 'pm enable --user 0 com.google.android.apps.tv.launcherx'`.
- **Apps say they cannot connect to Wi-Fi while YouTube and Netflix still play**: something set a global HTTP proxy. `dumpsys connectivity` shows Wi-Fi connected but never `VALIDATED`, and the log has `PROBE_HTTP ... Failed to connect to /127.0.0.1:9`. Check `settings get global global_http_proxy_host`, `global_http_proxy_port` and `http_proxy` (the short `http_proxy` key can read `null` while a proxy is set), then clear it with `settings delete global global_http_proxy_host`, `settings delete global global_http_proxy_port` and `settings put global http_proxy :0`, and confirm `VALIDATED` in `dumpsys connectivity`. On some sets the deletes alone leave the old proxy cached; the legacy `http_proxy :0` write is what makes the connectivity check pass again. Clearing the proxy also un-blocks whatever it was put there to block, usually ads or telemetry — use a DNS-level filter instead if you want that back.

## If the coding agent seems stuck

- **Normal on a first TV connection**: the TV shows an "Allow USB debugging?" popup. Tap **Allow** with **"Always allow"**.
- **If nothing has happened for about 30 to 60 seconds**, press **Esc** (or **Ctrl+C**) to interrupt, then type `continue`.
- The agent keeps its context, so it picks up where it stopped.
- **If it repeats the same step**, tell it: "Stop. Show me the last command and its output, then wait."

## Questions people ask

**Is this safe? Can it brick the TV?** Nothing is flashed, rooted or deleted; packages are disabled
in the user profile. The risk is disabling something the TV leans on, which is why lists are
applied in small batches with a restore script ready. The TV's own Settings → Apps page can
re-enable packages even without the tools.

**What about firmware updates?** Most lists disable the update service, so updates arrive only when
you re-enable it. If one lands, the worst case is a package coming back and you re-run the script.

**Won't the apps break?** Streaming apps still update through the Play Store, which is left alone.
The launcher just lists what is installed and launches it with intents that have been stable for a decade.

**Isn't ADB access a security hole?** It is one explicit "allow this computer" tap on the TV, the
same as developing an Android app. Switch USB debugging off afterwards; the launcher keeps working.

**AirPlay?** Not from this repo. Chromecast built-in stays for Android phones. For iPhones, an
open-source AirPlay receiver can be built and installed next to the launcher.

**Can I just download the APK?** Build it with the one-line script; it takes a minute and you
control what goes on your TV. No prebuilt binaries.

## Contributing

The most useful contribution is your TV's debloat list, so the next person with that model does
not have to rediscover it. Launcher improvements and sharper prompt wording are welcome too.
[CONTRIBUTING.md](CONTRIBUTING.md) has the format and the testing we ask for. Built something on
top, a launcher, a port, a theme? Add it to [SHOWCASE.md](SHOWCASE.md).

## Credits

[cleanyourtv (formerly tv-vibehack) by @Gathin23](https://github.com/Gathin23/cleanyourtv).

Data: the weather widget uses [Open-Meteo](https://open-meteo.com) for the current conditions and
[OpenStreetMap Nominatim](https://nominatim.openstreetmap.org) once during setup to turn a pincode
into coordinates; the Live TV app takes its channel lists from the public
[iptv-org](https://iptv-org.github.io) playlists.

## Licence

MIT, see [`LICENSE`](LICENSE). The Outfit typeface is under the SIL Open Font License, see
`launcher/FONT-LICENSE-OFL.txt`. The wallpapers that ship with the launcher are listed in
[`launcher/app/src/main/assets/wallpapers/CREDITS.md`](launcher/app/src/main/assets/wallpapers/CREDITS.md)
with the source and the licence of each one: fill in that file for any picture you add, because
they go out with the APK.

The CleanTV name, icon and banner are not covered by the MIT licence.

CleanTV copyright (c) 2026 barigalasunil.
