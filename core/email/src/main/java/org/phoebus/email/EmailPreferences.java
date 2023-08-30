/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.email;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** Preference settings for email
 *  @author Kunal Shroff
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class EmailPreferences
{
    @Preference public static String mailhost;
    @Preference public static int mailport;
    @Preference public static String username;
    @Preference public static String password;
    @Preference public static String from;

    /** @return Is email supported? */
    public static final boolean isEmailSupported()
    {
        // Allow DISABLE, DISABLED, Disabled, .. to disable
        return ! mailhost.toUpperCase().startsWith("DISABLE");
    }

    static
    {
    	AnnotatedPreferences.initialize(EmailPreferences.class, "/email_preferences.properties");
    }
}
