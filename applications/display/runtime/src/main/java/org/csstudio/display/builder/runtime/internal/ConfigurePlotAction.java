/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget;
import org.csstudio.display.builder.runtime.RuntimeAction;

/** RuntimeAction to trigger the configuration dialog of the XYPlot
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ConfigurePlotAction extends RuntimeAction
{
    private final XYPlotWidget plot;

    public ConfigurePlotAction(final XYPlotWidget plot)
    {
        super("Configure Plot",
              "platform:/plugin/org.csstudio.javafx.rtplot/icons/configure.png");
        this.plot = plot;
    }

    @Override
    public void run()
    {
        plot.runtimePropConfigure().trigger();
    }
}