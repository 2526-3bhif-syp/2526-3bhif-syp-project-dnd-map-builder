# Project Roadmap

## Phase 1: Foundation & Application Shell
- **Goal:** Set up JavaFX project structure, main window, and basic canvas.
- **Plans:** 1 plans
  - [x] 01-foundation-application-shell-01-PLAN.md — Refactor to MVP and build primary UI shell.
- **Tasks:**
  - Initialize Java/JavaFX project structure.
  - Create main application window with menu bar and primary workspace.
  - Implement basic map canvas for rendering.

## Phase 2: Core Map Generation
- **Goal:** Implement random map generation logic and parameter configuration.
- **Plans:** 4 plans
  - [ ] 02-core-map-generation-01-PLAN.md — Implement backend Map Generation engine and core math
  - [ ] 02-core-map-generation-02-PLAN.md — Bind MapGenerator to UI and implement debounced rendering
  - [ ] 02-core-map-generation-03-PLAN.md — Implement Layer Panel UI based on user sketch
- **Tasks:**
  - Build map generation algorithm (terrain, features).
  - Create configuration UI for map parameters (size, terrain types, density).
  - Render generated map on canvas.

## Phase 3: Interactive Elements (POIs & Labels)
- **Goal:** Allow users to place and edit POIs and text labels.
- **Tasks:**
  - Implement POI data model and rendering.
  - Build POI editing dialog.
  - Implement text label placement and formatting tools.
  - Implement drag-and-drop/movement logic for objects.

## Phase 4: Grids, Borders & LOD
- **Goal:** Add grids, province borders, and zoom/LOD features.
- **Tasks:**
  - Implement square and hex grid overlays.
  - Build province generation and editing tools.
  - Implement zoom functionality with Level of Detail rendering.

## Phase 5: File Operations & Export
- **Goal:** Support saving, loading, and exporting maps.
- **Tasks:**
  - Implement custom save/load format with versioning.
  - Add Undo/Redo stack.
  - Implement export to PNG, SVG, and PDF.

## Phase 6: Polish & Theming
- **Goal:** Finalize themes, UI polish, and cross-platform testing.
- **Tasks:**
  - Implement theme/style switching mechanism.
  - Test across Windows, macOS, and Linux.
  - Final bug fixes and performance optimization.
