# Phase 02: Core Map Generation - Context

**Status:** Ready for planning

<domain>
## Phase Boundary

Implement the map generation engine from scratch utilizing a custom seedable RNG, Perlin noise, and Fractional Brownian Motion (fBm). Generate 3 independent fields (elevation, temperature, rainfall) and resolve them into 16 distinct biomes. The architecture must focus on the backend generation strategy, producing a decoupled data structure ready for later graphical rendering, add-ons (like POIs), and modifications.

</domain>

<decisions>
## Implementation Decisions

### Core Data Structure (Prepared for Future Add-ons)
- **D-01 (Decoupled Grid):** Implement a backend `MapGrid` composed of `MapCell` objects. Each cell holds properties: x, y, elevation, temperature, rainfall, and a resolved `Biome` reference.
- **D-02 (Future-Proofing):** The `MapGrid` must be completely independent of JavaFX. This allows later additions (like POIs, province borders, structures) to attach data logically to cells before it's rendered visually.

### Map Generation Algorithm (Self-Contained Math)
- **D-03 (Noise Primitives):** Implement a seedable PRNG and standard 2D Perlin noise. Combine Perlin noise into Fractional Brownian Motion (fBm) using configurable octaves, lacunarity, and gain.
- **D-04 (Elevation Field):** 
  `Elevation = fBm_noise(x, y) + Island_Falloff_Mask(x, y)`
  *The mask forces the edges of the map to drop off into the ocean, creating a centralized landmass.*
- **D-05 (Temperature Field):** 
  `Temperature = fBm_noise(x, y) + Latitude_Gradient(y) - Altitude_Cooling(Elevation)`
  *Poles (top/bottom) are colder, equator (middle) is warmer. High elevation drastically reduces temperature.*
- **D-06 (Rainfall Field):** 
  `Rainfall = fBm_noise(x, y) + Inverse_Temperature_Bias(Temperature)`
  *Modifies rainfall chances based on temperature to create realistic arid zones and rainforests.*

### Biome Resolution
- **D-07 (16 Distinct Biomes):** Map the computed Elevation, Temperature, and Rainfall values to exactly **16 distinct Biomes** defined as constants/enums (e.g., Deep Ocean, Ocean, Shallow Sea, Beach, Scorched, Bare Rock, Tundra, Snow Peaks, Shrubland, Grassland, Savanna, Desert, Temperate Forest, Temperate Rainforest, Tropical Dry Forest, Tropical Rainforest).
- **D-08 (Whittaker Diagram Logic):** 
  1. If Elevation < WaterLevel -> Ocean/Sea variants.
  2. If Elevation > MountainLevel -> Bare Rock/Snow variants based on temp.
  3. Otherwise, use Temperature and Rainfall 2D lookup to determine the specific land biome.

### Rendering & Interaction
- **D-09 (CPU Translation Layer):** Translate the backend `MapGrid` data structure into visual colors and iterate via a JavaFX `PixelWriter` directly to a `WritableImage` on the Canvas.
- **D-10 (Debounced Binding):** Bind the MVP View's sliders (Size, Water %, etc.) and seed inputs directly to the generator with a debounce, triggering real-time preview updates on the canvas.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Specs
- `.planning/REQUIREMENTS.md` — Functional Requirement 1 & 2 (Random & Parameterized Generation).
- `.planning/phases/01-foundation-application-shell/01-CONTEXT.md` — MVP architecture constraints and Canvas references.

*(Note: No external implementation references or scripts are required. The math and biome logic defined in the decisions above are the sole source of truth).*
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- The JavaFX `Canvas` initialized in Phase 1 will be the rendering target for the translated `MapGrid`.

### Integration Points
- Map generation engine and `MapGrid` state reside in the `Model` layer of the MVP architecture.
- The `Presenter` listens to parameter changes, invokes the `Model` generator, translates the grid to an image, and pushes the `WritableImage` to the `View`.

</code_context>

<specifics>
## Specific Ideas
- The backend grid calculation loops must be highly performant. Avoid creating object instances inside the tight per-pixel `x/y` loops where possible; instead, initialize cells efficiently.
- `Biome` instances should define their base color representation as constant properties.
- Parameters like "Water Percentage" should dynamically shift the `WaterLevel` threshold.
- Parameters like "Desert/Snow Percentage" should dynamically shift the temperature and rainfall baseline biases.

</specifics>

<deferred>
## Deferred Ideas

- Placing actual interactable points of interest (POIs), cities, or structural elements (Phase 3).
- Adding custom Hex/Square Grid overlay features (Phase 4).
- Advanced export functions (Phase 5).

</deferred>
