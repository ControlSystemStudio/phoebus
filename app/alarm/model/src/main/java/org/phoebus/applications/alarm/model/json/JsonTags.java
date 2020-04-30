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
    public static final String ACTIONS = "actions";
    public static final String ANNUNCIATING = "annunciating";
    public static final String COMMAND = "command";
    public static final String COMMANDS = "commands";
    public static final String COUNT = "count";
    public static final String CURRENT_MESSAGE = "current_message";
    public static final String CURRENT_SEVERITY = "current_severity";
    public static final String DELAY = "delay";
    public static final String DELETE = "delete";
    public static final String DESCRIPTION = "description";
    public static final String DETAILS = "details";
    public static final String DISABLE_NOTIFY = "disable_notify";
    public static final String DISPLAYS = "displays";
    public static final String ENABLED = "enabled";
    public static final String ENABLE_NOTIFY = "enable_notify";
    public static final String FILTER = "filter";
    public static final String GUIDANCE = "guidance";
    public static final String HOST = "host";
    public static final String LATCH = "latch";
    public static final String LATCHING = "latching";
    public static final String MAINTENANCE = "maintenance";
    public static final String MESSAGE = "message";
    public static final String MODE = "mode";
    public static final String NANO = "nano";
    public static final String NORMAL = "normal";
    public static final String NOTIFY = "notify";
    public static final String SECONDS = "seconds";
    public static final String SEVERITY = "severity";
    public static final String STANDOUT = "standout";
    public static final String TALK = "talk";
    public static final String TIME = "time";
    public static final String TITLE = "title";
    public static final String USER = "user";
    public static final String VALUE = "value";
}
