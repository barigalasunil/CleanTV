# Live TV

An optional second app for the TV, next to the launcher in `launcher/`: a small plain-Java
Android TV player built on [Media3](https://developer.android.com/media/media3) ExoPlayer, with
thousands of channels grouped by language.

It is optional on purpose. The launcher works without it, and the home screen only shows the
Live TV tile when the app is installed. The launcher pins it as the first tile of the grid, below
the dock, when `live_tv_app_package` in `config.xml` names it and the app is there.

## Building and installing

```bash
LIVE_TV=1 TV_HOST=192.168.1.50 launcher/build-and-install.sh         # Linux and macOS
powershell -ExecutionPolicy Bypass -File launcher\build-and-install.ps1 -TvHost 192.168.1.50 -LiveTv
```

Or on its own:

```bash
cd apps/live-tv && ./gradlew assembleRelease
adb -s 192.168.1.50:5555 install -r app/build/outputs/apk/release/app-release.apk
```

The APK is at `app/build/outputs/apk/release/app-release.apk`. It needs JDK 17 and Android SDK
platform 34, the same as the launcher.

## Where the channels come from

The public [iptv-org](https://iptv-org.github.io) lists, one file per language:

```
https://iptv-org.github.io/iptv/languages/<code>.m3u
```

The app downloads the languages set in `config.xml` (`live_tv_languages`, Telugu, Hindi and English
by default), parses them and keeps them in its own storage. The list is refreshed when it is
older than a day. `all.m3u` and `channels/in.m3u` are 404; `index.m3u` is the full list of every
language in one file and is only used when a language is asked for that has no file of its own.

The channel logos are the ones in the lists themselves; they are loaded from the internet and
cached for a day. A channel that does not work shows its failure in the player rather than
crashing the app: some of the streams in the public lists are dead or blocked in some countries.

## The screens

- **Channels**: grouped by language, with a header per group, the channel logo, the name and the
  quality when the list says it.
- **Player**: Media3 ExoPlayer, HLS and DASH supported, the channel name on top and the remote's
  Back button leaves the player.
- **Settings**: how many channels are cached, the languages, and the button to clear the cache.

`ChannelsActivity` is the one Android TV opens with the remote's app list; the other two are
opened from it.
