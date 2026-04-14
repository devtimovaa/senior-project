package com.example.seniorproject.controller;

import com.example.seniorproject.model.AnalyzingModel;
import com.example.seniorproject.view.AnalyzingView;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class AnalyzingController {
    private final AnalyzingModel model;
    private final AnalyzingView view;
    private File selectedStegoFile;
    private File selectedOriginalFile;

    public AnalyzingController(AnalyzingModel model, AnalyzingView view) {
        this.model = model;
        this.view = view;
        initEventHandlers();
    }

    public Node getNode() {
        return view.getNode();
    }

    private void initEventHandlers() {
        view.getChooseOriginalButton().setOnAction(event -> openOriginalChooser());
        view.getChooseStegoButton().setOnAction(event -> openStegoChooser());
        view.getAnalyzeButton().setOnAction(event -> handleAnalyze());
        view.getClearButton().setOnAction(event -> {
            view.getImageArea().getChildren().removeAll(view.getImageRows());
            view.getImageRows().clear();
            selectedStegoFile = null;
            selectedOriginalFile = null;
            view.addDefaultRow();
        });
    }

    //Choose original image
    private void openOriginalChooser() {
        File file = openPngChooser();
        if (file != null) {
            selectedOriginalFile = file;
            view.getActiveOriginalView().setImage(new Image(file.toURI().toString()));
            view.getActiveOriginalLabel().setText(file.getName() + " (original)");
        }
    }

    // Choose stego image
    private void openStegoChooser() {
        File file = openPngChooser();
        if (file != null) {
            selectedStegoFile = file;
            view.getActiveStegoView().setImage(new Image(file.toURI().toString()));
            view.getActiveStegoLabel().setText(file.getName() + " (stego)");
        }
    }

    // Open screen for the user to choose an image
    private File openPngChooser() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png"));
        Window window = view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null;
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

            BufferedImage lsbSpec = model.lsbXraySpec(stego);

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
                heatmap = model.differenceHeatmap(original, stego, counts);
                modifiedPixels = counts[0];
                mse  = model.calculateMse(original, stego);
                psnr = mse == 0 ? Double.POSITIVE_INFINITY : 10.0 * Math.log10(255.0 * 255.0 / mse);
            }

            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            long memoryBytes = (long) stego.getWidth() * stego.getHeight() * 3;

            StringBuilder stats = new StringBuilder(String.format(
                    "Size: %d × %d px  |  Time: %d ms  |  Memory: %s",
                    stego.getWidth(), stego.getHeight(), elapsedMs, model.formatBytes(memoryBytes)));

            if (heatmap != null) {
                int total = stego.getWidth() * stego.getHeight();
                stats.append(String.format("  |  Modified pixels: %d / %d (%.2f%%)",
                        modifiedPixels, total, 100.0 * modifiedPixels / total));
                stats.append(String.format("  |  MSE: %.4f", mse));
                String psnrStr = Double.isInfinite(psnr) ? "∞" : String.format("%.2f", psnr);
                stats.append(String.format("  |  PSNR: %s dB", psnrStr));
            }

            // Update active row
            view.getActiveLsbAttackView().setImage(SwingFXUtils.toFXImage(lsbSpec, null));
            view.getActiveLsbAttackLabel().setText("LSB Attack");

            if (heatmap != null) {
                view.getActiveHeatmapView().setImage(SwingFXUtils.toFXImage(heatmap, null));
                view.getActiveHeatmapLabel().setText("Difference heatmap");
            } else {
                view.getActiveHeatmapLabel().setText("Difference heatmap - load an original to enable");
            }
            view.getActiveRow().getChildren().add(new Label(stats.toString()));

            // Reset
            selectedStegoFile = null;
            selectedOriginalFile = null;
            view.addDefaultRow();

            // Error handling
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Analysis Failed",
                    ex.getMessage() != null ? ex.getMessage() : "Unable to analyze the image.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
