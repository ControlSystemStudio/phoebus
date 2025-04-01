/*******************************************************************************
 * Copyright (c) 2017-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import static org.phoebus.pv.PV.logger;

import java.util.logging.Level;

import org.phoebus.framework.preferences.PreferencesReader;

/** Preferences for PVAccess
 *
 *  <p>The underlying PVA library, just like the Channel Access library,
 *  is unaware of phoebus and uses system settings, falling back to environment variables.
 *  In phoebus we aim to use Java preference settings.
 *  This class allows the following:
 *
 *  <ol>
 *  <li>Only environment variables are set: PVA lib uses them as found
 *  <li>System settings in place: PVA lib uses them as found
 *  <li>Preference setting in place: This code updates system settings from preferences,
 *      and PVA lib then uses the system settings.
 *  </ol>
 *
 *  <p>Based on code that was in the org.csstudio.platform.libs.epics.EpicsPlugin,
 *  Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton.
 *  When checking its license link, HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM,
 *  on 2016-08-18, it listed http://www.eclipse.org/org/documents/epl-v10.php,
 *
 *  @author Original author unknown
 *  @author Sergei Chevtsov - Contributed to EpicsPlugin
 *  @author Gabriele Carcassi - Contributed to EpicsPlugin
 *  @author Kay Kasemir
 *  @author Kunal Shroff
 */
@SuppressWarnings("nls")
public class PVA_Preferences
{
    private static final PVA_Preferences instance = new PVA_Preferences();

    public static int epics_pva_write_reply_timeout_ms;

    /** Prevent direct instantiation */
    private PVA_Preferences()
    {
    }

    /** Update the PVA related system properties from preferences
     *  @throws Exception on error
     */
    public void installPreferences() throws Exception
    {
        final PreferencesReader prefs = new PreferencesReader(PVA_PVFactory.class, "/pv_pva_preferences.properties");

        for (String setting : new String[]
                            {
                                "epics_pva_addr_list",
                                "epics_pva_auto_addr_list",
                                "epics_pva_name_servers",
                                "epics_pva_server_port",
                                "epics_pva_broadcast_port",
                                "epics_pva_conn_tmo",
                                "epics_pva_tcp_socket_tmo",
                                "epics_pva_max_array_formatting",
                                "epics_pva_send_buffer_size"
                            })
        {
            final String value = prefs.get(setting);
            if (value != null  &&  !value.isEmpty())
            {
                final String propname = setting.toUpperCase();
                System.setProperty(propname, value);
                logger.log(Level.INFO, "Setting {0} from {1}/{2}={3}",
                           new Object[] { propname, PVA_PVFactory.class.getPackageName(), setting, value });
            }
        }

        epics_pva_write_reply_timeout_ms = prefs.getInt("epics_pva_write_reply_timeout_ms");
    }

    /** @return Singleton instance */
    public static PVA_Preferences getInstance()
    {
        return instance;
    }
}
