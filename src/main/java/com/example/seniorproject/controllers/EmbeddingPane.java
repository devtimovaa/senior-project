package com.example.seniorproject.controllers;
import com.example.seniorproject.algorithms.LSBAlgorithm;
import com.example.seniorproject.algorithms.RandomizedLSBAlgorithm;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

// Pane for embedding a secret message in an image
public class EmbeddingPane {
    private final VBox root;
    private File selectedFile;
    private final ImageView baseImageView;
    private final ImageView resultImageView;
    private final TextArea secretTextArea;
    private final Button submitButton;
    private final ChoiceBox<String> algorithmChoice;
    private final Label statusLabel;
    private final TextField seedField;

    public Node getNode() {
        return root;
    }

    // Image picker, secret message, algorithm and result preview
    public EmbeddingPane() {

        Image defaultImage = loadDefaultImage();
        baseImageView = createImageView(defaultImage);
        resultImageView = createImageView(defaultImage);

        // Row 1 -  left (original image), right (secret message)
        Label baseLabel = new Label("Original Image");
        Button chooseImageButton = new Button("Choose image");
        chooseImageButton.setOnAction(event -> openImageChooser());
        VBox baseSection = new VBox(10, baseLabel, chooseImageButton, baseImageView);
        baseSection.setPadding(new Insets(10));
        baseSection.setPrefWidth(450);

        Label secretLabel = new Label("Secret Message:");
        ChoiceBox<String> secretTypeChoice =
                new ChoiceBox<>(FXCollections.observableArrayList("Text"));
        secretTypeChoice.getSelectionModel().selectFirst();
        Label secretTextLabel = new Label("Message to be embedded:");
        secretTextArea = new TextArea();
        secretTextArea.setPromptText("Write the secret message to be embedded:");
        VBox secretSection = new VBox(10, secretLabel, secretTypeChoice, secretTextLabel, secretTextArea);
        secretSection.setPadding(new Insets(10));
        secretSection.setPrefWidth(450);
        HBox row1 = new HBox(10, baseSection, secretSection);

        // Row 2 -  algorithm choice, submit button, and optional seed field
        Label algorithmLabel = new Label("Steganography Algorithm:");
        algorithmChoice = new ChoiceBox<>(FXCollections.observableArrayList("LSB", "Randomized LSB"));
        algorithmChoice.getSelectionModel().selectFirst();
        submitButton = new Button("Submit");
        submitButton.setOnAction(event -> handleSubmit());
        statusLabel = new Label("");
        HBox controlsRow = new HBox(10, algorithmLabel, algorithmChoice, submitButton, statusLabel);

        Label seedLabel = new Label("Key (integer):");
        seedField = new TextField();
        seedField.setPromptText("Enter an integer key");
        VBox seedBox = new VBox(5, seedLabel, seedField);
        seedBox.setVisible(false);
        seedBox.setManaged(false);

        algorithmChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isRandomized = "Randomized LSB".equals(newVal);
            seedBox.setVisible(isRandomized);
            seedBox.setManaged(isRandomized);
        });

        VBox row2 = new VBox(5, controlsRow, seedBox);
        row2.setPadding(new Insets(10));

        // Row 3 -  result image preview
        HBox row3 = new HBox(10, resultImageView);
        row3.setPadding(new Insets(10));

        root = new VBox(10, row1, row2, row3);
        root.setPadding(new Insets(10));
    }

    // ImageView
    private ImageView createImageView(Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(300);
        imageView.setFitHeight(300);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    // Default image
    private Image loadDefaultImage() {
        InputStream stream = getClass().getResourceAsStream("/com/example/seniorproject/img.png");
        if (stream != null) {
            return new Image(stream);
        }
        return new WritableImage(1, 1);
    }

    // File chooser to select image
    private void openImageChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png"));
        Window window = root.getScene() != null ? root.getScene().getWindow() : null;
        if (window == null) {
            return;
        }
        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            selectedFile = file;
            baseImageView.setImage(new Image(file.toURI().toString()));
        }
    }

    // Submit button
    private void handleSubmit() {
        // Validate all inputs before opening the save dialog
        if (selectedFile == null) {
            showAlert(Alert.AlertType.WARNING, "Image Missing", "Please choose an image!");
            return;
        }
        String secret = secretTextArea.getText();
        if (secret == null || secret.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Message Missing", "Please enter a secret message to embed.");
            return;
        }
        String algorithm = algorithmChoice.getValue();
        int seed = 0;
        if ("Randomized LSB".equals(algorithm)) {
            String seedText = seedField.getText();
            if (seedText == null || seedText.isBlank()) {
                showAlert(Alert.AlertType.ERROR, "Key Required", "Please enter an integer key for Randomized LSB.");
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
        Window window = root.getScene() != null ? root.getScene().getWindow() : null;
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

            if ("LSB".equals(algorithm)) {
                buffered = new LSBAlgorithm().embed(buffered, secret.getBytes(StandardCharsets.UTF_8));
            } else if ("Randomized LSB".equals(algorithm)) {
                buffered = new RandomizedLSBAlgorithm(seed).embed(buffered, secret.getBytes(StandardCharsets.UTF_8));
            }

            ImageIO.write(buffered, "png", outputFile);
            resultImageView.setImage(SwingFXUtils.toFXImage(buffered, null));
            statusLabel.setText("Saved: " + outputFile.getName());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Embedding Failed", ex.getMessage() != null ? ex.getMessage() : "Unable to embed the message.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

