/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.csstudio.archive.Engine;
import org.csstudio.archive.engine.model.ArchiveChannel;
import org.csstudio.archive.engine.model.ArchiveGroup;
import org.csstudio.archive.engine.model.BufferStats;
import org.csstudio.archive.engine.model.EngineModel;
import org.csstudio.archive.engine.model.SampleBuffer;

import com.fasterxml.jackson.core.JsonGenerator;

/** 'groups' web page
 *  @author Kay Kasemir
 *  @author Dominic Oram JSON support in previous version
 */
@SuppressWarnings("nls")
public class GroupServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    /** Maximum text length of last value that's displayed */
    private static final int MAX_VALUE_DISPLAY = 60;

    /** Bytes in a MegaByte */
    protected final static double MB = 1024.0*1024.0;

    @Override
    protected void doGet(final HttpServletRequest request,
                         final HttpServletResponse response) throws ServletException, IOException
    {
        final EngineModel model = Engine.getModel();

        final String group_name = request.getParameter("name");
        if (group_name == null)
        {
            response.sendError(400, "Missing group name");
            return;
        }
        final ArchiveGroup group = model.getGroup(group_name);
        if (group == null)
        {
            response.sendError(400, "Unknown group " + group_name);
            return;
        }

        if ("json".equals(request.getParameter("format")))
        {
            final JSONWriter json = new JSONWriter(request, response);
            final JsonGenerator jg = json.getGenerator();

            // Basic group info
            jg.writeBooleanField(Messages.HTTP_Enabled, group.isEnabled());

            final ArchiveChannel ena_channel = group.getEnablingChannel();
            if (ena_channel != null)
                jg.writeStringField(Messages.HTTP_EnablingChannel, ena_channel.getName());

            // JSON object of all channels in the group
            jg.writeArrayFieldStart(Messages.HTTP_Channels);

            final int channel_count = group.getChannelCount();
            for (int j=0; j<channel_count; ++j)
            {
                jg.writeStartObject();
                json.writeChannel(group.getChannel(j));
                jg.writeEndObject();
            }

            jg.writeEndArray();

            json.close();
        }
        else
        {
            final HTMLWriter html = new HTMLWriter(response, "Archive Engine Group " + group_name);

            // Basic group info
            html.openTable(2, Messages.HTTP_Status);
            html.tableLine(Messages.HTTP_State, group.isEnabled() ? Messages.HTTP_Enabled : Messages.HTTP_Disabled);
            final ArchiveChannel ena_channel = group.getEnablingChannel();
            if (ena_channel != null)
                html.tableLine(Messages.HTTP_EnablingChannel,
                               HTMLWriter.makeLink("channel?name=" + ena_channel.getName(),
                                                   ena_channel.getName()));
            html.closeTable();

            html.h2(Messages.HTTP_Channels);

            // HTML Table of all channels in the group
            html.openTable(1,
                Messages.HTTP_Channel,
                Messages.HTTP_Connected,
                Messages.HTTP_Mechanism,
                Messages.HTTP_CurrentValue,
                Messages.HTTP_LastArchivedValue,
                Messages.HTTP_ReceivedValues,
                Messages.HTTP_QueueLen,
                Messages.HTTP_QueueAvg,
                Messages.HTTP_QueueMax,
                Messages.HTTP_QueueCapacity,
                Messages.HTTP_QueueOverruns);
            final int channel_count = group.getChannelCount();
            for (int j=0; j<channel_count; ++j)
            {
                final ArchiveChannel channel = group.getChannel(j);
                final String connected = channel.isConnected()
                ? Messages.HTTP_Connected : HTMLWriter.makeRedText(Messages.HTTP_Disconnected);
                final SampleBuffer buffer = channel.getSampleBuffer();
                final BufferStats stats = buffer.getBufferStats();
                final int overrun_count = stats.getOverruns();
                String overruns = Integer.toString(overrun_count);
                if (overrun_count > 0)
                    overruns = HTMLWriter.makeRedText(overruns);

                String current_value = channel.getCurrentValueAsString();
                if (current_value.length() > MAX_VALUE_DISPLAY)
                    current_value = current_value.substring(0, MAX_VALUE_DISPLAY);
                String last_value = channel.getLastArchivedValueAsString();
                if (last_value.length() > MAX_VALUE_DISPLAY)
                    last_value = last_value.substring(0, MAX_VALUE_DISPLAY);
                html.tableLine(
                    HTMLWriter.makeLink("channel?name=" + channel.getName(), channel.getName()),
                    connected,
                    channel.getMechanism(),
                    current_value,
                    last_value,
                    Long.toString(channel.getReceivedValues()),
                    Integer.toString(buffer.getQueueSize()),
                    String.format("%.1f", stats.getAverageSize()),
                    Integer.toString(stats.getMaxSize()),
                    Integer.toString(buffer.getCapacity()),
                    overruns);
            }
            html.closeTable();

            html.close();
        }
    }
}
