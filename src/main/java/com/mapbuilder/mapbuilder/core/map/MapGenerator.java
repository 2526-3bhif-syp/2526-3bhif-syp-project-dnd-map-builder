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

    public void generate(MapGrid grid, int seed, int octaves, float scale, double falloff, double waterLevel, double temperatureBias, double rainfallBias) {
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

                // Elevation
                double e = elevationNoise.GetNoise((float) x, (float) y) * 0.5 + 0.5; 
                // Island falloff
                double cx = nx * 2 - 1;
                double cy = ny * 2 - 1;
                double d = Math.sqrt(cx * cx + cy * cy);
                // Squaring the distance creates a non-linear falloff (flatter in center, steeper at edges)
                // This keeps the center solid and connected, and pushes the water to the edges.
                e = e - Math.pow(d, 2.0) * (1.0 + falloff);
                cell.setElevation(e);

                // Temperature
                double t = tempNoise.GetNoise((float) x, (float) y) * 0.5 + 0.5;
                // Latitude gradient
                double lat = 1.0 - Math.abs(ny * 2 - 1);
                t = t + lat * 0.5 - Math.max(0, e - waterLevel) * 0.5 + temperatureBias;
                // Ocean temp moderation
                if (Math.abs(e - waterLevel) < 0.1) {
                    t = (t + 0.5) / 2.0; 
                }
                cell.setTemperature(t);

                // Rainfall
                double r = rainNoise.GetNoise((float) x, (float) y) * 0.5 + 0.5;
                // Inverse temp bias
                r = r + (1.0 - t) * 0.3 + rainfallBias;

                // Wind logic (rain shadow)
                if (x > 0) {
                    double prevE = grid.getCell(x - 1, y).getElevation();
                    if (e > prevE) {
                        r += 0.2;
                    } else if (e < prevE) {
                        r -= 0.2;
                    }
                }
                cell.setRainfall(r);

                // Resolve Biome
                Biome biome = resolveBiome(e, t, r, waterLevel);
                cell.setBiome(biome);

                // Mixed color
                cell.setMixedColorARGB(biome.getColorARGB());
            }
        }

        int riverCount = (width * height) / 64000;
        generateRivers(grid, seed, riverCount, waterLevel);
    }

    private void generateRivers(MapGrid grid, int seed, int targetRiverCount, double waterLevel) {
        int width = grid.getWidth();
        int height = grid.getHeight();
        Random rand = new Random(seed + 999);
        
        int successfulRivers = 0;
        int attempts = 0;
        int maxAttempts = targetRiverCount * 50; // Prevent infinite loops if map is entirely ocean/dry

        while (successfulRivers < targetRiverCount && attempts < maxAttempts) {
            attempts++;
            // Find a source
            int sx = rand.nextInt(width);
            int sy = rand.nextInt(height);
            MapCell source = grid.getCell(sx, sy);
            
            // Source must be high elevation and relatively wet
            if (source.getElevation() < waterLevel + 0.3 || source.getRainfall() < 0.3 || source.isRiver() || source.isLake()) {
                continue;
            }

            if (traceRiver(grid, source, waterLevel)) {
                successfulRivers++;
            }
        }
    }

    private boolean traceRiver(MapGrid grid, MapCell start, double waterLevel) {
        MapCell current = start;
        List<MapCell> path = new ArrayList<>();
        int maxLen = grid.getWidth() * grid.getHeight() / 10; // Prevent infinite loops
        int minRiverLen = Math.max(10, grid.getWidth() / 10); // Minimum river length: 10% of map width
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
                if (path.size() < minRiverLen && lowestNeighbor != null) {
                    lowestNeighbor.setElevation(current.getElevation() - 0.001); // Carve down
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

        // Apply river flags and thickness only if it's long enough
        if (path.size() >= minRiverLen) {
            if (formedLake) {
                formLake(grid, current, path, waterLevel);
            }
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
            return true;
        }
        return false;
    }

    private void formLake(MapGrid grid, MapCell center, List<MapCell> riverPath, double waterLevel) {
        // Organic lake generation using elevation-based flooding
        int baseArea = Math.max(100, (grid.getWidth() * grid.getHeight()) / 1600); // Scale lake size based on map size
        int targetSize = baseArea + (int)(Math.random() * baseArea); // Randomize area somewhat

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
