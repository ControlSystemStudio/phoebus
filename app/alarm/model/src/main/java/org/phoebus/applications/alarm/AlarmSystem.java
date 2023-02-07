/*******************************************************************************
 * Copyright (c) 2018-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.logging.Level;

import org.phoebus.applications.alarm.client.IdentificationHelper;
import org.phoebus.framework.macros.MacroOrSystemProvider;
import org.phoebus.framework.macros.MacroValueProvider;
import org.phoebus.framework.macros.Macros;
import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.util.time.SecondsParser;
import org.phoebus.util.time.TimeParser;

/** Common alarm system code
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmSystem extends AlarmSystemConstants
{
    /** Kafka Server host:port */
    @Preference public static String server;

    /** Kafka settings file */
    @Preference public static String kafka_properties;

    /** Name of alarm tree root
     *
     *  <p>Default name from preferences.
     *  UI instances may select different one at runtime,
     *  but this remains unchanged.
     */
    @Preference public static String config_name;

    /** Names of selectable alarm configurations */
    @Preference public static String[] config_names;

    /** Timeout in seconds for initial PV connection */
    @Preference public static int connection_timeout;

    /** Item level of alarm area. A level of 2 would show all the root levels children. */
    @Preference public static int alarm_area_level;

    /** Number of columns in the alarm area */
    @Preference public static int alarm_area_column_count;

    /** Gap between alarm area panel items */
    @Preference public static int alarm_area_gap;

    /** Font size for the alarm area view */
    @Preference public static int alarm_area_font_size;

    /** Limit for the number of context menu items */
    @Preference public static int alarm_menu_max_items;

    /** Initial Alarm Tree UI update delay [ms] */
    @Preference public static int alarm_tree_startup_ms;

    /** Alarm table columns */
    @Preference public static String[] alarm_table_columns;

    /** Use text(!) color for background to indicate alarm severity,
     *  instead of the common alarm severity text and background colors?
     */
    @Preference public static boolean alarm_table_color_legacy_background;

    /** Alarm table row limit */
    @Preference public static int alarm_table_max_rows;

    /** Directory used for executing commands */
    @Preference public static File command_directory;

    /** Annunciator threshold */
    @Preference public static int annunciator_threshold;

    /** Annunciator message retention count */
    @Preference public static int annunciator_retention_count;

    /** Timeout in milliseconds at which server sends idle state updates
     *  for the 'root' element if there's no real traffic
     */
    public static final long idle_timeout_ms;

    /** Name of the sender, the 'from' field of automated email actions */
    @Preference  public static String automated_email_sender;

    /** Automated actions that request follow-up when alarm no longer active */
    @Preference public static String[] automated_action_followup;

    /** Optional heartbeat PV */
    @Preference public static String heartbeat_pv;

    /** Heartbeat PV period in milliseconds */
    public static final long heartbeat_ms;

    /** Nag period in milliseconds */
    public static final long nag_period_ms;

    /** Connection validation period in seconds */
    @Preference public static long connection_check_secs;

    /** Disable notify feature */
    @Preference public static boolean disable_notify_visible;

    /** "Disable until.." shortcuts */
    @Preference public static String[] shelving_options;

    /** Macros used in UI display/command/web links */
    public static MacroValueProvider macros;

    static
    {
    	final PreferencesReader prefs = AnnotatedPreferences.initialize(AlarmSystem.class, "/alarm_preferences.properties");
        idle_timeout_ms = prefs.getInt("idle_timeout") * 1000L;
        heartbeat_ms = prefs.getInt("heartbeat_secs") * 1000L;

        double secs = 0.0;
        try
        {
            secs = SecondsParser.parseSeconds(prefs.get("nag_period"));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Invalid nag_period " + prefs.get("nag_period"), ex);
        }
        nag_period_ms = Math.round(Math.max(0, secs) * 1000.0);

        // Check if the provided options can be parsed to avoid later runtime errors
        final LocalDateTime now = LocalDateTime.now();
        for (String option : shelving_options)
        {
            try
            {
                final TemporalAmount amount = TimeParser.parseTemporalAmount(option);
                final LocalDateTime end = now.plus(amount);
                if (! end.isAfter(now))
                    throw new Exception("Invalid 'shelving_options' value '" + option + "'");
            }
            catch (Throwable ex)
            {
                logger.log(Level.WARNING, "Error in 'shelving_options'", ex);
            }
        }

        try
        {
            macros = new MacroOrSystemProvider(Macros.fromSimpleSpec(prefs.get("macros")));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Invalid macros '" + prefs.get("macros") + "'", ex);
            macros = new MacroOrSystemProvider(new Macros());
        }

        IdentificationHelper.initialize();
    }
}
