/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.email;

import org.phoebus.framework.preferences.PreferencesReader;

/** Preference settings for email
 *  @author Kunal Shroff
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EmailPreferences
{
    public static final String mailhost;
    public static final int mailport;
    public static final String username;
    public static final String password;
    public static final String from;

    /** @return Is email supported? */
    public static final boolean isEmailSupported()
    {
        // Allow DISABLE, DISABLED, Disabled, .. to disable
        return ! mailhost.toUpperCase().startsWith("DISABLE");
    }

    static
    {
        final PreferencesReader prefs = new PreferencesReader(EmailPreferences.class, "/email_preferences.properties");
        mailhost = prefs.get("mailhost");
        mailport = prefs.getInt("mailport");
        username = prefs.get("username");
        password = prefs.get("password");
        from = prefs.get("from");
    }
}
