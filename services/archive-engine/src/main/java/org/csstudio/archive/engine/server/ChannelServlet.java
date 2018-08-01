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

/** 'channel' web page
 *  @author Kay Kasemir
 *  @author Dominic Oram JSON support in previous version
 */
@SuppressWarnings("nls")
public class ChannelServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    /** Bytes in a MegaByte */
    protected final static double MB = 1024.0*1024.0;

    @Override
    protected void doGet(final HttpServletRequest request,
                         final HttpServletResponse response) throws ServletException, IOException
    {
        final String channel_name = request.getParameter("name");
        if (channel_name == null)
        {
            response.sendError(400, "Missing channel name");
            return;
        }

        final EngineModel model = Engine.getModel();
        final ArchiveChannel channel = model.getChannel(channel_name);

        if ("json".equals(request.getParameter("format")))
        {
            final JSONWriter json = new JSONWriter(request, response);
            final JsonGenerator jg = json.getGenerator();

            jg.writeObjectFieldStart(Messages.HTTP_ChannelInfo);
            json.writeChannel(channel);
            jg.writeEndObject();

            jg.writeObjectFieldStart("Group Membership");
            for (int i=0; i<channel.getGroupCount(); ++i)
            {
                final ArchiveGroup group = channel.getGroup(i);
                jg.writeBooleanField(group.getName(), group.isEnabled());
            }
            jg.writeEndObject();

            json.close();
        }
        else
        {
            final HTMLWriter html = new HTMLWriter(response, "Archive Engine Channel");
            html.openTable(2, Messages.HTTP_ChannelInfo);

            html.tableLine(Messages.HTTP_Channel, channel.getName());
            html.tableLine(Messages.HTTP_Connected, channel.isConnected()
                  ? Messages.HTTP_Connected
                  : HTMLWriter.makeRedText(Messages.HTTP_Disconnected));

            html.tableLine(Messages.HTTP_InternalState, channel.getInternalState());
            html.tableLine(Messages.HTTP_Mechanism, channel.getMechanism());
            html.tableLine(Messages.HTTP_CurrentValue, channel.getCurrentValueAsString());
            html.tableLine(Messages.HTTP_LastArchivedValue, channel.getLastArchivedValueAsString());
            html.tableLine(Messages.HTTP_Enablement, channel.getEnablement().toString());
            html.tableLine(Messages.HTTP_State, channel.isEnabled()
                    ? Messages.HTTP_Enabled
                    : HTMLWriter.makeRedText(Messages.HTTP_Disabled));

            final SampleBuffer buffer = channel.getSampleBuffer();
            html.tableLine(Messages.HTTP_QueueLen, Integer.toString(buffer.getQueueSize()));

            final BufferStats stats = buffer.getBufferStats();
            html.tableLine(Messages.HTTP_QueueAvg, String.format("%.1f", stats.getAverageSize()));

            html.tableLine(Messages.HTTP_QueueMax, Integer.toString(stats.getMaxSize()));

            html.tableLine(Messages.HTTP_QueueCapacity, Integer.toString(buffer.getCapacity()));

            final int overrun_count = stats.getOverruns();
            String overruns = Integer.toString(overrun_count);
            if (overrun_count > 0)
                overruns = HTMLWriter.makeRedText(overruns);
            html.tableLine(Messages.HTTP_QueueOverruns, overruns );

            html.closeTable();

            // Table of all the groups to which this channel belongs
            html.h2("Group Membership");
            html.openTable(1, Messages.HTTP_Group, Messages.HTTP_Enabled);
            for (int i=0; i<channel.getGroupCount(); ++i)
            {
                final ArchiveGroup group = channel.getGroup(i);
                html.tableLine(
                    HTMLWriter.makeLink("group?name=" + group.getName(), group.getName()),
                    group.isEnabled() ? Messages.HTTP_Enabled
                                      : Messages.HTTP_Disabled);
            }
            html.closeTable();

            html.close();
        }
    }
}
