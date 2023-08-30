/*******************************************************************************
 * Copyright (c) 2015-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.AxisWidgetProperty;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/** JUnit test of XYPlot widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XYPlotWidgetTest
{
    /** Check for the one default 'Y' axis */
    private void assertYAxis(final XYPlotWidget plot)
    {
        assertThat(plot.propYAxes().size(), equalTo(1));
        final AxisWidgetProperty axis = plot.propYAxes().getElement(0);
        assertThat(axis.title().getValue(), equalTo("Y"));
    }

    /** Check if XML out/in preserves the Y axis
     *  @throws Exception on error
     */
    @Test
    public void testXML() throws Exception
    {
        XYPlotWidget plot = new XYPlotWidget();
        assertYAxis(plot);

        final String xml = ModelWriter.getXML(List.of(plot));
        System.out.println(xml);

        final DisplayModel model = ModelReader.parseXML(xml);
        final List<Widget> widgets = model.getChildren();
        assertThat(widgets.size(), equalTo(1));
        assertThat(widgets.get(0), instanceOf(XYPlotWidget.class));
        plot = (XYPlotWidget) widgets.get(0);
        assertYAxis(plot);
   }
}