package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.MVPBase;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainView extends BorderPane implements MVPBase.View {
    
    private final Canvas canvas;
    private final ScrollPane scrollPane;
    private final TextField seedField;
    
    private final Slider sizeSlider;
    private final Slider octavesSlider;
    private final Slider scaleSlider;

    public MainView() {
        // Center Panel (Map Canvas)
        canvas = new Canvas(800, 800);
        StackPane canvasWrapper = new StackPane(canvas);
        canvasWrapper.setStyle("-fx-background-color: #333333;");
        canvasWrapper.setPadding(new Insets(20)); // Buffer around the map
        
        scrollPane = new ScrollPane(canvasWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: #2b2b2b; -fx-border-color: #2b2b2b;");
        
        this.setCenter(scrollPane);

        // Left Panel (Generator Settings)
        VBox leftPanel = new VBox(10);
        leftPanel.setPrefWidth(260);
        leftPanel.setPadding(new Insets(15));
        leftPanel.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 1 0 0;");
        
        seedField = new TextField("12345");
        
        sizeSlider = new Slider(200, 2000, 800);
        sizeSlider.setShowTickMarks(true);
        sizeSlider.setShowTickLabels(true);
        sizeSlider.setMajorTickUnit(400);

        octavesSlider = new Slider(1, 10, 5);
        octavesSlider.setShowTickMarks(true);
        octavesSlider.setShowTickLabels(true);
        octavesSlider.setMajorTickUnit(1);
        octavesSlider.setMinorTickCount(0);
        octavesSlider.setSnapToTicks(true);

        scaleSlider = new Slider(0.001, 0.05, 0.01);
        scaleSlider.setShowTickMarks(true);
        scaleSlider.setShowTickLabels(true);
        scaleSlider.setMajorTickUnit(0.01);

        leftPanel.getChildren().addAll(
            new Label("Generator Settings"),
            new Label("Seed"),
            seedField,
            new Label("Map Size"),
            sizeSlider,
            new Label("Octaves (Detail Level)"),
            octavesSlider,
            new Label("Scale (Zoom Level)"),
            scaleSlider,
            new Button("Generate")
        );
        this.setLeft(leftPanel);

        // Top Panel (Actions)
        HBox topActionBar = new HBox(15);
        topActionBar.setPadding(new Insets(10, 15, 10, 15));
        topActionBar.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");
        topActionBar.setAlignment(Pos.CENTER_RIGHT);
        
        Button saveBtn = new Button("Save");
        Button loadBtn = new Button("Load");
        Button exportBtn = new Button("Export");
        Button printBtn = new Button("Print");
        
        topActionBar.getChildren().addAll(saveBtn, loadBtn, exportBtn, printBtn);
        this.setTop(topActionBar);
    }

    @Override
    public Canvas getCanvas() {
        return canvas;
    }

    public TextField getSeedField() {
        return seedField;
    }

    public Slider getSizeSlider() {
        return sizeSlider;
    }

    public Slider getOctavesSlider() {
        return octavesSlider;
    }

    public Slider getScaleSlider() {
        return scaleSlider;
    }
}
