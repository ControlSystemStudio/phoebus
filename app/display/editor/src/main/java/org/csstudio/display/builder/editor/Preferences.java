/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import java.util.HashSet;
import java.util.Set;

import org.phoebus.framework.preferences.PreferencesReader;

/** Editor preferences
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    public static final String HIDDEN_WIDGETS = "hidden_widget_types";
    public static final String NEW_DISPLAY_TEMPLATE = "new_display_template";
    
    public static Set<String> hidden_widget_types = new HashSet<>();
    public static String new_display_template;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(Preferences.class, "/display_editor_preferences.properties");

        final String list = prefs.get(HIDDEN_WIDGETS);
        for (String item : list.split(" *, *"))
        {
            final String type = item.trim();
            if (! type.isEmpty())
                hidden_widget_types.add(type);
        }
        new_display_template = prefs.get(NEW_DISPLAY_TEMPLATE);
    }
}
