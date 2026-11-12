# Gym Planner

An offline Android app for planning your gym week, logging workouts, cardio and body
weight. No account, no server, no internet: everything stays on the phone.

Nothing about any one person is built in. A fresh install starts with a short setup
and the **4-Day Split** template, so you can hand the same APK to a friend.

## What's in version 2

| Tab | What you get |
| --- | --- |
| **Today** | The next day in your rotation (no fixed weekdays), a sets ring, Start workout, today's weight and this week's cardio. On an off day: week dots, weight trend, Log cardio, and *Train anyway* |
| **Plan** | Your plan's days (drag ≡ to reorder; *weekly* or *every 2 weeks*). Exercises with sets, rep range, to failure, per arm/leg, rest, warm-up, optional, drop sets and supersets |
| **Track** | Weight chart (7-day average), cardio log (bike, elliptical, treadmill, stair climber, rower, walk, or your own) with weekly kcal, and body measurements |
| **Progress** | Weekly streak, month calendar (gym, cardio, today), monthly stats, and a chart for every exercise |

Also:
* **Active workout:** one exercise per page (swipe). There's a set table with last time's numbers, big −/+ steppers, *Log set* with a haptic tick, drop sets pre-filled 20% lighter, and supersets that alternate set by set. A plate calculator appears for barbell lifts, and PR badges show when you beat your best. The rest timer keeps running and vibrates with the screen off.
* **How to:** every exercise (153 built in) has a body map, steps, common mistakes and a *Watch demo video* button (a YouTube search). You can fill in the guide for your own exercises.
* **Morning weigh-in:** a full-screen, alarm-style prompt at the time and days you choose. It doesn't show if you've already weighed in that day.
* **Home-screen widget:** your streak, the next day, and a *+ Weight* button.
* **Accent colour:** lime by default; orange, cyan or purple in Settings.
* **Sharing:** *Share plan* sends a `.gymplan.json` (format 2) that holds no weights, logs or personal info. Format 1 files from v1 still import.
* **Backup:** *Settings → Backup* saves one private file with a profile's settings, plans, weight, cardio, measurements and workouts. Restoring adds it as a new profile.

## Installing (updating from v1 keeps your data)

v2 uses the same app ID (`com.gymplanner.app`), a higher version code (2), and the
**same signing key** as the v1 APK you installed, so it installs as an update. The
database upgrade only adds tables and columns. It's tested on a real v1 database
(`MigrationTest`).

**Before updating, it's still wise to back up:** in v1, open Settings → *Back Up* and save the file somewhere safe.

* **Send the APK to yourself** (Drive, Telegram "Saved Messages", email), open it on the phone and tap *Update*.
* **Or by USB:** `adb install -r app-debug.apk` (`-r` keeps your data).

> **Signing key:** the v1/v2 APKs are signed with the debug key from the build machine
> (fingerprint `42:AC:17:C2:…:C3:F3`). A copy was sent to you as `v1-debug.keystore.backup`;
> keep it safe. Every future update must be signed with it, or Android refuses to update
> and you'd have to back up, uninstall and restore. To build with it on your own computer,
> copy it to `~/.android/debug.keystore`. APKs from GitHub Actions use a different debug
> key, so don't install those over this app unless you set up the signing secrets below.

## Build it yourself

You need **JDK 17 or newer** and the **Android SDK** (platform 36). Android Studio installs both.

```bash
cd GymPlanner
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # macOS: $HOME/Library/Android/sdk
./gradlew assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit + Robolectric tests (migration, backup, weigh-in, logic)
./gradlew testDebugUnitTest -Pscreenshots   # also renders screens to app/build/screenshots
```

Robolectric (test-only, not in the app) runs Android code on a computer. The tests use it
to build a real v1 database and upgrade it, round-trip backups, check the weigh-in alarm,
and draw screens for design checks.

### Release build with your own key (for friends)

```bash
./scripts/make-release-key.sh    # creates ~/gymplanner-release.jks + keystore.properties
./gradlew assembleRelease        # → app/build/outputs/apk/release/app-release.apk
```

A release key is a different key from the one on your phone. A release APK is a fresh
install for anyone who has the debug build, so they'd back up, uninstall and restore once.
GitHub Actions builds signed releases once the four secrets in
`.github/workflows/gym-planner-apk.yml` are set.

## How the code is organised

Kotlin, Jetpack Compose + Material 3, Room, Navigation Compose, kotlinx.serialization, Jetpack Glance (widget).
MVVM: Room DAO → Repository → ViewModel → Compose screen.

```
app/src/main/java/com/gymplanner/app/
  core/data        Room database (v2), entities, DAOs, migrations, repositories, prefs
  core/model       units, muscles, JSON file formats (plan + backup, formatVersion 2)
  core/ui          theme (tokens, Space Grotesk + Inter), number pad, charts, body map, components
  feature/onboarding
  feature/today    Today (rotation, off day), active workout, rest timer service, plate calculator
  feature/plan     plan, day editor, exercise picker/settings, exercise demo, import/share
  feature/track    cardio log, body measurements (weight lives in feature/weight)
  feature/progress streak, calendar, exercise history
  feature/weighin  alarm scheduling, full-screen weigh-in activity
  feature/widget   home-screen widget
  feature/settings settings + profile menu
app/src/main/assets/templates
  exercises.json            153 exercises with guides (generated by scripts/library/build.py)
  default.gymplan.json      4-Day Split template; add more .gymplan.json files for more templates
app/schemas/       Room schema history (1.json, 2.json) used by MigrationTest
licenses/          Open Font Licence for the bundled fonts
```

### Data rules
* Weights in kg, lengths in cm, energy in kcal; units only change what you see.
* Every table except the exercise library and guides carries a `profileId`; profiles never see each other's data.
* Deleting a plan never deletes history. Warm-up sets don't count toward set totals or volume. A drop set is its own row pointing at its main set.
* Schema changes: bump `version` in `AppDatabase.kt`, add a `Migration` to `Migrations.ALL`, extend `MigrationTest`. Never use a destructive fallback.
