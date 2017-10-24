/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
import org.csstudio.display.builder.model.widgets.plots.ImageWidget;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget;

/** Factory that creates widgets based on type
 *
 *  <p>Widgets register with their 'primary' type,
 *  which needs to be unique.
 *  In addition, they can provide alternate types
 *  to list the legacy types which they handle.
 *  More than one widget can register for the same
 *  alternate type.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WidgetFactory
{
    /** Extension point that allows contributing widgets */
    public static final String EXTENSION_POINT_ID = "org.csstudio.display.builder.model.widgets";

    /** Exception that indicates an unknown widget type */
    public static class WidgetTypeException extends Exception
    {
        private static final long serialVersionUID = 1L;
        private final String type;

        public WidgetTypeException(final String type, final String message)
        {
            super(message);
            this.type = type;
        }

        /** @return Widget type that's not known */
        public String getType()
        {
            return type;
        }
    }

    /** Singleton instance */
    private static final WidgetFactory instance = new WidgetFactory();

    /** List of widget types.
     *
     *  <p>Sorted by widget category, then type
     */
    private final SortedSet<WidgetDescriptor> descriptors =
        new TreeSet<>(
            Comparator.comparing(WidgetDescriptor::getCategory)
                      .thenComparing(WidgetDescriptor::getType));

    /** Map of primary type IDs to {@link WidgetDescriptor} */
    private final Map<String, WidgetDescriptor> descriptor_by_type = new ConcurrentHashMap<>();

    /** Map of alternate type IDs to {@link WidgetDescriptor}s */
    private final Map<String, List<WidgetDescriptor>> alternates_by_type = new ConcurrentHashMap<>();

    // Prevent instantiation
    private WidgetFactory()
    {
        registerKnownWidgets();
        // TODO Load widgets from service
    }

    /** Add known widgets as fallback in absence of registry information */
    private void registerKnownWidgets ( ) {
        addWidgetType(ActionButtonWidget.WIDGET_DESCRIPTOR);
        addWidgetType(ArcWidget.WIDGET_DESCRIPTOR);
        addWidgetType(ArrayWidget.WIDGET_DESCRIPTOR);
        addWidgetType(BoolButtonWidget.WIDGET_DESCRIPTOR);
        addWidgetType(ByteMonitorWidget.WIDGET_DESCRIPTOR);
        addWidgetType(CheckBoxWidget.WIDGET_DESCRIPTOR);
        addWidgetType(ClockWidget.WIDGET_DESCRIPTOR);
        addWidgetType(ComboWidget.WIDGET_DESCRIPTOR);
        addWidgetType(DigitalClockWidget.WIDGET_DESCRIPTOR);
        addWidgetType(EllipseWidget.WIDGET_DESCRIPTOR);
        addWidgetType(EmbeddedDisplayWidget.WIDGET_DESCRIPTOR);
        addWidgetType(GaugeWidget.WIDGET_DESCRIPTOR);
        addWidgetType(GroupWidget.WIDGET_DESCRIPTOR);
        addWidgetType(ImageWidget.WIDGET_DESCRIPTOR);
        addWidgetType(KnobWidget.WIDGET_DESCRIPTOR);
        addWidgetType(LabelWidget.WIDGET_DESCRIPTOR);
        addWidgetType(LEDWidget.WIDGET_DESCRIPTOR);
        addWidgetType(LinearMeterWidget.WIDGET_DESCRIPTOR);
        addWidgetType(MeterWidget.WIDGET_DESCRIPTOR);
        addWidgetType(MultiStateLEDWidget.WIDGET_DESCRIPTOR);
        addWidgetType(NavigationTabsWidget.WIDGET_DESCRIPTOR);
        addWidgetType(PictureWidget.WIDGET_DESCRIPTOR);
        addWidgetType(PolygonWidget.WIDGET_DESCRIPTOR);
        addWidgetType(PolylineWidget.WIDGET_DESCRIPTOR);
        addWidgetType(ProgressBarWidget.WIDGET_DESCRIPTOR);
        addWidgetType(RadioWidget.WIDGET_DESCRIPTOR);
        addWidgetType(RectangleWidget.WIDGET_DESCRIPTOR);
        addWidgetType(ScaledSliderWidget.WIDGET_DESCRIPTOR);
        addWidgetType(ScrollBarWidget.WIDGET_DESCRIPTOR);
        addWidgetType(SpinnerWidget.WIDGET_DESCRIPTOR);
        addWidgetType(SymbolWidget.WIDGET_DESCRIPTOR);
        addWidgetType(TableWidget.WIDGET_DESCRIPTOR);
        addWidgetType(TabsWidget.WIDGET_DESCRIPTOR);
        addWidgetType(TankWidget.WIDGET_DESCRIPTOR);
        addWidgetType(TextEntryWidget.WIDGET_DESCRIPTOR);
        addWidgetType(TextSymbolWidget.WIDGET_DESCRIPTOR);
        addWidgetType(TextUpdateWidget.WIDGET_DESCRIPTOR);
        addWidgetType(ThermometerWidget.WIDGET_DESCRIPTOR);
        addWidgetType(WebBrowserWidget.WIDGET_DESCRIPTOR);
        addWidgetType(XYPlotWidget.WIDGET_DESCRIPTOR);
    }

    /** @return Singleton instance */
    public static WidgetFactory getInstance()
    {
        return instance;
    }

    /** Inform factory about a widget type.
     *
     *  <p>This is meant to be called during plugin initialization,
     *  based on information from the registry.
     *
     *  @param descriptor {@link WidgetDescriptor}
     */
    public void addWidgetType(final WidgetDescriptor descriptor)
    {
        // Primary type must be unique
        if (descriptor_by_type.putIfAbsent(descriptor.getType(), descriptor) != null)
            throw new Error(descriptor + " already defined");

        // descriptors sorts by category and type
        descriptors.add(descriptor);

        for (final String alternate : descriptor.getAlternateTypes())
            alternates_by_type.computeIfAbsent(alternate, k -> new CopyOnWriteArrayList<>())
                              .add(descriptor);
    }

    /** @return Descriptions of all currently known widget types */
    public Set<WidgetDescriptor> getWidgetDescriptions()
    {
        return Collections.unmodifiableSortedSet(descriptors);
    }

    /** Check if type is defined.
     *  @param type Widget type ID
     *  @return WidgetDescriptor
     */
    public WidgetDescriptor getWidgetDescriptor(final String type)
    {
        return Objects.requireNonNull(descriptor_by_type.get(type));
    }

    /** Get all widget descriptors
     *  @param type Type ID of the widget or alternate type
     *  @return {@link WidgetDescriptor}s, starting with primary followed by possible alternates
     *  @throws WidgetTypeException when widget type is not known
     */
    public List<WidgetDescriptor> getAllWidgetDescriptors(final String type) throws WidgetTypeException
    {
        final List<WidgetDescriptor> descs = new ArrayList<>();
        final WidgetDescriptor descriptor = descriptor_by_type.get(type);
        if (descriptor != null)
            descs.add(descriptor);
        final List<WidgetDescriptor> alt = alternates_by_type.get(type);
        if (alt != null)
            descs.addAll(alt);
        if (descs.isEmpty())
            throw new WidgetTypeException(type, "Unknown widget type '" + type + "'");
        return descs;
    }
}
