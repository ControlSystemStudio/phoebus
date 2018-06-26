/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import java.io.File;
import java.util.logging.Logger;

import org.phoebus.applications.alarm.client.IdentificationHelper;
import org.phoebus.framework.preferences.PreferencesReader;

/** Common alarm system code
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmSystem
{
    /** Alarm system logger */
    public static final Logger logger = Logger.getLogger(AlarmSystem.class.getPackageName());

    /** Suffix for the topic where alarm server reports alarm state */
    public static final String STATE_TOPIC_SUFFIX = "State";

    /** Suffix for the topic that clients use to send commands to alarm server */
    public static final String COMMAND_TOPIC_SUFFIX = "Command";

    /** Suffix for the topic that server uses to send annunciations */
    public static final String TALK_TOPIC_SUFFIX = "Talk";

    /** Suffix for the topic that contains non compacted aggregate of other topics. */
    public static final String LONG_TERM_TOPIC_SUFFIX = "LongTerm";

    /** Kafka Server host:port */
    public static final String server;

    /** Name of alarm tree root */
    public static final String config_name;

    /** Timeout in seconds for initial PV connection */
    public static final int connection_timeout;

    /** Item level of alarm area. A level of 2 would show all the root levels children. */
    public static final int alarm_area_level;

    /** Number of columns in the alarm area */
    public static final int alarm_area_column_count;

    /** Gap between alarm area panel items */
    public static final int alarm_area_gap;

    /** Font size for the alarm area view */
    public static final int alarm_area_font_size;

    /** Limit for the number of context menu items */
    public static final int alarm_menu_max_items;

    /** Alarm table row limit */
    public static final int alarm_table_max_rows;

    /** Directory used for executing commands */
    public static final File command_directory;

    /** Annunciator threshold */
    public static final int annunciator_threshold;

    /** Annunciator message retention count */
    public static final int annunciator_retention_count;

    /** Name of the sender, the 'from' field of automated email actions */
    public static final String automated_email_sender;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(AlarmSystem.class, "/alarm_preferences.properties");
        server = prefs.get("server");
        config_name = prefs.get("config_name");
        connection_timeout = prefs.getInt("connection_timeout");
        alarm_area_level = prefs.getInt("alarm_area_level");
        alarm_area_column_count = prefs.getInt("alarm_area_column_count");
        alarm_area_gap = prefs.getInt("alarm_area_gap");
        alarm_area_font_size = prefs.getInt("alarm_area_font_size");
        alarm_menu_max_items = prefs.getInt("alarm_menu_max_items");
        alarm_table_max_rows = prefs.getInt("alarm_table_max_rows");
        command_directory = new File(PreferencesReader.replaceProperties(prefs.get("command_directory")));
        annunciator_threshold = prefs.getInt("annunciator_threshold");
        annunciator_retention_count = prefs.getInt("annunciator_retention_count");
        automated_email_sender = prefs.get("automated_email_sender");

        IdentificationHelper.initialize();
    }
}
