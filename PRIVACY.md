# Privacy

The launcher has no account, no analytics and no telemetry. It sends one thing off the TV.

## What leaves the TV

- **The weather**: one HTTPS request to [Open-Meteo](https://open-meteo.com) with the latitude and
  longitude saved in `config.xml` (or the git-ignored `config.json` override), asking for the current
  temperature, the weather code and whether it is day. Nothing else is sent: no name, no pincode, no
  device identifier, no wallpaper, no usage data. The answer is cached on the TV and refreshed every
  half an hour.
- The pincode is only used **once, on your computer**, by `tools/pincode-to-coords.py`, which looks the
  coordinates up with OpenStreetMap Nominatim. The TV never sends the pincode anywhere, and the
  launcher never talks to Nominatim.

## Your personal data

Your name, avatar, splash picture and wallpapers are stored on the TV in the launcher's own external
folder, `/sdcard/Android/data/com.gathin.tvhome/files/theme/`, and nowhere else. They are never
uploaded, sent to Open-Meteo, put in the APK or included in the weather request. The master copy on
your computer lives in the git-ignored `personal-assets/` folder. Uninstalling `com.gathin.tvhome`
deletes the TV copy, so keep the master copy and re-push it after a reinstall.

## What never leaves the TV

- **RAM and storage**: read from `ActivityManager` and `StatFs` locally. Nothing is sent for them and
  no value is written to a log.
- The wallpapers, the splash picture, the avatar, the app list, the hidden and moved tiles, the
  greeting name, the saved weather coordinates and the Now Playing details stay on the device. The
  only network-derived value kept is the cached weather answer, in the app's private preferences.

Nothing else leaves the TV. The only network request the launcher makes is the weather request, and
it carries only the saved latitude and longitude.

## Permissions it uses

- `ACCESS_NETWORK_STATE` and `ACCESS_WIFI_STATE` to show the Wi-Fi, Ethernet or VPN state and the
  network name in the control panel. Nothing is sent for them.
- `ACCESS_FINE_LOCATION` only to read the Wi-Fi network name shown in the panel; Android requires
  location permission for that. It is granted over ADB at install time; the launcher does not read
  GPS or track location.
- `INTERNET` for the weather request. Without it the widget stays hidden and everything else works.
- The notification listener (`MediaNoteService`) is opt-in and only reads the active media session so
  the Now Playing card can show what is playing. It keeps nothing.
- The optional accessibility services for the remote's Home and Input keys only read those key
  events and pass every other key through. They store nothing and are disabled unless enabled at
  install.

## Removing the data

`adb shell pm clear com.gathin.tvhome` removes the cached weather and every saved choice. To remove
the name, avatar, splash and wallpapers, delete the app's `theme/` folder, run
`tools/theme.sh --reset`, or uninstall `com.gathin.tvhome`; all delete the pushed personal data.
The local master copy stays in the git-ignored `personal-assets/` folder until you delete it. Edit
`config.xml` to remove the coordinates as well.
