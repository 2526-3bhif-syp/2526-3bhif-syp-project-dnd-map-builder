---
phase: 01-foundation-application-shell
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - "src/main/java/module-info.java"
  - "src/main/java/com/mapbuilder/mapbuilder/HelloApplication.java"
  - "src/main/java/com/mapbuilder/mapbuilder/core/MVPBase.java"
  - "src/main/java/com/mapbuilder/mapbuilder/main/MainView.java"
  - "src/main/java/com/mapbuilder/mapbuilder/main/MainPresenter.java"
autonomous: true
requirements: [FR-01]

must_haves:
  truths:
    - "Application starts up without FXML."
    - "A main window is visible."
    - "The main window contains the layout sketched by the user (center canvas, side panels)."
  artifacts:
    - path: "src/main/java/com/mapbuilder/mapbuilder/HelloApplication.java"
      provides: "Entry point and JavaFX Application initialization."
    - path: "src/main/java/com/mapbuilder/mapbuilder/main/MainView.java"
      provides: "Programmatic UI layout with sidebars and a central canvas."
    - path: "src/main/java/com/mapbuilder/mapbuilder/main/MainPresenter.java"
      provides: "Controller logic for the main window."
  key_links:
    - from: "src/main/java/com/mapbuilder/mapbuilder/HelloApplication.java"
      to: "src/main/java/com/mapbuilder/mapbuilder/main/MainView.java"
      via: "instantiation and scene assignment"
---

<objective>
Refactor the initial JavaFX project to use a programmatic MVP (Model-View-Presenter) architecture, removing FXML.
Establish the main application window with a Left Panel (Generator Settings), Center Canvas, Top Right Action Bar, and Bottom Right Layers Panel, according to the user's sketch.

Purpose: Foundation for map rendering and UI interactions without SceneBuilder overhead, enabling CSS theming.
Output: Working main application shell with a drawing canvas and empty configuration panels.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/01-foundation-application-shell/01-CONTEXT.md
</context>

<tasks>

<task type="auto">
  <name>Task 1: Clean up FXML and set up MVP structure</name>
  <files>src/main/java/module-info.java, src/main/resources/com/mapbuilder/mapbuilder/hello-view.fxml, src/main/java/com/mapbuilder/mapbuilder/HelloController.java</files>
  <action>
    - Delete `hello-view.fxml` and `HelloController.java`.
    - Update `module-info.java` to remove `requires javafx.fxml;` and `opens ... to javafx.fxml;`.
    - Create `src/main/java/com/mapbuilder/mapbuilder/core/MVPBase.java` to define base interfaces (`View`, `Presenter`).
  </action>
  <verify>
    <automated>mvn clean compile</automated>
  </verify>
  <done>No FXML files exist, module-info is updated, and core MVP interfaces are defined.</done>
</task>

<task type="auto">
  <name>Task 2: Implement Main Window Layout (D-01, D-02)</name>
  <files>src/main/java/com/mapbuilder/mapbuilder/HelloApplication.java, src/main/java/com/mapbuilder/mapbuilder/main/MainView.java, src/main/java/com/mapbuilder/mapbuilder/main/MainPresenter.java</files>
  <action>
    - Create `MainView` (implements View) extending `BorderPane`.
    - Left side: `VBox` for Generator Settings (Seed, Tabs, Sliders, Generate Button).
    - Center: `Canvas` wrapped in a resizable container (e.g., `Pane`) for map rendering (D-03).
    - Right side: `VBox` with Top Action Bar (Buttons: Speichern, Laden, Export, Drucken) and Bottom Layers Panel (Toggle layers).
    - Create `MainPresenter` (implements Presenter) to manage `MainView` interactions.
    - Refactor `HelloApplication` to instantiate `MainView`, wrap it in a `Scene`, and set it on the primary stage.
  </action>
  <verify>
    <automated>mvn javafx:run -DskipTests</automated>
  </verify>
  <done>Application starts, showing the complete layout (Left panel, Right panel, Top bar, Center Canvas) using programmatic UI and MVP.</done>
</task>

</tasks>

<verification>
Ensure the application compiles without FXML dependencies and the layout renders correctly upon launch.
</verification>

<success_criteria>
- The project successfully compiles using Maven.
- FXML files and controllers are completely removed.
- The `MainView` provides the requested layout structure with a central Canvas.
- The JavaFX app launches correctly.
</success_criteria>

<output>
After completion, create `.planning/phases/01-foundation-application-shell/01-foundation-application-shell-01-SUMMARY.md`
</output>