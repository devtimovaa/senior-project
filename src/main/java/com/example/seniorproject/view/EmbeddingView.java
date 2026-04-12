package com.example.seniorproject.view;

import java.io.InputStream;
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

// Pane for embedding a secret message in an image
public class EmbeddingView {
    private final VBox root;
    private final ImageView baseImageView;
    private final ImageView resultImageView;
    private final ImageView secretImageView;
    private final TextArea secretTextArea;
    private final Button submitButton;
    private final Button chooseImageButton;
    private final Button chooseSecretImageButton;
    private final Button clearButton;
    private final ChoiceBox<String> algorithmChoice;
    private final ChoiceBox<String> secretTypeChoice;
    private final Label statusLabel;
    private final Label secretTextLabel;
    private final TextField seedField;
    private final VBox seedBox;

    public Node getNode() {
        return root;
    }

    // Image picker, secret message, algorithm and result preview
    public EmbeddingView() {

        Image defaultImage = loadDefaultImage();
        baseImageView = createImageView(defaultImage);
        resultImageView = createImageView(defaultImage);

        // Row 1 -  left (original image), right (secret message)
        Label baseLabel = new Label("Original Image");
        chooseImageButton = new Button("Choose image");
        VBox baseSection = new VBox(10, baseLabel, chooseImageButton, baseImageView);
        baseSection.setPadding(new Insets(10));
        baseSection.setPrefWidth(450);

        Label secretLabel = new Label("Secret Message:");
        secretTypeChoice = new ChoiceBox<>(FXCollections.observableArrayList("Text", "Image"));
        secretTypeChoice.getSelectionModel().selectFirst();

        secretTextLabel = new Label("Message to be embedded:");
        secretTextArea = new TextArea();
        secretTextArea.setPromptText("Write the secret message to be embedded:");

        Image defaultSecretImage = loadDefaultImage();
        secretImageView = createImageView(defaultSecretImage);
        secretImageView.setVisible(false);
        secretImageView.setManaged(false);

        chooseSecretImageButton = new Button("Choose secret image");
        chooseSecretImageButton.setVisible(false);
        chooseSecretImageButton.setManaged(false);

        VBox secretSection = new VBox(10, secretLabel, secretTypeChoice, secretTextLabel, secretTextArea, chooseSecretImageButton, secretImageView);
        secretSection.setPadding(new Insets(10));
        secretSection.setPrefWidth(450);
        HBox row1 = new HBox(10, baseSection, secretSection);

        // Row 2 -  algorithm choice, submit button, and optional seed field
        Label algorithmLabel = new Label("Steganography Algorithm:");
        algorithmChoice = new ChoiceBox<>(FXCollections.observableArrayList("LSB", "Randomized LSB", "Josephus LSB 3-3-2"));
        algorithmChoice.getSelectionModel().selectFirst();
        submitButton = new Button("Submit");
        clearButton = new Button("Clear");
        statusLabel = new Label("");
        HBox controlsRow = new HBox(10, algorithmLabel, algorithmChoice, submitButton, clearButton, statusLabel);

        Label seedLabel = new Label("Key (integer):");
        seedField = new TextField();
        seedField.setPromptText("Enter an integer key");
        seedBox = new VBox(5, seedLabel, seedField);
        seedBox.setVisible(false);
        seedBox.setManaged(false);

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
    public Image loadDefaultImage() {
        InputStream stream = getClass().getResourceAsStream("/com/example/seniorproject/img.png");
        if (stream != null) {
            return new Image(stream);
        }
        return new WritableImage(1, 1);
    }

    public VBox getRoot() { return root; }
    public ImageView getBaseImageView() { return baseImageView; }
    public ImageView getResultImageView() { return resultImageView; }
    public ImageView getSecretImageView() { return secretImageView; }
    public TextArea getSecretTextArea() { return secretTextArea; }
    public Button getSubmitButton() { return submitButton; }
    public Button getChooseImageButton() { return chooseImageButton; }
    public Button getChooseSecretImageButton() { return chooseSecretImageButton; }
    public Button getClearButton() { return clearButton; }
    public ChoiceBox<String> getAlgorithmChoice() { return algorithmChoice; }
    public ChoiceBox<String> getSecretTypeChoice() { return secretTypeChoice; }
    public Label getStatusLabel() { return statusLabel; }
    public Label getSecretTextLabel() { return secretTextLabel; }
    public TextField getSeedField() { return seedField; }
    public VBox getSeedBox() { return seedBox; }
}
