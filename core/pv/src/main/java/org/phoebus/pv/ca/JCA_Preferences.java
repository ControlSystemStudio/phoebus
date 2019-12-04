/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.ca;

import static org.phoebus.pv.PV.logger;

import java.util.logging.Level;

import org.phoebus.framework.preferences.PreferencesReader;

import gov.aps.jca.Monitor;

/** Preferences for JCA
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
 */
@SuppressWarnings("nls")
public class JCA_Preferences
{
    private static final String VARIABLE_LENGTH_ARRAY = "variable_length_array";
    private static final String MAX_ARRAY_BYTES = "max_array_bytes";
    private static final String SERVER_PORT = "server_port";
    private static final String REPEATER_PORT = "repeater_port";
    private static final String BEACON_PERIOD = "beacon_period";
    private static final String CONNECTION_TIMEOUT = "connection_timeout";
    private static final String AUTO_ADDR_LIST = "auto_addr_list";
    private static final String ADDR_LIST = "addr_list";
    private static final String LARGE_ARRAY_THRESHOLD = "large_array_threshold";
    private static final String DBE_PROPERTY_SUPPORTED = "dbe_property_supported";
    private static final String MONITOR_MASK = "monitor_mask";
    private static final String NAME_SERVERS = "name_servers";

    private static final JCA_Preferences instance = new JCA_Preferences();

    private int monitor_mask = Monitor.VALUE | Monitor.ALARM;

    private boolean dbe_property_supported = false;

    private Boolean var_array_supported = Boolean.FALSE;

    private int large_array_threshold = 100000;

    /** Initialize */
    private JCA_Preferences()
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
        final PreferencesReader prefs = new PreferencesReader(JCA_PVFactory.class, "/pv_ca_preferences.properties");

        String code = prefs.get(MONITOR_MASK);
        switch (code)
        {
        case "ARCHIVE":
            monitor_mask = Monitor.LOG;
            break;
        case "ALARM":
            monitor_mask = Monitor.ALARM;
            break;
        default:
            logger.log(Level.WARNING, "Invalid " + MONITOR_MASK + "'" + code + "'");
        case "VALUE":
            monitor_mask = Monitor.VALUE | Monitor.ALARM;
            break;
        }

        dbe_property_supported = prefs.getBoolean(DBE_PROPERTY_SUPPORTED);

        code = prefs.get(VARIABLE_LENGTH_ARRAY);
        switch (code)
        {
        case "true":
            var_array_supported = Boolean.TRUE;
            break;
        case "false":
            var_array_supported = Boolean.FALSE;
            break;
        default:
            var_array_supported = null;
        }

        large_array_threshold = prefs.getInt(LARGE_ARRAY_THRESHOLD);

        // Set the 'CAJ' and 'JNI' copies of the settings
        setSystemProperty("com.cosylab.epics.caj.CAJContext.use_pure_java", "true");

        final String addr_list = prefs.get(ADDR_LIST);
        setSystemProperty("com.cosylab.epics.caj.CAJContext.addr_list", addr_list);
        setSystemProperty("gov.aps.jca.jni.JNIContext.addr_list", addr_list);
        logger.log(Level.INFO, "JCA " + ADDR_LIST + ": " + addr_list);

        final String auto_addr = prefs.get(AUTO_ADDR_LIST);
        setSystemProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", auto_addr);
        setSystemProperty("gov.aps.jca.jni.JNIContext.auto_addr_list", auto_addr);

        final String timeout = prefs.get(CONNECTION_TIMEOUT);
        setSystemProperty("com.cosylab.epics.caj.CAJContext.connection_timeout", timeout);
        setSystemProperty("gov.aps.jca.jni.JNIContext.connection_timeout", timeout);

        final String beacon_period = prefs.get(BEACON_PERIOD);
        setSystemProperty("com.cosylab.epics.caj.CAJContext.beacon_period", beacon_period);
        setSystemProperty("gov.aps.jca.jni.JNIContext.beacon_period", beacon_period);

        final String repeater_port = prefs.get(REPEATER_PORT);
        setSystemProperty("com.cosylab.epics.caj.CAJContext.repeater_port", repeater_port);
        setSystemProperty("gov.aps.jca.jni.JNIContext.repeater_port", repeater_port);

        final String server_port = prefs.get(SERVER_PORT);
        setSystemProperty("com.cosylab.epics.caj.CAJContext.server_port", server_port);
        setSystemProperty("gov.aps.jca.jni.JNIContext.server_port", server_port);

        final String max_array_bytes = prefs.get(MAX_ARRAY_BYTES);
        setSystemProperty("com.cosylab.epics.caj.CAJContext.max_array_bytes", max_array_bytes);
        setSystemProperty("gov.aps.jca.jni.JNIContext.max_array_bytes", max_array_bytes);

        final String name_servers = prefs.get(NAME_SERVERS);
        setSystemProperty("com.cosylab.epics.caj.CAJContext.name_servers", name_servers);

        // gov.aps.jca.event.QueuedEventDispatcher avoids
        // deadlocks when calling JCA while receiving JCA callbacks.
        // But JCA_PV avoids deadlocks, and QueuedEventDispatcher is faster
        setSystemProperty("gov.aps.jca.jni.ThreadSafeContext.event_dispatcher",
                          "gov.aps.jca.event.DirectEventDispatcher");
    }

    /** Sets property from preferences to System properties only if property
     *  value is not null or empty string.
     *  @param prop System property name
     *  @param value CSS preference name
     */
    private void setSystemProperty(final String prop, final String value)
    {
        if (value == null  ||  value.isEmpty())
            return;

        logger.log(Level.FINE, "{0} = {1}", new Object[] { prop, value });

        System.setProperty(prop, value);
    }

    /** @return Singleton instance */
    public static JCA_Preferences getInstance()
    {
        return instance;
    }

    /** @return Mask used to create CA monitors (subscriptions) */
    public int getMonitorMask()
    {
        return monitor_mask;
    }

    /** @return whether metadata updates are enabled */
    public boolean isDbePropertySupported()
    {
        return dbe_property_supported;
    }

    /** @return Whether variable array should be supported (true/false), or auto-detect (<code>null</code>) */
    public Boolean isVarArraySupported()
    {
        return var_array_supported;
    }

    public int largeArrayThreshold()
    {
        return large_array_threshold;
    }
}
