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
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

// Pane for analyzing which pixels were modified
public class AnalyzingPane {
    private static final int IMAGE_SIZE = 200;

    private final VBox root;
    private final VBox imageArea = new VBox(10);
    private final List<Node> imageRows = new ArrayList<>();

    private File selectedStegoFile;
    private File selectedOriginalFile;

    // Row with default values
    private VBox activeRow;

    private ImageView activeOriginalView;
    private ImageView activeStegoView;
    private ImageView activeLsbAttackView;
    private ImageView activeHeatmapView;

    private Label activeOriginalLabel;
    private Label activeStegoLabel;
    private Label activeLsbAttackLabel;
    private Label activeHeatmapLabel;

    public Node getNode() {
        return root;
    }

    public AnalyzingPane() {
        // Buttons for the pane
        Button chooseOriginalButton = new Button("Choose original image");
        chooseOriginalButton.setOnAction(event -> openOriginalChooser());

        Button chooseStegoButton = new Button("Choose stego image");
        chooseStegoButton.setOnAction(event -> openStegoChooser());

        Button analyzeButton = new Button("Analyze");
        analyzeButton.setOnAction(event -> handleAnalyze());

        // Clear button that will reset the pane with the default row only
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(event -> {
            imageArea.getChildren().removeAll(imageRows);
            imageRows.clear();
            selectedStegoFile = null;
            selectedOriginalFile = null;
            addDefaultRow();
        });

        // Row/Column where the buttons/images are positions 
        HBox controlsRow = new HBox(10, chooseOriginalButton, chooseStegoButton, analyzeButton, clearButton);
        controlsRow.setPadding(new Insets(10));
        VBox column = new VBox(10, controlsRow, imageArea);
        column.setPadding(new Insets(10));

        // ScrollPane so the user can go through a list of images
        ScrollPane scrollPane = new ScrollPane(column);
        scrollPane.setFitToWidth(true);
        root = new VBox(scrollPane); //container
        addDefaultRow();
    }

    // Default row with default images
    private void addDefaultRow() {
        Image def = loadDefaultImage();
        activeOriginalLabel  = new Label("Original");
        activeOriginalView   = createImageView(def);
        makeZoomable(activeOriginalView, activeOriginalLabel);
        VBox originalCol     = new VBox(5, activeOriginalLabel,  activeOriginalView);

        activeStegoLabel     = new Label("Stego");
        activeStegoView      = createImageView(def);
        makeZoomable(activeStegoView, activeStegoLabel);
        VBox stegoCol        = new VBox(5, activeStegoLabel,     activeStegoView);

        activeLsbAttackLabel = new Label("LSB Attack");
        activeLsbAttackView  = createImageView(def);
        makeZoomable(activeLsbAttackView, activeLsbAttackLabel);
        VBox lsbCol          = new VBox(5, activeLsbAttackLabel, activeLsbAttackView);

        activeHeatmapLabel   = new Label("Difference heatmap");
        activeHeatmapView    = createImageView(def);
        makeZoomable(activeHeatmapView, activeHeatmapLabel);
        VBox heatmapCol      = new VBox(5, activeHeatmapLabel,   activeHeatmapView);

        activeRow = new VBox(5, new HBox(10, originalCol, stegoCol, lsbCol, heatmapCol));
        activeRow.setPadding(new Insets(10));

        imageRows.add(activeRow);
        imageArea.getChildren().add(activeRow);
    }

    //Choose original image
    private void openOriginalChooser() {
        File file = openPngChooser();
        if (file != null) {
            selectedOriginalFile = file;
            activeOriginalView.setImage(new Image(file.toURI().toString()));
            activeOriginalLabel.setText(file.getName() + " (original)");
        }
    }

    // Choose stego image
    private void openStegoChooser() {
        File file = openPngChooser();
        if (file != null) {
            selectedStegoFile = file;
            activeStegoView.setImage(new Image(file.toURI().toString()));
            activeStegoLabel.setText(file.getName() + " (stego)");
        }
    }

    // Open screen for the user to choose an image
    private File openPngChooser() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png"));
        Window window = root.getScene() != null ? root.getScene().getWindow() : null;
        if (window == null) return null;
        return fc.showOpenDialog(window);
    }

    // Analyze the image
    private void handleAnalyze() {
        // Error handling
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

            BufferedImage lsbSpec = lsbXraySpec(stego);

            // Difference heatmap 
            BufferedImage heatmap = null;
            int modifiedPixels = 0;
            double mse  = Double.NaN;
            double psnr = Double.NaN;

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
                heatmap = differenceHeatmap(original, stego, counts);
                modifiedPixels = counts[0];
                mse  = calculateMse(original, stego);
                psnr = mse == 0 ? Double.POSITIVE_INFINITY : 10.0 * Math.log10(255.0 * 255.0 / mse);
            }

            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            long memoryBytes = (long) stego.getWidth() * stego.getHeight() * 3;

            StringBuilder stats = new StringBuilder(String.format(
                    "Size: %d × %d px  |  Time: %d ms  |  Memory: %s",
                    stego.getWidth(), stego.getHeight(), elapsedMs, formatBytes(memoryBytes)));

            if (heatmap != null) {
                int total = stego.getWidth() * stego.getHeight();
                stats.append(String.format("  |  Modified pixels: %d / %d (%.2f%%)",
                        modifiedPixels, total, 100.0 * modifiedPixels / total));
                stats.append(String.format("  |  MSE: %.4f", mse));
                String psnrStr = Double.isInfinite(psnr) ? "∞" : String.format("%.2f", psnr);
                stats.append(String.format("  |  PSNR: %s dB", psnrStr));
            }

            // Update active row
            activeLsbAttackView.setImage(SwingFXUtils.toFXImage(lsbSpec, null));
            activeLsbAttackLabel.setText("LSB Attack");

            if (heatmap != null) {
                activeHeatmapView.setImage(SwingFXUtils.toFXImage(heatmap, null));
                activeHeatmapLabel.setText("Difference heatmap");
            } else {
                activeHeatmapLabel.setText("Difference heatmap - load an original to enable");
            }
            activeRow.getChildren().add(new Label(stats.toString()));

            // Reset
            selectedStegoFile = null;
            selectedOriginalFile = null;
            addDefaultRow();

            // Error handling
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Analysis Failed",
                    ex.getMessage() != null ? ex.getMessage() : "Unable to analyze the image.");
        }
    }


    private static BufferedImage lsbXraySpec(BufferedImage source) {
        int w = source.getWidth(), h = source.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = source.getRGB(x, y);
                int r = ((rgb >> 16) & 0x01) * 255;
                int g = ((rgb >>  8) & 0x01) * 255;
                int b = (rgb         & 0x01) * 255;
                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    // heatmap
    private static BufferedImage differenceHeatmap(BufferedImage original, BufferedImage stego, int[] counts) {
        int w = stego.getWidth(), h = stego.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB); //create a copy
        int modified = 0; 

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int oRgb = original.getRGB(x, y);
                int sRgb = stego.getRGB(x, y);
                int r = Math.min(Math.abs(((oRgb >> 16) & 0xFF) - ((sRgb >> 16) & 0xFF)) * 50, 255);
                int g = Math.min(Math.abs(((oRgb >>  8) & 0xFF) - ((sRgb >>  8) & 0xFF)) * 50, 255);
                int b = Math.min(Math.abs((oRgb         & 0xFF) - (sRgb         & 0xFF)) * 50, 255);
                if (r != 0 || g != 0 || b != 0) modified++;
                out.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }
        counts[0] = modified;
        return out;
    }

    // Open the images in full-size 
    private static void makeZoomable(ImageView view, Label titleLabel) {
        view.setOnMouseClicked(e -> {
            Image img = view.getImage();
            if (img == null || img.getWidth() <= 1) return;
            ImageView zoomed = new ImageView(img);
            zoomed.setFitWidth(600);
            zoomed.setFitHeight(600);
            zoomed.setPreserveRatio(true);
            Alert dialog = new Alert(Alert.AlertType.NONE);
            dialog.setTitle(titleLabel.getText());
            dialog.getDialogPane().setContent(zoomed);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.show();
        });
    }

    // MSE = average squared difference per channel across all pixels
    private static double calculateMse(BufferedImage original, BufferedImage stego) {
        int w = original.getWidth(), h = original.getHeight();
        long mseSum = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int oRgb = original.getRGB(x, y);
                int sRgb = stego.getRGB(x, y);
                for (int shift : new int[]{16, 8, 0}) {
                    int diff = ((oRgb >> shift) & 0xFF) - ((sRgb >> shift) & 0xFF);
                    mseSum += (long) diff * diff;
                }
            }
        }
        return mseSum / (double)(w * h * 3);
    }

    // Format the bytes 
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }

    private ImageView createImageView(Image image) {
        ImageView view = new ImageView(image);
        view.setFitWidth(IMAGE_SIZE);
        view.setFitHeight(IMAGE_SIZE);
        view.setPreserveRatio(true);
        return view;
    }

    private Image loadDefaultImage() {
        InputStream stream = getClass().getResourceAsStream("/com/example/seniorproject/img.png");
        if (stream != null) return new Image(stream);
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
