package com.mapbuilder.mapbuilder.core.map.generator;

import com.mapbuilder.mapbuilder.core.map.MapGrid;
import com.mapbuilder.mapbuilder.core.map.MapCell;
import com.mapbuilder.mapbuilder.core.map.Biome;
import com.mapbuilder.mapbuilder.core.math.FastNoiseLite;

public class TerrainPass implements MapGenerationPass {

    @Override
    public void execute(MapGrid grid, GenerationParameters params) {
        int width = grid.getWidth();
        int height = grid.getHeight();

        FastNoiseLite elevationNoise = new FastNoiseLite(params.seed());
        elevationNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        elevationNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        elevationNoise.SetFractalOctaves(params.octaves());
        elevationNoise.SetFrequency(params.scale());

        FastNoiseLite tempNoise = new FastNoiseLite(params.seed() + 1);
        tempNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        tempNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        tempNoise.SetFractalOctaves(params.octaves());
        tempNoise.SetFrequency(params.scale());

        FastNoiseLite rainNoise = new FastNoiseLite(params.seed() + 2);
        rainNoise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        rainNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        rainNoise.SetFractalOctaves(params.octaves());
        rainNoise.SetFrequency(params.scale());

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                MapCell cell = grid.getCell(x, y);

                cell.setRiver(false);
                cell.setLake(false);

                double nx = (double) x / width;
                double ny = (double) y / height;

                double eNoise = elevationNoise.GetNoise((float) x, (float) y) * 0.5 + 0.5;
                double e = eNoise; 
                double cx = nx * 2 - 1;
                double cy = ny * 2 - 1;
                double d = Math.sqrt(cx * cx + cy * cy);
                
                double baseFalloff = Math.pow(Math.max(0, d - 0.1), 2.0) * 2.5;
                
                if (params.falloff() >= 0) {
                    e = e - baseFalloff * (1.0 - params.falloff() * 0.4);
                } else {
                    double absF = Math.abs(params.falloff());
                    e = (e - 0.5) * (1.0 + absF * 1.5) + 0.5;
                    e = e - baseFalloff - absF * 0.2;
                }
                cell.setElevation(e);

                double t = tempNoise.GetNoise((float) x, (float) y) * 0.5 + 0.5;
                double lat = Math.cos((ny * 2 - 1) * Math.PI / 2.0);
                
                double maxExpectedE = 1.0;
                double elevationRange = Math.max(0.1, maxExpectedE - params.waterLevel());
                double elevationPenalty = 0.0;
                if (e > params.waterLevel()) {
                    elevationPenalty = ((e - params.waterLevel()) / elevationRange) * 0.5;
                }
                
                t = t + lat * 0.5 - elevationPenalty + params.temperatureBias();
                if (Math.abs(e - params.waterLevel()) < 0.1) {
                    t = (t + 0.5) / 2.0; 
                }
                t = Math.clamp(t, 0.0, 1.0);
                cell.setTemperature(t);

                double r = rainNoise.GetNoise((float) x, (float) y) * 0.5 + 0.5;
                r = r + (1.0 - t) * 0.3 + params.rainfallBias();

                if (x > 0) {
                    double prevNoise = elevationNoise.GetNoise((float) (x - 1), (float) y) * 0.5 + 0.5;
                    if (eNoise > prevNoise) {
                        r += 0.2;
                    } else if (eNoise < prevNoise) {
                        r -= 0.2;
                    }
                }
                r = Math.clamp(r, 0.0, 1.0);
                cell.setRainfall(r);

                double landE = e > params.waterLevel() ? (e - params.waterLevel()) / Math.max(0.01, 1.0 - params.waterLevel()) : 0.0;
                Biome biome = resolveBiome(e, landE, t, r, params.waterLevel());
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
