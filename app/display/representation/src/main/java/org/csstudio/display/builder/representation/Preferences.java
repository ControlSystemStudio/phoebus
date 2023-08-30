/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** Preference settings
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    /** Preference setting */
    @Preference public static int performance_log_period_secs, performance_log_threshold_ms,
                      update_accumulation_time, update_delay, plot_update_delay, image_update_delay,
                      tooltip_length, embedded_timeout;

    static
    {
    	AnnotatedPreferences.initialize(Preferences.class, "/display_representation_preferences.properties");
    }
}
