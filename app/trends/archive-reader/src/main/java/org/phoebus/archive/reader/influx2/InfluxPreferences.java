/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.archive.reader.influx2;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

public class InfluxPreferences {
    @Preference static String ip;
    @Preference static String port;
    @Preference static String bucket;
    @Preference static String org;
    @Preference static String token;
    @Preference static boolean useHttps;
    static {
        AnnotatedPreferences.initialize(InfluxPreferences.class, "/influx_preferences.properties");
    }
}