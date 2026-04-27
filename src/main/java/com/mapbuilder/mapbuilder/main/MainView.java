package com.mapbuilder.mapbuilder.main;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class MainView extends AnchorPane {
    
    private Canvas canvas;
    private Canvas poiCanvas;
    private Pane canvasContainer;
    private Group canvasGroup;
    private double mouseX = 0;
    private double mouseY = 0;
    private TextField seedField;
    private Button randomSeedButton;

    private Slider sizeSlider;
    private Slider octavesSlider;
    private Slider scaleSlider;
    private Slider falloffSlider;
    private Slider waterLevelSlider;
    private Slider tempBiasSlider;
    private Slider rainBiasSlider;
    private Button randomizeSettingsButton;
    private CheckBox enableRiversToggle;
    private CheckBox enableLakesToggle;
    private Slider riverDensitySlider;
    private Slider lakeSizeSlider;
    private Slider minLakeAreaSlider;

    private Slider kingdomCountSlider;
    private Slider lloydPassesSlider;
    private CheckBox enableBordersToggle;
    private CheckBox enableKingdomOverlayToggle;
    private ToggleButton poiToggle;

    public MainView() {
        setupCanvasContainer();
        ScrollPane leftScroll = setupLeftPanel();
        Button showLeftBtn = setupLeftPanelShowButton(leftScroll);
        HBox topActionBar = setupTopActionBar();
        VBox layersPanel = setupRightLayersPanel();
        Button showRightBtn = setupRightPanelShowButton(layersPanel);
        
        this.getChildren().addAll(canvasContainer, leftScroll, showLeftBtn, topActionBar, layersPanel, showRightBtn);
    }

    private void setupCanvasContainer() {
        canvasContainer = new Pane();
        canvasContainer.setStyle("-fx-background-color: #333333;");
        canvas = new Canvas(800, 800);
        poiCanvas = new Canvas(800, 800);
        poiCanvas.setMouseTransparent(false);
        
        canvasGroup = new Group(canvas, poiCanvas);
        canvasContainer.getChildren().add(canvasGroup);
        
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

        // Track mouse position for POI hover labels
        canvasContainer.setOnMouseMoved(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
        });
    }

    private ScrollPane setupLeftPanel() {
        VBox leftPanel = new VBox(10);
        leftPanel.setPrefWidth(260);
        leftPanel.setPadding(new Insets(15));
        leftPanel.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 0;");
        leftPanel.getStyleClass().add("left-panel");
        
        HBox headerBox = new HBox();
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label headerLabel = new Label("Generator Settings");
        headerLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px;");
        Button collapseLeftBtn = new Button("\u25C0"); // ◀
        collapseLeftBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: white;");
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerBox.getChildren().addAll(headerLabel, spacer, collapseLeftBtn);

        Tab terrainTab = new Tab("Terrain");
        terrainTab.setClosable(false);
        VBox terrainContent = new VBox(10);
        terrainContent.setPadding(new Insets(10, 0, 10, 0));

        Tab hydrologyTab = new Tab("Hydrology");
        hydrologyTab.setClosable(false);
        VBox hydrologyContent = new VBox(10);
        hydrologyContent.setPadding(new Insets(10, 0, 10, 0));
        
        enableRiversToggle = new CheckBox("Enable Rivers");
        enableRiversToggle.setSelected(true);

        riverDensitySlider = new Slider(0, 200, 100);
        riverDensitySlider.setShowTickMarks(true);
        riverDensitySlider.setShowTickLabels(true);
        riverDensitySlider.setMajorTickUnit(50);

        enableLakesToggle = new CheckBox("Enable Lakes");
        enableLakesToggle.setSelected(true);

        lakeSizeSlider = new Slider(0, 200, 100);
        lakeSizeSlider.setShowTickMarks(true);
        lakeSizeSlider.setShowTickLabels(true);
        lakeSizeSlider.setMajorTickUnit(50);

        minLakeAreaSlider = new Slider(10, 500, 100);
        minLakeAreaSlider.setShowTickMarks(true);
        minLakeAreaSlider.setShowTickLabels(true);
        minLakeAreaSlider.setMajorTickUnit(100);

        hydrologyContent.getChildren().addAll(
                enableRiversToggle,
                new Label("River Density (%)"), riverDensitySlider,
                new Separator(),
                enableLakesToggle,
                new Label("Lake Size Multiplier (%)"), lakeSizeSlider,
                new Label("Minimum Lake Area"), minLakeAreaSlider
        );
        hydrologyTab.setContent(hydrologyContent);

        Tab kingdomsTab = new Tab("Kingdoms");
        kingdomsTab.setClosable(false);
        VBox kingdomsContent = new VBox(10);
        kingdomsContent.setPadding(new Insets(10, 0, 10, 0));

        kingdomCountSlider = new Slider(0, 50, 10);
        kingdomCountSlider.setShowTickMarks(true);
        kingdomCountSlider.setShowTickLabels(true);
        kingdomCountSlider.setMajorTickUnit(10);

        lloydPassesSlider = new Slider(0, 5, 1);
        lloydPassesSlider.setShowTickMarks(true);
        lloydPassesSlider.setShowTickLabels(true);
        lloydPassesSlider.setMajorTickUnit(1);
        lloydPassesSlider.setMinorTickCount(0);
        lloydPassesSlider.setSnapToTicks(true);

        enableBordersToggle = new CheckBox("Show Borders");
        enableBordersToggle.setSelected(true);

        enableKingdomOverlayToggle = new CheckBox("Show Kingdom Overlay");
        enableKingdomOverlayToggle.setSelected(true);

        kingdomsContent.getChildren().addAll(
                new Label("Kingdom Count"), kingdomCountSlider,
                new Label("Lloyd's Relaxation Passes"), lloydPassesSlider,
                new Separator(),
                enableBordersToggle,
                enableKingdomOverlayToggle
        );
        kingdomsTab.setContent(kingdomsContent);

        TabPane tabPane = new TabPane(terrainTab, hydrologyTab, kingdomsTab);
        tabPane.setStyle("-fx-background-color: transparent;");

        seedField = new TextField("12345");
        seedField.setPrefWidth(100);
        randomSeedButton = new Button("Random Seed");
        randomSeedButton.setStyle("-fx-cursor: hand;");
        HBox seedRow = new HBox(8);
        seedRow.setAlignment(Pos.CENTER_LEFT);
        seedRow.getChildren().addAll(new Label("Seed"), seedField, randomSeedButton);

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

        terrainContent.getChildren().addAll(
            mapSizeLabel, sizeSlider,
            octavesLabel, octavesSlider,
            scaleLabel, scaleSlider,
            falloffLabel, falloffSlider,
            seaLevelLabel, waterLevelSlider,
            tempBiasLabel, tempBiasSlider,
            rainBiasLabel, rainBiasSlider
        );
        terrainTab.setContent(terrainContent);
        
        randomizeSettingsButton = new Button("Randomize Settings");
        
        HBox actionRow = new HBox(8);
        actionRow.getChildren().addAll(randomizeSettingsButton);

        leftPanel.getChildren().addAll(
            headerBox,
            seedRow,
            seedField,
            tabPane,
            actionRow
        );

        ScrollPane leftScroll = new ScrollPane(leftPanel);
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setStyle("-fx-background: #2b2b2b; -fx-background-color: #2b2b2b; -fx-padding: 0; -fx-border-color: transparent; -fx-border-width: 0; -fx-background-insets: 0;");
        leftScroll.getStyleClass().add("left-panel-scroll");
        leftScroll.setPrefViewportHeight(600);
        
        AnchorPane.setTopAnchor(leftScroll, 0.0);
        AnchorPane.setLeftAnchor(leftScroll, 0.0);
        AnchorPane.setBottomAnchor(leftScroll, 0.0);

        // Store collapse logic in a property of the leftScroll so the show button can use it
        leftScroll.getProperties().put("collapseLeftBtn", collapseLeftBtn);

        return leftScroll;
    }

    private Button setupLeftPanelShowButton(ScrollPane leftScroll) {
        Button showLeftBtn = new Button("\u25B6"); // ▶
        showLeftBtn.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white; -fx-background-radius: 0 8 8 0; -fx-padding: 10 5; -fx-cursor: hand;");
        showLeftBtn.setVisible(false);
        AnchorPane.setTopAnchor(showLeftBtn, 0.0);
        AnchorPane.setLeftAnchor(showLeftBtn, 0.0);

        Button collapseLeftBtn = (Button) leftScroll.getProperties().get("collapseLeftBtn");
        collapseLeftBtn.setOnAction(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), leftScroll);
            tt.setToX(-280);
            tt.setOnFinished(evt -> showLeftBtn.setVisible(true));
            tt.play();
        });
        
        showLeftBtn.setOnAction(e -> {
            showLeftBtn.setVisible(false);
            TranslateTransition tt = new TranslateTransition(Duration.millis(300), leftScroll);
            tt.setToX(0);
            tt.play();
        });

        return showLeftBtn;
    }

    private HBox setupTopActionBar() {
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
        
        return topActionBar;
    }

    private VBox setupRightLayersPanel() {
        VBox layersPanel = new VBox(10);
        layersPanel.setPrefWidth(250);
        layersPanel.setStyle("-fx-background-color: #2b2b2b; -fx-padding: 15; -fx-background-radius: 8 0 0 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");
        
        HBox layersHeaderBox = new HBox();
        layersHeaderBox.setAlignment(Pos.CENTER_LEFT);
        Label layersHeaderLabel = new Label("Layers");
        layersHeaderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px;");
        Button collapseRightBtn = new Button("\u25B6"); // ▶
        collapseRightBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-text-fill: white;");
        Pane rightSpacer = new Pane();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);
        layersHeaderBox.getChildren().addAll(collapseRightBtn, rightSpacer, layersHeaderLabel);

        layersPanel.getChildren().add(layersHeaderBox);

        String[] layerNames = {"Markierungen", "Punkte von Interesse", "Strukturen & Straßen", "Berge", "Flüsse und Seen", "Grid"};

        for (int i = 0; i < layerNames.length; i++) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #3c3f41; -fx-padding: 8; -fx-background-radius: 5;");

            Label nameLabel = new Label(layerNames[i]);
            nameLabel.setStyle("-fx-text-fill: white;");

            Pane rowSpacer = new Pane();
            HBox.setHgrow(rowSpacer, Priority.ALWAYS);

            ToggleButton toggle = new ToggleButton("(o)");
            toggle.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand;");
            toggle.setSelected(true);  // All layers visible by default
            toggle.selectedProperty().addListener((obs, oldV, newV) -> {
                if (newV) {
                    toggle.setText("(/)");
                } else {
                    toggle.setText("(o)");
                }
                
                // Wire POI toggle to control POI canvas opacity
                if (i == 1) {  // "Punkte von Interesse"
                    poiCanvas.setOpacity(newV ? 1.0 : 0.0);
                }
            });

            // Store reference to POI toggle
            if (i == 1) {
                poiToggle = toggle;
            }

            row.getChildren().addAll(nameLabel, rowSpacer, toggle);
            layersPanel.getChildren().add(row);
        }

        AnchorPane.setTopAnchor(layersPanel, 80.0);
        AnchorPane.setRightAnchor(layersPanel, 0.0);

        layersPanel.getProperties().put("collapseRightBtn", collapseRightBtn);

        return layersPanel;
    }

    private Button setupRightPanelShowButton(VBox layersPanel) {
        Button showRightBtn = new Button("\u25C0"); // ◀
        showRightBtn.setStyle("-fx-background-color: #2b2b2b; -fx-text-fill: white; -fx-background-radius: 8 0 0 8; -fx-padding: 10 5;");
        showRightBtn.setVisible(false);
        AnchorPane.setTopAnchor(showRightBtn, 80.0);
        AnchorPane.setRightAnchor(showRightBtn, 0.0);

        Button collapseRightBtn = (Button) layersPanel.getProperties().get("collapseRightBtn");
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

        return showRightBtn;
    }

    public void centerMap() {
        double cw = canvasContainer.getWidth();
        double ch = canvasContainer.getHeight();
        double mw = canvas.getWidth();
        double mh = canvas.getHeight();
        double tx = cw > 0 ? (cw - mw) / 2 : 0;
        double ty = ch > 0 ? (ch - mh) / 2 : 0;
        
        canvasGroup.setScaleX(1.0);
        canvasGroup.setScaleY(1.0);
        canvasGroup.setTranslateX(tx);
        canvasGroup.setTranslateY(ty);
    }

    public Canvas getCanvas() { return canvas; }
    public Canvas getPoiCanvas() { return poiCanvas; }
    public double getMouseX() { return mouseX; }
    public double getMouseY() { return mouseY; }
    public ToggleButton getPoiToggle() { return poiToggle; }
    public TextField getSeedField() { return seedField; }
    public Slider getSizeSlider() { return sizeSlider; }
    public Slider getOctavesSlider() { return octavesSlider; }
    public Slider getScaleSlider() { return scaleSlider; }
    public Slider getFalloffSlider() { return falloffSlider; }
    public Slider getWaterLevelSlider() { return waterLevelSlider; }
    public Slider getTempBiasSlider() { return tempBiasSlider; }
    public Slider getRainBiasSlider() { return rainBiasSlider; }
    public Button getRandomSeedButton() { return randomSeedButton; }
    public Button getRandomizeSettingsButton() { return randomizeSettingsButton; }
    public CheckBox getEnableRiversToggle() { return enableRiversToggle; }
    public CheckBox getEnableLakesToggle() { return enableLakesToggle; }
    public Slider getRiverDensitySlider() { return riverDensitySlider; }
    public Slider getLakeSizeSlider() { return lakeSizeSlider; }
    public Slider getMinLakeAreaSlider() { return minLakeAreaSlider; }
    public Slider getKingdomCountSlider() { return kingdomCountSlider; }
    public Slider getLloydPassesSlider() { return lloydPassesSlider; }
    public CheckBox getEnableBordersToggle() { return enableBordersToggle; }
    public CheckBox getEnableKingdomOverlayToggle() { return enableKingdomOverlayToggle; }
}