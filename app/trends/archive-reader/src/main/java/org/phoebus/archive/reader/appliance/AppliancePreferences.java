/*******************************************************************************
 * Copyright (c) 2018-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.appliance;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/**
 * Settings for Appliance archive reader
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AppliancePreferences {
    @Preference static boolean useStatisticsForOptimizedData;
    @Preference static boolean useNewOptimizedOperator;
    @Preference static boolean useHttps;

    static {
    	AnnotatedPreferences.initialize(AppliancePreferences.class, "/appliance_preferences.properties");
    }
}
