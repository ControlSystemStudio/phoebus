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
import org.csstudio.archive.engine.model.BufferStats;
import org.csstudio.archive.engine.model.EngineModel;
import org.csstudio.archive.engine.model.SampleBuffer;

/** 'channel' web page
 *  @author Kay Kasemir
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

        final HTMLWriter html = new HTMLWriter(response, "Archive Engine Channel");
        html.openTable(2, "Channel Info");

        html.tableLine("Channel", channel.getName());
        html.tableLine("Connected", channel.isConnected()
              ? "Connected"
              : HTMLWriter.makeRedText("Disconnected"));

        html.tableLine("Internal State", channel.getInternalState());
        html.tableLine("Mechanism", channel.getMechanism());
        html.tableLine("Current Value", channel.getCurrentValueAsString());
        html.tableLine("Last Archived Value", channel.getLastArchivedValueAsString());
        html.tableLine("Enablement", channel.getEnablement().toString());
        html.tableLine("State", channel.isEnabled()
                ? "Enabled"
                : HTMLWriter.makeRedText("Disabled"));

        final SampleBuffer buffer = channel.getSampleBuffer();
        html.tableLine("Queue Len.", Integer.toString(buffer.getQueueSize()));

        final BufferStats stats = buffer.getBufferStats();
        html.tableLine("Queue Avg.", String.format("%.1f", stats.getAverageSize()));

        html.tableLine("Queue Max.", Integer.toString(stats.getMaxSize()));

        html.tableLine("Queue Capacity", Integer.toString(buffer.getCapacity()));

        final int overrun_count = stats.getOverruns();
        String overruns = Integer.toString(overrun_count);
        if (overrun_count > 0)
            overruns = HTMLWriter.makeRedText(overruns);
        html.tableLine("Queue Overruns", overruns );

        html.closeTable();
        html.close();
    }
}
