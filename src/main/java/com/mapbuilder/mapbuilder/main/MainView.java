package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.MVPBase;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;

public class MainView extends AnchorPane implements MVPBase.View {
    
    private final Canvas canvas;
    private final Pane canvasContainer;
    private final TextField seedField;
    private final Slider waterLevelSlider;
    private final Slider tempBiasSlider;
    private final Slider rainBiasSlider;

    public MainView() {
        // Center Panel (Canvas Container) - Now at the back of the AnchorPane
        canvasContainer = new Pane();
        canvasContainer.setStyle("-fx-background-color: #ffffff;");
        canvas = new Canvas(800, 600);
        canvasContainer.getChildren().add(canvas);
        
        // Resize canvas to fit container
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());
        
        AnchorPane.setTopAnchor(canvasContainer, 0.0);
        AnchorPane.setBottomAnchor(canvasContainer, 0.0);
        AnchorPane.setLeftAnchor(canvasContainer, 0.0);
        AnchorPane.setRightAnchor(canvasContainer, 0.0);

        // Left Panel (Generator Settings) - Floating
        VBox leftPanel = new VBox(10);
        leftPanel.setPrefWidth(250);
        leftPanel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 15; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        
        TabPane tabPane = new TabPane(new Tab("Tab 1"), new Tab("Tab 2"));
        tabPane.setStyle("-fx-background-color: transparent;");

        seedField = new TextField("12345");
        waterLevelSlider = new Slider(-0.5, 0.5, 0.0);
        waterLevelSlider.setShowTickMarks(true);
        tempBiasSlider = new Slider(-1.0, 1.0, 0.0);
        tempBiasSlider.setShowTickMarks(true);
        rainBiasSlider = new Slider(-1.0, 1.0, 0.0);
        rainBiasSlider.setShowTickMarks(true);

        leftPanel.getChildren().addAll(
            new Label("Generator Settings"),
            new Label("Seed"),
            seedField,
            new Label("Water Level"),
            waterLevelSlider,
            new Label("Temperature"),
            tempBiasSlider,
            new Label("Rainfall"),
            rainBiasSlider,
            tabPane,
            new Button("Generate")
        );
        AnchorPane.setTopAnchor(leftPanel, 10.0);
        AnchorPane.setLeftAnchor(leftPanel, 10.0);
        AnchorPane.setBottomAnchor(leftPanel, 10.0);

        // Top Right Panel (Actions) - Floating
        HBox topActionBar = new HBox(10); // increased spacing
        topActionBar.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 15; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        
        Button saveBtn = new Button("Save");
        saveBtn.setMinWidth(70);
        Button loadBtn = new Button("Load");
        loadBtn.setMinWidth(70);
        Button exportBtn = new Button("Export");
        exportBtn.setMinWidth(70);
        Button printBtn = new Button("Print");
        printBtn.setMinWidth(70);
        
        topActionBar.getChildren().addAll(saveBtn, loadBtn, exportBtn, printBtn);
        AnchorPane.setTopAnchor(topActionBar, 10.0);
        AnchorPane.setRightAnchor(topActionBar, 10.0);
        
        // Bottom Right Layers Panel - Floating
        VBox layersPanel = new VBox(10);
        layersPanel.setPrefWidth(200);
        layersPanel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 15; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        layersPanel.getChildren().addAll(
            new Label("Layers"),
            new Button("Toggle Layer 1")
        );
        AnchorPane.setBottomAnchor(layersPanel, 10.0);
        AnchorPane.setRightAnchor(layersPanel, 10.0);

        // Add everything to the AnchorPane
        this.getChildren().addAll(canvasContainer, leftPanel, topActionBar, layersPanel);
    }

    @Override
    public Canvas getCanvas() {
        return canvas;
    }

    @Override
    public TextField getSeedField() {
        return seedField;
    }

    @Override
    public Slider getWaterLevelSlider() {
        return waterLevelSlider;
    }

    @Override
    public Slider getTempBiasSlider() {
        return tempBiasSlider;
    }

    @Override
    public Slider getRainBiasSlider() {
        return rainBiasSlider;
    }
}
