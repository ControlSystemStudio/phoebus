/*******************************************************************************
 * Copyright (c) 2024-2025 aquenos GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.pv.jackie;

import com.aquenos.epics.jackie.common.exception.ErrorHandler;
import com.aquenos.epics.jackie.common.protocol.ChannelAccessConstants;
import com.aquenos.epics.jackie.common.protocol.ChannelAccessEventMask;
import com.aquenos.epics.jackie.common.util.Inet4AddressUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.phoebus.framework.preferences.PreferencesReader;

import java.net.Inet4Address;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * <p>
 * Preferences used by the {@link JackiePV} and {@link JackiePVFactory}.
 * </p>
 * <p>
 * Each of the parameters corresponds to a property in the preferences system,
 * using the <code>org.phoebus.pv.jackie</code> namespace. In addition to that,
 * there is the <code>use_env</code> property, which controls whether the
 * <code>ca_*</code> properties are actually used or whether the corresponding
 * environment variables are preferred.
 * </p>
 * <p>
 * Please refer to the <code>pv_jackie_preferences.properties</code> file for a
 * full list of available properties and their meanings.
 * </p>
 *
 * @param ca_address_list
 *  EPICS servers that are contacted via UDP when resolving channel names.
 *  <code>null</code> means that the <code>EPICS_CA_ADDR_LIST</code>
 *  environment variable shall be used instead.
 * @param ca_auto_address_list
 *  flag indicating whether the broadcast addresses of local interfaces shall
 *  be automatically added to the <code>ca_address_list</code>.
 *  <code>null</code> means that the <code>EPICS_CA_AUTO_ADDR_LIST</code>
 *  environment variable shall be used instead.
 * @param ca_auto_array_bytes
 *  flag indicating whether the <code>ca_max_array_bytes</code> setting shall
 *  be discarded. <code>null</code> means that the
 *  <code>EPICS_CA_AUTO_ARRAY_BYTES</code> environment variable shall be used
 *  instead.
 * @param ca_echo_interval
 *  time interval (in seconds) between sending echo requests to Channel Access
 *  servers. <code>null</code> means that the <code>EPICS_CA_CONN_TMO</code>
 *  environment variable shall be used instead.
 * @param ca_max_array_bytes
 *  maximum size (in bytes) of a serialized value that can be transferred via
 *  Channel  Access. This is not used when <code>ca_auto_array_bytes</code> is
 *  <code>true</code>. <code>null</code> means that the
 *  <code>EPICS_CA_MAX_ARRAY_BYTES</code> environment variable shall be used
 *  instead.
 * @param ca_max_search_period
 *  time interval (in seconds) for that is used for the highest search period
 *  when resolving channel names. <code>null</code> means that the
 *  <code>EPICS_CA_MAX_SEARCH_PERIOD</code> environment variable shall be used
 *  instead.
 * @param ca_multicast_ttl
 *  TTL used when sending multicast UDP packets. <code>null</code> means that
 *  the <code>EPICS_CA_MCAST_TTL</code> environment variable shall be used
 *  instead.
 * @param ca_name_servers
 *  EPICS servers that are contacted via TCP when resolving channel names.
 *  <code>null</code> means that the <code>EPICS_CA_NAME_SERVERS</code>
 *  environment variable shall be used instead.
 * @param ca_repeater_port
 *  UDP port used by the CA repeater. <code>null</code> means that the
 *  <code>EPICS_CA_REPEATER_PORT</code> environment variable shall be used
 *  instead.
 * @param ca_server_port
 *  TCP and UDP port used when connecting to CA servers and the port is not
 *  known. <code>null</code> means that the<code>EPICS_CA_SERVER_PORT</code>
 *  environment variable shall be used instead.
 * @param charset
 *  charset used when encoding or decoding Channel Access string values.
 * @param cid_block_reuse_time
 *  time (in milliseconds) after which a CID (identifying a certain channel on
 *  the client side) may be reused.
 * @param dbe_property_supported
 *  flag indicating whether a monitor using the <code>DBE_PROPERTY</code> event
 *  code shall be registered in order to be notified of meta-data changes.
 * @param honor_zero_precision
 *  flag indicating whether a floating-point value specifying a precision of
 *  zero shall be printed without any fractional digits (<code>true</code>) or
 *  whether such a value should be printed using a default format
 *  (<code>false</code>).
 * @param hostname
 *  hostname that is sent to the Channel Access server. <code>null</code> means
 *  that the hostname should be determined automatically.
 * @param monitor_mask
 *  event mask used for the regular monitor. This mask should typically include
 *  <code>DBE_ALARM</code> and one of <code>DBE_VALUE</code> or
 *  <code>DBE_ARCHIVE</code>.
 * @param rtyp_value_only
 *  flag indicating whether a value of type <code>DBR_STRING</code> instead of
 *  <code>DBR_TIME_STRING</code> should be requested when monitoring a channel
 *  with a name ending with <code>.RTYP</code>.
 * @param username
 *  username that is sent to the Channel Access server. <code>null</code> means
 *  that the hostname should be determined automatically.
 */
public record JackiePreferences(
        Set<Pair<Inet4Address, Integer>> ca_address_list,
        Boolean ca_auto_address_list,
        Boolean ca_auto_array_bytes,
        Double ca_echo_interval,
        Integer ca_max_array_bytes,
        Double ca_max_search_period,
        Integer ca_multicast_ttl,
        Set<Pair<Inet4Address, Integer>> ca_name_servers,
        Integer ca_repeater_port,
        Integer ca_server_port,
        Charset charset,
        long cid_block_reuse_time,
        boolean dbe_property_supported,
        boolean honor_zero_precision,
        String hostname,
        LongConversionMode long_conversion_mode,
        ChannelAccessEventMask monitor_mask,
        boolean rtyp_value_only,
        String username) {

    /**
     * Mode for handling integer numbers that are too large to fit into the
     * {@link Integer} type.
     */
    public enum LongConversionMode {
        /**
         * Limit the {@link Long} value to the {@link Integer} range.
         *
         * A value that is greater than {@link Integer#MAX_VALUE} is converted
         * to {@link Integer#MAX_VALUE}, and a value that is less than
         * {@link Integer#MIN_VALUE} is converted to {@link Integer#MIN_VALUE}.
         */
        COERCE,

        /**
         * Limit the {@link Long} value to the {@link Integer} range and log a
         * warning.
         *
         * This essentially is the same behavior as {@link #COERCE}, but a
         * warning message is written to the log.
         */
        COERCE_AND_WARN,

        /**
         * Convert the {@link Long} value to a {@link Double}.
         *
         * This means that some precision is lost.
         */
        CONVERT,

        /**
         * Convert the {@link Long} value to a {@link Double} and log a
         * warning.
         *
         * This essentially is the same behavior as {@link #CONVERT}, but a
         * warning message is written to the log.
         */
        CONVERT_AND_WARN,

        /**
         * Raise an {@link IllegalArgumentException}.
         */
        FAIL,

        /**
         * Cast the {@link Long} value to an {@link Integer}.
         *
         * This means that the value will overflow (e.g.
         * <code>Integer.MAX_VALUE + 1</code> becomes
         * <code>Integer.MIN_VALUE</code>.
         */
        TRUNCATE,

        /**
         * Cast the {@link Long} value to an {@link Integer} and log a warning.
         *
         * This essentially is the same behavior as {@link #CAST}, but a
         * warning message is written to the log.
         */
        TRUNCATE_AND_WARN,
    }

    private final static JackiePreferences DEFAULT_INSTANCE;

    static {
        DEFAULT_INSTANCE = loadPreferences();
    }

    /**
     * Returns the default instance of the preferences. This is the instance
     * that is automatically configured through Phoebus’s
     * {@link PreferencesReader}.
     *
     * @return preference instance created using the {@link PreferencesReader}.
     */
    public static JackiePreferences getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    private static JackiePreferences loadPreferences() {
        final var logger = Logger.getLogger(
                JackiePreferences.class.getName());
        final var preference_reader = new PreferencesReader(
                JackiePreferences.class,
                "/pv_jackie_preferences.properties");
        Set<Pair<Inet4Address, Integer>> ca_address_list = null;
        final var ca_address_list_string = preference_reader.get(
                "ca_address_list");
        Boolean ca_auto_address_list = null;
        final var ca_auto_address_list_string = preference_reader.get(
                "ca_auto_address_list");
        Boolean ca_auto_array_bytes = null;
        final var ca_auto_array_bytes_string = preference_reader.get(
                "ca_auto_array_bytes");
        Double ca_echo_interval = null;
        final var ca_echo_interval_string = preference_reader.get(
                "ca_echo_interval");
        Integer ca_max_array_bytes = null;
        final var ca_max_array_bytes_string = preference_reader.get(
                "ca_max_array_bytes");
        Double ca_max_search_period = null;
        final var ca_max_search_period_string = preference_reader.get(
                "ca_max_search_period");
        Integer ca_multicast_ttl = null;
        final var ca_multicast_ttl_string = preference_reader.get(
                "ca_multicast_ttl");
        Set<Pair<Inet4Address, Integer>> ca_name_servers = null;
        final var ca_name_servers_string = preference_reader.get(
                "ca_name_servers");
        Integer ca_repeater_port = null;
        final var ca_repeater_port_string = preference_reader.get(
                "ca_repeater_port");
        Integer ca_server_port = null;
        final var ca_server_port_string = preference_reader.get(
                "ca_server_port");
        Charset charset = null;
        final var charset_string = preference_reader.get("charset");
        if (!charset_string.isEmpty()) {
            try {
                charset = Charset.forName(charset_string);
            } catch (IllegalCharsetNameException
                     | UnsupportedCharsetException e) {
                logger.warning(
                        "Using UTF-8 charset because specified charset is "
                                + "invalid: "
                                + charset_string);
            }
        }
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }
        final var cid_block_reuse_time = preference_reader.getLong(
                "cid_block_reuse_time");
        final var dbe_property_supported = preference_reader.getBoolean(
                "dbe_property_supported");
        final var honor_zero_precision = preference_reader.getBoolean(
                "honor_zero_precision");
        var hostname = preference_reader.get("hostname");
        if (hostname.isEmpty()) {
            hostname = null;
        }
        final var long_conversion_mode_string = preference_reader.get(
                "long_conversion_mode");
        LongConversionMode long_conversion_mode;
        try {
            long_conversion_mode = LongConversionMode.valueOf(
                    long_conversion_mode_string);
        } catch (IllegalArgumentException|NullPointerException e) {
            logger.severe(
                    "Invalid long conversion mode: "
                            + long_conversion_mode_string);
            long_conversion_mode = LongConversionMode.COERCE_AND_WARN;
        }
        final var monitor_mask_string = preference_reader.get("monitor_mask");
        ChannelAccessEventMask monitor_mask;
        try {
            monitor_mask = parseMonitorMask(monitor_mask_string);
        } catch (IllegalArgumentException e) {
            logger.severe("Invalid monitor mask: " + monitor_mask_string);
            monitor_mask = ChannelAccessEventMask.DBE_VALUE.or(
                    ChannelAccessEventMask.DBE_ALARM);
        }
        final var rtyp_value_only = preference_reader.getBoolean(
                "rtyp_value_only");
        final var use_env = preference_reader.getBoolean("use_env");
        var username = preference_reader.get("username");
        if (username.isEmpty()) {
            username = null;
        }
        if (use_env) {
            if (!ca_address_list_string.isEmpty()) {
                logger.warning(
                        "use_env = true, ca_address_list setting is ignored.");
            }
            if (!ca_auto_address_list_string.isEmpty()) {
                logger.warning(
                        "use_env = true, ca_auto_address_list setting is "
                                + "ignored.");
            }
            if (!ca_auto_array_bytes_string.isEmpty()) {
                logger.warning(
                        "use_env = true, ca_auto_array_bytes setting is "
                                + "ignored.");
            }
            if (!ca_echo_interval_string.isEmpty()) {
                logger.warning(
                        "use_env = true, ca_echo_interval setting is "
                                + "ignored.");
            }
            if (!ca_max_array_bytes_string.isEmpty()) {
                logger.warning(
                        "use_env = true, ca_max_array_bytes setting is "
                                + "ignored.");
            }
            if (!ca_max_search_period_string.isEmpty()) {
                logger.warning(
                        "use_env = true, ca_max_search_period setting is "
                                + "ignored.");
            }
            if (!ca_multicast_ttl_string.isEmpty()) {
                logger.warning(
                        "use_env = true, ca_multicast_ttl setting is "
                                + "ignored.");
            }
            if (!ca_name_servers_string.isEmpty()) {
                logger.warning(
                        "use_env = true, ca_name_servers setting is ignored.");
            }
            if (!ca_repeater_port_string.isEmpty()) {
                logger.warning(
                        "use_env = true, ca_repeater_port setting is "
                                + "ignored.");
            }
            if (!ca_server_port_string.isEmpty()) {
                logger.warning(
                        "use_env = true, ca_server_port setting is ignored.");
            }
        } else {
            if (ca_auto_address_list_string.isEmpty()) {
                ca_auto_address_list = Boolean.TRUE;
            } else {
                ca_auto_address_list = Boolean.valueOf(
                        ca_auto_address_list_string);
            }
            if (ca_auto_array_bytes_string.isEmpty()) {
                ca_auto_array_bytes = Boolean.TRUE;
            } else {
                ca_auto_array_bytes = Boolean.valueOf(
                        ca_auto_array_bytes_string);
            }
            if (!ca_echo_interval_string.isEmpty()) {
                ca_echo_interval = 30.0;
            } else {
                try {
                    ca_echo_interval = Double.valueOf(ca_echo_interval_string);
                } catch (NumberFormatException e) {
                    logger.warning(
                            "Using ca_echo_interval = 30.0 because specified "
                                    + "value is invalid: "
                                    + ca_echo_interval_string);
                    ca_echo_interval = 30.0;
                }
                if (ca_echo_interval < 0.1) {
                    logger.warning(
                            "ca_echo_interval = "
                                    + ca_echo_interval
                                    + " is too small. Using ca_echo_inteval = "
                                    + "0.1 instead.");
                    ca_echo_interval = 0.1;
                }
                if (!Double.isFinite(ca_echo_interval)) {
                    logger.warning(
                            "Using ca_echo_interval = 30.0 because specified "
                                    + "value is invalid: "
                                    + ca_echo_interval);
                    ca_echo_interval = 30.0;
                }
            }
            if (ca_max_array_bytes_string.isEmpty()) {
                ca_max_array_bytes = 16384;
            } else {
                try {
                    ca_max_array_bytes = Integer.valueOf(
                            ca_max_array_bytes_string);
                } catch (NumberFormatException e) {
                    logger.warning(
                            "Using ca_max_array_bytes = 16384 because "
                                    + "specified value is invalid: "
                                    + ca_max_array_bytes_string);
                    ca_max_array_bytes = 16384;
                }
                if (ca_max_array_bytes < 16384) {
                    logger.warning(
                            "ca_max_array_bytes = "
                                    + ca_max_array_bytes
                                    + " is too small. Using "
                                    + "ca_max_array_bytes = 16384 instead.");
                    ca_max_array_bytes = 16384;
                }
            }
            if (ca_max_search_period_string.isEmpty()) {
                ca_max_search_period = 60.0;
            } else {
                try {
                    ca_max_search_period = Double.valueOf(
                            ca_max_search_period_string);
                } catch (NumberFormatException e) {
                    logger.warning(
                            "Using ca_max_search_period = 60.0 because "
                                    + "specified value is invalid: "
                                    + ca_max_search_period_string);
                    ca_max_search_period = 60.0;
                }
                if (ca_max_search_period < 60.0) {
                    logger.warning(
                            "ca_max_search_period = "
                                    + ca_max_search_period
                                    + " is too small. Using "
                                    + "ca_max_search_period = 60.0 instead.");
                    ca_max_search_period = 60.0;
                }
                if (!Double.isFinite(ca_max_search_period)) {
                    logger.warning(
                            "Using ca_max_search_period = 30.0 because "
                                    + "specified value is invalid: "
                                    + ca_max_search_period);
                    ca_max_search_period = 60.0;
                }
            }
            if (ca_multicast_ttl_string.isEmpty()) {
                ca_multicast_ttl = 1;
            } else {
                try {
                    ca_multicast_ttl = Integer.valueOf(ca_multicast_ttl_string);
                } catch (NumberFormatException e) {
                    logger.warning(
                            "Using ca_multicast_ttl = 1 because specified "
                                    + "value is invalid: "
                                    + ca_multicast_ttl_string);
                    ca_multicast_ttl = 1;
                }
                if (ca_multicast_ttl < 1) {
                    logger.warning(
                            "ca_multicast_ttl = "
                                    + ca_multicast_ttl
                                    + " is too small. Using ca_multicast_ttl "
                                    + "= 1 instead.");
                    ca_multicast_ttl = 1;
                }
                if (ca_multicast_ttl > 255) {
                    logger.warning(
                            "ca_multicast_ttl = "
                                    + ca_multicast_ttl
                                    + " is too large. Using ca_multicast_ttl "
                                    + "= 255 instead.");
                    ca_multicast_ttl = 255;
                }
            }
            if (ca_repeater_port_string.isEmpty()) {
                ca_repeater_port = (
                        ChannelAccessConstants.DEFAULT_REPEATER_PORT);
            } else {
                try {
                    ca_repeater_port = Integer.valueOf(ca_repeater_port_string);
                } catch (NumberFormatException e) {
                    logger.warning(
                            "Using ca_repeater_port = "
                                    + ChannelAccessConstants.DEFAULT_REPEATER_PORT
                                    + " because specified value is invalid: "
                                    + ca_repeater_port_string);
                    ca_repeater_port = (
                            ChannelAccessConstants.DEFAULT_REPEATER_PORT);
                }
                if (ca_repeater_port < 1 || ca_repeater_port > 65535) {
                    logger.warning(
                            "Using ca_repeater_port = "
                                    + ChannelAccessConstants.DEFAULT_REPEATER_PORT
                                    + " because specified value is invalid: "
                                    + ca_repeater_port);
                    ca_repeater_port = (
                            ChannelAccessConstants.DEFAULT_REPEATER_PORT);
                }
            }
            if (ca_server_port_string.isEmpty()) {
                ca_server_port = (
                        ChannelAccessConstants.DEFAULT_SERVER_PORT);
            } else {
                try {
                    ca_server_port = Integer.valueOf(ca_server_port_string);
                } catch (NumberFormatException e) {
                    logger.warning(
                            "Using ca_server_port = "
                                    + ChannelAccessConstants.DEFAULT_SERVER_PORT
                                    + " because specified value is invalid: "
                                    + ca_server_port_string);
                    ca_server_port = (
                            ChannelAccessConstants.DEFAULT_SERVER_PORT);
                }
                if (ca_server_port < 1 || ca_server_port > 65535) {
                    logger.warning(
                            "Using ca_server_port = "
                                    + ChannelAccessConstants.DEFAULT_SERVER_PORT
                                    + " because specified value is invalid: "
                                    + ca_server_port);
                    ca_server_port = (
                            ChannelAccessConstants.DEFAULT_SERVER_PORT);
                }
            }
            // We need the server port setting in order to process the address
            // lists, so we process them last.
            if (ca_address_list_string.isEmpty()) {
                ca_address_list = Collections.emptySet();
            } else {
                ca_address_list = parseAddressList(
                        ca_address_list_string,
                        ca_server_port,
                        "ca_address_list",
                        logger);
            }
            if (ca_name_servers_string.isEmpty()) {
                ca_name_servers = Collections.emptySet();
            } else {
                ca_name_servers = parseAddressList(
                        ca_name_servers_string,
                        ca_server_port,
                        "ca_name_servers",
                        logger);
            }
            // Log all CA related settings. We only do this if use_env is
            // false, because these settings are not used when use_env is true.
            logger.config(
                    "ca_address_list = " + serializeAddressList(
                            ca_address_list, ca_server_port));
            logger.config("ca_auto_address_list = " + ca_auto_address_list);
            logger.config("ca_auto_array_bytes = " + ca_auto_array_bytes);
            logger.config("ca_echo_interval = " + ca_echo_interval);
            logger.config("ca_max_array_bytes = " + ca_max_array_bytes);
            logger.config("ca_max_search_period = " + ca_max_search_period);
            logger.config("ca_multicast_ttl = " + ca_multicast_ttl);
            logger.config(
                    "ca_name_servers = " + serializeAddressList(
                            ca_name_servers, ca_server_port));
            logger.config("ca_repeater_port = " + ca_repeater_port);
            logger.config("ca_server_port = " + ca_server_port);
        }
        logger.config("charset = " + charset.name());
        logger.config("cid_block_reuse_time = " + cid_block_reuse_time);
        logger.config("dbe_property_supported = " + dbe_property_supported);
        logger.config("honor_zero_precision = " + honor_zero_precision);
        logger.config("hostname = " + hostname);
        logger.config("long_conversion_mode = " + long_conversion_mode);
        logger.config("monitor_mask = " + monitor_mask);
        logger.config("rtyp_value_only = " + rtyp_value_only);
        logger.config("use_env = " + use_env);
        logger.config("username = " + username);
        return new JackiePreferences(
                ca_address_list,
                ca_auto_address_list,
                ca_auto_array_bytes,
                ca_echo_interval,
                ca_max_array_bytes,
                ca_max_search_period,
                ca_multicast_ttl,
                ca_name_servers,
                ca_repeater_port,
                ca_server_port,
                charset,
                cid_block_reuse_time,
                dbe_property_supported,
                honor_zero_precision,
                hostname,
                long_conversion_mode,
                monitor_mask,
                rtyp_value_only,
                username);
    }

    private static Set<Pair<Inet4Address, Integer>> parseAddressList(
            final String address_list_string,
            final int default_port,
            final String setting_name,
            final Logger logger) {
        final ErrorHandler error_handler = (context, e, description) -> {
            final String message;
            if (description == null) {
                message = "Error while parsing address list in " + setting_name
                        + ".";
            } else {
                message = "Error while parsing address list in " + setting_name
                        + ": " + description;
            }
            if (e != null) {
                logger.log(Level.WARNING, message, e);
            } else {
                logger.log(Level.WARNING, message);
            }
        };
        final var socket_address_list = Inet4AddressUtil.stringToInet4SocketAddressList(
                address_list_string, default_port, false, error_handler);
        final Set<Pair<Inet4Address, Integer>> addresses = new LinkedHashSet<>();
        for (final var socket_address : socket_address_list) {
            var address = socket_address.getAddress();
            var port = socket_address.getPort();
            // We know that the socket addresses returned by
            // stringToInet4SocketAddressList only use instances of
            // Inet4Address, so we can cast without checking.
            addresses.add(Pair.of((Inet4Address) address, port));
        }
        return addresses;
    }

    private static ChannelAccessEventMask parseMonitorMask(final String mask_string) {
        ChannelAccessEventMask mask = ChannelAccessEventMask.DBE_NONE;
        for (final var token : mask_string.split("\\|")) {
            switch (token.trim()) {
                case "DBE_ALARM" -> mask = mask.setAlarm(true);
                case "DBE_ARCHIVE" -> mask = mask.setArchive(true);
                case "DBE_PROPERTY" -> mask = mask.setProperty(true);
                case "DBE_VALUE" -> mask = mask.setValue(true);
                default -> throw new IllegalArgumentException();
            }
        }
        return mask;
    }

    private static String serializeAddressList(
            final Set<Pair<Inet4Address, Integer>> address_list,
            final int default_port) {
        Function<Pair<Inet4Address, Integer>, String> entry_to_string = (entry) -> {
            var address = entry.getLeft();
            var port = entry.getRight();
            if (port == default_port) {
                return address.getHostAddress();
            } else {
                return address.getHostAddress() + ":" + port;
            }
        };
        return address_list.stream().map(entry_to_string).collect(
                Collectors.joining(" "));
    }

}
