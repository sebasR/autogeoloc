# AutoGeoloc — User guide

AutoGeoloc lets a phone publish its GPS position on a website, showing the
live location on a map with the trajectory taken.

- [Project website on GitHub Pages](https://sebasr.github.io/autogeoloc/)
- [Download the Android app (APK)](https://sebasr.github.io/autogeoloc/AutoGeoloc.apk)
- Licence: [CeCILL V2.1](https://sebasr.github.io/autogeoloc/Licence_CeCILL_V2.1-en.txt)

## Overview

The system has two parts:

1. **The Android app** on the phone: reads the GPS position regularly and
   sends it to a website.
2. **The web part**: a small PHP site that receives and stores the position,
   and displays it on a map.

The web part must be installed on a web server that supports PHP. If you
are not the person who installed it, ask them for the address of the
**publication URL** (see below).

## Installing the app on the phone

1. On the phone, open the project website
   <https://sebasr.github.io/autogeoloc/>.
2. Tap the **AutoGeoloc.apk** link. Android may ask you to confirm that you
   want to install an application from an unknown source — accept.

   Installation from a browser is not the Play Store: Android warns about
   "unknown sources". You must explicitly allow installing from the browser
   (or from "Files" if the download was saved there).

3. Open the **AutoGeoloc** app.
4. On first start the app asks for permissions. **Both are needed:**

   - **Location permission** (`Access precise location`): required, the app
     cannot work without it. Give it **"Allow only while using the app"**
     or **"Allow all the time"** — if the phone is carried without looking
     at the app, choose "Allow all the time" so the position keeps
     updating. Note that with "Allow only while using the app" the phone
     can be **still locked with the app not visible** in the background —
     the service keeps running. Choose this if you only need the track when
     moving. For continuous tracking choose "Allow all the time".
   - **Notifications permission**: required for the "GPS position active"
     notification. The app runs as a *foreground service* and Android
     needs to show a notification while it is running. If you refuse, the
     service cannot stay alive reliably.

5. The app shows the current position, the state of the publishing
   (green = last publication OK, red = error), and a **Settings** button.

## Configuring the app

Open **Settings** in the app:

- **Publication URL**: the URL of the server that receives the position.
  It must contain the two placeholders `{lat}` and `{lon}`, for example:

  ```
  https://my-server.example.com/geoloc.php?lat={lat}&lon={lon}
  ```

  If the web part is installed at the root of a domain, the URL is simply:

  ```
  https://my-server.example.com/geoloc.php
  ```

  Multiple URLs can be added — the position is sent to all of them.

  You can find the exact URL on the map page itself: click the **gear**
  (⚙) in the bottom-right of the page footer; the URL is displayed with a
  **Copy** button.

- **Refresh interval (seconds)**: how often the position is sent. 60 is a
  good default.

Save; the app applies the settings and continues publishing.

### Using "Modes and routines" (Samsung) or automation apps

You can start/stop the publishing automatically:

- **Start**: create a routine with the condition you want (e.g. "When I
  leave home") and the action **Open an application → AutoGeoloc**.
- **Stop**: another routine with the action **Close an application →
  AutoGeoloc**. Closing the app (or swiping it away from the recent
  applications) stops the service — the map then shows the last known
  position until the next publication.

#### Closing the app when a mode ends

Modes only revert *settings* (Wi-Fi, brightness, DND…) automatically when
they end — app open/close actions never reverse on their own. To close
AutoGeoloc when a mode ends, use a routine watching the mode:

- **Option A (newer One UI)**: in **Routines**, create a routine with the
  condition **"Mode is active"**, select your mode, and change the option
  to **"when it ends"**; add the action **Close an application →
  AutoGeoloc**.
- **Option B (all versions)**: if the mode is triggered by an event
  (e.g. car Bluetooth), build a routine on the *end* of that same event:
  "When the Bluetooth device disconnects" → **Close an application →
  AutoGeoloc**.
- A mode ending at a scheduled time can also work this way — but there is
  no "add an action when the mode ends" inside the Mode itself; the
  routine is the only place for it.

Battery: if the phone kills the app too quickly, set the app to **Unrestricted**
in the battery settings (Settings → Apps → AutoGeoloc → Battery), or disable
battery optimisation for it.

## Reading the website

Open the map page in any browser. It shows:

- the current position (marker) and the trajectory (teal line), refreshed
  every 5 seconds;
- the last update time;
- in the footer: a link to the project website and the gear (⚙) with the
  configuration help (publication URL + APK link).

The trajectory is kept in the browser memory only — closing the page
erases it. There is no history on the server: only the current position is
stored.

## Installing the web part (for the server owner)

If the web part is not yet installed on your server, see the
[developer guide](developer.html), section "Deploying the web part": the
short version is

1. Download `autogeoloc-web.tar.gz` from the project website;
2. extract it into a PHP-enabled directory of your host (the folder must
   be writable — the script stores `position.txt`);
3. open the page in a browser to check it works;
4. give the phone users the publication URL shown by the gear (⚙).