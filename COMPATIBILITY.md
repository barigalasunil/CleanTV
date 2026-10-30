# Compatibility

The launcher is a plain Android TV app: `minSdk 26` (Android 8), `targetSdk 34`. The table records
only what was actually checked on a **Redmi TV X50**. Everything not listed here is untested; no
other TV model or Android version is claimed.

| Feature | Redmi TV X50 | Notes |
|---|---|---|
| **Install, Home and boot** | Verified | Installed over ADB, set as Home, and the launcher came back after a reboot. |
| **Avatar pill and control panel** | Verified | The pill opened Inputs, network, Settings and Wallpapers; Back closed the panel and returned focus to the pill. |
| **Wallpaper picker** | Verified | Pictures were grouped by mood; Rotate all, Rotate one mood and Pin worked, and the choice survived a restart. |
| **Five-app dock** | Verified | Five slots appeared; the fifth app came from `dock_apps`, or from the next app in grid order when the list named only four. |
| **Personal theme push** | Verified | Name, avatar, splash and wallpapers pushed to the TV and appeared. |
| **Motion and animations** | Verified | Durations, the focus spring and the TV's animator duration scale behaved as designed. |
| **Home widgets and Now Playing** | Verified | Weather, RAM, storage and the Now Playing band appeared and stayed in their own row under the pill. |
| **Screensaver** | Verified | The wallpaper and clock screensavers started, and any remote key ended them. |
| **Debloat and restore** | Verified | Packages were disabled with `pm disable-user`, and the restore script re-enabled them. |
| **Live TV app** | Not tested | Optional, separate app; no Redmi TV X50 test is recorded. |

The details below are implementation notes, not claims about other devices.

## Home widgets

- The band is a fixed width of four 81 dp cells and three 10 dp gaps (354 dp), hung leftwards from
  the avatar pill's right edge. Density changes the pixels, not the dp size.
- The cells are square: their height follows their width. The hero name and the dock keep their own
  positions.
- The widgets use dark glass independent of the adaptive contrast used by the clock, so they stay
  readable on a bright wallpaper.
- **RAM** comes from `ActivityManager.MemoryInfo`; **storage** comes from `StatFs` on the data
  partition. Both return nothing rather than a guess when the system will not answer.
- If a widget is switched off the remaining cells keep their size and stay right aligned under the
  pill; the hidden widget's slot stays empty. If the weather has no coordinates, only the weather
  cell is left out.

## Now Playing

- The card needs the notification listener, granted once:
  `adb shell cmd notification allow_listener com.gathin.tvhome/com.gathin.tvhome.MediaNoteService`.
  Without it the card simply never appears; nothing else changes.
- It reads the active media session (title, artist, art, playback state) and can send previous,
  play/pause and next.

## Weather

- One request to Open-Meteo with the saved latitude and longitude for the current temperature, the
  weather code and `is_day`. No key, no lookup at run time, no forecast.
- Without coordinates the weather widget is hidden. With no network the last reading stays on screen;
  a failed request is not reported anywhere.
- The clock and date are the TV's, so a wrong time zone shows a wrong time, as anywhere else.

## Pictures and build

- `personal-assets/` is git-ignored. The pushed theme is not part of the APK; the stock pictures in
  `launcher/app/src/main/assets` are the fallback. A build with `-Ppublish` ships only the stock set.
- The build prints one line saying which set it used and how many pictures.
