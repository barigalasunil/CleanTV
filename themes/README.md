# Themes

A theme is a folder that is pushed to the TV, not built into the APK:

```
personal-assets/theme/
  config.json
  avatar.jpg                 optional
  splash.jpg                 optional
  wallpapers/w-01.jpg        the owner's pictures
  wallpapers/w-02.jpg
  thumbs/t-01.jpg            320x180 previews for the wallpaper picker
  thumbs/t-02.jpg
  wallpapers.json            names and moods for the picker
```

The launcher reads it from
`/sdcard/Android/data/com.gathin.tvhome/files/theme/` and overlays `config.json` on its built-in
`config.xml` the next time it starts. Missing or damaged theme files are skipped; the stock pictures
in the APK are the fallback. No storage permission is needed: this is the app's own external folder.

```bash
TV_HOST=192.168.1.50 tools/theme.sh personal-assets/theme       # push and restart Home
TV_HOST=192.168.1.50 tools/theme.sh --reset                     # remove the pushed theme
powershell -ExecutionPolicy Bypass -File tools\theme.ps1 -TvHost 192.168.1.50 personal-assets\theme
```

`tools/set-images.sh` (or `tools\set-images.ps1`) builds the folder from
`personal-assets/wallpapers/`, `personal-assets/splash/`, an optional `personal-assets/avatar.*`
and `personal-assets/config.json`, then uploads it when `TV_HOST` is set.

| Theme | What it does |
|---|---|
| `default/` | The built-in look written out as JSON, as a starting point for your own |
| `aurora/` | A dark aurora wallpaper (teal and violet on navy, darker at the bottom) with a matching background colour |
| `six-columns/` | Six smaller tiles per row and one grid row visible under the dock |

## Keys

Every key is optional; a missing key keeps the value from `config.xml`, an unknown or wrongly typed
key is ignored, and a file that doesn't parse is ignored as a whole, so a theme can never break Home.

| Key | Type | Meaning |
|---|---|---|
| `dock_apps` | list of package names | The dock row, in order; missing apps are skipped |
| `hidden_apps` | list of package names | Never shown as tiles |
| `live_tv_package`, `live_tv_action` | string | The Live TV app and, if it has no leanback entry, the action to start it with |
| `source_panel_service`, `source_panel_action` | string | The TV's own source panel, for sets with no TvInputManager |
| `input_action` | string | Action sent with an HDMI/AV row's channel URI when nothing handles the standard `VIEW` (XGIMI: `com.xgimi.action.hdmiPlayer`) |
| `columns` | 1 to 8 | Tiles per row; tiles shrink to fit from 5 up |
| `rows_below_dock` | 0 to 3 | Grid rows left visible under the dock |
| `shamsi_date` | bool | Show the Solar Hijri date above the Gregorian one |
| `module_cards` | bool | Look for module cards and ask about them (`true`); `false` turns the feature off |
| `vpn_app` | string | App the network panel opens when a VPN is configured; also shows a red "VPN off" badge while no VPN is up |
| `show_home_settings_shortcut` | bool | The "Default home" tile, where the TV has a home picker |
| `background_color` | `#RRGGBB` | Colour behind everything; black by default |
| `use_wallpaper` | bool | Show the theme's wallpapers (`true`), or keep the plain background (`false`) |
| `wallpaper_interval_seconds` | number | Seconds each picture stays on the home screen |
| `screensaver_enabled` | bool | Show the same pictures in the screensaver |
| `screensaver_interval_seconds` | number | Seconds each picture stays in the screensaver |
| `greeting_name` | string | The name in the greeting and welcome splash; empty means no name |
| `weather_lat`, `weather_lon` | number | The place the weather is for |
| `splash_message` | string | Splash text; `%s` is replaced with `greeting_name` |

## Pictures

Complete wallpapers are scaled down to fit 1920×1080 and saved as JPEG quality 80. Previews are
cropped to 320×180. The app reads both from the theme folder and downsamples the full picture to the
screen, so a 4K source costs no more than a 1080p one on a 1 GB TV. The original pictures are never
modified and the pushed copy is the only copy on the TV.

`wallpapers.json` can be a list or `{"wallpapers": [...]}`. Each entry looks like:

```json
{"file": "w-01.jpg", "thumb": "t-01.jpg", "name": "Aurora", "mood": "Night", "source": "aurora.jpg"}
```

`file` and `thumb` must be plain file names inside `wallpapers/` and `thumbs/`; `source` is only a
note for the person rebuilding the theme. Without `wallpapers.json`, every picture in
`wallpapers/` is used with its file name as the label.

`avatar.jpg` is the picture in the top-right control pill. Without one, the pill shows the first
letter of `greeting_name`, or a neutral dot when both are missing.

`config.json` is copied unchanged from `personal-assets/config.json`; keep personal names,
coordinates and pictures in the git-ignored `personal-assets/` folder, never in this repository.
Uninstalling `com.gathin.tvhome` deletes the pushed theme, so keep `personal-assets/theme` as the
master copy.

## Adding a theme

Open a PR with a new folder here, a one-line row in the table above and a screenshot from a real TV
in the PR description. Use generic dock packages unless the theme is for a specific TV, and say
which TV you tried it on.
