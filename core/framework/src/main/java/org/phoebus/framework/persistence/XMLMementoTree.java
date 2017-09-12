/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.persistence;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Tree-structured Memento implementation
 *
 *  <p>... based on XML DOM
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XMLMementoTree implements MementoTree
{
    // Basic implementation, using the attributes of elements for the data,
    // is based on org.eclipse.ui.XMLMemento
    private static final String ROOT = "memento";
    private final Document document;
    private final Element element;

    /** @return Default file used for the memento */
    public static File getDefaultFile()
    {
        // Same location as user preferences
        return new File(System.getProperty("java.util.prefs.userRoot", System.getProperty("user.home")),
                        ".phoebus/memento");
    }

    /** @return New, empty memento
     *  @throws Exception on error
     */
    public static XMLMementoTree create() throws Exception
    {
        final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        final Element root = document.createElement(ROOT);
        document.adoptNode(root);
        return new XMLMementoTree(document, root);
    }

    /** @param in Stream to which memento is written
     *  @throws Exception on error
     */
    public static XMLMementoTree read(final InputStream in) throws Exception
    {
        final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document document = builder.parse(in);
        final Element root = document.getDocumentElement();
        if (! root.getNodeName().equals(ROOT))
            throw new Exception("Expected <" + ROOT + "> but got <" + root.getNodeName() + ">");
        return new XMLMementoTree(document, root);
    }

    /** @param out Stream to which memento is written
     *  @throws Exception on error
     */
    public void write(final OutputStream out) throws Exception
    {
        XMLUtil.writeDocument(element, out);
    }


    private XMLMementoTree(final Document document, final Element element)
    {
        this.document = document;
        this.element = element;
    }

    @Override
    public void setString(final String key, final String value)
    {
        if (value == null)
            element.removeAttribute(key);
        else
            element.setAttribute(key, value);
    }

    @Override
    public void setNumber(final String key, final Number value)
    {
        setString(key, value.toString());
    }

    @Override
    public void setBoolean(final String key, final Boolean value)
    {
        setString(key, value.toString());
    }

    @Override
    public String getString(final String key)
    {
        final Attr attr = element.getAttributeNode(key);
        if (attr == null)
            return null;
        return attr.getValue();
    }

    @Override
    public Number getNumber(final String key)
    {
        final String text = getString(key);
        if (text == null)
            return null;
        try
        {
            final double dbl = Double.parseDouble(text);
            // If it is indeed a floating point number, return that
            if (Math.rint(dbl) != dbl)
                return dbl;
            // Parse as long to allow for large values
            final long lng = Long.parseLong(text);
            if (lng > Integer.MAX_VALUE  ||   lng < Integer.MIN_VALUE)
                return lng;
            // If small enough, fall back to integer
            return (int) lng;
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }

    @Override
    public Boolean getBoolean(final String key)
    {
        final String text = getString(key);
        if (text == null)
            return null;
        return Boolean.valueOf(text);
    }

    @Override
    public MementoTree getChild(final String key)
    {
        Element child = XMLUtil.getChildElement(element, key);
        if (child == null)
        {
            child = document.createElement(key);
            element.appendChild(child);
        }
        return new XMLMementoTree(document, child);
    }

    @Override
    public List<MementoTree> getChildren()
    {
        final List<MementoTree> children = new ArrayList<>();
        Node node = element.getFirstChild();
        while (node != null)
        {
            if (node instanceof Element)
                children.add(new XMLMementoTree(document, (Element)node));
            node = node.getNextSibling();
        }
        return children;
    }

    @Override
    public String toString()
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try
        {
            write(buf);
        }
        catch (Exception ex)
        {
            return "Error: " + ex;
        }
        return buf.toString();
    }
}
