# Phase 02: Core Map Generation - Research

**Researched:** 2026-04-04
**Domain:** Java Map Generation, Procedural Noise (Perlin/fBm), JavaFX Rendering Performance
**Confidence:** HIGH

## Summary

The core of this phase is translating mathematical noise into a structured, decoupled data model (`MapGrid`) that represents geographic and climatological data, then rendering that model onto a JavaFX `Canvas`. The major challenge lies in performance—calculating multiple layers of noise for millions of cells on the fly—and preventing the heavy computations from locking up the UI thread. 

**Primary recommendation:** Use an optimized, single-file noise library like FastNoiseLite for generating Perlin/fBm to guarantee mathematical correctness and performance, run map generation asynchronously via JavaFX `Task`, and use bulk pixel writing (`PixelWriter.setPixels`) to achieve real-time rendering when parameters change.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01 (Decoupled Grid):** Implement a backend `MapGrid` composed of `MapCell` objects. Each cell holds properties: x, y, elevation, temperature, rainfall, and a resolved `Biome` reference.
- **D-02 (Future-Proofing):** The `MapGrid` must be completely independent of JavaFX. This allows later additions (like POIs, province borders, structures) to attach data logically to cells before it's rendered visually.
- **D-03 (Noise Primitives):** Implement a seedable PRNG and standard 2D Perlin noise. Combine Perlin noise into Fractional Brownian Motion (fBm) using configurable octaves, lacunarity, and gain.
- **D-04 (Elevation Field):** `Elevation = fBm_noise(x, y) + Island_Falloff_Mask(x, y)`
- **D-05 (Temperature Field):** `Temperature = fBm_noise(x, y) + Latitude_Gradient(y) - Altitude_Cooling(Elevation)`
- **D-06 (Rainfall Field):** `Rainfall = fBm_noise(x, y) + Inverse_Temperature_Bias(Temperature)`
- **D-07 (16 Distinct Biomes):** Map the computed Elevation, Temperature, and Rainfall values to exactly **16 distinct Biomes** defined as constants/enums.
- **D-08 (Whittaker Diagram Logic):** 1. If Elevation < WaterLevel -> Ocean/Sea variants. 2. If Elevation > MountainLevel -> Bare Rock/Snow variants based on temp. 3. Otherwise, use Temperature and Rainfall 2D lookup to determine the specific land biome.
- **D-09 (CPU Translation Layer):** Translate the backend `MapGrid` data structure into visual colors and iterate via a JavaFX `PixelWriter` directly to a `WritableImage` on the Canvas.
- **D-10 (Debounced Binding):** Bind the MVP View's sliders (Size, Water %, etc.) and seed inputs directly to the generator with a debounce, triggering real-time preview updates on the canvas.

### the agent's Discretion
None explicitly declared in this phase.

### Deferred Ideas (OUT OF SCOPE)
- Placing actual interactable points of interest (POIs), cities, or structural elements (Phase 3).
- Adding custom Hex/Square Grid overlay features (Phase 4).
- Advanced export functions (Phase 5).
</user_constraints>

## Standard Stack

### Core
| Library / API | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JavaFX `Task` / `Service` | 21+ | Background execution | Required to keep the UI responsive while generating the 2D grid. |
| `FastNoiseLite` (Java port) | 1.1.0 | Procedural Noise | Single-file drop-in that highly optimizes 2D Perlin noise and built-in fBm parameters. |
| JavaFX `PauseTransition` | 21+ | UI Debouncing | Built-in native way to delay action triggers (like slider movements). |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `FastNoiseLite` | Hand-rolled Perlin Noise class | **Hand-rolling:** Fulfills "Implement..." strictly, but is generally 2-5x slower unless heavily optimized. FastNoiseLite is open-source, purely mathematical, and has no external dependencies. |
| `PixelWriter.setPixels` | `PixelWriter.setColor` in loop | **`setColor`:** Is significantly slower for large canvases because it executes bounds checking and format translation per pixel. `setPixels` with a pre-filled `int[]` array writes memory in a single block. |

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/mapbuilder/mapbuilder/
├── core/
│   ├── map/             # Backend decoupled data model
│   │   ├── MapGrid.java
│   │   ├── MapCell.java
│   │   ├── Biome.java   # Enum representing the 16 biomes
│   │   └── MapGenerator.java
│   └── math/            # Noise generation
│       ├── FastNoiseLite.java (or CustomPerlinNoise.java)
│       └── MathUtil.java
├── presentation/        # MVP structure
│   ├── MapModel.java
│   ├── MapPresenter.java
│   └── MapView.java
```

### Pattern 1: Debounced Property Listeners
**What:** Delaying the map generation until the user has stopped dragging the slider for ~300ms.
**When to use:** Whenever hooking JavaFX `Slider.valueProperty()` or `TextField.textProperty()` to the map generator.
**Example:**
```java
PauseTransition debounce = new PauseTransition(Duration.millis(300));
slider.valueProperty().addListener((obs, oldVal, newVal) -> {
    debounce.playFromStart(); // Resets the timer
});
debounce.setOnFinished(e -> presenter.onParameterChanged());
```

### Pattern 2: Bulk Pixel Writing
**What:** Writing colors to the JavaFX `WritableImage` via integer arrays instead of `setColor()`.
**When to use:** In the `CPU Translation Layer` rendering step (D-09).
**Example:**
```java
int width = grid.getWidth();
int height = grid.getHeight();
int[] pixels = new int[width * height];

for (int y = 0; y < height; y++) {
    for (int x = 0; x < width; x++) {
        MapCell cell = grid.getCell(x, y);
        // Pack ARGB
        pixels[y * width + x] = cell.getBiome().getColorARGB(); 
    }
}
PixelFormat<IntBuffer> format = PixelFormat.getIntArgbPreInstance();
writableImage.getPixelWriter().setPixels(0, 0, width, height, format, pixels, 0, width);
```

### Anti-Patterns to Avoid
- **Object Thrashing:** Instantiating new objects (e.g., `new Color(...)`, `new MapCell(...)`) inside the `x/y` noise generation loop repeatedly. Generate the `MapGrid` once, and update existing `MapCell` properties during regenerations.
- **UI Thread Blocking:** Running `MapGenerator.generate()` on the `Platform.runLater()` thread. This will freeze the app window during the noise calculation.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| UI Event Throttling | Custom timer threads with `Thread.sleep()` | JavaFX `PauseTransition` | Native, runs on UI thread, automatically handles concurrency and cancellation smoothly. |
| Perlin Math (if possible) | Copy-pasting old, non-optimized Ken Perlin C code | `FastNoiseLite` (Java) | Avoids floating-point rounding issues, handles seed combinations properly, and offers 10x performance for fBm calculations. |

## Common Pitfalls

### Pitfall 1: Memory Leaks on Regeneration
**What goes wrong:** Generating a new 1000x1000 `MapGrid` object every time a slider is moved causes massive garbage collector spikes and stuttering.
**Why it happens:** Creating 1,000,000 new `MapCell` objects per update.
**How to avoid:** The `MapGrid` should pre-allocate its 2D array of `MapCell`s once. Upon parameter change, iterate the existing cells and update their primitive values (`elevation`, `temperature`, `biome`).

### Pitfall 2: Disconnected Parameter Ranges
**What goes wrong:** Sliders output `0.0` to `100.0`, but noise parameters expect `0.001` to `0.05` for frequency, or `-1.0` to `1.0` for thresholds. 
**Why it happens:** Failure to normalize UI parameters into math boundaries.
**How to avoid:** Create a specific mapping function in the `MapGenerator` or `Presenter` that scales UI percentages (0-100) to actual algorithmic thresholds (e.g., Water Level % -> translates to a `-0.5` to `0.5` elevation cutoff).

### Pitfall 3: The "Square Island" Problem
**What goes wrong:** The `Island_Falloff_Mask` creates visible square bounds or harsh circles instead of natural shores.
**Why it happens:** Using naive linear distance from the center.
**How to avoid:** Use a rounded rectangle gradient or a Euclidean distance function multiplied by a scaling factor, and subtract it smoothly from the base noise.

## Code Examples

### Island Falloff Mask (Distance from center)
```java
// Normalize coordinates to -1.0 to 1.0 range
double nx = 2.0 * x / width - 1.0;
double ny = 2.0 * y / height - 1.0;

// Calculate distance from center (using Euclidean or Max dist)
double distance = Math.sqrt(nx * nx + ny * ny);

// Create a falloff curve (e.g., cubic or smoothstep)
double falloff = Math.max(0.0, 1.0 - (distance * distance));

// Apply to elevation: elevation = (noiseVal + 1) * falloff - 1;
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `java.awt.Color` in data layers | Decoupled enum attributes | Always | Maps can be serialized to JSON/Custom formats without pulling in AWT/JavaFX graphics libraries. |
| Nested Loop `setColor()` | Flat array `setPixels()` | JavaFX 8+ | Write operations are near-instantaneous due to native memory block copying. |

## Open Questions

1. **Cell Storage Efficiency**
   - What we know: `MapCell` holds multiple doubles and an enum reference. For a 2000x2000 grid, this is 4 million objects.
   - What's unclear: Will Java's object overhead cause out-of-memory errors on standard setups?
   - Recommendation: If OOM or severe GC pauses occur, convert `MapCell[][]` to a struct-like arrays approach (`double[] elevations`, `byte[] biomes`). For Phase 2, start with the cleaner OOP `MapCell` as requested by D-01, but keep the array-backing idea in reserve if performance tests fail.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java | Core project | ✓ | 21 | — |
| JavaFX | UI and Canvas | ✓ | 21.0.6 | — |
| FastNoiseLite (Java) | Noise math | ✗ | — | Implement single custom `PerlinNoise` class if adding external source files is prohibited. |

**Missing dependencies with fallback:**
- `FastNoiseLite`: Drop the single `.java` file into the `math` package.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 |
| Config file | `build.gradle.kts` |
| Quick run command | `./gradlew test` |
| Full suite command | `./gradlew test` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| REQ-01 | Generator outputs valid 2D array | unit | `./gradlew test --tests *MapGeneratorTest*` | ❌ Wave 0 |
| REQ-01 | Biome resolution logic respects limits | unit | `./gradlew test --tests *BiomeResolverTest*` | ❌ Wave 0 |
| REQ-02 | Debouncer delays execution correctly | unit | N/A (UI) | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew test`
- **Per wave merge:** `./gradlew build`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `src/test/java/com/mapbuilder/mapbuilder/core/map/MapGeneratorTest.java` — Test noise outputs and array bounds.
- [ ] `src/test/java/com/mapbuilder/mapbuilder/core/map/BiomeResolverTest.java` — Tests the Whittaker biome thresholds.

## Sources

### Primary (HIGH confidence)
- JavaFX API Docs (`PixelWriter`, `PauseTransition`)
- Project Context (`.planning/phases/02-core-map-generation/02-CONTEXT.md`)

### Secondary (MEDIUM confidence)
- Procedural Generation Patterns (Red Blob Games biome and falloff models - standard industry practice for map gen).
- FastNoiseLite documentation.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - JavaFX `Task`/`PixelWriter` + single-file noise is the definitive pattern for this domain.
- Architecture: HIGH - MVP pattern decoupling is explicitly ordered by context D-01/D-02.
- Pitfalls: HIGH - GC thrashing and UI thread blocking are the two most common reasons JavaFX canvas map generators fail.

**Research date:** 2026-04-04
**Valid until:** 2026-10-04
