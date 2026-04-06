package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.map.MapGenerator;
import com.mapbuilder.mapbuilder.core.map.MapGrid;

public class MainModel {
    private MapGrid currentGrid;
    private final MapGenerator generator;

    public MainModel() {
        this.currentGrid = new MapGrid(800, 800);
        this.generator = new MapGenerator();
    }

    public void generateMap(int seed, int size, int octaves, float scale, double falloff, double waterLevel, double tempBias, double rainBias, int riverCount) {
        if (currentGrid.getWidth() != size || currentGrid.getHeight() != size) {
            currentGrid = new MapGrid(size, size);
        }
        generator.generate(currentGrid, seed, octaves, scale, falloff, waterLevel, tempBias, rainBias, riverCount);
    }

    public MapGrid getCurrentGrid() {
        return currentGrid;
    }
}
