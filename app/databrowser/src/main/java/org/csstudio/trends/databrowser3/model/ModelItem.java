/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.javafx.rtplot.LineStyle;
import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.trends.databrowser3.persistence.XMLPersistence;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

import javafx.scene.paint.Color;

/** Base of {@link PVItem} and {@link FormulaItem},
 *  i.e. the items held by the {@link Model}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class ModelItem
{
    /** Name by which the item is identified: PV name, formula */
    private volatile String name;

    /** Model that contains this item. Empty while not assigned to a model
     */
    protected Optional<Model> model = Optional.empty();

    /** Preferred display name, used in plot legend */
    private volatile String display_name;

    /** Units, may be <code>null</code> */
    private volatile String units;

    /** Show item's samples? */
    private volatile boolean visible = true;

    /** RGB for item's color
     *  <p>
     *  Technically, javafx.scene.paint.Color adds a UI dependency to the Model.
     *  As long as the Model can still run without a Display
     *  or Shell, this might be OK.
     */
    private volatile Color color = null;

    /** How to display the trace */
    private volatile TraceType trace_type = Preferences.trace_type;

    /** Line width [pixel] */
    private volatile int line_width = Preferences.line_width;

    /** Line style */
    private volatile LineStyle line_style = LineStyle.SOLID;

    /** How to display the points of the trace */
    private volatile PointType point_type = PointType.NONE;

    /** Point size [pixel] */
    private volatile int point_size = Preferences.line_width;

    /** Y-Axis */
    private volatile AxisConfig axis = null;

    /** Sample that is currently selected, for example via cursor */
    private volatile Optional<PlotDataItem<Instant>> selected_sample = Optional.empty();

    /**
     * Unique string id that identifies this item. A random UUID is set upon instantiation,
     * and there is no setter method.
     */
    private String uniqueId;

    /** Initialize
     *  @param name Name of the PV or the formula
     */
    public ModelItem(final String name)
    {
        this.name = name;
        this.display_name = name;
        this.uniqueId = UUID.randomUUID().toString();
    }

    public ModelItem(){
        this.uniqueId = UUID.randomUUID().toString();
    }

    public String getUniqueId(){
        return uniqueId;
    }

    /** @return Model that contains this item */
    public Optional<Model> getModel()
    {
        return model;
    }

    /** Called by Model to add item to Model or remove it from model.
     *  Should not be called by other code!
     *  @param model Model to which item was added or <code>null</code> when removed
     */
    void setModel(final Model model)
    {
        final Optional<Model> new_model = Optional.ofNullable(model);
        if (model != null  &&  this.model.equals(new_model))
            throw new RuntimeException("Item re-assigned to same model: " + name);
        this.model = new_model;
    }

    /** @return Name of this item (PV, Formula, ...), may contain macros */
    public String getName()
    {
        return name;
    }

    /** @return Name of this item (PV, Formula, ...) with all macros resolved */
    public String getResolvedName()
    {
        if (model.isPresent())
            return model.get().resolveMacros(name);
        else
            return name;
    }

    /** @param new_name New item name
     *  @see #getName()
     *  @return <code>true</code> if name was actually changed
     *  @throws Exception on error (cannot create PV for new name, ...)
     */
    public boolean setName(String new_name) throws Exception
    {
        new_name = new_name.trim();
        if (new_name.isEmpty())
            throw new Exception("Empty name");
        if (new_name.equals(name))
            return false;
        name = new_name;
        fireItemLookChanged();
        return true;
    }

    /** @return Preferred display name, used in plot legend.
     *  May contain macros.
     */
    public String getDisplayName()
    {
        return display_name;
    }

    /** @return Preferred display name, used in plot legend,
     *          with macros resolved.
     */
    public String getResolvedDisplayName()
    {
        if (model.isPresent())
            return model.get().resolveMacros(display_name);
        else
            return display_name;
    }

    /** @param new_display_name New display name
     *  @see #getDisplayName()
     */
    public void setDisplayName(String new_display_name)
    {
        new_display_name = new_display_name.trim();
        if (new_display_name.equals(display_name))
            return;
        display_name = new_display_name;
        fireItemLookChanged();
    }

    /** @param units Units, may be <code>null</code> */
    public void setUnits(final String units)
    {
        if (Objects.equals(this.units, units))
            return;
        this.units = units;
        model.ifPresent(m -> m.fireItemUnitsChanged(this));
    }

    /** @return Units, may be <code>null</code> */
    public String getUnits()
    {
        return units;
    }

    /** @return <code>true</code> if item should be displayed */
    public boolean isVisible()
    {
        return visible;
    }

    /** @param visible Should item be displayed? */
    public void setVisible(final boolean visible)
    {
        if (this.visible == visible)
            return;
        this.visible = visible;
        model.ifPresent(m -> m.fireItemVisibilityChanged(this));
    }

    /** If (!) assigned to a model, inform it about a configuration change */
    protected void fireItemLookChanged()
    {
        model.ifPresent(m -> m.fireItemLookChanged(this));
    }

    /** Get item's color.
     *  For new items, the color is <code>null</code> until it's
     *  either set via setColor() or by adding it to a {@link Model}.
     *  @return Item's color
     *  @see #setColor(Color)
     */
    public Color getPaintColor()
    {
        return color;
    }

    /** @param new_col New color for this item */
    public void setColor(final Color new_col)
    {
        if (new_col.equals(color))
            return;
        color = new_col;
        fireItemLookChanged();
    }

    /** @return {@link TraceType} for displaying the trace */
    public TraceType getTraceType()
    {
        return trace_type;
    }

    /** @param trace_type New {@link TraceType} for displaying the trace */
    public void setTraceType(final TraceType trace_type)
    {
        if (this.trace_type == trace_type)
            return;
        this.trace_type = trace_type;
        fireItemLookChanged();
    }

    /** @return Line width */
    public int getLineWidth()
    {
        return line_width;
    }

    /** @param width New line width */
    public void setLineWidth(int width)
    {
        if (width < 0)
            width = 0;
        if (width == this.line_width)
            return;
        line_width = width;
        fireItemLookChanged();
    }

    /** @return Line Style */
    public LineStyle getLineStyle()
    {
        return line_style;
    }

    /** @param line_style Line Style */
    public void setLineStyle(final LineStyle line_style)
    {
        if (this.line_style == line_style)
            return;
        this.line_style = line_style;
        fireItemLookChanged();
    }

    /** @return {@link PointType} for displaying the trace */
    public PointType getPointType()
    {
        return point_type;
    }

    /** @param point_type New {@link PointType} for displaying the trace */
    public void setPointType(final PointType point_type)
    {
        if (this.point_type == point_type)
            return;
        this.point_type = point_type;
        fireItemLookChanged();
    }

    /** @return Point size */
    public int getPointSize()
    {
        return point_size;
    }

    /** @param size New point size */
    public void setPointSize(int size)
    {
        if (size < 0)
            size = 0;
        if (size == this.point_size)
            return;
        point_size = size;
        fireItemLookChanged();
    }

    /** @return Y-Axis */
    public AxisConfig getAxis()
    {
        return axis;
    }

    /** @return Index of Y-Axis in model */
    public int getAxisIndex()
    {   // Allow this to work in Tests w/o model
        if (model.isPresent())
            return model.get().getAxisIndex(axis);
        else
            return 0;
    }

    /** @param axis New X-Axis index */
    public void setAxis(final AxisConfig axis)
    {   // Comparing exact AxisConfig reference, not equals()!
        if (axis == this.axis)
            return;
        this.axis = axis;
        fireItemLookChanged();
    }

    /** This method should be overridden if the instance needs
     *  to change its behavior according to waveform index.
     *  If it is not overridden, this method always return 0.
     *  @return Waveform index
     */
    public int getWaveformIndex()
    {
        return 0;
    }

    /** This method should be overridden if the instance needs
     *  to change its behavior according to waveform index.
     *  @param index New waveform index
     */
    public void setWaveformIndex(int index)
    {
        // Do nothing.
    }

    /** @return Samples held by this item */
    abstract public PlotSamples getSamples();

    /** @param selected_sample Sample that is currently selected, for example via cursor */
    public void setSelectedSample(final Optional<PlotDataItem<Instant>> selected_sample)
    {
        this.selected_sample = selected_sample;
    }

    /** @return Sample that is currently selected, for example via cursor */
    public Optional<PlotDataItem<Instant>> getSelectedSample()
    {
        return selected_sample;
    }

    /** Write XML formatted item configuration
     *  @param writer {@link XMLStreamWriter}
     *  @throws Exception on error
     */
    abstract public void write(final XMLStreamWriter writer) throws Exception;

    /** Write XML configuration common to all Model Items
     *  @param writer {@link XMLStreamWriter}
     *  @throws Exception on error
     */
    protected void writeCommonConfig(final XMLStreamWriter writer) throws Exception
    {
        writer.writeStartElement(XMLPersistence.TAG_DISPLAYNAME);
        writer.writeCharacters(getDisplayName());
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_VISIBLE);
        writer.writeCharacters(Boolean.toString(isVisible()));
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_NAME);
        writer.writeCharacters(getName());
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_AXIS);
        writer.writeCharacters(Integer.toString(getAxisIndex()));
        writer.writeEndElement();

        XMLPersistence.writeColor(writer, XMLPersistence.TAG_COLOR, getPaintColor());

        writer.writeStartElement(XMLPersistence.TAG_TRACE_TYPE);
        writer.writeCharacters(getTraceType().name());
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_LINEWIDTH);
        writer.writeCharacters(Integer.toString(getLineWidth()));
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_LINE_STYLE);
        writer.writeCharacters(getLineStyle().name());
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_POINT_TYPE);
        writer.writeCharacters(getPointType().name());
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_POINT_SIZE);
        writer.writeCharacters(Integer.toString(getPointSize()));
        writer.writeEndElement();

        writer.writeStartElement(XMLPersistence.TAG_WAVEFORM_INDEX);
        writer.writeCharacters(Integer.toString(getWaveformIndex()));
        writer.writeEndElement();
    }

    /** Load common XML configuration elements into this item
     *  @param model Model to which this item will belong (but doesn't, yet)
     *  @param node XML document node for this item
     *  @throws Exception on error
     */
    protected void configureFromDocument(final Model model, final Element node) throws Exception
    {
        display_name = XMLUtil.getChildString(node, XMLPersistence.TAG_DISPLAYNAME).orElse(display_name);
        visible = XMLUtil.getChildBoolean(node, XMLPersistence.TAG_VISIBLE).orElse(true);
        // Ideally, configuration should define all axes before they're used,
        // but as a fall-back create missing axes
        final int axis_index = XMLUtil.getChildInteger(node, XMLPersistence.TAG_AXIS).orElse(0);
        while (model.getAxisCount() <= axis_index)
            model.addAxis();
        axis = model.getAxis(axis_index);
        line_width = XMLUtil.getChildInteger(node, XMLPersistence.TAG_LINEWIDTH).orElse(line_width);
        try
        {
            line_style = LineStyle.valueOf(XMLUtil.getChildString(node, XMLPersistence.TAG_LINE_STYLE).orElse(LineStyle.SOLID.name()));
        }
        catch (Throwable ex)
        {
            line_style = LineStyle.SOLID;
        }
        point_size = XMLUtil.getChildInteger(node, XMLPersistence.TAG_POINT_SIZE).orElse(point_size);

        XMLPersistence.loadColorFromDocument(node).ifPresent(col -> color = col);

        // First load PointType, which may be replaced by legacy point-in-TraceType below
        try
        {
            point_type = PointType.valueOf(XMLUtil.getChildString(node, XMLPersistence.TAG_POINT_TYPE).orElse(PointType.NONE.name()));
        }
        catch (Throwable ex)
        {
            point_type = PointType.NONE;
        }

        String type = XMLUtil.getChildString(node, XMLPersistence.TAG_TRACE_TYPE).orElse(TraceType.AREA.name());
        try
        {   // Replace XYGraph types with currently supported types
            if (type.equals("ERROR_BARS"))
                type = TraceType.AREA.name();
            else if (type.equals("CROSSES"))
                type = PointType.XMARKS.name();
            trace_type = TraceType.valueOf(type);
        }
        catch (Throwable ex)
        {   // Check if older config file used what is now a PointType for the TraceType
            try
            {
                point_type = PointType.valueOf(type);
                // If that succeeded, clear trace type
                trace_type = TraceType.NONE;
            }
            catch (Throwable ex2)
            {   // Never mind, leave PointType as is, and use default TraceType
                trace_type = TraceType.AREA;
            }
        }
        setWaveformIndex(XMLUtil.getChildInteger(node, XMLPersistence.TAG_WAVEFORM_INDEX).orElse(0));
    }

    /** Dispose all data */
    public abstract void dispose();

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return name;
    }
}
