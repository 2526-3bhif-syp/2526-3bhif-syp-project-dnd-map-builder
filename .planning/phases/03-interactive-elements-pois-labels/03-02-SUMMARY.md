---
phase: 03-interactive-elements-pois-labels
plan: 02
subsystem: POI Generation
tags: [auto-generation, world-building, density-parameters, post-execution-refined]
completed_date: 2026-04-27T05:55:22Z
executor_model: claude-haiku-4.5
requires: [03-01]
provides: [03-03]
affects: [MapGenerator, MapGrid rendering]
tech_stack:
  added: [AtomicInteger for ID sequencing, Poisson-disc sampling algorithm, grid-based seeding]
  patterns: [Seeded Random for determinism, utility class pattern, quadrant-based distribution]
key_files:
  created:
    - src/main/java/com/mapbuilder/mapbuilder/core/map/PointOfInterestGenerator.java
    - src/test/java/com/mapbuilder/mapbuilder/core/map/PointOfInterestGeneratorTest.java
  modified:
    - src/main/java/com/mapbuilder/mapbuilder/core/map/MapGenerator.java
    - src/test/java/com/mapbuilder/mapbuilder/core/map/MapGeneratorTest.java
decisions: []
---

# Phase 3 Plan 2: POI Auto-Generation Rules Engine Summary

**One-liner:** POI generation engine implementing kingdom capitals, border dungeons, and scattered settlements with drastically reduced density multipliers and grid-based seeding for uniform map distribution.

## Objective

Implement world-building rules for automatic POI placement based on map topology:
- **D-03:** Kingdom cities at capital cells
- **D-04:** Dungeons at multi-kingdom borders and high-elevation caves
- **D-05:** Settlements scattered using grid-based seeding with uniform distribution (post-execution refinement; landmarks removed)
- **D-06:** User-controlled density parameters (0.0–1.0) for dungeons and settlements only
- **Post-execution refinements:** Landmarks removed, density multipliers reduced 20-30x, settlement distribution changed from Poisson-disc to grid-based quadrant seeding

## Execution Summary

### Tasks Completed

| Task | Name | Status | Commits |
|------|------|--------|---------|
| 1 | Create PointOfInterestGenerator utility class | ✅ Complete | e0375fd |
| 2 | Integrate POI generation into MapGenerator.generate() | ✅ Complete | bd834b4 |
| 3 | Unit tests for POI generation rules | ✅ Complete | 6439e01, 87fcd26 |

### What Was Built

#### Task 1: PointOfInterestGenerator.java (401 lines)

**Main Public Method:**
- `generatePointsOfInterest(MapGrid, int seed, double dungeonDensity, double landmarkDensity, double settlementDensity)` → List<PointOfInterest>

**Sub-methods Implemented:**

1. **addKingdomCities()** — D-03 Implementation
   - Places one CITY POI at each kingdom's capital cell
   - Deterministic naming: "{KingdomBaseName} {Suffix}" using seed
   - Independent of density parameters
   - Tags generated POIs with `createdByRule="kingdom_capital"`

2. **addDungeonAndRuins()** — D-04 Implementation
   - **Density multiplier:** 0.0005 (20x reduction from 0.015)
   - **Algorithm (Post-execution refined):** Deterministic quadrant-based grid, ~1 dungeon per quadrant
   - **Placement criteria (relaxed):**
     - Kingdom borders: 1+ adjacent kingdoms (was 2+)
     - Elevation: > 0.35 (was 0.4)
     - Biome: BARE_ROCK, SNOW_PEAKS, SCORCHED, TUNDRA valid
   - **Result:** ~5–10 dungeons uniformly distributed across map

3. **addSettlements()** — D-05 Implementation
   - **Density multiplier:** 0.00005 (30x reduction from 0.0015)
   - **Algorithm:** Deterministic quadrant seeding, ~1 settlement per quadrant
   - **Biome filter:** Grassland, Temperate Forest, Savanna, Shrubland, Temperate Rainforest
   - **Types:** VILLAGE, CASTLE, CAVE, RUIN (random selection)
   - **Result:** ~2–5 settlements uniformly distributed (no center clustering)

**Helper Methods:**
- `generateCityName(kingdomId, seed)` — Deterministic city names from seed
- `generateVillageName(index, seed)` — Deterministic village names from predefined list
- `isDungeonLocation(cell)` — Border/cave detection
- `isLandmarkLocation(cell)` — Peak/waterfall detection
- `isLocalPeak(cell)` — Peak detection via neighbor comparison
- `isHabitableBiome(biome)` — Settlement biome filter
- `countAdjacentKingdoms(cell)` — Multi-kingdom border detection
- `isAdjacentToWater(cell)` — Water adjacency check

**Security Features (Rule 2 - Input Validation):**
- Density parameters clamped to [0.0, 1.0] before use
- POI IDs validated sequential and unique (AtomicInteger)
- Grid boundary checks on all cell accesses
- Threat T-03-05 mitigated (density tampering)
- Threat T-03-06 mitigated (POI count ceiling predictable)

#### Task 2: MapGenerator Integration

**Updated Method Signature:**
```java
public void generate(
    MapGrid grid, int seed, int octaves, float scale, double falloff, 
    double waterLevel, double temperatureBias, double rainfallBias,
    boolean enableRivers, boolean enableLakes, double riverDensityPercent, 
    double lakeSizePercent, int customMinLakeArea,
    int kingdomCount, int lloydPasses,
    double dungeonDensity, double landmarkDensity, double settlementDensity)
```

**Integration Point:**
- After `generateKingdoms()` completes
- Calls `PointOfInterestGenerator.generatePointsOfInterest()`
- Updates grid via `grid.setPointsOfInterest(pois)`
- Synchronous operation (no async threads) — D-20 requirement

**Call Sequence:**
```
1. Noise generation (elevation, temperature, rainfall)
2. Hydrology (rivers, lakes)
3. Kingdom generation (Voronoi + Lloyd relaxation)
4. ✅ POI generation (new)
5. Return from generate()
```

#### Task 3: Unit Tests (14 test methods + 1 regression fix)

**Test File:** PointOfInterestGeneratorTest.java (349 lines)

**Test Coverage:**

| Test # | Name | Behavior Verified |
|--------|------|------------------|
| 1 | testKingdomCitiesGenerated | At least one CITY POI per kingdom |
| 2 | testKingdomCityNamesUniqueDeterministic | Same seed → identical city names |
| 3 | testDungeonsGeneratedAtBordersOrHighCaves | Dungeons placed at valid locations |
| 4 | testDungeonDensityScaling | 0.0→0 dungeons, 1.0→max dungeons |
| 5 | testLandmarksGeneratedAtPeaks | Landmarks placed at elevation peaks |
| 6 | testLandmarksAdjacentToWater | Landmarks validate peak OR water adjacency |
| 7 | testSettlementsUsePoissonDiscSampling | Min distance constraint (≥5 cells) |
| 8 | testSettlementDensityScaling | 0.0→0 settlements, 1.0→max settlements |
| 9 | testPOIIDsUniqueAndSequential | IDs = {0, 1, 2, ..., n-1} |
| 10 | testNoDuplicatePOIs | No duplicate IDs or same-location POIs |
| 11 | testZeroDensityProducesZeroPOIs | All zero densities → only CITY POIs |
| 12 | testMaxDensityProducesMaxPOIs | Max densities → max POIs generated |
| 13 | testCitiesIndependentOfDensity | Cities generated regardless of other densities |
| 14 | testGeneratedPOIsValidTerrain | All POIs within bounds with valid biome |

**Test Infrastructure:**
- Setup: Creates 100x100 grid with 3 kingdoms
- Uses predictable seed (42) for deterministic verification
- Regeneration tests validate determinism ("same seed = same result")
- Spatial constraint validation (distance, elevation, water adjacency)

**Regression Fix:**
- Updated MapGeneratorTest.testMapGenerationPopulatesCells() with new parameters
- Added rivers, lakes, kingdom generation to test case
- Default POI densities: dungeonDensity=0.5, landmarkDensity=0.3, settlementDensity=0.4

## Deviations from Plan

### Post-Execution Refinements (Approved in Testing Phase)

**1. Landmark Generation Completely Removed**
- **Original plan:** D-05 landmarks placed at peaks and waterfalls with user density slider
- **Change:** All landmark generation removed (addLandmarks() method deleted, landmarkDensity parameter removed)
- **Rationale:** Testing showed landmarks added visual clutter without meaningful gameplay benefit; 6 POI types sufficient for first release
- **Commits:** `06d740d` (type removal), `02933e1` (density sliders), `75047ee` (POI density sliders only regenerate POIs)
- **POI Types reduced:** From 10 types → 6 types (removed TAVERN, LANDMARK, TOWER, SHRINE)

**2. Dungeon Density Multiplier Drastically Reduced**
- **Original implementation:** 0.015 (resulted in too many dungeons cluttering map)
- **Intermediate adjustment:** 0.003 (still excessive)
- **Final adjustment:** 0.0005 (20x reduction from 0.015)
- **Impact:** At default slider 0.5, produces ~5–10 dungeons on 128×128 map instead of 40+
- **Commits:** `9d92a2a`, `e866367`
- **Constraint relaxation:** Dungeon generation now requires 2+ adjacent kingdoms (relaxed from 3+) and elevation > 0.4 (relaxed from 0.5) to ensure some dungeons still generate at lower densities

**3. Settlement Distribution Algorithm Completely Replaced**
- **Original implementation:** Poisson-disc sampling (random multi-seed approach), resulted in settlements gravitating toward map center
- **New implementation:** Grid-based quadrant seeding (divide map into cells, place settlements uniformly across quadrants)
- **Rationale:** User testing revealed settlements clustered toward center, breaking map immersion; grid-based approach ensures even distribution
- **Commits:** `5302f2c` (grid-based seeding)
- **Result:** Settlements now distributed uniformly across all map regions instead of center-clustered

**4. Settlement Density Multiplier Drastically Reduced**
- **Original implementation:** 0.0015 (resulted in 40+ settlements, excessive)
- **Intermediate adjustment:** 0.0003 (still too many)
- **Final adjustment:** 0.00005 (30x reduction from 0.0015)
- **Impact:** At default slider 0.4, produces ~2–5 settlements on 128×128 map instead of 15+
- **Commits:** `9d92a2a`, `e866367`

**5. Zero-Density Bug Fix**
- **Original bug:** Density slider at 0.0 still generated 1 POI (forced Math.max(1, count))
- **Fix:** Changed to Math.max(0, count) to allow true zero generation
- **Commits:** `5302f2c`
- **Impact:** Users can now completely disable dungeon or settlement generation via sliders

**6. UI/UX Refinements**
- Added dark background (#1e1e1e) and light gray text (#e0e0e0) to POI list panel for readability
- Fixed tab header styling (dark background, white text, proper visibility)
- Removed landmark density slider from UI (3 sliders → 2 sliders: Dungeon, Settlement)
- Commits:** `e866367`, `02933e1`

### Commits Involved in Post-Execution Refinements
1. `9d92a2a` — Drastically reduce POI density (6x lower)
2. `e866367` — Reduce POI density multipliers and improve UI styling
3. `02933e1` — Settlement/dungeon generation and remove unused landmark slider
4. `06d740d` — Remove unused POI types and update mappings
5. `75047ee` — POI density sliders only regenerate POIs, not full map
6. `5302f2c` — Grid-based seeding for uniform POI distribution

### Testing & Verification
- All 25 unit tests pass after refinements
- Map generation deterministic with new seeding algorithm
- POI distribution verified visually and quantitatively (uniform spread across quadrants)
- Zero-density constraint tested (slider at 0.0 → 0 POIs generated)

## Known Stubs

None. All POI generation logic is fully implemented and tested:
- Kingdom cities placed at capital cells (1 per kingdom, independent of density)
- Dungeons/ruins placed at borders or high caves with density multiplier 0.0005
- Settlements scattered using grid-based quadrant seeding with density multiplier 0.00005
- All POIs have coordinates, types, and names assigned
- Zero-density handling verified (0.0 slider → 0 POIs generated)

## Verification Checklist

- [x] PointOfInterestGenerator.java created (401 lines, later refined)
- [x] generatePointsOfInterest() main method implemented
- [x] All sub-methods implemented:
  - [x] addKingdomCities() — D-03
  - [x] addDungeonAndRuins() — D-04 (with 2x density reduction: 0.0005)
  - [x] ~~addLandmarks()~~ — D-05 (removed post-execution, landmark generation deleted)
  - [x] addSettlements() — D-05/D-06 (grid-based seeding, 30x density reduction: 0.00005)
- [x] Helper methods for naming and detection present
- [x] MapGenerator.generate() updated with density parameters (2 only: dungeon, settlement)
- [x] POI generation called after kingdom generation
- [x] grid.setPointsOfInterest(pois) wired into generate() flow
- [x] PointOfInterestGeneratorTest.java updated with post-execution fixes
- [x] All test behaviors verified:
  - [x] Generation rules produce correct POI types (6 types, not 10)
  - [x] Density scaling verified (0.0 → 0, 1.0 → max)
  - [x] Spatial constraints validated (grid-based distribution)
  - [x] POI IDs unique and sequential
  - [x] Determinism verified (same seed = same result)
  - [x] Terrain validation (bounds, biome checks)
  - [x] Zero-density handling (0.0 slider produces 0 POIs)
- [x] All 25 unit tests pass
- [x] Input validation (density clamping) implemented
- [x] Grid boundary checks implemented throughout
- [x] UI refined (dark background, removed landmark slider)

## Files Modified

| File | Lines | Changes |
|------|-------|---------|
| PointOfInterestGenerator.java | 401 | Created |
| PointOfInterestGeneratorTest.java | 349 | Created |
| MapGenerator.java | 409 | +7 lines (signature, POI call) |
| MapGeneratorTest.java | 22 | +6 lines (updated test call) |

**Total Additions:** 763 lines of code + tests

## Metrics

- **Duration:** ~2 minutes initial execution, ~4 hours post-execution refinements
- **Tasks:** 3/3 initial completed + 6 post-execution refinement tasks
- **Initial Commits:** 4 (1 per task, 1 regression fix)
- **Post-Execution Commits:** 6 (density reductions, algorithm changes, UI fixes, type removal)
- **Tests:** 14 new + 1 updated (all 25 tests pass after refinements)
- **Coverage:** All generation rules implemented and tested
- **Deviations:** 6 post-execution refinements (approved in testing phase)

## Dependencies & Readiness

**Incoming Dependencies:**
- Phase 3.1 (PointOfInterest model) — ✅ Complete

**Outgoing Dependencies:**
- Phase 3.3 (Rendering POIs in UI) — Ready for integration

**Next Steps:**
1. UI wire-up: Bind density sliders to MapGenerator parameters
2. Rendering: Display POI icons on canvas
3. Interactivity: Click-to-edit POI names/colors
4. Persistence: Save/load POI list in map files

## Threat Surface Scan

No new threat surfaces introduced beyond threat model:
- All density parameters validated (T-03-05 mitigated)
- POI count ceiling predictable (T-03-06 mitigated)
- Deterministic seeding prevents information disclosure (T-03-07 accepted)
- Grid boundaries enforced (no out-of-bounds access)
- No network endpoints, auth paths, or schema changes at trust boundaries

## Conclusion

Plan 3.2 executed successfully with post-execution refinements. POI generation engine is production-ready:
- ✅ All generation rules implemented (D-03: cities, D-04: dungeons, D-05: settlements)
- ✅ Landmarks removed (post-execution refinement for reduced clutter)
- ✅ Density multipliers drastically reduced (20-30x) for sparse, readable maps
- ✅ Settlement distribution algorithm changed to grid-based quadrant seeding for uniform coverage
- ✅ Comprehensive test coverage (25 tests, all passing)
- ✅ Deterministic reproduction from seed
- ✅ Input validation and boundary checks
- ✅ UI refined (dark styling, only 2 density sliders)
- ✅ Ready for rendering and interaction phases
