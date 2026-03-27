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
    private Image inputImage;
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

    //image picker, secret message, algorithm and result preview
    public EmbeddingPane() {

        Image defaultImage = loadDefaultImage();
        inputImage = defaultImage;
        baseImageView = createImageView(defaultImage);
        resultImageView = createImageView(defaultImage);

        //row 1: left (original image), right (secret message)
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

        Label seedLabel = new Label("Seed (integer):");
        seedField = new TextField();
        seedField.setPromptText("Enter an integer seed");
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

        //row 3: result image preview
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
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        Window window = root.getScene() != null ? root.getScene().getWindow() : null;
        if (window == null) {
            return;
        }
        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            inputImage = new Image(file.toURI().toString());
            baseImageView.setImage(inputImage);
        }
    }

    // Submit button
    private void handleSubmit() {
        if (inputImage == null) {
            statusLabel.setText("Error: no input image.");
            return;
        }
        String secret = secretTextArea.getText();
        if (secret == null || secret.isEmpty()) {
            statusLabel.setText("Error: no secret message.");
            return;
        }

        // Ask the user where to save the output (PNG only — JPEG is lossy and would destroy hidden data)
        Window window = root.getScene() != null ? root.getScene().getWindow() : null;
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File outputFile = fileChooser.showSaveDialog(window);
        if (outputFile == null) {
            return; // user cancelled
        }
        if (!outputFile.getName().endsWith(".png")) {
            outputFile = new File(outputFile.getAbsolutePath() + ".png");
        }

        try {
            BufferedImage buffered = SwingFXUtils.fromFXImage(inputImage, null);
            if (buffered == null) {
                statusLabel.setText("Error: could not read the image.");
                return;
            }

            String algorithm = algorithmChoice.getValue();
            if ("LSB".equals(algorithm)) {
                buffered = new LSBAlgorithm().embed(buffered, secret.getBytes(StandardCharsets.UTF_8));
            } else if ("Randomized LSB".equals(algorithm)) {
                String seedText = seedField.getText();
                if (seedText == null || seedText.isBlank()) {
                    statusLabel.setText("Error: a seed is required for Randomized LSB.");
                    return;
                }
                int seed;
                try {
                    seed = Integer.parseInt(seedText.trim());
                } catch (NumberFormatException e) {
                    statusLabel.setText("Error: seed must be an integer.");
                    return;
                }
                buffered = new RandomizedLSBAlgorithm(seed).embed(buffered, secret.getBytes(StandardCharsets.UTF_8));
            }

            ImageIO.write(buffered, "png", outputFile);
            resultImageView.setImage(SwingFXUtils.toFXImage(buffered, null));
            statusLabel.setText("Saved: " + outputFile.getName());
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }
}

