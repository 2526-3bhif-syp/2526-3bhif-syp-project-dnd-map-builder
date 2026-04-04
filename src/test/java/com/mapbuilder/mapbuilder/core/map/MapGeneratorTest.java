package com.mapbuilder.mapbuilder.core.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapGeneratorTest {

    @Test
    void testMapGenerationPopulatesCells() {
        MapGrid grid = new MapGrid(10, 10);
        MapGenerator generator = new MapGenerator();
        
        generator.generate(grid, 12345, 5, 0.01f, 0.5, 0.3, 0.0, 0.0);
        
        MapCell centerCell = grid.getCell(5, 5);
        assertNotNull(centerCell.getBiome(), "Biome should not be null after generation");
        assertTrue(centerCell.getElevation() >= -1.0 && centerCell.getElevation() <= 1.0, "Elevation should be within range");
        assertTrue(centerCell.getTemperature() >= -2.0 && centerCell.getTemperature() <= 2.0, "Temperature should be within range");
        assertTrue(centerCell.getRainfall() >= -2.0 && centerCell.getRainfall() <= 2.0, "Rainfall should be within range");
        assertNotEquals(0, centerCell.getMixedColorARGB(), "Mixed color should be populated");
    }
}
