package com.example.seniorproject.controllers;

import com.example.seniorproject.algorithms.LSBAlgorithm;
import com.example.seniorproject.algorithms.RandomizedLSBAlgorithm;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

// Pane for extracting a secret message from an image
public class ExtractingPane {
    private final VBox root; //root container
    private final ImageView imageView; //show the chosen/default image (top left)
    private final ImageView resultImageView; //show the original image after extraction (below right)
    private final TextArea extractedTextArea; //display the extracted secret (below right)
    private final Button submitButton;
    private final ChoiceBox<String> algorithmChoice;
    private final TextField seedField;
    private File selectedFile;

    public Node getNode() {
        return root;
    }

    // Constructor for the UI
    public ExtractingPane() {
        // Load a default image to appear (top left)
        Image defaultImage = loadDefaultImage();
        imageView = createImageView(defaultImage);
        resultImageView = createImageView(defaultImage); 

        extractedTextArea = new TextArea();
        extractedTextArea.setEditable(false);
        extractedTextArea.setPromptText("Extracted message will appear here");

        // Algorithm choices
        algorithmChoice = new ChoiceBox<>(
                FXCollections.observableArrayList("LSB", "Randomized LSB"));
        algorithmChoice.getSelectionModel().selectFirst();

        submitButton = new Button("Submit");
        submitButton.setOnAction(event -> handleSubmit());

        // Image selection and default/chosen image
        Label imageLabel = new Label("Image to Extract From");
        Button chooseButton = new Button("Choose image");
        chooseButton.setOnAction(event -> openImageChooser());
        VBox leftColumn = new VBox(10, imageLabel, chooseButton, imageView);
        leftColumn.setPadding(new Insets(10));
        leftColumn.setPrefWidth(350);

     
        Label algorithmLabel = new Label("Steganography Algorithm:");
        HBox controlsRow = new HBox(10, algorithmLabel, algorithmChoice, submitButton);
        controlsRow.setAlignment(Pos.CENTER);

        Label seedLabel = new Label("Key:");
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

        VBox topRight = new VBox(10, controlsRow, seedBox);
        topRight.setPadding(new Insets(10));
        topRight.setPrefWidth(450);
        topRight.setAlignment(Pos.CENTER);

        //First row - left (image to extract from + default/loaded image), right (algorithm + submit)
        HBox row1 = new HBox(10, leftColumn, topRight);

        //Second row - left (original Image), right (extracted secret and text area)
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

    // Consistent image view
    private ImageView createImageView(Image image) {
        ImageView view = new ImageView(image);
        view.setFitWidth(300);
        view.setFitHeight(300);
        view.setPreserveRatio(true);
        return view;
    }

    // Default image
    private Image loadDefaultImage() {
        InputStream stream = getClass().getResourceAsStream("/com/example/seniorproject/img.png");
        if (stream != null) {
            return new Image(stream);
        }
        return new WritableImage(1, 1);
    }

    //Choose an image
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
            Image image = new Image(file.toURI().toString());
            imageView.setImage(image);
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
            String extracted = extractWithSelectedAlgorithm(buffered);
            if (extracted == null) {
                return;
            }
            resultImageView.setImage(imageView.getImage());
            extractedTextArea.setText(extracted);

            if (extracted.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Empty Image", "No secret message could be found.");
            }

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Extraction Failed", ex.getMessage() != null ? ex.getMessage() : "Unable to extract the secret.");
        }
    }

    private String extractWithSelectedAlgorithm(BufferedImage buffered) {
        String selection = algorithmChoice.getValue();
        if ("LSB".equals(selection)) {
            byte[] raw = new LSBAlgorithm().extract(buffered);
            return raw.length == 0 ? "" : new String(raw, StandardCharsets.UTF_8);
        }
        if ("Randomized LSB".equals(selection)) {
            String seedText = seedField.getText();
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
            byte[] raw = new RandomizedLSBAlgorithm(seed).extract(buffered);
            return raw.length == 0 ? "" : new String(raw, StandardCharsets.UTF_8);
        }
        return "";
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
