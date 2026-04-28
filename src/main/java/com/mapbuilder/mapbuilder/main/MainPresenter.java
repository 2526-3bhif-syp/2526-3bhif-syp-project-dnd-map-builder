package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.map.MapCell;
import com.mapbuilder.mapbuilder.core.map.MapGrid;
import com.mapbuilder.mapbuilder.core.map.PointOfInterest;
import com.mapbuilder.mapbuilder.core.math.FastNoiseLite;
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

public class MainPresenter {
    
    public static final int COLOR_RIVER = 0xFF00BFFF; // Deep Sky Blue
    public static final int COLOR_LAKE = 0xFF1E90FF;  // Dodger Blue

    private MainView view;
    private final MainModel model;
    private final PauseTransition debounce;
    private final PauseTransition viewportDebounce;
    private Image spriteSheet;

    // Viewport state — zoom/pan handled in software, no Group transforms
    private double viewOffsetX = 0;
    private double viewOffsetY = 0;
    private double pixelsPerCell = 1.0;

    // Noise generator for organic biome-border perturbation at high zoom
    private final FastNoiseLite borderNoise;

    public MainPresenter() {
        this.model = new MainModel();
        this.debounce = new PauseTransition(Duration.millis(300));
        this.debounce.setOnFinished(e -> generateMapAsync());
        this.viewportDebounce = new PauseTransition(Duration.millis(100));
        this.viewportDebounce.setOnFinished(e -> renderMap());
        this.borderNoise = new FastNoiseLite(42);
        this.borderNoise.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
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
            double factor = deltaY > 0 ? 1.05 : 1.0 / 1.05;
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
            pendingViewportInit = true;
            renderMap();
        });
        new Thread(task).start();
    }

    private void onZoom(double factor, double mouseX, double mouseY) {
        double mapFocusX = viewOffsetX + mouseX / pixelsPerCell;
        double mapFocusY = viewOffsetY + mouseY / pixelsPerCell;
        pixelsPerCell = Math.max(0.1, Math.min(pixelsPerCell * factor, 32.0));
        viewOffsetX = mapFocusX - mouseX / pixelsPerCell;
        viewOffsetY = mapFocusY - mouseY / pixelsPerCell;
        viewportDebounce.playFromStart();
    }

    private void onPan(double dx, double dy) {
        viewOffsetX -= dx / pixelsPerCell;
        viewOffsetY -= dy / pixelsPerCell;
        viewportDebounce.playFromStart();
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
        MapGrid grid = model.getCurrentGrid();
        if (grid == null) return;
        Canvas canvas = view.getCanvas();

        int canvasW = (int) canvas.getWidth();
        int canvasH = (int) canvas.getHeight();
        if (canvasW <= 0 || canvasH <= 0) return;

        int gridW = grid.getWidth();
        int gridH = grid.getHeight();

        if (pendingViewportInit) {
            pendingViewportInit = false;
            pixelsPerCell = Math.min((double) canvasW / gridW, (double) canvasH / gridH);
            viewOffsetX = (gridW - canvasW / pixelsPerCell) / 2.0;
            viewOffsetY = (gridH - canvasH / pixelsPerCell) / 2.0;
        }

        boolean showBorders = view.getEnableBordersToggle().isSelected();
        boolean showOverlay = view.getEnableKingdomOverlayToggle().isSelected();

        int[] pixels = new int[canvasW * canvasH];

        for (int py = 0; py < canvasH; py++) {
            for (int px = 0; px < canvasW; px++) {
                double mapX = viewOffsetX + px / pixelsPerCell;
                double mapY = viewOffsetY + py / pixelsPerCell;

                int color;
                if (pixelsPerCell >= 8.0) {
                    // Bilinear + noise-perturbed sampling for organic borders
                    float noise = borderNoise.GetNoise((float)(mapX * 6), (float)(mapY * 6));
                    double perturbedX = mapX + noise * 0.45;
                    double perturbedY = mapY + noise * 0.45;
                    color = sampleBilinear(perturbedX, perturbedY, grid, showBorders, showOverlay);
                } else if (pixelsPerCell >= 3.0) {
                    // Bilinear interpolation for smooth color gradients
                    color = sampleBilinear(mapX, mapY, grid, showBorders, showOverlay);
                } else {
                    // Nearest-neighbor — fast path for zoomed-out view
                    color = getColorAt((int) mapX, (int) mapY, grid, showBorders, showOverlay);
                }

                pixels[py * canvasW + px] = color;
            }
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.getPixelWriter().setPixels(0, 0, canvasW, canvasH,
                PixelFormat.getIntArgbPreInstance(), pixels, 0, canvasW);

        renderPOIs();
        view.getPOIListPanel().updatePOIList(grid.getPointsOfInterest());
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

    private int blendColors(int c00, int c10, int c01, int c11, double tx, double ty) {
        int r = (int) (lerp(lerp((c00 >> 16) & 0xFF, (c10 >> 16) & 0xFF, tx),
                            lerp((c01 >> 16) & 0xFF, (c11 >> 16) & 0xFF, tx), ty));
        int g = (int) (lerp(lerp((c00 >> 8) & 0xFF, (c10 >> 8) & 0xFF, tx),
                            lerp((c01 >> 8) & 0xFF, (c11 >> 8) & 0xFF, tx), ty));
        int b = (int) (lerp(lerp(c00 & 0xFF, c10 & 0xFF, tx),
                            lerp(c01 & 0xFF, c11 & 0xFF, tx), ty));
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
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

        // Render label for hovered POI
        if (hoveredPOI != null) {
            double labelX = (hoveredPOI.getX() - viewOffsetX) * pixelsPerCell;
            double labelY = (hoveredPOI.getY() - viewOffsetY) * pixelsPerCell - 20;

            // Draw label with shadow for legibility
            gc.setFont(new Font("Arial", 11));

            // Black shadow
            gc.setFill(javafx.scene.paint.Color.BLACK);
            gc.fillText(hoveredPOI.getName(), labelX + 1, labelY + 1);

            // White text
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

