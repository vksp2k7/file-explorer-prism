# Merged code fixes — techflyers/Prism-File-Explorer

This tree is a **shallow clone of upstream master** with all safe code-side fixes from the issue findings report applied in one merge.

**Base commit:** `b2ecc8ab50ae1f3b580f4a3483042de67d311a3f` (or current origin/master at clone time)

## Included fixes

| Issue | What changed |
|-------|----------------|
| #70 | Tool tabs open in new tab; Back via parentTab/id; Vault relocks on stop |
| #72 | Horizontal swipe on CategoriesRow to change category |
| #73 | Bottom-bar icons use label as contentDescription |
| #74 | Tasks/Bookmarks overflow only when not on bottom bar |
| #75 | TypeFilterRibbon + contentSnapshot filter for ordinary folders |
| #77 | One-time battery optimization reminder (general settings only) |
| #78 | openFolderImpl latest-request-wins while loading |
| #79 | Free space on storage cards; tab reorder by identity; Close-others confirmation |
| #80 | Ordinary folder open restores scroll position |
| #81 | Scroll-above-ribbon delay 60ms → 16ms |
| #82 | `.github/ISSUE_TEMPLATE/config.yml` for Discussions routing |

## Not included (still open)

- #71 Copy-into session
- #76 ANR (needs device trace)
- #61 / #69 validation only
- Full nested gesture ownership, Toast→snackbar, drawer edit, adaptive layout, two-file SHA-256 UI

## Build

```bash
./gradlew assembleDebug
```

Device-test the acceptance scenarios from the findings report before closing issues.
