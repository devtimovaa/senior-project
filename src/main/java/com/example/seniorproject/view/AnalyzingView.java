package com.example.seniorproject.view;

import java.io.InputStream;
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
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

// Pane for analyzing which pixels were modified
public class AnalyzingView {
    private static final int IMAGE_SIZE = 200;

    private final VBox root;
    private final VBox imageArea = new VBox(10);
    private final List<Node> imageRows = new ArrayList<>();

    private final Button chooseOriginalButton;
    private final Button chooseStegoButton;
    private final Button analyzeButton;
    private final Button clearButton;

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

    public AnalyzingView() {
        // Buttons for the pane
        chooseOriginalButton = new Button("Choose original image");

        chooseStegoButton = new Button("Choose stego image");

        analyzeButton = new Button("Analyze");

        // Clear button that will reset the pane with the default row only
        clearButton = new Button("Clear");

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
    public void addDefaultRow() {
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

    private ImageView createImageView(Image image) {
        ImageView view = new ImageView(image);
        view.setFitWidth(IMAGE_SIZE);
        view.setFitHeight(IMAGE_SIZE);
        view.setPreserveRatio(true);
        return view;
    }

    public Image loadDefaultImage() {
        InputStream stream = getClass().getResourceAsStream("/com/example/seniorproject/img.png");
        if (stream != null) return new Image(stream);
        return new WritableImage(1, 1);
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
    public ImageView getActiveLsbAttackView() { return activeLsbAttackView; }
    public ImageView getActiveHeatmapView() { return activeHeatmapView; }
    public Label getActiveOriginalLabel() { return activeOriginalLabel; }
    public Label getActiveStegoLabel() { return activeStegoLabel; }
    public Label getActiveLsbAttackLabel() { return activeLsbAttackLabel; }
    public Label getActiveHeatmapLabel() { return activeHeatmapLabel; }
}
