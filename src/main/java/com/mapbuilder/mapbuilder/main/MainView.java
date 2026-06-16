package com.mapbuilder.mapbuilder.main;

import com.mapbuilder.mapbuilder.ui.POIListPanel;
import com.mapbuilder.mapbuilder.ui.ProvinceListPanel;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private Tab kingdomsTab;
    
    private Slider dungeonDensitySlider;
    private Slider settlementDensitySlider;
    private Slider ruinCastleDensitySlider;
    
    private POIListPanel poiListPanel;
    private ToggleButton addPoiToggle;

    // Province editing
    private ProvinceListPanel provinceListPanel;
    private ToggleButton provincePaintToggle;
    private Slider brushSizeSlider;
    private Label selectedProvinceLabel;
    private javafx.scene.shape.Rectangle selectedProvinceColorBox;

    public MainView() {
        setupCanvasContainer();
        ScrollPane leftScroll = setupLeftPanel();
        Button showLeftBtn = setupLeftPanelShowButton(leftScroll);
        HBox topActionBar = setupTopActionBar();
        VBox layersPanel = setupRightLayersPanel();
        Button showRightBtn = setupRightPanelShowButton(layersPanel);
        HBox infoToast = setupInfoToast();
        
        this.getChildren().addAll(canvasContainer, leftScroll, showLeftBtn, topActionBar, layersPanel, showRightBtn, infoToast);
    }

    private void setupCanvasContainer() {
        canvasContainer = new Pane();
        canvasContainer.setStyle("-fx-background-color: #333333;");
        canvas = new Canvas(800, 800);
        poiCanvas = new Canvas(800, 800);
        poiCanvas.setMouseTransparent(false);

        // Canvas fills the container — the viewport renderer handles zoom/pan in software
        canvasContainer.widthProperty().addListener((obs, oldW, newW) -> {
            canvas.setWidth(newW.doubleValue());
            poiCanvas.setWidth(newW.doubleValue());
        });
        canvasContainer.heightProperty().addListener((obs, oldH, newH) -> {
            canvas.setHeight(newH.doubleValue());
            poiCanvas.setHeight(newH.doubleValue());
        });

        canvasGroup = new Group(canvas, poiCanvas);
        canvasContainer.getChildren().add(canvasGroup);
        
        AnchorPane.setTopAnchor(canvasContainer, 0.0);
        AnchorPane.setBottomAnchor(canvasContainer, 0.0);
        AnchorPane.setLeftAnchor(canvasContainer, 0.0);
        AnchorPane.setRightAnchor(canvasContainer, 0.0);

        // On Windows, scroll events are routed to the focused node rather than
        // the node under the cursor. Making the canvas focusable and requesting
        // focus on mouse enter ensures zoom works the same as on macOS/Linux.
        canvasContainer.setFocusTraversable(true);
        canvasContainer.setOnMouseEntered(event -> canvasContainer.requestFocus());

        // Track mouse position for POI hover labels
        canvasContainer.setOnMouseMoved(event -> {
            mouseX = event.getX();
            mouseY = event.getY();
        });
        // Scroll/drag handlers are set by MainPresenter via getCanvasContainer()
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
        terrainTab.setStyle("-fx-text-fill: white;");
        VBox terrainContent = new VBox(10);
        terrainContent.setPadding(new Insets(10, 0, 10, 0));

        Tab hydrologyTab = new Tab("Hydrology");
        hydrologyTab.setClosable(false);
        hydrologyTab.setStyle("-fx-text-fill: white;");
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

        kingdomsTab = new Tab("Kingdoms");
        kingdomsTab.setClosable(false);
        kingdomsTab.setStyle("-fx-text-fill: white;");
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

        // ── Province Editing section (added to same kingdoms VBox) ──────────

        provincePaintToggle = new ToggleButton("\uD83C\uDFA8  Province Paint Mode");
        provincePaintToggle.setId("province-paint-toggle");
        provincePaintToggle.setMaxWidth(Double.MAX_VALUE);

        selectedProvinceLabel = new Label("Selected: None");
        selectedProvinceLabel.setStyle("-fx-text-fill: white; -fx-font-style: italic;");
        selectedProvinceColorBox = new javafx.scene.shape.Rectangle(12, 12, javafx.scene.paint.Color.TRANSPARENT);
        selectedProvinceColorBox.setStroke(javafx.scene.paint.Color.WHITE);
        HBox selectedProvinceBox = new HBox(5, new Label("Paint Target:"), selectedProvinceColorBox, selectedProvinceLabel);
        selectedProvinceBox.setAlignment(Pos.CENTER_LEFT);
        selectedProvinceBox.setStyle("-fx-text-fill: white;");

        Label brushSizeLabel = new Label("Brush Size");
        brushSizeLabel.setStyle("-fx-text-fill: white;");
        brushSizeSlider = new Slider(1, 15, 3);
        brushSizeSlider.setShowTickMarks(true);
        brushSizeSlider.setShowTickLabels(true);
        brushSizeSlider.setMajorTickUnit(7);
        brushSizeSlider.setMinorTickCount(6);

        Label provListTitle = new Label("Provinces");
        provListTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        provinceListPanel = new ProvinceListPanel(null);
        VBox.setVgrow(provinceListPanel, Priority.ALWAYS);

        kingdomsContent.getChildren().addAll(
                new Separator(),
                provincePaintToggle,
                selectedProvinceBox,
                brushSizeLabel, brushSizeSlider,
                new Separator(),
                provListTitle,
                provinceListPanel
        );

        // Create POIs Tab
        Tab poisTab = new Tab("POIs");
        poisTab.setClosable(false);
        poisTab.setStyle("-fx-text-fill: white;");
        VBox poisContent = new VBox(10);
        poisContent.setPadding(new Insets(10, 0, 10, 0));

        // Dungeon Density Slider
        dungeonDensitySlider = new Slider(0.0, 1.0, 0.3);
        dungeonDensitySlider.setShowTickMarks(true);
        dungeonDensitySlider.setShowTickLabels(true);
        dungeonDensitySlider.setMajorTickUnit(0.5);
        dungeonDensitySlider.setMinorTickCount(4);
        dungeonDensitySlider.setSnapToTicks(true);

        // Settlement Density Slider
        settlementDensitySlider = new Slider(0.0, 1.0, 0.3);
        settlementDensitySlider.setShowTickMarks(true);
        settlementDensitySlider.setShowTickLabels(true);
        settlementDensitySlider.setMajorTickUnit(0.5);
        settlementDensitySlider.setMinorTickCount(4);
        settlementDensitySlider.setSnapToTicks(true);

        // Ruin & Castle Density Slider
        ruinCastleDensitySlider = new Slider(0.0, 1.0, 0.3);
        ruinCastleDensitySlider.setShowTickMarks(true);
        ruinCastleDensitySlider.setShowTickLabels(true);
        ruinCastleDensitySlider.setMajorTickUnit(0.5);
        ruinCastleDensitySlider.setMinorTickCount(4);
        ruinCastleDensitySlider.setSnapToTicks(true);

        addPoiToggle = new ToggleButton("➕  Add POI");
        addPoiToggle.setId("add-poi-toggle");
        addPoiToggle.setMaxWidth(Double.MAX_VALUE);

        poisContent.getChildren().addAll(
                new Label("Dungeon Density"), dungeonDensitySlider,
                new Label("Settlement Density"), settlementDensitySlider,
                new Label("Ruin & Castle Density"), ruinCastleDensitySlider,
                new Separator(),
                new Label("Points of Interest"),
                addPoiToggle
        );

        // Add POI List Panel to POIs tab
        poiListPanel = new POIListPanel(null);  // Presenter will be set later
        poiListPanel.setPrefHeight(200);
        VBox.setVgrow(poiListPanel, Priority.ALWAYS);
        poisContent.getChildren().add(poiListPanel);

        poisTab.setContent(poisContent);

        TabPane tabPane = new TabPane(terrainTab, hydrologyTab, kingdomsTab, poisTab);
        tabPane.setStyle("-fx-background-color: #2b2b2b; " +
                "-fx-control-inner-background: #2b2b2b; " +
                "-fx-tab-header-background: #2b2b2b; " +
                "-fx-text-fill: white; " +
                "-fx-text-base-color: white;");
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);

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

    private HBox setupInfoToast() {
        // Icon
        ImageView infoIcon = new ImageView(loadIcon("/assets/info.png"));
        infoIcon.setFitWidth(16);
        infoIcon.setFitHeight(16);
        infoIcon.setPreserveRatio(true);
        infoIcon.setSmooth(true);

        Label hintLabel = new Label("Labels: Double-click to add/edit · Drag to move · Right-click to remove");
        hintLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;");
        hintLabel.setWrapText(false);

        Button dismissBtn = new Button("✕");
        dismissBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #888888; -fx-cursor: hand; " +
            "-fx-font-size: 11px; -fx-padding: 0 0 0 6;"
        );
        dismissBtn.setOnMouseEntered(e -> dismissBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-cursor: hand; " +
            "-fx-font-size: 11px; -fx-padding: 0 0 0 6;"
        ));
        dismissBtn.setOnMouseExited(e -> dismissBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #888888; -fx-cursor: hand; " +
            "-fx-font-size: 11px; -fx-padding: 0 0 0 6;"
        ));

        HBox infoToast = new HBox(8, infoIcon, hintLabel, dismissBtn);
        infoToast.setAlignment(Pos.CENTER_LEFT);
        infoToast.setPadding(new Insets(8, 12, 8, 12));
        infoToast.setStyle(
            "-fx-background-color: rgba(30,30,30,0.88); " +
            "-fx-background-radius: 8; " +
            "-fx-border-color: #444444; " +
            "-fx-border-radius: 8; " +
            "-fx-border-width: 1; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.55), 8, 0, 0, 2);"
        );

        dismissBtn.setOnAction(e -> {
            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(250), infoToast);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(evt -> infoToast.setVisible(false));
            ft.play();
        });

        AnchorPane.setBottomAnchor(infoToast, 18.0);
        AnchorPane.setRightAnchor(infoToast, 18.0);

        return infoToast;
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

        String[] layerNames = {"Labels", "Points of Interest", "Structures & Roads", "Mountains", "Rivers & Lakes", "Grid"};

        Image eyeOpen  = loadIcon("/assets/eye.png");
        Image eyeClosed = loadIcon("/assets/eye-blind.png");

        for (int i = 0; i < layerNames.length; i++) {
            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #3c3f41; -fx-padding: 8; -fx-background-radius: 5;");

            Label nameLabel = new Label(layerNames[i]);
            nameLabel.setStyle("-fx-text-fill: white;");

            Pane rowSpacer = new Pane();
            HBox.setHgrow(rowSpacer, Priority.ALWAYS);

            ImageView iconView = new ImageView(eyeOpen);
            iconView.setFitWidth(18);
            iconView.setFitHeight(18);
            iconView.setPreserveRatio(true);
            iconView.setSmooth(true);

            ToggleButton toggle = new ToggleButton("", iconView);
            toggle.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2 4;");
            toggle.setSelected(true);  // All layers visible by default
            int finalI = i;
            toggle.selectedProperty().addListener((obs, oldV, newV) -> {
                iconView.setImage(newV ? eyeOpen : eyeClosed);

                // Wire POI toggle to control POI canvas opacity
                if (finalI == 1) {
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

    public Canvas getCanvas() { return canvas; }
    public Pane getCanvasContainer() { return canvasContainer; }
    public Group getCanvasGroup() { return canvasGroup; }
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
    
    public Slider getDungeonDensitySlider() { return dungeonDensitySlider; }
    public Slider getSettlementDensitySlider() { return settlementDensitySlider; }
    public Slider getRuinCastleDensitySlider() { return ruinCastleDensitySlider; }
    
    public POIListPanel getPOIListPanel() { return poiListPanel; }
    public ToggleButton getAddPoiToggle() { return addPoiToggle; }

    public ToggleButton getProvincePaintToggle()    { return provincePaintToggle; }
    public Tab getKingdomsTab()                     { return kingdomsTab; }
    public Slider       getBrushSizeSlider()        { return brushSizeSlider; }
    public ProvinceListPanel getProvinceListPanel() { return provinceListPanel; }
    public Label getSelectedProvinceLabel()         { return selectedProvinceLabel; }
    public javafx.scene.shape.Rectangle getSelectedProvinceColorBox() { return selectedProvinceColorBox; }

    private Image loadIcon(String resourcePath) {
        java.io.InputStream stream = getClass().getResourceAsStream(resourcePath);
        if (stream != null) {
            return new Image(stream);
        }
        // Transparent 1×1 fallback so the button still renders
        return new javafx.scene.image.WritableImage(1, 1);
    }
}