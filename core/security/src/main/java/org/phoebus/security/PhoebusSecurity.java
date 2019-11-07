/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.security;

import java.util.logging.Logger;

import org.phoebus.framework.preferences.PreferencesReader;

/** Phoebus security logger & Preference settings
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PhoebusSecurity
{
    public static final Logger logger = Logger.getLogger(PhoebusSecurity.class.getPackageName());

    public static final String authorization_file;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(PhoebusSecurity.class, "/phoebus_security_preferences.properties");
        authorization_file = prefs.get("authorization_file");
    }
}
