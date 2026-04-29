package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.map.MapCell;
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

public class MainPresenter {
    
    public static final int COLOR_RIVER = 0xFF00BFFF; // Deep Sky Blue
    public static final int COLOR_LAKE = 0xFF1E90FF;  // Dodger Blue

    private MainView view;
    private final MainModel model;
    private final PauseTransition debounce;
    private com.mapbuilder.mapbuilder.core.map.MapLabel draggedLabel = null;
    private Image spriteSheet; // Cached sprite sheet for POI rendering

    public MainPresenter() {
        this.model = new MainModel();
        this.debounce = new PauseTransition(Duration.millis(300));
        this.debounce.setOnFinished(e -> generateMapAsync());
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

        view.getCanvasGroup().setOnMouseClicked(event -> {
            double x = event.getX();
            double y = event.getY();
            com.mapbuilder.mapbuilder.core.map.MapLabel clickedLabel = getLabelAt(x, y);

            if (event.getClickCount() == 2) {
                if (clickedLabel != null) {
                    final com.mapbuilder.mapbuilder.core.map.MapLabel finalLabel = clickedLabel;
                    if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                        model.removeLabel(finalLabel);
                        renderMap();
                    } else {
                        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(finalLabel.getText());
                        dialog.setTitle("Edit Map Label");
                        dialog.setHeaderText("Edit the label text:");
                        dialog.setContentText("Label:");
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
                        dialog.showAndWait().ifPresent(text -> {
                            model.addLabel(new com.mapbuilder.mapbuilder.core.map.MapLabel(text, x, y));
                            renderMap();
                        });
                    }
                }
            } else if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY && event.getClickCount() == 1) {
                if (clickedLabel != null) {
                     model.removeLabel(clickedLabel);
                     renderMap();
                }
            }
        });

        view.getCanvasGroup().setOnMousePressed(event -> {
            if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                double x = event.getX();
                double y = event.getY();
                com.mapbuilder.mapbuilder.core.map.MapLabel label = getLabelAt(x, y);
                if (label != null) {
                    draggedLabel = label;
                    event.consume();
                }
            }
        });

        view.getCanvasGroup().setOnMouseDragged(event -> {
            if (draggedLabel != null) {
                draggedLabel.setX(event.getX());
                draggedLabel.setY(event.getY());
                renderMap();
                event.consume();
            }
        });

        view.getCanvasGroup().setOnMouseReleased(event -> {
            draggedLabel = null;
        });
        
        // Wire POI density sliders to trigger generation
        setupPOIDensityListeners();
    }

    private com.mapbuilder.mapbuilder.core.map.MapLabel getLabelAt(double x, double y) {
        if (model.getCurrentGrid() == null) return null;
        double hitRadiusX = 40.0;
        double hitRadiusY = 20.0;
        for (com.mapbuilder.mapbuilder.core.map.MapLabel label : model.getLabels()) {
            if (Math.abs(label.getX() - x) < hitRadiusX && Math.abs(label.getY() - y) < hitRadiusY) {
                return label;
            }
        }
        return null;
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

        task.setOnSucceeded(e -> renderMap());
        new Thread(task).start();
    }

    private void renderMap() {
        MapGrid grid = model.getCurrentGrid();
        if (grid == null) return;
        Canvas canvas = view.getCanvas();
        Canvas poiCanvas = view.getPoiCanvas();
        
        int width = grid.getWidth();
        int height = grid.getHeight();
        boolean needsCentering = false;
        if (canvas.getWidth() != width || canvas.getHeight() != height) {
            canvas.setWidth(width);
            canvas.setHeight(height);
            poiCanvas.setWidth(width);
            poiCanvas.setHeight(height);
            needsCentering = true;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        PixelWriter pixelWriter = gc.getPixelWriter();

        boolean showBorders = view.getEnableBordersToggle().isSelected();
        boolean showOverlay = view.getEnableKingdomOverlayToggle().isSelected();

        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                MapCell cell = grid.getCell(x, y);
                int color = cell.getMixedColorARGB();
                
                if (cell.isLake()) {
                    color = COLOR_LAKE;
                } else if (cell.isRiver()) {
                    color = COLOR_RIVER;
                }

                if (cell.getKingdom() != null) {
                    if (showOverlay) {
                        int kColor = cell.getKingdom().getColorARGB();
                        int a = (kColor >> 24) & 0xFF;
                        int r = (kColor >> 16) & 0xFF;
                        int g = (kColor >> 8) & 0xFF;
                        int b = kColor & 0xFF;
                        int cr = (color >> 16) & 0xFF;
                        int cg = (color >> 8) & 0xFF;
                        int cb = color & 0xFF;
                        cr = (cr + r) / 2;
                        cg = (cg + g) / 2;
                        cb = (cb + b) / 2;
                        color = (0xFF << 24) | (cr << 16) | (cg << 8) | cb;
                    }

                    if (showBorders) {
                        boolean isBorder = false;
                        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
                        for (int[] dir : dirs) {
                            int nx = x + dir[0];
                            int ny = y + dir[1];
                            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                                MapCell neighbor = grid.getCell(nx, ny);
                                if (neighbor.getKingdom() != cell.getKingdom()) {
                                    isBorder = true;
                                    break;
                                }
                            }
                        }
                        if (isBorder) {
                            color = 0xFF000000; // Black border
                        }
                    }
                }
                
                pixels[y * width + x] = color;
            }
        }

        pixelWriter.setPixels(0, 0, width, height, PixelFormat.getIntArgbPreInstance(), pixels, 0, width);

        // Update map labels using JavaFX nodes to keep them crisp and fixed-size during zoom
        javafx.scene.Group canvasGroup = view.getCanvasGroup();
        canvasGroup.getChildren().removeIf(node -> node instanceof javafx.scene.layout.StackPane);

        for (com.mapbuilder.mapbuilder.core.map.MapLabel label : model.getLabels()) {
            javafx.scene.text.Text textNode = new javafx.scene.text.Text(label.getText());
            textNode.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
            textNode.setFill(javafx.scene.paint.Color.web("#FFFFFF"));

            // Use a dense soft glow instead of a hard pixelated stroke
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setRadius(5.0);
            glow.setSpread(0.8);
            glow.setOffsetX(0);
            glow.setOffsetY(0);
            glow.setColor(javafx.scene.paint.Color.color(0, 0, 0, 1.0));
            textNode.setEffect(glow);

            javafx.scene.layout.StackPane labelContainer = new javafx.scene.layout.StackPane(textNode);
            
            // Add a semi-transparent dark background "pill" to ensure it's always readable
            labelContainer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6); -fx-background-radius: 12px; -fx-padding: 4px 10px;");
            
            labelContainer.setLayoutX(label.getX());
            labelContainer.setLayoutY(label.getY());
            
            // Dynamically center the container based on its actual width/height
            labelContainer.translateXProperty().bind(labelContainer.widthProperty().divide(-2));
            labelContainer.translateYProperty().bind(labelContainer.heightProperty().divide(-2));
            
            // Inverse scale the container so the label stays the same visual size when zooming in/out
            labelContainer.scaleXProperty().bind(javafx.beans.binding.Bindings.divide(1.0, canvasGroup.scaleXProperty()));
            labelContainer.scaleYProperty().bind(javafx.beans.binding.Bindings.divide(1.0, canvasGroup.scaleYProperty()));

            // Pass mouse events down to the canvas
            labelContainer.setMouseTransparent(true);
            
            canvasGroup.getChildren().add(labelContainer);
        }

        
        // Render POI overlay after main map
        renderPOIs();
        
        // Update POI list panel
        view.getPOIListPanel().updatePOIList(grid.getPointsOfInterest());
        
        if (needsCentering) {
            Platform.runLater(() -> view.centerMap());
        }
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

        // Render each POI
        for (PointOfInterest poi : pois) {
            // Convert map coordinates to screen coordinates
            // (In a real app with zoom/pan, this would apply scale and translation)
            double screenX = poi.getX();
            double screenY = poi.getY();

            // Skip if off-screen
            if (screenX < 0 || screenX >= width || screenY < 0 || screenY >= height) {
                continue;
            }

            // Get color for this POI
            Integer customColor = poi.getCustomColor();
            int poiColor = customColor != null ? customColor : POIIconMapper.getDefaultColor(poi.getType());

            // Draw colored circle (12-16px diameter, using 14px)
            gc.setFill(toFXColor(poiColor));
            gc.fillOval(screenX - 7, screenY - 7, 14, 14);

            // Draw sprite icon (16x16px centered on circle)
            int[] spriteCoords = POIIconMapper.getSpriteCoordinates(poi.getType());
            if (spriteCoords != null && spriteSheet != null && !spriteSheet.isError()) {
                gc.drawImage(
                    spriteSheet,
                    spriteCoords[0], spriteCoords[1], 32, 32,  // Source rect (32x32 from 512x512 sheet)
                    screenX - 8, screenY - 8, 16, 16           // Destination rect (16x16 on canvas)
                );
            }

            // Check if mouse is hovering over this POI (within 20px)
            double distance = Math.sqrt(
                Math.pow(mouseX - screenX, 2) + Math.pow(mouseY - screenY, 2)
            );
            if (distance < 20 && (hoveredPOI == null || distance < 
                Math.sqrt(Math.pow(mouseX - hoveredPOI.getX(), 2) + Math.pow(mouseY - hoveredPOI.getY(), 2)))) {
                hoveredPOI = poi;
            }
        }

        // Render label for hovered POI
        if (hoveredPOI != null) {
            double labelX = hoveredPOI.getX();
            double labelY = hoveredPOI.getY() - 20; // Above the POI

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

