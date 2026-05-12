package com.mapbuilder.mapbuilder.core.map;

import com.mapbuilder.mapbuilder.core.math.FastNoiseLite;
import com.mapbuilder.mapbuilder.core.map.generator.*;
import java.util.ArrayList;
import java.util.List;

public class MapGenerator {
    
    private final List<MapGenerationPass> passes = new ArrayList<>();
    
    private GenerationParameters lastParams = null;
    private MapGrid lastGrid = null;

    public MapGenerator() {
        passes.add(new TerrainPass());
        passes.add(new HydrologyPass());
        passes.add(new KingdomPass());
        passes.add(new POIPass());
    }

    public void generate(MapGrid grid, int seed, int octaves, float scale, double falloff, double waterLevel, double temperatureBias, double rainfallBias,
                         boolean enableRivers, boolean enableLakes, double riverDensityPercent, double lakeSizePercent, int customMinLakeArea,
                         int kingdomCount, int lloydPasses, double dungeonDensity, double ruinCastleDensity, double settlementDensity) {
        
        GenerationParameters params = new GenerationParameters(
            seed, octaves, scale, falloff, waterLevel, temperatureBias, rainfallBias,
            enableRivers, enableLakes, riverDensityPercent, lakeSizePercent, customMinLakeArea,
            kingdomCount, lloydPasses, dungeonDensity, ruinCastleDensity, settlementDensity
        );
        
        int startPass = 0;
        if (lastParams != null && lastGrid == grid) {
            startPass = determineStartingPass(lastParams, params);
        }
        
        for (int i = startPass; i < passes.size(); i++) {
            passes.get(i).execute(grid, params);
        }
        
        lastParams = params;
        lastGrid = grid;
    }

    private int determineStartingPass(GenerationParameters oldP, GenerationParameters newP) {
        // Terrain dependencies
        if (oldP.seed() != newP.seed() ||
            oldP.octaves() != newP.octaves() ||
            Float.compare(oldP.scale(), newP.scale()) != 0 ||
            Double.compare(oldP.falloff(), newP.falloff()) != 0 ||
            Double.compare(oldP.waterLevel(), newP.waterLevel()) != 0 ||
            Double.compare(oldP.temperatureBias(), newP.temperatureBias()) != 0 ||
            Double.compare(oldP.rainfallBias(), newP.rainfallBias()) != 0) {
            return 0;
        }
        
        // Hydrology dependencies. Hydrology carves into terrain elevation, 
        // so we must rerun TerrainPass (0) if hydrology settings change.
        if (oldP.enableRivers() != newP.enableRivers() ||
            oldP.enableLakes() != newP.enableLakes() ||
            Double.compare(oldP.riverDensityPercent(), newP.riverDensityPercent()) != 0 ||
            Double.compare(oldP.lakeSizePercent(), newP.lakeSizePercent()) != 0 ||
            oldP.customMinLakeArea() != newP.customMinLakeArea()) {
            return 0; 
        }
        
        // Kingdom dependencies -> start at KingdomPass (2)
        if (oldP.kingdomCount() != newP.kingdomCount() ||
            oldP.lloydPasses() != newP.lloydPasses()) {
            return 2;
        }
        
        // POI dependencies -> start at POIPass (3)
        if (Double.compare(oldP.dungeonDensity(), newP.dungeonDensity()) != 0 ||
            Double.compare(oldP.ruinCastleDensity(), newP.ruinCastleDensity()) != 0 ||
            Double.compare(oldP.settlementDensity(), newP.settlementDensity()) != 0) {
            return 3;
        }
        
        // If exact same parameters, skip generation
        return passes.size();
    }

    public void generateLOD(MapGrid lodGrid, int seed, int octaves, float scale,
                            double falloff, double waterLevel,
                            double temperatureBias, double rainfallBias,
                            double worldOffsetX, double worldOffsetY,
                            double cellWorldSize,
                            int worldWidth, int worldHeight) {
        int w = lodGrid.getWidth();
        int h = lodGrid.getHeight();

        FastNoiseLite elevationNoise = new FastNoiseLite(seed);
        elevationNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        elevationNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        elevationNoise.SetFractalOctaves(octaves);
        elevationNoise.SetFrequency(scale);

        FastNoiseLite tempNoise = new FastNoiseLite(seed + 1);
        tempNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        tempNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        tempNoise.SetFractalOctaves(octaves);
        tempNoise.SetFrequency(scale);

        FastNoiseLite rainNoise = new FastNoiseLite(seed + 2);
        rainNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        rainNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        rainNoise.SetFractalOctaves(octaves);
        rainNoise.SetFrequency(scale);

        for (int cx = 0; cx < w; cx++) {
            for (int cy = 0; cy < h; cy++) {
                MapCell cell = lodGrid.getCell(cx, cy);
                cell.setRiver(false);
                cell.setLake(false);

                float wx = (float)(worldOffsetX + cx * cellWorldSize);
                float wy = (float)(worldOffsetY + cy * cellWorldSize);

                double eNoise = elevationNoise.GetNoise(wx, wy) * 0.5 + 0.5;
                double e = eNoise;
                double nx = wx / worldWidth;
                double ny = wy / worldHeight;
                double fcx = nx * 2 - 1;
                double fcy = ny * 2 - 1;
                double d = Math.sqrt(fcx * fcx + fcy * fcy);
                
                double baseFalloff = Math.pow(Math.max(0, d - 0.1), 2.0) * 2.5;
                
                if (falloff >= 0) {
                    e = e - baseFalloff * (1.0 - falloff * 0.4);
                } else {
                    double absF = Math.abs(falloff);
                    e = (e - 0.5) * (1.0 + absF * 1.5) + 0.5;
                    e = e - baseFalloff - absF * 0.2;
                }
                cell.setElevation(e);

                double t = tempNoise.GetNoise(wx, wy) * 0.5 + 0.5;
                double lat = Math.cos((ny * 2 - 1) * Math.PI / 2.0);
                
                double maxExpectedE = 1.0;
                double elevationRange = Math.max(0.1, maxExpectedE - waterLevel);
                double elevationPenalty = 0.0;
                if (e > waterLevel) {
                    elevationPenalty = ((e - waterLevel) / elevationRange) * 0.5;
                }
                
                t = t + lat * 0.5 - elevationPenalty + temperatureBias;
                if (Math.abs(e - waterLevel) < 0.1) t = (t + 0.5) / 2.0;
                t = Math.clamp(t, 0.0, 1.0);
                cell.setTemperature(t);

                double r = rainNoise.GetNoise(wx, wy) * 0.5 + 0.5;
                r = r + (1.0 - t) * 0.3 + rainfallBias;
                if (cx > 0) {
                    float prevWx = (float)(worldOffsetX + (cx - 1) * cellWorldSize);
                    double prevNoise = elevationNoise.GetNoise(prevWx, wy) * 0.5 + 0.5;
                    if (eNoise > prevNoise) r += 0.2;
                    else if (eNoise < prevNoise) r -= 0.2;
                }
                r = Math.clamp(r, 0.0, 1.0);
                cell.setRainfall(r);

                double landE = e > waterLevel ? (e - waterLevel) / Math.max(0.01, 1.0 - waterLevel) : 0.0;
                Biome biome = resolveBiome(e, landE, t, r, waterLevel);
                cell.setBiome(biome);
                cell.setMixedColorARGB(biome.getColorARGB());
            }
        }
    }

    private Biome resolveBiome(double e, double landE, double t, double r, double waterLevel) {
        if (e < waterLevel - 0.3) return Biome.DEEP_OCEAN;
        if (e < waterLevel - 0.1) return Biome.OCEAN;
        if (e < waterLevel) return Biome.SHALLOW_SEA;
        if (landE < 0.05) return Biome.BEACH;

        if (landE > 0.6) {
            if (t > 0.5) return Biome.BARE_ROCK;
            return Biome.SNOW_PEAKS;
        }

        if (t > 0.7) {
            if (r < 0.3) return Biome.SCORCHED;
            if (r < 0.5) return Biome.DESERT;
            if (r < 0.8) return Biome.SAVANNA;
            return Biome.TROPICAL_DRY_FOREST;
        } else if (t > 0.4) {
            if (r < 0.3) return Biome.SHRUBLAND;
            if (r < 0.6) return Biome.GRASSLAND;
            if (r < 0.8) return Biome.TEMPERATE_FOREST;
            return Biome.TROPICAL_RAINFOREST;
        } else {
            if (r < 0.5) return Biome.TUNDRA;
            return Biome.TEMPERATE_RAINFOREST;
        }
    }
}
