/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.pv.influx2;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.phoebus.framework.preferences.PreferencesReader;

/**
 * Singleton class responsible for loading and providing access to InfluxDB connection preferences.
 * <p>
 * Preferences are read from a properties file named {@code pv_influx_preferences.properties}
 * using the {@link PreferencesReader} utility. This class centralizes configuration details
 * such as host, port, organization, authentication token, and HTTPS usage flag.
 *
 */
public class InfluxDB_Preferences {
    /** Logger used for logging information and errors related to InfluxDB preferences. */
    private static final Logger LOGGER = Logger.getLogger(InfluxDB_Preferences.class.getName());

    private static final String HOST = "influx_host";
    private static final String PORT = "influx_port";
    private static final String ORGANIZATION = "influx_organization";
    private static final String TOKEN = "influx_token";
    private static final String USE_HTTPS = "influx_useHttps";
    private static final String DISCONNECT_TIMEOUT = "influx_disconnectTimeoutMs";

    /** The singleton instance of this class. */
    private static final InfluxDB_Preferences INSTANCE = new InfluxDB_Preferences();

    private static String host;
    private static int port;
    private String organization;
    private String token;
    private boolean useHttps;
    private long refreshPeriod;
    private long disconnectTimeout;

    /**
     * Private constructor that loads preferences upon instantiation.
     * This is part of the singleton pattern.
     */
    private InfluxDB_Preferences() {
        try {
            installPreferences();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while loading InfluxDB preferences", e);
        }
    }

    /**
     * Reads the InfluxDB preferences from the properties file.
     * Uses the {@link PreferencesReader} to retrieve values.
     */
    private void installPreferences() {
        PreferencesReader prefs = new PreferencesReader(InfluxDB_Preferences.class, "/pv_influx_preferences.properties");

        try {
            Class<?> prefsClass = Class.forName("org.csstudio.display.builder.runtime.Preferences");
            Field field = prefsClass.getField("update_throttle_ms");
            refreshPeriod = (Integer) field.get(null);
        } catch (Exception e) {
            refreshPeriod = 250;
        }

        host = prefs.get(HOST);
        port = prefs.getInt(PORT);
        organization = prefs.get(ORGANIZATION);
        token = prefs.get(TOKEN);
        useHttps = prefs.getBoolean(USE_HTTPS);
        disconnectTimeout = prefs.getLong(DISCONNECT_TIMEOUT);

        if (disconnectTimeout == 0) {
            disconnectTimeout = 3000;
        }
    }

    /**
     * Returns the singleton instance of {@code InfluxDB_Preferences}.
     *
     * @return the singleton instance
     */
    public static InfluxDB_Preferences getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the configured InfluxDB host name or IP address.
     *
     * @return the InfluxDB host
     */
    public static String getHost() {
        return host;
    }

    /**
     * Returns the configured port number for connecting to InfluxDB.
     *
     * @return the InfluxDB port
     */
    public static int getPort() {
        return port;
    }

    /**
     * Returns the name of the InfluxDB organization.
     *
     * @return the InfluxDB organization
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * Returns the authentication token used for accessing InfluxDB.
     *
     * @return the InfluxDB token
     */
    public String getToken() {
        return token;
    }

    /**
     * Indicates whether HTTPS should be used when connecting to InfluxDB.
     *
     * @return {@code true} if HTTPS is enabled; {@code false} otherwise
     */
    public boolean isUseHttps() {
        return useHttps;
    }

    /**
     * Retuns the refresh period of a PV in milliseconds.
     * Default: 1000ms
     *
     * @return the refrehs period
     */
    public long getRefreshPeriod() {
        return refreshPeriod;
    }

    /**
     * Returns the timeout before a PV is considered as disconnected in milliseconds.
     * Disconnected state is triggered when a PV isn't updated for a defined amount of time.
     * Default: 3000 ms
     *
     * @return timeout disconnected
     */
    public long getDisconnectTimeout() {
        return disconnectTimeout;
    }
}
