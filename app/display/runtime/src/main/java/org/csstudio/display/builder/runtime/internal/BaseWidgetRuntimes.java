/*******************************************************************************
 * Copyright (c) 2018-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;
import static java.util.Map.entry;

import java.util.Map;
import java.util.function.Supplier;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.ArrayWidget;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.KnobWidget;
import org.csstudio.display.builder.model.widgets.NavigationTabsWidget;
import org.csstudio.display.builder.model.widgets.ScaledSliderWidget;
import org.csstudio.display.builder.model.widgets.ScrollBarWidget;
import org.csstudio.display.builder.model.widgets.TableWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.plots.DataBrowserWidget;
import org.csstudio.display.builder.model.widgets.plots.ImageWidget;
import org.csstudio.display.builder.model.widgets.plots.StripchartWidget;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.spi.WidgetRuntimesService;

/** SPI for runtimes of base widgets
 *  @author Kay Kasemir
 */
public class BaseWidgetRuntimes implements WidgetRuntimesService
{
    @Override
    public Map<String, Supplier<WidgetRuntime<? extends Widget>>> getWidgetRuntimeFactories()
    {
        return Map.ofEntries(
            entry(DisplayModel.WIDGET_TYPE,                          () -> new DisplayRuntime()),
            entry(ArrayWidget.WIDGET_DESCRIPTOR.getType(),           () -> new ArrayWidgetRuntime()),
            entry(EmbeddedDisplayWidget.WIDGET_DESCRIPTOR.getType(), () -> new EmbeddedDisplayRuntime()),
            entry(GroupWidget.WIDGET_DESCRIPTOR.getType(),           () -> new GroupWidgetRuntime()),
            entry(KnobWidget.WIDGET_DESCRIPTOR.getType(),            () -> new KnobWidgetRuntime()),
            entry(ImageWidget.WIDGET_DESCRIPTOR.getType(),           () -> new ImageWidgetRuntime()),
            entry(NavigationTabsWidget.WIDGET_DESCRIPTOR.getType(),  () -> new NavigationTabsRuntime()),
            entry(ScaledSliderWidget.WIDGET_DESCRIPTOR.getType(),    () -> new SliderWidgetRuntime()),
            entry(ScrollBarWidget.WIDGET_DESCRIPTOR.getType(),       () -> new SliderWidgetRuntime()),
            entry(StripchartWidget.WIDGET_DESCRIPTOR.getType(),      () -> new StripchartWidgetRuntime()),
            entry(TableWidget.WIDGET_DESCRIPTOR.getType(),           () -> new TableWidgetRuntime()),
            entry(TabsWidget.WIDGET_DESCRIPTOR.getType(),            () -> new TabsWidgetRuntime()),
            entry(DataBrowserWidget.WIDGET_DESCRIPTOR.getType(),     () -> new DataBrowserWidgetRuntime()),
            entry(XYPlotWidget.WIDGET_DESCRIPTOR.getType(),          () -> new XYPlotWidgetRuntime())
        );
    }
}
