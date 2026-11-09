# Gym Planner

An offline Android app for planning your gym week, logging workouts and tracking your
body weight. No account, no server, no internet: everything stays on the phone.

Nothing about any one person is built in. Every user starts with a short setup, so you
can hand the same APK to a friend and it works for them from a clean start.

## What it does

| Tab | What you get |
| --- | --- |
| **Today** | Today's day from your active plan, its exercises, estimated time, a *Light day* switch, **Start Workout**, and a quick **+ Weight** button |
| **Plan** | Your active plan's days as cards (drag ≡ to reorder). Tap a day to edit exercises, sets, rep ranges, rest, notes, optional and warm-up flags |
| **Weight** | Current weight, change since start and over the last 30 days, a small BMI number, a chart (7-day average line plus raw points, 1M / 3M / 1Y / All) and the full history |
| **Progress** | This week's workouts, sets and volume, plus every exercise's best-set chart and past sessions |

Your profile avatar (top right) switches profiles, adds a profile, and opens **Settings**
(units, default rest, theme, backup/restore, delete profile).

### Logging a workout
* Each set has a weight box, a reps box and a round tick button. Grey numbers are what
  you did last time. Tick straight away to reuse them, or tap a box to type with a big number pad.
* Ticking a set starts the rest timer. It keeps running with the screen off, shows a
  countdown notification (+30 s / Skip), and vibrates when rest is over.
* A small **PR** badge appears when weight × reps beats your best for that exercise.
* The screen stays awake. If the app is closed mid-workout, **Resume Workout** is waiting on Today.
* Optional exercises are greyed out with a **Skip** button. *Light day* leaves them out
  and takes one set off everything else, for that session only.

### Sharing plans
* **Plan → Share** (or *Plans → tap a plan → Share*) sends a small `.gymplan.json` file through
  WhatsApp, Telegram, Drive and so on. The file holds the plan only: no weights, logs or personal info.
* Opening that file on another phone (or *Plans → Import from File*) shows a preview, then
  **Add to My Plans**. Exercises are matched by name; missing ones are created.
* **Settings → Backup** is separate and private: one JSON file with a profile's plans,
  weight and workouts, for moving to a new phone. Restoring adds it as a new profile.

## Design

The look is modelled on Apple's iOS apps:

* Inset grouped lists on a grey (light) or black (dark) background
* Large bold titles, one blue accent colour, and system green, red and orange that always mean the same thing
* Green switches, segmented controls, steppers, and "Cancel · Title · Done" sheets
* Big tap targets (number keys are 58 dp, tick buttons 48 dp) for sweaty hands
* Text sizes in `sp`, so they follow the phone's font-size setting
* Dark theme by default; Light and System are in Settings

## Install on your phone

**Option A: send the file to yourself.** Put `app-debug.apk` (or `app-release.apk`) in
Google Drive, Telegram "Saved Messages" or email. Open it on the phone and allow
"Install unknown apps" for that app when Android asks.

**Option B: USB cable.**
1. On the phone: Settings → About phone → Software information → tap *Build number* 7 times.
   Then Settings → Developer options → turn on *USB debugging*.
2. Connect the phone and run:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

**Option C: GitHub Actions (no Android setup needed).** Every push builds the APK.
Go to the repository → **Actions** → the latest *Gym Planner APK* run → **Artifacts** →
download `gym-planner-debug-apk` (a zip that contains the APK).

> Every machine signs debug builds with its own key. If Android says "App not installed"
> when updating, uninstall the old copy first. This deletes the app's data, so back up
> first in Settings. Release builds signed with *your* key don't have this problem.

## Build it yourself

You need **JDK 17 or newer** and the **Android SDK** (platform 36, build-tools 36).
The easiest way to get both is to install [Android Studio](https://developer.android.com/studio).
Then:

```bash
cd GymPlanner
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # macOS: $HOME/Library/Android/sdk
./gradlew assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
```

### Release build (the one to give friends)

```bash
./scripts/make-release-key.sh    # creates ~/gymplanner-release.jks + keystore.properties
./gradlew assembleRelease        # → app/build/outputs/apk/release/app-release.apk
```

**Keep `~/gymplanner-release.jks` and its password safe**, for example in a password manager
plus a cloud backup. Every future update must be signed with the same key. If you lose it,
friends have to uninstall (and lose their data) before installing a new version.
`keystore.properties` and `*.jks` are git-ignored on purpose.

To have GitHub build signed releases too, add four repository secrets
(Settings → Secrets and variables → Actions): `KEYSTORE_BASE64` (output of
`base64 -w0 ~/gymplanner-release.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS` (`gymplanner`)
and `KEY_PASSWORD`.

## How the code is organised

Kotlin, Jetpack Compose with Material 3, Room, Navigation Compose and kotlinx.serialization.
MVVM: Room DAO → Repository → ViewModel → Compose screen. A single Gradle module, with one package per area:

```
app/src/main/java/com/gymplanner/app/
  core/data        Room database, entities, DAOs, migrations, repositories, prefs
  core/model       units, enums, JSON file formats (formatVersion = 1)
  core/ui          theme, number pad, chart, drag-to-reorder, shared components
  feature/onboarding
  feature/today    Today tab, active workout, rest timer service, summary
  feature/plan     plan, day editor, exercise picker, exercise settings, import/share
  feature/weight
  feature/progress
  feature/settings settings + profile menu
app/src/main/assets/templates
  exercises.json             76 built-in exercises grouped by muscle
  5-day-split.gymplan.json   starter template (add more .gymplan.json files here, no code needed)
app/schemas/       Room schema history, used to check migrations
```

### Data rules
* All weights are stored in kg and heights in cm. Units only change what you see.
* Every table except the built-in exercise library carries a `profileId`, so profiles never see each other's data.
* Only one plan per profile is active at a time.
* Deleting a plan never deletes workout history: sessions store the day name, and sets point straight at the exercise.
* The database starts at version 1, with no destructive fallback. When the schema changes,
  bump `version` in `AppDatabase.kt` and add a `Migration` to `Migrations.ALL`.
