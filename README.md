# AutoGeoloc — Automatic Geolocation System

Publish a phone's GPS position on a website and watch it live on a map with
its trajectory.

- **Android app**: foreground service that reads the GPS position and sends
  it over HTTP to your server.
- **Web part**: tiny PHP page that stores the position and displays it on a
  map (Leaflet + OpenStreetMap).
- **Full documentation**: [project website on GitHub Pages](https://sebasr.github.io/autogeoloc/)
  — [user guide](https://sebasr.github.io/autogeoloc/doc/user.html) and
  [developer guide](https://sebasr.github.io/autogeoloc/doc/developer.html).
- **Licence**: [CeCILL V2.1](Licence_CeCILL_V2.1-en.txt)

## Quick install

1. **On the phone**: download and install
   [AutoGeoloc.apk](https://sebasr.github.io/autogeoloc/AutoGeoloc.apk)
   from the website, grant the location and notification permissions, then
   enter the publication URL in the app settings (found via the ⚙ gear at
   the bottom of the map page).

2. **On the server**: download
   [autogeoloc-web.tar.gz](https://sebasr.github.io/autogeoloc/autogeoloc-web.tar.gz),
   extract it into any PHP-enabled directory (must be writable), and open
   the page in a browser.

## Automation (Samsung Modes and Routines)

The app can be started and stopped automatically — including closing it
exactly when a Samsung mode ends. See the
[user guide — "Using Modes and routines"](https://sebasr.github.io/autogeoloc/doc/user.html#using-modes-and-routines-samsung-or-automation-apps)
for the routine recipes.

## For developers

See the [developer guide](doc/developer.md): repository layout, building
the APK with Android Studio or Gradle, project architecture, deploying the
web part. The website is generated automatically by GitHub Actions on every
push to `main` (build APK, pack the web part, convert the docs to HTML,
deploy to GitHub Pages).