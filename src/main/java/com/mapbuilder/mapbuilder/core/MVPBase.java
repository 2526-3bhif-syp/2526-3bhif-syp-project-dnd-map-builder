package com.mapbuilder.mapbuilder.core;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;

public interface MVPBase {
    interface View {
        Canvas getCanvas();
        TextField getSeedField();
        Slider getSizeSlider();
        Slider getOctavesSlider();
        Slider getScaleSlider();
        Slider getFalloffSlider();
        Slider getWaterLevelSlider();
        Slider getTempBiasSlider();
        Slider getRainBiasSlider();
        Button getRandomSeedButton();
        Button getGenerateButton();
        Button getRandomizeSettingsButton();
    }

    interface Presenter<V extends View> {
        void setView(V view);
        V getView();
    }
}
