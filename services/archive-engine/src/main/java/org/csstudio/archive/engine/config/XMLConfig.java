/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.config;

import java.io.File;
import java.io.FileOutputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.csstudio.archive.engine.model.ArchiveChannel;
import org.csstudio.archive.engine.model.ArchiveGroup;
import org.csstudio.archive.engine.model.DeltaArchiveChannel;
import org.csstudio.archive.engine.model.Enablement;
import org.csstudio.archive.engine.model.EngineModel;
import org.csstudio.archive.engine.model.MonitoredArchiveChannel;
import org.csstudio.archive.engine.model.ScannedArchiveChannel;
import org.phoebus.framework.persistence.IndentingXMLStreamWriter;
import org.phoebus.framework.persistence.XMLUtil;

/** XML-based engine configuration
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XMLConfig
{
    private static final String CHANNEL = "channel";
    private static final String DELTA = "delta";
    private static final String ENABLE = "enable";
    private static final String ENGINECONFIG = "engineconfig";
    private static final String GROUP = "group";
    private static final String MONITOR = "monitor";
    private static final String NAME = "name";
    private static final String PERIOD = "period";
    private static final String SCAN = "scan";

    private final EngineModel model;

    /** @param model {@link EngineModel} to configure
     *  @throws Exception on error
     */
    public XMLConfig(final EngineModel model) throws Exception
    {
        this.model = model;
    }

    public void write(final File file) throws Exception
    {
        final XMLStreamWriter base =
                XMLOutputFactory.newInstance().createXMLStreamWriter(new FileOutputStream(file), XMLUtil.ENCODING);
        final XMLStreamWriter writer = new IndentingXMLStreamWriter(base);
        writer.writeStartDocument(XMLUtil.ENCODING, "1.0");
        writer.writeStartElement(ENGINECONFIG);
        {
            for (int g=0; g<model.getGroupCount(); ++g)
                write(writer, model.getGroup(g));
        }
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();
    }

    private void write(final XMLStreamWriter writer, final ArchiveGroup group) throws Exception
    {
        writer.writeStartElement(GROUP);
        {
            writer.writeStartElement(NAME);
            writer.writeCharacters(group.getName());

            for (int c=0; c<group.getChannelCount(); ++c)
                write(writer, model.getChannel(c));
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    private void write(final XMLStreamWriter writer, final ArchiveChannel channel) throws Exception
    {
        writer.writeStartElement(CHANNEL);
        {
            writer.writeStartElement(NAME);
            writer.writeCharacters(channel.getName());
            writer.writeEndElement();

            if (channel instanceof DeltaArchiveChannel)
            {
                writer.writeEmptyElement(MONITOR);

                writer.writeStartElement(PERIOD);
                writer.writeCharacters(Double.toString(((DeltaArchiveChannel)channel).getPeriodEstimate()));
                writer.writeEndElement();

                writer.writeStartElement(DELTA);
                writer.writeCharacters(Double.toString(((DeltaArchiveChannel)channel).getDelta()));
                writer.writeEndElement();

            }

            if (channel instanceof MonitoredArchiveChannel)
            {
                writer.writeEmptyElement(MONITOR);
                writer.writeStartElement(PERIOD);
                writer.writeCharacters(Double.toString(((MonitoredArchiveChannel)channel).getPeriodEstimate()));
                writer.writeEndElement();
            }

            if (channel instanceof ScannedArchiveChannel)
            {
                writer.writeEmptyElement(SCAN);
                writer.writeStartElement(PERIOD);
                writer.writeCharacters(Double.toString(((ScannedArchiveChannel)channel).getPeriod()));
                writer.writeEndElement();
            }

            if (channel.getEnablement() == Enablement.Enabling)
                writer.writeEmptyElement(ENABLE);
        }
        writer.writeEndElement();
    }
}
