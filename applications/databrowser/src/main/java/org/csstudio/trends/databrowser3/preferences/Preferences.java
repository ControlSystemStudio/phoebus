/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.preferences;

import java.time.Duration;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.phoebus.framework.preferences.PreferencesReader;

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
        AUTOMATIC_HISTORY_REFRESH = "automatic_history_refresh",
        BUFFER_SIZE = "live_buffer_size",
        LINE_WIDTH = "line_width",
        TIME_SPAN = "time_span",
        TRACE_TYPE = "trace_type",
        USE_AUTO_SCALE = "use_auto_scale",
        USE_DEFAULT_ARCHIVES = "use_default_archives",
        USE_TRACE_NAMES = "use_trace_names",

        // Later...
        SCAN_PERIOD = "scan_period",
        UPDATE_PERIOD = "update_period",
        OPACITY = "opacity",
        ARCHIVE_FETCH_DELAY = "archive_fetch_delay",
        PLOT_BINS = "plot_bins",
        URLS = "urls",
        ARCHIVES = "archives",
        PROMPT_FOR_ERRORS = "prompt_for_errors",
        ARCHIVE_RESCALE = "archive_rescale",
        TIME_SPAN_SHORTCUTS = "time_span_shortcuts",
        EMAIL_DEFAULT_SENDER = "email_default_sender",
        SCROLL_STEP = "scroll_step"
        ;


    public static List<ArchiveDataSource> archives;
    public static boolean automatic_history_refresh;
    public static int buffer_size;
    public static int line_width;
    public static Duration time_span;
    public static TraceType trace_type;
    public static boolean use_auto_scale;
    public static boolean use_default_archives;
    public static boolean use_trace_names;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Activator.class, "databrowser_preferences.properties");

        // TODO Read archives
        archives = List.of();

        automatic_history_refresh = prefs.getBoolean(AUTOMATIC_HISTORY_REFRESH);
        buffer_size = prefs.getInt(BUFFER_SIZE);
        line_width = prefs.getInt(LINE_WIDTH);
        time_span = Duration.ofSeconds( Math.round( Math.max(prefs.getDouble(TIME_SPAN), 1.0) ) );

        final String type_name = prefs.get(TRACE_TYPE);
        try
        {
            trace_type = TraceType.valueOf(type_name);
        }
        catch (Exception ex)
        {
            Activator.logger.log(Level.WARNING, "Undefined trace type option '" + type_name + "'", ex);
        }

        use_auto_scale = prefs.getBoolean(USE_AUTO_SCALE);
        use_default_archives = prefs.getBoolean(USE_DEFAULT_ARCHIVES);
        use_trace_names = prefs.getBoolean(USE_TRACE_NAMES);
    }
}
