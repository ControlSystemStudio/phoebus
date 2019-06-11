/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui;

import org.csstudio.trends.databrowser3.ui.plot.ModelBasedPlot;
import org.csstudio.trends.databrowser3.ui.properties.FontButton;
import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/** Demo of the {@link ModelBasedPlot}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FontButtonDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final Font initial_font = Font.font("Liberation Sans", FontWeight.BOLD, FontPosture.ITALIC, 12.0);
        final FontButton button = new FontButton(initial_font, font ->
        {
            System.out.println("Selected " + font);
        });
        final BorderPane layout = new BorderPane(button);
        final Scene scene = new Scene(layout, 300, 300);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(FontButtonDemo.class, args);
    }
}
