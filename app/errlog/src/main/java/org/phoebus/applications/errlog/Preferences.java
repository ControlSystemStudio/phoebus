/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.errlog;

import org.phoebus.framework.preferences.PreferencesReader;

/** LineLog preference settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    public static int max_lines;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Preferences.class, "/errlog_preferences.properties");
        max_lines = prefs.getInt("max_lines");
    }
}
