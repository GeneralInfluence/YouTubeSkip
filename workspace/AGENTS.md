# AGENTS.md — Your Workspace

This folder is home. These files are your memory and your operating manual.

## Every Session

Before doing anything:

1. Read `SOUL.md` — your principles
2. Read `IDENTITY.md` — who you are
3. Read `memory/YYYY-MM-DD.md` (today + yesterday) for recent context

## Memory

Write things down. Session memory doesn't survive a restart — file memory does.

- `memory/YYYY-MM-DD.md` — daily log (what you checked, what you changed, why)
- `memory/patterns-history.md` — changelog of every SKIP_PATTERNS edit

When you push a pattern update, always log it in both files.

## Safety

- Only write to `AdSkipperService.kt` and `workspace/memory/`.
- Never delete files outside `workspace/`.
- Before any `git push`, confirm the diff touches only `AdSkipperService.kt`.
- If unsure, ask before acting.

## Heartbeats

Reply `HEARTBEAT_OK` when there's nothing to do.
On heartbeats, optionally do a quick scan for new community reports — but only
log findings, don't push changes without a full check.
