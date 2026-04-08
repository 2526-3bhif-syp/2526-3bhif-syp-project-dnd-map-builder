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

## the agent's Discretion

None — all areas discussed.

## Deferred Ideas

None — discussion stayed within phase scope.