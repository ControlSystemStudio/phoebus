/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.ModelReader;
import org.csstudio.display.builder.model.persist.ModelWriter;
import org.csstudio.display.builder.model.persist.XMLTags;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Widget property that describes scripts.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScriptsWidgetProperty extends WidgetProperty<List<ScriptInfo>>
{
    /** Constructor
     *  @param descriptor Property descriptor
     *  @param widget Widget that holds the property and handles listeners
     *  @param default_value Default and initial value
     */
    public ScriptsWidgetProperty(
            final WidgetPropertyDescriptor<List<ScriptInfo>> descriptor,
            final Widget widget,
            final List<ScriptInfo> default_value)
    {
        super(descriptor, widget, default_value);
    }

    /** @param value Must be ScriptInfo array or List */
    @Override
    public void setValueFromObject(final Object value) throws Exception
    {
        if (value instanceof ScriptInfo[])
            setValue(Arrays.asList((ScriptInfo[]) value));
        else if (value instanceof Collection)
        {
            final List<ScriptInfo> scripts = new ArrayList<>();
            for (Object item : (Collection<?>)value)
                if (item instanceof ScriptInfo)
                    scripts.add((ScriptInfo)item);
                else
                    throw new Exception("Need ScriptInfo[], got " + value);
            setValue(scripts);
        }
        else
            throw new Exception("Need ScriptInfo[], got " + value);
    }

    @Override
    public void writeToXML(final ModelWriter model_writer, final XMLStreamWriter writer) throws Exception
    {
        // <script file="..">
        //   <pv trigger="true">pv_name</pv>
        // </script>
        for (final ScriptInfo info : value)
        {
            writer.writeStartElement(XMLTags.SCRIPT);
            writer.writeAttribute(XMLTags.FILE, info.getPath());
            if (! info.getCheckConnections())
                writer.writeAttribute(XMLTags.CHECK_CONNECTIONS, Boolean.FALSE.toString());
            final String text = info.getText();
            if (text != null)
            {
                writer.writeStartElement(XMLTags.TEXT);
                writer.writeCData(text);
                writer.writeEndElement();
            }
            for (final ScriptPV pv : info.getPVs())
            {
                writer.writeStartElement(XMLTags.PV_NAME);
                if (! pv.isTrigger())
                    writer.writeAttribute(XMLTags.TRIGGER, Boolean.FALSE.toString());
                writer.writeCharacters(pv.getName());
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    @Override
    public void readFromXML(final ModelReader model_reader, final Element property_xml) throws Exception
    {
        // Also handles legacy XML
        // <path pathString="test.py" checkConnect="true" sfe="false" seoe="false">
        //    <scriptText><![CDATA[  print "Hi!" ]]></scriptText>
        //    <pv trig="true">input1</pv>
        // </path>
        Iterable<Element> script_xml;
        if (XMLUtil.getChildElement(property_xml, XMLTags.SCRIPT) != null)
            script_xml = XMLUtil.getChildElements(property_xml, XMLTags.SCRIPT);
        else // Fall back to legacy tag
            script_xml = XMLUtil.getChildElements(property_xml, "path");

        final List<ScriptInfo> scripts = new ArrayList<>();
        for (final Element xml : script_xml)
        {
            String file = xml.getAttribute(XMLTags.FILE);
            if (file.isEmpty())
                file = xml.getAttribute("pathString");
            if (file.isEmpty())
                logger.log(Level.WARNING, "Missing script 'file'");

            String tag = xml.getAttribute(XMLTags.CHECK_CONNECTIONS);
            if (tag.isEmpty())
                tag = xml.getAttribute("checkConnect");
            final boolean check_connections = tag.isEmpty()
                    ? true
                    : Boolean.valueOf(tag);

            // Script content embedded in XML?
            Element text_xml = XMLUtil.getChildElement(xml, XMLTags.TEXT);
            if (text_xml == null)  // Fall back to legacy tag
                text_xml = XMLUtil.getChildElement(xml, "scriptText");
            final String text = text_xml != null
                ? text_xml.getFirstChild().getNodeValue()
                : null;

            final List<ScriptPV> pvs = readPVs(xml);
            scripts.add(new ScriptInfo(file, text, check_connections, pvs));
        }
        setValue(scripts);
    }

    private List<ScriptPV> readPVs(final Element xml)
    {
        final List<ScriptPV> pvs = new ArrayList<>();
        // Legacy used just 'pv'
        final Iterable<Element> pvs_xml;
        if (XMLUtil.getChildElement(xml, XMLTags.PV_NAME) != null)
            pvs_xml = XMLUtil.getChildElements(xml, XMLTags.PV_NAME);
        else
            pvs_xml = XMLUtil.getChildElements(xml, "pv");
        for (final Element pv_xml : pvs_xml)
        {   // Unless either the new or old attribute is _present_ and set to false,
            // default to triggering on this PV
            final boolean trigger =
                XMLUtil.parseBoolean(pv_xml.getAttribute(XMLTags.TRIGGER), true) &&
                XMLUtil.parseBoolean(pv_xml.getAttribute("trig"), true);
            final String name = XMLUtil.getString(pv_xml);
            pvs.add(new ScriptPV(name, trigger));
        }
        return pvs;
    }
}
