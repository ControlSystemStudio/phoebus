/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.command;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Persist {@link ScanCommand}s as XML to stream.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XMLCommandWriter
{
    /** Create DOM for commands
     *  @param commands Commands to write into DOM
     *  @return {@link Document}
     *  @throws Exception on error
     */
    private static Document createDOM(final List<ScanCommand> commands) throws Exception
    {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = dbf.newDocumentBuilder();
        final Document dom = builder.newDocument();

        final Element root = dom.createElement("commands");
        dom.appendChild(root);

        for (ScanCommand command : commands)
            command.writeXML(dom, root);

        return dom;
    }

    /** Write XML-formatted commands into stream
     *  @param stream Where to write the commands
     *  @param commands Commands to write as XML to output stream
     *  @throws Exception on error
     */
    public static void write(final OutputStream stream, final List<ScanCommand> commands) throws Exception
    {
        final Document dom = createDOM(commands);

        // Write XML into stream
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final Transformer transformer = transformerFactory.newTransformer();
        final DOMSource source = new DOMSource(dom);
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        final StreamResult result = new StreamResult(stream);
        transformer.transform(source, result);
    }

    /** Convert commands to XML-formatted commands into stream
     *  @param commands Commands
     *  @return XML-formatted document text for the commands
     *  @throws Exception on error
     */
    public static String toXMLString(final List<ScanCommand> commands) throws Exception
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, commands);
        out.close();
        return out.toString();
    }
}
