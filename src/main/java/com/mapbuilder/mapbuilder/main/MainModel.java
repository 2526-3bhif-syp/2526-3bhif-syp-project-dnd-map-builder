package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.map.MapGenerator;
import com.mapbuilder.mapbuilder.core.map.MapGrid;
import com.mapbuilder.mapbuilder.core.map.MapLabel;

import java.util.ArrayList;
import java.util.List;

public class MainModel {
    private MapGrid currentGrid;
    private final MapGenerator generator;
    private final List<MapLabel> labels = new ArrayList<>();

    public MainModel() {
        this.currentGrid = new MapGrid(800, 800);
        this.generator = new MapGenerator();
    }

    public void generateMap(int seed, int size, int octaves, float scale, double falloff, double waterLevel, double tempBias, double rainBias,
                            boolean enableRivers, boolean enableLakes, double riverDensity, double lakeSize, int minLakeArea, int kingdomCount, int lloydPasses,
                            double dungeonDensity, double landmarkDensity, double settlementDensity) {
        if (currentGrid.getWidth() != size || currentGrid.getHeight() != size) {
            double scaleRatioX = (double) size / currentGrid.getWidth();
            double scaleRatioY = (double) size / currentGrid.getHeight();
            for (MapLabel label : labels) {
                label.setX(label.getX() * scaleRatioX);
                label.setY(label.getY() * scaleRatioY);
            }
            currentGrid = new MapGrid(size, size);
        }
        generator.generate(currentGrid, seed, octaves, scale, falloff, waterLevel, tempBias, rainBias,
                           enableRivers, enableLakes, riverDensity, lakeSize, minLakeArea, kingdomCount, lloydPasses,
                           dungeonDensity, landmarkDensity, settlementDensity);
    }

    public MapGrid getCurrentGrid() {
        return currentGrid;
    }

    public List<MapLabel> getLabels() {
        return labels;
    }

    public void addLabel(MapLabel label) {
        labels.add(label);
    }

    public void removeLabel(MapLabel label) {
        labels.remove(label);
    }
}

