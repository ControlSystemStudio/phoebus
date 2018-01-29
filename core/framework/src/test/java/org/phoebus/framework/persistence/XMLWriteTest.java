/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.persistence;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.junit.Test;

/** JUnit test/demo of writing XML
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XMLWriteTest
{
    /** Create example XML content */
    private void writeExampleContent(final XMLStreamWriter writer) throws XMLStreamException
    {
        writer.writeStartDocument(XMLUtil.ENCODING, "1.0");
        {   // Blocks mimic nesting of elements
            writer.writeStartElement("display");
            {
                writer.writeStartElement("widget");
                writer.writeAttribute("type", "whatever");
                {
                    writer.writeStartElement("x");
                    writer.writeCharacters("42");
                    writer.writeEndElement();

                    writer.writeStartElement("y");
                    writer.writeCharacters("73");
                    writer.writeEndElement();

                    writer.writeEmptyElement("align");
                    writer.writeAttribute("side", "left");

                    writer.writeStartElement("text");
                    writer.writeCharacters("Hello, Dolly!");
                    writer.writeEndElement();

                    writer.writeEmptyElement("option");
                    writer.writeAttribute("wrap", "true");
                }
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

    /** Demonstrate plain XML output with default writer */
    @Test
    public void testPlainXML() throws Exception
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final XMLStreamWriter base = XMLOutputFactory.newInstance().createXMLStreamWriter(buf, XMLUtil.ENCODING);
        writeExampleContent(base);

        final String xml = buf.toString();
        System.out.println("\n== Plain XML output ==");
        System.out.println(xml);

        assertThat(xml, containsString("<?xml"));
    }

    /** Demonstrate plain XML output with default writer */
    @Test
    public void testIndentedXML() throws Exception
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final XMLStreamWriter base = XMLOutputFactory.newInstance().createXMLStreamWriter(buf, XMLUtil.ENCODING);
        final IndentingXMLStreamWriter indent = new IndentingXMLStreamWriter(base);
        writeExampleContent(indent);

        System.out.println("\n== Indented XML output ==");
        final String xml = buf.toString();
        System.out.println(xml);

        // There should be some newlines
        assertThat(xml, containsString("\n"));
        // The '<widget ..' should be indented by at least 2 spaces
        assertThat(xml, containsString("  <widget"));

        // Should look like this where each child element
        // is indented (by 2 spaces) relative to the parent
        final String desired = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                               "<display>\n" +
                               "  <widget type=\"whatever\">\n" +
                               "    <x>42</x>\n" +
                               "    <y>73</y>\n" +
                               "    <align side=\"left\"/>\n" +
                               "    <text>Hello, Dolly!</text>\n" +
                               "    <option wrap=\"true\"/>\n" +
                               "  </widget>\n" +
                               "</display>\n";
        System.out.println("Desired output:");
        System.out.println(desired);

        assertThat(xml, equalTo(desired));
    }
}
