/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.csstudio.archive.engine.model.ArchiveChannel;
import org.csstudio.archive.engine.model.BufferStats;
import org.csstudio.archive.engine.model.SampleBuffer;
import org.csstudio.archive.writer.rdb.TimestampHelper;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Helper for creating JSON for a servlet response.
 *  @author Kay Kasemir
 *  @author Dominic Oram JSON support in previous version
 */
@SuppressWarnings("nls")
public class JSONWriter implements AutoCloseable
{
    public static final ObjectMapper mapper = JsonMapper.builder().build();

    private final PrintWriter out;
    private final JsonGenerator jg;

    JSONWriter(final HttpServletRequest request,
               final HttpServletResponse response) throws IOException
    {
        response.setContentType("application/json");
        out = response.getWriter();
        jg = mapper.tokenStreamFactory().createGenerator(out);
        jg.writeStartObject();
    }

    public JsonGenerator getGenerator()
    {
        return jg;
    }

    @Override
    public void close() throws IOException
    {
        jg.writeEndObject();
        jg.close();
        out.close();
    }

    public void writeVType(final VType value) throws IOException
    {
        if (value == null)
            jg.writeNullProperty("Value");
        else
        {
            Instant timestamp = VTypeHelper.getTimestamp(value);
            jg.writeStringProperty("Timestamp", TimestampHelper.format(timestamp));
            jg.writeStringProperty("Value", VTypeHelper.toString(value));
        }
    }

    public void writeChannel(final ArchiveChannel channel) throws IOException
    {
        jg.writeStringProperty(Messages.HTTP_Channel, channel.getName());
        jg.writeBooleanProperty(Messages.HTTP_Connected, channel.isConnected());
        jg.writeStringProperty(Messages.HTTP_InternalState, channel.getInternalState());
        jg.writeStringProperty(Messages.HTTP_Mechanism, channel.getMechanism());

        jg.writeObjectPropertyStart(Messages.HTTP_CurrentValue);
        writeVType(channel.getCurrentValue());
        jg.writeEndObject();

        jg.writeObjectPropertyStart(Messages.HTTP_LastArchivedValue);
        writeVType(channel.getLastArchivedValue());
        jg.writeEndObject();

        jg.writeNumberProperty(Messages.HTTP_ReceivedValues, channel.getReceivedValues());
        jg.writeBooleanProperty(Messages.HTTP_State, channel.isEnabled());

        final SampleBuffer buffer = channel.getSampleBuffer();
        final BufferStats stats = buffer.getBufferStats();
        jg.writeNumberProperty(Messages.HTTP_QueueLen, buffer.getQueueSize());
        jg.writeNumberProperty(Messages.HTTP_QueueAvg, stats.getAverageSize());
        jg.writeNumberProperty(Messages.HTTP_QueueMax, stats.getMaxSize());
        jg.writeNumberProperty(Messages.HTTP_QueueCapacity, buffer.getCapacity());
        jg.writeNumberProperty(Messages.HTTP_QueueOverruns, stats.getOverruns());
    }
}
