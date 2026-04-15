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

//Handles user interaction for the Analyze tab
public class AnalyzingController extends BaseController {
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
        view.getClearButton().setOnAction(event -> handleClear());
    }

    private void openOriginalChooser() {
        File file = openPngChooser();
        if (file != null) {
            selectedOriginalFile = file;
            view.getActiveOriginalView().setImage(new Image(file.toURI().toString()));
            view.getActiveOriginalLabel().setText(file.getName() + " (original)");
        }
    }

    private void openStegoChooser() {
        File file = openPngChooser();
        if (file != null) {
            selectedStegoFile = file;
            view.getActiveStegoView().setImage(new Image(file.toURI().toString()));
            view.getActiveStegoLabel().setText(file.getName() + " (stego)");
        }
    }

    //Opens a file chooser filtered to PNG images
    private File openPngChooser() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png"));
        Window window = view.getRoot().getScene() != null ? view.getRoot().getScene().getWindow() : null;
        if (window == null) return null;
        return fc.showOpenDialog(window);
    }

    //Runs the analysis
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

            BufferedImage lsbXray = model.lsbXray(stego);

            BufferedImage heatmap = null;
            int modifiedPixels = 0;
            double mse = Double.NaN;
            double psnr = Double.NaN;

            //Comparison metrics are only available when an original image is loaded
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
                AnalyzingModel.HeatmapResult result = model.differenceHeatmap(original, stego);
                heatmap = result.image();
                modifiedPixels = result.modifiedPixels();
                mse = model.calculateMse(original, stego);
                psnr = model.calculatePsnr(mse);
            }

            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            String stats = buildStatsText(stego, elapsedMs, heatmap != null, modifiedPixels, mse, psnr);

            updateView(lsbXray, heatmap, stats);

            selectedStegoFile = null;
            selectedOriginalFile = null;
            view.addDefaultRow();

        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Analysis Failed",
                    ex.getMessage() != null ? ex.getMessage() : "Unable to analyze the image.");
        }
    }

    //Formats all analysis metrics into a single display string
    private String buildStatsText(BufferedImage stego, long elapsedMs,
                                  boolean hasComparison, int modifiedPixels,
                                  double mse, double psnr) {
        long imageDataBytes = (long) stego.getWidth() * stego.getHeight() * 3;
        StringBuilder sb = new StringBuilder(String.format(
                "Size: %d × %d px  |  Time: %d ms  |  Image data: %s",
                stego.getWidth(), stego.getHeight(), elapsedMs, model.formatBytes(imageDataBytes)));

        if (hasComparison) {
            int total = stego.getWidth() * stego.getHeight();
            sb.append(String.format("  |  Modified pixels: %d / %d (%.2f%%)",
                    modifiedPixels, total, 100.0 * modifiedPixels / total));
            sb.append(String.format("  |  MSE: %.4f", mse));
            String psnrStr = Double.isInfinite(psnr) ? "∞" : String.format("%.2f", psnr);
            sb.append(String.format("  |  PSNR: %s dB", psnrStr));
        }
        return sb.toString();
    }

    //Pushes analysis results into the active row
    private void updateView(BufferedImage lsbXray, BufferedImage heatmap, String stats) {
        view.getActiveLsbXrayView().setImage(SwingFXUtils.toFXImage(lsbXray, null));
        view.getActiveLsbXrayLabel().setText("LSB X-ray");

        if (heatmap != null) {
            view.getActiveHeatmapView().setImage(SwingFXUtils.toFXImage(heatmap, null));
            view.getActiveHeatmapLabel().setText("Difference heatmap");
        } else {
            view.getActiveHeatmapLabel().setText("Difference heatmap - load an original to enable");
        }
        view.getActiveRow().getChildren().add(new Label(stats));
    }

    private void handleClear() {
        view.getImageArea().getChildren().removeAll(view.getImageRows());
        view.getImageRows().clear();
        selectedStegoFile = null;
        selectedOriginalFile = null;
        view.addDefaultRow();
    }
}
