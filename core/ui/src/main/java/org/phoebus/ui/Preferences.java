/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui;

import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.ui.application.TopResources;

/** Preference settings
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    public static final String TOP_RESOURCES = "top_resources";

    public static TopResources top_resources;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Preferences.class, "/phoebus_ui_preferences.properties");
        top_resources = TopResources.parse(prefs.get(TOP_RESOURCES));
    }
}
