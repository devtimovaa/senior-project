package com.example.seniorproject.view;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

//Builds the UI layout for the Embed tab
public class EmbeddingView extends BaseView {
    private static final int IMAGE_SIZE = 300;

    private final VBox root;
    private final ImageView coverImageView;
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
    private final TextField keyField;
    private final VBox keyBox;

    public Node getNode() {
        return root;
    }

    public EmbeddingView() {
        Image defaultImage = loadDefaultImage();
        coverImageView = createImageView(defaultImage, IMAGE_SIZE);
        resultImageView = createImageView(defaultImage, IMAGE_SIZE);

        // Row 1 - left (original image), right (secret message)
        Label coverLabel = new Label("Original Image");
        chooseImageButton = new Button("Choose image");
        VBox coverSection = new VBox(10, coverLabel, chooseImageButton, coverImageView);
        coverSection.setPadding(new Insets(10));
        coverSection.setPrefWidth(450);

        Label secretLabel = new Label("Secret Message:");
        secretTypeChoice = new ChoiceBox<>(FXCollections.observableArrayList("Text", "Image"));
        secretTypeChoice.getSelectionModel().selectFirst();

        secretTextLabel = new Label("Message to be embedded:");
        secretTextArea = new TextArea();
        secretTextArea.setPromptText("Write the secret message to be embedded:");

        Image defaultSecretImage = loadDefaultImage();
        secretImageView = createImageView(defaultSecretImage, IMAGE_SIZE);
        secretImageView.setVisible(false);
        secretImageView.setManaged(false);

        chooseSecretImageButton = new Button("Choose secret image");
        chooseSecretImageButton.setVisible(false);
        chooseSecretImageButton.setManaged(false);

        VBox secretSection = new VBox(10, secretLabel, secretTypeChoice, secretTextLabel, secretTextArea, chooseSecretImageButton, secretImageView);
        secretSection.setPadding(new Insets(10));
        secretSection.setPrefWidth(450);
        HBox row1 = new HBox(10, coverSection, secretSection);

        // Row 2 - algorithm choice, submit button, and optional key field
        Label algorithmLabel = new Label("Steganography Algorithm:");
        algorithmChoice = new ChoiceBox<>(FXCollections.observableArrayList("LSB", "Randomized LSB", "Josephus LSB 3-3-2"));
        algorithmChoice.getSelectionModel().selectFirst();
        submitButton = new Button("Submit");
        clearButton = new Button("Clear");
        statusLabel = new Label("");
        HBox controlsRow = new HBox(10, algorithmLabel, algorithmChoice, submitButton, clearButton, statusLabel);

        Label keyLabel = new Label("Key (integer):");
        keyField = new TextField();
        keyField.setPromptText("Enter an integer key");
        keyBox = new VBox(5, keyLabel, keyField);
        keyBox.setVisible(false);
        keyBox.setManaged(false);

        VBox row2 = new VBox(5, controlsRow, keyBox);
        row2.setPadding(new Insets(10));

        // Row 3 - result image preview
        HBox row3 = new HBox(10, resultImageView);
        row3.setPadding(new Insets(10));

        root = new VBox(10, row1, row2, row3);
        root.setPadding(new Insets(10));
    }

    public VBox getRoot() { return root; }
    public ImageView getCoverImageView() { return coverImageView; }
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
    public TextField getKeyField() { return keyField; }
    public VBox getKeyBox() { return keyBox; }
}
