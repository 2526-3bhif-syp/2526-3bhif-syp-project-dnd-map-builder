package com.mapbuilder.mapbuilder;

import com.mapbuilder.mapbuilder.main.MainPresenter;
import com.mapbuilder.mapbuilder.main.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MapBuilderApplication extends Application {
    @Override
    public void start(Stage stage) {
        MainView view = new MainView();
        MainPresenter presenter = new MainPresenter();
        presenter.setView(view);

        Scene scene = new Scene(view, 1200, 800);
        String css = getClass().getResource("/styles.css").toExternalForm();
        scene.getStylesheets().add(css);
        
        stage.setTitle("DnD Map Builder");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
