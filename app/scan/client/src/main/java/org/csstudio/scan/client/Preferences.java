/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.scan.client;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/** Preference settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Preferences
{
    @Preference public static String host;
    @Preference public static int port;
    @Preference public static int poll_period;

    static
    {
    	AnnotatedPreferences.initialize(Preferences.class, "/scan_client_preferences.properties");
    }
}
