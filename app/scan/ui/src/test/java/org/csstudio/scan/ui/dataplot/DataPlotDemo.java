/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.dataplot;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** Demo of DataPlot
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataPlotDemo extends ApplicationWrapper
{
    @Override
    public void start(final Stage stage) throws Exception
    {
        final DataPlot plot = new DataPlot(id -> System.err.println("Selected scan #" + id));
        final Scene scene = new Scene(plot, 800, 600);
        stage.setScene(scene);
        stage.show();

        plot.selectScan(71);
        plot.selectXDevice("xpos");
        plot.selectYDevice(0, "ypos");
        plot.addYDevice("loc:/xx(2)");
    }

    public static void main(final String[] args)
    {
        launch(DataPlotDemo.class, args);
    }
}
