# Phase 02: Core Map Generation - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-06
**Phase:** 02-core-map-generation
**Areas discussed:** UI Layout, River Controls, Lake Controls, River/Lake Dependency

---

## UI Layout

| Option | Description | Selected |
|--------|-------------|----------|
| Collapsible section | Keeps the sidebar clean as the number of settings grows | |
| Flat list of sliders | Simple and keeps everything visible at once | |
| Separate tab | A separate tab panel entirely for water features | ✓ |

**User's choice:** as a separate tab in the existing tab system

## Regeneration

| Option | Description | Selected |
|--------|-------------|----------|
| Auto-generate on change | Maintains real-time feedback with current debounce | ✓ |
| Manual Apply button | Saves performance if users want to tweak multiple settings before rendering | |

**User's choice:** Auto-generate on change

## Enable/Disable

| Option | Description | Selected |
|--------|-------------|----------|
| Master toggles | Provides an explicit way to turn off features without losing slider values | ✓ |
| Just slider to 0 | Simpler UI, but setting it to 0 loses the previous 'good' value | |

**User's choice:** Master toggles

---

## River Controls

| Option | Description | Selected |
|--------|-------------|----------|
| River Density / Count | Control how many rivers generate | ✓ |
| Min Length Ratio | Control the minimum length before a river becomes a river | |
| Carving Depth | Control how aggressively a river cuts through valleys | |

**User's choice:** River Density / Count

## Density Range

| Option | Description | Selected |
|--------|-------------|----------|
| 0% to 200% slider | Easy to understand, 50% is half, 200% is double the default | ✓ |
| Low/Med/High dropdown | Fewer options, less overwhelming | |
| Raw integer slider | Direct control over the exact number of generation attempts | |

**User's choice:** 0% to 200% slider

## Zero Density

| Option | Description | Selected |
|--------|-------------|----------|
| No rivers at 0% | Provides complete control to turn them off | ✓ |
| Keep 1-2 major rivers | Always guarantees a minimal hydrological system | |

**User's choice:** No rivers at 0%

---

## Lake Controls

| Option | Description | Selected |
|--------|-------------|----------|
| Lake Size Multiplier | A slider for how large the lakes are allowed to grow | ✓ |
| Minimum Lake Area | Slider for the minimum number of cells a lake must occupy | ✓ |
| Just toggle (Auto-scale) | No extra sliders, just use the master toggle and let them scale automatically | |

**User's choice:** Lake Size Multiplier, Minimum Lake Area

## Lake Size Scale

| Option | Description | Selected |
|--------|-------------|----------|
| 0% to 200% slider | Consistent with river density, easy to understand | ✓ |
| Raw numerical slider | Directly adjusting the base area radius mathematically | |

**User's choice:** 0% to 200% slider

---

## Dependencies (River/Lake Dependency)

| Option | Description | Selected |
|--------|-------------|----------|
| Independent lakes | Lakes form at local minimums even if no river leads to them | ✓ |
| Tied to rivers | Lakes only form where rivers terminate or pool | |

**User's choice:** Independent lakes

---

**Date:** 2026-04-21
**Phase:** 02-core-map-generation
**Areas discussed:** Seed Placement, Distance Metric, Visual Representation, UI / Configuration, Relaxation Passes, Sea Claims

---

## Seed Placement

| Option | Description | Selected |
|--------|-------------|----------|
| Poisson Disc | Uniformly spaced, prevents clumping, easily restricted from oceans | ✓ |
| Biome-weighted Random | Higher spawn chance in fertile biomes (plains, forests), 0 in ocean | |
| Pure Random | Simple random selection, might clump | |

**User's choice:** Poisson Disc (Recommended)

---

## Distance Metric

| Option | Description | Selected |
|--------|-------------|----------|
| Terrain Cost | Mountains/oceans cost more to cross, borders naturally follow ridges/coasts | ✓ |
| Euclidean Distance | Standard Voronoi, ignores terrain entirely | |
| Manhattan / Chebyshev | Similar to Euclidean but faster to compute, though slightly blocky | |

**User's choice:** Terrain Cost (Recommended)

---

## Visuals

| Option | Description | Selected |
|--------|-------------|----------|
| Solid / Dashed Outline | Draws a distinct colored line only on the edge pixels between different kingdoms | |
| Tinted Overlay | The entire kingdom area is slightly tinted with a distinct color | |
| Outline + Tint | Combines both the outline and the tint | |

**User's choice:** "add toggles to the UI to toggle on/off both outlines and tints" (Custom)

---

## UI

| Option | Description | Selected |
|--------|-------------|----------|
| Slider (Auto-update) | A slider for kingdom count that updates in real-time like other parameters | ✓ |
| Separate Regen Button | A separate button to regenerate only kingdoms without changing terrain | |
| Main Map Generation | Only configurable before generating the entire map | |

**User's choice:** Slider (Auto-update) (Recommended)

---

## Relaxation Passes

| Option | Description | Selected |
|--------|-------------|----------|
| 1-3 Passes | More passes make borders more uniform and natural, but take longer to compute | |
| 0 Passes (Base Voronoi)| Fastest generation, borders might be slightly irregular | |
| 5+ Passes | Perfectly uniform cells, but very slow with terrain costs | |

**User's choice:** "add a slider to the UI to configure lloyd passes (slider from 0 to 5)" (Custom)

---

## Sea Claims

| Option | Description | Selected |
|--------|-------------|----------|
| Stop at Coastline | Borders stop exactly at the coast, oceans are unowned | ✓ |
| Claim Shallow Water | Kingdoms claim nearby shallow water, deep ocean unowned | |
| Claim Entire Ocean | Kingdoms extend into the ocean infinitely until they meet another | |

**User's choice:** Stop at Coastline (Recommended)

---

## the agent's Discretion

None — all areas discussed.

## Deferred Ideas

None — discussion stayed within phase scope.