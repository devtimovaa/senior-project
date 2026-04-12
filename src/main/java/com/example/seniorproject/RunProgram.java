package com.example.seniorproject;

import com.example.seniorproject.controller.AnalyzingController;
import com.example.seniorproject.controller.EmbeddingController;
import com.example.seniorproject.controller.ExtractingController;
import com.example.seniorproject.model.AnalyzingModel;
import com.example.seniorproject.model.EmbeddingModel;
import com.example.seniorproject.model.ExtractingModel;
import com.example.seniorproject.view.AnalyzingView;
import com.example.seniorproject.view.EmbeddingView;
import com.example.seniorproject.view.ExtractingView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class RunProgram extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        EmbeddingModel embeddingModel = new EmbeddingModel();
        EmbeddingView embeddingView = new EmbeddingView();
        EmbeddingController embeddingController = new EmbeddingController(embeddingModel, embeddingView);

        ExtractingModel extractingModel = new ExtractingModel();
        ExtractingView extractingView = new ExtractingView();
        ExtractingController extractingController = new ExtractingController(extractingModel, extractingView);

        AnalyzingModel analyzingModel = new AnalyzingModel();
        AnalyzingView analyzingView = new AnalyzingView();
        AnalyzingController analyzingController = new AnalyzingController(analyzingModel, analyzingView);

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(new Tab("Embed Message", embeddingController.getNode()));
        tabPane.getTabs().add(new Tab("Extract Message", extractingController.getNode()));
        tabPane.getTabs().add(new Tab("Analyze Image", analyzingController.getNode()));
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);


        primaryStage.setTitle("Steganography Application");
        primaryStage.setWidth(900);
        primaryStage.setHeight(900);
        primaryStage.setScene(new Scene(tabPane));
        primaryStage.show();
    }
}
