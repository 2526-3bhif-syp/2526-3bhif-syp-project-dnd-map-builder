package com.mapbuilder.mapbuilder.core.map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for generating Points of Interest (POIs) on a map using world-building rules.
 * Implements deterministic generation based on seed and terrain topology.
 * 
 * Generation rules:
 * - D-03: Kingdom cities at capital cells
 * - D-04: Dungeons at multi-kingdom borders and high caves
 * - D-05: Landmarks at waterfalls and peaks
 * - D-06: Settlements scattered using Poisson-disc sampling
 */
public class PointOfInterestGenerator {

    private static final String[] KINGDOM_CITY_SUFFIXES = {
        "City", "Haven", "Town", "Hold", "Fort", "Keep"
    };

    private static final String[] VILLAGE_NAMES = {
        "Briarwood", "Sunhaven", "Willowmere", "Stonepeak", "Thornvale",
        "Silverdell", "Brighthaven", "Goldleaf", "Shadowbrook", "Eastwind",
        "Westmarch", "Northwood", "Southport", "Harborside", "Crossroads"
    };

    /**
     * Main method to generate all Points of Interest for a map.
     * Returns a new list of POIs with sequential unique IDs.
     * 
     * @param grid The MapGrid to generate POIs for
     * @param seed Random seed for deterministic generation
     * @param dungeonDensity Dungeon density multiplier (0.0–1.0)
     * @param landmarkDensity Landmark density multiplier (0.0–1.0)
     * @param settlementDensity Settlement density multiplier (0.0–1.0)
     * @return List of generated PointOfInterest objects
     */
    public static List<PointOfInterest> generatePointsOfInterest(
            MapGrid grid, int seed, double dungeonDensity, double landmarkDensity, double settlementDensity) {
        
        List<PointOfInterest> pois = new ArrayList<>();
        AtomicInteger idCounter = new AtomicInteger(0);
        
        // Clamp density values to valid range (Rule 2: input validation)
        dungeonDensity = Math.max(0.0, Math.min(1.0, dungeonDensity));
        landmarkDensity = Math.max(0.0, Math.min(1.0, landmarkDensity));
        settlementDensity = Math.max(0.0, Math.min(1.0, settlementDensity));
        
        // Generate in order: cities, dungeons, landmarks, settlements
        addKingdomCities(pois, grid, seed, idCounter);
        addDungeonAndRuins(pois, grid, seed, dungeonDensity, idCounter);
        addLandmarks(pois, grid, seed, landmarkDensity, idCounter);
        addSettlements(pois, grid, seed, settlementDensity, idCounter);
        
        return pois;
    }

    /**
     * D-03: Place CITY POI at each kingdom's capital cell.
     */
    private static void addKingdomCities(List<PointOfInterest> pois, MapGrid grid, int seed, AtomicInteger idCounter) {
        Set<Kingdom> processedKingdoms = new HashSet<>();
        
        // Iterate all cells to find unique kingdoms
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                MapCell cell = grid.getCell(x, y);
                if (cell != null && cell.getKingdom() != null) {
                    Kingdom kingdom = cell.getKingdom();
                    if (!processedKingdoms.contains(kingdom)) {
                        processedKingdoms.add(kingdom);
                        
                        // Place city at capital
                        MapCell capital = kingdom.getCapital();
                        if (capital != null) {
                            String cityName = generateCityName(kingdom.getId(), seed);
                            PointOfInterest city = new PointOfInterest(
                                idCounter.getAndIncrement(),
                                capital.getX(),
                                capital.getY(),
                                POIType.CITY,
                                cityName,
                                "kingdom_capital"
                            );
                            pois.add(city);
                        }
                    }
                }
            }
        }
    }

    /**
     * D-04: Place DUNGEON/RUIN at multi-kingdom borders and high caves.
     * Uses grid area and dungeonDensity to calculate count.
     */
    private static void addDungeonAndRuins(
            List<PointOfInterest> pois, MapGrid grid, int seed, double dungeonDensity, AtomicInteger idCounter) {
        
        // Density is already 0.0-1.0, multiply directly (not by 1/100)
        int baseCount = (int) (grid.getWidth() * grid.getHeight() * dungeonDensity * 0.015);
        int targetCount = Math.max(1, baseCount);  // Ensure at least 1 dungeon attempt
        
        Random rand = new Random(seed + 1001);
        int placed = 0;
        int attempts = 0;
        int maxAttempts = Math.max(100, targetCount * 5);
        
        while (placed < targetCount && attempts < maxAttempts) {
            attempts++;
            
            int x = rand.nextInt(grid.getWidth());
            int y = rand.nextInt(grid.getHeight());
            MapCell cell = grid.getCell(x, y);
            
            if (cell != null && isDungeonLocation(grid, cell)) {
                String dungeonName = "Dungeon_" + placed;
                POIType type = rand.nextBoolean() ? POIType.DUNGEON : POIType.RUIN;
                
                PointOfInterest dungeon = new PointOfInterest(
                    idCounter.getAndIncrement(),
                    x,
                    y,
                    type,
                    dungeonName,
                    "dungeon_border_cave"
                );
                pois.add(dungeon);
                placed++;
            }
        }
    }

    /**
     * Check if a cell qualifies as a dungeon location (border or high cave).
     */
    private static boolean isDungeonLocation(MapGrid grid, MapCell cell) {
        if (cell.getElevation() <= 0.3) return false; // Must be above water level baseline
        
        // Check for multi-kingdom border (3+ adjacent kingdoms)
        int uniqueKingdoms = countAdjacentKingdoms(grid, cell);
        if (uniqueKingdoms >= 3) return true;
        
        // Check for high cave/mountain with low kingdom probability
        if (cell.getElevation() > 0.5 && cell.getKingdom() == null) {
            return true;
        }
        
        return false;
    }

    /**
     * Count unique kingdoms in adjacent cells.
     */
    private static int countAdjacentKingdoms(MapGrid grid, MapCell cell) {
        Set<Kingdom> adjacentKingdoms = new HashSet<>();
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};
        
        for (int[] dir : dirs) {
            int nx = cell.getX() + dir[0];
            int ny = cell.getY() + dir[1];
            MapCell neighbor = grid.getCell(nx, ny);
            if (neighbor != null && neighbor.getKingdom() != null) {
                adjacentKingdoms.add(neighbor.getKingdom());
            }
        }
        
        return adjacentKingdoms.size();
    }

    /**
     * D-05: Place LANDMARK at waterfalls and peaks.
     * Uses grid area and landmarkDensity to calculate count.
     */
    private static void addLandmarks(
            List<PointOfInterest> pois, MapGrid grid, int seed, double landmarkDensity, AtomicInteger idCounter) {
        
        // Density is already 0.0-1.0, multiply directly (not by 1/100)
        int baseCount = (int) (grid.getWidth() * grid.getHeight() * landmarkDensity * 0.01);
        int targetCount = Math.max(0, baseCount);
        
        Random rand = new Random(seed + 1002);
        int placed = 0;
        int attempts = 0;
        int maxAttempts = Math.max(100, targetCount * 5);
        
        while (placed < targetCount && attempts < maxAttempts) {
            attempts++;
            
            int x = rand.nextInt(grid.getWidth());
            int y = rand.nextInt(grid.getHeight());
            MapCell cell = grid.getCell(x, y);
            
            if (cell != null && isLandmarkLocation(grid, cell)) {
                String landmarkName = generateLandmarkName(cell, seed + placed);
                
                PointOfInterest landmark = new PointOfInterest(
                    idCounter.getAndIncrement(),
                    x,
                    y,
                    POIType.LANDMARK,
                    landmarkName,
                    "landmark_natural"
                );
                pois.add(landmark);
                placed++;
            }
        }
    }

    /**
     * Check if a cell qualifies as a landmark location (peak or waterfall).
     */
    private static boolean isLandmarkLocation(MapGrid grid, MapCell cell) {
        if (cell.getElevation() <= 0.3) return false; // Must be elevated
        
        // Check if adjacent to water
        boolean nearWater = isAdjacentToWater(grid, cell);
        if (cell.getElevation() > 0.6 && nearWater) return true; // Waterfall
        
        // Check if peak (highest in local cluster)
        if (isLocalPeak(grid, cell)) return true;
        
        return false;
    }

    /**
     * Check if cell is adjacent to water (river or coast).
     */
    private static boolean isAdjacentToWater(MapGrid grid, MapCell cell) {
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};
        
        for (int[] dir : dirs) {
            int nx = cell.getX() + dir[0];
            int ny = cell.getY() + dir[1];
            MapCell neighbor = grid.getCell(nx, ny);
            if (neighbor != null && (neighbor.isRiver() || neighbor.isLake() || neighbor.getElevation() <= 0.3)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check if this cell is a local elevation peak.
     */
    private static boolean isLocalPeak(MapGrid grid, MapCell cell) {
        double cellElev = cell.getElevation();
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};
        
        for (int[] dir : dirs) {
            int nx = cell.getX() + dir[0];
            int ny = cell.getY() + dir[1];
            MapCell neighbor = grid.getCell(nx, ny);
            if (neighbor != null && neighbor.getElevation() > cellElev) {
                return false; // Found a higher neighbor
            }
        }
        
        return true; // No higher neighbors found
    }

    /**
     * Generate a human-readable landmark name.
     */
    private static String generateLandmarkName(MapCell cell, int seed) {
        if (cell.getKingdom() != null) {
            return "Peak of " + cell.getKingdom().getId();
        }
        return "Landmark_" + cell.getX() + "_" + cell.getY();
    }

    /**
     * D-06: Scatter VILLAGE/TOWN POIs using Poisson-disc sampling.
     * Uses grid area and settlementDensity to calculate count.
     * Improved algorithm samples annulus around each point, not just 8 directions.
     */
    private static void addSettlements(
            List<PointOfInterest> pois, MapGrid grid, int seed, double settlementDensity, AtomicInteger idCounter) {
        
        // Density is already 0.0-1.0, multiply directly (not by 1/100)
        int targetCount = (int) (grid.getWidth() * grid.getHeight() * settlementDensity * 0.025);
        if (targetCount <= 0) return;
        
        Random rand = new Random(seed + 1003);
        List<MapCell> habitableCells = new ArrayList<>();
        
        // Collect habitable cells from interior only (avoid edges)
        int margin = 10;  // Avoid placing settlements too close to map edges
        for (int x = margin; x < grid.getWidth() - margin; x++) {
            for (int y = margin; y < grid.getHeight() - margin; y++) {
                MapCell cell = grid.getCell(x, y);
                if (cell != null && isHabitableBiome(cell.getBiome())) {
                    habitableCells.add(cell);
                }
            }
        }
        
        if (habitableCells.isEmpty()) return;
        
        // Improved Poisson-disc sampling with annulus sampling
        int minDistance = 8;  // Minimum distance between settlements
        Set<MapCell> selected = new HashSet<>();
        List<MapCell> active = new ArrayList<>();
        
        // Start with a random cell from interior
        MapCell first = habitableCells.get(rand.nextInt(habitableCells.size()));
        selected.add(first);
        active.add(first);
        
        int searchRadius = minDistance * 3;  // Search within 3x minimum distance
        
        while (!active.isEmpty() && selected.size() < targetCount) {
            int idx = rand.nextInt(active.size());
            MapCell current = active.get(idx);
            boolean found = false;
            
            // Sample random points around current in annulus (minDistance, searchRadius)
            int samples = 30;  // Try 30 random samples around this point
            for (int i = 0; i < samples && !found && selected.size() < targetCount; i++) {
                // Random point in annulus: minDistance <= r <= searchRadius
                double angle = rand.nextDouble() * Math.PI * 2;
                double radius = minDistance + rand.nextDouble() * (searchRadius - minDistance);
                int nx = (int) (current.getX() + radius * Math.cos(angle));
                int ny = (int) (current.getY() + radius * Math.sin(angle));
                
                // Clamp to map bounds and margin
                nx = Math.max(margin, Math.min(nx, grid.getWidth() - margin - 1));
                ny = Math.max(margin, Math.min(ny, grid.getHeight() - margin - 1));
                
                MapCell neighbor = grid.getCell(nx, ny);
                if (neighbor != null && isHabitableBiome(neighbor.getBiome()) && !selected.contains(neighbor)) {
                    // Verify minimum distance to all selected cells
                    boolean tooClose = false;
                    for (MapCell sel : selected) {
                        int dx = sel.getX() - neighbor.getX();
                        int dy = sel.getY() - neighbor.getY();
                        double dist = Math.sqrt(dx*dx + dy*dy);
                        if (dist < minDistance) {
                            tooClose = true;
                            break;
                        }
                    }
                    
                    if (!tooClose) {
                        selected.add(neighbor);
                        active.add(neighbor);
                        found = true;
                    }
                }
            }
            
            if (!found) {
                active.remove(idx);
            }
        }
        
        // Create POIs from selected cells
        int villageCount = 0;
        for (MapCell cell : selected) {
            String villageName = generateVillageName(villageCount, seed);
            
            PointOfInterest settlement = new PointOfInterest(
                idCounter.getAndIncrement(),
                cell.getX(),
                cell.getY(),
                POIType.VILLAGE,
                villageName,
                "settlement_scattered"
            );
            pois.add(settlement);
            villageCount++;
        }
    }

    /**
     * Check if a biome is habitable for settlements.
     */
    private static boolean isHabitableBiome(Biome biome) {
        if (biome == null) return false;
        
        return biome == Biome.GRASSLAND || 
               biome == Biome.TEMPERATE_FOREST ||
               biome == Biome.SAVANNA ||
               biome == Biome.TEMPERATE_RAINFOREST ||
               biome == Biome.SHRUBLAND;
    }

    /**
     * Generate a deterministic city name from kingdom ID and seed.
     */
    private static String generateCityName(int kingdomId, int seed) {
        String baseName = new String[]{
            "Ashland", "Westmarch", "Easthold", "Northfort", "Southaven",
            "Silverhill", "Goldgate", "Stormsgate", "Oakhall", "Meadowbrook"
        }[(kingdomId + seed) % 10];
        
        int suffixIdx = (kingdomId + seed * 7) % KINGDOM_CITY_SUFFIXES.length;
        return baseName + " " + KINGDOM_CITY_SUFFIXES[suffixIdx];
    }

    /**
     * Generate a deterministic village name.
     */
    private static String generateVillageName(int index, int seed) {
        int nameIdx = (index + seed) % VILLAGE_NAMES.length;
        return VILLAGE_NAMES[nameIdx];
    }
}
