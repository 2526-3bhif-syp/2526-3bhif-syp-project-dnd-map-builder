package com.mapbuilder.mapbuilder.core.map.generator;
import com.mapbuilder.mapbuilder.core.map.MapGrid;

public interface MapGenerationPass {
    void execute(MapGrid grid, GenerationParameters params);
}
