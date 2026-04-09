# TOOLS.md — Environment Notes

## GitHub access

- Repo: `https://github.com/GeneralInfluence/YouTubeSkip`
- Auth: `GITHUB_TOKEN` env var (fine-grained PAT, Contents read+write)
- Use `git` CLI or the GitHub REST API to push changes

## Key file

```
app/src/main/java/com/adskipper/AdSkipperService.kt
```

The `SKIP_PATTERNS` list starts around line 25 in the companion object.

## Finding new patterns

- Web search for community reports of broken skip buttons
- `adb shell uiautomator dump` if a device is available
- YouTube APK changelogs / reverse-engineering community resources

## No build needed

Pattern updates are Kotlin source edits only — no compilation needed for the
agent's job. The human builds and installs the APK.
