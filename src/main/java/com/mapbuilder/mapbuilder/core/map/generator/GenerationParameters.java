package com.mapbuilder.mapbuilder.core.map.generator;

public record GenerationParameters(
    int seed,
    int octaves,
    float scale,
    double falloff,
    double waterLevel,
    double temperatureBias,
    double rainfallBias,
    boolean enableRivers,
    boolean enableLakes,
    double riverDensityPercent,
    double lakeSizePercent,
    int customMinLakeArea,
    int kingdomCount,
    int lloydPasses,
    double dungeonDensity,
    double landmarkDensity,
    double settlementDensity
) {}
