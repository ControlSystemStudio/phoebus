/*******************************************************************************
 * Copyright (c) 2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.pv.pvws;


import org.phoebus.framework.preferences.PreferencesReader;

/**
 * <p>
 * Preferences used by the {@link PVWS_PV} and {@link PVWS_PVFactory}.
 * </p>
 *
 * @param pvws_address PVWS server URL
 */
public class PVWS_Preferences {

    private static final PVWS_Preferences instance = new PVWS_Preferences();


    /**
     * Prevent direct instantiation
     */
    private PVWS_Preferences() {
    }


    public void installPreferences() throws Exception {
        final PreferencesReader prefs = new PreferencesReader(PVWS_PVFactory.class, "/pv_pvws_preferences.properties");

        for (String setting : new String[]
                {
                        "pvws_address"
                })
        {
            final String value = prefs.get(setting);
            if (value != null && !value.isEmpty()) {
                final String propname = setting.toUpperCase();
                System.setProperty(propname, value);
            }
        }

    }


    /**
     * @return Singleton instance
     */
    public static PVWS_Preferences getInstance() {
        return instance;
    }
}
