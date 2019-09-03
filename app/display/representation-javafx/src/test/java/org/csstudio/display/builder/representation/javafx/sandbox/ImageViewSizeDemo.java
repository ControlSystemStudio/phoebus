/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import java.io.FileInputStream;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/** ImageView size demo
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageViewSizeDemo extends ApplicationWrapper
{
    public static void main(final String[] args)
    {
        launch(ImageViewSizeDemo.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        final Image image;
        try
        {
            image = new Image(new FileInputStream("src/main/resources/icons/embedded_script.png"));
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }

        // ImageView will by default use the size of its image.
        // ImageView is not resized.
        final ImageView imageview = new ImageView(image);

        // Wrapping it in a pane allows listening to the pane's size
        final Pane pane = new Pane(imageview);
        pane.setStyle("-fx-background-color: red");
        final ChangeListener<? super Number> resize_listener = (p, o, n) ->
        {
            System.out.println("Size: " + pane.getWidth() + " x " + pane.getHeight());
            // In this example, image is centered.
            // Could also imageview.setFitWidth(..), ..Height(..)
            // to ask image view to stretch the image,
            // or create a new image with size that matches the pane.
            imageview.setX((pane.getWidth() + image.getWidth()) / 2);
            imageview.setY((pane.getHeight() + image.getHeight()) / 2);
        };
        pane.widthProperty().addListener(resize_listener);
        pane.heightProperty().addListener(resize_listener);

        final BorderPane layout = new BorderPane(pane);
        final Scene scene = new Scene(layout, 800, 600);
        stage.setScene(scene);
        stage.show();
    }
}
