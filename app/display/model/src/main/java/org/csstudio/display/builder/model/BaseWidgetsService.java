/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.util.Collection;
import java.util.List;

import org.csstudio.display.builder.model.spi.WidgetsService;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.builder.model.widgets.ArcWidget;
import org.csstudio.display.builder.model.widgets.ArrayWidget;
import org.csstudio.display.builder.model.widgets.BoolButtonWidget;
import org.csstudio.display.builder.model.widgets.ByteMonitorWidget;
import org.csstudio.display.builder.model.widgets.CheckBoxWidget;
import org.csstudio.display.builder.model.widgets.ClockWidget;
import org.csstudio.display.builder.model.widgets.ComboWidget;
import org.csstudio.display.builder.model.widgets.DigitalClockWidget;
import org.csstudio.display.builder.model.widgets.EllipseWidget;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.model.widgets.FileSelectorWidget;
import org.csstudio.display.builder.model.widgets.GaugeWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.KnobWidget;
import org.csstudio.display.builder.model.widgets.LEDWidget;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.csstudio.display.builder.model.widgets.LinearMeterWidget;
import org.csstudio.display.builder.model.widgets.MeterWidget;
import org.csstudio.display.builder.model.widgets.MultiStateLEDWidget;
import org.csstudio.display.builder.model.widgets.NavigationTabsWidget;
import org.csstudio.display.builder.model.widgets.PictureWidget;
import org.csstudio.display.builder.model.widgets.PolygonWidget;
import org.csstudio.display.builder.model.widgets.PolylineWidget;
import org.csstudio.display.builder.model.widgets.ProgressBarWidget;
import org.csstudio.display.builder.model.widgets.RadioWidget;
import org.csstudio.display.builder.model.widgets.RectangleWidget;
import org.csstudio.display.builder.model.widgets.ScaledSliderWidget;
import org.csstudio.display.builder.model.widgets.ScrollBarWidget;
import org.csstudio.display.builder.model.widgets.SpinnerWidget;
import org.csstudio.display.builder.model.widgets.SymbolWidget;
import org.csstudio.display.builder.model.widgets.TableWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.TankWidget;
import org.csstudio.display.builder.model.widgets.TextEntryWidget;
import org.csstudio.display.builder.model.widgets.TextSymbolWidget;
import org.csstudio.display.builder.model.widgets.TextUpdateWidget;
import org.csstudio.display.builder.model.widgets.ThermometerWidget;
import org.csstudio.display.builder.model.widgets.WebBrowserWidget;
import org.csstudio.display.builder.model.widgets.plots.DataBrowserWidget;
import org.csstudio.display.builder.model.widgets.plots.ImageWidget;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget;

/** SPI for the base widgets
 *  @author Kay Kasemir
 */
public class BaseWidgetsService implements WidgetsService
{
    @Override
    public Collection<WidgetDescriptor> getWidgetDescriptors()
    {
        return List.of(
            ActionButtonWidget.WIDGET_DESCRIPTOR,
            ArcWidget.WIDGET_DESCRIPTOR,
            ArrayWidget.WIDGET_DESCRIPTOR,
            BoolButtonWidget.WIDGET_DESCRIPTOR,
            ByteMonitorWidget.WIDGET_DESCRIPTOR,
            CheckBoxWidget.WIDGET_DESCRIPTOR,
            ClockWidget.WIDGET_DESCRIPTOR,
            ComboWidget.WIDGET_DESCRIPTOR,
            DigitalClockWidget.WIDGET_DESCRIPTOR,
            EllipseWidget.WIDGET_DESCRIPTOR,
            EmbeddedDisplayWidget.WIDGET_DESCRIPTOR,
            FileSelectorWidget.WIDGET_DESCRIPTOR,
            GaugeWidget.WIDGET_DESCRIPTOR,
            GroupWidget.WIDGET_DESCRIPTOR,
            ImageWidget.WIDGET_DESCRIPTOR,
            KnobWidget.WIDGET_DESCRIPTOR,
            LabelWidget.WIDGET_DESCRIPTOR,
            LEDWidget.WIDGET_DESCRIPTOR,
            LinearMeterWidget.WIDGET_DESCRIPTOR,
            MeterWidget.WIDGET_DESCRIPTOR,
            MultiStateLEDWidget.WIDGET_DESCRIPTOR,
            NavigationTabsWidget.WIDGET_DESCRIPTOR,
            PictureWidget.WIDGET_DESCRIPTOR,
            PolygonWidget.WIDGET_DESCRIPTOR,
            PolylineWidget.WIDGET_DESCRIPTOR,
            ProgressBarWidget.WIDGET_DESCRIPTOR,
            RadioWidget.WIDGET_DESCRIPTOR,
            RectangleWidget.WIDGET_DESCRIPTOR,
            ScaledSliderWidget.WIDGET_DESCRIPTOR,
            ScrollBarWidget.WIDGET_DESCRIPTOR,
            SpinnerWidget.WIDGET_DESCRIPTOR,
            SymbolWidget.WIDGET_DESCRIPTOR,
            TableWidget.WIDGET_DESCRIPTOR,
            TabsWidget.WIDGET_DESCRIPTOR,
            TankWidget.WIDGET_DESCRIPTOR,
            TextEntryWidget.WIDGET_DESCRIPTOR,
            TextSymbolWidget.WIDGET_DESCRIPTOR,
            TextUpdateWidget.WIDGET_DESCRIPTOR,
            ThermometerWidget.WIDGET_DESCRIPTOR,
            WebBrowserWidget.WIDGET_DESCRIPTOR,
            DataBrowserWidget.WIDGET_DESCRIPTOR,
            XYPlotWidget.WIDGET_DESCRIPTOR);
    }
}
