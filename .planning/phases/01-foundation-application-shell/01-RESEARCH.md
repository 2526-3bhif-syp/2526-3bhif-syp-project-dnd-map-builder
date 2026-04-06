# Phase 1: Foundation & Application Shell - Research

**Researched:** 2026-04-04
**Domain:** JavaFX Application Shell & MVP Architecture
**Confidence:** HIGH

## Summary

This phase establishes the foundational architecture for the DnD Map Builder using JavaFX 21.0.6 and Java 24. The core approach centers around a strict Model-View-Presenter (MVP) architecture without FXML, leveraging programmatic JavaFX building.

**Primary recommendation:** Use standard JavaFX Layout containers (`BorderPane`, `VBox`, `HBox`, `SplitPane`) built programmatically in the View classes, wired to Presenters via direct constructor injection. Use JavaFX `Canvas` for the central drawing area, and JavaFX CSS with custom properties (`-fx-base`) for theming.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Build UI programmatically using Java code (No FXML/SceneBuilder).
- **D-02:** Follow the UI sketch provided by the user for the primary application window layout. This features a central canvas, a collapsible left sidebar for generator parameters (with tabs like Welt, POI, Königreiche, etc., sliders for stats, seed input, and generate buttons), a top-right action bar (Speichern, Laden, Export, Drucken), and a collapsible bottom-right layer visibility panel (Markierungen, POIs, Strukturen, Berge, Flüsse, Grid).
- **D-03:** Use a JavaFX `Canvas` node as the high-performance drawing layer for rendering the map and its features.
- **D-04:** Organize the Java packages using the MVP (Model-View-Presenter) architectural pattern.
- **D-05:** Use Direct Injection for communication between Presenters and Views (no centralized event bus).
- **D-06:** Establish styling using CSS variables to easily support light/dark modes and custom themes later.

### the agent's Discretion
None specified.

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope.
</user_constraints>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JavaFX `javafx.controls` | 21.0.6 | UI layout containers and widgets | Required foundation; provides `BorderPane`, `VBox`, `Accordion` |
| JavaFX `javafx.graphics` | 21.0.6 | High performance 2D drawing API | Provides `Canvas` and `GraphicsContext` needed for map rendering |
| JavaFX CSS (`.css`) | Built-in | Theming and variable management | Supports `-fx-` variables for robust dark/light modes |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| FXML/Controllers | Programmatic Java UI | Programmatic is strictly required per D-01. Much easier refactoring and strictly typed dependency injection in MVP. |
| EventBus / Guava | Direct Injection | Direct injection (D-05) is safer, highly traceable, and avoids "magic" decoupling that plagues large JavaFX applications. |

## Architecture Patterns

### Recommended Project Structure
```text
src/main/java/com/mapbuilder/mapbuilder/
├── model/
│   └── MapSettings.java      # State and generator parameters
├── view/
│   ├── MainView.java         # Root layout (BorderPane)
│   ├── LeftSidebarView.java  # Sliders, Tabs, Generate button
│   ├── CanvasView.java       # Wraps the JavaFX Canvas
│   ├── TopBarView.java       # Action buttons
│   └── RightSidebarView.java # Layer toggles
├── presenter/
│   ├── MainPresenter.java    # Coordinates sub-presenters
│   ├── SidebarPresenter.java # Handles generator inputs
│   └── CanvasPresenter.java  # Drives rendering based on model
└── App.java                  # Replaces HelloApplication, bootstraps MVP
```

### Pattern 1: Programmatic MVP with Direct Injection
**What:** The View holds UI components and exposes public methods to register callbacks/listeners. The Presenter constructs/receives the View, binds to its events, and updates the View when the Model changes.
**When to use:** Everywhere in this project (per D-04 and D-05).
**Example:**
```java
// View Layer
public class MapSidebarView extends VBox {
    private final Button generateBtn;
    public MapSidebarView() {
        generateBtn = new Button("Karte Generieren");
        getChildren().add(generateBtn);
    }
    public void setOnGenerate(Runnable action) {
        generateBtn.setOnAction(e -> action.run());
    }
}

// Presenter Layer
public class MapSidebarPresenter {
    private final MapSidebarView view;
    private final MapModel model;

    public MapSidebarPresenter(MapSidebarView view, MapModel model) {
        this.view = view;
        this.model = model;
        // Direct injection wiring
        this.view.setOnGenerate(this::handleGenerate);
    }
    private void handleGenerate() {
        // interact with model
    }
}
```

### Anti-Patterns to Avoid
- **FXML & Controllers:** Strictly banned by D-01. Do not generate `.fxml` files.
- **Presenter extending JavaFX Nodes:** Presenters should be POJOs. Only Views should extend `VBox`, `BorderPane`, etc.
- **Model accessing View:** The model must be completely ignorant of JavaFX nodes.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Application Layout | Custom coordinate math | JavaFX `BorderPane` | Standard desktop layout matches the UI sketch (Top: ActionBar, Left: Sidebar, Center: Canvas, Right: Layers). |
| Collapsible Panels | Custom hide/show animations | JavaFX `TitledPane` or `Accordion` | Built-in components handle focus, state, and standard UI expectations flawlessly. |

## Common Pitfalls

### Pitfall 1: Canvas Scaling and DPI
**What goes wrong:** The map looks blurry on high-DPI (Retina/4K) displays.
**Why it happens:** JavaFX Canvas dimensions are in logical pixels, not physical pixels.
**How to avoid:** Ensure the canvas rendering routine takes the screen's DPI scale into account, or rely on JavaFX's built-in scaling management by cleanly redrawing upon resize events.

### Pitfall 2: Blocking the JavaFX Application Thread
**What goes wrong:** UI freezes when clicking "Karte Generieren".
**Why it happens:** Running long generation tasks directly in the Presenter's event handler.
**How to avoid:** Even though generation logic is added later, the shell must set up `Task<Void>` or `CompletableFuture` patterns for background work if generation takes > 16ms.

### Pitfall 3: CSS Variables Scope
**What goes wrong:** CSS variables like `-color-primary` don't apply correctly.
**Why it happens:** Custom CSS properties in JavaFX must be declared correctly and usually prefix with `-fx-` or are resolved awkwardly if not set at the `.root` level.
**How to avoid:** Define themes in a centralized `theme.css` under `.root { -fx-base: #2A2A2A; ... }`.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| FXML + SceneBuilder | Fluent Programmatic UI | Continuous | Easier refactoring, better type safety, cleaner Git diffs, strict MVP adherence. |

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java | Runtime | ✓ | 24 | — |
| Gradle | Build Tool | ✓ | 8+ | — |

## Code Examples

### CSS Variable Definition (`style.css`)
```css
.root {
    -fx-base-color: #2b2b2b;
    -fx-accent-color: #4a90e2;
    -fx-text-fill: #e0e0e0;
    -fx-background-color: -fx-base-color;
}

.button {
    -fx-background-color: -fx-accent-color;
    -fx-text-fill: white;
}
```

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Confirmed via `build.gradle.kts`.
- Architecture: HIGH - Dictated by explicit user decisions (MVP + programmatic).
- Pitfalls: HIGH - Standard JavaFX known issues (Canvas DPI, Threading).

**Research date:** 2026-04-04
**Valid until:** 2026-05-04
