package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.map.MapGenerator;
import com.mapbuilder.mapbuilder.core.map.MapGrid;

public class MainModel {
    private MapGrid currentGrid;
    private final MapGenerator generator;

    public MainModel() {
        this.currentGrid = new MapGrid(500, 500);
        this.generator = new MapGenerator();
    }

    public void generateMap(int seed, double waterLevel, double tempBias, double rainBias) {
        generator.generate(currentGrid, seed, waterLevel, tempBias, rainBias);
    }

    public MapGrid getCurrentGrid() {
        return currentGrid;
    }
}
