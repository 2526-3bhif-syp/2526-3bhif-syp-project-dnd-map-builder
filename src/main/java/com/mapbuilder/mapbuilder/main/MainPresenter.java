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
        view.getWaterLevelSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getTempBiasSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getRainBiasSlider().valueProperty().addListener((obs, oldV, newV) -> triggerGeneration());
        view.getSeedField().textProperty().addListener((obs, oldV, newV) -> triggerGeneration());
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
        
        double waterLevel = view.getWaterLevelSlider().getValue();
        double tempBias = view.getTempBiasSlider().getValue();
        double rainBias = view.getRainBiasSlider().getValue();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                model.generateMap(seed, waterLevel, tempBias, rainBias);
                return null;
            }
        };

        task.setOnSucceeded(e -> renderMap());
        new Thread(task).start();
    }

    private void renderMap() {
        MapGrid grid = model.getCurrentGrid();
        Canvas canvas = view.getCanvas();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        PixelWriter pixelWriter = gc.getPixelWriter();

        int width = grid.getWidth();
        int height = grid.getHeight();
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                MapCell cell = grid.getCell(x, y);
                pixels[y * width + x] = cell.getMixedColorARGB();
            }
        }

        pixelWriter.setPixels(0, 0, width, height, PixelFormat.getIntArgbPreInstance(), pixels, 0, width);
    }
}
