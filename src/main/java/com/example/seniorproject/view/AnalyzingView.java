package com.example.seniorproject.view;

import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

//Builds the UI layout for the Analyze tab
public class AnalyzingView extends BaseView {
    private static final int IMAGE_SIZE = 200;

    private final VBox root;
    private final VBox imageArea = new VBox(10);
    private final List<Node> imageRows = new ArrayList<>();

    private final Button chooseOriginalButton;
    private final Button chooseStegoButton;
    private final Button analyzeButton;
    private final Button clearButton;

    private VBox activeRow;
    private ImageView activeOriginalView;
    private ImageView activeStegoView;
    private ImageView activeLsbXrayView;
    private ImageView activeHeatmapView;
    private Label activeOriginalLabel;
    private Label activeStegoLabel;
    private Label activeLsbXrayLabel;
    private Label activeHeatmapLabel;

    public Node getNode() {
        return root;
    }

    public AnalyzingView() {
        chooseOriginalButton = new Button("Choose original image");
        chooseStegoButton = new Button("Choose stego image");
        analyzeButton = new Button("Analyze");
        clearButton = new Button("Clear");

        HBox controlsRow = new HBox(10, chooseOriginalButton, chooseStegoButton, analyzeButton, clearButton);
        controlsRow.setPadding(new Insets(10));
        VBox column = new VBox(10, controlsRow, imageArea);
        column.setPadding(new Insets(10));

        //ScrollPane lets the user scroll through multiple analysis rows
        ScrollPane scrollPane = new ScrollPane(column);
        scrollPane.setFitToWidth(true);
        root = new VBox(scrollPane);
        addDefaultRow();
    }

    //Creates a fresh row with placeholder images for the next analysis
    public void addDefaultRow() {
        Image def = loadDefaultImage();
        activeOriginalLabel  = new Label("Original");
        activeOriginalView   = createImageView(def, IMAGE_SIZE);
        makeZoomable(activeOriginalView, activeOriginalLabel);
        VBox originalCol     = new VBox(5, activeOriginalLabel,  activeOriginalView);

        activeStegoLabel     = new Label("Stego");
        activeStegoView      = createImageView(def, IMAGE_SIZE);
        makeZoomable(activeStegoView, activeStegoLabel);
        VBox stegoCol        = new VBox(5, activeStegoLabel,     activeStegoView);

        activeLsbXrayLabel   = new Label("LSB X-ray");
        activeLsbXrayView    = createImageView(def, IMAGE_SIZE);
        makeZoomable(activeLsbXrayView, activeLsbXrayLabel);
        VBox lsbCol          = new VBox(5, activeLsbXrayLabel,   activeLsbXrayView);

        activeHeatmapLabel   = new Label("Difference heatmap");
        activeHeatmapView    = createImageView(def, IMAGE_SIZE);
        makeZoomable(activeHeatmapView, activeHeatmapLabel);
        VBox heatmapCol      = new VBox(5, activeHeatmapLabel,   activeHeatmapView);

        activeRow = new VBox(5, new HBox(10, originalCol, stegoCol, lsbCol, heatmapCol));
        activeRow.setPadding(new Insets(10));

        imageRows.add(activeRow);
        imageArea.getChildren().add(activeRow);
    }

    //Opens a full-size preview of the image in a dialog
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

    public VBox getRoot() { return root; }
    public VBox getImageArea() { return imageArea; }
    public List<Node> getImageRows() { return imageRows; }
    public Button getChooseOriginalButton() { return chooseOriginalButton; }
    public Button getChooseStegoButton() { return chooseStegoButton; }
    public Button getAnalyzeButton() { return analyzeButton; }
    public Button getClearButton() { return clearButton; }
    public VBox getActiveRow() { return activeRow; }
    public ImageView getActiveOriginalView() { return activeOriginalView; }
    public ImageView getActiveStegoView() { return activeStegoView; }
    public ImageView getActiveLsbXrayView() { return activeLsbXrayView; }
    public ImageView getActiveHeatmapView() { return activeHeatmapView; }
    public Label getActiveOriginalLabel() { return activeOriginalLabel; }
    public Label getActiveStegoLabel() { return activeStegoLabel; }
    public Label getActiveLsbXrayLabel() { return activeLsbXrayLabel; }
    public Label getActiveHeatmapLabel() { return activeHeatmapLabel; }
}
