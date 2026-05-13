package com.mapbuilder.mapbuilder.core.map.generator;

import com.mapbuilder.mapbuilder.core.map.MapGrid;
import com.mapbuilder.mapbuilder.core.map.PointOfInterest;
import com.mapbuilder.mapbuilder.core.map.PointOfInterestGenerator;
import java.util.List;

public class POIPass implements MapGenerationPass {

    @Override
    public void execute(MapGrid grid, GenerationParameters params) {
        List<PointOfInterest> pois = PointOfInterestGenerator.generatePointsOfInterest(
            grid, 
            params.seed(), 
            params.dungeonDensity(), 
            params.ruinCastleDensity(), 
            params.settlementDensity()
        );
        grid.setPointsOfInterest(pois);
    }
}
