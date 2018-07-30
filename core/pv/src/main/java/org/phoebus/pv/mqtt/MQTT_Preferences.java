/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.pv.mqtt;

import org.phoebus.framework.preferences.PreferencesReader;

public class MQTT_Preferences
{
    private static final PreferencesReader prefs = new PreferencesReader(MQTT_PVConn.class, "/pv_mqtt_preferences.properties");
    
    private static String brokerURL;
    
    static
    {
        brokerURL = prefs.get("mqtt_broker");
    }
    
    public static String getBrokerURL()
    {
        return brokerURL;
    }
}
