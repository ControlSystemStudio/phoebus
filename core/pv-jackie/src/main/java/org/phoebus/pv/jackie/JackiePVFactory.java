/*******************************************************************************
 * Copyright (c) 2024 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.pv.jackie;

import com.aquenos.epics.jackie.client.ChannelAccessClient;
import com.aquenos.epics.jackie.client.ChannelAccessClientConfiguration;
import com.aquenos.epics.jackie.client.DefaultChannelAccessClient;
import com.aquenos.epics.jackie.client.beacon.BeaconDetectorConfiguration;
import com.aquenos.epics.jackie.client.resolver.ChannelNameResolverConfiguration;
import com.aquenos.epics.jackie.common.exception.JavaUtilLoggingErrorHandler;
import com.aquenos.epics.jackie.common.util.ListenerLockPolicy;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVFactory;

import java.util.logging.Level;

/**
 * <p>
 * Factory for instances of {@link JackiePV}.
 * </p>
 * <p>
 * Typically, this factory should not be used directly but through
 * {@link org.phoebus.pv.PVPool}. There is no need to create more than one
 * instance of this class, because all its state is static anyway.
 * </p>
 * <p>
 * This class statically creates an instance of EPICS Jackie’s
 * {@link DefaultChannelAccessClient}, which is configured using the default
 * instance of {@link JackiePreferences}.
 * </p>
 */
public class JackiePVFactory implements PVFactory {

    private final static ChannelAccessClient CLIENT;
    private final static JackiePreferences PREFERENCES;
    private final static String TYPE = "jackie";

    static {
        PREFERENCES = JackiePreferences.getDefaultInstance();
        // We want to use a higher log-level for errors, so that we can be sure
        // that they are reported, even if INFO logging is not enabled.
        var error_handler = new JavaUtilLoggingErrorHandler(
                Level.SEVERE, Level.WARNING);
        var beacon_detector_config = new BeaconDetectorConfiguration(
                error_handler,
                PREFERENCES.ca_server_port(),
                PREFERENCES.ca_repeater_port());
        var resolver_config = new ChannelNameResolverConfiguration(
                PREFERENCES.charset(),
                error_handler,
                PREFERENCES.hostname(),
                PREFERENCES.username(),
                PREFERENCES.ca_server_port(),
                PREFERENCES.ca_name_servers(),
                PREFERENCES.ca_address_list(),
                PREFERENCES.ca_auto_address_list(),
                PREFERENCES.ca_max_search_period(),
                PREFERENCES.ca_echo_interval(),
                PREFERENCES.ca_multicast_ttl());
        var client_config = new ChannelAccessClientConfiguration(
                PREFERENCES.charset(),
                PREFERENCES.hostname(),
                PREFERENCES.username(),
                PREFERENCES.ca_max_array_bytes(),
                PREFERENCES.ca_max_array_bytes(),
                PREFERENCES.ca_echo_interval(),
                PREFERENCES.cid_block_reuse_time(),
                null,
                Boolean.TRUE,
                error_handler,
                beacon_detector_config,
                resolver_config);
        // We use ListenerLockPolicy.IGNORE, because we call listeners from our
        // code, and we cannot be sure whether these listeners might acquire
        // locks, so the BLOCK policy could result in deadlocks.
        CLIENT = new DefaultChannelAccessClient(
                client_config, ListenerLockPolicy.IGNORE);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public PV createPV(String name, String base_name) throws Exception {
        return new JackiePV(CLIENT, PREFERENCES, name, base_name);
    }

}
