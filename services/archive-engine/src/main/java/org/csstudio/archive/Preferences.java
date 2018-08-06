/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive;

import org.phoebus.framework.preferences.PreferencesReader;

/** Archive preferences
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    public static final String url;
    public static final String user;
    public static final String password;
    public static final String schema;
    public static final int timeout_secs;
    public static final boolean use_array_blob;
    public static final String write_sample_table;
    public static final int max_text_sample_length;
    public static final boolean use_postgres_copy;
    public static final int log_trouble_samples;
    public static final int log_overrun;
    public static final int write_period;
    public static final int max_repeats;
    public static final int batch_size;
    public static final double buffer_reserve;
    public static final int ignored_future;


    static
    {
        final PreferencesReader prefs = new PreferencesReader(Preferences.class, "/archive_preferences.properties");
        url = prefs.get("url");
        user = prefs.get("user");
        password = prefs.get("password");
        schema = prefs.get("schema");
        timeout_secs = prefs.getInt("timeout_secs");
        use_array_blob = prefs.getBoolean("use_array_blob");
        write_sample_table = prefs.get("write_sample_table");
        max_text_sample_length = prefs.getInt("max_text_sample_length");
        use_postgres_copy = prefs.getBoolean("use_postgres_copy");
        log_trouble_samples = prefs.getInt("log_trouble_samples");
        log_overrun = prefs.getInt("log_overrun");
        write_period = prefs.getInt("write_period");
        max_repeats = prefs.getInt("max_repeats");
        batch_size = prefs.getInt("batch_size");
        buffer_reserve = prefs.getDouble("buffer_reserve");
        ignored_future = prefs.getInt("ignored_future");
    }
}
