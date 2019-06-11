/*******************************************************************************
 * Copyright (c) 2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Ellipse;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

//incorporate changing values, somehow
@SuppressWarnings("nls")
public class PaneEllipsesDemo extends ApplicationWrapper
{
    public static void main(final String[] args)
    {
        launch(PaneEllipsesDemo.class, args);
    }

	final int number = 8;
	final int size = 50;
    @Override
    public void start(final Stage stage)
    {
        Color [] value_colors = getColors(170); //10101010
        String[] labels = {"1","2","4","8","16","32","64","128"};

        //===widget-relevant code===//
        Pane pane = new Pane();
        Ellipse ellipses [] = new Ellipse [number];
        Text textLabels [] = new Text [number];
        for (int i = 0; i < number; i++) {
            ellipses[i] = new Ellipse();
            textLabels[i] = createText(labels[i]);
            pane.getChildren().addAll(ellipses[i], textLabels[i]);
        }

        pane.setPrefSize(size*number, size);
        for (int i = 0; i < number; i++) {
            ellipses[i].setCenterX(size/2 + i*size);
            ellipses[i].setCenterY(size/2);
            ellipses[i].setRadiusX(size/2);
            ellipses[i].setRadiusY(size/2);
            textLabels[i].setX(i*size);
            textLabels[i].setY(size/2);
            textLabels[i].setWrappingWidth(size);
        }

        for (int i = 0; i < number; i++)
            ellipses[i].setFill(
                new LinearGradient(0, 0, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                                   new Stop(0, value_colors[i].interpolate(Color.WHITESMOKE, 0.8)),
                                   new Stop(1, value_colors[i])));

        //=end widget-relevant code=//

        //VBox.setVgrow(pane, Priority.NEVER);
        VBox vbox = new VBox(pane);

        final Scene scene = new Scene(vbox, 800, 700);
        stage.setScene(scene);
        stage.setTitle("Pane with Ellipses");

        stage.show();
    }

    private final Text createText(String text) {
        final Text newText = new Text(text);
        newText.setFont(new Font(20));
        newText.setTextOrigin(VPos.CENTER);
        newText.setTextAlignment(TextAlignment.CENTER);
        return newText;
    }

    private Color[] getColors(int bits) {
        Color [] result = new Color [size];
        Color bright = Color.web("0x00FF00",1.0);
        Color dark = Color.web("0x007700",1.0);
        for (int i = 0; i < size; i++) {
            result[i] = (bits & 1) == 1 ? bright : dark;
            bits = bits >> 1;
        }
        return result;
    }

}
