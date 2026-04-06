package com.mapbuilder.mapbuilder.core.map;

public enum Biome {
    DEEP_OCEAN(0xFF00008B),
    OCEAN(0xFF0000CD),
    SHALLOW_SEA(0xFF4169E1),
    BEACH(0xFFF4A460),
    SCORCHED(0xFF555555),
    BARE_ROCK(0xFF888888),
    TUNDRA(0xFFAAAAAA),
    SNOW_PEAKS(0xFFFFFFFF),
    SHRUBLAND(0xFF88AA55),
    GRASSLAND(0xFF7CFC00),
    SAVANNA(0xFFBDB76B),
    DESERT(0xFFEDC9AF),
    TEMPERATE_FOREST(0xFF228B22),
    TEMPERATE_RAINFOREST(0xFF006400),
    TROPICAL_DRY_FOREST(0xFF556B2F),
    TROPICAL_RAINFOREST(0xFF004B23);

    private final int colorARGB;

    Biome(int colorARGB) {
        this.colorARGB = colorARGB;
    }

    public int getColorARGB() {
        return colorARGB;
    }
}
