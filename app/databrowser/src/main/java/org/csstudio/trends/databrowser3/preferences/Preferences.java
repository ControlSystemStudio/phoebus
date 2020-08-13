/*******************************************************************************
 * Copyright (c) 2010-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.preferences;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.ArchiveRescale;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;

/** Helper for reading preference settings
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    /** Preference tags.
     *  For explanation of the settings see preferences.ini
     */
    final public static String
        ARCHIVE_FETCH_DELAY = "archive_fetch_delay",
        CONCURRENT_REQUESTS = "concurrent_requests",
        ARCHIVE_RESCALE = "archive_rescale",
        ARCHIVES = "archives",
        URLS = "urls",
        AUTOMATIC_HISTORY_REFRESH = "automatic_history_refresh",
        BUFFER_SIZE = "live_buffer_size",
        LINE_WIDTH = "line_width",
        OPACITY = "opacity",
        PLOT_BINS = "plot_bins",
        SCAN_PERIOD = "scan_period",
        SCROLL_STEP = "scroll_step",
        TIME_SPAN = "time_span",
        TRACE_TYPE = "trace_type",
        UPDATE_PERIOD = "update_period",
        USE_AUTO_SCALE = "use_auto_scale",
        USE_DEFAULT_ARCHIVES = "use_default_archives",
        DROP_FAILED_ARCHIVES = "drop_failed_archives",
        USE_TRACE_NAMES = "use_trace_names",
        PROMPT_FOR_RAW_DATA = "prompt_for_raw_data_request",
        PROMPT_FOR_VISIBILITY = "prompt_for_visibility",
        TIME_SPAN_SHORTCUTS = "time_span_shortcuts";

    public static class TimePreset
    {
        public final String label;
        public final TimeRelativeInterval range;

        public TimePreset(final String label, final TimeRelativeInterval range)
        {
            this.label = label;
            this.range = range;
        }
    }

    public static int archive_fetch_delay;
    public static int concurrent_requests;
    public static ArchiveRescale archive_rescale = ArchiveRescale.STAGGER;
    public static List<ArchiveDataSource> archive_urls;
    public static List<ArchiveDataSource> archives;
    public static boolean automatic_history_refresh;
    public static int buffer_size;
    public static int line_width;
    public static int opacity;
    public static int plot_bins;
    public static double scan_period;
    public static Duration scroll_step;
    public static Duration time_span;
    public static TraceType trace_type = TraceType.AREA;
    public static double update_period;
    public static boolean use_auto_scale;
    public static boolean use_default_archives;
    public static boolean drop_failed_archives;
    public static boolean use_trace_names;
    public static boolean prompt_for_raw_data_request;
    public static boolean prompt_for_visibility;
    public static List<TimePreset> time_presets;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Activator.class, "/databrowser_preferences.properties");

        archive_fetch_delay = prefs.getInt(ARCHIVE_FETCH_DELAY);

        // Allow at least one at a time
        concurrent_requests = Math.max(1, prefs.getInt(CONCURRENT_REQUESTS));

        String enum_name = prefs.get(ARCHIVE_RESCALE);
        try
        {
            archive_rescale = ArchiveRescale.valueOf(enum_name);
        }
        catch (Exception ex)
        {
            Activator.logger.log(Level.WARNING, "Undefined rescale option '" + enum_name + "'", ex);
        }

        archive_urls = parseArchives(prefs.get(URLS));
        archives = parseArchives(prefs.get(ARCHIVES));

        automatic_history_refresh = prefs.getBoolean(AUTOMATIC_HISTORY_REFRESH);
        buffer_size = prefs.getInt(BUFFER_SIZE);
        line_width = prefs.getInt(LINE_WIDTH);
        opacity = prefs.getInt(OPACITY);
        plot_bins = prefs.getInt(PLOT_BINS);
        scan_period = prefs.getDouble(SCAN_PERIOD);
        scroll_step = Duration.ofSeconds( Math.max(1, prefs.getInt(SCROLL_STEP)) );
        time_span = Duration.ofSeconds( Math.round( Math.max(prefs.getDouble(TIME_SPAN), 1.0) ) );

        enum_name = prefs.get(TRACE_TYPE);
        try
        {
            trace_type = TraceType.valueOf(enum_name);
        }
        catch (Exception ex)
        {
            Activator.logger.log(Level.WARNING, "Undefined trace type option '" + enum_name + "'", ex);
        }

        update_period = prefs.getDouble(UPDATE_PERIOD);
        use_auto_scale = prefs.getBoolean(USE_AUTO_SCALE);
        use_default_archives = prefs.getBoolean(USE_DEFAULT_ARCHIVES);
        drop_failed_archives = prefs.getBoolean(DROP_FAILED_ARCHIVES);
        use_trace_names = prefs.getBoolean(USE_TRACE_NAMES);

        prompt_for_raw_data_request = prefs.getBoolean(PROMPT_FOR_RAW_DATA);
        prompt_for_visibility = prefs.getBoolean(PROMPT_FOR_VISIBILITY);


        time_presets = new ArrayList<>();
        for (String preset : prefs.get(TIME_SPAN_SHORTCUTS).split("\\|"))
        {
            final String[] label_span = preset.split(",");
            time_presets.add(new TimePreset(label_span[0], TimeRelativeInterval.startsAt(TimeParser.parseTemporalAmount(label_span[1]))));
        }
    }

    public static void setRawDataPrompt(final boolean value)
    {
        prompt_for_raw_data_request = value;
        update(PROMPT_FOR_RAW_DATA, value);
    }

    public static void setVisibilityPrompt(final boolean value)
    {
        prompt_for_visibility = value;
        update(PROMPT_FOR_VISIBILITY, value);
    }


    private static void update(final String setting, final boolean value)
    {
        final java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(Activator.class);
        prefs.putBoolean(setting, value);
        try
        {
            prefs.flush();
        }
        catch (Exception ex)
        {
            Activator.logger.log(Level.WARNING, "Cannot write preferences", ex);
        }
    }

    public static List<ArchiveDataSource> parseArchives(final String setting)
    {
        final List<ArchiveDataSource> urls = new ArrayList<>();
        for (String fragment : setting.split("\\*"))
        {
            final String[] strs = fragment.split("\\|");
            if (strs.length == 1)
                urls.add(new ArchiveDataSource(strs[0], strs[0]));
            else if (strs.length >= 2)
                urls.add(new ArchiveDataSource(strs[0], strs[1]));
        }
        return urls;
    }
}
