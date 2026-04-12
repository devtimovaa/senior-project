package com.example.seniorproject.view;

import java.io.InputStream;
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
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

// Pane for extracting a secret message from an image
public class ExtractingView {
    private final VBox root; //root container
    private final ImageView imageView; //show the chosen/default image (top left)
    private final TextArea extractedTextArea; //display the extracted secret
    private final Label secretLabel;
    private final Button submitButton;
    private final Button chooseButton;
    private final Button clearButton;
    private final ChoiceBox<String> algorithmChoice;
    private final TextField seedField;
    private final VBox seedBox;
    private final ImageView extractedImageView;

    public Node getNode() {
        return root;
    }

    // Constructor for the UI
    public ExtractingView() {
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

        clearButton = new Button("Clear");

        // Image selection and default/chosen image
        Label imageLabel = new Label("Image to Extract From");
        chooseButton = new Button("Choose image");
        VBox leftColumn = new VBox(10, imageLabel, chooseButton, imageView);
        leftColumn.setPadding(new Insets(10));
        leftColumn.setPrefWidth(350);

     
        Label algorithmLabel = new Label("Steganography Algorithm:");
        HBox controlsRow = new HBox(10, algorithmLabel, algorithmChoice, submitButton, clearButton);
        controlsRow.setAlignment(Pos.CENTER);

        Label seedLabel = new Label("Key (integer):");
        seedField = new TextField();
        seedField.setPromptText("Enter an integer key");
        seedBox = new VBox(5, seedLabel, seedField);
        seedBox.setVisible(false);
        seedBox.setManaged(false);

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
    public Image loadDefaultImage() {
        InputStream stream = getClass().getResourceAsStream("/com/example/seniorproject/img.png");
        if (stream != null) {
            return new Image(stream);
        }
        return new WritableImage(1, 1);
    }

    public VBox getRoot() { return root; }
    public ImageView getImageView() { return imageView; }
    public TextArea getExtractedTextArea() { return extractedTextArea; }
    public Label getSecretLabel() { return secretLabel; }
    public Button getSubmitButton() { return submitButton; }
    public Button getChooseButton() { return chooseButton; }
    public Button getClearButton() { return clearButton; }
    public ChoiceBox<String> getAlgorithmChoice() { return algorithmChoice; }
    public TextField getSeedField() { return seedField; }
    public VBox getSeedBox() { return seedBox; }
    public ImageView getExtractedImageView() { return extractedImageView; }
}
