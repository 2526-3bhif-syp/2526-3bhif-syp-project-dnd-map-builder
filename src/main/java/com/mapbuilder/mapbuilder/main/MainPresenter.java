package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.MVPBase;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
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
        int seed;
        try {
            seed = Integer.parseInt(view.getSeedField().getText());
        } catch (NumberFormatException e) {
            seed = view.getSeedField().getText().hashCode();
        }
        
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
        // implemented in next task
    }
}
