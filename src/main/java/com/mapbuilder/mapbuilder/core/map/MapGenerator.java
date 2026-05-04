package com.mapbuilder.mapbuilder.core.map;

import com.mapbuilder.mapbuilder.core.math.FastNoiseLite;
import java.util.Random;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class MapGenerator {
    // --- River Generation Constants ---
    public static final int RIVER_DENSITY_DIVISOR = 64000;
    public static final int MAX_RIVER_ATTEMPTS_MULTIPLIER = 50;
    
    public static final double MIN_SOURCE_ELEVATION_OFFSET = 0.3;
    public static final double MIN_SOURCE_RAINFALL = 0.3;
    
    public static final double MIN_RIVER_LENGTH_RATIO = 0.1; // 10% of map width
    public static final double MAX_RIVER_LENGTH_RATIO = 0.1; // 10% of map area
    public static final double RIVER_CARVING_DEPTH = 0.001;

    public static final int MIN_LAKE_AREA = 100;
    public static final double LAKE_AREA_RATIO = 1.0 / 1600.0;
    // ----------------------------------

    public void generate(MapGrid grid, int seed, int octaves, float scale, double falloff, double waterLevel, double temperatureBias, double rainfallBias,
                         boolean enableRivers, boolean enableLakes, double riverDensityPercent, double lakeSizePercent, int customMinLakeArea,
                         int kingdomCount, int lloydPasses, double dungeonDensity, double landmarkDensity, double settlementDensity) {
        int width = grid.getWidth();
        int height = grid.getHeight();

        FastNoiseLite elevationNoise = new FastNoiseLite(seed);
        elevationNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        elevationNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        elevationNoise.SetFractalOctaves(octaves);
        elevationNoise.SetFrequency(scale);

        FastNoiseLite tempNoise = new FastNoiseLite(seed + 1);
        tempNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        tempNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        tempNoise.SetFractalOctaves(octaves);
        tempNoise.SetFrequency(scale);

        FastNoiseLite rainNoise = new FastNoiseLite(seed + 2);
        rainNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        rainNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        rainNoise.SetFractalOctaves(octaves);
        rainNoise.SetFrequency(scale);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                MapCell cell = grid.getCell(x, y);

                // Reset river and lake state from previous generation
                cell.setRiver(false);
                cell.setLake(false);

                double nx = (double) x / width;
                double ny = (double) y / height;

                // Elevation (offset by 0.5 to avoid integer grid artifacts in Perlin noise)
                double eNoise = elevationNoise.GetNoise((float) x + 0.5f, (float) y + 0.5f) * 0.5 + 0.5;
                double e = eNoise; 
                // Island falloff
                double cx = nx * 2 - 1;
                double cy = ny * 2 - 1;
                double d = Math.sqrt(cx * cx + cy * cy);
                // Squaring the distance creates a non-linear falloff (flatter in center, steeper at edges)
                // This keeps the center solid and connected, and pushes the water to the edges.
                e = e - Math.pow(d, 2.0) * (1.0 + falloff);
                cell.setElevation(e);

                // Temperature
                double tNoise = tempNoise.GetNoise((float) x + 0.5f, (float) y + 0.5f) * 0.5 + 0.5;
                double t = tNoise;
                // Latitude gradient (smooth cosine instead of sharp absolute value to prevent quadrant lines)
                double lat = Math.cos((ny * 2 - 1) * Math.PI / 2.0);
                
                // Scale elevation penalty relative to max possible land height to prevent freezing at low water levels
                double maxExpectedE = 1.0;
                double elevationRange = Math.max(0.1, maxExpectedE - waterLevel);
                double elevationPenalty = 0.0;
                if (e > waterLevel) {
                    elevationPenalty = ((e - waterLevel) / elevationRange) * 0.5;
                }
                
                t = t + lat * 0.5 - elevationPenalty + temperatureBias;
                // Ocean temp moderation
                if (Math.abs(e - waterLevel) < 0.1) {
                    t = (t + 0.5) / 2.0; 
                }
                t = Math.clamp(t, 0.0, 1.0); // Clamp to standard range
                cell.setTemperature(t);

                // Rainfall
                double rNoise = rainNoise.GetNoise((float) x + 0.5f, (float) y + 0.5f) * 0.5 + 0.5;
                double r = rNoise;
                // Inverse temp bias
                r = r + (1.0 - t) * 0.3 + rainfallBias;

                // Wind logic (rain shadow)
                if (x > 0) {
                    // Use noise derivative to prevent macro-falloff from creating vertical stripes
                    double prevNoise = elevationNoise.GetNoise((float) (x - 1) + 0.5f, (float) y + 0.5f) * 0.5 + 0.5;
                    if (eNoise > prevNoise) {
                        r += 0.2;
                    } else if (eNoise < prevNoise) {
                        r -= 0.2;
                    }
                }
                r = Math.clamp(r, 0.0, 1.0); // Clamp to standard range
                cell.setRainfall(r);

                // Resolve Biome
                Biome biome = resolveBiome(e, t, r, waterLevel);
                cell.setBiome(biome);

                // Mixed color
                cell.setMixedColorARGB(biome.getColorARGB());
            }
        }

        if (enableRivers || enableLakes) {
            int baseRiverCount = (width * height) / RIVER_DENSITY_DIVISOR;
            int targetRiverCount = (int) (baseRiverCount * (riverDensityPercent / 100.0));
            
            // If rivers are disabled but lakes are enabled, we might want to still run the river logic but only keep the lakes, or run a separate lake logic.
            // A simple way to get natural lakes is to run the river trace, but not mark the path as rivers.
            generateHydrology(grid, seed, targetRiverCount, waterLevel, enableRivers, enableLakes, lakeSizePercent, customMinLakeArea);
        }

        if (kingdomCount > 0) {
            generateKingdoms(grid, seed, kingdomCount, Math.min(5, lloydPasses), waterLevel);
        }

        // Generate Points of Interest after kingdoms
        List<PointOfInterest> pois = PointOfInterestGenerator.generatePointsOfInterest(
            grid, seed, dungeonDensity, landmarkDensity, settlementDensity
        );
        grid.setPointsOfInterest(pois);
    }

    /**
     * Generates terrain for a LOD sub-region at higher resolution.
     * Samples noise at fractional world coordinates so the same seed produces
     * consistent, finer-grained detail when zoomed in.
     * Rivers/lakes are NOT generated here — they are propagated from the base grid
     * after generation to keep spatial consistency and avoid expensive re-tracing.
     */
    public void generateLOD(MapGrid lodGrid, int seed, int octaves, float scale,
                            double falloff, double waterLevel,
                            double temperatureBias, double rainfallBias,
                            double worldOffsetX, double worldOffsetY,
                            double cellWorldSize,
                            int worldWidth, int worldHeight) {
        int w = lodGrid.getWidth();
        int h = lodGrid.getHeight();

        FastNoiseLite elevationNoise = new FastNoiseLite(seed);
        elevationNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        elevationNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        elevationNoise.SetFractalOctaves(octaves);
        elevationNoise.SetFrequency(scale);

        FastNoiseLite tempNoise = new FastNoiseLite(seed + 1);
        tempNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        tempNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        tempNoise.SetFractalOctaves(octaves);
        tempNoise.SetFrequency(scale);

        FastNoiseLite rainNoise = new FastNoiseLite(seed + 2);
        rainNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        rainNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        rainNoise.SetFractalOctaves(octaves);
        rainNoise.SetFrequency(scale);

        for (int cx = 0; cx < w; cx++) {
            for (int cy = 0; cy < h; cy++) {
                MapCell cell = lodGrid.getCell(cx, cy);
                cell.setRiver(false);
                cell.setLake(false);

                float wx = (float)(worldOffsetX + cx * cellWorldSize);
                float wy = (float)(worldOffsetY + cy * cellWorldSize);

                double eNoise = elevationNoise.GetNoise(wx + 0.5f, wy + 0.5f) * 0.5 + 0.5;
                double e = eNoise;
                // Island falloff uses the world position normalised against the original grid
                double nx = wx / worldWidth;
                double ny = wy / worldHeight;
                double fcx = nx * 2 - 1;
                double fcy = ny * 2 - 1;
                double d2 = fcx * fcx + fcy * fcy;
                e = e - d2 * (1.0 + falloff);
                cell.setElevation(e);

                double tNoise = tempNoise.GetNoise(wx + 0.5f, wy + 0.5f) * 0.5 + 0.5;
                double t = tNoise;
                double lat = Math.cos((ny * 2 - 1) * Math.PI / 2.0);
                
                double maxExpectedE = 1.0;
                double elevationRange = Math.max(0.1, maxExpectedE - waterLevel);
                double elevationPenalty = 0.0;
                if (e > waterLevel) {
                    elevationPenalty = ((e - waterLevel) / elevationRange) * 0.5;
                }
                
                t = t + lat * 0.5 - elevationPenalty + temperatureBias;
                if (Math.abs(e - waterLevel) < 0.1) t = (t + 0.5) / 2.0;
                t = Math.clamp(t, 0.0, 1.0);
                cell.setTemperature(t);

                double rNoise = rainNoise.GetNoise(wx + 0.5f, wy + 0.5f) * 0.5 + 0.5;
                double r = rNoise;
                r = r + (1.0 - t) * 0.3 + rainfallBias;
                if (cx > 0) {
                    float prevWx = (float)(worldOffsetX + (cx - 1) * cellWorldSize);
                    double prevNoise = elevationNoise.GetNoise(prevWx + 0.5f, wy + 0.5f) * 0.5 + 0.5;
                    if (eNoise > prevNoise) r += 0.2;
                    else if (eNoise < prevNoise) r -= 0.2;
                }
                r = Math.clamp(r, 0.0, 1.0);
                cell.setRainfall(r);

                Biome biome = resolveBiome(e, t, r, waterLevel);
                cell.setBiome(biome);
                cell.setMixedColorARGB(biome.getColorARGB());
            }
        }

    }

    private void generateHydrology(MapGrid grid, int seed, int targetRiverCount, double waterLevel, boolean enableRivers, boolean enableLakes, double lakeSizePercent, int customMinLakeArea) {
        int width = grid.getWidth();
        int height = grid.getHeight();
        Random rand = new Random(seed + 999);
        
        int successfulRivers = 0;
        int attempts = 0;
        
        // If targetRiverCount is 0 but we want lakes, we need some attempts.
        int effectiveRiverCount = Math.max(targetRiverCount, (enableLakes ? ((width * height) / RIVER_DENSITY_DIVISOR) : 0));
        int maxAttempts = effectiveRiverCount * MAX_RIVER_ATTEMPTS_MULTIPLIER; 

        while (successfulRivers < effectiveRiverCount && attempts < maxAttempts) {
            attempts++;
            // Find a source
            int sx = rand.nextInt(width);
            int sy = rand.nextInt(height);
            MapCell source = grid.getCell(sx, sy);
            
            // Source must be high elevation and relatively wet
            if (source.getElevation() < waterLevel + MIN_SOURCE_ELEVATION_OFFSET || source.getRainfall() < MIN_SOURCE_RAINFALL || source.isRiver() || source.isLake()) {
                continue;
            }

            if (traceHydrology(grid, source, waterLevel, enableRivers, enableLakes, lakeSizePercent, customMinLakeArea, targetRiverCount > 0 && successfulRivers < targetRiverCount)) {
                successfulRivers++;
            }
        }
    }

    private boolean traceHydrology(MapGrid grid, MapCell start, double waterLevel, boolean enableRivers, boolean enableLakes, double lakeSizePercent, int customMinLakeArea, boolean isRiverAttempt) {
        MapCell current = start;
        List<MapCell> path = new ArrayList<>();
        int maxLen = (int) (grid.getWidth() * grid.getHeight() * MAX_RIVER_LENGTH_RATIO); // Prevent infinite loops
        int minRiverLen = Math.max(10, (int) (grid.getWidth() * MIN_RIVER_LENGTH_RATIO)); // Minimum river length
        int len = 0;
        boolean formedLake = false;

        while (len < maxLen) {
            path.add(current);
            
            if (current.getElevation() <= waterLevel) {
                break; // Reached ocean
            }

            MapCell next = null;
            double lowest = current.getElevation();
            MapCell lowestNeighbor = null;
            double absoluteLowest = Double.MAX_VALUE;

            // Find lowest neighbor
            int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};
            for (int[] dir : dirs) {
                MapCell neighbor = grid.getCell(current.getX() + dir[0], current.getY() + dir[1]);
                if (neighbor != null && !path.contains(neighbor)) {
                    if (neighbor.getElevation() < lowest) {
                        lowest = neighbor.getElevation();
                        next = neighbor;
                    }
                    if (neighbor.getElevation() < absoluteLowest) {
                        absoluteLowest = neighbor.getElevation();
                        lowestNeighbor = neighbor;
                    }
                }
            }

            if (next == null) {
                // Local minimum. If river is too short, force it to continue by carving through the lowest available neighbor
                if (path.size() < minRiverLen && lowestNeighbor != null && isRiverAttempt) {
                    lowestNeighbor.setElevation(current.getElevation() - RIVER_CARVING_DEPTH); // Carve down
                    next = lowestNeighbor;
                } else {
                    // It's long enough, or no neighbors left, form a lake
                    formedLake = true;
                    break;
                }
            } else if (next.isRiver() || next.isLake()) {
                // Joined another river or lake
                break;
            }

            current = next;
            len++;
        }

        boolean success = false;
        
        // If it's a river attempt and long enough, or we don't care about river length for standalone lakes
        if (path.size() >= minRiverLen || (!isRiverAttempt && formedLake)) {
            if (formedLake && enableLakes) {
                formLake(grid, current, waterLevel, lakeSizePercent, customMinLakeArea);
                success = true;
            }
            if (enableRivers && isRiverAttempt && path.size() >= minRiverLen) {
                for (MapCell cell : path) {
                    cell.setRiver(true);
                    // Make river thicker by adding adjacent cells
                    int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
                    for (int[] dir : dirs) {
                        MapCell neighbor = grid.getCell(cell.getX() + dir[0], cell.getY() + dir[1]);
                        if (neighbor != null && neighbor.getElevation() > waterLevel) {
                            neighbor.setRiver(true);
                        }
                    }
                }
                success = true;
            }
        }
        return success;
    }

    private void formLake(MapGrid grid, MapCell center, double waterLevel, double lakeSizePercent, int customMinLakeArea) {
        // Organic lake generation using elevation-based flooding
        int baseArea = Math.max(customMinLakeArea, (int) (grid.getWidth() * grid.getHeight() * LAKE_AREA_RATIO)); // Scale lake size based on map size
        int targetSize = (int) ((baseArea + (int)(Math.random() * baseArea)) * (lakeSizePercent / 100.0)); // Apply size multiplier

        PriorityQueue<MapCell> floodQueue = new PriorityQueue<>(Comparator.comparingDouble(MapCell::getElevation));
        Set<MapCell> visited = new HashSet<>();
        List<MapCell> lakeCells = new ArrayList<>();

        floodQueue.add(center);
        visited.add(center);

        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};

        while (!floodQueue.isEmpty() && lakeCells.size() < targetSize) {
            MapCell current = floodQueue.poll();
            
            // Only add to lake if it's land
            if (current.getElevation() > waterLevel) {
                lakeCells.add(current);
                current.setLake(true);

                // Add neighbors to the queue
                for (int[] dir : dirs) {
                    MapCell neighbor = grid.getCell(current.getX() + dir[0], current.getY() + dir[1]);
                    // Don't flood back into the ocean or outside bounds
                    if (neighbor != null && neighbor.getElevation() > waterLevel && !visited.contains(neighbor)) {
                        visited.add(neighbor);
                        floodQueue.add(neighbor);
                    }
                }
            } else {
                // If the lake flooded into the ocean, stop filling here.
                break;
            }
        }
    }

    private static class ExpansionNode implements Comparable<ExpansionNode> {
        final MapCell cell;
        final double cost;
        final Kingdom kingdom;

        ExpansionNode(MapCell cell, double cost, Kingdom kingdom) {
            this.cell = cell;
            this.cost = cost;
            this.kingdom = kingdom;
        }

        @Override
        public int compareTo(ExpansionNode other) {
            return Double.compare(this.cost, other.cost);
        }
    }

    private void generateKingdoms(MapGrid grid, int seed, int kingdomCount, int lloydPasses, double waterLevel) {
        int width = grid.getWidth();
        int height = grid.getHeight();
        Random rand = new Random(seed + 888);

        List<Kingdom> kingdoms = new ArrayList<>();
        List<MapCell> landCells = new ArrayList<>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                MapCell cell = grid.getCell(x, y);
                cell.setKingdom(null);
                if (cell.getElevation() > waterLevel) {
                    landCells.add(cell);
                }
            }
        }

        if (landCells.isEmpty() || kingdomCount <= 0) return;

        // Poisson disc simplified / random selection for capitals
        for (int i = 0; i < kingdomCount; i++) {
            MapCell capital = landCells.get(rand.nextInt(landCells.size()));
            int color = 0xFF000000 | rand.nextInt(0xFFFFFF);
            kingdoms.add(new Kingdom(i, color, capital));
        }

        for (int pass = 0; pass <= lloydPasses; pass++) {
            PriorityQueue<ExpansionNode> queue = new PriorityQueue<>();
            for (Kingdom k : kingdoms) {
                queue.add(new ExpansionNode(k.getCapital(), 0.0, k));
            }

            for (MapCell cell : landCells) {
                cell.setKingdom(null);
            }

            int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
            while (!queue.isEmpty()) {
                ExpansionNode curr = queue.poll();
                MapCell cell = curr.cell;
                double cost = curr.cost;
                Kingdom k = curr.kingdom;

                if (cell.getKingdom() != null) continue;
                cell.setKingdom(k);

                for (int[] dir : dirs) {
                    int nx = cell.getX() + dir[0];
                    int ny = cell.getY() + dir[1];
                    if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                        MapCell neighbor = grid.getCell(nx, ny);
                        if (neighbor != null && neighbor.getKingdom() == null && neighbor.getElevation() > waterLevel) {
                            double elevationPenalty = Math.max(0.0, neighbor.getElevation());
                            double stepCost = 1.0 + (elevationPenalty * 5.0); // Cost based on elevation (positive only)
                            queue.add(new ExpansionNode(neighbor, cost + stepCost, k));
                        }
                    }
                }
            }

            if (pass < lloydPasses) {
                // Lloyd relaxation
                long[] sumX = new long[kingdomCount];
                long[] sumY = new long[kingdomCount];
                int[] count = new int[kingdomCount];

                for (MapCell cell : landCells) {
                    Kingdom k = cell.getKingdom();
                    if (k != null) {
                        sumX[k.getId()] += cell.getX();
                        sumY[k.getId()] += cell.getY();
                        count[k.getId()]++;
                    }
                }

                for (int i = 0; i < kingdomCount; i++) {
                    if (count[i] > 0) {
                        int cx = (int) (sumX[i] / count[i]);
                        int cy = (int) (sumY[i] / count[i]);
                        MapCell newCapital = grid.getCell(cx, cy);
                        if (newCapital != null && newCapital.getElevation() > waterLevel) {
                            kingdoms.set(i, new Kingdom(i, kingdoms.get(i).getColorARGB(), newCapital));
                        }
                    }
                }
            }
        }
    }

    private Biome resolveBiome(double e, double t, double r, double waterLevel) {
        if (e < waterLevel - 0.3) return Biome.DEEP_OCEAN;
        if (e < waterLevel - 0.1) return Biome.OCEAN;
        if (e < waterLevel) return Biome.SHALLOW_SEA;
        if (e < waterLevel + 0.05) return Biome.BEACH;

        if (e > waterLevel + 0.6) {
            if (t > 0.5) return Biome.BARE_ROCK;
            return Biome.SNOW_PEAKS;
        }

        if (t > 0.7) {
            if (r < 0.3) return Biome.SCORCHED;
            if (r < 0.5) return Biome.DESERT;
            if (r < 0.8) return Biome.SAVANNA;
            return Biome.TROPICAL_DRY_FOREST;
        } else if (t > 0.4) {
            if (r < 0.3) return Biome.SHRUBLAND;
            if (r < 0.6) return Biome.GRASSLAND;
            if (r < 0.8) return Biome.TEMPERATE_FOREST;
            return Biome.TROPICAL_RAINFOREST;
        } else {
            if (r < 0.5) return Biome.TUNDRA;
            return Biome.TEMPERATE_RAINFOREST;
        }
    }
}

