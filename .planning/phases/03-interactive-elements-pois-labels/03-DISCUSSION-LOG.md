# Phase 03: Interactive Elements (POIs & Labels) - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-24
**Phase:** 03-interactive-elements-pois-labels
**Areas discussed:** POI Data Model & Attachment, POI Visual Representation, POI Placement Interaction, POI Editing & Metadata, Text Labels vs. POIs, Layer Visibility & Performance

---

## POI Data Model & Attachment

| Option | Description | Selected |
|--------|-------------|----------|
| Standalone list in MapGrid | MapGrid holds a List<PointOfInterest>. POIs have independent x/y coordinates. Simplest, most flexible — easier to add/remove, serialize, and query for rendering. | ✓ |
| One POI per MapCell | Each MapCell optionally holds a POI. Couples POI to grid cells — limits to one POI per cell, but ties placement to grid structure. | |
| Free-floating coordinates with index | MapGrid holds a spatial index (QuadTree, Grid-based) for fast POI lookup by proximity. Most complex but supports dense POI queries efficiently. | |

**User's choice:** Standalone list in MapGrid
**Notes:** Clean separation, flexible for future expansion. Spatial indexing deferred to Phase 4+ if needed.

---

## POI Visual Representation

| Option | Description | Selected |
|--------|-------------|----------|
| Simple circular markers with colors | Colored circles (size ~10px) on canvas. Minimal rendering cost, instant feedback. User can choose color per POI. | |
| Icon symbols from enum + names | Predefined icon set (castle, town, ruins, treasure, etc.). Requires asset management but more visually distinctive. Names appear in labels above icons. | |
| Markers with text labels | Circle marker + name label directly adjacent. Combines position clarity with identifier. | |

**User's choice:** Colored circles with icons + hover labels (hybrid approach)
**Notes:** User specified: colored circles as base + sprite icons overlaid. Labels appear ONLY on hover, not by default. Keeps map uncluttered while maintaining visual clarity. User preference for visual distinction via icons over raw circles.

---

## POI Icon Assets

| Option | Description | Selected |
|--------|-------------|----------|
| Sprite sheet icons (PNG/SVG) | Use 16x16 or 24x24 pixel PNG/SVG icons embedded in a sprite sheet. Assets included in jar; fastest rendering. | ✓ |
| Programmatic drawing | Draw icons programmatically (e.g., draw a small castle shape with lines/shapes). No external assets, but more CPU overhead. | |
| Deferred — circles only for Phase 3 | Start simple: colored circle only. Add icons in Phase 4 or later if needed. | |

**User's choice:** Sprite sheet icons (PNG/SVG)
**Notes:** Visual polish prioritized over development speed. Asset should be tightly packed (e.g., 256x256 px containing 16 unique 16x16 icons) for memory efficiency.

---

## POI Placement Interaction

| Option | Description | Selected |
|--------|-------------|----------|
| Click-to-place + right-click edit + drag-to-move | Click on canvas to place a new POI at that location. Right-click existing POI to edit or delete. Drag to move. Intuitive and direct. | |
| Toolbar button + mode + click | Toolbar button or menu → enables placement mode → click canvas to confirm. Requires mode toggling but explicit. | |
| Right-click context menu | Right-click context menu to add/edit. More traditional menu-driven. | |

**User's choice:** POIs auto-generated during map generation; no manual placement in Phase 3
**Notes:** User clarified that POIs should be generated automatically using world-building logic rather than requiring user placement. This is a significant scope clarification — Phase 3 is about auto-generation + editing, not interactive placement. Manual placement deferred to Phase 4+.

---

## POI Auto-Generation Strategy

| Aspect | Decision |
|--------|----------|
| **Generation Trigger** | During map generation (MapGenerator phase), not separately |
| **POI Types** | Cities (at kingdom capitals + additional habitable biomes), Dungeons/Ruins (at multi-kingdom borders + mountain caves), Landmarks (waterfalls, peaks), Villages/Towns (Poisson-disc scattered in habitable biomes) |
| **Generation Rules** | World-building logic: biome viability for settlements, strategic placement for dungeons/landmarks. NOT random; purposeful placement. |
| **User Control** | Three density sliders: Dungeon Density, Landmark Density, Settlement Density (0.0–1.0 range). Trigger regeneration on change. |

**User's choice:** All suggested types with user sliders for control
**Notes:** User explicitly wanted world-building reasoning (not random placement) and parametrization. Accepted the world-building rules proposed by planner; user adds: consider some types beyond just cities and dungeons; also include settlements/villages and landmarks at terrain features.

---

## POI Editing & Metadata

| Option | Description | Selected |
|--------|-------------|----------|
| Name only | Just name (editable). Type and color determined by auto-generation rules. | |
| Name + Type + custom color | Name + Type (enum: city, dungeon, landmark, etc.) + optional custom color. Type affects icon. User can edit all. | |
| Full metadata: name, type, description, color, icon | Name + Type + Description (rich text) + custom color + custom icon override. Full rich POI record. | ✓ |

**User's choice:** Full metadata: name, type, description, color, icon override
**Notes:** Users want ability to customize POIs beyond auto-generated defaults. Full record supports later features (e.g., adventuring log, quest markers, NPC descriptions).

---

## POI Editing Interface

| Option | Description | Selected |
|--------|-------------|----------|
| Modal dialog | Modal dialog with text fields (name, description), dropdowns (type, icon), color picker. Click POI on canvas to open. | |
| Sidebar list + inline edit | Right sidebar panel lists all POIs. Click in list to edit inline or open editor. Easier for bulk editing. | |
| Sidebar list + modal for details | Sidebar shows list, clicking opens full modal dialog for detailed editing. | ✓ |

**User's choice:** Sidebar list + modal for details
**Notes:** Combines overview (sidebar list) with detailed editing capability (modal). User can quickly browse all POIs and drill into details.

---

## Text Labels vs. POIs

| Option | Description | Selected |
|--------|-------------|----------|
| Independent text labels | Text labels are standalone entities. Separate model (TextLabel), separate placement/editing. Can exist anywhere on map independent of POIs. | |
| Labels always tied to POIs | Labels always attached to POIs. Every POI has an optional name label (shown on hover or always visible). No separate label system. | |
| Hybrid: optional per-POI labels + standalone labels | Labels are optional per POI. Each POI can have label on/off toggle. Plus optional standalone labels for other annotations. | |

**User's choice:** Hybrid approach later; POIs only for Phase 3
**Notes:** User deferred text labels to Phase 4+. Phase 3 implements POI names shown on hover only. Phase 4+ can expand with standalone labels and more flexible label management. This keeps Phase 3 scope tight.

---

## Layer Visibility & Performance

| Option | Description | Selected |
|--------|-------------|----------|
| Always redraw with map | POIs redraw on every map regeneration (when parameters change). Simplest but may cause flicker if POI data changes during redrawn. | |
| Cache POI layer, only update on POI change | POI data cached separately. Only redraw POI layer when POI data changes (add/edit/delete). Biome/kingdom layer unchanged. | |
| Separate overlay canvas for POIs | Render POIs to a separate JavaFX Canvas overlay (StackPane above map canvas). Map and POI updates independent. | ✓ |

**User's choice:** Separate overlay canvas for POIs
**Notes:** Clean separation of concerns. POI layer updates independently of map regeneration. Enables smooth interaction and prevents flicker. Aligns with canvas-based rendering architecture.

---

## Summary of Decisions

### Locked Decisions (Phase 3 Scope)
1. **Data:** POIs stored as standalone list in MapGrid.
2. **Visuals:** Colored circles + sprite icons; names on hover only.
3. **Generation:** Auto-generated using world-building rules (cities in kingdoms, dungeons at borders, landmarks at terrain features, villages in habitable biomes).
4. **Parametrization:** User controls density via three sliders (Dungeon, Landmark, Settlement).
5. **Editing:** Sidebar list shows all POIs; click to open modal editor (name, type, description, color, icon override).
6. **Architecture:** Separate overlay canvas in StackPane for POI rendering.
7. **Placement:** NO manual placement in Phase 3; deferred to Phase 4+.
8. **Labels:** NO text label system in Phase 3; POI names on hover only. Deferred to Phase 4+.

### Deferred to Phase 4+
- Manual POI placement (drag, right-click)
- Standalone text labels or expanded label system
- Advanced features (search, categories, relationships, spatial indexing)

---

## Post-Execution Refinements (2026-04-27)

**Date:** 2026-04-27
**Phase:** 03-interactive-elements-pois-labels
**Testing Results:** 25 unit tests passing; visual testing revealed usability improvements needed

### POI Type Reduction: 10 → 6

| Decision | Rationale | Commits |
|----------|-----------|---------|
| Remove: TAVERN, LANDMARK, TOWER, SHRINE | Testing showed these 4 types added visual clutter without meaningful gameplay benefit. Reduced feature surface to focus on core POI types (CITY, VILLAGE, CASTLE, DUNGEON, CAVE, RUIN) sufficient for first release. | `06d740d` |
| Keep: CITY, VILLAGE, CASTLE, DUNGEON, CAVE, RUIN | These 6 types cover essential world-building roles: settlements (city/village/castle), danger zones (dungeon/cave), and history (ruin). Streamlined set improves map readability. | |

**User Impact:** POI list now shows only meaningful types; icon mapper updated for 6 sprites; tests updated to use valid types.

### Landmark Generation Removal

| Decision | Rationale | Commits |
|----------|-----------|---------|
| Remove: addLandmarks() method, landmarkDensity slider, LANDMARK POI type | Landmark generation (peaks/waterfalls) added clutter without enhancing gameplay. Dungeons and settlements provide sufficient POI variety. Removing landmark slider simplifies UI (3 sliders → 2). | `02933e1`, `06d740d` |
| Keep: Dungeon & Settlement sliders only | Dungeons provide challenge/exploration POIs; settlements provide civilization/trading POIs; both are strategically important. Landmark removal reduces cognitive load. | |

**User Impact:** Cleaner POI layer on map; simpler UI (fewer sliders); reduced map clutter.

### Dungeon Density Multiplier Reduction: 0.015 → 0.0005 (20x)

| Iteration | Multiplier | Result | Status |
|-----------|------------|--------|--------|
| Initial | 0.015 | ~40+ dungeons on 128×128 map; excessive clutter | ❌ Rejected (visual test) |
| Intermediate | 0.003 | ~12–15 dungeons; still too many | ⚠️ Better but inadequate |
| Final | 0.0005 | ~5–10 dungeons; sparse, meaningful placement | ✅ Approved |

**Rationale:** At default slider value (0.5), result should be sparse, accessory to map. Excessive dungeons broke immersion; 20x reduction achieves appropriate balance.

**Commits:** `9d92a2a` (6x reduction), `e866367` (refined to 20x)

**Dungeon Generation Constraint Relaxation:**
- Border requirement: 3+ adjacent kingdoms → 2+ adjacent kingdoms (more realistic dungeon placement)
- Elevation threshold: 0.5 → 0.4 (allows more cave/mountain locations for dungeons)

**Result:** Dungeons now generate reliably even at lower density settings; placement more distributed across map.

### Settlement Density Multiplier Reduction: 0.0015 → 0.00005 (30x)

| Iteration | Multiplier | Result | Status |
|-----------|------------|--------|--------|
| Initial | 0.0015 | ~40+ settlements on 128×128 map; excessive | ❌ Rejected (visual test) |
| Intermediate | 0.0003 | ~10–15 settlements; still crowded | ⚠️ Better but inadequate |
| Final | 0.00005 | ~2–5 settlements; sparse, distinctive | ✅ Approved |

**Rationale:** Excessive settlements dominated POI layer. 30x reduction achieves sparse placement that highlights each settlement as meaningful landmark.

**Commits:** `9d92a2a` (6x reduction), `e866367` (refined to 30x)

### Settlement Distribution Algorithm: Poisson-Disc → Grid-Based Quadrant Seeding

| Approach | Description | Problem | Status |
|----------|-------------|---------|--------|
| Poisson-Disc Sampling | Random multi-seed approach with minimum distance constraint. Simple greedy algorithm. | **Settlement Clustering:** All settlements gravitated toward map center instead of distributing evenly; visual testing showed dense clusters in center, empty periphery. | ❌ Rejected |
| Grid-Based Quadrant Seeding | Divide map into grid quadrants; place 1 settlement per quadrant deterministically. Uniform coverage guaranteed. | None identified. Uniform distribution across map; no center bias. | ✅ Approved |

**Rationale:** Poisson-disc approach had inherent bias toward center due to random seeding behavior. Grid-based quadrant approach ensures uniform distribution without clustering.

**Commits:** `5302f2c` (implemented grid-based seeding)

**Result:** Settlements now distributed uniformly across all map regions; each quadrant has roughly equal settlement density; improved map coherence.

### Zero-Density Bug Fix

| Issue | Symptom | Fix | Commits |
|-------|---------|-----|---------|
| Forced minimum count | Setting density slider to 0.0 still generated 1 POI | Changed Math.max(1, count) → Math.max(0, count) | `5302f2c` |
| User expectation | Users expected 0.0 slider to disable generation completely | Now: 0.0 → 0 POIs, 0.5 → ~5–10 POIs, 1.0 → max POIs | ✅ Verified |

**Result:** Density sliders now truly span full range from disabled (0.0) to maximum (1.0).

### UI/UX Refinements

| Change | Rationale | Commits |
|--------|-----------|---------|
| POI list dark background (#1e1e1e) + light text (#e0e0e0) | Original white text on white background was unreadable | `e866367` |
| Tab header styling (dark bg, white text) | Tab headers not visible; needed dark background for contrast | `e866367` |
| Removed landmark density slider | Simplifies UI; only 2 sliders needed after landmark removal | `02933e1` |

**Result:** POI list and tab headers now readable; UI simplified.

### Additional Refinements (Post-Documentation)

| Change | Details | Commits |
|--------|---------|---------|
| Dungeon distribution | Replaced random placement with quadrant seeding (sqrt(targetCount*2) grid) | `0ee6790` |
| Dungeon placement rules | Relaxed: kingdom borders 2+→1+, elevation 0.4→0.35, added biome checks | `0ee6790` |
| Settlement algorithm | Fixed remaining center-clustering by proper quadrant iteration | `10a17a7` |

**Result:** All POI types (settlements, dungeons) now uniformly distributed; no center clustering.

### Summary of Refinements

**Goal:** Reduce visual clutter, improve map readability, simplify UI, enable sparse POI distribution.

**Achievements:**
- POI type set reduced from 10 → 6 core types
- Landmarks completely removed (generation + slider + type)
- Dungeon density reduced 20x for sparse placement
- Settlement density reduced 30x for sparse placement
- Settlement distribution changed to grid-based quadrant seeding (uniform coverage)
- Zero-density bug fixed (0.0 now truly disables generation)
- UI styling improved (readable text, visible tabs)

**Outcome:** Phase 3 now delivers sparse, readable POI layer that enhances map without overwhelming it. All 25 tests passing; visual testing approved.
