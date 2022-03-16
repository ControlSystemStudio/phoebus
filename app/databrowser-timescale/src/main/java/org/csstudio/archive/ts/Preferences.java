/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * are made available under the terms of the Eclipse Public License v1.0
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.ts;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** TimestampDB archive settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    @Preference
    public static String user, password;

    @Preference
    public static int timeout_secs, fetch_size;

    static
    {
        AnnotatedPreferences.initialize(Preferences.class, "/archive_ts_preferences.properties");
    }
}
