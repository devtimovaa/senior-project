module com.example.seniorproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires transitive java.desktop;

    requires org.controlsfx.controls;

    opens com.example.seniorproject to javafx.fxml;
    exports com.example.seniorproject;
    exports com.example.seniorproject.model;
    exports com.example.seniorproject.model.algorithm;
    opens com.example.seniorproject.model to javafx.fxml;
    opens com.example.seniorproject.model.algorithm to javafx.fxml;
    opens com.example.seniorproject.view to javafx.fxml;
    opens com.example.seniorproject.controller to javafx.fxml;
}