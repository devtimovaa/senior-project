package com.example.seniorproject.controller;

import com.example.seniorproject.model.EmbeddingModel;
import com.example.seniorproject.view.EmbeddingView;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class EmbeddingController {
    private final EmbeddingModel model;
    private final EmbeddingView view;
    private File selectedFile;
    private File selectedSecretFile;

    public EmbeddingController(EmbeddingModel model, EmbeddingView view) {
        this.model = model;
        this.view = view;
        initEventHandlers();
    }

    public Node getNode() {
        return view.getNode();
    }

    private void initEventHandlers() {
        view.getChooseImageButton().setOnAction(event -> openImageChooser());
        view.getChooseSecretImageButton().setOnAction(event -> openSecretImageChooser());
        view.getSubmitButton().setOnAction(event -> handleSubmit());
        view.getClearButton().setOnAction(event -> handleClear());

        view.getSecretTypeChoice().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isImage = "Image".equals(newVal);
            view.getSecretTextLabel().setVisible(!isImage);
            view.getSecretTextLabel().setManaged(!isImage);
            view.getSecretTextArea().setVisible(!isImage);
            view.getSecretTextArea().setManaged(!isImage);
            view.getChooseSecretImageButton().setVisible(isImage);
            view.getChooseSecretImageButton().setManaged(isImage);
            view.getSecretImageView().setVisible(isImage);
            view.getSecretImageView().setManaged(isImage);
        });

        view.getAlgorithmChoice().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean needsSeed = "Randomized LSB".equals(newVal) || "Josephus LSB 3-3-2".equals(newVal);
            view.getSeedBox().setVisible(needsSeed);
            view.getSeedBox().setManaged(needsSeed);
        });
    }

    // File chooser to select image
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
            view.getBaseImageView().setImage(new Image(file.toURI().toString()));
        }
    }

    // File chooser to select the secret image
    private void openSecretImageChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        Window window = view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null;
        if (window == null) {
            return;
        }
        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            selectedSecretFile = file;
            view.getSecretImageView().setImage(new Image(file.toURI().toString()));
        }
    }

    // Submit button logic
    private void handleSubmit() {

        // Validate all inputs before opening the save dialog
        if (selectedFile == null) {
            showAlert(Alert.AlertType.WARNING, "Image Missing", "Please choose an image!");
            return;
        }
        boolean hidingImage = "Image".equals(view.getSecretTypeChoice().getValue());
        byte[] bytesToHide;
        if (hidingImage) {
            if (selectedSecretFile == null) {
                showAlert(Alert.AlertType.WARNING, "Secret Image Missing", "Please choose a secret image to embed.");
                return;
            }
            try {
                BufferedImage secretBuffered = ImageIO.read(selectedSecretFile);
                if (secretBuffered == null) {
                    showAlert(Alert.AlertType.ERROR, "Image Error", "Could not read the secret image.");
                    return;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(secretBuffered, "png", baos);
                bytesToHide = baos.toByteArray();
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Image Error", "Failed to encode secret image: " + e.getMessage());
                return;
            }
        } else {
            String secret = view.getSecretTextArea().getText();
            if (secret == null || secret.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Message Missing", "Please enter a secret message to embed.");
                return;
            }
            bytesToHide = secret.getBytes(StandardCharsets.UTF_8);
        }
        String algorithm = view.getAlgorithmChoice().getValue();
        int seed = 0;
        if ("Randomized LSB".equals(algorithm) || "Josephus LSB 3-3-2".equals(algorithm)) {
            String seedText = view.getSeedField().getText();
            if (seedText == null || seedText.isBlank()) {
                showAlert(Alert.AlertType.ERROR, "Key Required", "Please enter an integer key.");
                return;
            }
            try {
                seed = Integer.parseInt(seedText.trim());
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Invalid Key", "The key must be an integer.");
                return;
            }
        }

        // Ask the user where to save the output (PNG only)
        Window window = view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null;
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File outputFile = fileChooser.showSaveDialog(window);

        if (outputFile == null) {
            return;
        }

        if (!outputFile.getName().endsWith(".png")) {
            outputFile = new File(outputFile.getAbsolutePath() + ".png");
        }

        try {
            BufferedImage buffered = ImageIO.read(selectedFile);
            if (buffered == null) {
                showAlert(Alert.AlertType.ERROR, "Image Error", "Could not read the selected image.");
                return;
            }

            buffered = model.embed(buffered, bytesToHide, algorithm, seed);

            ImageIO.write(buffered, "png", outputFile);
            view.getResultImageView().setImage(SwingFXUtils.toFXImage(buffered, null));
            view.getStatusLabel().setText("Saved: " + outputFile.getName());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Embedding Failed", ex.getMessage() != null ? ex.getMessage() : "Unable to embed the message.");
        }
    }

    private void handleClear() {
        Image defaultImage = view.loadDefaultImage();
        selectedFile = null;
        selectedSecretFile = null;
        view.getBaseImageView().setImage(defaultImage);
        view.getResultImageView().setImage(defaultImage);
        view.getSecretTextArea().clear();
        view.getSecretImageView().setImage(defaultImage);
        view.getSecretImageView().setVisible(false);
        view.getSecretImageView().setManaged(false);
        view.getSecretTypeChoice().getSelectionModel().selectFirst();
        view.getAlgorithmChoice().getSelectionModel().selectFirst();
        view.getSeedField().clear();
        view.getStatusLabel().setText("");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
