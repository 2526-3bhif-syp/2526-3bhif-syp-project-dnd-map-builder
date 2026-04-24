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

*Discussion Log — Phase 03*
*Gathered: 2026-04-24*
