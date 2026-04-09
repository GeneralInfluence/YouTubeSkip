# AdSkipper

Auto-skips YouTube ads on Android using the Accessibility API. No root required.

## Setup

1. Open the project in Android Studio (Electric Eel or newer).
2. Build and install on your device (`Run > Run 'app'`).
3. Tap **Enable in Accessibility Settings** in the app.
4. Find **Ad Skipper** in the list and toggle it on.
5. Open YouTube — ads will now be skipped automatically.

## How it works

The app registers an `AccessibilityService` that watches YouTube's view
hierarchy for nodes matching known skip/next button IDs. When a match is
found, it calls `performAction(ACTION_CLICK)` on that node — no pixel
coordinates involved, so it works regardless of where the button appears
on screen or how long into the ad it shows up.

## Updating button patterns after a YouTube update

YouTube occasionally renames its internal view IDs. If skipping stops
working after a YouTube update:

### Using adb (fastest)

```bash
# While a skip button is visible on screen:
adb shell uiautomator dump /sdcard/dump.xml
adb pull /sdcard/dump.xml
```

Open `dump.xml` and search for `skip` or `next`. Look for attributes like:

```xml
resource-id="com.google.android.youtube:id/skip_ad_button"
content-desc="Skip Ad"
```

### Using Android Studio Layout Inspector

1. Connect your device with USB debugging on.
2. In Android Studio: **Tools > Layout Inspector**.
3. Select the YouTube process.
4. While an ad is playing and the skip button is visible, click the button
   in the Layout Inspector tree to see its `resource-id` and
   `content-desc`.

### Adding the new pattern

Open `AdSkipperService.kt` and add the new value to `SKIP_PATTERNS`:

```kotlin
val SKIP_PATTERNS = listOf(
    "skip_ad_button",
    "your_new_id_here",   // <-- add here
    ...
)
```

Rebuild and reinstall.

## Project structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/adskipper/
│   ├── AdSkipperService.kt   ← core logic
│   └── MainActivity.kt       ← status UI
└── res/
    ├── layout/activity_main.xml
    ├── values/strings.xml
    ├── values/themes.xml
    ├── values/colors.xml
    ├── drawable/circle_green.xml
    ├── drawable/circle_red.xml
    ├── drawable/ic_launcher_foreground.xml
    └── xml/accessibility_service_config.xml
```
