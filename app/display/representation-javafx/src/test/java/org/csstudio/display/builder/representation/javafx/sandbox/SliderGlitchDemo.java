/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * Bug demonstration for JFX rendering of slider and rectangles.
 * 
 * A bug occurs when the slider's value is NaN. If one of the rectangles
 * are dragged from the corner to adjust size, with the cursor at the corner when
 * the border crosses the slider, the rectangle disappears. The dashed rectangle
 * freezes the program, necessitating a forced shutdown. The solid one merely
 * reappears once its border has cleared the slider.
 * 
 * @author Amanda Carpenter
 */
public class SliderGlitchDemo extends ApplicationWrapper
{
    public static void main(String [] args)
    {
        launch(SliderGlitchDemo.class, args);
    }

    double startDragX = 0, startDragY = 0;
    double startWidth = 10, startHeight = 10;
    @SuppressWarnings("nls")
    @Override
    public void start(final Stage stage)
    {
        Slider slider = new Slider();
        slider.setOrientation(Orientation.VERTICAL);
        slider.setLayoutX(110);
        slider.setPrefHeight(200);
        slider.setValue(Double.NaN);

        Rectangle rect1 = createRect(10);
        rect1.setStyle("-fx-stroke-width: 1; -fx-stroke-dash-array: 5.0, 5.0; -fx-stroke: blue; -fx-fill: rgb(0, 0, 255, 0.05);");

        Rectangle rect2 = createRect(30);
        rect2.setStyle("-fx-stroke-width: 1; -fx-stroke: blue; -fx-fill: rgb(0, 0, 255, 0.05);");

        final Pane pane = new Pane(slider, rect1, rect2);
        pane.setPadding(new Insets(5));

        final Label label = new Label("Drag the bottom right corner of each rectangle across the slider. When the slider value is NaN,\n"
                + "the dashed rectangle freezes the program; the solid-bordered one disappears and reappears.\n"
                + "When it is finite, the rectangles behave as expected.");
        
        Button button = new Button("Toggle NaN/finite value.");
        button.setOnAction(e->
        {
            slider.setValue(Double.isFinite(slider.getValue()) ? Double.NaN : 50);
        });

        final VBox root = new VBox(pane, label, button);
        final Scene scene = new Scene(root, 800, 700);

        stage.setScene(scene);
        stage.setTitle("Slider Glitch Demo");

        stage.show();
    }

    private Rectangle createRect(double y)
    {
        Rectangle rect = new Rectangle(10, y, startWidth, startHeight);
        rect.setOnMousePressed((event)->
        {
            startDragX = event.getX();
            startDragY = event.getY();
            startWidth = rect.getWidth();
            startHeight = rect.getHeight();
        });
        rect.setOnMouseDragged((event)->
        {
            double deltaX = event.getX() - startDragX;
            double deltaY = event.getY() - startDragY;
            rect.setWidth(Math.max(startWidth+deltaX, 1));
            rect.setHeight(Math.max(startHeight+deltaY, 1));
        });
        return rect;
    }
}