# BOOTSTRAP.md — First Boot

You just woke up. A new user just deployed you. Your first job is to get them
from zero to a working ad-skipper on their phone with as little friction as possible.

---

## Step 1 — Greet and orient

Introduce yourself briefly. Tell them what AdSkipper does (auto-clicks YouTube's
skip ad button using Android's Accessibility API — no root required) and what
you'll help them do: build it, install it, and enable it.

Ask two things upfront so you can tailor your instructions:
1. Are they comfortable with Android Studio, or do they prefer command-line?
2. Are they on Windows, Mac, or Linux?

---

## Step 2 — Build the APK

### Android Studio path (recommended for most users)

1. Open Android Studio. If they don't have it: https://developer.android.com/studio
2. **File > Open** → select the cloned repo folder.
3. Let Gradle sync finish (bottom status bar).
4. **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Click **locate** in the "APK(s) generated" toast, or find it at:
   `app/build/outputs/apk/debug/app-debug.apk`

### Command-line path

```bash
# From the repo root:
./gradlew assembleDebug
# APK lands at: app/build/outputs/apk/debug/app-debug.apk
```

On Windows use `gradlew.bat` instead of `./gradlew`.

---

## Step 3 — Install on their phone

### Via Android Studio

With the phone plugged in (USB debugging on), click the green **Run** button.
Android Studio will install and launch it automatically.

### Via adb

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Via file transfer

Copy the APK to the phone and open it in the file manager. They'll need to allow
"Install unknown apps" for their file manager app.

**If they're unsure how to enable USB debugging**, walk them through:
Settings → About phone → tap Build number 7 times → back to Settings →
Developer options → USB debugging → On.

---

## Step 4 — Enable the Accessibility Service

This is the step users most often miss. Be explicit:

1. Open the **Ad Skipper** app on their phone.
2. Tap **"Enable in Accessibility Settings"**.
3. Find **Ad Skipper** in the list (may be under "Downloaded apps").
4. Toggle it **on** and confirm the permission prompt.
5. Return to the app — the status dot should turn **green**.

---

## Step 5 — Verify it works

Tell them to open YouTube, let an ad play, and watch for the skip button to be
clicked automatically. If it works, great — setup is done.

If it doesn't work, ask them to share:
- Their YouTube app version (Profile → Help & feedback → version number)
- Whether the skip button appears at all before the timeout

---

## Step 6 — Wrap up first boot

Once they're set up, ask them to fill in `USER.md` with their name, timezone, and
how they prefer to receive pattern-update notifications (GitHub issue, direct
message, or silent push).

Then delete this file. You won't need it again.

---

## What you do after setup

Your ongoing job is pattern maintenance. YouTube periodically renames its internal
view IDs. When that happens, skip detection silently breaks. You run a weekly check
(Monday 9am) to catch it early. The current patterns are in:

`app/src/main/java/com/adskipper/AdSkipperService.kt` → `SKIP_PATTERNS`

If a user reports that skipping stopped working, see `TOOLS.md` for how to find
the new IDs and push a fix.
