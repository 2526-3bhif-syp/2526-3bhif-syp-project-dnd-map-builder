package com.mapbuilder.mapbuilder.core.map.generator;

import com.mapbuilder.mapbuilder.core.map.MapGrid;
import com.mapbuilder.mapbuilder.core.map.MapCell;
import java.util.Random;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

public class HydrologyPass implements MapGenerationPass {

    public static final int RIVER_DENSITY_DIVISOR = 64000;
    public static final int MAX_RIVER_ATTEMPTS_MULTIPLIER = 50;
    public static final double MIN_SOURCE_ELEVATION_OFFSET = 0.3;
    public static final double MIN_SOURCE_RAINFALL = 0.3;
    public static final double MIN_RIVER_LENGTH_RATIO = 0.1; 
    public static final double MAX_RIVER_LENGTH_RATIO = 0.1; 
    public static final double RIVER_CARVING_DEPTH = 0.001;
    public static final int MIN_LAKE_AREA = 100;
    public static final double LAKE_AREA_RATIO = 1.0 / 1600.0;

    @Override
    public void execute(MapGrid grid, GenerationParameters params) {
        if (!params.enableRivers() && !params.enableLakes()) return;

        int width = grid.getWidth();
        int height = grid.getHeight();
        int baseRiverCount = (width * height) / RIVER_DENSITY_DIVISOR;
        int targetRiverCount = (int) (baseRiverCount * (params.riverDensityPercent() / 100.0));
        
        Random rand = new Random(params.seed() + 999);
        int successfulRivers = 0;
        int attempts = 0;
        
        int effectiveRiverCount = Math.max(targetRiverCount, (params.enableLakes() ? ((width * height) / RIVER_DENSITY_DIVISOR) : 0));
        int maxAttempts = effectiveRiverCount * MAX_RIVER_ATTEMPTS_MULTIPLIER; 

        while (successfulRivers < effectiveRiverCount && attempts < maxAttempts) {
            attempts++;
            int sx = rand.nextInt(width);
            int sy = rand.nextInt(height);
            MapCell source = grid.getCell(sx, sy);
            
            if (source.getElevation() < params.waterLevel() + MIN_SOURCE_ELEVATION_OFFSET || source.getRainfall() < MIN_SOURCE_RAINFALL || source.isRiver() || source.isLake()) {
                continue;
            }

            if (traceHydrology(grid, source, params.waterLevel(), params.enableRivers(), params.enableLakes(), params.lakeSizePercent(), params.customMinLakeArea(), targetRiverCount > 0 && successfulRivers < targetRiverCount)) {
                successfulRivers++;
            }
        }
    }

    private boolean traceHydrology(MapGrid grid, MapCell start, double waterLevel, boolean enableRivers, boolean enableLakes, double lakeSizePercent, int customMinLakeArea, boolean isRiverAttempt) {
        MapCell current = start;
        List<MapCell> path = new ArrayList<>();
        int maxLen = (int) (grid.getWidth() * grid.getHeight() * MAX_RIVER_LENGTH_RATIO);
        int minRiverLen = Math.max(10, (int) (grid.getWidth() * MIN_RIVER_LENGTH_RATIO));
        int len = 0;
        boolean formedLake = false;

        while (len < maxLen) {
            path.add(current);
            
            if (current.getElevation() <= waterLevel) {
                break;
            }

            MapCell next = null;
            double lowest = current.getElevation();
            MapCell lowestNeighbor = null;
            double absoluteLowest = Double.MAX_VALUE;

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
                if (path.size() < minRiverLen && lowestNeighbor != null && isRiverAttempt) {
                    lowestNeighbor.setElevation(current.getElevation() - RIVER_CARVING_DEPTH);
                    next = lowestNeighbor;
                } else {
                    formedLake = true;
                    break;
                }
            } else if (next.isRiver() || next.isLake()) {
                break;
            }

            current = next;
            len++;
        }

        boolean success = false;
        if (path.size() >= minRiverLen || (!isRiverAttempt && formedLake)) {
            if (formedLake && enableLakes) {
                formLake(grid, current, waterLevel, lakeSizePercent, customMinLakeArea);
                success = true;
            }
            if (enableRivers && isRiverAttempt && path.size() >= minRiverLen) {
                for (MapCell cell : path) {
                    cell.setRiver(true);
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
        int baseArea = Math.max(customMinLakeArea, (int) (grid.getWidth() * grid.getHeight() * LAKE_AREA_RATIO));
        int targetSize = (int) ((baseArea + (int)(Math.random() * baseArea)) * (lakeSizePercent / 100.0));

        PriorityQueue<MapCell> floodQueue = new PriorityQueue<>(Comparator.comparingDouble(MapCell::getElevation));
        Set<MapCell> visited = new HashSet<>();
        List<MapCell> lakeCells = new ArrayList<>();

        floodQueue.add(center);
        visited.add(center);

        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}, {-1,-1}, {-1,1}, {1,-1}, {1,1}};

        while (!floodQueue.isEmpty() && lakeCells.size() < targetSize) {
            MapCell current = floodQueue.poll();
            
            if (current.getElevation() > waterLevel) {
                lakeCells.add(current);
                current.setLake(true);

                for (int[] dir : dirs) {
                    MapCell neighbor = grid.getCell(current.getX() + dir[0], current.getY() + dir[1]);
                    if (neighbor != null && neighbor.getElevation() > waterLevel && !visited.contains(neighbor)) {
                        visited.add(neighbor);
                        floodQueue.add(neighbor);
                    }
                }
            } else {
                break;
            }
        }
    }
}
