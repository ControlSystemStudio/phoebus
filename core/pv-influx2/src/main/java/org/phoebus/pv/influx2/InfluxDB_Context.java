/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.pv.influx2;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Singleton context for managing the InfluxDB client instance.
 * <p>
 * This class initializes and holds a single {@link InfluxDBClient} that can be
 * accessed globally throughout the application. Connection settings such as host,
 * port, token, and HTTPS usage are read from {@link InfluxDB_Preferences}.
 *
 */
public class InfluxDB_Context {
    /** Logger for logging initialization and configuration messages. */
    private static final Logger LOGGER = Logger.getLogger(InfluxDB_Context.class.getName());

    /** The InfluxDB client used for querying or writing data. */
    private final InfluxDBClient client;

    /**
     * Private constructor that creates an {@link InfluxDBClient} based on application preferences.
     * <p>
     * Builds the client using host, port, token, and organization defined in the
     * {@code pv_influx_preferences.properties} file.
     */
    private InfluxDB_Context() {
        InfluxDB_Preferences prefs = InfluxDB_Preferences.getInstance();
        String url = String.format("%s://%s:%d",
                prefs.isUseHttps() ? "https" : "http",
                prefs.getHost(),
                prefs.getPort());

        client = InfluxDBClientFactory.create(url, prefs.getToken().toCharArray(), prefs.getOrganization());
        LOGGER.log(Level.CONFIG, "InfluxDB client created with URL: {0}", url);
    }

    private static class Holder {
        private static final InfluxDB_Context INSTANCE = new InfluxDB_Context();
    }

    /**
     * Returns the singleton instance of {@code InfluxDB_Context}.
     *
     * @return the singleton {@code InfluxDB_Context} instance
     */
    public static InfluxDB_Context getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Returns the underlying {@link InfluxDBClient} instance.
     *
     * @return the InfluxDB client
     */
    public InfluxDBClient getClient() {
        return client;
    }
}
