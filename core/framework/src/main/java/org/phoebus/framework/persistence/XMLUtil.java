/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.persistence;

import java.io.InputStream;
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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** XML Helper
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XMLUtil
{
    public static final String ENCODING = "UTF-8";

    /** Open XML document with line-number-aware reader and locate root element
     *  @param stream XML stream
     *  @param expected_root Desired name of root element
     *  @return That root element
     *  @throws Exception on error, including document with wrong root
     */
    public static Element openXMLDocument(final InputStream stream,
            final String expected_root) throws Exception
    {
        // Parse XML
        final Document doc = PositionalXMLReader.readXML(stream);
        doc.getDocumentElement().normalize();

        // Check root element
        final Element root_node = doc.getDocumentElement();
        if (! expected_root.equals(root_node.getNodeName()))
            throw new Exception("Wrong document type. Expected <" +
                    expected_root + "> but found <" +
                    root_node.getNodeName() + ">");
        return root_node;
    }

    /** (Try to) obtain original line number in XML file for a node.
     *
     *  @param node Node in document
     *  @return Line number. Empty if not known.
     */
    public static Optional<Integer> getLineNumber(final Node node)
    {
        final Object info = node.getUserData(PositionalXMLReader.LINE_NUMBER);
        if (info instanceof Integer)
            return Optional.of((Integer)info);
        return Optional.empty();
    }

    /** Get line number info for XML-related error messages.
     *
     *  @param node Node in document
     *  @return Line number as string or "unknown"
     */
    public static String getLineInfo(final Node node)
    {
        final Optional<Integer> number = getLineNumber(node);
        if (number.isPresent())
            return Integer.toString(number.get());
        return "unknown";
    }

    /** Iterator over all Elements (not just Nodes) of a parent */
    private static class ElementIterator implements Iterator<Element>
    {
        private Element next_node;

        ElementIterator(final Node parent)
        {
            next_node = findElement(parent.getFirstChild());
        }

        @Override
        public boolean hasNext()
        {
            return next_node != null;
        }

        @Override
        public Element next()
        {
            final Element current = next_node;
            next_node = findElement(current.getNextSibling());
            return current;
        }
    }

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

    /** Look for Element node.
     *
     *  <p>Checks the node and its siblings.
     *  Does not descent down the 'child' links.
     *
     *  @param node Node where to start.
     *  @return Returns node, next Element sibling or <code>null</code>.
     */
    private  static final Element findElement(Node node)
    {
        while (node != null)
        {
            if (node.getNodeType() == Node.ELEMENT_NODE)
                return (Element) node;
            node = node.getNextSibling();
        }
        return null;
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

    /** Obtain all child elements.
     *  @param parent Parent node
     *  @return {@link Iterable} for child elements
     */
    public static Iterable<Element> getChildElements(final Node parent)
    {
        return () -> new ElementIterator(parent);
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

    /** Given a parent element, locate integer value of a child node.
     *  @param parent Parent element
     *  @param name Name of child element
     *  @return Value of child element, or empty result
     *  @throws Exception on error parsing the number
     */
    public static Optional<Integer> getChildInteger(final Element parent, final String name) throws Exception
    {
        final Element child = getChildElement(parent, name);
        if (child == null)
            return Optional.empty();
        try
        {
            return Optional.of(Integer.valueOf(getString(child)));
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Expected integer for <" + name +">", ex);
        }
    }

    /** Given a parent element, locate long value of a child node.
     *  @param parent Parent element
     *  @param name Name of child element
     *  @return Value of child element, or empty result
     *  @throws Exception on error parsing the number
     */
    public static Optional<Long> getChildLong(final Element parent, final String name) throws Exception
    {
        final Element child = getChildElement(parent, name);
        if (child == null)
            return Optional.empty();
        try
        {
            return Optional.of(Long.valueOf(getString(child)));
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Expected long for <" + name +">", ex);
        }
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

    /** @param text Text that should contain true or false
     *  @param default_value Value to use when text is empty
     *  @return Boolean value of text
     */
    public static boolean parseBoolean(final String text, final boolean default_value)
    {
        if (text == null  ||  text.isEmpty())
            return default_value;
        return Boolean.parseBoolean(text);
    }

    /** Transform xml element and children into a string
    *
    * @param nd Node root of elements to transform
    * @return String representation of xml
    */
   public static String elementToString(Node nd, boolean add_newlines)
   {
       if (Node.CDATA_SECTION_NODE == nd.getNodeType())
           return "<![CDATA[" + nd.getNodeValue() + "]]&gt;";

       // return if simple element type
       final String name = nd.getNodeName();
       if (name.startsWith("#"))
       {
           if (name.equals("#text"))
               return nd.getNodeValue();
           return "";
       }

       // output name
       String ret = "<" + name;

       // output attributes
       NamedNodeMap attrs = nd.getAttributes();
       if (attrs != null)
       {
           for (int idx = 0; idx < attrs.getLength(); idx++)
           {
               Node attr = attrs.item(idx);
               ret += " " + attr.getNodeName() + "=\"" + attr.getNodeValue() + "\"";
           }
       }

       final String text = nd.getTextContent();
       final NodeList child_ndls = nd.getChildNodes();
       String all_child_str = "";

       for (int idx = 0; idx < child_ndls.getLength(); idx++)
       {
           final String child_str = elementToString(child_ndls.item(idx), add_newlines);
           if ((child_str != null) && (child_str.length() > 0))
               all_child_str += child_str;
       }
       if (all_child_str.length() > 0)
       {
           // output children
           ret += ">" + (add_newlines ? "\n" : " ");
           ret += all_child_str;
           ret += "</" + name + ">";
       }
       else if ((text != null) && (text.length() > 0))
       {
           // output text
           ret += text;
           ret += "</" + name + ">";
       }
       else
       {
           // output nothing
           ret += "/>" + (add_newlines ? "\n" : " ");
       }

       return ret;
   }

   public static String elementsToString(NodeList nls, boolean add_newlines)
   {
       String ret = "";
       for (int i = 0; i < nls.getLength(); i++)
       {
           final String nextstr = elementToString(nls.item(i), add_newlines).trim();
           if (nextstr.length() > 0)
           {
               if (ret.length() > 0)
                   ret += (add_newlines)? "\n" : " ";
               ret += nextstr;
           }
       }
       return ret;
   }

   /** Update the value of a tag
    *
    *  <p>Creates or updates a child element with given tag and value.
    *  The child element will have only that value as a text node,
    *  other existing values will be removed.
    *
    *  @param parent Parent element
    *  @param name Tag name to update
    *  @param value Value of that tag
    */
   public static void updateTag(final Element parent, final String name, final String value)
   {
       final Document doc = parent.getOwnerDocument();
       Element child = getChildElement(parent, name);
       if (child == null)
       {
           child = doc.createElement(name);
           parent.appendChild(child);
       }
       Node n = child.getFirstChild();
       while (n != null)
       {
           child.removeChild(n);
           n = n.getNextSibling();
       }
       child.appendChild(doc.createTextNode(value));
  }

}
