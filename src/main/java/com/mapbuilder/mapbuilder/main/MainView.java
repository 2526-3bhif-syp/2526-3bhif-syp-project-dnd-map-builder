package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.core.MVPBase;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

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

        // Panning functionality
        final double[] dragStart = new double[2];
        canvasContainer.setOnMousePressed(event -> {
            dragStart[0] = event.getSceneX() - canvasGroup.getTranslateX();
            dragStart[1] = event.getSceneY() - canvasGroup.getTranslateY();
        });
        
        canvasContainer.setOnMouseDragged(event -> {
            canvasGroup.setTranslateX(event.getSceneX() - dragStart[0]);
            canvasGroup.setTranslateY(event.getSceneY() - dragStart[1]);
        });

        // Left Panel (Generator Settings) - Floating
        VBox leftPanel = new VBox(10);
        leftPanel.setPrefWidth(260);
        leftPanel.setPadding(new Insets(15));
        leftPanel.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");
        leftPanel.getStyleClass().add("left-panel");
        
        // Removed broken GaussianBlur as it blurs the panel itself, not the background behind it.

        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label("Generator Settings");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px;");
        Button collapseLeftBtn = new Button("<<");
        collapseLeftBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: white;");
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        headerBox.getChildren().addAll(headerLabel, spacer, collapseLeftBtn);

        TabPane tabPane = new TabPane(new Tab("Tab 1"), new Tab("Tab 2"));
        tabPane.setStyle("-fx-background-color: transparent;");

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

        Label seedLabel = new Label("Seed"); seedLabel.setStyle("-fx-text-fill: white;");
        Label mapSizeLabel = new Label("Map Size"); mapSizeLabel.setStyle("-fx-text-fill: white;");
        Label octavesLabel = new Label("Octaves (Detail Level)"); octavesLabel.setStyle("-fx-text-fill: white;");
        Label scaleLabel = new Label("Scale (Zoom Level)"); scaleLabel.setStyle("-fx-text-fill: white;");
        Label falloffLabel = new Label("Island Falloff"); falloffLabel.setStyle("-fx-text-fill: white;");
        Label seaLevelLabel = new Label("Sea Level"); seaLevelLabel.setStyle("-fx-text-fill: white;");
        Label tempBiasLabel = new Label("Temperature Bias"); tempBiasLabel.setStyle("-fx-text-fill: white;");
        Label rainBiasLabel = new Label("Rainfall Bias"); rainBiasLabel.setStyle("-fx-text-fill: white;");
        
        Button generateBtn = new Button("Generate");
        generateBtn.setStyle("-fx-background-color: #3c3f41; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");

        leftPanel.getChildren().addAll(
            headerBox,
            seedLabel, seedField,
            tabPane,
            mapSizeLabel, sizeSlider,
            octavesLabel, octavesSlider,
            scaleLabel, scaleSlider,
            falloffLabel, falloffSlider,
            seaLevelLabel, waterLevelSlider,
            tempBiasLabel, tempBiasSlider,
            rainBiasLabel, rainBiasSlider,
            generateBtn
        );
        
        ScrollPane leftScroll = new ScrollPane(leftPanel);
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        leftScroll.setPrefViewportHeight(600);
        
        AnchorPane.setTopAnchor(leftScroll, 10.0);
        AnchorPane.setLeftAnchor(leftScroll, 10.0);
        AnchorPane.setBottomAnchor(leftScroll, 10.0);

        Button showLeftBtn = new Button(">>");
        showLeftBtn.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white; -fx-background-radius: 0 8 8 0; -fx-padding: 10 5; -fx-cursor: hand;");
        showLeftBtn.setVisible(false);
        AnchorPane.setTopAnchor(showLeftBtn, 10.0);
        AnchorPane.setLeftAnchor(showLeftBtn, 0.0);

        collapseLeftBtn.setOnAction(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), leftScroll);
            tt.setToX(-300);
            tt.setOnFinished(evt -> showLeftBtn.setVisible(true));
            tt.play();
        });
        
        showLeftBtn.setOnAction(e -> {
            showLeftBtn.setVisible(false);
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), leftScroll);
            tt.setToX(0);
            tt.play();
        });

        // Top Right Panel (Actions) - Floating
        HBox topActionBar = new HBox(15);
        topActionBar.setPadding(new Insets(10, 15, 10, 15));
        topActionBar.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");
        topActionBar.setAlignment(Pos.CENTER_RIGHT);
        
        String btnStyle = "-fx-background-color: #3c3f41; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;";
        Button saveBtn = new Button("Save"); saveBtn.setStyle(btnStyle);
        Button loadBtn = new Button("Load"); loadBtn.setStyle(btnStyle);
        Button exportBtn = new Button("Export"); exportBtn.setStyle(btnStyle);
        Button printBtn = new Button("Print"); printBtn.setStyle(btnStyle);
        
        topActionBar.getChildren().addAll(saveBtn, loadBtn, exportBtn, printBtn);
        AnchorPane.setTopAnchor(topActionBar, 10.0);
        AnchorPane.setRightAnchor(topActionBar, 10.0);

        
        // Right Layers Panel - Floating
        VBox layersPanel = new VBox(10);
        layersPanel.setPrefWidth(250);
        layersPanel.setStyle("-fx-background-color: #2b2b2b; -fx-padding: 15; -fx-background-radius: 8 0 0 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");
        
        HBox layersHeaderBox = new HBox();
        layersHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label layersHeaderLabel = new Label("Layers");
        layersHeaderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px;");
        Button collapseRightBtn = new Button(">");
        collapseRightBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: white;");
        Pane rightSpacer = new Pane();
        HBox.setHgrow(rightSpacer, javafx.scene.layout.Priority.ALWAYS);
        layersHeaderBox.getChildren().addAll(collapseRightBtn, rightSpacer, layersHeaderLabel);

        layersPanel.getChildren().add(layersHeaderBox);

        String[] layerNames = {"Markierungen", "Punkte von Interesse", "Strukturen & Straßen", "Berge", "Flüsse und Seen", "Grid"};
        ToggleButton[] layerToggles = new ToggleButton[layerNames.length];
        
        for (int i = 0; i < layerNames.length; i++) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #3c3f41; -fx-padding: 8; -fx-background-radius: 5;");
            
            Label nameLabel = new Label(layerNames[i]);
            nameLabel.setStyle("-fx-text-fill: white;");
            
            Pane rowSpacer = new Pane();
            HBox.setHgrow(rowSpacer, javafx.scene.layout.Priority.ALWAYS);
            
            ToggleButton toggle = new ToggleButton("(o)");
            toggle.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
            toggle.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    toggle.setText("(/)");
                } else {
                    toggle.setText("(o)");
                }
            });
            layerToggles[i] = toggle;
            
            row.getChildren().addAll(nameLabel, rowSpacer, toggle);
            layersPanel.getChildren().add(row);
        }
        
        AnchorPane.setTopAnchor(layersPanel, 80.0);
        AnchorPane.setRightAnchor(layersPanel, 0.0);

        Button showRightBtn = new Button("<");
        showRightBtn.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white; -fx-background-radius: 8 0 0 8; -fx-padding: 10 5;");
        showRightBtn.setVisible(false);
        AnchorPane.setTopAnchor(showRightBtn, 80.0);
        AnchorPane.setRightAnchor(showRightBtn, 0.0);

        collapseRightBtn.setOnAction(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), layersPanel);
            tt.setToX(250);
            tt.setOnFinished(evt -> showRightBtn.setVisible(true));
            tt.play();
        });

        showRightBtn.setOnAction(e -> {
            showRightBtn.setVisible(false);
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), layersPanel);
            tt.setToX(0);
            tt.play();
        });

        // Add everything to the AnchorPane

        this.getChildren().addAll(canvasContainer, leftScroll, showLeftBtn, topActionBar, layersPanel, showRightBtn);
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
