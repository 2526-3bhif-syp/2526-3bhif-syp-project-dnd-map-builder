package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.MVPBase;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

public class MainView extends BorderPane implements MVPBase.View {
    
    private final Canvas canvas;
    private final Pane canvasContainer;

    public MainView() {
        // Left Panel (Generator Settings)
        VBox leftPanel = new VBox(10);
        leftPanel.setPrefWidth(250);
        leftPanel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10;");
        leftPanel.getChildren().addAll(
            new Label("Generator Settings"),
            new TextField("Seed..."),
            new TabPane(new Tab("Tab 1"), new Tab("Tab 2")),
            new Slider(0, 100, 50),
            new Button("Generate")
        );
        this.setLeft(leftPanel);

        // Center Panel (Canvas Container)
        canvasContainer = new Pane();
        canvasContainer.setStyle("-fx-background-color: #ffffff;");
        canvas = new Canvas(800, 600);
        canvasContainer.getChildren().add(canvas);
        
        // Resize canvas to fit container
        canvas.widthProperty().bind(canvasContainer.widthProperty());
        canvas.heightProperty().bind(canvasContainer.heightProperty());
        
        this.setCenter(canvasContainer);

        // Right Panel
        VBox rightPanel = new VBox(10);
        rightPanel.setPrefWidth(250);
        rightPanel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10;");

        // Top Action Bar in Right Panel
        HBox topActionBar = new HBox(5);
        topActionBar.getChildren().addAll(
            new Button("Speichern"),
            new Button("Laden"),
            new Button("Export"),
            new Button("Drucken")
        );
        
        // Bottom Layers Panel in Right Panel
        VBox layersPanel = new VBox(5);
        layersPanel.getChildren().addAll(
            new Label("Layers"),
            new Button("Toggle Layer 1")
        );

        rightPanel.getChildren().addAll(topActionBar, layersPanel);
        this.setRight(rightPanel);
    }
}
