/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.dataplot;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of DataPlot
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataPlotDemo extends Application
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final DataPlot plot = new DataPlot();
        final Scene scene = new Scene(plot, 800, 600);
        stage.setScene(scene);
        stage.show();

        plot.selectScan(71);
        plot.selectXDevice("xpos");
        plot.addYDevice("ypos");
    }

    public static void main(final String[] args)
    {
        launch(args);
    }
}
