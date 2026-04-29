package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.map.LodGridCache;
import com.mapbuilder.mapbuilder.core.map.LodLevel;
import com.mapbuilder.mapbuilder.core.map.LodRegion;
import com.mapbuilder.mapbuilder.core.map.MapCell;
import com.mapbuilder.mapbuilder.core.map.MapGenerator;
import com.mapbuilder.mapbuilder.core.map.MapGrid;
import com.mapbuilder.mapbuilder.core.map.PointOfInterest;
import com.mapbuilder.mapbuilder.ui.POIEditorDialog;
import com.mapbuilder.mapbuilder.ui.POIIconMapper;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.text.Font;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class MainPresenter {
    
    public static final int COLOR_RIVER = 0xFF00BFFF; // Deep Sky Blue
    public static final int COLOR_LAKE = 0xFF1E90FF;  // Dodger Blue
    private static final int[][] BORDER_DIRS = {{-1,0},{1,0},{0,-1},{0,1}};

    private MainView view;
    private final MainModel model;
    private final PauseTransition debounce;
    private final PauseTransition lodDebounce;
    private Image spriteSheet;

    // Viewport state — zoom/pan handled in software, no Group transforms
    private double viewOffsetX = 0;
    private double viewOffsetY = 0;
    private double pixelsPerCell = 1.0;

    // Background render task — cancelled when a new render is requested
    private volatile Task<int[]> runningRenderTask;

    // LOD system
    private final LodGridCache lodCache = new LodGridCache();
    private volatile Task<MapGrid> runningLodTask;
    private final MapGenerator lodGenerator = new MapGenerator();
    // Tracks the LOD level of the last committed LOD generation so we only
    // regenerate when the user crosses a zoom threshold, not on every pan.
    private LodLevel activeLodLevel = LodLevel.LOD_0;

    public MainPresenter() {
        this.model = new MainModel();
        this.debounce = new PauseTransition(Duration.millis(300));
        this.debounce.setOnFinished(e -> generateMapAsync());
        this.lodDebounce = new PauseTransition(Duration.millis(300));
        this.lodDebounce.setOnFinished(e -> checkLodNeeded());
    }

    public void setView(MainView view) {
        this.view = view;
        // Set presenter for POI list panel callbacks
        view.getPOIListPanel().setPresenter(this);
        bind();
        triggerGeneration(); // initial generation
    }

    public MainView getView() {
        return view;
    }

    private void bind() {
        final double[] dragStart = new double[2];
        view.getCanvasContainer().setOnScroll(event -> {
            double deltaY = event.getDeltaY();
            // Linux/X11 fires a zero-delta ghost event before each real scroll tick
            if (deltaY == 0) { event.consume(); return; }
            double factor = deltaY > 0 ? 1.15 : 1.0 / 1.15;
            onZoom(factor, event.getX(), event.getY());
            event.consume();
        });
        view.getCanvasContainer().setOnMousePressed(event -> {
            view.getCanvasContainer().requestFocus();
            dragStart[0] = event.getSceneX();
            dragStart[1] = event.getSceneY();
        });
        view.getCanvasContainer().setOnMouseDragged(event -> {
            double dx = event.getSceneX() - dragStart[0];
            double dy = event.getSceneY() - dragStart[1];
            dragStart[0] = event.getSceneX();
            dragStart[1] = event.getSceneY();
            onPan(dx, dy);
        });

        view.getRandomSeedButton().setOnAction(e -> {
            int randomSeed = ThreadLocalRandom.current().nextInt(10000000, 100000000);
            view.getSeedField().setText(String.valueOf(randomSeed));
        });
        view.getRandomizeSettingsButton().setOnAction(e -> randomizeSettings());
        view.getSizeSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getOctavesSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getScaleSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getSeedField().textProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getFalloffSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getWaterLevelSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getTempBiasSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getRainBiasSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        
        view.getEnableRiversToggle().selectedProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getEnableLakesToggle().selectedProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getRiverDensitySlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getLakeSizeSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getMinLakeAreaSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        
        view.getKingdomCountSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getLloydPassesSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        
        view.getEnableBordersToggle().selectedProperty().addListener((obs, oldV, newV) -> renderMap());
        view.getEnableKingdomOverlayToggle().selectedProperty().addListener((obs, oldV, newV) -> renderMap());
        
        // Wire POI density sliders to trigger generation
        setupPOIDensityListeners();
    }
    
    /**
     * Sets up listeners for POI density sliders.
     * Changes to density sliders trigger POI regeneration only (not full map regeneration).
     * This allows adjusting POI density without affecting terrain, hydrology, or kingdoms.
     */
    private void setupPOIDensityListeners() {
        // POI sliders trigger only POI regeneration, not full map generation
        // Use a debounce to avoid excessive recalculation while user is adjusting sliders
        PauseTransition poiDebounce = new PauseTransition(Duration.millis(300));
        poiDebounce.setOnFinished(e -> generatePOIsOnly());
        
        view.getDungeonDensitySlider().valueProperty().addListener((obs, oldV, newV) -> {
            poiDebounce.playFromStart();
        });
        view.getSettlementDensitySlider().valueProperty().addListener((obs, oldV, newV) -> {
            poiDebounce.playFromStart();
        });
    }
    
    /**
     * Regenerates only POIs without affecting terrain, hydrology, or kingdoms.
     */
    private void generatePOIsOnly() {
        MapGrid grid = model.getCurrentGrid();
        if (grid == null) return;
        
        int parsedSeed;
        try {
            parsedSeed = Integer.parseInt(view.getSeedField().getText());
        } catch (NumberFormatException e) {
            parsedSeed = view.getSeedField().getText().hashCode();
        }
        
        double dungeonDensity = view.getDungeonDensitySlider().getValue();
        double settlementDensity = view.getSettlementDensitySlider().getValue();
        
        // Regenerate POIs with current density settings using PointOfInterestGenerator
        grid.setPointsOfInterest(
            com.mapbuilder.mapbuilder.core.map.PointOfInterestGenerator.generatePointsOfInterest(
                grid, parsedSeed, dungeonDensity, 0.0, settlementDensity
            )
        );
        
        // Re-render the map with new POIs
        renderMap();
    }

    private void triggerGeneration() {
        debounce.playFromStart();
    }

    private void randomizeSettings() {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        view.getSizeSlider().setValue(rand.nextInt(200, 2001));
        view.getOctavesSlider().setValue(rand.nextInt(1, 11));
        view.getScaleSlider().setValue(rand.nextDouble(0.001, 0.05));
        view.getFalloffSlider().setValue(rand.nextDouble(-1.0, 1.0));
        view.getWaterLevelSlider().setValue(rand.nextDouble(-1.0, 1.0));
        view.getTempBiasSlider().setValue(rand.nextDouble(-0.5, 0.5));
        view.getRainBiasSlider().setValue(rand.nextDouble(-0.5, 0.5));
        view.getRiverDensitySlider().setValue(rand.nextDouble(0, 200));
        view.getLakeSizeSlider().setValue(rand.nextDouble(0, 200));
        view.getMinLakeAreaSlider().setValue(rand.nextDouble(10, 500));
        triggerGeneration();
    }

    private void generateMapAsync() {
        int parsedSeed;
        try {
            parsedSeed = Integer.parseInt(view.getSeedField().getText());
        } catch (NumberFormatException e) {
            parsedSeed = view.getSeedField().getText().hashCode();
        }
        
        final int seed = parsedSeed;
        final int size = (int) view.getSizeSlider().getValue();
        final int octaves = (int) view.getOctavesSlider().getValue();
        final float scale = (float) view.getScaleSlider().getValue();
        
        final double falloff = view.getFalloffSlider().getValue();
        final double waterLevel = view.getWaterLevelSlider().getValue();
        final double tempBias = view.getTempBiasSlider().getValue();
        final double rainBias = view.getRainBiasSlider().getValue();
        
        final boolean enableRivers = view.getEnableRiversToggle().isSelected();
        final boolean enableLakes = view.getEnableLakesToggle().isSelected();
        final double riverDensity = view.getRiverDensitySlider().getValue();
        final double lakeSize = view.getLakeSizeSlider().getValue();
        final int minLakeArea = (int) view.getMinLakeAreaSlider().getValue();
        final int kingdomCount = (int) view.getKingdomCountSlider().getValue();
        final int lloydPasses = (int) view.getLloydPassesSlider().getValue();

        Task<Void> task = new Task<>() {
             @Override
            protected Void call() {
                // Read POI density parameters from sliders
                double dungeonDensity = view.getDungeonDensitySlider().getValue();
                double settlementDensity = view.getSettlementDensitySlider().getValue();
                
                model.generateMap(seed, size, octaves, scale, falloff, waterLevel, tempBias, rainBias,
                                  enableRivers, enableLakes, riverDensity, lakeSize, minLakeArea,
                                  kingdomCount, lloydPasses,
                                  dungeonDensity, 0.0, settlementDensity);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            // Invalidate all cached LOD grids — they belong to the old map
            Task<MapGrid> oldLod = runningLodTask;
            if (oldLod != null) oldLod.cancel(false);
            lodCache.invalidateAll();
            activeLodLevel = LodLevel.LOD_0;
            pendingViewportInit = true;
            renderMap();
        });
        new Thread(task).start();
    }

    private void onZoom(double factor, double mouseX, double mouseY) {
        double mapFocusX = viewOffsetX + mouseX / pixelsPerCell;
        double mapFocusY = viewOffsetY + mouseY / pixelsPerCell;
        pixelsPerCell = Math.max(0.1, Math.min(pixelsPerCell * factor, 64.0));
        viewOffsetX = mapFocusX - mouseX / pixelsPerCell;
        viewOffsetY = mapFocusY - mouseY / pixelsPerCell;
        renderMap();          // immediate visual response
        lodDebounce.playFromStart(); // LOD check after settling
    }

    private void onPan(double dx, double dy) {
        viewOffsetX -= dx / pixelsPerCell;
        viewOffsetY -= dy / pixelsPerCell;
        renderMap();          // immediate visual response
        lodDebounce.playFromStart(); // LOD check after settling
    }

    private void initViewport(int gridW, int gridH) {
        double canvasW = view.getCanvas().getWidth();
        double canvasH = view.getCanvas().getHeight();
        if (canvasW <= 0 || canvasH <= 0) return;
        pixelsPerCell = Math.min(canvasW / gridW, canvasH / gridH);
        viewOffsetX = (gridW - canvasW / pixelsPerCell) / 2.0;
        viewOffsetY = (gridH - canvasH / pixelsPerCell) / 2.0;
        renderMap();
    }

    private boolean pendingViewportInit = false;

    private void renderMap() {
        MapGrid baseGrid = model.getCurrentGrid();
        if (baseGrid == null) return;
        Canvas canvas = view.getCanvas();

        int canvasW = (int) canvas.getWidth();
        int canvasH = (int) canvas.getHeight();
        if (canvasW <= 0 || canvasH <= 0) return;

        if (pendingViewportInit) {
            pendingViewportInit = false;
            int gridW = baseGrid.getWidth();
            int gridH = baseGrid.getHeight();
            pixelsPerCell = Math.min((double) canvasW / gridW, (double) canvasH / gridH);
            viewOffsetX = (gridW - canvasW / pixelsPerCell) / 2.0;
            viewOffsetY = (gridH - canvasH / pixelsPerCell) / 2.0;
        }

        // Determine active grid: cached LOD grid or base grid fallback
        LodRegion region = computeRequiredLOD(canvasW, canvasH, baseGrid);
        final MapGrid activeGrid;
        final double activeOffX, activeOffY, activePPC;

        if (region != null) {
            MapGrid lodGrid = lodCache.get(region.cacheKey());
            if (lodGrid != null) {
                int mult = region.lod.multiplier;
                activeGrid  = lodGrid;
                activeOffX  = (viewOffsetX - region.wx0) * mult;
                activeOffY  = (viewOffsetY - region.wy0) * mult;
                // pixelsPerCell is in world-units; LOD cells are 1/mult world units each,
                // so there are mult times as many LOD pixels per canvas pixel
                activePPC   = pixelsPerCell / mult;
            } else {
                // LOD pending — render base grid immediately so the screen isn't blank
                activeGrid  = baseGrid;
                activeOffX  = viewOffsetX;
                activeOffY  = viewOffsetY;
                activePPC   = pixelsPerCell;
            }
        } else {
            activeGrid  = baseGrid;
            activeOffX  = viewOffsetX;
            activeOffY  = viewOffsetY;
            activePPC   = pixelsPerCell;
        }

        final boolean showBorders = view.getEnableBordersToggle().isSelected();
        final boolean showOverlay = view.getEnableKingdomOverlayToggle().isSelected();
        final int w = canvasW;
        final int h = canvasH;
        final int gW = activeGrid.getWidth();
        final int gH = activeGrid.getHeight();

        // Cancel any running render before starting a new one
        Task<int[]> prev = runningRenderTask;
        if (prev != null) prev.cancel(false);

        Task<int[]> task = new Task<>() {
            @Override
            protected int[] call() {
                // Pre-compute one color per grid cell — eliminates redundant getColorAt calls
                // Each grid cell is shared by multiple canvas pixels in bilinear sampling.
                int[] colorCache = buildColorCache(activeGrid, gW, gH, showBorders, showOverlay);
                if (isCancelled()) return null;

                final int[] pixels = new int[w * h];
                final boolean bilinear = activePPC >= 3.0;
                final int[] cc = colorCache; // local alias for lambda capture

                // Parallel horizontal strips — each row writes to a non-overlapping
                // region of pixels[] so no synchronisation is needed.
                IntStream.range(0, h).parallel().forEach(py -> {
                    for (int px = 0; px < w; px++) {
                        double mapX = activeOffX + px / activePPC;
                        double mapY = activeOffY + py / activePPC;
                        int color;
                        if (bilinear) {
                            color = sampleBilinearFast(mapX, mapY, cc, gW, gH);
                        } else {
                            int ix = Math.max(0, Math.min(gW - 1, (int) mapX));
                            int iy = Math.max(0, Math.min(gH - 1, (int) mapY));
                            color = cc[iy * gW + ix];
                        }
                        pixels[py * w + px] = color;
                    }
                });
                return isCancelled() ? null : pixels;
            }
        };

        task.setOnSucceeded(e -> {
            int[] pixels = task.getValue();
            if (pixels == null) return;
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.getPixelWriter().setPixels(0, 0, w, h,
                    PixelFormat.getIntArgbPreInstance(), pixels, 0, w);
            renderPOIs();
            view.getPOIListPanel().updatePOIList(baseGrid.getPointsOfInterest());
        });

        runningRenderTask = task;
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Computes the LOD region required for the current viewport.
     * Returns null when LOD_0 (base grid is sufficient).
     *
     * Region is snapped to 64-world-cell blocks and covers 3× the visible area
     * so that panning does not constantly invalidate the cache key.
     */
    private LodRegion computeRequiredLOD(int canvasW, int canvasH, MapGrid baseGrid) {
        LodLevel lod = LodLevel.forZoom(pixelsPerCell);
        if (lod == LodLevel.LOD_0) return null;

        int gridW = baseGrid.getWidth();
        int gridH = baseGrid.getHeight();

        // Visible cells on screen
        int visW = Math.max(1, (int) Math.ceil(canvasW / pixelsPerCell));
        int visH = Math.max(1, (int) Math.ceil(canvasH / pixelsPerCell));

        // Expand to 3× viewport so panning doesn't bust the cache immediately
        int padW = visW;   // 1× padding each side → 3× total
        int padH = visH;

        int rawX0 = (int) viewOffsetX - padW;
        int rawY0 = (int) viewOffsetY - padH;
        int rawX1 = (int) viewOffsetX + visW + padW;
        int rawY1 = (int) viewOffsetY + visH + padH;

        // Snap to 64-cell boundaries so small movements keep the same cache key
        final int SNAP = 64;
        int wx0 = Math.max(0,     (rawX0 / SNAP) * SNAP);
        int wy0 = Math.max(0,     (rawY0 / SNAP) * SNAP);
        int wx1 = Math.min(gridW, ((rawX1 + SNAP - 1) / SNAP) * SNAP);
        int wy1 = Math.min(gridH, ((rawY1 + SNAP - 1) / SNAP) * SNAP);

        if (wx1 <= wx0 || wy1 <= wy0) return null;
        return new LodRegion(wx0, wy0, wx1, wy1, lod);
    }

    /**
     * Called by lodDebounce after the viewport has settled (300 ms of inactivity).
     * Only triggers LOD generation on level-change or when the viewport has
     * panned outside the currently cached region.
     */
    private void checkLodNeeded() {
        MapGrid baseGrid = model.getCurrentGrid();
        if (baseGrid == null) return;
        int canvasW = (int) view.getCanvas().getWidth();
        int canvasH = (int) view.getCanvas().getHeight();

        LodLevel newLevel = LodLevel.forZoom(pixelsPerCell);

        // Dropping back to base grid — cancel any running LOD task
        if (newLevel == LodLevel.LOD_0) {
            Task<MapGrid> old = runningLodTask;
            if (old != null) old.cancel(false);
            activeLodLevel = LodLevel.LOD_0;
            return;
        }

        LodRegion region = computeRequiredLOD(canvasW, canvasH, baseGrid);
        if (region == null) return;

        // If the level didn't change AND the cache already has this region, skip
        if (newLevel == activeLodLevel && lodCache.get(region.cacheKey()) != null) return;

        requestLodGeneration(region, baseGrid);
    }

    /**
     * Kicks off async LOD grid generation for the given region.
     * Cancels any in-flight LOD task first.
     */
    private void requestLodGeneration(LodRegion region, MapGrid baseGrid) {
        // Snapshot all slider values on the FX thread before handing off
        int seed;
        try { seed = Integer.parseInt(view.getSeedField().getText()); }
        catch (NumberFormatException ex) { seed = view.getSeedField().getText().hashCode(); }

        final int    fSeed      = seed;
        final int    fOctaves   = (int) view.getOctavesSlider().getValue();
        final float  fScale     = (float) view.getScaleSlider().getValue();
        final double fFalloff   = view.getFalloffSlider().getValue();
        final double fWaterLevel= view.getWaterLevelSlider().getValue();
        final double fTempBias  = view.getTempBiasSlider().getValue();
        final double fRainBias  = view.getRainBiasSlider().getValue();
        final int    fWorldW    = baseGrid.getWidth();
        final int    fWorldH    = baseGrid.getHeight();

        Task<MapGrid> prev = runningLodTask;
        if (prev != null) prev.cancel(false);

        Task<MapGrid> task = new Task<>() {
            @Override
            protected MapGrid call() {
                if (isCancelled()) return null;
                MapGrid lodGrid = new MapGrid(region.lodGridW, region.lodGridH);
                lodGenerator.generateLOD(
                        lodGrid, fSeed, fOctaves, fScale, fFalloff, fWaterLevel,
                        fTempBias, fRainBias,
                        region.wx0, region.wy0, region.cellWorldSize,
                        fWorldW, fWorldH
                );
                return isCancelled() ? null : lodGrid;
            }
        };

        task.setOnSucceeded(e -> {
            MapGrid lodGrid = task.getValue();
            if (lodGrid == null) return;
            propagateBaseGridState(lodGrid, region, baseGrid);
            lodCache.put(region.cacheKey(), lodGrid);
            activeLodLevel = region.lod;
            renderMap(); // re-render with the freshly cached LOD grid
        });

        runningLodTask = task;
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Copies kingdom, river, and lake state from the base grid into each LOD cell.
     * At LOD_2 (mult >= 4), rivers are dilated by searching a 1-base-cell radius so
     * that single-cell rivers appear mult pixels wide instead of 1.
     */
    private void propagateBaseGridState(MapGrid lodGrid, LodRegion region, MapGrid baseGrid) {
        int mult   = region.lod.multiplier;
        int lodW   = lodGrid.getWidth();
        int lodH   = lodGrid.getHeight();
        // For LOD_2+ we check a neighbourhood of base cells to widen rivers
        int searchR = (mult >= 4) ? 1 : 0;

        for (int cx = 0; cx < lodW; cx++) {
            for (int cy = 0; cy < lodH; cy++) {
                int bx = region.wx0 + cx / mult;
                int by = region.wy0 + cy / mult;
                MapCell baseCell = baseGrid.getCell(bx, by);
                if (baseCell == null) continue;
                MapCell lodCell = lodGrid.getCell(cx, cy);
                lodCell.setKingdom(baseCell.getKingdom());

                if (searchR == 0) {
                    // LOD_1: 1:1 propagation
                    lodCell.setRiver(baseCell.isRiver());
                    lodCell.setLake(baseCell.isLake());
                } else {
                    // LOD_2: dilate — mark as river/lake if ANY neighbour within
                    // searchR base-cells is river/lake
                    boolean isRiver = false, isLake = false;
                    outer:
                    for (int dx = -searchR; dx <= searchR; dx++) {
                        for (int dy = -searchR; dy <= searchR; dy++) {
                            MapCell nb = baseGrid.getCell(bx + dx, by + dy);
                            if (nb == null) continue;
                            if (nb.isRiver()) { isRiver = true; break outer; }
                            if (nb.isLake())  { isLake  = true; break outer; }
                        }
                    }
                    lodCell.setRiver(isRiver);
                    lodCell.setLake(isLake);
                }
            }
        }
    }

    /** Pre-computes one ARGB color per grid cell into a flat array [y*gW+x]. */
    private int[] buildColorCache(MapGrid grid, int gW, int gH,
                                  boolean showBorders, boolean showOverlay) {
        int[] cache = new int[gW * gH];
        for (int cy = 0; cy < gH; cy++) {
            for (int cx = 0; cx < gW; cx++) {
                cache[cy * gW + cx] = getColorAt(cx, cy, grid, showBorders, showOverlay);
            }
        }
        return cache;
    }

    /** Bilinear sample from pre-computed color cache — no MapGrid access in the hot loop. */
    private int sampleBilinearFast(double mapX, double mapY,
                                   int[] colorCache, int gW, int gH) {
        int x0 = (int) mapX;
        int y0 = (int) mapY;
        // Clamp so bilinear never reads outside the cache
        if (x0 < 0) x0 = 0; else if (x0 >= gW - 1) x0 = gW - 2;
        if (y0 < 0) y0 = 0; else if (y0 >= gH - 1) y0 = gH - 2;
        int tx256 = (int)((mapX - x0) * 256);
        int ty256 = (int)((mapY - y0) * 256);
        int c00 = colorCache[ y0      * gW + x0    ];
        int c10 = colorCache[ y0      * gW + x0 + 1];
        int c01 = colorCache[(y0 + 1) * gW + x0    ];
        int c11 = colorCache[(y0 + 1) * gW + x0 + 1];
        return blendColorsInt(c00, c10, c01, c11, tx256, ty256);
    }

    private int sampleBilinear(double mapX, double mapY, MapGrid grid, boolean showBorders, boolean showOverlay) {
        int x0 = (int) Math.floor(mapX);
        int y0 = (int) Math.floor(mapY);
        double tx = mapX - x0;
        double ty = mapY - y0;
        int c00 = getColorAt(x0,     y0,     grid, showBorders, showOverlay);
        int c10 = getColorAt(x0 + 1, y0,     grid, showBorders, showOverlay);
        int c01 = getColorAt(x0,     y0 + 1, grid, showBorders, showOverlay);
        int c11 = getColorAt(x0 + 1, y0 + 1, grid, showBorders, showOverlay);
        return blendColors(c00, c10, c01, c11, tx, ty);
    }

    private int getColorAt(int cx, int cy, MapGrid grid, boolean showBorders, boolean showOverlay) {
        if (cx < 0 || cx >= grid.getWidth() || cy < 0 || cy >= grid.getHeight()) {
            return 0xFF333333;
        }
        MapCell cell = grid.getCell(cx, cy);
        int color = cell.getMixedColorARGB();
        if (cell.isLake()) {
            color = COLOR_LAKE;
        } else if (cell.isRiver()) {
            color = COLOR_RIVER;
        }
        if (cell.getKingdom() != null) {
            if (showOverlay) {
                int kColor = cell.getKingdom().getColorARGB();
                int r = (kColor >> 16) & 0xFF;
                int g = (kColor >> 8) & 0xFF;
                int b = kColor & 0xFF;
                int cr = (color >> 16) & 0xFF;
                int cg = (color >> 8) & 0xFF;
                int cb = color & 0xFF;
                color = (0xFF << 24) | (((cr + r) / 2) << 16) | (((cg + g) / 2) << 8) | ((cb + b) / 2);
            }
            if (showBorders) {
                int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
                for (int[] dir : dirs) {
                    int nx = cx + dir[0], ny = cy + dir[1];
                    if (nx >= 0 && nx < grid.getWidth() && ny >= 0 && ny < grid.getHeight()) {
                        if (grid.getCell(nx, ny).getKingdom() != cell.getKingdom()) {
                            return 0xFF000000;
                        }
                    }
                }
            }
        }
        return color;
    }

    /** Integer bilinear blend — avoids FP division in the hot path. t256 in [0,256]. */
    private static int blendColorsInt(int c00, int c10, int c01, int c11,
                                      int tx256, int ty256) {
        int r = iLerp2(c00 >> 16, c10 >> 16, c01 >> 16, c11 >> 16, tx256, ty256) & 0xFF;
        int g = iLerp2(c00 >>  8, c10 >>  8, c01 >>  8, c11 >>  8, tx256, ty256) & 0xFF;
        int b = iLerp2(c00,       c10,       c01,       c11,        tx256, ty256) & 0xFF;
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private static int iLerp2(int v00, int v10, int v01, int v11,
                               int tx256, int ty256) {
        int top    = (v00 & 0xFF) + (((( v10 & 0xFF) - (v00 & 0xFF)) * tx256) >> 8);
        int bottom = (v01 & 0xFF) + ((((v11 & 0xFF) - (v01 & 0xFF)) * tx256) >> 8);
        return top + (((bottom - top) * ty256) >> 8);
    }

    private int blendColors(int c00, int c10, int c01, int c11, double tx, double ty) {
        int tx256 = (int)(tx * 256);
        int ty256 = (int)(ty * 256);
        return blendColorsInt(c00, c10, c01, c11, tx256, ty256);
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /**
     * Renders the POI overlay canvas with colored circles and sprite icons.
     * Called after renderMap() to draw POIs on a separate canvas layer.
     * This prevents flickering when map parameters change.
     */
    private void renderPOIs() {
        MapGrid grid = model.getCurrentGrid();
        if (grid == null) return;

        Canvas poiCanvas = view.getPoiCanvas();
        if (poiCanvas == null) return;

        int width = (int) poiCanvas.getWidth();
        int height = (int) poiCanvas.getHeight();
        GraphicsContext gc = poiCanvas.getGraphicsContext2D();

        // Clear canvas
        gc.clearRect(0, 0, width, height);

        // Load sprite sheet if not cached
        if (spriteSheet == null) {
            try {
                // Try loading from resources first (for packaged app)
                String resourcePath = getClass().getResource("/assets/poi-icons.png").toExternalForm();
                spriteSheet = new Image(resourcePath);
            } catch (Exception e1) {
                // Fallback to file path for development
                try {
                    spriteSheet = new Image("file:src/main/resources/assets/poi-icons.png");
                } catch (Exception e2) {
                    System.err.println("Failed to load POI sprite sheet: " + e2.getMessage());
                    return;
                }
            }
        }

        // Get list of POIs from grid
        List<PointOfInterest> pois = grid.getPointsOfInterest();
        if (pois == null || pois.isEmpty()) return;

        // Track mouse position for hover labels
        double mouseX = view.getMouseX();
        double mouseY = view.getMouseY();
        PointOfInterest hoveredPOI = null;

        // Scale icon size with zoom, clamped to a sensible range
        double iconSize = Math.max(8, Math.min(32, 14 * pixelsPerCell));
        double halfIcon = iconSize / 2.0;
        double spriteSize = Math.max(8, Math.min(32, 16 * pixelsPerCell));
        double halfSprite = spriteSize / 2.0;
        double hoverRadius = Math.max(12, Math.min(28, 20 * pixelsPerCell));

        // At high zoom show labels for all visible POIs; otherwise only hovered
        boolean showAllLabels = pixelsPerCell >= 12.0;
        double labelFontSize = Math.max(10, Math.min(16, 10 + (pixelsPerCell - 12) * 0.3));

        java.util.List<double[]> labelPositions = showAllLabels ? new java.util.ArrayList<>() : null;
        java.util.List<String>   labelTexts     = showAllLabels ? new java.util.ArrayList<>() : null;

        // Render each POI
        for (PointOfInterest poi : pois) {
            // Transform map coordinates to screen coordinates via viewport
            double screenX = (poi.getX() - viewOffsetX) * pixelsPerCell;
            double screenY = (poi.getY() - viewOffsetY) * pixelsPerCell;

            // Skip if off-screen (with margin for icon size)
            if (screenX < -halfIcon || screenX >= width + halfIcon
                    || screenY < -halfIcon || screenY >= height + halfIcon) {
                continue;
            }

            // Get color for this POI
            Integer customColor = poi.getCustomColor();
            int poiColor = customColor != null ? customColor : POIIconMapper.getDefaultColor(poi.getType());

            // Draw colored circle scaled with zoom
            gc.setFill(toFXColor(poiColor));
            gc.fillOval(screenX - halfIcon, screenY - halfIcon, iconSize, iconSize);

            // Draw sprite icon scaled with zoom
            int[] spriteCoords = POIIconMapper.getSpriteCoordinates(poi.getType());
            if (spriteCoords != null && spriteSheet != null && !spriteSheet.isError()) {
                gc.drawImage(
                    spriteSheet,
                    spriteCoords[0], spriteCoords[1], 32, 32,
                    screenX - halfSprite, screenY - halfSprite, spriteSize, spriteSize
                );
            }

            if (showAllLabels) {
                labelPositions.add(new double[]{screenX, screenY});
                labelTexts.add(poi.getName());
            }

            // Check if mouse is hovering over this POI
            double distance = Math.sqrt(
                Math.pow(mouseX - screenX, 2) + Math.pow(mouseY - screenY, 2)
            );
            if (distance < hoverRadius && (hoveredPOI == null || distance <
                Math.sqrt(Math.pow(mouseX - (hoveredPOI.getX() - viewOffsetX) * pixelsPerCell, 2)
                        + Math.pow(mouseY - (hoveredPOI.getY() - viewOffsetY) * pixelsPerCell, 2)))) {
                hoveredPOI = poi;
            }
        }

        // Render labels — all visible POIs at high zoom, or just the hovered one
        if (showAllLabels && labelPositions != null) {
            gc.setFont(new Font("Arial", labelFontSize));
            for (int i = 0; i < labelPositions.size(); i++) {
                double lx = labelPositions.get(i)[0];
                double ly = labelPositions.get(i)[1] - halfSprite - 4;
                gc.setFill(javafx.scene.paint.Color.BLACK);
                gc.fillText(labelTexts.get(i), lx + 1, ly + 1);
                gc.setFill(javafx.scene.paint.Color.WHITE);
                gc.fillText(labelTexts.get(i), lx, ly);
            }
        } else if (hoveredPOI != null) {
            double labelX = (hoveredPOI.getX() - viewOffsetX) * pixelsPerCell;
            double labelY = (hoveredPOI.getY() - viewOffsetY) * pixelsPerCell - 20;
            gc.setFont(new Font("Arial", 11));
            gc.setFill(javafx.scene.paint.Color.BLACK);
            gc.fillText(hoveredPOI.getName(), labelX + 1, labelY + 1);
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.fillText(hoveredPOI.getName(), labelX, labelY);
        }
    }

    /**
     * Converts an ARGB integer color to JavaFX Color.
     *
     * @param argb ARGB color value
     * @return JavaFX Color
     */
    private javafx.scene.paint.Color toFXColor(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return javafx.scene.paint.Color.color(r / 255.0, g / 255.0, b / 255.0, a / 255.0);
    }
    
    /**
     * Opens the POI editor modal dialog for the given POI.
     * 
     * @param poi The POI to edit
     */
    public void openPOIEditor(PointOfInterest poi) {
        if (poi == null || view == null) return;
        
        POIEditorDialog dialog = new POIEditorDialog(poi, view.getScene().getWindow(), this);
        dialog.showAndWait();
    }
    
    /**
     * Saves changes to a POI and updates the map display.
     * 
     * @param poi The POI to save (assumed to already have changes applied)
     */
    public void savePOI(PointOfInterest poi) {
        if (poi == null) return;
        
        MapGrid grid = model.getCurrentGrid();
        if (grid == null) return;
        
        // POI is already in the list and modified in-place, just trigger re-render
        renderMap();
    }
    
    /**
     * Deletes a POI from the map and updates the display.
     * 
     * @param poi The POI to delete
     */
    public void deletePOI(PointOfInterest poi) {
        if (poi == null) return;
        
        MapGrid grid = model.getCurrentGrid();
        if (grid == null) return;
        
        grid.removePointOfInterest(poi.getId());
        renderMap();
    }
}

