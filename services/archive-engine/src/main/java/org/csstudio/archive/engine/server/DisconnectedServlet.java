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
import org.csstudio.archive.engine.model.EngineModel;

import com.fasterxml.jackson.core.JsonGenerator;

/** 'disconnected' web page
 *  @author Kay Kasemir
 *  @author Dominic Oram JSON support in previous version
 */
@SuppressWarnings("nls")
public class DisconnectedServlet extends HttpServlet
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

            jg.writeArrayFieldStart(Messages.HTTP_DisconnectedTitle);

            final int group_count = model.getGroupCount();
            for (int i=0; i<group_count; ++i)
            {
                final ArchiveGroup group = model.getGroup(i);
                final int channel_count = group.getChannelCount();
                for (int j=0; j<channel_count; ++j)
                {
                    final ArchiveChannel channel = group.getChannel(j);
                    if (! channel.isConnected())
                    {
                        jg.writeStartObject();
                        jg.writeStringField(Messages.HTTP_Channel, channel.getName());
                        jg.writeStringField(Messages.HTTP_Group, group.getName());
                        jg.writeEndObject();
                    }
                }
            }

            jg.writeEndArray();

            json.close();
        }
        else
        {
            final HTMLWriter html = new HTMLWriter(response, "Disconnected Channels");

            html.openTable(1, "#", "Channel", "Group");

            final int group_count = model.getGroupCount();
            int disconnected = 0;
            for (int i=0; i<group_count; ++i)
            {
                final ArchiveGroup group = model.getGroup(i);
                final int channel_count = group.getChannelCount();
                for (int j=0; j<channel_count; ++j)
                {
                    final ArchiveChannel channel = group.getChannel(j);
                    if (channel.isConnected())
                        continue;
                    ++disconnected;
                    html.tableLine(
                        Integer.toString(disconnected),
                        HTMLWriter.makeLink("channel?name=" + channel.getName(), channel.getName()),
                        HTMLWriter.makeLink("group?name=" + group.getName(), group.getName())
                    );
                }
            }

            html.closeTable();

            if (disconnected == 0)
                html.h2("All channels are connected");

            html.close();
        }
    }
}
