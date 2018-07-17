/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import java.util.Objects;
import java.util.Optional;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.trends.databrowser3.persistence.XMLPersistence;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

import javafx.scene.paint.Color;

/** Information about configuration of an axis
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AxisConfig
{
    /** Model to which this axis belongs */
    private Optional<Model> model = Optional.empty();

    /** Visible? */
    private boolean visible;

    /** Name, axis label */
    private String name;

    /** Use axis name as axis label? */
    private boolean use_axis_name = false;

    /** Use trace names as axis label? */
    private boolean use_trace_names = true;

    /** Is axis on right side of plot? */
    private boolean is_right = false;

    /** Color */
    private Color color;

    /** Axis range */
    private double min, max;

    /** Show grid line? */
    private boolean show_grid;

    /** Auto-scale? */
    private boolean auto_scale;

    /** Logarithmic scale? */
    private boolean log_scale;

    /** Initialize with defaults
     *  @param name
     */
    public AxisConfig(final String name)
    {
        this(true, name, !Preferences.use_trace_names, Preferences.use_trace_names, false,
             Color.BLACK, 0.0, 10.0, false, Preferences.use_auto_scale, false);
    }

    /** Initialize
     *  @param visible
     *  @param name
     *  @param use_axis_name
     *  @param use_trace_names
     *  @param is_right
     *  @param rgb
     *  @param min
     *  @param max
     *  @param auto_scale
     *  @param log_scale
     */
    public AxisConfig(final boolean visible, final String name,
            final boolean use_axis_name,
            final boolean use_trace_names,
            final boolean is_right,
            final Color col,
            final double min,
            final double max,
            final boolean show_grid,
            final boolean auto_scale,
            final boolean log_scale)
    {
        this.visible = visible;
        this.name = Objects.requireNonNull(name);
        this.use_axis_name = use_axis_name;
        this.use_trace_names = use_trace_names;
        this.is_right = is_right;
        this.color = Objects.requireNonNull(col);
        this.min = min;
        this.max = max;
        this.show_grid = show_grid;
        this.auto_scale = auto_scale;
        this.log_scale = log_scale;
    }

    /** @param model Model to which this item belongs */
    void setModel(final Model model)
    {
        this.model = Optional.ofNullable(model);
    }

    /** @return <code>true</code> if axis should be displayed */
    public boolean isVisible()
    {
        return visible;
    }

    /** @param visible Should axis be displayed? */
    public void setVisible(final boolean visible)
    {
        this.visible = visible;
        fireAxisChangeEvent();
    }

    /** @return Axis title, may include macros */
    public String getName()
    {
        return name;
    }

    /** @return Axis title, macros have been resolved */
    public String getResolvedName()
    {
        if (model.isPresent())
            return model.get().resolveMacros(name);
        else
            return name;
    }

    /** @param name New axis title */
    public void setName(final String name)
    {
        this.name = name;
        fireAxisChangeEvent();
    }

    /** @return <code>true</code>if axis name is used */
    public boolean isUsingAxisName()
    {
        return use_axis_name;
    }

    /** @param use_axis_name If <code>true</code>, show axis name */
    public void useAxisName(final boolean use_axis_name)
    {
        this.use_axis_name = use_axis_name;
        fireAxisChangeEvent();
    }

    /** @return <code>true</code> if using trace names as axis label */
    public boolean isUsingTraceNames()
    {
        return use_trace_names;
    }

    /** @param use_trace_names <code>true</code> to use trace names as axis label */
    public void useTraceNames(final boolean use_trace_names)
    {
        this.use_trace_names = use_trace_names;
        fireAxisChangeEvent();
    }

    /** Is axis on right side of plot? */
    public boolean isOnRight()
    {
        return is_right;
    }

    /** @param is_right Is axis on right side of plot? */
    public void setOnRight(final boolean is_right)
    {
        this.is_right = is_right;
        fireAxisChangeEvent();
    }

    /** @return Color */
    public Color getPaintColor()
    {
        return color;
    }

    /** @param color New color */
    public void setColor(final Color color)
    {
        this.color = Objects.requireNonNull(color);
        fireAxisChangeEvent();
    }

    /** @return Axis range minimum */
    public double getMin()
    {
        return min;
    }

    /** @return Axis range maximum */
    public double getMax()
    {
        return max;
    }

    /** @param min New axis range maximum
     *  @param max New axis range maximum
     */
    public void setRange(final double min, final double max)
    {
        // Ignore empty range
        if (min == max) return;
        // Assert min is below max
        if (min < max)
        {
            this.min = min;
            this.max = max;
        }
        else
        {
            this.min = max;
            this.max = min;
        }
        fireAxisChangeEvent();
    }

    /** @return <code>true</code> if grid lines are drawn */
    public boolean isGridVisible()
    {
        return show_grid;
    }

    /** @param visible Should grid be visible? */
    public void setGridVisible(final boolean grid)
    {
        show_grid = grid;
        fireAxisChangeEvent();
    }

    /** @return Auto-scale? */
    public boolean isAutoScale()
    {
        return auto_scale;
    }

    /** @param auto_scale Should axis auto-scale? */
    public void setAutoScale(final boolean auto_scale)
    {
        this.auto_scale = auto_scale;
        fireAxisChangeEvent();
    }

    /** @return Logarithmic scale? */
    public boolean isLogScale()
    {
        return log_scale;
    }

    /** @param log_scale Use logarithmic scale? */
    public void setLogScale(final boolean log_scale)
    {
        this.log_scale = log_scale;
        fireAxisChangeEvent();
    }

    /** Notify model about changes */
    private void fireAxisChangeEvent()
    {
        if (model.isPresent())
            model.get().fireAxisChangedEvent(Optional.of(this));
    }

    /** Write XML formatted axis configuration
     *  @param writer {@link XMLStreamWriter}
     *  @throws Exception on error
     */
    public void write(final XMLStreamWriter writer) throws Exception
    {
        writer.writeStartElement(XMLPersistence.TAG_AXIS);

        writer.writeStartElement(XMLPersistence.TAG_VISIBLE);
        writer.writeCharacters(Boolean.toString(visible));
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_NAME);
        writer.writeCharacters(name);
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_USE_AXIS_NAME);
        writer.writeCharacters(Boolean.toString(use_axis_name));
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_USE_TRACE_NAMES);
        writer.writeCharacters(Boolean.toString(use_trace_names));
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_RIGHT);
        writer.writeCharacters(Boolean.toString(is_right));
        writer.writeEndElement();
        if (color != null)
            XMLPersistence.writeColor(writer, XMLPersistence.TAG_COLOR, color);

        writer.writeStartElement(XMLPersistence.TAG_MIN);
        writer.writeCharacters(Double.toString(min));
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_MAX);
        writer.writeCharacters(Double.toString(max));
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_GRID);
        writer.writeCharacters(Boolean.toString(show_grid));
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_AUTO_SCALE);
        writer.writeCharacters(Boolean.toString(auto_scale));
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_LOG_SCALE);
        writer.writeCharacters(Boolean.toString(log_scale));
        writer.writeEndElement();

        writer.writeEndElement();
    }

    /** Create Axis info from XML document
     *  @param node
     *  @return AxisConfig
     *  @throws Exception on error
     */
    public static AxisConfig fromDocument(final Element node) throws Exception
    {
        final boolean visible = XMLUtil.getChildBoolean(node, XMLPersistence.TAG_VISIBLE).orElse(true);
        final String name = XMLUtil.getChildString(node, XMLPersistence.TAG_NAME).orElse("");
        final boolean use_axis_name = XMLUtil.getChildBoolean(node, XMLPersistence.TAG_USE_AXIS_NAME).orElse(true);
        final boolean use_trace_names = XMLUtil.getChildBoolean(node, XMLPersistence.TAG_USE_TRACE_NAMES).orElse(Preferences.use_trace_names);
        final boolean right = XMLUtil.getChildBoolean(node, XMLPersistence.TAG_RIGHT).orElse(false);
        final Color rgb = XMLPersistence.loadColorFromDocument(node).orElse(Color.BLACK);
        final double min = XMLUtil.getChildDouble(node, XMLPersistence.TAG_MIN).orElse(0.0);
        final double max = XMLUtil.getChildDouble(node, XMLPersistence.TAG_MAX).orElse(10.0);
        final boolean show_grid = XMLUtil.getChildBoolean(node, XMLPersistence.TAG_GRID).orElse(false);
        final boolean auto_scale = XMLUtil.getChildBoolean(node, XMLPersistence.TAG_AUTO_SCALE).orElse(Preferences.use_auto_scale);
        final boolean log_scale = XMLUtil.getChildBoolean(node, XMLPersistence.TAG_LOG_SCALE).orElse(false);
        return new AxisConfig(visible, name, use_axis_name, use_trace_names, right, rgb, min, max, show_grid, auto_scale, log_scale);
    }

    /** @return Copied axis configuration. Not associated with a model */
    public AxisConfig copy()
    {
        return new AxisConfig(visible, name, use_axis_name, use_trace_names,
                is_right, color, min, max, show_grid, auto_scale, log_scale);
    }

    /** @return String representation for debugging */
    @Override
    public String toString()
    {
        return "Axis '" + name + "', range " + min + " ... " + max + ", "
                + color.toString();
    }
}
