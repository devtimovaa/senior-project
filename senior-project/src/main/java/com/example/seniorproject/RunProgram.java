package com.example.seniorproject;

import com.example.seniorproject.controllers.EmbeddingPane;
import com.example.seniorproject.controllers.ExtractingPane;

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
        EmbeddingPane embeddingPane = new EmbeddingPane();
        ExtractingPane extractingPane = new ExtractingPane();

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(new Tab("Embed Message", embeddingPane.getNode()));
        tabPane.getTabs().add(new Tab("Extract Message", extractingPane.getNode()));
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);


        primaryStage.setTitle("Hello!");
        primaryStage.setWidth(900);
        primaryStage.setHeight(900);
        primaryStage.setScene(new Scene(tabPane));
        primaryStage.show();
    }
}
