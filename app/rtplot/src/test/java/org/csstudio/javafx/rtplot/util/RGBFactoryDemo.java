/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.util;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/** Demo of {@link RGBFactory}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RGBFactoryDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage)
    {
        final TilePane layout = new TilePane();
        layout.setPrefColumns(3);
        final RGBFactory colors = new RGBFactory();
        int index = 0;
        for (int row=0; row<20; ++row)
            for (int col=0; col<3; ++col)
            {
                final Color color = colors.next();
                final Label text = new Label("COLOR " + (++index) + ": " + color);
                text.setTextFill(color);
                layout.getChildren().add(text);
            }

        final Scene scene = new Scene(layout);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(final String[] args)
    {
        launch(RGBFactoryDemo.class, args);
    }

}
