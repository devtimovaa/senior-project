package com.example.seniorproject.view;

import java.io.InputStream;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

//Shared UI helpers for all view classes
public abstract class BaseView {

    //Loads the placeholder image from resources
    public Image loadDefaultImage() {
        InputStream stream = getClass().getResourceAsStream("/com/example/seniorproject/img.png");
        if (stream != null) {
            return new Image(stream);
        }
        return new WritableImage(1, 1);
    }

    //Creates a square ImageView with the given size
    protected ImageView createImageView(Image image, int size) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(true);
        return imageView;
    }
}
