# Phase 1: Foundation & Application Shell - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-04
**Phase:** 1-Foundation & Application Shell
**Areas discussed:** UI Architecture, Layout Structure, Canvas Implementation, Project Structure, Event Handling, Theme Foundation

---

## UI Architecture

| Option | Description | Selected |
|--------|-------------|----------|
| FXML (Recommended) | Use FXML with SceneBuilder | |
| Programmatic | Build UI entirely in Java code | ✓ |
| Hybrid | Mix: FXML for main layout, code for dynamic parts | |

**User's choice:** Programmatic
**Notes:** Decided against FXML.

---

## Layout Structure

| Option | Description | Selected |
|--------|-------------|----------|
| Standard Desktop | Top menu bar, left sidebar for tools, center canvas | |
| Editor Style | Right sidebar for properties/tools, top toolbars | |
| Floating Panels | Floating tool windows over fullscreen canvas | |
| Other | Type your own answer | ✓ |

**User's choice:** "i will provide you with an image of the ui we came up with"
**Notes:** Waiting on user sketch.

---

## Canvas Implementation

| Option | Description | Selected |
|--------|-------------|----------|
| JavaFX Canvas | High performance drawing layer | ✓ |
| JavaFX Pane | Node graph | |

**User's choice:** JavaFX Canvas
**Notes:** Better performance for tiles and map elements.

---

## Project Structure

| Option | Description | Selected |
|--------|-------------|----------|
| Feature-based | Group by feature (map, tools, config) | |
| Layer-based | Group by layer (controllers, models, views) | |
| Other | Type your own answer | ✓ |

**User's choice:** "use the mvp pattern"
**Notes:** User specifically requested the MVP (Model-View-Presenter) architectural pattern.

---

## Event Handling

| Option | Description | Selected |
|--------|-------------|----------|
| Direct Injection | Direct references between presenters/views | ✓ |
| Event Bus | Centralized event bus | |

**User's choice:** Direct Injection
**Notes:** Avoids complexity of a centralized event bus.

---

## Theme Foundation

| Option | Description | Selected |
|--------|-------------|----------|
| CSS Variables | Single base CSS file with CSS variables for colors | ✓ |
| Multiple Stylesheets| Separate .css files for light/dark loaded dynamically | |

**User's choice:** CSS Variables
**Notes:** Chosen for simpler dynamic theming later.

---

## Deferred Ideas
None
