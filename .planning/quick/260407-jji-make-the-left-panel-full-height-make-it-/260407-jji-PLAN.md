---
phase: quick
plan: 1
type: execute
wave: 1
depends_on: []
files_modified:
  - src/main/java/com/mapbuilder/mapbuilder/main/MainView.java
  - src/main/resources/styles.css
autonomous: true
requirements: []
must_haves:
  truths:
    - Left panel is full height and connects with the screen border.
    - Scrollbar is removed from the left panel.
  artifacts:
    - path: src/main/java/com/mapbuilder/mapbuilder/main/MainView.java
      provides: Layout and structure for the left panel.
    - path: src/main/resources/styles.css
      provides: Styling for scrollbar hiding and panel borders.
  key_links: []
---

<objective>
Make the left panel full height, connect with the screen border like the layers panel, and remove its scrollbar.
Purpose: Improve the UI consistency by matching the left panel's layout and styling to the layers panel.
Output: Updated MainView layout and CSS.
</objective>

<context>
@src/main/java/com/mapbuilder/mapbuilder/main/MainView.java
@src/main/resources/styles.css
</context>

<tasks>

<task type="auto">
  <name>Task 1: Adjust Left Panel Layout and Styling</name>
  <files>src/main/java/com/mapbuilder/mapbuilder/main/MainView.java, src/main/resources/styles.css</files>
  <action>
    - Inspect `MainView.java` to identify the left panel container (likely a VBox or ScrollPane on the left of a BorderPane or HBox).
    - Modify the layout constraints (margins, paddings, anchors, Vgrow/Hgrow) so the left panel extends full height (top to bottom) and touches the left screen border, matching the behavior of the layers panel (right panel).
    - Identify the ScrollPane (or similar scrolling container) associated with the left panel.
    - Update `styles.css` (and/or inline styles in `MainView.java`) to hide the scrollbar for the left panel. In JavaFX, this is typically done by setting the ScrollPane policy (`setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER)`) or using CSS `.scroll-pane .scroll-bar:vertical { -fx-opacity: 0; }`. Ensure scrolling still functions via mouse wheel if required, or simply hide the visual scrollbar.
  </action>
  <verify>
    <automated>./gradlew build</automated>
  </verify>
  <done>Left panel spans the full height of the window, connects seamlessly to the left border, and has no visible scrollbar.</done>
</task>

</tasks>

<verification>
Review the JavaFX application layout manually or via tests to ensure visual requirements are met.
</verification>

<success_criteria>
The left panel is full-height without margins from the window border and its scrollbar is completely hidden.
</success_criteria>

<output>
After completion, create `.planning/quick/260407-jji-make-the-left-panel-full-height-make-it-/260407-jji-SUMMARY.md`
</output>
