/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.pv.mqtt;

import org.phoebus.framework.preferences.PreferencesReader;

/**
 * Singleton preferences class for MQTT PVs
 * @author Evan Smith
 */
@SuppressWarnings("nls")
public class MQTT_Preferences
{
    public static final String brokerURL;

    static
    {
        final PreferencesReader prefs = new PreferencesReader(MQTT_PVConn.class, "/pv_mqtt_preferences.properties");
        brokerURL = prefs.get("mqtt_broker");
    }
}
