module com.example.seniorproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires transitive java.desktop;

    requires org.controlsfx.controls;

    opens com.example.seniorproject to javafx.fxml;
    exports com.example.seniorproject;
    exports com.example.seniorproject.algorithms;
    opens com.example.seniorproject.algorithms to javafx.fxml;
}