/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.rdb;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** Settings for RDB archive reader
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RDBPreferences
{
    @Preference static String user, password, prefix;
    @Preference static int timeout_secs;
    @Preference static boolean use_array_blob;
    @Preference static String stored_procedure;
    @Preference static String starttime_function;
    @Preference static int fetch_size;

    static
    {
    	AnnotatedPreferences.initialize(RDBPreferences.class, "/archive_reader_rdb_preferences.properties");
    }
}
