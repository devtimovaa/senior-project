package com.example.seniorproject.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

//Shared helpers
public abstract class BaseController {

    protected void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    //Reads and validates the key field
    protected int parseKey(String algorithm, TextField keyField) {
        if (!"Randomized LSB".equals(algorithm) && !"Josephus LSB 3-3-2".equals(algorithm)) {
            return 0;
        }
        String keyText = keyField.getText();
        if (keyText == null || keyText.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Key Required", "Please enter an integer key.");
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(keyText.trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Key", "The key must be an integer.");
            return Integer.MIN_VALUE;
        }
    }
}
