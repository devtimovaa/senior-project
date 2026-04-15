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

//Handles user interaction for the Embed tab
public class EmbeddingController extends BaseController {
    private final EmbeddingModel model;
    private final EmbeddingView view;
    private File selectedCoverFile;
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
        view.getChooseImageButton().setOnAction(event -> openCoverChooser());
        view.getChooseSecretImageButton().setOnAction(event -> openSecretImageChooser());
        view.getSubmitButton().setOnAction(event -> handleSubmit());
        view.getClearButton().setOnAction(event -> handleClear());

        //Text and image input depending on the secret type
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

        //Only show the key field for algorithms that need one
        view.getAlgorithmChoice().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean needsKey = "Randomized LSB".equals(newVal) || "Josephus LSB 3-3-2".equals(newVal);
            view.getKeyBox().setVisible(needsKey);
            view.getKeyBox().setManaged(needsKey);
        });
    }

    private void openCoverChooser() {
        File file = openFileChooser("Image Files", "*.png");
        if (file != null) {
            selectedCoverFile = file;
            view.getCoverImageView().setImage(new Image(file.toURI().toString()));
        }
    }

    private void openSecretImageChooser() {
        File file = openFileChooser("Image Files", "*.png", "*.jpg", "*.jpeg");
        if (file != null) {
            selectedSecretFile = file;
            view.getSecretImageView().setImage(new Image(file.toURI().toString()));
        }
    }

    //Opens a file chooser with the given extension filter
    private File openFileChooser(String description, String... extensions) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensions));
        Window window = view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null;
        if (window == null) return null;
        return fc.showOpenDialog(window);
    }

    //Validates inputs, embeds the secret and saves the result
    private void handleSubmit() {
        if (selectedCoverFile == null) {
            showAlert(Alert.AlertType.WARNING, "Image Missing", "Please choose an image!");
            return;
        }

        byte[] secret = prepareSecret();
        if (secret == null) return;

        String algorithm = view.getAlgorithmChoice().getValue();
        int key = parseKey(algorithm, view.getKeyField());
        if (key == Integer.MIN_VALUE) return;

        File outputFile = openSaveDialog();
        if (outputFile == null) return;

        try {
            BufferedImage coverImage = ImageIO.read(selectedCoverFile);
            if (coverImage == null) {
                showAlert(Alert.AlertType.ERROR, "Image Error", "Could not read the selected image.");
                return;
            }

            BufferedImage stegoImage = model.embed(coverImage, secret, algorithm, key);

            ImageIO.write(stegoImage, "png", outputFile);
            view.getResultImageView().setImage(SwingFXUtils.toFXImage(stegoImage, null));
            view.getStatusLabel().setText("Saved: " + outputFile.getName());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Embedding Failed",
                    ex.getMessage() != null ? ex.getMessage() : "Unable to embed the message.");
        }
    }

    //Converts the user's text or image input into the byte array
    private byte[] prepareSecret() {
        boolean hidingImage = "Image".equals(view.getSecretTypeChoice().getValue());
        if (hidingImage) {
            if (selectedSecretFile == null) {
                showAlert(Alert.AlertType.WARNING, "Secret Image Missing", "Please choose a secret image to embed.");
                return null;
            }
            try {
                BufferedImage secretImage = ImageIO.read(selectedSecretFile);
                if (secretImage == null) {
                    showAlert(Alert.AlertType.ERROR, "Image Error", "Could not read the secret image.");
                    return null;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(secretImage, "png", baos);
                return baos.toByteArray();
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Image Error", "Failed to encode secret image: " + e.getMessage());
                return null;
            }
        } else {
            String text = view.getSecretTextArea().getText();
            if (text == null || text.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Message Missing", "Please enter a secret message to embed.");
                return null;
            }
            return text.getBytes(StandardCharsets.UTF_8);
        }
    }

    //Opens a save dialog for the output PNG
    private File openSaveDialog() {
        Window window = view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null;
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File file = fc.showSaveDialog(window);
        if (file != null && !file.getName().endsWith(".png")) {
            file = new File(file.getAbsolutePath() + ".png");
        }
        return file;
    }

    private void handleClear() {
        Image defaultImage = view.loadDefaultImage();
        selectedCoverFile = null;
        selectedSecretFile = null;
        view.getCoverImageView().setImage(defaultImage);
        view.getResultImageView().setImage(defaultImage);
        view.getSecretTextArea().clear();
        view.getSecretImageView().setImage(defaultImage);
        view.getSecretImageView().setVisible(false);
        view.getSecretImageView().setManaged(false);
        view.getSecretTypeChoice().getSelectionModel().selectFirst();
        view.getAlgorithmChoice().getSelectionModel().selectFirst();
        view.getKeyField().clear();
        view.getStatusLabel().setText("");
    }
}
