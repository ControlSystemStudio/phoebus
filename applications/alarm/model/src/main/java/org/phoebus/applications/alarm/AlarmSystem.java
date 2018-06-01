/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import java.util.logging.Logger;

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

    /** Kafka Server host:port */
    public static final String server;

    /** Name of alarm tree root */
    public static final String config_name;

    /** Timeout in seconds for initial PV connection */
    public static final int connection_timeout;

    /** Number of columns in the alarm area */
    public static final int area_column_count;

    /** Item level of alarm area. A level of 2 would show all the root levels children. */
    public static final int alarm_area_level;
    static
    {
        final PreferencesReader prefs = new PreferencesReader(AlarmSystem.class, "/alarm_preferences.properties");
        server = prefs.get("server");
        config_name = prefs.get("config_name");
        connection_timeout = prefs.getInt("connection_timeout");
        area_column_count = prefs.getInt("area_column_count");
        alarm_area_level = prefs.getInt("alarm_area_level");
    }
}
