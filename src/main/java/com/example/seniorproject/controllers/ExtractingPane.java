package com.example.seniorproject.controllers;

import com.example.seniorproject.algorithms.JosephusLSB332Algorithm;
import com.example.seniorproject.algorithms.LSBAlgorithm;
import com.example.seniorproject.algorithms.RandomizedLSBAlgorithm;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
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
    private final TextArea extractedTextArea; //display the extracted secret
    private final Label secretLabel;
    private final Button submitButton;
    private final ChoiceBox<String> algorithmChoice;
    private final TextField seedField;
    private final ImageView extractedImageView;
    private File selectedFile;

    public Node getNode() {
        return root;
    }

    // Constructor for the UI
    public ExtractingPane() {
        // Load a default image to appear (top left)
        Image defaultImage = loadDefaultImage();
        imageView = createImageView(defaultImage);

        extractedTextArea = new TextArea();
        extractedTextArea.setEditable(false);
        extractedTextArea.setPromptText("Extracted message will appear here");
        extractedTextArea.setVisible(false);
        extractedTextArea.setManaged(false);

        // Algorithm choices
        algorithmChoice = new ChoiceBox<>(
                FXCollections.observableArrayList("LSB", "Randomized LSB", "Josephus LSB 3-3-2"));
        algorithmChoice.getSelectionModel().selectFirst();

        submitButton = new Button("Submit");
        submitButton.setOnAction(event -> handleSubmit());

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(event -> handleClear());

        // Image selection and default/chosen image
        Label imageLabel = new Label("Image to Extract From");
        Button chooseButton = new Button("Choose image");
        chooseButton.setOnAction(event -> openImageChooser());
        VBox leftColumn = new VBox(10, imageLabel, chooseButton, imageView);
        leftColumn.setPadding(new Insets(10));
        leftColumn.setPrefWidth(350);

     
        Label algorithmLabel = new Label("Steganography Algorithm:");
        HBox controlsRow = new HBox(10, algorithmLabel, algorithmChoice, submitButton, clearButton);
        controlsRow.setAlignment(Pos.CENTER);

        Label seedLabel = new Label("Key (integer):");
        seedField = new TextField();
        seedField.setPromptText("Enter an integer key");
        VBox seedBox = new VBox(5, seedLabel, seedField);
        seedBox.setVisible(false);
        seedBox.setManaged(false);

        algorithmChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean needsSeed = "Randomized LSB".equals(newVal) || "Josephus LSB 3-3-2".equals(newVal);
            seedBox.setVisible(needsSeed);
            seedBox.setManaged(needsSeed);
            if ("Randomized LSB".equals(newVal)) {
                seedLabel.setText("Key (" + RandomizedLSBAlgorithm.MIN_KEY + " - " + RandomizedLSBAlgorithm.MAX_KEY + "):");
            } else if ("Josephus LSB 3-3-2".equals(newVal)) {
                seedLabel.setText("Key (" + JosephusLSB332Algorithm.MIN_KEY + " - " + JosephusLSB332Algorithm.MAX_KEY + "):");
            }
        });

        VBox topRight = new VBox(10, controlsRow, seedBox);
        topRight.setPadding(new Insets(10));
        topRight.setPrefWidth(450);
        topRight.setAlignment(Pos.CENTER);

        //First row - left (image to extract from + default/loaded image), right (algorithm + submit)
        HBox row1 = new HBox(10, leftColumn, topRight);

        //Second row - extracted secret (text or image), shown only after submission
        extractedImageView = new ImageView();
        extractedImageView.setFitWidth(400);
        extractedImageView.setFitHeight(400);
        extractedImageView.setPreserveRatio(true);
        extractedImageView.setVisible(false);
        extractedImageView.setManaged(false);

        secretLabel = new Label("Extracted Secret:");
        secretLabel.setVisible(false);
        secretLabel.setManaged(false);

        VBox row2 = new VBox(10, secretLabel, extractedTextArea, extractedImageView);
        row2.setPadding(new Insets(10));
        row2.setAlignment(Pos.CENTER);

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

            extractedTextArea.clear();
            extractedTextArea.setVisible(false);
            extractedTextArea.setManaged(false);

            extractedImageView.setImage(null);
            extractedImageView.setVisible(false);
            extractedImageView.setManaged(false);
            
            secretLabel.setVisible(false);
            secretLabel.setManaged(false);
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
            secretLabel.setVisible(true);
            secretLabel.setManaged(true);

            if (isPngBytes(raw)) {
                BufferedImage hiddenImage = ImageIO.read(new ByteArrayInputStream(raw));
                if (hiddenImage == null) {
                    showAlert(Alert.AlertType.ERROR, "Decode Failed", "Could not decode the hidden image.");
                    return;
                }
                extractedImageView.setImage(SwingFXUtils.toFXImage(hiddenImage, null));
                extractedImageView.setVisible(true);
                extractedImageView.setManaged(true);
                extractedTextArea.setVisible(false);
                extractedTextArea.setManaged(false);
            } else {
                String extracted = raw.length == 0 ? "" : new String(raw, StandardCharsets.UTF_8);
                extractedTextArea.setText(extracted);
                extractedTextArea.setVisible(true);
                extractedTextArea.setManaged(true);
                extractedImageView.setVisible(false);
                extractedImageView.setManaged(false);
                if (extracted.isEmpty()) {
                    showAlert(Alert.AlertType.INFORMATION, "Empty Result", "No secret message could be found.");
                }
            }

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Extraction Failed", ex.getMessage() != null ? ex.getMessage() : "Unable to extract the secret.");
        }
    }

    private byte[] extractBytesWithSelectedAlgorithm(BufferedImage buffered) {
        String selection = algorithmChoice.getValue();
        if ("LSB".equals(selection)) {
            return new LSBAlgorithm().extract(buffered);
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
            if (seed < RandomizedLSBAlgorithm.MIN_KEY || seed > RandomizedLSBAlgorithm.MAX_KEY) {
                showAlert(Alert.AlertType.ERROR, "Key Out of Range",
                        "Key must be between " + RandomizedLSBAlgorithm.MIN_KEY + " and " + RandomizedLSBAlgorithm.MAX_KEY + ".");
                return null;
            }
            return new RandomizedLSBAlgorithm(seed).extract(buffered);
        }
        if ("Josephus LSB 3-3-2".equals(selection)) {
            String seedText = seedField.getText();
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
            if (seed < JosephusLSB332Algorithm.MIN_KEY || seed > JosephusLSB332Algorithm.MAX_KEY) {
                showAlert(Alert.AlertType.ERROR, "Key Out of Range",
                        "Key must be between " + JosephusLSB332Algorithm.MIN_KEY + " and " + JosephusLSB332Algorithm.MAX_KEY + ".");
                return null;
            }
            return new JosephusLSB332Algorithm(seed).extract(buffered);
        }
        return new byte[0];
    }

    // Detect if text or image was hidden
    private static boolean isPngBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 8) return false;
        return (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50  
                && bytes[2] == 0x4E  
                && bytes[3] == 0x47  
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private void handleClear() {
        Image defaultImage = loadDefaultImage();
        selectedFile = null;
        imageView.setImage(defaultImage);
        extractedTextArea.clear();
        extractedTextArea.setVisible(false);
        extractedTextArea.setManaged(false);
        extractedImageView.setImage(null);
        extractedImageView.setVisible(false);
        extractedImageView.setManaged(false);
        secretLabel.setVisible(false);
        secretLabel.setManaged(false);
        algorithmChoice.getSelectionModel().selectFirst();
        seedField.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
