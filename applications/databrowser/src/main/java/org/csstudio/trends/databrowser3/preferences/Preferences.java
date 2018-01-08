/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.preferences;

import org.csstudio.trends.databrowser3.Activator;
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
        USE_AUTO_SCALE = "use_auto_scale",
        USE_TRACE_NAMES = "use_trace_names",

        // Later...
        TIME_SPAN = "time_span",
        SCAN_PERIOD = "scan_period",
        BUFFER_SIZE = "live_buffer_size",
        UPDATE_PERIOD = "update_period",
        LINE_WIDTH = "line_width",
        OPACITY = "opacity",
        TRACE_TYPE = "trace_type",
        ARCHIVE_FETCH_DELAY = "archive_fetch_delay",
        PLOT_BINS = "plot_bins",
        URLS = "urls",
        ARCHIVES = "archives",
        USE_DEFAULT_ARCHIVES = "use_default_archives",
        PROMPT_FOR_ERRORS = "prompt_for_errors",
        ARCHIVE_RESCALE = "archive_rescale",
        TIME_SPAN_SHORTCUTS = "time_span_shortcuts",
        EMAIL_DEFAULT_SENDER = "email_default_sender",
        AUTOMATIC_HISTORY_REFRESH = "automatic_history_refresh",
        SCROLL_STEP = "scroll_step"
        ;


    public static boolean use_auto_scale;
    public static boolean use_trace_names;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Activator.class, "databrowser_preferences.properties");
        use_auto_scale = prefs.getBoolean(USE_AUTO_SCALE);
        use_trace_names = prefs.getBoolean(USE_TRACE_NAMES);
    }
}
