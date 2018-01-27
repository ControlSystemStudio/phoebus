/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.persistence;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/** Indentation wrapper for STAX XMLStreamWriter.
 *
 *  <p>Delegates all calls to base writer.
 *  Adds indentation except when closing elements that contained
 *  text/data.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class IndentingXMLStreamWriter implements XMLStreamWriter
{
    private static final String NEWLINE = "\n";
    private static final String INDENTATION = "  ";

    private final XMLStreamWriter base;

    private int level = 0;
    enum State { Idle, InElement, WithData }
    private State state = State.Idle;

    /** Constructor.
     *
     *  @param base XML writer to wrap.
     */
    public IndentingXMLStreamWriter(final XMLStreamWriter base)
    {
        this.base = base;
    }

    private void indent(final int level) throws XMLStreamException
    {
        base.writeCharacters(NEWLINE);
        for (int i=0; i<level; ++i)
            base.writeCharacters(INDENTATION);
        //state = State.InElement;
    }

    @Override
    public void writeDTD(final String dtd) throws XMLStreamException
    {
        base.writeDTD(dtd);
    }

    @Override
    public Object getProperty(final String name) throws IllegalArgumentException
    {
        return base.getProperty(name);
    }

    @Override
    public String getPrefix(final String uri) throws XMLStreamException
    {
        return base.getPrefix(uri);
    }

    @Override
    public void setPrefix(final String prefix, final String uri)
            throws XMLStreamException
    {
        base.setPrefix(prefix, uri);
    }

    @Override
    public void setDefaultNamespace(final String uri) throws XMLStreamException
    {
        base.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(final NamespaceContext context)
            throws XMLStreamException
    {
        base.setNamespaceContext(context);
    }

    @Override
    public NamespaceContext getNamespaceContext()
    {
        return base.getNamespaceContext();
    }

    @Override
    public void writeStartDocument() throws XMLStreamException
    {
        base.writeStartDocument();
    }

    @Override
    public void writeStartDocument(final String version)
            throws XMLStreamException
    {
        base.writeStartDocument(version);
    }

    @Override
    public void writeStartDocument(final String encoding, final String version)
            throws XMLStreamException
    {
        base.writeStartDocument(encoding, version);
    }

    @Override
    public void writeEmptyElement(final String namespaceURI, final String localName)
            throws XMLStreamException
    {
        indent(level);
        base.writeEmptyElement(namespaceURI, localName);
        state = level > 0 ? State.InElement : State.Idle;
    }

    @Override
    public void writeEmptyElement(final String prefix, final String localName,
            final String namespaceURI) throws XMLStreamException
    {
        indent(level);
        base.writeEmptyElement(prefix, localName, namespaceURI);
        state = level > 0 ? State.InElement : State.Idle;
    }

    @Override
    public void writeEmptyElement(final String localName)
            throws XMLStreamException
    {
        indent(level);
        base.writeEmptyElement(localName);
        state = level > 0 ? State.InElement : State.Idle;
    }

    @Override
    public void writeStartElement(final String localName)
            throws XMLStreamException
    {
        indent(level++);
        base.writeStartElement(localName);
        state = State.InElement;
    }

    @Override
    public void writeStartElement(final String namespaceURI, final String localName)
            throws XMLStreamException
    {
        indent(level++);
        base.writeStartElement(namespaceURI, localName);
        state = State.InElement;
    }

    @Override
    public void writeStartElement(final String prefix, final String localName,
            final String namespaceURI) throws XMLStreamException
    {
        indent(level++);
        base.writeStartElement(prefix, localName, namespaceURI);
        state = State.InElement;
    }

    @Override
    public void writeAttribute(final String localName, final String value)
            throws XMLStreamException
    {
        base.writeAttribute(localName, value);
    }

    @Override
    public void writeAttribute(final String prefix, final String namespaceURI,
            final String localName, final String value) throws XMLStreamException
    {
        base.writeAttribute(prefix, namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute(final String namespaceURI, final String localName,
            final String value) throws XMLStreamException
    {
        base.writeAttribute(namespaceURI, localName, value);
    }

    @Override
    public void writeNamespace(final String prefix, final String namespaceURI)
            throws XMLStreamException
    {
        base.writeNamespace(prefix, namespaceURI);
    }

    @Override
    public void writeDefaultNamespace(final String namespaceURI)
            throws XMLStreamException
    {
        base.writeDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeComment(final String data) throws XMLStreamException
    {
        base.writeComment(data);
    }

    @Override
    public void writeProcessingInstruction(final String target)
            throws XMLStreamException
    {
        base.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction(final String target, final String data)
            throws XMLStreamException
    {
        base.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeEntityRef(final String name) throws XMLStreamException
    {
        base.writeEntityRef(name);
    }

    @Override
    public void writeCharacters(final String text) throws XMLStreamException
    {
        state = State.WithData;
        base.writeCharacters(text);
    }

    @Override
    public void writeCharacters(final char[] text, final int start, final int len)
            throws XMLStreamException
    {
        state = State.WithData;
        base.writeCharacters(text, start, len);
    }

    @Override
    public void writeCData(final String data) throws XMLStreamException
    {
        state = State.WithData;
        base.writeCData(data);
    }

    @Override
    public void writeEndElement() throws XMLStreamException
    {
        --level;
        if (state == State.InElement)
            indent(level);
        base.writeEndElement();
        state = level > 0 ? State.InElement : State.Idle;
    }

    @Override
    public void writeEndDocument() throws XMLStreamException
    {
        base.writeEndDocument();
        base.writeCharacters(NEWLINE);
    }

    @Override
    public void flush() throws XMLStreamException
    {
        base.flush();
    }

    @Override
    public void close() throws XMLStreamException
    {
        base.close();
    }
}