/*******************************************************************************
 * Copyright (c) 2010-2022 Oak Ridge National Laboratory.
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
import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
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
    final private static String
        PROMPT_FOR_RAW_DATA = "prompt_for_raw_data_request",
	    PROMPT_FOR_VISIBILITY = "prompt_for_visibility";

    /** Predefined time range */
    public static class TimePreset
    {
        /** Label */
        public final String label;
        /** Time range */
        public final TimeRelativeInterval range;

        TimePreset(final String label, final TimeRelativeInterval range)
        {
            this.label = label;
            this.range = range;
        }
    }

    /** Setting */
    @Preference public static int archive_fetch_delay;
    /** Setting */
    @Preference public static int concurrent_requests;
    /** Setting */
    @Preference public static ArchiveRescale archive_rescale;
    /** Setting */
    public static List<ArchiveDataSource> archive_urls;
    /** Setting */
    public static List<ArchiveDataSource> archives;
    /** Setting */
    @Preference public static boolean automatic_history_refresh;
    /** Setting */
    @Preference public static int live_buffer_size;
    /** Setting */
    @Preference public static int line_width;
    /** Setting */
    @Preference public static int opacity;
    /** Setting */
    @Preference public static int plot_bins;
    /** Setting */
    @Preference public static double scan_period;
    /** Setting */
    public static Duration scroll_step;
    /** Setting */
    public static Duration time_span;
    /** Setting */
    @Preference public static TraceType trace_type;
    /** Setting */
    @Preference public static double update_period;
    /** Setting */
    @Preference public static boolean use_auto_scale;
    /** Setting */
    @Preference public static boolean use_default_archives;
    /** Setting */
    @Preference public static boolean drop_failed_archives;
    /** Setting */
    @Deprecated
    @Preference public static String[]  equivalent_pv_prefixes;
    /** Setting */
    @Preference public static boolean use_trace_names;
    /** Setting */
    @Preference public static boolean prompt_for_raw_data_request;
    /** Setting */
    @Preference public static boolean prompt_for_visibility;
    /** Setting */
    public static final List<TimePreset> time_presets = new ArrayList<>();
    /** Setting */
    @Preference public static boolean config_dialog_supported;

    @Preference
    public static boolean assign_pvs_from_clipboard_to_the_same_axis_by_default;


    @Preference
    public static String value_axis_label_policy;


    static
    {
        final PreferencesReader prefs = AnnotatedPreferences.initialize(Activator.class, Preferences.class, "/databrowser_preferences.properties");

        // Allow at least one at a time
        if (concurrent_requests < 1)
        	concurrent_requests = 1;

        archive_urls = parseArchives(prefs.get("urls"));
        archives = parseArchives(prefs.get("archives"));

        scroll_step = Duration.ofSeconds( Math.max(1, prefs.getInt("scroll_step")) );
        time_span = Duration.ofSeconds( Math.round( Math.max(prefs.getDouble("time_span"), 1.0) ) );

        for (String preset : prefs.get("time_span_shortcuts").split("\\|"))
        {
            final String[] label_span = preset.split(",");
            time_presets.add(new TimePreset(label_span[0], TimeRelativeInterval.startsAt(TimeParser.parseTemporalAmount(label_span[1]))));
        }
    }

    /** @param show Display warning regarding raw data? */
    public static void setRawDataPrompt(final boolean show)
    {
        prompt_for_raw_data_request = show;
        update(PROMPT_FOR_RAW_DATA, show);
    }

    /** @param show Display warning regarding trace visiblity? */
    public static void setVisibilityPrompt(final boolean show)
    {
        prompt_for_visibility = show;
        update(PROMPT_FOR_VISIBILITY, show);
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

    /** @param setting Text with list of archives, separated by 'pipe' symbol
     *  @return List of parsed data sources
     */
    public static List<ArchiveDataSource> parseArchives(final String setting)
    {
        final List<ArchiveDataSource> urls = new ArrayList<>();
        //When settings is empty do not search for a archive datasource
        if (setting != null && !setting.trim().isEmpty()) {
            for (String fragment : setting.split("\\*")) {
                final String[] strs = fragment.split("\\|");
                if (strs.length == 1)
                    urls.add(new ArchiveDataSource(strs[0], strs[0]));
                else if (strs.length >= 2)
                    urls.add(new ArchiveDataSource(strs[0], strs[1]));
            }
        }
        return urls;
    }
}
