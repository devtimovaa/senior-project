package com.example.seniorproject.controllers;

import com.example.seniorproject.algorithms.LSBSteganography;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
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

//pane for extracting a secret message from an image
public class ExtractingPane {
    private final VBox root; //root container
    private final ImageView imageView; //show the chosen/default image (top left)
    private final ImageView resultImageView; //show the original image after extraction (below right)
    private final TextArea extractedTextArea; //display the extracted secret (below right)
    private final Button submitButton;
    private final ChoiceBox<String> algorithmChoice;

    public Node getNode() {
        return root;
    }

    //constructor for the UI
    public ExtractingPane() {
        //load a default image to appear (top left)
        Image defaultImage = loadDefaultImage();
        imageView = createImageView(defaultImage);
        resultImageView = createImageView(defaultImage); // default image until extraction is done

        extractedTextArea = new TextArea();
        extractedTextArea.setEditable(false);
        extractedTextArea.setPromptText("Extracted message will appear here");

        //algorithm choices
        algorithmChoice = new ChoiceBox<>(
                FXCollections.observableArrayList("LSB", "Randomized LSB", "DCT"));
        algorithmChoice.getSelectionModel().selectFirst();

        submitButton = new Button("Submit");
        submitButton.setOnAction(event -> clickSubmit());

        //image selection and default/chosen image
        Label imageLabel = new Label("Image to Extract From");
        Button chooseButton = new Button("Choose image");
        chooseButton.setOnAction(event -> openImageChooser());
        VBox leftColumn = new VBox(10, imageLabel, chooseButton, imageView);
        leftColumn.setPadding(new Insets(10));
        leftColumn.setPrefWidth(350);

        //algorithm choice and submit button (centered in right area)
        Label algorithmLabel = new Label("Steganography Algorithm:");
        HBox controlsRow = new HBox(10, algorithmLabel, algorithmChoice, submitButton);
        controlsRow.setAlignment(Pos.CENTER);
        VBox topRight = new VBox(controlsRow);
        topRight.setPadding(new Insets(10));
        topRight.setPrefWidth(450);
        topRight.setAlignment(Pos.CENTER);

        //first row - left (image to extract from + default/loaded image), right (algorithm + submit)
        HBox row1 = new HBox(10, leftColumn, topRight);

        //second row - left (original Image), right (extracted secret and text area)
        Label originalImageLabel = new Label("Original Image");
        VBox row2Left = new VBox(10, originalImageLabel, resultImageView);
        row2Left.setPadding(new Insets(10));
        row2Left.setPrefWidth(350);

        Label secretLabel = new Label("Extracted Secret:");
        VBox row2Right = new VBox(10, secretLabel, extractedTextArea);
        row2Right.setPadding(new Insets(10));
        row2Right.setPrefWidth(450);

        HBox row2 = new HBox(10, row2Left, row2Right);

        root = new VBox(10, row1, row2);
        root.setPadding(new Insets(10));
    }

    //consistent image view
    private ImageView createImageView(Image image) {
        ImageView view = new ImageView(image);
        view.setFitWidth(300);
        view.setFitHeight(300);
        view.setPreserveRatio(true);
        return view;
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

    //choose an image
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
            imageView.setImage(image);
        }
    }

    // When user clicks Submit: get image, extract message, show it
    private void clickSubmit() {
        Image image = imageView.getImage();
        if (image == null) {
            showWarning("Please choose an image first.");
            return;
        }

        try {
            BufferedImage buffered = SwingFXUtils.fromFXImage(image, null);
            if (buffered == null) {
                showError("Could not use this image.");
                return;
            }

            String message = extractMessage(buffered);
            resultImageView.setImage(image);
            extractedTextArea.setText(message);

            if (message.isEmpty()) {
                showInfo("No secret message found in this image.");
            }
        } catch (Exception e) {
            showError("Something went wrong. Try another image.");
        }
    }

    // Use the selected algorithm to get the hidden text from the image
    private String extractMessage(BufferedImage buffered) {
        String algorithm = algorithmChoice.getSelectionModel().getSelectedItem();
        if ("LSB".equals(algorithm)) {
            return new LSBSteganography().extract(buffered);
        }
        return "";
    }

    private void showWarning(String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Image Missing");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showInfo(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Result");
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
