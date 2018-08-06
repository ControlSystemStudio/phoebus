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

import com.fasterxml.jackson.core.JsonGenerator;

/** 'groups' web page
 *  @author Kay Kasemir
 *  @author Dominic Oram JSON support in previous version
 */
@SuppressWarnings("nls")
public class GroupsServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    /** Bytes in a MegaByte */
    protected final static double MB = 1024.0*1024.0;

    @Override
    protected void doGet(final HttpServletRequest request,
                         final HttpServletResponse response) throws ServletException, IOException
    {
        final EngineModel model = Engine.getModel();

        if ("json".equals(request.getParameter("format")))
        {
            final JSONWriter json = new JSONWriter(request, response);
            final JsonGenerator jg = json.getGenerator();

            jg.writeArrayFieldStart("Archive Engine Groups");

            final int group_count = model.getGroupCount();
            int total_channels = 0;
            int total_connect = 0;
            long total_received_values = 0;
            // Per group objects
            for (int i=0; i<group_count; ++i)
            {
                final ArchiveGroup group = model.getGroup(i);
                final int channel_count = group.getChannelCount();
                int connect_count = 0;
                double queue_avg = 0;
                int queue_max = 0;
                long received_values = 0;
                for (int j=0; j<channel_count; ++j)
                {
                    final ArchiveChannel channel = group.getChannel(j);
                    if (channel.isConnected())
                        ++connect_count;
                    received_values += channel.getReceivedValues();
                    final BufferStats stats =
                        channel.getSampleBuffer().getBufferStats();
                    queue_avg += stats.getAverageSize();
                    if (queue_max < stats.getMaxSize())
                        queue_max = stats.getMaxSize();
                }
                if (channel_count > 0)
                    queue_avg /= channel_count;
                total_channels += channel_count;
                total_connect += connect_count;
                total_received_values += received_values;

                jg.writeStartObject();
                jg.writeStringField(Messages.HTTP_Group, group.getName());
                jg.writeBooleanField(Messages.HTTP_Enabled, group.isEnabled());
                jg.writeNumberField(Messages.HTTP_ChannelCount, channel_count);
                jg.writeNumberField(Messages.HTTP_Connected, connect_count);
                jg.writeNumberField(Messages.HTTP_ReceivedValues, received_values);
                jg.writeNumberField(Messages.HTTP_QueueAvg, queue_avg);
                jg.writeNumberField(Messages.HTTP_QueueMax, queue_max);
                jg.writeEndObject();
            }

            // 'Total' object
            jg.writeStartObject();
            jg.writeStringField(Messages.HTTP_Group, Messages.HTTP_Total);
            jg.writeNumberField(Messages.HTTP_ChannelCount, total_channels);
            jg.writeNumberField(Messages.HTTP_Connected, total_connect);
            jg.writeNumberField(Messages.HTTP_ReceivedValues, total_received_values);
            jg.writeEndObject();

            jg.writeEndArray();

            json.close();
        }
        else
        {
            final HTMLWriter html = new HTMLWriter(response, "Archive Engine Groups");
            html.openTable(1, Messages.HTTP_Group,
                              Messages.HTTP_Enabled,
                              Messages.HTTP_ChannelCount,
                              Messages.HTTP_Connected,
                              Messages.HTTP_ReceivedValues,
                              Messages.HTTP_QueueAvg,
                              Messages.HTTP_QueueMax);

            final int group_count = model.getGroupCount();
            int total_channels = 0;
            int total_connect = 0;
            long total_received_values = 0;
            // Per group lines
            for (int i=0; i<group_count; ++i)
            {
                final ArchiveGroup group = model.getGroup(i);
                final int channel_count = group.getChannelCount();
                int connect_count = 0;
                double queue_avg = 0;
                int queue_max = 0;
                long received_values = 0;
                for (int j=0; j<channel_count; ++j)
                {
                    final ArchiveChannel channel = group.getChannel(j);
                    if (channel.isConnected())
                        ++connect_count;
                    received_values += channel.getReceivedValues();
                    final BufferStats stats =
                        channel.getSampleBuffer().getBufferStats();
                    queue_avg += stats.getAverageSize();
                    if (queue_max < stats.getMaxSize())
                        queue_max = stats.getMaxSize();
                }
                if (channel_count > 0)
                    queue_avg /= channel_count;
                total_channels += channel_count;
                total_connect += connect_count;
                total_received_values += received_values;

                final String connected = (channel_count == connect_count)
                    ? Integer.toString(connect_count)
                    : HTMLWriter.makeRedText(Integer.toString(connect_count));

                html.tableLine(
                    HTMLWriter.makeLink("group?name=" + group.getName(), group.getName()),
                    group.isEnabled()
                      ? Messages.HTTP_Enabled : HTMLWriter.makeRedText(Messages.HTTP_Disabled),
                    Integer.toString(channel_count),
                    connected,
                    Long.toString(received_values),
                    String.format("%.1f", queue_avg),
                    Integer.toString(queue_max));
            }
            // 'Total' line
            final String connected = (total_channels == total_connect)
                ? Integer.toString(total_connect)
                : HTMLWriter.makeRedText(Integer.toString(total_connect));
            html.tableLine(
                Messages.HTTP_Total,
                "",
                Integer.toString(total_channels),
                connected,
                Long.toString(total_received_values),
                "",
                "");

            html.closeTable();
            html.close();
        }
    }
}
