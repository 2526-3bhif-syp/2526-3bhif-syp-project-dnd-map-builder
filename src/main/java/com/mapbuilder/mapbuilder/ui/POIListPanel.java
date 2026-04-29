package com.mapbuilder.mapbuilder.ui;

import com.mapbuilder.mapbuilder.core.map.PointOfInterest;
import com.mapbuilder.mapbuilder.main.MainPresenter;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * POIListPanel is a JavaFX component that displays all Points of Interest on the map
 * in a scrollable list. Each POI is shown with a color square and name.
 * 
 * Clicking on a POI in the list opens the POI editor modal dialog.
 */
public class POIListPanel extends VBox {
    
    private MainPresenter presenter;
    private ListView<PointOfInterest> poiListView;
    
    /**
     * Constructs a POIListPanel with a reference to the MainPresenter for callbacks.
     * 
     * @param presenter The MainPresenter instance for handling POI editor callbacks (can be null)
     */
    public POIListPanel(MainPresenter presenter) {
        super(5);
        this.presenter = presenter;
        this.setStyle("-fx-padding: 5; -fx-border-color: transparent;");
        this.setFillWidth(true);
        initializeListView();
    }
    
    /**
     * Sets the presenter for this panel (used if created with null presenter).
     * 
     * @param presenter The MainPresenter instance
     */
    public void setPresenter(MainPresenter presenter) {
        this.presenter = presenter;
    }
    
    /**
     * Initializes the POI list view with custom cell rendering and click handling.
     */
    private void initializeListView() {
        poiListView = new ListView<>();
        poiListView.setPrefHeight(200);
        poiListView.setStyle("-fx-background-color: #1e1e1e; -fx-control-inner-background: #1e1e1e; -fx-text-fill: white;");
        poiListView.setCellFactory(param -> new ListCell<PointOfInterest>() {
            @Override
            protected void updateItem(PointOfInterest poi, boolean empty) {
                super.updateItem(poi, empty);
                setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: white;");
                if (empty || poi == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Create HBox with icon color square + name + coordinates
                    HBox cell = new HBox(5);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    cell.setStyle("-fx-background-color: #1e1e1e;");
                    
                    // Color square (16x16px, type color)
                    Rectangle colorSquare = new Rectangle(16, 16);
                    Integer customColor = poi.getCustomColor();
                    int poiColor = customColor != null ? customColor : POIIconMapper.getDefaultColor(poi.getType());
                    colorSquare.setFill(toFXColor(poiColor));
                    
                    // POI name and details
                    String displayText = String.format("%s (%s) @ %d,%d", 
                        poi.getName(), poi.getType(), poi.getX(), poi.getY());
                    Label label = new Label(displayText);
                    label.setStyle("-fx-text-fill: #e0e0e0;");
                    
                    cell.getChildren().addAll(colorSquare, label);
                    setGraphic(cell);
                }
            }
        });
        
        // Handle selection: click to open editor
        poiListView.setOnMouseClicked(event -> {
            PointOfInterest selected = poiListView.getSelectionModel().getSelectedItem();
            if (selected != null && presenter != null) {
                presenter.openPOIEditor(selected);
            }
        });
        
        // Make list view grow to fill available space
        VBox.setVgrow(poiListView, Priority.ALWAYS);
        
        // Wrap in scroll pane for better rendering
        ScrollPane scrollPane = new ScrollPane(poiListView);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #1e1e1e; -fx-control-inner-background: #1e1e1e; -fx-padding: 0;");
        
        this.getChildren().add(scrollPane);
    }
    
    /**
     * Updates the POI list with a new set of POIs.
     * Called after map regeneration or POI edits.
     * 
     * @param pois List of PointOfInterest objects to display
     */
    public void updatePOIList(List<PointOfInterest> pois) {
        if (pois == null) {
            poiListView.setItems(FXCollections.emptyObservableList());
        } else {
            poiListView.setItems(FXCollections.observableArrayList(pois));
        }
    }
    
    /**
     * Converts an ARGB integer color to JavaFX Color.
     * 
     * @param argb ARGB color value
     * @return JavaFX Color
     */
    private Color toFXColor(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return Color.color(r / 255.0, g / 255.0, b / 255.0, a / 255.0);
    }
}
