module com.example.seniorproject {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens com.example.seniorproject to javafx.fxml;
    exports com.example.seniorproject;
}