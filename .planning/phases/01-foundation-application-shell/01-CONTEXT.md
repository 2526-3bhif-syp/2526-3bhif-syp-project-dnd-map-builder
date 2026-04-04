# Phase 1: Foundation & Application Shell - Context

**Gathered:** 2026-04-04
**Status:** Ready for planning

<domain>
## Phase Boundary

Setting up the initial JavaFX application shell, project architecture, and core map rendering canvas. This establishes the foundation upon which map generation and editing features will be built.

</domain>

<decisions>
## Implementation Decisions

### UI Architecture
- **D-01:** Build UI programmatically using Java code (No FXML/SceneBuilder).

### Layout Structure
- **D-02:** Follow the UI sketch provided by the user for the primary application window layout.

### Canvas Implementation
- **D-03:** Use a JavaFX `Canvas` node as the high-performance drawing layer for rendering the map and its features.

### Project Structure
- **D-04:** Organize the Java packages using the MVP (Model-View-Presenter) architectural pattern.

### Event Handling
- **D-05:** Use Direct Injection for communication between Presenters and Views (no centralized event bus).

### Theme Foundation
- **D-06:** Establish styling using CSS variables to easily support light/dark modes and custom themes later.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Specs
- `.planning/REQUIREMENTS.md` — Core requirements and features.
- `.planning/ROADMAP.md` — Phase definitions.

### External Assets
- `User's UI Sketch` — The user will provide a sketch detailing the window layout. Downstream agents should request this if not provided.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- None. The existing app is an empty `HelloApplication` template using FXML, which will be refactored per D-01.

### Established Patterns
- **Language/Framework:** Java 21+ and JavaFX.

### Integration Points
- `src/main/java/com/mapbuilder/mapbuilder/HelloApplication.java` will be the entry point to refactor into the main MVP initialization.

</code_context>

<specifics>
## Specific Ideas

- The user specifically requested the MVP architecture.
- The UI layout will strictly follow a sketch provided by the user.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 01-foundation-application-shell*
*Context gathered: 2026-04-04*
