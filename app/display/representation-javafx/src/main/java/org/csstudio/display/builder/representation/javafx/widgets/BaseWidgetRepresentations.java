/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static java.util.Map.entry;

import java.util.Map;

import org.csstudio.display.builder.model.WidgetDescriptor;
import org.csstudio.display.builder.model.widgets.ActionButtonWidget;
import org.csstudio.display.builder.model.widgets.ArcWidget;
import org.csstudio.display.builder.model.widgets.ArrayWidget;
import org.csstudio.display.builder.model.widgets.BoolButtonWidget;
import org.csstudio.display.builder.model.widgets.ByteMonitorWidget;
import org.csstudio.display.builder.model.widgets.CheckBoxWidget;
import org.csstudio.display.builder.model.widgets.ComboWidget;
import org.csstudio.display.builder.model.widgets.EllipseWidget;
import org.csstudio.display.builder.model.widgets.EmbeddedDisplayWidget;
import org.csstudio.display.builder.model.widgets.FileSelectorWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.LEDWidget;
import org.csstudio.display.builder.model.widgets.LabelWidget;
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
import org.csstudio.display.builder.model.widgets.SlideButtonWidget;
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
import org.csstudio.display.builder.representation.WidgetRepresentation;
import org.csstudio.display.builder.representation.WidgetRepresentationFactory;
import org.csstudio.display.builder.representation.javafx.widgets.plots.DataBrowserRepresentation;
import org.csstudio.display.builder.representation.javafx.widgets.plots.ImageRepresentation;
import org.csstudio.display.builder.representation.javafx.widgets.plots.XYPlotRepresentation;
import org.csstudio.display.builder.representation.spi.WidgetRepresentationsService;

/** SPI for representations of base widgets
 *  @author Kay Kasemir
 */
public class BaseWidgetRepresentations implements WidgetRepresentationsService
{
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <TWP, TW> Map<WidgetDescriptor, WidgetRepresentationFactory<TWP, TW>> getWidgetRepresentationFactories()
    {
        return Map.ofEntries(
            entry(ActionButtonWidget.WIDGET_DESCRIPTOR,    () -> (WidgetRepresentation) new ActionButtonRepresentation()),
            entry(ArcWidget.WIDGET_DESCRIPTOR,             () -> (WidgetRepresentation) new ArcRepresentation()),
            entry(ArrayWidget.WIDGET_DESCRIPTOR,           () -> (WidgetRepresentation) new ArrayRepresentation()),
            entry(BoolButtonWidget.WIDGET_DESCRIPTOR,      () -> (WidgetRepresentation) new BoolButtonRepresentation()),
            entry(ByteMonitorWidget.WIDGET_DESCRIPTOR,     () -> (WidgetRepresentation) new ByteMonitorRepresentation()),
            entry(CheckBoxWidget.WIDGET_DESCRIPTOR,        () -> (WidgetRepresentation) new CheckBoxRepresentation()),
            entry(ComboWidget.WIDGET_DESCRIPTOR,           () -> (WidgetRepresentation) new ComboRepresentation()),
            entry(EmbeddedDisplayWidget.WIDGET_DESCRIPTOR, () -> (WidgetRepresentation) new EmbeddedDisplayRepresentation()),
            entry(EllipseWidget.WIDGET_DESCRIPTOR,         () -> (WidgetRepresentation) new EllipseRepresentation()),
            entry(FileSelectorWidget.WIDGET_DESCRIPTOR,    () -> (WidgetRepresentation) new FileSelectorRepresentation()),
            entry(GroupWidget.WIDGET_DESCRIPTOR,           () -> (WidgetRepresentation) new GroupRepresentation()),
            entry(ImageWidget.WIDGET_DESCRIPTOR,           () -> (WidgetRepresentation) new ImageRepresentation()),
            entry(LabelWidget.WIDGET_DESCRIPTOR,           () -> (WidgetRepresentation) new LabelRepresentation()),
            entry(LEDWidget.WIDGET_DESCRIPTOR,             () -> (WidgetRepresentation) new LEDRepresentation()),
            entry(MultiStateLEDWidget.WIDGET_DESCRIPTOR,   () -> (WidgetRepresentation) new MultiStateLEDRepresentation()),
            entry(NavigationTabsWidget.WIDGET_DESCRIPTOR,  () -> (WidgetRepresentation) new NavigationTabsRepresentation()),
            entry(PictureWidget.WIDGET_DESCRIPTOR,         () -> (WidgetRepresentation) new PictureRepresentation()),
            entry(PolygonWidget.WIDGET_DESCRIPTOR,         () -> (WidgetRepresentation) new PolygonRepresentation()),
            entry(PolylineWidget.WIDGET_DESCRIPTOR,        () -> (WidgetRepresentation) new PolylineRepresentation()),
            entry(ProgressBarWidget.WIDGET_DESCRIPTOR,     () -> (WidgetRepresentation) new ProgressBarRepresentation()),
            entry(RadioWidget.WIDGET_DESCRIPTOR,           () -> (WidgetRepresentation) new RadioRepresentation()),
            entry(RectangleWidget.WIDGET_DESCRIPTOR,       () -> (WidgetRepresentation) new RectangleRepresentation()),
            entry(ScaledSliderWidget.WIDGET_DESCRIPTOR,    () -> (WidgetRepresentation) new ScaledSliderRepresentation()),
            entry(ScrollBarWidget.WIDGET_DESCRIPTOR,       () -> (WidgetRepresentation) new ScrollBarRepresentation()),
            entry(SlideButtonWidget.WIDGET_DESCRIPTOR,     () -> (WidgetRepresentation) new SlideButtonRepresentation()),
            entry(SpinnerWidget.WIDGET_DESCRIPTOR,         () -> (WidgetRepresentation) new SpinnerRepresentation()),
            entry(SymbolWidget.WIDGET_DESCRIPTOR,          () -> (WidgetRepresentation) new SymbolRepresentation()),
            entry(TableWidget.WIDGET_DESCRIPTOR,           () -> (WidgetRepresentation) new TableRepresentation()),
            entry(TabsWidget.WIDGET_DESCRIPTOR,            () -> (WidgetRepresentation) new TabsRepresentation()),
            entry(TankWidget.WIDGET_DESCRIPTOR,            () -> (WidgetRepresentation) new TankRepresentation()),
            entry(TextEntryWidget.WIDGET_DESCRIPTOR,       () -> (WidgetRepresentation) new TextEntryRepresentation()),
            entry(TextSymbolWidget.WIDGET_DESCRIPTOR,      () -> (WidgetRepresentation) new TextSymbolRepresentation()),
            entry(TextUpdateWidget.WIDGET_DESCRIPTOR,      () -> (WidgetRepresentation) new TextUpdateRepresentation()),
            entry(ThermometerWidget.WIDGET_DESCRIPTOR,     () -> (WidgetRepresentation) new ThermometerRepresentation()),
            entry(WebBrowserWidget.WIDGET_DESCRIPTOR,      () -> (WidgetRepresentation) new WebBrowserRepresentation()),
            entry(DataBrowserWidget.WIDGET_DESCRIPTOR,     () -> (WidgetRepresentation) new DataBrowserRepresentation()),
            entry(XYPlotWidget.WIDGET_DESCRIPTOR,          () -> (WidgetRepresentation) new XYPlotRepresentation()));
    }
}
