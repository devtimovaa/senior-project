package com.example.seniorproject.view;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

//Builds the UI layout for the Extract tab
public class ExtractingView extends BaseView {
    private static final int IMAGE_SIZE = 300;
    private static final int EXTRACTED_IMAGE_SIZE = 400;

    private final VBox root;
    private final ImageView imageView;
    private final TextArea extractedTextArea;
    private final Label secretLabel;
    private final Button submitButton;
    private final Button chooseButton;
    private final Button clearButton;
    private final ChoiceBox<String> algorithmChoice;
    private final TextField keyField;
    private final VBox keyBox;
    private final ImageView extractedImageView;

    public Node getNode() {
        return root;
    }

    public ExtractingView() {
        Image defaultImage = loadDefaultImage();
        imageView = createImageView(defaultImage, IMAGE_SIZE);

        extractedTextArea = new TextArea();
        extractedTextArea.setEditable(false);
        extractedTextArea.setPromptText("Extracted message will appear here");
        extractedTextArea.setVisible(false);
        extractedTextArea.setManaged(false);

        algorithmChoice = new ChoiceBox<>(
                FXCollections.observableArrayList("LSB", "Randomized LSB", "Josephus LSB 3-3-2"));
        algorithmChoice.getSelectionModel().selectFirst();

        submitButton = new Button("Submit");
        clearButton = new Button("Clear");

        // Left column - image to extract from
        Label imageLabel = new Label("Image to Extract From");
        chooseButton = new Button("Choose image");
        VBox leftColumn = new VBox(10, imageLabel, chooseButton, imageView);
        leftColumn.setPadding(new Insets(10));
        leftColumn.setPrefWidth(350);

        // Right column - algorithm selection and key input
        Label algorithmLabel = new Label("Steganography Algorithm:");
        HBox controlsRow = new HBox(10, algorithmLabel, algorithmChoice, submitButton, clearButton);
        controlsRow.setAlignment(Pos.CENTER);

        Label keyLabel = new Label("Key (integer):");
        keyField = new TextField();
        keyField.setPromptText("Enter an integer key");
        keyBox = new VBox(5, keyLabel, keyField);
        keyBox.setVisible(false);
        keyBox.setManaged(false);

        VBox topRight = new VBox(10, controlsRow, keyBox);
        topRight.setPadding(new Insets(10));
        topRight.setPrefWidth(450);
        topRight.setAlignment(Pos.CENTER);

        // Row 1 - left (image), right (algorithm + submit)
        HBox row1 = new HBox(10, leftColumn, topRight);

        // Row 2 - extracted secret (text or image), shown only after submission
        extractedImageView = new ImageView();
        extractedImageView.setFitWidth(EXTRACTED_IMAGE_SIZE);
        extractedImageView.setFitHeight(EXTRACTED_IMAGE_SIZE);
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

    public VBox getRoot() { return root; }
    public ImageView getImageView() { return imageView; }
    public TextArea getExtractedTextArea() { return extractedTextArea; }
    public Label getSecretLabel() { return secretLabel; }
    public Button getSubmitButton() { return submitButton; }
    public Button getChooseButton() { return chooseButton; }
    public Button getClearButton() { return clearButton; }
    public ChoiceBox<String> getAlgorithmChoice() { return algorithmChoice; }
    public TextField getKeyField() { return keyField; }
    public VBox getKeyBox() { return keyBox; }
    public ImageView getExtractedImageView() { return extractedImageView; }
}
