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
- **Goal:** Implement Points of Interest auto-generation, rendering, and editing UI.
- **Plans:** 4/4 plans complete
  - [x] 03-01-PLAN.md — POI data model (PointOfInterest, POIType enum, MapGrid integration)
  - [x] 03-02-PLAN.md — POI auto-generation rules (kingdoms, dungeons, landmarks, settlements)
  - [x] 03-03-PLAN.md — POI rendering and layer integration (overlay canvas, hover labels)
  - [x] 03-04-PLAN.md — POI UI controls (density sliders, list panel, editor modal)
- **Tasks:**
  - Implement POI data model and auto-generation rules.
  - Render POIs with colored circles and sprite icons.
  - Build POI editing dialog and density controls.
  - Deferred: Text labels, manual placement (Phase 4+).

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

## Phase 7: Refactor Generation Algorithm
- **Goal:** Split `MapGenerator` into multiple passes.
- **Plans:** 1 plans
  - [x] 07-refactor-generation-algorithm-01-PLAN.md — Refactor generation into separate passes.
- **Tasks:**
  - Create `GenerationParameters` record.
  - Create `MapGenerationPass` interface.
  - Implement `TerrainPass`, `HydrologyPass`, `KingdomPass`, and `POIPass`.
  - Refactor `MapGenerator` to use passes.
