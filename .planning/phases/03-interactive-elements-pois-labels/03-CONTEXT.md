# Phase 03: Interactive Elements (POIs & Labels) - Context

**Gathered:** 2026-04-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Implement Points of Interest (POIs) auto-generation during map creation using world-building rules (kingdom-based cities, dungeon placement at multi-kingdom borders, landmarks at terrain features). POIs are rendered with colored circles + sprite icons on a separate overlay canvas. Users can view, edit (name, type, description, icon, color), and toggle visibility. Text labels are deferred to Phase 4+.

</domain>

<decisions>
## Implementation Decisions

### POI Data Model
- **D-01 (Standalone Storage):** MapGrid holds a `List<PointOfInterest>`. Each POI has independent x/y coordinates (not bound to single MapCell). Enables flexible placement, querying, serialization, and later modifications.
- **D-02 (POI Properties):** Each POI instance contains:
  - `id` (UUID or int)
  - `x, y` (map coordinates as integers)
  - `type` (enum: CITY, DUNGEON, LANDMARK, RUIN, VILLAGE, CASTLE, TOWER, CAVE, SHRINE, TAVERN, etc. — expandable)
  - `name` (user-editable string, initially generated)
  - `description` (user-editable text, may be empty)
  - `customColor` (ARGB, nullable — if null, use type-based default)
  - `customIcon` (optional icon type override, nullable)
  - `createdAt` / `createdByRule` (metadata: "kingdom_capital", "border_dungeon", "terrain_landmark", or "user_placed" for future manual placement)

### POI Auto-Generation Rules
- **D-03 (Kingdom Cities):** Place a CITY POI at each kingdom's capital cell (already identified in Phase 2). All biomes are viable for cities (no filtering). Assign city name based on kingdom ID or user preferences (TBD in planner).
- **D-04 (Dungeon/Ruin Placement):** Place DUNGEON POIs at strategic terrain-based locations:
  - **Multi-Kingdom Borders:** Cell where 3+ kingdom borders meet (high conflict zones).
  - **Cave/Mountain Clusters:** Cells in high-elevation zones (Bare Rock, Snow, etc.) with low kingdom ownership probability.
  - **Rule Frequency:** User controls density via slider: `dungeonDensity` (0.0–1.0, default 0.5). Scales the count of placed dungeons relative to map area.
- **D-05 (Landmark Placement):** Place LANDMARK POIs at extreme terrain features:
  - **Waterfalls/Rapids:** Cells with high elevation adjacent to water (rivers/coasts).
  - **Mountain Peaks:** Highest elevation cells per kingdom cluster.
  - **Rule Frequency:** User controls via slider: `landmarkDensity` (0.0–1.0, default 0.3).
- **D-06 (Additional Villages/Towns):** Optionally scatter VILLAGE/TOWN POIs in habitable biomes (grassland, temperate forest, savanna):
  - **Probability per Biome:** Each biome defines a base settlement probability. Grassland/temperate forest higher, desert/tundra lower.
  - **Spacing:** Poisson-disc sampling with minimum distance (e.g., 5–10 cells apart) to prevent clustering.
  - **Rule Frequency:** User controls via slider: `settlementDensity` (0.0–1.0, default 0.4).
- **D-07 (User Parametrization):** Right-side panel adds three new sliders:
  - `Dungeon Density` (0.0–1.0)
  - `Landmark Density` (0.0–1.0)
  - `Settlement Density` (0.0–1.0)
  - These sliders trigger map regeneration (POI list cleared and regenerated), following Phase 2 debounce pattern.

### POI Visual Representation
- **D-08 (Rendering Layer):** POIs render on a separate JavaFX Canvas (overlaid above the map biome/kingdom canvas via StackPane). This decouples POI rendering from biome/kingdom updates and prevents flicker when parameters change.
- **D-09 (Marker Style):** Each POI renders as:
  - **Colored Circle:** 12–16px diameter, color derived from POI type (or custom override). Drawn first.
  - **Icon Sprite:** 16x16px icon from sprite sheet, centered on circle. Type-based icon mapping (city → building, dungeon → skull, etc.).
- **D-10 (Icon Assets):** POI icons provided as a single sprite sheet PNG/SVG (e.g., `assets/poi-icons.png`, 256x256 px containing 16 unique 16x16 icons). Icons are tightly packed; index mapping in code (e.g., `icon_index = 0` = CITY at 0,0; `icon_index = 1` = DUNGEON at 16,0, etc.).
- **D-11 (Labels on Hover):** POI name labels appear ONLY on mouse hover, rendered above marker. Font size ~11px, white/black shadow for legibility. No labels by default to avoid clutter.
- **D-12 (Layer Toggle):** Layer panel's "Punkte von Interesse" toggle controls POI overlay visibility (canvas set to opacity 0 or 1). Does NOT affect POI data; data persists.

### POI Editing & Metadata
- **D-13 (Sidebar List + Modal Editor):** Right panel includes a POI list section below the layer toggles showing all POIs (name + type icon + coordinates). Clicking a POI opens a modal dialog.
- **D-14 (Modal POI Editor):** Modal contains:
  - Text field: POI name (editable)
  - Dropdown: POI type (enum: CITY, DUNGEON, LANDMARK, etc.)
  - Text area: Description (editable, multi-line)
  - Color picker: Custom color ARGB (nullable; if cleared, uses type default)
  - Icon picker: Select override icon or use type default
  - Buttons: Save, Cancel, Delete (delete removes POI from list)
- **D-15 (No Manual Placement in Phase 3):** Phase 3 focuses on viewing and editing auto-generated POIs. Manual placement (drag-to-place, right-click context menu) deferred to Phase 4.

### Text Labels
- **D-16 (Deferred to Phase 4+):** Text labels (standalone or POI-attached) are NOT implemented in Phase 3. POI names appear on hover only. Phase 4+ can add a separate TextLabel model with independent placement/editing, or tie labels directly to POIs with configurable visibility.

### Integration & Architecture
- **D-17 (MVP Pattern Continuation):** POI data model resides in `MainModel` (via `MapGrid.getPointsOfInterest()`). POI rendering logic in `MainPresenter` (new method `renderPOIs()` called after main map render). POI UI (sidebar list, modal editor) in `MainView`.
- **D-18 (Rendering Pipeline):**
  ```
  User adjusts POI density sliders
    ↓ (debounce 300ms)
  MainPresenter.triggerGeneration()
    ↓
  MapGenerator.generate(grid) → generates biomes, kingdoms, then POIs via world-building rules
    ↓ (on MainModel update)
  MainPresenter.renderMap() — renders biome/kingdom to main canvas
  MainPresenter.renderPOIs() — renders POI overlay canvas independently
    ↓
  Both canvases visible in StackPane
  ```
- **D-19 (Overlay Canvas Details):** POI canvas positioned above map canvas in StackPane. Same dimensions as map canvas. Click events on POI canvas bubble to map canvas for zoom/pan (or POI selection captured first, then non-POI clicks fall through).
- **D-20 (Debounce & Async):** POI generation included in `MapGenerator.generate()` call (same async background thread as biome generation). No separate async phase.

### Layer Panel UI Updates
- **D-21 (New UI Elements):**
  - Add three sliders below layer toggles: Dungeon Density, Landmark Density, Settlement Density (0.0–1.0 range, default 0.5/0.3/0.4).
  - Add subsection in right panel: "Points of Interest" list showing all POIs (sortable by name/type, optional search box).
  - Clicking POI in list opens modal editor.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Specs
- `.planning/REQUIREMENTS.md` — Functional Requirement 4 (POI management: auto-generate and manage).
- `.planning/phases/02-core-map-generation/02-CONTEXT.md` — Prior decisions on data structure (MapGrid/MapCell), rendering pipeline, and MVP architecture.
- `.planning/phases/01-foundation-application-shell/01-CONTEXT.md` — JavaFX Canvas and MVP structure.

### World-Building & Procedural Generation
- *(No external references; generation rules defined in-context above)*

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets & Patterns
- **MapGrid & MapCell:** Existing model used in Phase 2; extend MapGrid with `List<PointOfInterest> pois` field and getter/setter.
- **MapGenerator:** Existing generation loop; extend with new POI generation phase after biome/kingdom resolution.
- **Rendering Pipeline:** `MainPresenter.renderMap()` handles biome/kingdom rendering to pixel array. Add new method `renderPOIs()` to draw overlay canvas.
- **MVP Binding:** `MainPresenter` uses debounce for parameter changes; reuse for new POI density sliders.
- **StackPane Layout:** MainView already uses StackPane in right panel; can extend for POI list section.
- **Canvas & GraphicsContext:** Existing map canvas has GraphicsContext; overlay canvas created similarly (new Canvas added to StackPane).

### Integration Points
- **UI Layer:** MainView.setupRightLayersPanel() (line 357) already lists "Punkte von Interesse"; extend this method to include density sliders and POI list.
- **Presenter:** MainPresenter.renderMap() completes at line ~150; add renderPOIs() call immediately after.
- **Model:** MainModel wraps MapGrid; MapGrid gains POI list field.
- **Canvas Events:** MainView.canvasContainer handles zoom/pan; POI overlay canvas must integrate into event flow (click to select POI, allow drag-pan if no POI clicked).

### Rendering Details
- **Pixel Rendering:** Existing code uses `PixelWriter` for direct pixel manipulation. POI overlay uses `GraphicsContext.drawImage()` (sprite sheet) and `fillOval()` (circles).
- **Coordinates:** Map cells use integer x/y (0 to gridSize-1). POIs also use integer x/y. Rendering multiplies by zoom factor and applies translation for viewport centering.

### Performance Considerations
- **Overlay Canvas Separate:** POI canvas not affected by biome/kingdom changes; avoids redundant redraws.
- **Sprite Sheet:** Single image asset (256x256px) minimizes memory; index mapping avoids image duplication.
- **Hover Detection:** Track mouse position; only redraw labels on hover change (not every frame).

</code_context>

<specifics>
## Specific Ideas

- **POI Naming Convention:** Cities could inherit kingdom name + suffix (e.g., "Ashland City" for kingdom "Ashland"). Dungeons could use generic fantasy names (Darkwood Cavern, Blackthorn Crypt) or coordinate-based (Dungeon @ 42,67). Planner to decide.
- **Icon Asset Management:** Provide a mapping file (JSON or enum constant) documenting which sprite sheet index maps to which POI type. Example: `{ "CITY": 0, "DUNGEON": 1, "LANDMARK": 2, ... }`.
- **POI Count Feedback:** Display count in UI ("12 Cities, 8 Dungeons, 5 Landmarks") after generation to show user impact of density sliders.
- **Future Extensibility:** POI type enum should be easily expandable (Phase 4 might add SHRINE, TOWER, SHIPWRECK, etc. without restructuring core logic).
- **Backward Compatibility:** Phase 5 save/load must serialize POI list. Design PointOfInterest with serialization-friendly fields (all primitives, no circular references).

</specifics>

<deferred>
## Deferred Ideas

### Text Labels
- Independent text label system (Phase 4+).
- Optional per-POI labels with on/off toggle (Phase 4+).
- Label collision detection / stacking (Phase 4+).

### Manual POI Placement
- Drag-to-place new POIs on canvas (Phase 4+).
- Right-click context menu for POI creation (Phase 4+).
- Undo/Redo for POI operations (Phase 5, integrated with main undo stack).

### Advanced POI Features
- POI search/filter in sidebar (Phase 4+).
- POI categories/grouping (Phase 4+).
- Custom POI types via user plugins (Phase 6+).
- POI relationships (e.g., "this dungeon belongs to this kingdom") — might emerge in Phase 5 during data modeling.

### Performance Optimization
- Quadtree spatial indexing for POI lookup (consider Phase 4+ if POI count exceeds 1000+).
- Viewport culling: only render POIs visible in current zoom/pan window (Phase 4+).

</deferred>

---

*Phase: 03-interactive-elements-pois-labels*
*Context gathered: 2026-04-24*
