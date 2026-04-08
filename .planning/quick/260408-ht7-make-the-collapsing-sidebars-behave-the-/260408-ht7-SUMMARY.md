---
phase: quick
plan: 260408-ht7
type: execute
wave: 1
status: completed
subsystem: ui
tags: [ui, sidebar, css]
tech-stack:
  - javafx
  - css
key-files:
  modified:
    - src/main/java/com/mapbuilder/mapbuilder/main/MainView.java
decisions:
  - Applied CSS to remove default ScrollPane borders (`-fx-background: #2b2b2b; -fx-padding: 0; -fx-border-color: transparent; -fx-border-width: 0; -fx-background-insets: 0;`).
  - Verified animation logic matches exactly for both sidebars.
metrics:
  duration: 5s
  tasks-completed: 2
  tasks-total: 2
  files-modified: 1
---

# Phase quick Plan 260408-ht7: make the collapsing sidebars behave the same Summary

Eliminated the 1px white border offset on the left sidebar by applying custom CSS to the `ScrollPane`, ensuring it matches the background and has no borders. Verified that the collapsing animation behaviors for both left and right sidebars are identical.

## Completed Tasks

1. **Task 1: Fix 1px offset and styling on the left panel**
   - Modified `leftScroll` styling in `setupLeftPanel` to eliminate the default 1px border.
   - Commit: `9007daa`

2. **Task 2: Ensure identical collapse behavior**
   - Reviewed `TranslateTransition` for the left sidebar and confirmed the logic matches the right sidebar precisely.
   - Commit: `c584d8b`

## Deviations from Plan

None - plan executed exactly as written.

## Self-Check: PASSED

- `src/main/java/com/mapbuilder/mapbuilder/main/MainView.java` exists and has the expected changes.
- Commits `9007daa` and `c584d8b` exist in the log.
