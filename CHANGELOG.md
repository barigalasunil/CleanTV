# Changelog

## 2026-10-30 - Control panel, five-app dock and pushed theme

### Added

- **Avatar pill and control panel**: one frosted top-right pill shows the pushed avatar (or the
  name's first letter, or a person glyph) and an optional clock. OK opens Inputs, network, Settings
  and Wallpapers; hold Settings for Module cards and hold network for Wi-Fi settings; Back closes the
  panel and returns focus to the pill.
- **Wallpaper picker**: every pushed picture, grouped by mood, with Rotate all, Rotate one mood and
  Pin one picture. The choice is saved in the launcher's `wallpapers` preferences and applied at
  once; an old single-picture choice is read as a pin.
- **Five-app dock**: `columns` defaults to `5`, so the dock has five slots. `dock_apps` sets their
  order; the stock list names four apps and leaves the fifth slot to the next app in grid order.
  Tiles keep their 180x101 dp design size at four columns and shrink from five up.
- **Motion system**: FAST 160 ms, MEDIUM 260 ms and SLOW 420 ms, shared ease-out, overshoot and close
  curves, and one spring. Durations follow the TV's animator duration scale; `0` turns animation off.
- **Personal data pushed to the TV**: name, avatar, splash and wallpapers live in the app's external
  `theme/` folder. `build-and-install` re-pushes `personal-assets/theme` after an install when it
  exists; uninstalling deletes the pushed folder, and the APK only ships the stock pictures.
- **Tests and link checker**: eight JVM test classes cover app order, module cards, config overlay,
  motion maths, wallpaper indexing and modes, and weather codes; `tools/check-links.py` checks local
  links and images in the root docs, `themes/` and `examples/`.

### Fixed

- The README, prompt and setup questions now match the launcher: the dock has five slots, the top
  row is the avatar pill rather than separate Inputs/Wi-Fi/Settings pills, and personal pictures are
  pushed to the TV instead of being built into the APK.

## Rollback and home widgets

### Removed

- **Day and night theming**: `ThemeManager`, `ThemeMath`, the phase palette, the wallpaper
  `ColorMatrixColorFilter` tints, the separate `wallpapers/day` and `wallpapers/night` sets, the
  sunrise and sunset fields in the weather request, the `theme_*` keys and the theme setup question.
  The colours, text shadow, scrim, glass highlight and adaptive contrast are the ones from before the
  engine existed.
- **The RAM, storage and network sentences** in the top bar, with their `InfoPill` and the link-speed
  reading. The top row is the single avatar pill; its panel holds Inputs, network, Settings and
  Wallpapers.
- **The weather shelf** with its five-day forecast, and the smaller two-row pill style it needed.

### Added

- **Home widgets**: one display-only row under the avatar pill, anchored to the right edge. Today's
  weather (two cells, icon, large temperature and a condition word), a RAM ring gauge and a storage
  ring gauge, all on dark frosted glass with solid white text. The weather is one Open-Meteo request
  for the current temperature, weather code and `is_day`, cached and refreshed every half an hour.
  RAM is read every five seconds, storage every thirty, on the home screen's existing ticker. The
  gauges turn amber from 75% and red from 90%. RAM used includes caches.
- **Config keys**: `widgets_enabled` (default `true`), `widget_weather_enabled`,
  `widget_ram_enabled`, `widget_storage_enabled`.
- **Now Playing** lives in the widget band under the avatar pill: same top, same height, same glass,
  and clear of the widgets. It no longer has a shelf of its own, so nothing moves when it appears.
- **Personal pictures**: personal wallpapers, splash and avatar are pushed to the TV's external
  `theme/` folder; no personal picture is scaled into or bundled with the APK. The tracked stock
  pictures are untouched, and `-Ppublish` builds always ship the stock set.
- **Screensaver**: the Now Playing line, and the drift every fifteen seconds.

### Fixed

- Pill text is readable with nothing focused and while focused: the adaptive contrast colour is used
  as it is, without the alpha-masking step that made it invisible.
