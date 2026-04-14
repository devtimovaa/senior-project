package com.example.seniorproject.controller;

import com.example.seniorproject.model.ExtractingModel;
import com.example.seniorproject.view.ExtractingView;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class ExtractingController {
    private final ExtractingModel model;
    private final ExtractingView view;
    private File selectedFile;

    public ExtractingController(ExtractingModel model, ExtractingView view) {
        this.model = model;
        this.view = view;
        initEventHandlers();
    }

    public Node getNode() {
        return view.getNode();
    }

    private void initEventHandlers() {
        view.getChooseButton().setOnAction(event -> openImageChooser());
        view.getSubmitButton().setOnAction(event -> handleSubmit());
        view.getClearButton().setOnAction(event -> handleClear());

        view.getAlgorithmChoice().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean needsSeed = "Randomized LSB".equals(newVal) || "Josephus LSB 3-3-2".equals(newVal);
            view.getSeedBox().setVisible(needsSeed);
            view.getSeedBox().setManaged(needsSeed);
        });
    }

    //Choose an image
    private void openImageChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png"));
        Window window = view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null;
        if (window == null) {
            return;
        }
        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            selectedFile = file;
            Image image = new Image(file.toURI().toString());
            view.getImageView().setImage(image);

            view.getExtractedTextArea().clear();
            view.getExtractedTextArea().setVisible(false);
            view.getExtractedTextArea().setManaged(false);

            view.getExtractedImageView().setImage(null);
            view.getExtractedImageView().setVisible(false);
            view.getExtractedImageView().setManaged(false);
            
            view.getSecretLabel().setVisible(false);
            view.getSecretLabel().setManaged(false);
        }
    }

    private void handleSubmit() {
        if (selectedFile == null) {
            showAlert(Alert.AlertType.WARNING, "Image Missing", "Please choose an image!");
            return;
        }

        try {
            BufferedImage buffered = ImageIO.read(selectedFile);
            if (buffered == null) {
                showAlert(Alert.AlertType.ERROR, "Load Failed", "Could not read the image file.");
                return;
            }
            byte[] raw = extractBytesWithSelectedAlgorithm(buffered);
            if (raw == null) {
                return;
            }
            view.getSecretLabel().setVisible(true);
            view.getSecretLabel().setManaged(true);

            if (ExtractingModel.isPngBytes(raw)) {
                BufferedImage hiddenImage = ImageIO.read(new ByteArrayInputStream(raw));
                if (hiddenImage == null) {
                    showAlert(Alert.AlertType.ERROR, "Decode Failed", "Could not decode the hidden image.");
                    return;
                }
                view.getExtractedImageView().setImage(SwingFXUtils.toFXImage(hiddenImage, null));
                view.getExtractedImageView().setVisible(true);
                view.getExtractedImageView().setManaged(true);
                view.getExtractedTextArea().setVisible(false);
                view.getExtractedTextArea().setManaged(false);
            } else {
                String extracted = raw.length == 0 ? "" : new String(raw, StandardCharsets.UTF_8);
                view.getExtractedTextArea().setText(extracted);
                view.getExtractedTextArea().setVisible(true);
                view.getExtractedTextArea().setManaged(true);
                view.getExtractedImageView().setVisible(false);
                view.getExtractedImageView().setManaged(false);
                if (extracted.isEmpty()) {
                    showAlert(Alert.AlertType.INFORMATION, "Empty Result", "No secret message could be found.");
                }
            }

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Extraction Failed", ex.getMessage() != null ? ex.getMessage() : "Unable to extract the secret.");
        }
    }

    private byte[] extractBytesWithSelectedAlgorithm(BufferedImage buffered) {
        String selection = view.getAlgorithmChoice().getValue();
        if ("LSB".equals(selection)) {
            return model.extract(buffered, selection, 0);
        }
        if ("Randomized LSB".equals(selection)) {
            String seedText = view.getSeedField().getText();
            if (seedText == null || seedText.isBlank()) {
                showAlert(Alert.AlertType.ERROR, "Key Required", "Please enter an integer key for Randomized LSB.");
                return null;
            }
            int seed;
            try {
                seed = Integer.parseInt(seedText.trim());
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Key", "The key must be an integer.");
                return null;
            }
            return model.extract(buffered, selection, seed);
        }
        if ("Josephus LSB 3-3-2".equals(selection)) {
            String seedText = view.getSeedField().getText();
            if (seedText == null || seedText.isBlank()) {
                showAlert(Alert.AlertType.ERROR, "Key Required", "Please enter an integer key.");
                return null;
            }
            int seed;
            try {
                seed = Integer.parseInt(seedText.trim());
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Key", "The key must be an integer.");
                return null;
            }
            return model.extract(buffered, selection, seed);
        }
        return new byte[0];
    }

    private void handleClear() {
        Image defaultImage = view.loadDefaultImage();
        selectedFile = null;
        view.getImageView().setImage(defaultImage);
        view.getExtractedTextArea().clear();
        view.getExtractedTextArea().setVisible(false);
        view.getExtractedTextArea().setManaged(false);
        view.getExtractedImageView().setImage(null);
        view.getExtractedImageView().setVisible(false);
        view.getExtractedImageView().setManaged(false);
        view.getSecretLabel().setVisible(false);
        view.getSecretLabel().setManaged(false);
        view.getAlgorithmChoice().getSelectionModel().selectFirst();
        view.getSeedField().clear();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
