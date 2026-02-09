package com.example.seniorproject.controllers;

import java.io.InputStream;
import java.util.Objects;
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
import javafx.stage.Stage;
import javafx.stage.Window;

//This class builds the UI pane for embedding a secret message in an image
public class EmbeddingPane {
    private final VBox root; //root container
    private final ImageView baseImageView; //show the original choosen image
    private final ImageView resultImageView; //show the resulted image

    public Node getNode() {
        return root;
    }

    //constructor for the UI
    public EmbeddingPane(Stage primaryStage) {
        //load a default image to appear
        Image defaultImage = loadDefaultImage();
        //image views - for original as well as the end image
        baseImageView = createImageView(defaultImage);
        resultImageView = createImageView(defaultImage);


        //Pane for the original image

        //Left pane holding the original image and the button
        Label baseLabel = new Label("Original Image");
        Button chooseImageButton = new Button("Choose image");
        chooseImageButton.setOnAction(event -> openImageChooser()); //action which the button performs
        //display the pane in a VBox (left)
        VBox baseSection = new VBox(10, baseLabel, chooseImageButton, baseImageView);
        baseSection.setPadding(new Insets(10));
        baseSection.setPrefWidth(450); //preview width of the image

        //Right pane with a textbox and a button to embed the text
        Label secretLabel = new Label("Secret Message:");
        //placeholder for what to be embedded in the image - at this time text
        ChoiceBox<String> secretTypeChoice =
                new ChoiceBox<>(FXCollections.observableArrayList("Text"));
        secretTypeChoice.getSelectionModel().selectFirst();
        Label secretTextLabel = new Label("Message to be embedded:");
        //area to type the message
        TextArea secretTextArea = new TextArea();
        secretTextArea.setPromptText("Write the secret message to be embedded:");
        //display the pane in a VBox (right)
        VBox secretSection = new VBox(10, secretLabel, secretTypeChoice, secretTextLabel, secretTextArea);
        secretSection.setPadding(new Insets(10));
        secretSection.setPrefWidth(450);
        // first row - consists of the original image section and the secret section side by side
        HBox row1 = new HBox(10, baseSection, secretSection);

        //Section for the choice of algorithm
        Label algorithmLabel = new Label("Steganograohy Algorithm:");
        //placeholder for what are the choices for algorithms
        ChoiceBox<String> algorithmChoice =
                new ChoiceBox<>(FXCollections.observableArrayList("LSB", "Randomized LSB", "DCT"));
        algorithmChoice.getSelectionModel().selectFirst(); //showcase the first option
        //button to confirm the algorithm to be used
        Button submitButton = new Button("Submit");
        //second row showcasing the algorithms that can be used
        HBox row2 = new HBox(10, algorithmLabel, algorithmChoice, submitButton);
        row2.setPadding(new Insets(10));

        //third row - preview pane with the end result umage
        HBox row3 = new HBox(10, resultImageView);
        row3.setPadding(new Insets(10));

        //stack all vertically
        root = new VBox(10, row1, row2, row3);
        root.setPadding(new Insets(10));
    }

    //create consistent image
    private ImageView createImageView(Image image) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(300);
        imageView.setFitHeight(300);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    //load the default image
    private Image loadDefaultImage() {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getResourceAsStream("/com/example/seniorproject/img.png"))) {
            return new Image(stream);
        } catch (Exception ex) {
            //exception handling
            return new WritableImage(1, 1);
        }
    }

    //lets the user choose an image
    private void openImageChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        Window window = root.getScene() != null ? root.getScene().getWindow() : null;
        if (window == null) { //exception handling
            return;
        }
        java.io.File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            Image image = new Image(file.toURI().toString());
            baseImageView.setImage(image);
        }
    }
}

