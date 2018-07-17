/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.macros;

import static org.csstudio.display.builder.model.ModelPlugin.logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.csstudio.display.builder.model.persist.XMLTags;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.persistence.XMLUtil;
import org.w3c.dom.Element;

/** Read/write macros as XML
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacroXMLUtil
{
    /** Write macros (without surrounding "&ltmacros>") into XML stream
     *  @param writer XML writer
     *  @param macros Macros to write
     *  @throws Exception on error
     */
    public static void writeMacros(final XMLStreamWriter writer, final Macros macros) throws Exception
    {
        for (String name : macros.getNames())
        {
            writer.writeStartElement(name);
            writer.writeCharacters(macros.getValue(name));
            writer.writeEndElement();
        }
    }

    /** Read content of "&ltmacros>"
     *  @param macros_xml XML that contains macros
     */
    public static Macros readMacros(final Element macros_xml)
    {
        final Macros macros = new Macros();
        for (Element element : XMLUtil.getChildElements(macros_xml))
        {
            final String name = element.getTagName();
            final String value = XMLUtil.getString(element);
            // Legacy used 'include_parent_macros'
            // in a way that actually conflicts with a macro of that name.
            // This implementation _always_ inherits parent macros,
            // so that setting is obsolete.
            if (name.equals("include_parent_macros"))
                continue;
            macros.add(name, value);
        }
        return macros;
    }

    /** Read content of "&ltmacros>", without the surrounding "&ltmacros>
     *  @param macros_xml Text that contains XML for macros
     *  @throws Exception on error in XML
     */
    public static Macros readMacros(final String macros_xml) throws Exception
    {
        final String full_xml = "<" + XMLTags.MACROS + ">" + macros_xml + "</" + XMLTags.MACROS + ">";
        try
        {
            final ByteArrayInputStream stream = new ByteArrayInputStream(full_xml.getBytes());
            final Element root = XMLUtil.openXMLDocument(stream, XMLTags.MACROS);
            return readMacros(root);
        }
        catch (Exception ex)
        {
            throw new Exception("Macros need to be in the format " +
                                "<name>value</name><other_name>value</other_name>, got " + macros_xml,
                                ex);
        }
    }

    /** @param macros Macros to write
     *  @return XML for macros (without surrounding "&ltmacros>")
     */
    public static String toString(final Macros macros)
    {
        final ByteArrayOutputStream xml = new ByteArrayOutputStream();
        try
        {
            final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(xml, XMLUtil.ENCODING);
            writeMacros(writer, macros);
            writer.close();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot serialize macros " + macros, ex);
        }
        return xml.toString();
    }
}
