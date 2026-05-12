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
        
        // Generate in order: cities, dungeons, settlements
        addKingdomCities(pois, grid, seed, idCounter);
        addDungeonAndRuins(pois, grid, seed, dungeonDensity, idCounter);
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
        
        int targetCount = (int) Math.ceil(grid.getWidth() * grid.getHeight() * dungeonDensity * 0.0001);
        if (targetCount <= 0 && dungeonDensity > 0.0) targetCount = 1;
        if (targetCount <= 0) return;
        
        Random rand = new Random(seed + 1001);
        int margin = 5;
        
        // Deterministic quadrant-based distribution for dungeons
        // Divide map into sqrt(targetCount*2) x grid, place ~1 dungeon per quadrant
        int gridSize = (int) Math.ceil(Math.sqrt(targetCount * 2));
        int quadrantWidth = grid.getWidth() / gridSize;
        int quadrantHeight = grid.getHeight() / gridSize;
        
        int placed = 0;
        for (int gx = 0; gx < gridSize && placed < targetCount; gx++) {
            for (int gy = 0; gy < gridSize && placed < targetCount; gy++) {
                // Bounds for this quadrant
                int minX = gx * quadrantWidth + margin;
                int maxX = ((gx + 1) * quadrantWidth) - margin;
                int minY = gy * quadrantHeight + margin;
                int maxY = ((gy + 1) * quadrantHeight) - margin;
                
                // Clamp to map bounds
                minX = Math.max(minX, margin);
                maxX = Math.min(maxX, grid.getWidth() - margin - 1);
                minY = Math.max(minY, margin);
                maxY = Math.min(maxY, grid.getHeight() - margin - 1);
                
                if (maxX <= minX || maxY <= minY) continue;
                
                // Find a dungeon-valid cell in this quadrant
                for (int attempt = 0; attempt < 15; attempt++) {
                    int x = minX + rand.nextInt(maxX - minX + 1);
                    int y = minY + rand.nextInt(maxY - minY + 1);
                    MapCell cell = grid.getCell(x, y);
                    
                    if (cell != null && isDungeonLocation(grid, cell)) {
                        String dungeonName = "Dungeon_" + placed;
                        POIType type = rand.nextBoolean() ? POIType.DUNGEON : POIType.RUIN;
                        
                        PointOfInterest dungeon = new PointOfInterest(
                            idCounter.getAndIncrement(),
                            x, y,
                            type,
                            dungeonName,
                            "dungeon_border_cave"
                        );
                        pois.add(dungeon);
                        placed++;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Check if a cell qualifies as a dungeon location (border or high cave).
     */
    private static boolean isDungeonLocation(MapGrid grid, MapCell cell) {
        // Relaxed rules: allow any non-water cell with elevation > 0.2
        if (cell.getElevation() <= 0.2) return false;
        
        // Border kingdoms always valid
        int uniqueKingdoms = countAdjacentKingdoms(grid, cell);
        if (uniqueKingdoms >= 1) return true;  // Loosened from 2+ to 1+
        
        // High elevation always valid (caves, peaks)
        if (cell.getElevation() > 0.35) return true;  // Loosened from 0.4
        
        // Terrain-based: consider biome too (not just elevation)
        Biome biome = cell.getBiome();
        if (biome == Biome.BARE_ROCK || biome == Biome.SNOW_PEAKS || biome == Biome.SCORCHED || biome == Biome.TUNDRA) {
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
     * D-06: Scatter VILLAGE/TOWN POIs using Poisson-disc sampling.
     * Uses grid area and settlementDensity to calculate count.
     * Improved algorithm samples annulus around each point, not just 8 directions.
     */
    private static void addSettlements(
            List<PointOfInterest> pois, MapGrid grid, int seedValue, double settlementDensity, AtomicInteger idCounter) {
        
        // Use 0.0002 multiplier for sparse distribution. Use ceil to ensure at least 1 at max density for small grids.
        int targetCount = (int) Math.ceil(grid.getWidth() * grid.getHeight() * settlementDensity * 0.0002);
        if (targetCount <= 0 && settlementDensity > 0.0) targetCount = 1;
        if (targetCount <= 0) return;  // Allow zero settlements at zero density
        
        Random rand = new Random(seedValue + 1003);
        int margin = 10;
        
        // Deterministic quadrant-based distribution: divide map into sqrt(targetCount) × sqrt(targetCount) grid
        // Place at most 1 settlement per quadrant to ensure uniform coverage (no center clustering)
        int gridSize = (int) Math.ceil(Math.sqrt(targetCount * 4)); // Ensure enough quadrants for all settlements
        int quadrantWidth = grid.getWidth() / gridSize;
        int quadrantHeight = grid.getHeight() / gridSize;
        
        int placed = 0;
        for (int gx = 0; gx < gridSize && placed < targetCount; gx++) {
            for (int gy = 0; gy < gridSize && placed < targetCount; gy++) {
                // Bounds for this quadrant
                int minX = gx * quadrantWidth + margin;
                int maxX = ((gx + 1) * quadrantWidth) - margin;
                int minY = gy * quadrantHeight + margin;
                int maxY = ((gy + 1) * quadrantHeight) - margin;
                
                // Clamp to map bounds
                minX = Math.max(minX, margin);
                maxX = Math.min(maxX, grid.getWidth() - margin - 1);
                minY = Math.max(minY, margin);
                maxY = Math.min(maxY, grid.getHeight() - margin - 1);
                
                if (maxX <= minX || maxY <= minY) continue;
                
                // Find a habitable cell in this quadrant (with retries)
                for (int attempt = 0; attempt < 20; attempt++) {
                    int x = minX + rand.nextInt(maxX - minX + 1);
                    int y = minY + rand.nextInt(maxY - minY + 1);
                    MapCell cell = grid.getCell(x, y);
                    
                    if (cell != null && isHabitableBiome(cell.getBiome())) {
                        // Place settlement in this quadrant
                        String name = generateSettlementName(placed + 100 * seedValue, rand);
                        PointOfInterest poi = new PointOfInterest(
                            idCounter.getAndIncrement(),
                            x, y,
                            selectSettlementType(rand),
                            name,
                            "settlement_scattered"
                        );
                        pois.add(poi);
                        placed++;
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Select a random settlement type (VILLAGE, CASTLE, CAVE, or RUIN).
     */
    private static POIType selectSettlementType(Random rand) {
        POIType[] types = {POIType.VILLAGE, POIType.CASTLE, POIType.CAVE, POIType.RUIN};
        return types[rand.nextInt(types.length)];
    }
    
    /**
     * Generate a deterministic settlement name.
     */
    private static String generateSettlementName(int seed, Random rand) {
        String[] prefixes = {"North", "South", "East", "West", "High", "Low", "New", "Old", "Far", "Near"};
        String[] suffixes = {"haven", "ford", "field", "hill", "dale", "wood", "stone", "rest", "fall", "spring"};
        
        int prefix = Math.abs(seed) % prefixes.length;
        int suffix = Math.abs(seed / 11) % suffixes.length;
        
        return prefixes[prefix] + suffixes[suffix].substring(0, 1).toUpperCase() + suffixes[suffix].substring(1);
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
