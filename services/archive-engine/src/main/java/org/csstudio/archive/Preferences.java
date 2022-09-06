/*******************************************************************************
 * Copyright (c) 2018-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** Archive preferences
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    @Preference public static String url;
    @Preference public static String user;
    @Preference public static String password;
    @Preference public static String schema;
    @Preference public static int timeout_secs;
    @Preference public static boolean use_array_blob;
    @Preference public static String write_sample_table;
    @Preference public static int max_text_sample_length;
    @Preference public static boolean use_postgres_copy;
    @Preference public static String[] equivalent_pv_prefixes;
    @Preference public static int log_trouble_samples;
    @Preference public static int log_overrun;
    @Preference public static int write_period;
    @Preference public static int max_repeats;
    @Preference public static int batch_size;
    @Preference public static double buffer_reserve;
    @Preference public static int ignored_future;


    static
    {
    	AnnotatedPreferences.initialize(Preferences.class, "/archive_preferences.properties");
    }
}
