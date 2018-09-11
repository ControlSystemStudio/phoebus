/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.appliance;

import org.phoebus.framework.preferences.PreferencesReader;

/**
 * Settings for Appliance archive reader
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AppliancePreferences {
    static final String USESTATS = "useStatisticsForOptimizedData";
    static final String USEOPTIMIZED = "useNewOptimizedOperator";

    static boolean useStatisticsForOptimizedData;
    static boolean useNewOptimizedOperator;

    static {
        final PreferencesReader prefs = new PreferencesReader(AppliancePreferences.class, "/appliance_preferences.properties");
        useStatisticsForOptimizedData = prefs.getBoolean(USESTATS);
        useNewOptimizedOperator = prefs.getBoolean(USEOPTIMIZED);
    }
}
