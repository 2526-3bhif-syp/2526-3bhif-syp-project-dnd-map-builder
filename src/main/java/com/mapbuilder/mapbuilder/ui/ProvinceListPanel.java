package com.mapbuilder.mapbuilder.ui;

import com.mapbuilder.mapbuilder.core.map.Kingdom;
import com.mapbuilder.mapbuilder.main.MainPresenter;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;

/**
 * ProvinceListPanel displays all provinces (kingdoms) in a scrollable list.
 *
 * <p>Features:
 * <ul>
 *   <li>Color swatch + province name per row</li>
 *   <li>Inline {@link ColorPicker} to change a province's colour</li>
 *   <li>Single click → selects the province for painting</li>
 *   <li>Double click → opens a rename dialog</li>
 * </ul>
 */
public class ProvinceListPanel extends VBox {

    private MainPresenter presenter;
    private ListView<Kingdom> listView;
    private ScrollPane scroll;
    private Label warningLabel;

    /**
     * @param presenter the {@link MainPresenter} (may be {@code null}; set later via
     *                  {@link #setPresenter(MainPresenter)})
     */
    public ProvinceListPanel(MainPresenter presenter) {
        super(5);
        this.presenter = presenter;
        setStyle("-fx-padding: 5; -fx-border-color: transparent;");
        setFillWidth(true);
        initListView();
        initWarningLabel();
    }

    /** Late-initialises the presenter reference (called from {@code MainPresenter.setView()}). */
    public void setPresenter(MainPresenter presenter) {
        this.presenter = presenter;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void initListView() {
        listView = new ListView<>();
        listView.setPrefHeight(180);
        listView.setStyle(
                "-fx-background-color: #1e1e1e; -fx-control-inner-background: #1e1e1e;");

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Kingdom k, boolean empty) {
                super.updateItem(k, empty);
                setStyle("-fx-background-color: #1e1e1e;");
                if (empty || k == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #1e1e1e;");

                // Colour swatch – updated whenever the picker fires
                Rectangle swatch = new Rectangle(14, 14);
                swatch.setArcWidth(3);
                swatch.setArcHeight(3);
                swatch.setFill(argbToFX(k.getColorARGB()));

                // Province name
                Label nameLabel = new Label(k.getName());
                nameLabel.setStyle("-fx-text-fill: #e0e0e0;");
                HBox.setHgrow(nameLabel, Priority.ALWAYS);

                // Compact inline colour picker
                ColorPicker picker = new ColorPicker(argbToFX(k.getColorARGB()));
                picker.setPrefSize(60, 24);
                picker.setOnAction(e -> {
                    Color c = picker.getValue();
                    k.setColorARGB(fxToArgb(c));
                    swatch.setFill(c);
                    if (presenter != null) {
                        presenter.onProvinceColorChanged();
                        presenter.setSelectedKingdom(k);
                    }
                });

                row.getChildren().addAll(swatch, nameLabel, picker);
                setGraphic(row);
            }
        });

        // Click handling
        listView.setOnMouseClicked(event -> {
            Kingdom selected = listView.getSelectionModel().getSelectedItem();
            if (selected == null || presenter == null) return;

            // Always inform the presenter of the selection
            presenter.setSelectedKingdom(selected);

            // Double-click → rename dialog
            if (event.getClickCount() == 2) {
                TextInputDialog dlg = new TextInputDialog(selected.getName());
                dlg.setTitle("Rename Province");
                dlg.setHeaderText("Enter a new name for this province:");
                dlg.setContentText("Name:");
                dlg.getDialogPane().getStylesheets().add(
                        getClass().getResource("/styles.css").toExternalForm());
                dlg.showAndWait().ifPresent(name -> {
                    if (!name.isBlank()) {
                        selected.setName(name.trim());
                        listView.refresh();
                        presenter.setSelectedKingdom(selected);
                    }
                });
            }
        });

        VBox.setVgrow(listView, Priority.ALWAYS);

        scroll = new ScrollPane(listView);
        scroll.setFitToWidth(true);
        scroll.setStyle(
                "-fx-background: #1e1e1e; -fx-control-inner-background: #1e1e1e; -fx-padding: 0;");

        getChildren().add(scroll);
    }

    private void initWarningLabel() {
        warningLabel = new Label("Please select a Province before painting");
        warningLabel.setStyle(
                "-fx-text-fill: #ff6b6b; -fx-font-size: 11px; -fx-padding: 4 6; " +
                "-fx-background-color: rgba(255,60,60,0.12); -fx-background-radius: 4;");
        warningLabel.setWrapText(true);
        warningLabel.setMaxWidth(Double.MAX_VALUE);
        warningLabel.setVisible(false);
        warningLabel.setManaged(false);
        getChildren().add(0, warningLabel);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API

    /** Replaces the list contents. Called after every map regeneration. */
    public void updateProvinceList(List<Kingdom> kingdoms) {
        if (kingdoms == null || kingdoms.isEmpty()) {
            listView.setItems(FXCollections.emptyObservableList());
        } else {
            listView.setItems(FXCollections.observableArrayList(kingdoms));
        }
    }

    /** Highlights {@code kingdom} in the list (e.g. after a map-click selection). */
    public void selectKingdom(Kingdom kingdom) {
        if (kingdom == null) {
            listView.getSelectionModel().clearSelection();
            return;
        }
        listView.getSelectionModel().select(kingdom);
        listView.scrollTo(kingdom);
    }

    /** Returns the currently selected kingdom, or {@code null}. */
    public Kingdom getSelectedKingdom() {
        return listView.getSelectionModel().getSelectedItem();
    }

    /**
     * Shows a "please select a province" warning and briefly highlights the
     * list with a red border. Safe to call from any event handler on the FX thread.
     */
    public void flashError() {
        // Show warning label
        warningLabel.setVisible(true);
        warningLabel.setManaged(true);

        // Red border on the scroll pane
        scroll.setStyle(
                "-fx-background: #1e1e1e; -fx-control-inner-background: #1e1e1e; -fx-padding: 0; " +
                "-fx-border-color: #ff4444; -fx-border-width: 2;");

        PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
        pause.setOnFinished(e -> {
            warningLabel.setVisible(false);
            warningLabel.setManaged(false);
            scroll.setStyle(
                    "-fx-background: #1e1e1e; -fx-control-inner-background: #1e1e1e; -fx-padding: 0;");
        });
        pause.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Colour helpers

    private static Color argbToFX(int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >>  8) & 0xFF;
        int b =  argb        & 0xFF;
        return Color.color(r / 255.0, g / 255.0, b / 255.0, a / 255.0);
    }

    private static int fxToArgb(Color c) {
        int a = (int) Math.round(c.getOpacity() * 255);
        int r = (int) Math.round(c.getRed()     * 255);
        int g = (int) Math.round(c.getGreen()   * 255);
        int b = (int) Math.round(c.getBlue()    * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
