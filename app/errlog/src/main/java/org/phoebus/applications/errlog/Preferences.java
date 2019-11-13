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
    static final String font_name;
    static final int font_size;
    public static int max_lines;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Preferences.class, "/errlog_preferences.properties");
        font_name = prefs.get("font_name");
        font_size = prefs.getInt("font_size");
        max_lines = prefs.getInt("max_lines");
    }
}
