/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.scan.client;

import org.phoebus.framework.preferences.PreferencesReader;

/** Preference settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    public static String host;
    public static int port;
    public static int poll_period;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Preferences.class, "/scan_client_preferences.properties");
        host = prefs.get("host");
        port = prefs.getInt("port");
        poll_period = prefs.getInt("poll_period");
    }
}
