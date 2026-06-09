package com.mapbuilder.mapbuilder.core.map.generator;

import com.mapbuilder.mapbuilder.core.map.MapGrid;
import com.mapbuilder.mapbuilder.core.map.MapCell;
import com.mapbuilder.mapbuilder.core.map.Kingdom;
import java.util.Random;
import java.util.PriorityQueue;
import java.util.ArrayList;
import java.util.List;

public class KingdomPass implements MapGenerationPass {

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

    @Override
    public void execute(MapGrid grid, GenerationParameters params) {
        if (params.kingdomCount() <= 0) {
            grid.setKingdoms(new ArrayList<>());
            for (int x = 0; x < grid.getWidth(); x++)
                for (int y = 0; y < grid.getHeight(); y++)
                    grid.getCell(x, y).setKingdom(null);
            return;
        }

        int width = grid.getWidth();
        int height = grid.getHeight();
        Random rand = new Random(params.seed() + 888);

        List<Kingdom> kingdoms = new ArrayList<>();
        List<MapCell> landCells = new ArrayList<>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                MapCell cell = grid.getCell(x, y);
                cell.setKingdom(null);
                if (cell.getElevation() > params.waterLevel()) {
                    landCells.add(cell);
                }
            }
        }

        if (landCells.isEmpty()) return;

        for (int i = 0; i < params.kingdomCount(); i++) {
            MapCell capital = landCells.get(rand.nextInt(landCells.size()));
            int color = 0xFF000000 | rand.nextInt(0xFFFFFF);
            kingdoms.add(new Kingdom(i, color, capital));
        }

        int passes = Math.min(5, params.lloydPasses());
        for (int pass = 0; pass <= passes; pass++) {
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
                        if (neighbor != null && neighbor.getKingdom() == null && neighbor.getElevation() > params.waterLevel()) {
                            double elevationPenalty = Math.max(0.0, neighbor.getElevation());
                            double stepCost = 1.0 + (elevationPenalty * 5.0);
                            queue.add(new ExpansionNode(neighbor, cost + stepCost, k));
                        }
                    }
                }
            }

            if (pass < passes) {
                long[] sumX = new long[params.kingdomCount()];
                long[] sumY = new long[params.kingdomCount()];
                int[] count = new int[params.kingdomCount()];

                for (MapCell cell : landCells) {
                    Kingdom k = cell.getKingdom();
                    if (k != null) {
                        sumX[k.getId()] += cell.getX();
                        sumY[k.getId()] += cell.getY();
                        count[k.getId()]++;
                    }
                }

                for (int i = 0; i < params.kingdomCount(); i++) {
                    if (count[i] > 0) {
                        int cx = (int) (sumX[i] / count[i]);
                        int cy = (int) (sumY[i] / count[i]);
                        MapCell newCapital = grid.getCell(cx, cy);
                        if (newCapital != null && newCapital.getElevation() > params.waterLevel()) {
                            kingdoms.set(i, new Kingdom(i, kingdoms.get(i).getColorARGB(), newCapital));
                        }
                    }
                }
            }
        }
        grid.setKingdoms(kingdoms);
    }
}
