/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.persist;

import java.io.InputStream;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Helper for preserving XML line number information.
 *
 *  <p>Adds user data to each node. To fetch line number:
 *  <pre>
 *    node.getUserData("lineNumber")
 *  </pre>
 *  User object is of type {@link Integer}
 *
 *  @author http://stackoverflow.com/questions/4915422/get-line-number-from-xml-node-java
 */
@SuppressWarnings("nls")
class PositionalXMLReader
{
    /** User data tag for line number */
    public static final String LINE_NUMBER = "lineNumber";

    /** Read XML, creating document where nodes have line number in user data.
     *  @param stream
     *  @return {@link Document}
     *  @throws Exception on error
     */
    public static Document readXML(final InputStream stream) throws Exception
    {
        final Document doc;
        SAXParser parser;
        try
        {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
            final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                    .newInstance();
            final DocumentBuilder docBuilder = docBuilderFactory
                    .newDocumentBuilder();
            doc = docBuilder.newDocument();
        }
        catch (final ParserConfigurationException e)
        {
            throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
        }

        final Stack<Element> elementStack = new Stack<Element>();
        final StringBuilder textBuffer = new StringBuilder();
        final DefaultHandler handler = new DefaultHandler()
        {
            private Locator locator;

            @Override
            public void setDocumentLocator(final Locator locator)
            {
                this.locator = locator; // Save the locator, so that it can be
                                        // used later for line tracking when
                                        // traversing nodes.
            }

            @Override
            public void startElement(final String uri, final String localName,
                    final String qName, final Attributes attributes)
                    throws SAXException
            {
                addTextIfNeeded();
                final Element el = doc.createElement(qName);
                for (int i = 0; i < attributes.getLength(); i++)
                {
                    el.setAttribute(attributes.getQName(i),
                            attributes.getValue(i));
                }
                el.setUserData(LINE_NUMBER,
                        Integer.valueOf(this.locator.getLineNumber()), null);
                elementStack.push(el);
            }

            @Override
            public void endElement(final String uri, final String localName,
                    final String qName)
            {
                addTextIfNeeded();
                final Element closedEl = elementStack.pop();
                if (elementStack.isEmpty())
                { // Is this the root element?
                    doc.appendChild(closedEl);
                }
                else
                {
                    final Element parentEl = elementStack.peek();
                    parentEl.appendChild(closedEl);
                }
            }

            @Override
            public void characters(final char ch[], final int start,
                    final int length) throws SAXException
            {
                textBuffer.append(ch, start, length);
            }

            // Outputs text accumulated under the current node
            private void addTextIfNeeded()
            {
                if (textBuffer.length() > 0)
                {
                    final Element el = elementStack.peek();
                    final Node textNode = doc.createTextNode(textBuffer
                            .toString());
                    el.appendChild(textNode);
                    textBuffer.delete(0, textBuffer.length());
                }
            }
        };
        parser.parse(stream, handler);

        return doc;
    }
}