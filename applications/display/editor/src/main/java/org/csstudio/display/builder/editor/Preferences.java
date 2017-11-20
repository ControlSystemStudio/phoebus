/*******************************************************************************
 * Copyright (c) 2015 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;

/** Editor preferences
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    public static Set<String> getHiddenWidgets()
    {
        final Set<String> deprecated = new HashSet<>();
        final IPreferencesService prefs = Platform.getPreferencesService();
        if (prefs != null)
        {
            final String list = prefs.getString(Plugin.ID, "hidden_widget_types", "", null);
            for (String item : list.split(" *, *"))
            {
                final String type = item.trim();
                if (! type.isEmpty())
                    deprecated.add(type);
            }
        }
        return deprecated;
    }
}
