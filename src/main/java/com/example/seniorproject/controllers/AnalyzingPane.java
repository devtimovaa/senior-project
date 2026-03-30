package com.example.seniorproject.controllers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

// Pane for analyzing an image
public class AnalyzingPane {
    private final VBox root;
    private final VBox imageArea = new VBox(10);
    private final List<Node> imageRows = new ArrayList<>();

    private File selectedStegoFile;
    private File selectedOriginalFile;

    private ImageView activeOriginalImageView;
    private ImageView activeStegoImageView;
    private ImageView activeResultImageView;
    private Label activeOriginalLabel;
    private Label activeStegoLabel;
    private Label activeResultLabel;
    private VBox activeRow;

    public Node getNode() {
        return root;
    }

    public AnalyzingPane() {
        Button chooseOriginalButton = new Button("Choose original image");
        chooseOriginalButton.setOnAction(event -> openOriginalChooser());

        Button chooseStegoButton = new Button("Choose stego image");
        chooseStegoButton.setOnAction(event -> openStegoChooser());

        Button analyzeButton = new Button("Analyze");
        analyzeButton.setOnAction(event -> handleAnalyze());

        Button clearButton = new Button("Clear");
        clearButton.setOnAction(event -> {
            imageArea.getChildren().removeAll(imageRows);
            imageRows.clear();
            selectedStegoFile = null;
            selectedOriginalFile = null;
            addDefaultRow();
        });

        HBox controlsRow = new HBox(10, chooseOriginalButton, chooseStegoButton, analyzeButton, clearButton);
        controlsRow.setPadding(new Insets(10));

        VBox column = new VBox(10, controlsRow, imageArea);
        column.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(column);
        scrollPane.setFitToWidth(true);

        root = new VBox(scrollPane);

        addDefaultRow();
    }

    // Default image row
    private void addDefaultRow() {
        Image defaultImage = loadDefaultImage();

        activeOriginalLabel = new Label("No original image selected");
        activeOriginalImageView = createImageView(defaultImage);
        VBox originalColumn = new VBox(5, activeOriginalLabel, activeOriginalImageView);

        activeStegoLabel = new Label("No stego image selected");
        activeStegoImageView = createImageView(defaultImage);
        VBox stegoColumn = new VBox(5, activeStegoLabel, activeStegoImageView);

        activeResultLabel = new Label("Result");
        activeResultImageView = createImageView(defaultImage);
        VBox resultColumn = new VBox(5, activeResultLabel, activeResultImageView);

        activeRow = new VBox(5, new HBox(10, originalColumn, stegoColumn, resultColumn));
        activeRow.setPadding(new Insets(10));

        imageRows.add(activeRow);
        imageArea.getChildren().add(activeRow);
    }

    private void openStegoChooser() {
        File file = openPngChooser();
        if (file != null) {
            selectedStegoFile = file;
            activeStegoImageView.setImage(new Image(file.toURI().toString()));
            activeStegoLabel.setText(file.getName() + " (stego)");
        }
    }

    private void openOriginalChooser() {
        File file = openPngChooser();
        if (file != null) {
            selectedOriginalFile = file;
            activeOriginalImageView.setImage(new Image(file.toURI().toString()));
            activeOriginalLabel.setText(file.getName() + " (original)");
        }
    }

    private File openPngChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png"));
        Window window = root.getScene() != null ? root.getScene().getWindow() : null;
        if (window == null) return null;
        return fileChooser.showOpenDialog(window);
    }

    private void handleAnalyze() {
        if (selectedStegoFile == null) {
            showAlert(Alert.AlertType.WARNING, "Stego Image Missing", "Please choose a stego image first.");
            return;
        }
        try {
           
            BufferedImage stego = ImageIO.read(selectedStegoFile);
            if (stego == null) {
                showAlert(Alert.AlertType.ERROR, "Load Failed", "Could not read the stego image.");
                return;
            }

            long startTime = System.nanoTime();

            BufferedImage lsbResult = showLastBitOnly(stego);

            BufferedImage diffResult = null;
            int modifiedPixels = 0;
            if (selectedOriginalFile != null) {
                BufferedImage original = ImageIO.read(selectedOriginalFile);
                if (original == null) {
                    showAlert(Alert.AlertType.ERROR, "Load Failed", "Could not read the original image.");
                    return;
                }
                if (original.getWidth() != stego.getWidth() || original.getHeight() != stego.getHeight()) {
                    showAlert(Alert.AlertType.WARNING, "Size Mismatch",
                            "Original and stego images must be the same size for comparison.");
                    return;
                }
                int[] counts = new int[1];
                diffResult = showDifference(original, stego, counts);
                modifiedPixels = counts[0];
            }

            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            long memoryBytes = (long) stego.getWidth() * stego.getHeight() * 3;
            StringBuilder stats = new StringBuilder(String.format(
                    "Size: %d x %d px  |  Simulation time: %d ms  |  Memory: %s",
                    stego.getWidth(), stego.getHeight(), elapsedMs, formatBytes(memoryBytes)));
            if (diffResult != null) {
                int totalPixels = stego.getWidth() * stego.getHeight();
                stats.append(String.format("  |  Modified pixels: %d / %d (%.2f%%)",
                        modifiedPixels, totalPixels, 100.0 * modifiedPixels / totalPixels));
            }

            if (diffResult != null) {
                activeResultImageView.setImage(SwingFXUtils.toFXImage(diffResult, null));
                activeResultLabel.setText("Result: changed pixels (white = modified)");
            } else {
                activeResultImageView.setImage(SwingFXUtils.toFXImage(lsbResult, null));
                activeResultLabel.setText("Result: LSB plane (no original loaded)");
            }

            activeRow.getChildren().add(new Label(stats.toString()));

            selectedStegoFile = null;
            selectedOriginalFile = null;
            addDefaultRow();

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Analysis Failed",
                    ex.getMessage() != null ? ex.getMessage() : "Unable to analyze the image.");
        }
    }

    private static BufferedImage showLastBitOnly(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int r = ((rgb >> 16) & 0x01) * 255;
                int g = ((rgb >> 8)  & 0x01) * 255;
                int b = (rgb         & 0x01) * 255;
                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    private static BufferedImage showDifference(BufferedImage original, BufferedImage stego, int[] counts) {
        int width = stego.getWidth();
        int height = stego.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int modified = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int oRgb = original.getRGB(x, y);
                int sRgb = stego.getRGB(x, y);
                int rDiff = ((oRgb >> 16) ^ (sRgb >> 16)) & 0x01;
                int gDiff = ((oRgb >>  8) ^ (sRgb >>  8)) & 0x01;
                int bDiff = (oRgb         ^  sRgb)         & 0x01;
                if (rDiff != 0 || gDiff != 0 || bDiff != 0) modified++;
                result.setRGB(x, y, (rDiff * 255 << 16) | (gDiff * 255 << 8) | (bDiff * 255));
            }
        }
        counts[0] = modified;
        return result;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }

    private ImageView createImageView(Image image) {
        ImageView view = new ImageView(image);
        view.setFitWidth(270);
        view.setFitHeight(270);
        view.setPreserveRatio(true);
        return view;
    }

    private Image loadDefaultImage() {
        InputStream stream = getClass().getResourceAsStream("/com/example/seniorproject/img.png");
        if (stream != null) {
            return new Image(stream);
        }
        return new WritableImage(1, 1);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
