package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.map.MapCell;
import com.mapbuilder.mapbuilder.core.map.MapGrid;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.util.Duration;

import java.util.concurrent.ThreadLocalRandom;

public class MainPresenter {
    
    public static final int COLOR_RIVER = 0xFF00BFFF; // Deep Sky Blue
    public static final int COLOR_LAKE = 0xFF1E90FF;  // Dodger Blue

    private MainView view;
    private final MainModel model;
    private final PauseTransition debounce;

    public MainPresenter() {
        this.model = new MainModel();
        this.debounce = new PauseTransition(Duration.millis(300));
        this.debounce.setOnFinished(e -> generateMapAsync());
    }

    public void setView(MainView view) {
        this.view = view;
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
                model.generateMap(seed, size, octaves, scale, falloff, waterLevel, tempBias, rainBias,
                                  enableRivers, enableLakes, riverDensity, lakeSize, minLakeArea,
                                  kingdomCount, lloydPasses);
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
        
        int width = grid.getWidth();
        int height = grid.getHeight();
        boolean needsCentering = false;
        if (canvas.getWidth() != width || canvas.getHeight() != height) {
            canvas.setWidth(width);
            canvas.setHeight(height);
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
        if (needsCentering) {
            Platform.runLater(() -> view.centerMap());
        }
    }
}

