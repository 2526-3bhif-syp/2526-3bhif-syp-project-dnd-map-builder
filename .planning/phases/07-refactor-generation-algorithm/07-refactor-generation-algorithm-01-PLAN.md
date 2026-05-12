# 07-01-PLAN: Refactor Generation Algorithm

## Current State
One generate function that is a massive 700 line monster doing all steps sequentially.

## Desired State
Every important generation step is defined as a pass. Split up code into multiple files.

## Steps
1. Create `MapGenerationPass` interface and `GenerationParameters` record.
2. Extract terrain generation to `TerrainPass`.
3. Extract river and lake generation to `HydrologyPass`.
4. Extract kingdom generation to `KingdomPass`.
5. Extract POI generation to `POIPass`.
6. Update `MapGenerator` to run passes in sequence.
