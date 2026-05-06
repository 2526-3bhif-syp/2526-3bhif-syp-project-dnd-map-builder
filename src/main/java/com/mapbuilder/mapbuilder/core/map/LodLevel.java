package com.mapbuilder.mapbuilder.core.map;

public enum LodLevel {
    LOD_0(1,  0.0,  4.0),
    LOD_1(2,  4.0, 12.0),
    LOD_2(4, 12.0, 64.0);

    /** How many LOD cells represent one base-grid cell in each axis. */
    public final int multiplier;
    public final double minPPC;
    public final double maxPPC;

    LodLevel(int multiplier, double minPPC, double maxPPC) {
        this.multiplier = multiplier;
        this.minPPC     = minPPC;
        this.maxPPC     = maxPPC;
    }

    public static LodLevel forZoom(double pixelsPerCell) {
        if (pixelsPerCell >= LOD_2.minPPC) return LOD_2;
        if (pixelsPerCell >= LOD_1.minPPC) return LOD_1;
        return LOD_0;
    }
}
