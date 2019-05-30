/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import org.csstudio.display.builder.representation.javafx.widgets.SliderMarkers;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

/** Slider demo
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SliderDemo extends ApplicationWrapper
{
    public static void main(String [] args)
    {
        launch(SliderDemo.class, args);
    }

    @Override
    public void start(final Stage stage)
    {
        final Slider slider = new Slider();
        slider.setOrientation(Orientation.HORIZONTAL);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(20.0);
        slider.setMin(-100.0);
        slider.setMax(100.0);
        slider.setValue(10.0);

        slider.valueProperty().addListener((observable, old, value)->
        {
            System.out.println("Value: " + value);
        });

        final SliderMarkers markers = new SliderMarkers(slider);
        markers.setAlarmMarkers(-100, -10, 70, 90);

        final String font = "-fx-font-size: 30px";
        slider.setStyle(font);
        markers.setStyle(font);

        final GridPane layout = new GridPane();
        layout.add(markers, 0, 0);
        layout.getChildren().add(slider);
        if (slider.getOrientation() == Orientation.VERTICAL)
        {
            GridPane.setConstraints(slider, 1, 0);
            GridPane.setVgrow(slider, Priority.ALWAYS);
        }
        else
        {
            GridPane.setConstraints(slider, 0, 1);
            GridPane.setHgrow(slider, Priority.ALWAYS);
        }
        final Scene scene = new Scene(layout, 800, 700);
        stage.setScene(scene);
        stage.setTitle("Slider Demo");

        stage.show();
        markers.update();
    }
}