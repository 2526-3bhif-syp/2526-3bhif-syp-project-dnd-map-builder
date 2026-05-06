package com.mapbuilder.mapbuilder.ui;

import com.mapbuilder.mapbuilder.core.map.POIType;
import com.mapbuilder.mapbuilder.core.map.PointOfInterest;
import com.mapbuilder.mapbuilder.main.MainPresenter;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Window;

/**
 * POIEditorDialog is a modal dialog for editing POI properties.
 * 
 * It allows users to:
 * - Edit POI name (TextField)
 * - Change POI type (ComboBox with all POIType values)
 * - Edit description (TextArea)
 * - Choose custom color (ColorPicker)
 * - Override icon type (ComboBox)
 * - Save, Cancel, or Delete the POI
 * 
 * Extends Dialog<PointOfInterest> to return the edited POI or null on cancel.
 */
public class POIEditorDialog extends Dialog<PointOfInterest> {
    
    private PointOfInterest poi;
    private MainPresenter presenter;
    
    private TextField nameField;
    private ComboBox<POIType> typeCombo;
    private TextArea descriptionArea;
    private ColorPicker colorPicker;
    private ComboBox<POIType> iconCombo;
    
    /**
     * Constructs a POI editor dialog.
     * 
     * @param poi The POI to edit (non-null)
     * @param owner The parent window (for modal behavior)
     * @param presenter The MainPresenter for save/delete callbacks
     */
    public POIEditorDialog(PointOfInterest poi, Window owner, MainPresenter presenter) {
        this.poi = poi;
        this.presenter = presenter;
        
        this.setTitle("Edit POI: " + poi.getName());
        this.initOwner(owner);
        this.setResizable(true);
        this.getDialogPane().setPrefWidth(500);
        this.getDialogPane().setPrefHeight(400);
        this.getDialogPane().getStylesheets().add(
            getClass().getResource("/styles.css").toExternalForm()
        );

        initializeForm();
        setupButtons();
    }
    
    /**
     * Initializes all form fields with current POI values.
     */
    private void initializeForm() {
        VBox form = new VBox(10);
        form.setPadding(new javafx.geometry.Insets(15));
        
        // Name field
        nameField = new TextField(poi.getName() != null ? poi.getName() : "");
        nameField.setPrefWidth(300);
        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("Name:");
        nameLabel.setTextFill(javafx.scene.paint.Color.web("#e0e0e0"));
        nameBox.getChildren().addAll(nameLabel, nameField);

        // Type combo box
        typeCombo = new ComboBox<>(FXCollections.observableArrayList(POIType.values()));
        typeCombo.setValue(poi.getType());
        typeCombo.setPrefWidth(200);
        HBox typeBox = new HBox(10);
        typeBox.setAlignment(Pos.CENTER_LEFT);
        Label typeLabel = new Label("Type:");
        typeLabel.setTextFill(javafx.scene.paint.Color.web("#e0e0e0"));
        typeBox.getChildren().addAll(typeLabel, typeCombo);

        // Description area
        descriptionArea = new TextArea(poi.getDescription() != null ? poi.getDescription() : "");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefHeight(100);
        descriptionArea.setPrefWidth(300);
        HBox descBox = new HBox(10);
        descBox.setAlignment(Pos.TOP_LEFT);
        Label descLabel = new Label("Description:");
        descLabel.setTextFill(javafx.scene.paint.Color.web("#e0e0e0"));
        descBox.getChildren().addAll(descLabel, descriptionArea);

        // Color picker
        colorPicker = new ColorPicker();
        if (poi.getCustomColor() != null) {
            colorPicker.setValue(toFXColor(poi.getCustomColor()));
        } else {
            colorPicker.setValue(toFXColor(POIIconMapper.getDefaultColor(poi.getType())));
        }
        HBox colorBox = new HBox(10);
        colorBox.setAlignment(Pos.CENTER_LEFT);
        Label colorLabel = new Label("Color:");
        colorLabel.setTextFill(javafx.scene.paint.Color.web("#e0e0e0"));
        colorBox.getChildren().addAll(colorLabel, colorPicker);

        // Icon override combo
        iconCombo = new ComboBox<>(FXCollections.observableArrayList(POIType.values()));
        if (poi.getCustomIcon() != null) {
            iconCombo.setValue(poi.getCustomIcon());
        } else {
            iconCombo.setValue(poi.getType());
        }
        iconCombo.setPrefWidth(200);
        HBox iconBox = new HBox(10);
        iconBox.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label("Icon:");
        iconLabel.setTextFill(javafx.scene.paint.Color.web("#e0e0e0"));
        iconBox.getChildren().addAll(iconLabel, iconCombo);
        
        form.getChildren().addAll(
            nameBox,
            typeBox,
            descBox,
            colorBox,
            iconBox
        );
        
        this.getDialogPane().setContent(form);
    }
    
    /**
     * Sets up the Save, Cancel, and Delete buttons.
     */
    private void setupButtons() {
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OTHER);
        
        this.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType, deleteButtonType);
        
        // Handle Save
        Button saveBtn = (Button) this.getDialogPane().lookupButton(saveButtonType);
        saveBtn.setOnAction(e -> {
            // Update POI with field values
            poi.setName(nameField.getText());
            poi.setDescription(descriptionArea.getText());
            poi.setCustomColor(colorToARGB(colorPicker.getValue()));
            poi.setCustomIcon(iconCombo.getValue());
            
            // Notify presenter to save
            if (presenter != null) {
                presenter.savePOI(poi);
            }
            
            this.setResult(poi);
            this.close();
        });
        
        // Handle Delete
        Button deleteBtn = (Button) this.getDialogPane().lookupButton(deleteButtonType);
        deleteBtn.setOnAction(e -> {
            if (presenter != null) {
                presenter.deletePOI(poi);
            }
            this.setResult(null);
            this.close();
        });
        
        // Handle Cancel
        Button cancelBtn = (Button) this.getDialogPane().lookupButton(cancelButtonType);
        cancelBtn.setOnAction(e -> {
            this.setResult(null);
            this.close();
        });
    }
    
    /**
     * Converts a JavaFX Color to ARGB integer.
     * 
     * @param color The JavaFX Color
     * @return ARGB integer value
     */
    private int colorToARGB(Color color) {
        int a = (int) (color.getOpacity() * 255);
        int r = (int) (color.getRed() * 255);
        int g = (int) (color.getGreen() * 255);
        int b = (int) (color.getBlue() * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
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
