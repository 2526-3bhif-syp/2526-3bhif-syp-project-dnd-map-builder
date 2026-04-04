package com.mapbuilder.mapbuilder.core.map;

public class MapGrid {
    private final int width;
    private final int height;
    private final MapCell[][] grid;

    public MapGrid(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new MapCell[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = new MapCell(x, y);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public MapCell getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return null;
        }
        return grid[x][y];
    }
}
