/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.pv.mqtt;

import org.phoebus.framework.preferences.AnnotatedPreferences;
import org.phoebus.framework.preferences.Preference;

/**
 * Singleton preferences class for MQTT PVs
 * @author Evan Smith
 */
@SuppressWarnings("nls")
public class MQTT_Preferences
{
    /** Broker URL */
    @Preference public static String mqtt_broker;

    static
    {
    	AnnotatedPreferences.initialize(MQTT_Preferences.class, "/pv_mqtt_preferences.properties");
    }
}
