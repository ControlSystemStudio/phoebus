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

import com.fasterxml.jackson.core.JsonGenerator;

/** 'main' web page
 *  @author Kay Kasemir
 *  @author Dominic Oram JSON support in previous version
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

        // Determine statistics
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
        final int disconnectCount = totalChannelCount - connect_count;
        final Instant start = model.getStartTime();
        final Instant last_write_time = model.getLastWriteTime();

        if ("json".equals(request.getParameter("format")))
        {
            final JSONWriter json = new JSONWriter(request, response);
            final JsonGenerator jg = json.getGenerator();

            jg.writeStringField(Messages.HTTP_Version, Engine.VERSION);
            jg.writeStringField(Messages.HTTP_Description, model.getName());
            jg.writeStringField(Messages.HTTP_State, model.getState().name());

            if (start != null)
            {
                jg.writeStringField(Messages.HTTP_StartTime, TimestampHelper.format(start));
                final double up_secs =
                        TimeDuration.toSecondsDouble(Duration.between(start, Instant.now()));
                jg.writeStringField(Messages.HTTP_Uptime, SecondsParser.formatSeconds(up_secs));
            }

            jg.writeNumberField(Messages.HTTP_GroupCount, group_count);
            jg.writeNumberField(Messages.HTTP_ChannelCount, totalChannelCount);
            jg.writeNumberField(Messages.HTTP_Disconnected, disconnectCount);
            jg.writeNumberField(Messages.HTTP_BatchSize, Preferences.batch_size);
            jg.writeNumberField(Messages.HTTP_WritePeriod, Preferences.write_period);

            jg.writeStringField(Messages.HTTP_WriteState, (SampleBuffer.isInErrorState()
                    ? Messages.HTTP_WriteError : "OK"));

            jg.writeStringField(Messages.HTTP_LastWriteTime, last_write_time == null ? "Never" : TimestampHelper.format(last_write_time));
            jg.writeNumberField(Messages.HTTP_WriteCount, model.getWriteCount());
            jg.writeNumberField(Messages.HTTP_WriteDuration, model.getWriteDuration());
            jg.writeNumberField(Messages.HTTP_Idletime, model.getIdlePercentage());

            final Runtime runtime = Runtime.getRuntime();
            final double used_mem = runtime.totalMemory() / MB;
            final double max_mem = runtime.maxMemory() / MB;
            final double perc_mem = max_mem > 0 ?
                         used_mem / max_mem * 100.0 : 0.0;

            jg.writeNumberField("Used Memory", used_mem);
            jg.writeNumberField("Max Memory", max_mem);
            jg.writeNumberField("Percentage Memory", perc_mem);

            json.close();
        }
        else
        {
            final HTMLWriter html = new HTMLWriter(response, Messages.HTTP_MainTitle);

            html.openTable(2, "Summary");
            html.tableLine(Messages.HTTP_Version, Engine.VERSION);
            html.tableLine(Messages.HTTP_Description, model.getName());
            html.tableLine(Messages.HTTP_State, model.getState().name());

            if (start != null)
            {
                html.tableLine(Messages.HTTP_StartTime, TimestampHelper.format(start));
                final double up_secs =
                        TimeDuration.toSecondsDouble(Duration.between(start, Instant.now()));
                html.tableLine(Messages.HTTP_Uptime, SecondsParser.formatSeconds(up_secs));
            }

            html.tableLine(Messages.HTTP_GroupCount, Integer.toString(group_count));
            html.tableLine(Messages.HTTP_ChannelCount, Integer.toString(totalChannelCount));
            if (disconnectCount > 0)
                html.tableLine(Messages.HTTP_Disconnected, HTMLWriter.makeRedText(Integer.toString(disconnectCount)));

            html.tableLine(Messages.HTTP_BatchSize, Preferences.batch_size + " samples");
            html.tableLine(Messages.HTTP_WritePeriod, Preferences.write_period + " sec");

            html.tableLine(Messages.HTTP_WriteState, (SampleBuffer.isInErrorState()
                    ? HTMLWriter.makeRedText(Messages.HTTP_WriteError)
                    : "OK"));

            html.tableLine(Messages.HTTP_LastWriteTime, last_write_time == null ? "Never" : TimestampHelper.format(last_write_time));
            html.tableLine(Messages.HTTP_WriteCount, (int) model.getWriteCount() + " samples");
            html.tableLine(Messages.HTTP_WriteDuration, String.format("%.1f sec", model.getWriteDuration()));

            html.tableLine(Messages.HTTP_Idletime, String.format("%.1f %%", model.getIdlePercentage()));

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
}
