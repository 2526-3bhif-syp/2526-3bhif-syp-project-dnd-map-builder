---
phase: quick
plan: 1
type: execute
wave: 1
depends_on: []
files_modified:
  - src/main/java/com/mapbuilder/mapbuilder/main/MainView.java
autonomous: true
requirements: []
must_haves:
  truths:
    - "Left panel and top action bar use the same dark theme as the layers panel"
    - "Text on the left panel is readable with white color"
  artifacts:
    - path: src/main/java/com/mapbuilder/mapbuilder/main/MainView.java
      provides: "MainView component"
  key_links: []
---

<objective>
Modify the UI components in MainView (Left Panel and Top Action Bar) to match the dark theme style of the refactored layers panel.

Purpose: To ensure a consistent and modern UI look across the entire application.
Output: Updated MainView.java with unified styling.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
</execution_context>

<context>
@.planning/STATE.md
@src/main/java/com/mapbuilder/mapbuilder/main/MainView.java
</context>

<tasks>

<task type="auto">
  <name>Task 1: Apply dark theme to Left Panel and Top Action Bar</name>
  <files>src/main/java/com/mapbuilder/mapbuilder/main/MainView.java</files>
  <action>
    Modify the styling of `leftPanel`, `topActionBar`, `showLeftBtn`, and `collapseLeftBtn` to match the `layersPanel` styling.
    
    Specific changes:
    - Change `leftPanel` background to `#2b2b2b` and set text fill to white for all labels inside it. Update the border/radius to match the right panel but mirrored if needed.
    - Change `topActionBar` background to `#2b2b2b` and set its buttons' text fill to white or a suitable contrast color.
    - Update `showLeftBtn` and `collapseLeftBtn` to use the same background and text colors as the right panel toggles (e.g., `#2b2b2b` and white text).
    - Ensure all labels added directly to `leftPanel` (like "Seed", "Map Size", etc.) are created explicitly with a white text fill style, or set a global stylesheet class if easier (though inline styles seem to be the current pattern).
  </action>
  <verify>grep -q "#2b2b2b" src/main/java/com/mapbuilder/mapbuilder/main/MainView.java</verify>
  <done>The left panel and top action bar share the dark theme of the right panel, and text is readable.</done>
</task>

</tasks>

<verification>
Check if all panels use `#2b2b2b` as their background color and text is readable.
</verification>

<success_criteria>
MainView.java is updated with unified dark theme styling for all panels without breaking existing functionality.
</success_criteria>

<output>
After completion, create `.planning/phases/quick/quick-1-SUMMARY.md`
</output>
