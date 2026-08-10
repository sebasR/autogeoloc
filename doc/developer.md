# AutoGeoloc — Developer guide

This document is for developers who want to build, modify or deploy
AutoGeoloc. Basic users should read [the user guide](user.html) instead.

- [Project website on GitHub Pages](https://sebasr.github.io/autogeoloc/)
- [Download the APK](https://sebasr.github.io/autogeoloc/AutoGeoloc.apk)
- Source: <https://github.com/sebasR/autogeoloc>
- Licence: [CeCILL V2.1](https://sebasr.github.io/autogeoloc/Licence_CeCILL_V2.1-en.txt)
  ([French version](https://sebasr.github.io/autogeoloc/Licence_CeCILL_V2.1-fr.txt)) —
  see `Licence_CeCILL_V2.1-{en,fr}.txt` in the repository

## Getting the source

With git:

```
git clone git@github.com:sebasR/autogeoloc.git
cd autogeoloc
```

Repository layout:

```
android/   the Android app source (Gradle project)
web/       the web part (PHP map page + geolocation endpoint)
doc/       this documentation (Markdown)
```

## Building the Android app

Requirements:

- [Android Studio](https://developer.android.com/studio) (the project was
  created with Android Studio, recommended for newbies)
- JDK 17 or newer (bundled with Android Studio)
- Git

### From Android Studio (recommended for newbies)

1. Open Android Studio.
2. Choose **Open**, then select the `android/` folder. Wait for Gradle
   synchronisation to finish (first run downloads the Gradle wrapper and
   dependencies, it can take a few minutes).
3. When prompted, agree to install any missing SDK components.
4. In the toolbar, select the **app** run configuration and your device
   (a physical phone with USB debugging enabled, or an emulator).
5. Click the green **Run** button (play icon). The app is compiled,
   installed and started on the device.
6. To produce an APK file without a device: menu **Build → Build
   APK(s) → Build APK(s)**. The file is written to
   `android/app/build/outputs/apk/debug/app-debug.apk`.

### From the command line

```
cd android
./gradlew assembleDebug          # Linux / macOS
gradlew.bat assembleDebug        # Windows
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Project architecture

### Android app (`android/`)

- `app/src/main/java/com/example/autogeoloc/MainActivity.java`
  Request the permissions, start the service, display the current status
  (polls static fields of the service once per second).
- `app/src/main/java/com/example/autogeoloc/GeolocationService.java`
  Foreground service that periodically retreives the GPS position and
  publishes it over HTTP to every URL configured in the settings.
  - URLs must start with `http://` or `https://` and contain the
    placeholders `{lat}` and `{lon}` which are replaced by the coordinates.
  - Foreground service with `START_STICKY`; stops itself when the task is
    removed (swipe from the recents list, or a Samsung "routine" closing the
    app). `stopWithTask` is `false` so the service survives a screen
    rotation.
- `app/src/main/java/com/example/autogeoloc/SettingsActivity.java`
  Manage the publication URLs and the refresh interval (seconds).
  Preferences are stored in the SharedPreferences file `geoloc_prefs`
  (keys `publication_urls`, `refresh_interval_seconds`).
- `app/src/main/AndroidManifest.xml` permissions: internet, fine + coarse
  location, foreground service (location type), notifications.
- `app/build.gradle` — application id `com.example.autogeoloc`,
  compile/target SDK 34, min SDK 21.

Build configuration notes:

- Gradle wrapper 9.3.0 with Android Gradle Plugin **8.13.0** (older AGP
  versions do not work with this Gradle version).
- `gradle.properties` sets `org.gradle.parallel=false` and
  `org.gradle.workers.max=1`.
- The debug APK is signed with the debug keystore and is directly
  installable — it is the artifact published on the website.

### Web part (`web/`)

- `index.php` — the map page (Leaflet + OpenStreetMap tiles, auto-refresh
  every 5 s, trajectory drawn client-side). Also serves the JSON poll
  response when called as `index.php?json=1`.
- `geoloc.php` — the endpoint the phone publishes to. Reads `lat`, `lon`
  (and optionally `time`) and writes them to `position.txt`. Returns
  `OK`/HTTP error codes.
- `position.txt` — the most recent position, written by `geoloc.php`.
- `favicon.svg` — the site icon (also mirrored in the app launcher icon).
- `.htaccess` — disables auth on shared hosts that require it.

There is no database: the current position is only stored in the plain
text file `position.txt` in the same directory as `geoloc.php`. The
trajectory is kept in the browser memory of every visitor (it disappears
when the page is closed).

### GitHub Pages site

The repository is published with GitHub Actions (`.github/workflows/
publish.yml`): every push to `main` builds the APK, packs `web/` into a
tar archive, converts the Markdown documentation to HTML, and deploys
everything to the website at <https://sebasr.github.io/autogeoloc/>.

Website root content:

- `AutoGeoloc.apk` — the Android app
- `autogeoloc-web.tar.gz` — the web part (PHP), to deploy on your own
  PHP-capable host
- `index.html`, `doc/*.html` — this documentation
- `LICENSE`

## Deploying the web part

The web part must run on a server with PHP (any version ≥ 5.4 is fine).

1. Download `autogeoloc-web.tar.gz` from the website.
2. Extract it into the web directory of your host:

   ```
   tar xzf autogeoloc-web.tar.gz
   ```

   The archive contains a `web/` folder with `index.php`, `geoloc.php`,
   `favicon.svg` and `.htaccess`.
3. Make sure PHP is enabled (`index.php` must be executed, not downloaded).
4. Check that the directory is writable by the web server user, because
   `geoloc.php` writes `position.txt` into it.
5. Open the page in a browser: you should see the map with the header and
   the footer. Click the gear — the footer shows the URL that must be
   entered in the phone app (`https://your-host/.../geoloc.php`).

Note that the map only displays the position once the phone has published
at least once (see the user guide for the phone configuration).