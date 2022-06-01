/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model.json;

/** JSON tags
 *
 *  <p>These constants are used to assert that the
 *  JSON messages do not change, because clients
 *  beyond just this code might monitor the alarm traffic.
 *
 *  <p>For that reason, instead of simply serializing the Java 'Instant'
 *  with 'epocSeconds', we use a more generic 'seconds' tag.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public interface JsonTags
{
    /** JSON tags */
    public static final String ACTIONS = "actions",
                               ANNUNCIATING = "annunciating",
                               COMMAND = "command",
                               COMMANDS = "commands",
                               COUNT = "count",
                               CURRENT_MESSAGE = "current_message",
                               CURRENT_SEVERITY = "current_severity",
                               DELAY = "delay",
                               DELETE = "delete",
                               DESCRIPTION = "description",
                               DETAILS = "details",
                               DISABLE_NOTIFY = "disable_notify",
                               DISPLAYS = "displays",
                               ENABLED = "enabled",
                               ENABLE_NOTIFY = "enable_notify",
                               FILTER = "filter",
                               GUIDANCE = "guidance",
                               HOST = "host",
                               LATCH = "latch",
                               LATCHING = "latching",
                               MAINTENANCE = "maintenance",
                               MESSAGE = "message",
                               MODE = "mode",
                               NANO = "nano",
                               NORMAL = "normal",
                               NOTIFY = "notify",
                               SECONDS = "seconds",
                               SEVERITY = "severity",
                               STANDOUT = "standout",
                               TALK = "talk",
                               TIME = "time",
                               TITLE = "title",
                               USER = "user",
                               VALUE = "value";
}
