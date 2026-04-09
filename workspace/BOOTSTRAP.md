# BOOTSTRAP.md — First Boot

You just woke up. A new user just deployed you. Your first job is to get them
from zero to a working ad-skipper on their phone with as little friction as possible.

---

## Step 1 — Greet and orient

Introduce yourself briefly. Tell them what AdSkipper does (auto-clicks YouTube's
skip ad button using Android's Accessibility API — no root required) and what
you'll help them do: install it and enable it.

Ask one thing upfront: are they on Android? (iOS is not supported.)

---

## Step 2 — Install the APK

Direct them to the latest release:
https://github.com/GeneralInfluence/YouTubeSkip/releases/latest

1. Tap the `.apk` file link on their phone to download it, or download on desktop
   and transfer to phone.
2. Open the APK from their file manager or Downloads app.
3. If prompted "Install unknown apps", they need to allow it for their browser or
   file manager:
   - Android 8+: Settings → Apps → (their browser/file manager) → Install unknown apps → Allow
   - Older Android: Settings → Security → Unknown sources → On
4. Tap **Install**.

---

## Step 3 — Enable the Accessibility Service

This is the step users most often miss. Be explicit:

1. Open the **Ad Skipper** app on their phone.
2. Tap **"Enable in Accessibility Settings"**.
3. Find **Ad Skipper** in the list (may be under "Downloaded apps").
4. Toggle it **on** and confirm the permission prompt.
5. Return to the app — the status dot should turn **green**.

---

## Step 4 — Verify it works

Tell them to open YouTube, let an ad play, and watch for the skip button to be
clicked automatically. If it works, setup is done.

If it doesn't work, ask them to share:
- Their YouTube app version (Profile → Help & feedback → version number)
- Whether the skip button appears at all before the timeout

---

## Step 5 — Wrap up first boot

Once they're set up, ask them to fill in `USER.md` with their name, timezone, and
how they prefer to receive pattern-update notifications (GitHub issue, direct
message, or silent push).

Then delete this file. You won't need it again.

---

## Building from source (for users who want it)

If someone wants to build the APK themselves:

### Android Studio
1. Install Android Studio: https://developer.android.com/studio
2. **File > Open** → select the cloned repo folder.
3. Let Gradle sync finish.
4. **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. APK lands at: `app/build/outputs/apk/debug/app-debug.apk`

### Command-line
```bash
./gradlew assembleDebug
# Windows: gradlew.bat assembleDebug
```

---

## What you do after setup

Your ongoing job is pattern maintenance. YouTube periodically renames its internal
view IDs. When that happens, skip detection silently breaks. You run a weekly check
(Monday 9am) to catch it early. The current patterns are in:

`app/src/main/java/com/adskipper/AdSkipperService.kt` → `SKIP_PATTERNS`

If a user reports that skipping stopped working, see `TOOLS.md` for how to find
the new IDs and push a fix. A new APK release will then be needed — ask the user
to build and upload one, or note it in a GitHub issue.
