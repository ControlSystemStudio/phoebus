/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.logging.Level;

import org.phoebus.framework.preferences.PreferencesReader;

/** Preference settings
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    public static int performance_log_period_secs, performance_log_threshold_ms,
                      update_accumulation_time, update_delay, plot_update_delay, image_update_delay,
                      tooltip_length, embedded_timeout;

    static
    {
        try
        {
            PreferencesReader prefs = new PreferencesReader(Preferences.class, "/display_representation_preferences.properties");
            performance_log_period_secs = prefs.getInt("performance_log_period_secs");
            performance_log_threshold_ms = prefs.getInt("performance_log_threshold_ms");
            update_accumulation_time = prefs.getInt("update_accumulation_time");
            update_delay = prefs.getInt("update_delay");
            plot_update_delay = prefs.getInt("plot_update_delay");
            image_update_delay = prefs.getInt("image_update_delay");
            tooltip_length = prefs.getInt("tooltip_length");
            embedded_timeout = prefs.getInt("embedded_timeout");
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Display representation preference error", ex);
        }
    }
}
