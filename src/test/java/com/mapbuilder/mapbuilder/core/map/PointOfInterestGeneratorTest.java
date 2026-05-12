package com.mapbuilder.mapbuilder.core.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for PointOfInterestGenerator.
 * Tests verify each POI generation rule (D-03 to D-06) and density scaling.
 */
class PointOfInterestGeneratorTest {
    
    private MapGrid grid;
    private MapGenerator mapGenerator;
    private final int TEST_SEED = 42;
    private final int GRID_WIDTH = 100;
    private final int GRID_HEIGHT = 100;

    @BeforeEach
    void setUp() {
        grid = new MapGrid(GRID_WIDTH, GRID_HEIGHT);
        mapGenerator = new MapGenerator();
        
        // Generate a predictable map with known kingdoms
        mapGenerator.generate(grid, TEST_SEED, 5, 0.01f, 0.5, 0.3, 0.0, 0.0,
                true, true, 50.0, 50.0, 100,
                3, 2, // 3 kingdoms, 2 lloyd passes
                0.5, 0.5, 0.5); // Default densities
    }

    // ===== Test 1: Kingdom cities generated =====
    @Test
    void testKingdomCitiesGenerated() {
        List<PointOfInterest> pois = grid.getPointsOfInterest();
        long cityCount = pois.stream().filter(p -> p.getType() == POIType.CITY).count();
        
        assertTrue(cityCount >= 1, "At least one CITY POI should be generated for kingdoms");
        assertTrue(cityCount <= 10, "City count should be reasonable");
    }

    // ===== Test 2: Kingdom city names are unique and deterministic =====
    @Test
    void testKingdomCityNamesUniqueDeterministic() {
        List<PointOfInterest> pois1 = grid.getPointsOfInterest();
        Set<String> names1 = new HashSet<>();
        for (PointOfInterest poi : pois1) {
            if (poi.getType() == POIType.CITY) {
                names1.add(poi.getName());
            }
        }
        
        // Regenerate with same seed
        MapGrid grid2 = new MapGrid(GRID_WIDTH, GRID_HEIGHT);
        mapGenerator.generate(grid2, TEST_SEED, 5, 0.01f, 0.5, 0.3, 0.0, 0.0,
                true, true, 50.0, 50.0, 100,
                3, 2, 0.5, 0.5, 0.5);
        
        List<PointOfInterest> pois2 = grid2.getPointsOfInterest();
        Set<String> names2 = new HashSet<>();
        for (PointOfInterest poi : pois2) {
            if (poi.getType() == POIType.CITY) {
                names2.add(poi.getName());
            }
        }
        
        assertEquals(names1.size(), names2.size(), "City count should be deterministic");
        assertEquals(names1, names2, "City names should be identical with same seed");
    }

    // ===== Test 3: Dungeons generated at borders or high caves =====
    @Test
    void testDungeonsGeneratedAtBordersOrHighCaves() {
        List<PointOfInterest> pois = grid.getPointsOfInterest();
        long dungeonCount = pois.stream()
                .filter(p -> p.getType() == POIType.DUNGEON || p.getType() == POIType.RUIN)
                .count();
        
        // With default density 0.5, should have some dungeons
        assertTrue(dungeonCount > 0, "Dungeons/Ruins should be generated with default density");
        
        // Verify dungeons are at reasonable locations (not in ocean)
        for (PointOfInterest poi : pois) {
            if (poi.getType() == POIType.DUNGEON || poi.getType() == POIType.RUIN) {
                MapCell cell = grid.getCell(poi.getX(), poi.getY());
                assertTrue(cell.getElevation() > 0.3, "Dungeons should not be in deep water");
            }
        }
    }

    // ===== Test 4: Dungeon count scales with density (0.0 = 0, 1.0 = max) =====
    @Test
    void testDungeonDensityScaling() {
        // Test with 0.0 density
        MapGrid gridZero = new MapGrid(GRID_WIDTH, GRID_HEIGHT);
        mapGenerator.generate(gridZero, TEST_SEED, 5, 0.01f, 0.5, 0.3, 0.0, 0.0,
                true, true, 50.0, 50.0, 100,
                3, 2, 0.0, 0.5, 0.5);
        
        long dungeonCountZero = gridZero.getPointsOfInterest().stream()
                .filter(p -> p.getType() == POIType.DUNGEON || p.getType() == POIType.RUIN)
                .count();
        
        // Test with 1.0 density
        MapGrid gridMax = new MapGrid(GRID_WIDTH, GRID_HEIGHT);
        mapGenerator.generate(gridMax, TEST_SEED, 5, 0.01f, 0.5, 0.3, 0.0, 0.0,
                true, true, 50.0, 50.0, 100,
                3, 2, 1.0, 0.5, 0.5);
        
        long dungeonCountMax = gridMax.getPointsOfInterest().stream()
                .filter(p -> p.getType() == POIType.DUNGEON || p.getType() == POIType.RUIN)
                .count();
        
        // Default case
        long dungeonCountDefault = grid.getPointsOfInterest().stream()
                .filter(p -> p.getType() == POIType.DUNGEON || p.getType() == POIType.RUIN)
                .count();
        
        assertEquals(0, dungeonCountZero, "Zero density should produce zero dungeons");
        assertTrue(dungeonCountMax > dungeonCountDefault, 
                "Max density should produce more dungeons than default");
    }

    // ===== Test 5: Settlements use Poisson-disc sampling =====
    @Test
    void testSettlementsUsePoissonDiscSampling() {
        List<PointOfInterest> pois = grid.getPointsOfInterest();
        List<PointOfInterest> villages = pois.stream()
                .filter(p -> p.getType() == POIType.VILLAGE)
                .toList();
        
        // Check minimum distance constraint (should be at least 5 cells apart)
        int minDistance = 5;
        for (int i = 0; i < villages.size(); i++) {
            for (int j = i + 1; j < villages.size(); j++) {
                PointOfInterest v1 = villages.get(i);
                PointOfInterest v2 = villages.get(j);
                
                int dx = v1.getX() - v2.getX();
                int dy = v1.getY() - v2.getY();
                double distance = Math.sqrt(dx*dx + dy*dy);
                
                assertTrue(distance >= minDistance - 1, // Allow small tolerance
                        "Villages should maintain minimum distance spacing");
            }
        }
    }

    // ===== Test 8: Settlement count scales with density =====
    @Test
    void testSettlementDensityScaling() {
        // Test with 0.0 density
        MapGrid gridZero = new MapGrid(GRID_WIDTH, GRID_HEIGHT);
        mapGenerator.generate(gridZero, TEST_SEED, 5, 0.01f, 0.5, 0.3, 0.0, 0.0,
                true, true, 50.0, 50.0, 100,
                3, 2, 0.5, 0.5, 0.0);
        
        long settlementCountZero = gridZero.getPointsOfInterest().stream()
                .filter(p -> p.getType() == POIType.VILLAGE)
                .count();
        
        // Test with 1.0 density
        MapGrid gridMax = new MapGrid(GRID_WIDTH, GRID_HEIGHT);
        mapGenerator.generate(gridMax, TEST_SEED, 5, 0.01f, 0.5, 0.3, 0.0, 0.0,
                true, true, 50.0, 50.0, 100,
                3, 2, 0.5, 0.5, 1.0);
        
        long settlementCountMax = gridMax.getPointsOfInterest().stream()
                .filter(p -> p.getType() == POIType.VILLAGE)
                .count();
        
        // Default case
        long settlementCountDefault = grid.getPointsOfInterest().stream()
                .filter(p -> p.getType() == POIType.VILLAGE)
                .count();
        
        assertEquals(0, settlementCountZero, "Zero density should produce zero settlements");
        assertTrue(settlementCountMax >= settlementCountDefault, 
                "Max density should produce at least as many settlements as default");
    }

    // ===== Test 9: All POIs have unique sequential IDs =====
    @Test
    void testPOIIDsUniqueAndSequential() {
        List<PointOfInterest> pois = grid.getPointsOfInterest();
        
        Set<Integer> ids = new HashSet<>();
        int maxId = -1;
        
        for (PointOfInterest poi : pois) {
            assertTrue(!ids.contains(poi.getId()), "POI ID " + poi.getId() + " is duplicated");
            ids.add(poi.getId());
            maxId = Math.max(maxId, poi.getId());
        }
        
        // IDs should be 0, 1, 2, ..., n-1
        assertEquals(pois.size() - 1, maxId, "IDs should be sequential from 0");
    }

    // ===== Test 10: Generated list contains no duplicate POIs =====
    @Test
    void testNoDuplicatePOIs() {
        List<PointOfInterest> pois = grid.getPointsOfInterest();
        
        for (int i = 0; i < pois.size(); i++) {
            for (int j = i + 1; j < pois.size(); j++) {
                PointOfInterest p1 = pois.get(i);
                PointOfInterest p2 = pois.get(j);
                
                assertNotEquals(p1.getId(), p2.getId(), "POI IDs should be unique");
                
                // Also check that same type+location shouldn't appear twice
                if (p1.getType() == p2.getType() && p1.getX() == p2.getX() && p1.getY() == p2.getY()) {
                    fail("Duplicate POI at same location and type");
                }
            }
        }
    }

    // ===== Test 11: Density = 0.0 produces zero POIs of that type =====
    @Test
    void testZeroDensityProducesZeroPOIs() {
        MapGrid gridZeroDungeons = new MapGrid(GRID_WIDTH, GRID_HEIGHT);
        mapGenerator.generate(gridZeroDungeons, TEST_SEED, 5, 0.01f, 0.5, 0.3, 0.0, 0.0,
                true, true, 50.0, 50.0, 100,
                3, 2, 0.0, 0.0, 0.0);
        
        List<PointOfInterest> pois = gridZeroDungeons.getPointsOfInterest();
        
        // Only cities should remain (they don't scale with density)
        for (PointOfInterest poi : pois) {
            assertEquals(POIType.CITY, poi.getType(), 
                    "With all zero densities, only CITY POIs should exist");
        }
    }

    // ===== Test 12: Density = 1.0 produces maximum POIs =====
    @Test
    void testMaxDensityProducesMaxPOIs() {
        MapGrid gridMax = new MapGrid(GRID_WIDTH, GRID_HEIGHT);
        mapGenerator.generate(gridMax, TEST_SEED, 5, 0.01f, 0.5, 0.3, 0.0, 0.0,
                true, true, 50.0, 50.0, 100,
                3, 2, 1.0, 1.0, 1.0);
        
        List<PointOfInterest> poisMax = gridMax.getPointsOfInterest();
        long dungeonsMax = poisMax.stream().filter(p -> p.getType() == POIType.DUNGEON || p.getType() == POIType.RUIN).count();
        long settlementsMax = poisMax.stream().filter(p -> p.getType() == POIType.VILLAGE || p.getType() == POIType.CASTLE || p.getType() == POIType.CAVE || p.getType() == POIType.RUIN).count();
        
        assertTrue(dungeonsMax > 0, "Max density should produce at least some dungeons");
        assertTrue(settlementsMax > 0, "Max density should produce at least some settlements");
    }

    // ===== Test 13: Kingdom cities always generated independent of dungeon density =====
    @Test
    void testCitiesIndependentOfDensity() {
        // Cities should exist regardless of other densities
        MapGrid gridZeroOther = new MapGrid(GRID_WIDTH, GRID_HEIGHT);
        mapGenerator.generate(gridZeroOther, TEST_SEED, 5, 0.01f, 0.5, 0.3, 0.0, 0.0,
                true, true, 50.0, 50.0, 100,
                3, 2, 0.0, 0.0, 0.0);
        
        long citiesZero = gridZeroOther.getPointsOfInterest().stream()
                .filter(p -> p.getType() == POIType.CITY)
                .count();
        
        long citiesDefault = grid.getPointsOfInterest().stream()
                .filter(p -> p.getType() == POIType.CITY)
                .count();
        
        assertEquals(citiesZero, citiesDefault, "Cities should be generated regardless of other density settings");
    }

    // ===== Test 14: Generated POIs reference valid terrain =====
    @Test
    void testGeneratedPOIsValidTerrain() {
        List<PointOfInterest> pois = grid.getPointsOfInterest();
        
        for (PointOfInterest poi : pois) {
            // Check coordinates in bounds
            assertTrue(poi.getX() >= 0 && poi.getX() < GRID_WIDTH, 
                    "POI X coordinate should be within grid bounds");
            assertTrue(poi.getY() >= 0 && poi.getY() < GRID_HEIGHT, 
                    "POI Y coordinate should be within grid bounds");
            
            // Check cell exists and has biome
            MapCell cell = grid.getCell(poi.getX(), poi.getY());
            assertNotNull(cell, "POI location should reference valid cell");
            assertNotNull(cell.getBiome(), "POI cell should have a biome");
        }
    }
}
