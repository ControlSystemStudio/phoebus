/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.rdb;

import org.phoebus.framework.preferences.PreferencesReader;

/** Settings for RDB archive reader
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RDBPreferences
{
    static final String USER = "user";
    static final String PASSWORD = "password";
    static final String PREFIX = "prefix";
    static final String TIMEOUT_SECS = "timeout_secs";
    static final String USE_ARRAY_BLOB = "use_array_blob";
    static final String STORED_PROCEDURE = "stored_procedure";
    static final String STARTTIME_FUNCTION = "starttime_function";
    static final String FETCH_SIZE = "fetch_size";

    static String user, password, prefix;
    static int timeout;
    static boolean use_array_blob;
    static String stored_procedure, starttime_function;
    static int fetch_size;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(RDBPreferences.class, "/archive_reader_rdb_preferences.properties");
        user               = prefs.get(USER);
        password           = prefs.get(PASSWORD);
        prefix             = prefs.get(PREFIX);
        timeout            = prefs.getInt(TIMEOUT_SECS);
        use_array_blob     = prefs.getBoolean(USE_ARRAY_BLOB);
        stored_procedure   = prefs.get(STORED_PROCEDURE);
        starttime_function = prefs.get(STARTTIME_FUNCTION);
        fetch_size         = prefs.getInt(FETCH_SIZE);
    }
}
