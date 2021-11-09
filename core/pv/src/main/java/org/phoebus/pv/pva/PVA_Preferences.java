/*******************************************************************************
 * Copyright (c) 2017-2021 Oak Ridge National Laboratory.
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

    private static final String EPICS_PVA_ADDR_LIST = "epics_pva_addr_list";
    private static final String EPICS_PVA_AUTO_ADDR_LIST = "epics_pva_auto_addr_list";
    private static final String EPICS_PVA_NAME_SERVERS = "epics_pva_name_servers";
    private static final String EPICS_PVA_SERVER_PORT = "epics_pva_server_port";
    private static final String EPICS_PVA_BROADCAST_PORT = "epics_pva_broadcast_port";
    private static final String EPICS_PVA_CONN_TMO = "epics_pva_conn_tmo";
    private static final String EPICS_PVA_MAX_ARRAY_FORMATTING = "epics_pva_max_array_formatting";
    private static final String EPICS_PVA_SEND_BUFFER_SIZE = "epics_pva_send_buffer_size";

    private static final PVA_Preferences instance = new PVA_Preferences();

    /** Initialize */
    private PVA_Preferences()
    {
        try
        {
            installPreferences();
        }
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Preferences Error", ex);
        }
    }

    /** Update the JCA/CAJ related properties from preferences
     *  @throws Exception on error
     */
    public void installPreferences() throws Exception
    {
        final PreferencesReader prefs = new PreferencesReader(PVA_PVFactory.class, "/pv_pva_preferences.properties");

        final String addr_list = prefs.get(EPICS_PVA_ADDR_LIST);
        setSystemProperty("EPICS_PVA_ADDR_LIST", addr_list);
        logger.log(Level.INFO, "PVA " + EPICS_PVA_ADDR_LIST + ": " + addr_list);

        final String name_servers = prefs.get(EPICS_PVA_NAME_SERVERS);
        setSystemProperty("EPICS_PVA_NAME_SERVERS", name_servers);
        logger.log(Level.INFO, "PVA " + EPICS_PVA_NAME_SERVERS + ": " + name_servers);

        final String auto_addr = prefs.get(EPICS_PVA_AUTO_ADDR_LIST);
        setSystemProperty("EPICS_PVA_AUTO_ADDR_LIST", auto_addr);
        logger.log(Level.INFO, "PVA " + EPICS_PVA_AUTO_ADDR_LIST + ": " + auto_addr);

        final String server_port = prefs.get(EPICS_PVA_SERVER_PORT);
        setSystemProperty("EPICS_PVA_SERVER_PORT", server_port);
        logger.log(Level.INFO, "PVA " + EPICS_PVA_SERVER_PORT + ": " + server_port);

        final String broadcast_port = prefs.get(EPICS_PVA_BROADCAST_PORT);
        setSystemProperty("EPICS_PVA_BROADCAST_PORT", broadcast_port);
        logger.log(Level.INFO, "PVA " + EPICS_PVA_BROADCAST_PORT + ": " + broadcast_port);

        final String connection_time_out = prefs.get(EPICS_PVA_CONN_TMO);
        setSystemProperty("EPICS_PVA_CONN_TMO", connection_time_out);
        logger.log(Level.INFO, "PVA " + EPICS_PVA_CONN_TMO + ": " + connection_time_out);

        final String max_array_formatting = prefs.get(EPICS_PVA_MAX_ARRAY_FORMATTING);
        setSystemProperty("EPICS_PVA_MAX_ARRAY_FORMATTING", max_array_formatting);
        logger.log(Level.INFO, "PVA " + EPICS_PVA_MAX_ARRAY_FORMATTING + ": " + max_array_formatting);

        final String send_buffer_size = prefs.get(EPICS_PVA_SEND_BUFFER_SIZE);
        setSystemProperty("EPICS_PVA_SEND_BUFFER_SIZE", send_buffer_size);
        logger.log(Level.INFO, "PVA " + EPICS_PVA_SEND_BUFFER_SIZE + ": " + send_buffer_size);

    }

    /** Sets property from preferences to System properties only if property
     *  value is not null or empty string.
     *  @param prop System property name
     *  @param value phoebus preference name
     */
    private void setSystemProperty(final String prop, final String value)
    {
        if (value == null  ||  value.isEmpty())
            return;

        logger.log(Level.FINE, "{0} = {1}", new Object[] { prop, value });

        System.setProperty(prop, value);
    }

    /** @return Singleton instance */
    public static PVA_Preferences getInstance()
    {
        return instance;
    }

}
