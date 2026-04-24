package com.mapbuilder.mapbuilder.core.map;

import java.util.ArrayList;
import java.util.List;

public class MapGrid {
    private final int width;
    private final int height;
    private final MapCell[][] grid;
    private List<PointOfInterest> pois;

    public MapGrid(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new MapCell[width][height];
        this.pois = new ArrayList<>();

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

    /**
     * Get the list of all Points of Interest on this map.
     * Returns a mutable list that can be iterated over by UI components.
     */
    public List<PointOfInterest> getPointsOfInterest() {
        return pois;
    }

    /**
     * Replace the entire POI list. Used for map regeneration.
     */
    public void setPointsOfInterest(List<PointOfInterest> newPois) {
        this.pois = newPois != null ? newPois : new ArrayList<>();
    }

    /**
     * Add a POI to the map.
     */
    public void addPointOfInterest(PointOfInterest poi) {
        if (poi != null) {
            pois.add(poi);
        }
    }

    /**
     * Remove a POI from the map by ID.
     */
    public void removePointOfInterest(int poiId) {
        pois.removeIf(poi -> poi.getId() == poiId);
    }

    /**
     * Get a POI from the map by ID.
     */
    public PointOfInterest getPointOfInterestById(int id) {
        for (PointOfInterest poi : pois) {
            if (poi.getId() == id) {
                return poi;
            }
        }
        return null;
    }
}
