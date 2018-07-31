/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.server;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.csstudio.archive.Engine;
import org.csstudio.archive.Preferences;
import org.csstudio.archive.engine.model.ArchiveGroup;
import org.csstudio.archive.engine.model.EngineModel;
import org.csstudio.archive.engine.model.SampleBuffer;
import org.csstudio.archive.writer.rdb.TimestampHelper;
import org.phoebus.util.time.SecondsParser;
import org.phoebus.util.time.TimeDuration;

/** 'main' web page
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MainServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    /** Bytes in a MegaByte */
    protected final static double MB = 1024.0*1024.0;

    @Override
    protected void doGet(final HttpServletRequest request,
                         final HttpServletResponse response) throws ServletException, IOException
    {
        final EngineModel model = Engine.getModel();
        final int group_count = model.getGroupCount();
        int connect_count = 0;
        int totalChannelCount = 0;
        for (int i=0; i<group_count; ++i)
        {
            final ArchiveGroup group = model.getGroup(i);
            final int channel_count = group.getChannelCount();
            for (int j=0; j<channel_count; ++j)
            {
                if (group.getChannel(j).isConnected())
                    ++connect_count;
            }
            totalChannelCount += channel_count;
        }
        int disconnectCount = totalChannelCount - connect_count;

        final HTMLWriter html = new HTMLWriter(response, "Archive Engine");

        html.openTable(2, "Summary");
        html.tableLine("Version", Engine.VERSION);
        html.tableLine("Description", model.getName());
        html.tableLine("State", model.getState().name());

        final Instant start = model.getStartTime();
        if (start != null)
        {
            html.tableLine("Start Time", TimestampHelper.format(start));
            final double up_secs =
                    TimeDuration.toSecondsDouble(Duration.between(start, Instant.now()));
            html.tableLine("Uptime", SecondsParser.formatSeconds(up_secs));
        }

        html.tableLine("Groups", Integer.toString(group_count));
        html.tableLine("Channels", Integer.toString(totalChannelCount));
        if (disconnectCount > 0)
            html.tableLine("Disconnected", HTMLWriter.makeRedText(Integer.toString(disconnectCount)));

        html.tableLine("Batch Size", Preferences.batch_size + " samples");
        html.tableLine("Write Period", Preferences.write_period + " sec");

        html.tableLine("Write State", (SampleBuffer.isInErrorState()
                ? HTMLWriter.makeRedText("Write Error")
                : "OK"));

        final Instant last_write_time = model.getLastWriteTime();
        html.tableLine("Last Written", last_write_time == null ? "Never" : TimestampHelper.format(last_write_time));
        html.tableLine("Write Count", model.getWriteCount() + " samples");
        html.tableLine("Write Duration", String.format("%.1f sec", model.getWriteDuration()));

        html.tableLine("Idle Time", String.format("%.1f %%", model.getIdlePercentage()));

        final Runtime runtime = Runtime.getRuntime();
        final double used_mem = runtime.totalMemory() / MB;
        final double max_mem = runtime.maxMemory() / MB;
        final double perc_mem = max_mem > 0 ?
                     used_mem / max_mem * 100.0 : 0.0;
        html.tableLine("Memory", String.format("%.1f MB of %.1f MB used (%.1f %%)", used_mem, max_mem, perc_mem));

        html.closeTable();

        html.close();
    }
}
