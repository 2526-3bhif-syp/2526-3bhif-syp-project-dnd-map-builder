package com.mapbuilder.mapbuilder.core.map;

/**
 * Describes the world-space region that a LOD grid covers and the resolution
 * at which it was (or should be) generated.
 */
public final class LodRegion {
    public final int wx0, wy0, wx1, wy1;
    public final LodLevel lod;
    /** Width of the LOD MapGrid = (wx1 - wx0) * lod.multiplier */
    public final int lodGridW;
    /** Height of the LOD MapGrid = (wy1 - wy0) * lod.multiplier */
    public final int lodGridH;
    /** World units per LOD cell = 1.0 / lod.multiplier */
    public final double cellWorldSize;

    public LodRegion(int wx0, int wy0, int wx1, int wy1, LodLevel lod) {
        this.wx0 = wx0; this.wy0 = wy0;
        this.wx1 = wx1; this.wy1 = wy1;
        this.lod = lod;
        this.lodGridW    = (wx1 - wx0) * lod.multiplier;
        this.lodGridH    = (wy1 - wy0) * lod.multiplier;
        this.cellWorldSize = 1.0 / lod.multiplier;
    }

    public LodGridCache.Key cacheKey() {
        return new LodGridCache.Key(wx0, wy0, wx1, wy1, lod);
    }
}
