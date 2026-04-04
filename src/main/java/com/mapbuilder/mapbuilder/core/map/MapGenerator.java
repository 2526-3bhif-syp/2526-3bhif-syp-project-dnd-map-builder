package com.mapbuilder.mapbuilder.core.map;

import com.mapbuilder.mapbuilder.core.math.FastNoiseLite;

public class MapGenerator {

    public void generate(MapGrid grid, int seed, int octaves, float scale) {
        int width = grid.getWidth();
        int height = grid.getHeight();

        double waterLevel = 0.25;
        double temperatureBias = 0.0;
        double rainfallBias = 0.0;

        FastNoiseLite elevationNoise = new FastNoiseLite(seed);
        elevationNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        elevationNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        elevationNoise.SetFractalOctaves(octaves);
        elevationNoise.SetFrequency(scale);

        FastNoiseLite tempNoise = new FastNoiseLite(seed + 1);
        tempNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        tempNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        tempNoise.SetFractalOctaves(octaves);
        tempNoise.SetFrequency(scale);

        FastNoiseLite rainNoise = new FastNoiseLite(seed + 2);
        rainNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
        rainNoise.SetFractalType(FastNoiseLite.FractalType.FBm);
        rainNoise.SetFractalOctaves(octaves);
        rainNoise.SetFrequency(scale);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                MapCell cell = grid.getCell(x, y);

                double nx = (double) x / width;
                double ny = (double) y / height;

                // Elevation
                double e = elevationNoise.GetNoise((float) x, (float) y) * 0.6 + 0.5; 
                // Island falloff
                double cx = nx * 2 - 1;
                double cy = ny * 2 - 1;
                // Squared distance gives a broader center landmass before dropping off
                double d2 = cx * cx + cy * cy;
                e = e - d2 * 0.3;
                cell.setElevation(e);

                // Temperature
                double t = tempNoise.GetNoise((float) x, (float) y) * 0.5 + 0.5;
                // Latitude gradient
                double lat = 1.0 - Math.abs(ny * 2 - 1);
                t = t + lat * 0.5 - Math.max(0, e - waterLevel) * 0.5 + temperatureBias;
                // Ocean temp moderation
                if (Math.abs(e - waterLevel) < 0.1) {
                    t = (t + 0.5) / 2.0; 
                }
                cell.setTemperature(t);

                // Rainfall
                double r = rainNoise.GetNoise((float) x, (float) y) * 0.5 + 0.5;
                // Inverse temp bias
                r = r + (1.0 - t) * 0.3 + rainfallBias;

                // Wind logic (rain shadow)
                if (x > 0) {
                    double prevE = grid.getCell(x - 1, y).getElevation();
                    if (e > prevE) {
                        r += 0.2;
                    } else if (e < prevE) {
                        r -= 0.2;
                    }
                }
                cell.setRainfall(r);

                // Resolve Biome
                Biome biome = resolveBiome(e, t, r, waterLevel);
                cell.setBiome(biome);

                // Mixed color
                cell.setMixedColorARGB(biome.getColorARGB());
            }
        }
    }

    private Biome resolveBiome(double e, double t, double r, double waterLevel) {
        if (e < waterLevel - 0.3) return Biome.DEEP_OCEAN;
        if (e < waterLevel - 0.1) return Biome.OCEAN;
        if (e < waterLevel) return Biome.SHALLOW_SEA;
        if (e < waterLevel + 0.05) return Biome.BEACH;

        if (e > waterLevel + 0.6) {
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
