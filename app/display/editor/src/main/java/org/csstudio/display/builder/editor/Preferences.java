/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor;

import java.util.HashSet;
import java.util.Set;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** Editor preferences
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
	@Preference(name="hidden_widget_types") private static String[] hidden_widget_spec;
	/** Setting */
    public static Set<String> hidden_widget_types = new HashSet<>();
    /** Setting */
    @Preference public static String new_display_template;
    /** Setting */
    @Preference public static int undo_stack_size;

    static
    {
    	AnnotatedPreferences.initialize(Preferences.class, "/display_editor_preferences.properties");

        for (String item : hidden_widget_spec)
            hidden_widget_types.add(item);
    }
}
