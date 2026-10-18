# DNSCrypt Monitor for Android

A native Kotlin app for managing a **local** `dnscrypt-proxy` installation on a
rooted Android device. It does not contain SSH, SSHFS, remote-host, analytics,
or network-client code.

## Features

- Watches `blocked-names.log` continuously in an Android foreground service.
- Sends a high-priority notification for each newly blocked domain.
- Includes an **Allow domain** action directly in block notifications.
- Lists recent blocked domains and excludes entries already allowlisted.
- Appends exact domains to `allowed-names.txt`.
- Restarts/reloads dnscrypt-proxy after allowlist changes when enabled.
- Provides local DNS enable/disable controls through configurable root commands.
- Resumes monitoring after boot when enabled.
- Clears the blocked log and resets notification history.

## Device requirements

- Android 8.0 or newer.
- Root with a working `su` binary (for example Magisk).
- A local dnscrypt-proxy installation that writes the three files below into
  one directory:
  - `blocked-names.log`
  - `blocked-names.txt`
  - `allowed-names.txt`

The default folder is `/data/adb/dnscrypt-proxy`. Open **Settings** after
installation and change it to the location used by your dnscrypt-proxy module.

Restart and DNS switching vary between Android distributions and Magisk
modules. The app therefore exposes all three local commands in Settings. Every
command is executed on the device with `su -c`; review and tailor the defaults
before relying on the toggles.

## Build

The GitHub Actions workflow builds and tests the project on every push and
uploads `app-debug.apk` as an artifact.

To build locally with JDK 17 and Android SDK 35:

```sh
./gradlew testDebugUnitTest assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Notification behavior

Monitoring begins at the current end of the log, matching the desktop tool, so
installing the app does not generate alerts for the entire existing history.
Domain names are deduplicated across service restarts. Use **Reset notification
history** to notify for previously seen domains again.
