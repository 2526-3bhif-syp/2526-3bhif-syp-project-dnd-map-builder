package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.MVPBase;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class MainView extends AnchorPane implements MVPBase.View {
    
    private final Canvas canvas;
    private final Pane canvasContainer;
    private final TextField seedField;
    
    private final Slider sizeSlider;
    private final Slider octavesSlider;
    private final Slider scaleSlider;
    private final Slider falloffSlider;
    private final Slider waterLevelSlider;
    private final Slider tempBiasSlider;
    private final Slider rainBiasSlider;

    public MainView() {
        // Center Panel (Canvas Container) - Now at the back of the AnchorPane
        canvasContainer = new Pane();
        canvasContainer.setStyle("-fx-background-color: #333333;");
        canvas = new Canvas(800, 800);
        
        Group canvasGroup = new Group(canvas);
        canvasContainer.getChildren().add(canvasGroup);
        
        // Center the canvas inside the canvas container manually (optional) or let the generator resize it
        
        AnchorPane.setTopAnchor(canvasContainer, 0.0);
        AnchorPane.setBottomAnchor(canvasContainer, 0.0);
        AnchorPane.setLeftAnchor(canvasContainer, 0.0);
        AnchorPane.setRightAnchor(canvasContainer, 0.0);
        
        // Zoom functionality
        canvasContainer.setOnScroll(event -> {
            double zoomFactor = 1.05;
            double deltaY = event.getDeltaY();
            if (deltaY < 0) {
                zoomFactor = 1 / zoomFactor;
            }
            double newScaleX = canvasGroup.getScaleX() * zoomFactor;
            double newScaleY = canvasGroup.getScaleY() * zoomFactor;
            
            newScaleX = Math.max(0.1, Math.min(newScaleX, 10.0));
            newScaleY = Math.max(0.1, Math.min(newScaleY, 10.0));
            
            canvasGroup.setScaleX(newScaleX);
            canvasGroup.setScaleY(newScaleY);
            event.consume();
        });

        // Left Panel (Generator Settings) - Floating
        VBox leftPanel = new VBox(10);
        leftPanel.setPrefWidth(260);
        leftPanel.setPadding(new Insets(15));
        leftPanel.setStyle("-fx-background-color: #f4f4f4; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        
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

        scaleSlider = new Slider(0.001, 0.05, 0.005);
        scaleSlider.setShowTickMarks(true);
        scaleSlider.setShowTickLabels(true);
        scaleSlider.setMajorTickUnit(0.01);

        falloffSlider = new Slider(-1.0, 1.0, 0.0);
        falloffSlider.setShowTickMarks(true);
        falloffSlider.setShowTickLabels(true);
        falloffSlider.setMajorTickUnit(0.5);

        waterLevelSlider = new Slider(-1.0, 1.0, 0.0);
        waterLevelSlider.setShowTickMarks(true);
        waterLevelSlider.setShowTickLabels(true);
        waterLevelSlider.setMajorTickUnit(0.5);

        tempBiasSlider = new Slider(-0.5, 0.5, 0.0);
        tempBiasSlider.setShowTickMarks(true);
        tempBiasSlider.setShowTickLabels(true);
        tempBiasSlider.setMajorTickUnit(0.25);

        rainBiasSlider = new Slider(-0.5, 0.5, 0.0);
        rainBiasSlider.setShowTickMarks(true);
        rainBiasSlider.setShowTickLabels(true);
        rainBiasSlider.setMajorTickUnit(0.25);

        leftPanel.getChildren().addAll(
            new Label("Generator Settings"),
            new Label("Seed"), seedField,
            new Label("Map Size"), sizeSlider,
            new Label("Octaves (Detail Level)"), octavesSlider,
            new Label("Scale (Zoom Level)"), scaleSlider,
            new Label("Island Falloff"), falloffSlider,
            new Label("Sea Level"), waterLevelSlider,
            new Label("Temperature Bias"), tempBiasSlider,
            new Label("Rainfall Bias"), rainBiasSlider,
            new Button("Generate")
        );
        
        ScrollPane leftScroll = new ScrollPane(leftPanel);
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        leftScroll.setPrefViewportHeight(600);
        
        AnchorPane.setTopAnchor(leftScroll, 10.0);
        AnchorPane.setLeftAnchor(leftScroll, 10.0);
        AnchorPane.setBottomAnchor(leftScroll, 10.0);

        // Top Right Panel (Actions) - Floating
        HBox topActionBar = new HBox(15);
        topActionBar.setPadding(new Insets(10, 15, 10, 15));
        topActionBar.setStyle("-fx-background-color: #f4f4f4; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        topActionBar.setAlignment(Pos.CENTER_RIGHT);
        
        Button saveBtn = new Button("Save");
        Button loadBtn = new Button("Load");
        Button exportBtn = new Button("Export");
        Button printBtn = new Button("Print");
        
        topActionBar.getChildren().addAll(saveBtn, loadBtn, exportBtn, printBtn);
        AnchorPane.setTopAnchor(topActionBar, 10.0);
        AnchorPane.setRightAnchor(topActionBar, 10.0);

        // Bottom Right Layers Panel - Floating
        VBox layersPanel = new VBox(10);
        layersPanel.setPrefWidth(200);
        layersPanel.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 15; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        layersPanel.getChildren().addAll(
            new Label("Layers"),
            new Button("Toggle Layer 1")
        );
        AnchorPane.setBottomAnchor(layersPanel, 10.0);
        AnchorPane.setRightAnchor(layersPanel, 10.0);

        // Add everything to the AnchorPane
        this.getChildren().addAll(canvasContainer, leftScroll, topActionBar, layersPanel);
    }

    @Override
    public Canvas getCanvas() { return canvas; }
    @Override
    public TextField getSeedField() { return seedField; }
    @Override
    public Slider getSizeSlider() { return sizeSlider; }
    @Override
    public Slider getOctavesSlider() { return octavesSlider; }
    @Override
    public Slider getScaleSlider() { return scaleSlider; }
    @Override
    public Slider getFalloffSlider() { return falloffSlider; }
    @Override
    public Slider getWaterLevelSlider() { return waterLevelSlider; }
    @Override
    public Slider getTempBiasSlider() { return tempBiasSlider; }
    @Override
    public Slider getRainBiasSlider() { return rainBiasSlider; }
}
