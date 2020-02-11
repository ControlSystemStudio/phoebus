/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.persist;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.ArrayWidgetProperty;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Preferences;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.widgets.PlaceholderWidget;
import org.phoebus.framework.persistence.IndentingXMLStreamWriter;
import org.phoebus.framework.persistence.XMLUtil;

/** Write model as XML.
 *
 *  <p>For each widget, writes each property.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ModelWriter implements Closeable
{
    /** Internal flag for unit tests.
     *  Default values are usually not written,
     *  but for tests they can be included in the XML output.
     *
     *  <b>Not API.</b>
     */
    public static boolean skip_defaults = Preferences.skip_defaults;

    private final XMLStreamWriter writer;

    /** Convert widgets into XML
     *  @param widgets Widgets
     *  @return XML for the model
     *  @throws Exception on error
     */
    public static String getXML(final List<Widget> widgets) throws Exception
    {
        final ByteArrayOutputStream xml = new ByteArrayOutputStream();
        try
        (
            final ModelWriter writer = new ModelWriter(xml);
        )
        {
            writer.writeWidgets(widgets);
        }
        return xml.toString(XMLUtil.ENCODING);
    }

    /** Create writer.
     *
     *  <p>Best used in try-with-resources to support auto-close.
     *
     *  @param stream Output stream to write, will be closed
     *  @throws Exception on error
     */
    public ModelWriter(final OutputStream stream) throws Exception
    {
        final XMLStreamWriter base =
            XMLOutputFactory.newInstance().createXMLStreamWriter(stream, XMLUtil.ENCODING);
        writer = new IndentingXMLStreamWriter(base);

        writer.writeStartDocument(XMLUtil.ENCODING, "1.0");
        writer.writeStartElement(XMLTags.DISPLAY);
        writer.writeAttribute(XMLTags.VERSION, DisplayModel.VERSION.toString());
    }

    /** @return {@link XMLStreamWriter}, can be used to customize the XML */
    public XMLStreamWriter getWriter()
    {
        return writer;
    }

    /** Write display model
     *  @param model Display model to write
     *  @throws Exception on error
     */
    public void writeModel(final DisplayModel model) throws Exception
    {
        // Write properties of display itself
        writeWidgetProperties(model);

        // Write each widget of the display
        writeWidgets(model.runtimeChildren().getValue());
        writer.flush();
    }

    /** Write widgets and their children
     *
     *  @param widgets Widgets to write
     *  @throws Exception on error
     */
    public void writeWidgets(final List<Widget> widgets) throws Exception
    {
        for (Widget widget : widgets)
            writeWidget(widget);
    }

    /** Write widget
     *  @param widget Widget to write
     *  @throws Exception on error
     */
    protected void writeWidget(final Widget widget) throws Exception
    {   // 'protected' to allow unit test calls
        if (widget instanceof PlaceholderWidget)
            ((PlaceholderWidget)widget).writeToXML(this, writer);
        else
        {
            writer.writeStartElement(XMLTags.WIDGET);
            writer.writeAttribute(XMLTags.TYPE, widget.getType());
            writer.writeAttribute(XMLTags.VERSION, widget.getVersion().toString());

            writeWidgetProperties(widget);

            ChildrenProperty children = ChildrenProperty.getChildren(widget);
            if (children != null)
                children.writeToXML(this, writer);

            writer.writeEndElement();
        }
    }

    /** @param widget All properties of this widget, except for runtime and default props, are written
     *  @throws Exception on error
     */
    private void writeWidgetProperties(final Widget widget) throws Exception
    {
        for (final WidgetProperty<?> property : widget.getProperties())
        {   // Skip runtime properties
            if (property.getCategory() == WidgetPropertyCategory.RUNTIME)
                continue;
            // Skip read-only properties
            if (property.isReadonly())
                continue;
            // Skip writing default values for certain properties
            if (skip_defaults && property.isDefaultValue())
                continue;

            writeProperty(property);
        }
    }

    /** @param property Single property to write
     *  @throws Exception on error
     */
    public void writeProperty(final WidgetProperty<?> property) throws Exception
    {
        if (property instanceof ArrayWidgetProperty<?>)
        {   // Skip empty arrays, which would just be a start/end tag
            final ArrayWidgetProperty<?> array = (ArrayWidgetProperty<?>) property;
            if (array.getValue().isEmpty())
                return;
        }
        writer.writeStartElement(property.getName());
        if (property.isUsingWidgetClass())
            writer.writeAttribute(XMLTags.USE_CLASS, Boolean.TRUE.toString());
        property.writeToXML(this, writer);
        writer.writeEndElement();
    }

    /** Flush and close XML. */
    @Override
    public void close() throws IOException
    {
        try
        {
            // End display
            writer.writeEndElement();

            // End and close document
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        }
        catch (final Exception ex)
        {
            throw new IOException("Failed to close XML", ex);
        }
    }
}
