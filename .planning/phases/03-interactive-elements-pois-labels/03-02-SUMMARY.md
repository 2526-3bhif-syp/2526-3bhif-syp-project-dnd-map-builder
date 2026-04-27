---
phase: 03-interactive-elements-pois-labels
plan: 02
subsystem: POI Generation
tags: [auto-generation, world-building, density-parameters]
completed_date: 2026-04-27T05:55:22Z
executor_model: claude-haiku-4.5
requires: [03-01]
provides: [03-03]
affects: [MapGenerator, MapGrid rendering]
tech_stack:
  added: [AtomicInteger for ID sequencing, Poisson-disc sampling algorithm]
  patterns: [Seeded Random for determinism, utility class pattern]
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

**One-liner:** POI generation engine implementing kingdom capitals, border dungeons, terrain landmarks, and scattered settlements with user-controlled density parameters.

## Objective

Implement world-building rules for automatic POI placement based on map topology:
- **D-03:** Kingdom cities at capital cells
- **D-04:** Dungeons at multi-kingdom borders and high-elevation caves
- **D-05:** Landmarks at waterfalls and mountain peaks
- **D-06:** Settlements scattered using Poisson-disc sampling
- **D-07:** User-controlled density parameters (0.0–1.0) for each POI type

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
   - Count: `(gridArea * dungeonDensity / 100.0) * 0.15`
   - Placement criteria:
     - Multi-kingdom borders (3+ adjacent kingdoms), OR
     - High caves/mountains (elevation > 0.5, unowned territory)
   - Uses seeded Random for deterministic selection
   - Alternates DUNGEON ↔ RUIN type randomly

3. **addLandmarks()** — D-05 Implementation
   - Count: `(gridArea * landmarkDensity / 100.0) * 0.1`
   - Placement criteria:
     - High-elevation peaks (local maxima), OR
     - Waterfalls (elevation > 0.6 adjacent to water)
   - Deterministic naming: "Peak of {KingdomID}" or fallback location-based
   - Validates adjacency to rivers/lakes/coast

4. **addSettlements()** — D-06 Implementation
   - Count: `(gridArea * settlementDensity / 100.0) * 0.25`
   - Uses Poisson-disc sampling with minimum distance = 5 cells
   - Filter: Only habitable biomes (Grassland, Temperate Forest, Savanna, Shrubland, Temperate Rainforest)
   - Creates VILLAGE POIs with deterministic fantasy names
   - Tags with `createdByRule="settlement_scattered"`

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

### Auto-fixed Issues

**1. [Rule 2 - Missing Input Validation] Added density parameter clamping**
- **Found during:** Task 1 implementation (threat T-03-05)
- **Issue:** Density parameters could exceed [0.0, 1.0] range, causing incorrect POI counts or negative values
- **Fix:** Added clamp logic at start of generatePointsOfInterest():
  ```java
  dungeonDensity = Math.max(0.0, Math.min(1.0, dungeonDensity));
  landmarkDensity = Math.max(0.0, Math.min(1.0, landmarkDensity));
  settlementDensity = Math.max(0.0, Math.min(1.0, settlementDensity));
  ```
- **Files modified:** PointOfInterestGenerator.java (lines 57-59)
- **Commits:** e0375fd

**2. [Rule 2 - Missing Boundary Validation] Added grid bounds checking**
- **Found during:** Task 1 implementation
- **Issue:** Poisson-disc sampling could reference cells outside grid bounds
- **Fix:** Added null checks on MapGrid.getCell() calls throughout:
  - isDungeonLocation() validates cell != null
  - isLandmarkLocation() validates cell != null
  - isAdjacentToWater() validates neighbor != null
  - Settlement cell iteration validates grid boundaries
- **Files modified:** PointOfInterestGenerator.java (multiple methods)
- **Commits:** e0375fd

**3. [Rule 3 - Compilation Requirement] Updated MapGeneratorTest**
- **Found during:** Task 2 (method signature changed)
- **Issue:** Existing test called generate() with old signature (fewer parameters)
- **Fix:** Updated test to pass all required parameters:
  - Added rivers (true), lakes (true)
  - Added kingdom count (2), lloyd passes (1)
  - Added density parameters (0.5, 0.3, 0.4)
- **Files modified:** MapGeneratorTest.java (line 13-18)
- **Commits:** 87fcd26

## Known Stubs

None. All POI generation logic is fully wired:
- Dungeons/ruins count determined by grid area and density
- Landmarks placed using peak/waterfall detection
- Settlements use Poisson-disc algorithm with valid cell checks
- All POIs have coordinates, types, and names assigned
- No placeholder text or empty data structures

## Verification Checklist

- [x] PointOfInterestGenerator.java created (401 lines)
- [x] generatePointsOfInterest() main method implemented
- [x] All 4 sub-methods implemented:
  - [x] addKingdomCities() — D-03
  - [x] addDungeonAndRuins() — D-04
  - [x] addLandmarks() — D-05
  - [x] addSettlements() — D-06
- [x] Helper methods for naming and detection present
- [x] MapGenerator.generate() updated with 3 new density parameters
- [x] POI generation called after kingdom generation
- [x] grid.setPointsOfInterest(pois) wired into generate() flow
- [x] PointOfInterestGeneratorTest.java created with 14 test methods
- [x] All test behaviors verified:
  - [x] Generation rules produce correct POI types
  - [x] Density scaling (0.0 → 0, 1.0 → max)
  - [x] Spatial constraints validated
  - [x] POI IDs unique and sequential
  - [x] Determinism verified (same seed = same result)
  - [x] Terrain validation (bounds, biome checks)
- [x] MapGeneratorTest updated for backward compatibility
- [x] Input validation (density clamping) implemented
- [x] Grid boundary checks implemented throughout

## Files Modified

| File | Lines | Changes |
|------|-------|---------|
| PointOfInterestGenerator.java | 401 | Created |
| PointOfInterestGeneratorTest.java | 349 | Created |
| MapGenerator.java | 409 | +7 lines (signature, POI call) |
| MapGeneratorTest.java | 22 | +6 lines (updated test call) |

**Total Additions:** 763 lines of code + tests

## Metrics

- **Duration:** ~2 minutes execution time
- **Tasks:** 3/3 completed
- **Commits:** 4 (1 per task, 1 regression fix)
- **Tests:** 14 new + 1 updated
- **Coverage:** All D-03 to D-07 requirements implemented
- **Deviations:** 3 (all Rule 1/2 auto-fixes)

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

Plan 3.2 executed successfully. POI generation engine is production-ready:
- ✅ All generation rules (D-03 to D-06) implemented
- ✅ Density parameter integration complete
- ✅ Comprehensive test coverage (14 test methods)
- ✅ Deterministic reproduction from seed
- ✅ Input validation and boundary checks
- ✅ Ready for UI integration in Phase 3.3
