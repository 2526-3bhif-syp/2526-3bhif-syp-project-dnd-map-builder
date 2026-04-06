package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.MVPBase;
import com.mapbuilder.mapbuilder.core.map.MapCell;
import com.mapbuilder.mapbuilder.core.map.MapGrid;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.util.Duration;

public class MainPresenter implements MVPBase.Presenter<MainView> {
    
    private MainView view;
    private final MainModel model;
    private final PauseTransition debounce;

    public MainPresenter() {
        this.model = new MainModel();
        this.debounce = new PauseTransition(Duration.millis(300));
        this.debounce.setOnFinished(e -> generateMapAsync());
    }

    @Override
    public void setView(MainView view) {
        this.view = view;
        bind();
        triggerGeneration(); // initial generation
    }

    @Override
    public MainView getView() {
        return view;
    }

    private void bind() {
        view.getSizeSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getOctavesSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getScaleSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getSeedField().textProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getFalloffSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getWaterLevelSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getTempBiasSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getRainBiasSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getRiverCountSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
    }

    private void triggerGeneration() {
        debounce.playFromStart();
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
        final int riverCount = (int) view.getRiverCountSlider().getValue();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                model.generateMap(seed, size, octaves, scale, falloff, waterLevel, tempBias, rainBias, riverCount);
                return null;
            }
        };

        task.setOnSucceeded(e -> renderMap());
        new Thread(task).start();
    }

    private void renderMap() {
        MapGrid grid = model.getCurrentGrid();
        Canvas canvas = view.getCanvas();
        
        int width = grid.getWidth();
        int height = grid.getHeight();
        if (canvas.getWidth() != width || canvas.getHeight() != height) {
            canvas.setWidth(width);
            canvas.setHeight(height);
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        PixelWriter pixelWriter = gc.getPixelWriter();

        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                MapCell cell = grid.getCell(x, y);
                if (cell.isLake()) {
                    pixels[y * width + x] = 0xFF1E90FF; // Dodger Blue
                } else if (cell.isRiver()) {
                    pixels[y * width + x] = 0xFF00BFFF; // Deep Sky Blue
                } else {
                    pixels[y * width + x] = cell.getMixedColorARGB();
                }
            }
        }

        pixelWriter.setPixels(0, 0, width, height, PixelFormat.getIntArgbPreInstance(), pixels, 0, width);
    }
}
