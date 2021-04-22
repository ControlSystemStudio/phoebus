/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 *
 */
public class ViewableImageListDemo extends ApplicationWrapper
{
    public static void main(String[] args)
    {
        launch(ViewableImageListDemo.class, args);
    }

    @Override
    public void start(final Stage stage) throws URISyntaxException, MalformedURLException {
        final ImageList imageList = new ImageList(false);
        imageList.setImages(
                Arrays.asList(
                        new Image(String.valueOf(this.getClass().getClassLoader().getResource("image_1.png").toURI().toURL()))));
        final BorderPane layout = new BorderPane(imageList);
        stage.setScene(new Scene(layout));
        stage.show();
    }
}
