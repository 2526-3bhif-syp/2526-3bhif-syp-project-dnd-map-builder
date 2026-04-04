module com.mapbuilder.mapbuilder {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.mapbuilder.mapbuilder to javafx.fxml;
    exports com.mapbuilder.mapbuilder;
}