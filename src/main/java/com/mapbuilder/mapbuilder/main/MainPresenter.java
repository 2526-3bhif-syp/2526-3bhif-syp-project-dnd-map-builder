package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.map.Kingdom;
import com.mapbuilder.mapbuilder.core.map.LodGridCache;
import com.mapbuilder.mapbuilder.core.map.LodLevel;
import com.mapbuilder.mapbuilder.core.map.LodRegion;
import com.mapbuilder.mapbuilder.core.map.MapCell;
import com.mapbuilder.mapbuilder.core.map.MapGenerator;
import com.mapbuilder.mapbuilder.core.map.MapGrid;
import com.mapbuilder.mapbuilder.core.map.PointOfInterest;
import com.mapbuilder.mapbuilder.core.map.POIType;
import com.mapbuilder.mapbuilder.ui.POIEditorDialog;
import com.mapbuilder.mapbuilder.ui.POIIconMapper;
import com.mapbuilder.mapbuilder.ui.ProvinceListPanel;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import javafx.scene.image.WritableImage;

public class MainPresenter {
    
    public static final int COLOR_RIVER = 0xFF00BFFF; // Deep Sky Blue
    public static final int COLOR_LAKE = 0xFF1E90FF;  // Dodger Blue
    private static final int[][] BORDER_DIRS = {{-1,0},{1,0},{0,-1},{0,1}};

    private MainView view;
    private final MainModel model;
    private final PauseTransition debounce;
    private final PauseTransition lodDebounce;
    private Image spriteSheet;
    private com.mapbuilder.mapbuilder.core.map.MapLabel draggedLabel = null;

    // Viewport state — zoom/pan handled in software, no Group transforms
    private double viewOffsetX = 0;
    private double viewOffsetY = 0;
    private double pixelsPerCell = 1.0;

    // Label node cache — reuse nodes across renders
    private final Map<com.mapbuilder.mapbuilder.core.map.MapLabel, javafx.scene.text.Text> labelNodeCache = new java.util.IdentityHashMap<>();

    // Image Caching
    private WritableImage baseMapImage;
    private final Map<LodGridCache.Key, WritableImage> lodImageCache = new HashMap<>();

    // LOD system
    private final LodGridCache lodCache = new LodGridCache();
    private volatile Task<MapGrid> runningLodTask;
    private final MapGenerator lodGenerator = new MapGenerator();
    private LodLevel activeLodLevel = LodLevel.LOD_0;

    // Province editing
    private boolean provincePaintMode = false;
    private Kingdom selectedPaintKingdom = null;
    private double lastPaintX = -1;
    private double lastPaintY = -1;

    // POI editing
    private boolean addPoiMode = false;
    private PointOfInterest draggedPOI = null;

    public MainPresenter() {
        this.model = new MainModel();
        this.debounce = new PauseTransition(Duration.millis(300));
        this.debounce.setOnFinished(e -> generateMapAsync());
        this.lodDebounce = new PauseTransition(Duration.millis(300));
        this.lodDebounce.setOnFinished(e -> checkLodNeeded());
    }

    public void setView(MainView view) {
        this.view = view;
        view.getPOIListPanel().setPresenter(this);
        view.getProvinceListPanel().setPresenter(this);
        bind();
        triggerGeneration();
    }

    public MainView getView() {
        return view;
    }

    private void bind() {
        final double[] dragStart = new double[2];
        view.getCanvasContainer().setOnScroll(event -> {
            double deltaY = event.getDeltaY();
            if (deltaY == 0) { event.consume(); return; }
            double factor = deltaY > 0 ? 1.15 : 1.0 / 1.15;
            onZoom(factor, event.getX(), event.getY());
            event.consume();
        });
        
        view.getCanvasContainer().setOnMousePressed(event -> {
            view.getCanvasContainer().requestFocus();
            // Province paint mode — paint cells on press
            if (provincePaintMode && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                if (selectedPaintKingdom == null) {
                    view.getProvinceListPanel().flashError();
                    event.consume();
                    return;
                }
                lastPaintX = event.getX();
                lastPaintY = event.getY();
                paintCellsAtScreen(event.getX(), event.getY());
                event.consume();
                return;
            }
            // Add-POI mode — place a new POI on press
            if (addPoiMode && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                placePOIAtScreen(event.getX(), event.getY());
                event.consume();
                return;
            }
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                double x = event.getX();
                double y = event.getY();
                com.mapbuilder.mapbuilder.core.map.MapLabel label = getLabelAt(x, y);
                if (label != null) {
                    draggedLabel = label;
                    event.consume();
                    return;
                }
                // Start dragging a POI marker under the cursor
                PointOfInterest poi = getPOIAt(x, y);
                if (poi != null) {
                    draggedPOI = poi;
                    event.consume();
                    return;
                }
            }
            dragStart[0] = event.getSceneX();
            dragStart[1] = event.getSceneY();
        });
        
        view.getCanvasContainer().setOnMouseDragged(event -> {
            // Province paint mode — paint cells while dragging
            if (provincePaintMode && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                if (lastPaintX >= 0 && lastPaintY >= 0) {
                    paintCellsBetweenScreen(lastPaintX, lastPaintY, event.getX(), event.getY());
                } else {
                    paintCellsAtScreen(event.getX(), event.getY());
                }
                lastPaintX = event.getX();
                lastPaintY = event.getY();
                event.consume();
                return;
            }
            if (draggedLabel != null) {
                double worldX = viewOffsetX + event.getX() / pixelsPerCell;
                double worldY = viewOffsetY + event.getY() / pixelsPerCell;
                draggedLabel.setX(worldX);
                draggedLabel.setY(worldY);
                renderMap();
                event.consume();
                return;
            }
            if (draggedPOI != null) {
                MapGrid grid = model.getCurrentGrid();
                if (grid != null) {
                    int cx = (int) (viewOffsetX + event.getX() / pixelsPerCell);
                    int cy = (int) (viewOffsetY + event.getY() / pixelsPerCell);
                    cx = Math.max(0, Math.min(grid.getWidth() - 1, cx));
                    cy = Math.max(0, Math.min(grid.getHeight() - 1, cy));
                    draggedPOI.setX(cx);
                    draggedPOI.setY(cy);
                    renderMap();
                }
                event.consume();
                return;
            }
            double dx = event.getSceneX() - dragStart[0];
            double dy = event.getSceneY() - dragStart[1];
            dragStart[0] = event.getSceneX();
            dragStart[1] = event.getSceneY();
            onPan(dx, dy);
        });
        
        view.getCanvasContainer().setOnMouseReleased(event -> {
            if (provincePaintMode && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                lastPaintX = -1;
                lastPaintY = -1;
                annexEnclosedAreas();
            }
            draggedLabel = null;
            draggedPOI = null;
        });

        view.getRandomSeedButton().setOnAction(e -> {
            int randomSeed = ThreadLocalRandom.current().nextInt(10000000, 100000000);
            view.getSeedField().setText(String.valueOf(randomSeed));
        });
        view.getRandomizeSettingsButton().setOnAction(e -> randomizeSettings());
        view.getResetSettingsButton().setOnAction(e -> resetSettings());
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
        
        view.getEnableBordersToggle().selectedProperty().addListener((obs, oldV, newV) -> regenerateImages());
        view.getEnableKingdomOverlayToggle().selectedProperty().addListener((obs, oldV, newV) -> regenerateImages());

        view.getCanvasContainer().setOnMouseClicked(event -> {
            double x = event.getX();
            double y = event.getY();
            com.mapbuilder.mapbuilder.core.map.MapLabel clickedLabel = getLabelAt(x, y);

            // Double-click on a POI marker opens its editor (takes priority)
            if (event.getClickCount() == 2 && !provincePaintMode && !addPoiMode) {
                PointOfInterest poiHit = getPOIAt(x, y);
                if (poiHit != null) {
                    openPOIEditor(poiHit);
                    return;
                }
            }

            if (event.getClickCount() == 2 && !provincePaintMode) {
                if (clickedLabel != null) {
                    final com.mapbuilder.mapbuilder.core.map.MapLabel finalLabel = clickedLabel;
                    if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                        confirmAndRemoveLabel(finalLabel);
                    } else {
                        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(finalLabel.getText());
                        dialog.setTitle("Edit Map Label");
                        dialog.setHeaderText("Edit the label text:");
                        dialog.setContentText("Label:");
                        applyDarkTheme(dialog);
                        dialog.showAndWait().ifPresent(text -> {
                            finalLabel.setText(text);
                            renderMap();
                        });
                    }
                } else {
                    if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("New Label");
                        dialog.setTitle("Add Map Label");
                        dialog.setHeaderText("Enter label text:");
                        dialog.setContentText("Label:");
                        applyDarkTheme(dialog);
                        dialog.showAndWait().ifPresent(text -> {
                            double worldX = viewOffsetX + x / pixelsPerCell;
                            double worldY = viewOffsetY + y / pixelsPerCell;
                            model.addLabel(new com.mapbuilder.mapbuilder.core.map.MapLabel(text, worldX, worldY));
                            renderMap();
                        });
                    }
                }
            } else if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY && event.getClickCount() == 1) {
                if (clickedLabel != null) {
                     confirmAndRemoveLabel(clickedLabel);
                }
            } else if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY
                    && event.getClickCount() == 1 && !provincePaintMode) {
                // Single left-click in normal mode: select the province under the cursor
                if (clickedLabel == null) {
                    Kingdom k = getKingdomAtScreen(x, y);
                    if (k != null) setSelectedKingdom(k);
                }
            }
        });
        
        setupPOIDensityListeners();

        // Province paint mode toggle
        view.getProvincePaintToggle().selectedProperty().addListener((obs, oldV, newV) -> {
            provincePaintMode = newV;
            view.getCanvasContainer().setCursor(
                    newV ? javafx.scene.Cursor.CROSSHAIR : javafx.scene.Cursor.DEFAULT);
            if (!newV) setSelectedKingdom(null);
            // Mutually exclusive with add-POI mode
            if (newV && view.getAddPoiToggle().isSelected()) {
                view.getAddPoiToggle().setSelected(false);
            }
        });

        // Add-POI mode toggle
        view.getAddPoiToggle().selectedProperty().addListener((obs, oldV, newV) -> {
            addPoiMode = newV;
            view.getCanvasContainer().setCursor(
                    newV ? javafx.scene.Cursor.CROSSHAIR : javafx.scene.Cursor.DEFAULT);
            // Mutually exclusive with province paint mode
            if (newV && view.getProvincePaintToggle().isSelected()) {
                view.getProvincePaintToggle().setSelected(false);
            }
        });

        // Automatically turn off paint mode when leaving the Kingdoms tab
        view.getKingdomsTab().selectedProperty().addListener((obs, oldV, newV) -> {
            if (!newV) {
                view.getProvincePaintToggle().setSelected(false);
            }
        });
    }

    private com.mapbuilder.mapbuilder.core.map.MapLabel getLabelAt(double screenX, double screenY) {
        if (model.getCurrentGrid() == null) return null;
        double hitRadiusX = 40.0;
        double hitRadiusY = 20.0;
        for (com.mapbuilder.mapbuilder.core.map.MapLabel label : model.getLabels()) {
            double labelScreenX = (label.getX() - viewOffsetX) * pixelsPerCell;
            double labelScreenY = (label.getY() - viewOffsetY) * pixelsPerCell;
            if (Math.abs(labelScreenX - screenX) < hitRadiusX && Math.abs(labelScreenY - screenY) < hitRadiusY) {
                return label;
            }
        }
        return null;
    }

    /**
     * Returns the POI whose marker is under the given screen position, or null.
     * Uses the same hover-radius logic as {@link #renderPOIs()}, picking the
     * closest marker when several overlap.
     */
    private PointOfInterest getPOIAt(double screenX, double screenY) {
        MapGrid grid = model.getCurrentGrid();
        if (grid == null) return null;
        List<PointOfInterest> pois = grid.getPointsOfInterest();
        if (pois == null) return null;

        double hoverRadius = Math.max(12, Math.min(28, 20 * pixelsPerCell));
        PointOfInterest hit = null;
        double bestDist = Double.MAX_VALUE;
        for (PointOfInterest poi : pois) {
            double sx = (poi.getX() - viewOffsetX) * pixelsPerCell;
            double sy = (poi.getY() - viewOffsetY) * pixelsPerCell;
            double dist = Math.hypot(screenX - sx, screenY - sy);
            if (dist < hoverRadius && dist < bestDist) {
                bestDist = dist;
                hit = poi;
            }
        }
        return hit;
    }

    /**
     * Places a new user POI at the cell under the given screen position, adds it
     * to the map, and opens the editor for naming/typing. Add-POI mode is turned
     * off after a single placement to avoid repeated modal dialogs.
     */
    private void placePOIAtScreen(double screenX, double screenY) {
        MapGrid grid = model.getCurrentGrid();
        if (grid == null) return;

        int cx = (int) (viewOffsetX + screenX / pixelsPerCell);
        int cy = (int) (viewOffsetY + screenY / pixelsPerCell);
        cx = Math.max(0, Math.min(grid.getWidth() - 1, cx));
        cy = Math.max(0, Math.min(grid.getHeight() - 1, cy));

        int nextId = 0;
        for (PointOfInterest poi : grid.getPointsOfInterest()) {
            if (poi.getId() >= nextId) nextId = poi.getId() + 1;
        }

        PointOfInterest created = new PointOfInterest(
                nextId, cx, cy, POIType.CITY, "New POI", "user_placed");
        grid.addPointOfInterest(created);
        renderMap();

        view.getAddPoiToggle().setSelected(false);
        openPOIEditor(created);
    }

    private void applyDarkTheme(javafx.scene.control.Dialog<?> dialog) {
        String css = getClass().getResource("/styles.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(css);
    }

    private void confirmAndRemoveLabel(com.mapbuilder.mapbuilder.core.map.MapLabel label) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Label");
        alert.setHeaderText("Are you sure you want to delete this label?");
        alert.setContentText("Label text: " + label.getText());
        applyDarkTheme(alert);

        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            model.removeLabel(label);
            renderMap();
        }
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
        view.getRuinCastleDensitySlider().valueProperty().addListener((obs, oldV, newV) -> {
            poiDebounce.playFromStart();
        });
    }
    
    private void regenerateImages() {
        MapGrid baseGrid = model.getCurrentGrid();
        if (baseGrid == null) return;
        boolean showBorders = view.getEnableBordersToggle().isSelected();
        boolean showOverlay = view.getEnableKingdomOverlayToggle().isSelected();
        
        baseMapImage = createImageFromGrid(baseGrid, showBorders, showOverlay);
        lodImageCache.clear();
        
        renderMap();
    }

    /**
     * Regenerates only POIs without affecting terrain, hydrology, or kingdoms.
     */
    private void generatePOIsOnly() {
        triggerGeneration();
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

    private void resetSettings() {
        // Terrain
        view.getSeedField().setText("12345");
        view.getSizeSlider().setValue(800);
        view.getOctavesSlider().setValue(5);
        view.getScaleSlider().setValue(0.005);
        view.getFalloffSlider().setValue(0.0);
        view.getWaterLevelSlider().setValue(0.0);
        view.getTempBiasSlider().setValue(0.0);
        view.getRainBiasSlider().setValue(0.0);
        // Hydrology
        view.getEnableRiversToggle().setSelected(true);
        view.getEnableLakesToggle().setSelected(true);
        view.getRiverDensitySlider().setValue(100);
        view.getLakeSizeSlider().setValue(100);
        view.getMinLakeAreaSlider().setValue(100);
        // Kingdoms
        view.getKingdomCountSlider().setValue(10);
        view.getLloydPassesSlider().setValue(1);
        // POIs
        view.getDungeonDensitySlider().setValue(0.3);
        view.getSettlementDensitySlider().setValue(0.3);
        view.getRuinCastleDensitySlider().setValue(0.3);
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
                double ruinCastleDensity = view.getRuinCastleDensitySlider().getValue();
                double settlementDensity = view.getSettlementDensitySlider().getValue();
                
                model.generateMap(seed, size, octaves, scale, falloff, waterLevel, tempBias, rainBias,
                                  enableRivers, enableLakes, riverDensity, lakeSize, minLakeArea,
                                  kingdomCount, lloydPasses,
                                  dungeonDensity, ruinCastleDensity, settlementDensity);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            // Invalidate all cached LOD grids — they belong to the old map
            Task<MapGrid> oldLod = runningLodTask;
            if (oldLod != null) oldLod.cancel(false);
            lodCache.invalidateAll();
            lodImageCache.clear();
            activeLodLevel = LodLevel.LOD_0;
            pendingViewportInit = true;
            
            // Generate the base map image once
            MapGrid grid = model.getCurrentGrid();
            if (grid != null) {
                boolean showBorders = view.getEnableBordersToggle().isSelected();
                boolean showOverlay = view.getEnableKingdomOverlayToggle().isSelected();
                baseMapImage = createImageFromGrid(grid, showBorders, showOverlay);
            }
            
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
        if (baseGrid == null || baseMapImage == null) return;
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

        GraphicsContext gc = canvas.getGraphicsContext2D();
        // Fill out-of-bounds space with DEEP_OCEAN color
        gc.setFill(javafx.scene.paint.Color.web("#00008B"));
        gc.fillRect(0, 0, canvasW, canvasH);
        
        // Determine active image: cached LOD image or base map image fallback
        LodRegion region = computeRequiredLOD(canvasW, canvasH, baseGrid);
        Image activeImage;
        double sourceX, sourceY, sourceW, sourceH;

        if (region != null && lodImageCache.containsKey(region.cacheKey())) {
            activeImage = lodImageCache.get(region.cacheKey());
            int mult = region.lod.multiplier;
            double activePPC = pixelsPerCell / mult;
            
            sourceX = (viewOffsetX - region.wx0) * mult;
            sourceY = (viewOffsetY - region.wy0) * mult;
            sourceW = canvasW / activePPC;
            sourceH = canvasH / activePPC;
        } else {
            // Base grid fallback
            activeImage = baseMapImage;
            sourceX = viewOffsetX;
            sourceY = viewOffsetY;
            sourceW = canvasW / pixelsPerCell;
            sourceH = canvasH / pixelsPerCell;
        }

        double imgW = activeImage.getWidth();
        double imgH = activeImage.getHeight();

        // Calculate intersection to avoid stretching artifacts
        double intersectX = Math.max(0, sourceX);
        double intersectY = Math.max(0, sourceY);
        double intersectW = Math.min(imgW - intersectX, sourceW - (intersectX - sourceX));
        double intersectH = Math.min(imgH - intersectY, sourceH - (intersectY - sourceY));

        if (intersectW > 0 && intersectH > 0) {
            // Map cropped source coordinates to destination coordinates
            double destX = (intersectX - sourceX) * (canvasW / sourceW);
            double destY = (intersectY - sourceY) * (canvasH / sourceH);
            double destW = intersectW * (canvasW / sourceW);
            double destH = intersectH * (canvasH / sourceH);

            // Draw hardware-accelerated image properly cropped
            gc.drawImage(activeImage, intersectX, intersectY, intersectW, intersectH, destX, destY, destW, destH);
        }
        
        renderPOIs();
        renderLabels();
        view.getPOIListPanel().updatePOIList(baseGrid.getPointsOfInterest());
        view.getProvinceListPanel().updateProvinceList(baseGrid.getKingdoms());
    }

    private void renderLabels() {
        javafx.scene.Group canvasGroup = view.getCanvasGroup();
        java.util.List<com.mapbuilder.mapbuilder.core.map.MapLabel> labels = model.getLabels();

        // Evict cache entries for deleted labels
        java.util.IdentityHashMap<com.mapbuilder.mapbuilder.core.map.MapLabel, ?> alive = new java.util.IdentityHashMap<>();
        labels.forEach(l -> alive.put(l, null));
        labelNodeCache.keySet().retainAll(alive.keySet());
        canvasGroup.getChildren().removeIf(node -> node instanceof javafx.scene.text.Text);

        for (com.mapbuilder.mapbuilder.core.map.MapLabel label : labels) {
            javafx.scene.text.Text textNode = labelNodeCache.computeIfAbsent(label, l -> {
                javafx.scene.text.Text t = new javafx.scene.text.Text(l.getText());
                t.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
                t.setFill(javafx.scene.paint.Color.web("#FFFFFF"));

                javafx.scene.effect.DropShadow shadow = new javafx.scene.effect.DropShadow();
                shadow.setRadius(8.0);
                shadow.setSpread(0.4);
                shadow.setOffsetX(1);
                shadow.setOffsetY(1);
                shadow.setColor(javafx.scene.paint.Color.color(0, 0, 0, 0.85));
                t.setEffect(shadow);

                t.setMouseTransparent(true);
                return t;
            });

            textNode.setText(label.getText());

            double screenX = (label.getX() - viewOffsetX) * pixelsPerCell;
            double screenY = (label.getY() - viewOffsetY) * pixelsPerCell;

            textNode.setX(screenX - textNode.getLayoutBounds().getWidth() / 2);
            textNode.setY(screenY + textNode.getLayoutBounds().getHeight() / 4);

            if (!canvasGroup.getChildren().contains(textNode)) {
                canvasGroup.getChildren().add(textNode);
            }
        }
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
            
            boolean showBorders = view.getEnableBordersToggle().isSelected();
            boolean showOverlay = view.getEnableKingdomOverlayToggle().isSelected();
            lodImageCache.put(region.cacheKey(), createImageFromGrid(lodGrid, showBorders, showOverlay));
            
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

    private WritableImage createImageFromGrid(MapGrid grid, boolean showBorders, boolean showOverlay) {
        int w = grid.getWidth();
        int h = grid.getHeight();
        int[] pixels = buildColorCache(grid, w, h, showBorders, showOverlay);
        WritableImage image = new WritableImage(w, h);
        image.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbPreInstance(), pixels, 0, w);
        return image;
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

    private int getColorAt(int cx, int cy, MapGrid grid, boolean showBorders, boolean showOverlay) {
        if (cx < 0 || cx >= grid.getWidth() || cy < 0 || cy >= grid.getHeight()) {
            return 0xFF00008B;
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

    // ── Province Editing ─────────────────────────────────────────────────────

    /**
     * Called by {@link com.mapbuilder.mapbuilder.ui.ProvinceListPanel} whenever a
     * province colour is changed via the inline {@code ColorPicker}.
     * Rebuilds the cached map image so the new colour is immediately visible.
     */
    public void onProvinceColorChanged() {
        regenerateImages();
    }

    /**
     * Marks {@code kingdom} as the province to paint and highlights it in the
     * province list panel.
     */
    public void setSelectedKingdom(Kingdom kingdom) {
        selectedPaintKingdom = kingdom;
        view.getProvinceListPanel().selectKingdom(kingdom);
        if (kingdom != null) {
            view.getSelectedProvinceLabel().setText(kingdom.getName());
            view.getSelectedProvinceColorBox().setFill(toFXColor(kingdom.getColorARGB()));
        } else {
            view.getSelectedProvinceLabel().setText("None");
            view.getSelectedProvinceColorBox().setFill(javafx.scene.paint.Color.TRANSPARENT);
        }
    }

    /**
     * Returns the {@link Kingdom} whose territory covers the given screen
     * position, or {@code null} if the position is over water or outside the map.
     */
    private Kingdom getKingdomAtScreen(double screenX, double screenY) {
        MapGrid grid = model.getCurrentGrid();
        if (grid == null) return null;
        int cx = (int) (viewOffsetX + screenX / pixelsPerCell);
        int cy = (int) (viewOffsetY + screenY / pixelsPerCell);
        MapCell cell = grid.getCell(cx, cy);
        return cell != null ? cell.getKingdom() : null;
    }

    /**
     * Paints all land cells within the brush radius around the given screen
     * position to {@link #selectedPaintKingdom}.
     * Only land cells (elevation > waterLevel) are reassigned.
     */
    private void paintCellsAtScreen(double screenX, double screenY) {
        MapGrid grid = model.getCurrentGrid();
        if (grid == null || selectedPaintKingdom == null) return;

        double waterLevel = view.getWaterLevelSlider().getValue();
        int cx = (int) (viewOffsetX + screenX / pixelsPerCell);
        int cy = (int) (viewOffsetY + screenY / pixelsPerCell);
        int radius = (int) view.getBrushSizeSlider().getValue();

        boolean changed = false;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx * dx + dy * dy <= radius * radius) {
                    MapCell cell = grid.getCell(cx + dx, cy + dy);
                    if (cell != null && cell.getElevation() > waterLevel) {
                        if (cell.getKingdom() != selectedPaintKingdom) {
                            cell.setKingdom(selectedPaintKingdom);
                            minX = Math.min(minX, cx + dx);
                            minY = Math.min(minY, cy + dy);
                            maxX = Math.max(maxX, cx + dx);
                            maxY = Math.max(maxY, cy + dy);
                            changed = true;
                        }
                    }
                }
            }
        }

        if (changed) {
            updateImagePixels(minX, minY, maxX, maxY);
        }
    }

    private void paintCellsBetweenScreen(double x0, double y0, double x1, double y1) {
        MapGrid grid = model.getCurrentGrid();
        if (grid == null || selectedPaintKingdom == null) return;

        double waterLevel = view.getWaterLevelSlider().getValue();
        int radius = (int) view.getBrushSizeSlider().getValue();
        
        double dist = Math.hypot(x1 - x0, y1 - y0);
        // Paint every pixel cell step to ensure no gaps
        int steps = (int) Math.max(1, dist / (pixelsPerCell * Math.max(0.5, radius * 0.5)));
        
        boolean changed = false;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double sx = x0 + t * (x1 - x0);
            double sy = y0 + t * (y1 - y0);
            
            int cx = (int) (viewOffsetX + sx / pixelsPerCell);
            int cy = (int) (viewOffsetY + sy / pixelsPerCell);
            
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    if (dx * dx + dy * dy <= radius * radius) {
                        MapCell cell = grid.getCell(cx + dx, cy + dy);
                        if (cell != null && cell.getElevation() > waterLevel) {
                            if (cell.getKingdom() != selectedPaintKingdom) {
                                cell.setKingdom(selectedPaintKingdom);
                                minX = Math.min(minX, cx + dx);
                                minY = Math.min(minY, cy + dy);
                                maxX = Math.max(maxX, cx + dx);
                                maxY = Math.max(maxY, cy + dy);
                                changed = true;
                            }
                        }
                    }
                }
            }
        }
        
        if (changed) {
            updateImagePixels(minX, minY, maxX, maxY);
        }
    }

    private void updateImagePixels(int minX, int minY, int maxX, int maxY) {
        MapGrid grid = model.getCurrentGrid();
        if (grid == null || baseMapImage == null) return;
        boolean showBorders = view.getEnableBordersToggle().isSelected();
        boolean showOverlay = view.getEnableKingdomOverlayToggle().isSelected();
        
        // Expand bounding box slightly for borders
        minX = Math.max(0, minX - 1);
        minY = Math.max(0, minY - 1);
        maxX = Math.min(grid.getWidth() - 1, maxX + 1);
        maxY = Math.min(grid.getHeight() - 1, maxY + 1);
        
        PixelWriter writer = baseMapImage.getPixelWriter();
        for (int cy = minY; cy <= maxY; cy++) {
            for (int cx = minX; cx <= maxX; cx++) {
                writer.setArgb(cx, cy, getColorAt(cx, cy, grid, showBorders, showOverlay));
            }
        }
        // Invalidate LOD cache so zoomed-out views get the new pixels too
        lodImageCache.clear();
        renderMap();
    }

    private void annexEnclosedAreas() {
        MapGrid grid = model.getCurrentGrid();
        if (grid == null || selectedPaintKingdom == null) return;

        int w = grid.getWidth();
        int h = grid.getHeight();
        boolean[][] reachesEdge = new boolean[w][h];
        java.util.Queue<int[]> queue = new java.util.LinkedList<>();

        // 1. Add all edge cells that are not selectedPaintKingdom
        for (int x = 0; x < w; x++) {
            if (grid.getCell(x, 0).getKingdom() != selectedPaintKingdom) {
                reachesEdge[x][0] = true;
                queue.add(new int[]{x, 0});
            }
            if (grid.getCell(x, h - 1).getKingdom() != selectedPaintKingdom) {
                reachesEdge[x][h - 1] = true;
                queue.add(new int[]{x, h - 1});
            }
        }
        for (int y = 0; y < h; y++) {
            if (grid.getCell(0, y).getKingdom() != selectedPaintKingdom) {
                reachesEdge[0][y] = true;
                queue.add(new int[]{0, y});
            }
            if (grid.getCell(w - 1, y).getKingdom() != selectedPaintKingdom) {
                reachesEdge[w - 1][y] = true;
                queue.add(new int[]{w - 1, y});
            }
        }

        // 2. Flood fill
        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int cx = p[0], cy = p[1];
            for (int i = 0; i < 4; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h) {
                    if (!reachesEdge[nx][ny] && grid.getCell(nx, ny).getKingdom() != selectedPaintKingdom) {
                        reachesEdge[nx][ny] = true;
                        queue.add(new int[]{nx, ny});
                    }
                }
            }
        }

        // 3. Annex cells that couldn't reach the edge
        boolean changed = false;
        double waterLevel = view.getWaterLevelSlider().getValue();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!reachesEdge[x][y]) {
                    MapCell cell = grid.getCell(x, y);
                    if (cell.getKingdom() != selectedPaintKingdom && cell.getElevation() > waterLevel) {
                        cell.setKingdom(selectedPaintKingdom);
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                        changed = true;
                    }
                }
            }
        }

        if (changed) {
            updateImagePixels(minX, minY, maxX, maxY);
        }
    }
}
