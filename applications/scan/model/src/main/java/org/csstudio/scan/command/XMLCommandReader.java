/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.command;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Read {@link ScanCommand}s from XML stream.
 *
 *  <p>Depends on a {@link SimpleScanCommandFactory} to
 *  create the {@link ScanCommand} instances.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XMLCommandReader
{
    /** Read scan commands from XML string
     *  @param xml_text XML text that contains commands
     *  @return List of {@link ScanCommand}s
     *  @throws Exception on error: Unknown commands, missing command-specific details
     */
    public static List<ScanCommand> readXMLString(final String xml_text) throws Exception
    {
        final InputStream stream = new ByteArrayInputStream(xml_text.getBytes());
        return readXMLStream(stream);
    }

    /** Read scan commands from XML stream
     *  @param in XML stream that contains commands
     *  @return List of {@link ScanCommand}s
     *  @throws Exception on error: Unknown commands, missing command-specific details
     */
    public static List<ScanCommand> readXMLStream(final InputStream in) throws Exception
    {
        final DocumentBuilder docBuilder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document doc = docBuilder.parse(in);
        return readDocument(doc);
    }

    /** Read scan commands from XML document
     *  @param doc Document that contains commands
     *  @return List of {@link ScanCommand}s
     *  @throws Exception on error: Unknown commands, missing command-specific details
     */
    public static List<ScanCommand> readDocument(final Document doc) throws Exception
    {
        doc.getDocumentElement().normalize();
        final Element root_node = doc.getDocumentElement();
        if (! "commands".equals(root_node.getNodeName()))
            throw new Exception("Wrong document type");
        return ScanCommandFactory.readCommands(root_node.getFirstChild());
    }
}
