package com.example.seniorproject.controller;

import com.example.seniorproject.model.ExtractingModel;
import com.example.seniorproject.view.ExtractingView;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;

//Handles user interaction for the Extract tab
public class ExtractingController extends BaseController {
    private final ExtractingModel model;
    private final ExtractingView view;
    private File selectedStegoFile;

    public ExtractingController(ExtractingModel model, ExtractingView view) {
        this.model = model;
        this.view = view;
        initEventHandlers();
    }

    public Node getNode() {
        return view.getNode();
    }

    private void initEventHandlers() {
        view.getChooseButton().setOnAction(event -> openStegoChooser());
        view.getSubmitButton().setOnAction(event -> handleSubmit());
        view.getClearButton().setOnAction(event -> handleClear());

        //Only show the key field for algorithms that need one
        view.getAlgorithmChoice().getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean needsKey = "Randomized LSB".equals(newVal) || "Josephus LSB 3-3-2".equals(newVal);
            view.getKeyBox().setVisible(needsKey);
            view.getKeyBox().setManaged(needsKey);
        });
    }

    //Lets the user pick a stego image and resets previous results
    private void openStegoChooser() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png"));
        Window window = view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null;
        if (window == null) return;

        File file = fc.showOpenDialog(window);
        if (file != null) {
            selectedStegoFile = file;
            view.getImageView().setImage(new Image(file.toURI().toString()));
            clearResults();
        }
    }

    //Validates inputs, extracts the secret and displays the result
    private void handleSubmit() {
        if (selectedStegoFile == null) {
            showAlert(Alert.AlertType.WARNING, "Image Missing", "Please choose an image!");
            return;
        }

        try {
            BufferedImage stegoImage = ImageIO.read(selectedStegoFile);
            if (stegoImage == null) {
                showAlert(Alert.AlertType.ERROR, "Load Failed", "Could not read the image file.");
                return;
            }

            String algorithm = view.getAlgorithmChoice().getValue();
            int key = parseKey(algorithm, view.getKeyField());
            if (key == Integer.MIN_VALUE) return;

            byte[] secret = model.extract(stegoImage, algorithm, key);
            displayResult(secret);

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Extraction Failed",
                    ex.getMessage() != null ? ex.getMessage() : "Unable to extract the secret.");
        }
    }

    //Shows the extracted secret as either an image or text
    private void displayResult(byte[] secret) throws Exception {
        view.getSecretLabel().setVisible(true);
        view.getSecretLabel().setManaged(true);

        if (ExtractingModel.isPngBytes(secret)) {
            BufferedImage hiddenImage = ImageIO.read(new ByteArrayInputStream(secret));
            if (hiddenImage == null) {
                showAlert(Alert.AlertType.ERROR, "Decode Failed", "Could not decode the hidden image.");
                return;
            }
            view.getExtractedImageView().setImage(SwingFXUtils.toFXImage(hiddenImage, null));
            view.getExtractedImageView().setVisible(true);
            view.getExtractedImageView().setManaged(true);
            view.getExtractedTextArea().setVisible(false);
            view.getExtractedTextArea().setManaged(false);
        } else {
            String extracted = secret.length == 0 ? "" : new String(secret, StandardCharsets.UTF_8);
            view.getExtractedTextArea().setText(extracted);
            view.getExtractedTextArea().setVisible(true);
            view.getExtractedTextArea().setManaged(true);
            view.getExtractedImageView().setVisible(false);
            view.getExtractedImageView().setManaged(false);
            if (extracted.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Empty Result", "No secret message could be found.");
            }
        }
    }

    //Hides the extracted text/image output
    private void clearResults() {
        view.getExtractedTextArea().clear();
        view.getExtractedTextArea().setVisible(false);
        view.getExtractedTextArea().setManaged(false);
        view.getExtractedImageView().setImage(null);
        view.getExtractedImageView().setVisible(false);
        view.getExtractedImageView().setManaged(false);
        view.getSecretLabel().setVisible(false);
        view.getSecretLabel().setManaged(false);
    }

    private void handleClear() {
        Image defaultImage = view.loadDefaultImage();
        selectedStegoFile = null;
        view.getImageView().setImage(defaultImage);
        clearResults();
        view.getAlgorithmChoice().getSelectionModel().selectFirst();
        view.getKeyField().clear();
    }
}
