/*******************************************************************************
 * Copyright (c) 2019-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** Preference settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class JFXPreferences
{
    /** Type of slider to create */
    @Preference public static boolean inc_dec_slider;
    /** Tooltip delay */
    @Preference public static int tooltip_delay_ms;
    /** Tooltip duration */
    @Preference public static int tooltip_display_sec;
    /** make the transparent parts of symbols clickable */
    @Preference public static boolean pick_on_bounds;

    static
    {
    	AnnotatedPreferences.initialize(JFXPreferences.class, "/jfx_repr_preferences.properties");
    }
}
