/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.persistence;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.Optional;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** XML Helper
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XMLUtil
{
    public static final String ENCODING = "UTF-8";

    /** Iterator over all Elements (not just Nodes) of a parent
     *  that have specific name.
     *
     *  This iterator allows appending new elements to the document,
     *  after the current iterator position, and the next() call will
     *  then find them.
     *  For that reason 'next' cannot already identify the following
     *  element, because it may not exist, yet.
     */
    private static class NamedElementIterator implements Iterator<Element>
    {
        private final String name;
        private Node first;
        private Element current;

        NamedElementIterator(final Node parent, final String name)
        {
            this.name = name;
            first = parent.getFirstChild();
            current = null;
        }

        @Override
        public boolean hasNext()
        {
            if (first != null)
            {
                current = findElementByName(first, name);
                first = null;
            }
            else if (current != null)
                current = findElementByName(current.getNextSibling(), name);
            return current != null;
        }

        @Override
        public Element next()
        {
            return current;
        }
    }

    /** Write DOM to stream
     *  @param node Node from which on to write. May be the complete {@link Document}
     *  @param stream Output stream
     *  @throws Exception on error
     */
    public static void writeDocument(Node node, OutputStream stream) throws Exception
    {
        final Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.ENCODING, ENCODING);
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        transformer.transform(new DOMSource(node), new StreamResult(stream));
    }

    public static Element createTextElement(final Document doc, final String name, final String value)
    {
        final Element el = doc.createElement(name);
        el.appendChild(doc.createTextNode(value));
        return el;
    }

    /** Look for Element node of given name.
     *
     *  <p>Checks the node itself and its siblings for an {@link Element}.
     *  Does not descent down the 'child' links.
     *
     *  @param node Node where to start.
     *  @param name Name of the node to look for.
     *  @return Returns node, the next matching sibling, or <code>null</code>.
     */
    private static final Element findElementByName(Node node, final String name)
    {
        while (node != null)
        {
            if (node.getNodeType() == Node.ELEMENT_NODE &&
                node.getNodeName().equals(name))
                return (Element) node;
            node = node.getNextSibling();
        }
        return null;
    }

    /** Look for child node of given name.
     *
     *  @param parent Node where to start.
     *  @param name Name of the node to look for.
     *  @return Returns Element or <code>null</code>.
     */
    public static final Element getChildElement(final Node parent, final String name)
    {
        return findElementByName(parent.getFirstChild(), name);
    }

    /** Obtain all child elements with given name.
     *  @param parent Parent node
     *  @param name Name of child elements
     *  @return {@link Iterable} for matching child elements
     */
    public static Iterable<Element> getChildElements(final Node parent, final String name)
    {
        return () -> new NamedElementIterator(parent, name);
    }


    /** Get string value of an element.
     *  @param element Element
     *  @return String of the node. Empty string if nothing found.
     */
    public static String getString(final Element element)
    {
        final Node text = element.getFirstChild();
        if (text == null) // <empty /> node
            return "";
        if ((text.getNodeType() == Node.TEXT_NODE  ||
             text.getNodeType() == Node.CDATA_SECTION_NODE))
            return text.getNodeValue();
        return "";
    }

    /** Given a parent element, locate string value of a child node.
     *  @param parent Parent element
     *  @param name Name of child element
     *  @return Value of child element, or empty result
     */
    public static Optional<String> getChildString(final Element parent, final String name)
    {
        final Element child = getChildElement(parent, name);
        if (child != null)
            return Optional.of(getString(child));
        else
            return Optional.empty();
    }

    /** Given a parent element, locate double value of a child node.
     *  @param parent Parent element
     *  @param name Name of child element
     *  @return Value of child element, or empty result
     *  @throws Exception on error parsing the number
     */
    public static Optional<Double> getChildDouble(final Element parent, final String name) throws Exception
    {
        final Element child = getChildElement(parent, name);
        if (child == null)
            return Optional.empty();
        try
        {
            return Optional.of(Double.valueOf(getString(child)));
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Expected double for <" + name +">", ex);
        }
    }

    /** Given a parent element, locate boolean value of a child node.
     *  @param parent Parent element
     *  @param name Name of child element
     *  @return Value of child element, or empty result
     */
    public static Optional<Boolean> getChildBoolean(final Element parent, final String name)
    {
        final Element child = getChildElement(parent, name);
        if (child != null)
            return Optional.of(Boolean.parseBoolean(getString(child)));
        else
            return Optional.empty();
    }
}
