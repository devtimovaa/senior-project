package com.example.seniorproject.controllers;
import com.example.seniorproject.algorithms.LSBSteganography;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Objects;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

//pane for embedding a secret message in an image
public class EmbeddingPane {
    private final VBox root;
    private Image inputImage;
    private final ImageView baseImageView;
    private final ImageView resultImageView;
    private final TextArea secretTextArea;
    private final Button submitButton;
    private final ChoiceBox<String> algorithmChoice;
    private final Label statusLabel;

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

        //row 2: algorithm choice, submit button
        Label algorithmLabel = new Label("Steganography Algorithm:");
        algorithmChoice = new ChoiceBox<>(FXCollections.observableArrayList("LSB", "Randomized LSB", "DCT"));
        algorithmChoice.getSelectionModel().selectFirst();
        submitButton = new Button("Submit");
        submitButton.setOnAction(event -> clickSubmit());
        statusLabel = new Label("");
        HBox row2 = new HBox(10, algorithmLabel, algorithmChoice, submitButton, statusLabel);
        row2.setPadding(new Insets(10));

        //row 3: result image preview
        HBox row3 = new HBox(10, resultImageView);
        row3.setPadding(new Insets(10));

        root = new VBox(10, row1, row2, row3);
        root.setPadding(new Insets(10));
    }

    //ImageView
    private ImageView createImageView(Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(300);
        imageView.setFitHeight(300);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    //default image
    private Image loadDefaultImage() {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getResourceAsStream("/com/example/seniorproject/img.png"))) {
            return new Image(stream);
        } catch (Exception ex) {
            return new WritableImage(1, 1);
        }
    }

    //file chooser to select image
    private void openImageChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        Window window = root.getScene() != null ? root.getScene().getWindow() : null;
        if (window == null) {
            return;
        }
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            Image image = new Image(file.toURI().toString());
            inputImage = image;
            baseImageView.setImage(image);
        }
    }

   //submit button
    private void clickSubmit() {
        Window window = root.getScene() != null ? root.getScene().getWindow() : null;
        if (window == null) {
            statusLabel.setText("Error: no window.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG", "*.png"));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg"));
        java.io.File outputFile = fileChooser.showSaveDialog(window);
        if (outputFile == null) {
            return; // user cancelled
        }
        if (inputImage == null) {
            statusLabel.setText("Error: no input image.");
            return;
        }
        String secret = secretTextArea.getText();
        if (secret == null || secret.isEmpty()) {
            statusLabel.setText("Error: no secret message.");
            return;
        }
        try {
            BufferedImage buffered = SwingFXUtils.fromFXImage(inputImage, null);
            if (buffered == null) {
                statusLabel.setText("Error: could not convert image.");
                return;
            }
            String algorithm = algorithmChoice.getSelectionModel().getSelectedItem();
            if ("LSB".equals(algorithm)) {
                buffered = new LSBSteganography().embed(buffered, secret);
            }
            //output
            String path = outputFile.getAbsolutePath().toLowerCase();
            String format = path.endsWith(".jpg") || path.endsWith(".jpeg") ? "jpg" : "png";
            if (!path.endsWith(".png") && !path.endsWith(".jpg") && !path.endsWith(".jpeg")) {
                outputFile = new java.io.File(outputFile.getAbsolutePath() + (format.equals("jpg") ? ".jpg" : ".png"));
            }
            ImageIO.write(buffered, format, outputFile);
            Image resultImage = SwingFXUtils.toFXImage(buffered, null);
            resultImageView.setImage(resultImage);
            statusLabel.setText("Saved: " + outputFile.getName());
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }
}

