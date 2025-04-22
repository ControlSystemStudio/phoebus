/*******************************************************************************
 * Copyright (c) 2019-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva;

import java.util.logging.Level;
import java.util.logging.Logger;

/** PVA Settings
 *
 *  <p>Settings are first read from Java properties.
 *  If not defined, environment variables are checked.
 *  Falling back to defaults defined in here.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVASettings
{
    /** Common logger */
    public static final Logger logger = Logger.getLogger(PVASettings.class.getPackage().getName());

    /** Address list.
     *
     *  <p>May contain space-separated host names or IP addresses.
     *  Each may be followed by ":port", otherwise defaulting to EPICS_PVA_BROADCAST_PORT.
     *  When empty, local IPv4 subnet is used.
     *
     *  <p>When configuring EPICS_PVA_ADDR_LIST, it is suggested to also set
     *  EPICS_PVA_AUTO_ADDR_LIST=NO to assert that only the indented configuration
     *  is used without automatically added items which might not be desired.
     *
     *  <p>Example entries:
     *
     *  <pre>
     *  192.168.10.20              Search via unicast to IPv4 address
     *  192.168.10.255             Search via broadcast to IPv4 subnet
     *  192.168.10.255:9876         .. on port 9876 instead of EPICS_PVA_BROADCAST_PORT
     *  224.0.2.3,255@192.168.1.1  Search via multicast to IPv4 224.0.2.3, TTL 255, using interface with address 192.168.1.1
     *
     *  ::1                        Search via IPv6 unicast to localhost
     *  [::1]                      Same
     *  [::1]:9876                 Same with non-standard port
     *  [ff02::42:1]@eth2          Search via multicast to IPv6 ff02::42:1, interface eth2
     *  [ff02::42:1]:9876,10@eth2  Same, but specify port 9876 and TTL 10
     *  </pre>
     *
     *  IPv6 support is enabled by simply listing one or more IPv6 addresses.
     *  Both IPv4 and IPv6 addresses can be listed at the same time.
     *  The IPv6 multicast `[ff02::42:1]` effectively replaces the IPv4 broadcast,
     *  but note that a network interface must be provided via
     *  `[ff02::42:1]@iface`, the client will not automatically multicast
     *  on each network interface.
     */
    public static String EPICS_PVA_ADDR_LIST = "";

    /** Add local IPv4 broadcast addresses to addr list?
     *
     *  <p>Should be set to 'NO' (false) whenever EPICS_PVA_ADDR_LIST
     *  is configured.
     *
     *  <p>When setting the environment variable or java property,
     *  values 'YES' or 'NO' are used instead of 'true' and 'false'
     *  for compatibility with EPICS base usage of these environment
     *  variables.
     */
    public static boolean EPICS_PVA_AUTO_ADDR_LIST = true;

    /** List of TCP name servers
     *
     *  Space separated list of addresses, each with optional port.
     *  To search for channels, client will connect to each one via TCP
     *  and send the search request.
     *
     *  <p>Example entries:
     *
     *  <pre>
     *  192.168.10.20              Send name lookups to that IPv4 TCP address at EPICS_PVA_SERVER_PORT (default 5075)
     *  ::1                        Search to IPv6 localhost at EPICS_PVA_SERVER_PORT
     *  [::1]:9876                 Same with non-standard port
     *  pvas://192.168.10.20       Use TLS, defaulting to EPICS_PVAS_TLS_PORT (5076)
     *  pvas://192.168.10.20:5086  Use TLS with specific port
     *  </pre>
     */
    public static String EPICS_PVA_NAME_SERVERS = "";

    /** PVA client port for sending name searches and receiving beacons */
    public static int EPICS_PVA_BROADCAST_PORT = 5076;

    /** First PVA port used by plain TCP server */
    public static int EPICS_PVA_SERVER_PORT = 5075;

    /** First PVA port used by TLS server */
    public static int EPICS_PVAS_TLS_PORT = 5076;

    /** Local addresses to which server will listen.
     *
     *  <p>First must be an IPv4 and/or IPv6 address that enables
     *  support for that protocol family.
     *  There can be at most one address for each protocol family.
     *
     *  <p>Options for IPv4:
     *  <pre>
     *  0.0.0.0   - Listen to unicasts or broadcasts on any interface
     *  127.0.0.1 - Listen on a specific address
     *  </pre>
     *
     *  <p>Options for IPv6:
     *  <pre>
     *  ::         - Listen on any interface
     *  [::]       - Listen on any interface
     *  ::1        - Listen on localhost
     *  [::1]:9876 - Listen on localhost but use non-default port
     *  [fe80:8263:4a27:9ef1%en0] - Listen on a specific address
     *  </pre>
     *  The square brackets are optional unless a port is provided.
     *
     *  <p>Next, multicast groups may be added.
     *  Each multicast group must include an interface.
     *  <pre>
     *  224.0.1.1,1@127.0.0.1     - Listen to local IPv4 multicasts
     *  [ff02::42:1],1@::1        - Listen to local IPv6 multicasts
     *  [ff02::42:1],1@en1        - Listen to IPv6 multicasts on network interface en1
     *  </pre>
     */
    public static String EPICS_PVAS_INTF_ADDR_LIST = "0.0.0.0 [::] 224.0.1.1,1@127.0.0.1 [ff02::42:1],1@::1";

    /** PVA server port for name searches and beacons */
    public static int EPICS_PVAS_BROADCAST_PORT = EPICS_PVA_BROADCAST_PORT;

    /** Multicast address used for the local re-send of IPv4 unicasts */
    public static String EPICS_PVA_MULTICAST_GROUP = "224.0.0.128";

    /** Path to PVA server keystore and truststore, a PKCS12 file that contains server's public and private key
     *  as well as trusted CAs that are used to verify client certificates.
     *
     *  <p>Format: "/path/to/file;password".
     *
     *  <p>When empty, PVA server does not support secure (TLS) communication.
     */
    public static String EPICS_PVAS_TLS_KEYCHAIN = "";

    /** Secure server options
     *
     *  <ul>
     *  <li><code>client_cert=optional</code>:
     *      Default; clients can provide certificate for "X509" authentication,
     *      but may also use "ca" or "anonymous" authentication
     *  <li><code>client_cert=require</code>:
     *      Clients must provide certificate for "X509" authentication.
     *      Socket with otherwise be closed during initial handshake.
     *      Server will log "SSLHandshakeException: Empty client certificate chain",
     *      client will log "SSLHandshakeException: Received fatal alert: bad_certificate"
     *  </ul>
     */
    public static String EPICS_PVAS_TLS_OPTIONS = "";

    /** Does EPICS_PVAS_TLS_OPTIONS contain "client_cert=require"? */
    public static boolean require_client_cert;

    /** Path to PVA client keystore and truststore, a PKCS12 file that contains the certificates or root CA
     *  that the client will trust when verifying a server certificate,
     *  and optional client certificate used with x509 authentication to establish the client's name.
     *
     *  <p>Format: "/path/to/file;password".
     *
     *  <p>When empty, PVA client does not support secure (TLS) communication.
     *  When configured, PVA client can reply to PVA servers that offer "tls" in a search reply,
     *  and searches via EPICS_PVA_NAME_SERVERS will also use TLS.
     */
    public static String EPICS_PVA_TLS_KEYCHAIN = "";

    /** TCP buffer size for sending data
     *
     *  <p>Messages are constructed within this buffer,
     *  so it needs to be pre-configured to hold the maximum
     *  package size.
     */
    // 1 million 'double' plus some protocol overhead
    public static int EPICS_PVA_SEND_BUFFER_SIZE = 8001000;

    /** Initial TCP buffer size for receiving data
     *
     *  <p>Buffer grows when larger packages are received.
     */
    public static final int EPICS_PVA_RECEIVE_BUFFER_SIZE = 16 * 1024;

    /** UDP maximum send message size (for sending search requests).
     *
     *  <p>MAX_UDP: 1500 (max of ethernet and 802.{2,3} MTU) - 20/40(IPv4/IPv6) - 8(UDP) - some reserve (e.g. IPSEC)
     * (the MTU of Ethernet is currently independent of its speed variant)
     */
    public static final int MAX_UDP_UNFRAGMENTED_SEND = 1440;

    /** UDP maximum receive message size.
     *
     *  <p>MAX_UDP: 65535 (max UDP packet size) - 20/40(IPv4/IPv6) - 8(UDP)
     */
    public static final int MAX_UDP_PACKET = 65487;

    /** (Initial) TCP buffer size */
    public static final int TCP_BUFFER_SIZE = 1024 * 16;

    /** Connection timeout [seconds]
     *
     * <p>When approaching this time without having received a new value,
     * an 'Echo' request is sent. If still no reply, the channel is disconnected.
     */
    public static int EPICS_PVA_CONN_TMO = 30;

    /** Socket timeout for TCP connections
     *
     * TCP socket creation timeout in seconds
     */
    public static int EPICS_PVA_TCP_SOCKET_TMO = 5;

    /** Maximum number of array elements shown when printing data */
    public static int EPICS_PVA_MAX_ARRAY_FORMATTING = 256;

    /** Range of beacon periods in seconds recognized as "fast, new" beacons
     *  that re-start searches for disconnected channels.
     *
     *  <p>The first beacon received from a server has a nominal period of 0.
     *  Newly started servers send beacons every ~15 seconds.
     *  After about 5 minutes they relax the beacon period to ~180 seconds.
     *
     *  <p>The criteria for restarting searches is min &lt;= period &lt; max.
     *  A min..max range of 0..30 recognizes the initial and ~15 second beacons
     *  as new.
     *  A range of 1..30 would ignore the initial beacon but re-start searches
     *  on 15 sec beacons.
     *  A min..max range of 0..0 would disable the re-start of searches.
     */
    public static int EPICS_PVA_FAST_BEACON_MIN = 0,
                      EPICS_PVA_FAST_BEACON_MAX = 30;

    /** Maximum age of beacons in seconds
     *
     *  <p>Beacon information older than this,
     *  i.e. beacons that have not been received again
     *  for this time are removed from the cache to preserve
     *  memory. If they re-appear, they will be considered
     *  new beacons
     */
    public static int EPICS_PVA_MAX_BEACON_AGE = 300;



    /** Whether to allow PVA to use IPv6 
     *
     *  <p> If this is false then PVA will not attempt to 
     *   use any IPv6 capability at all. This is useful if your
     *   system does not have any IPv6 support.  
     */
    public static boolean EPICS_PVA_ENABLE_IPV6 = true;

    static
    {
        EPICS_PVA_ADDR_LIST = get("EPICS_PVA_ADDR_LIST", EPICS_PVA_ADDR_LIST);
        EPICS_PVA_AUTO_ADDR_LIST = get("EPICS_PVA_AUTO_ADDR_LIST", EPICS_PVA_AUTO_ADDR_LIST);
        EPICS_PVA_NAME_SERVERS = get("EPICS_PVA_NAME_SERVERS", EPICS_PVA_NAME_SERVERS);
        EPICS_PVA_SERVER_PORT = get("EPICS_PVA_SERVER_PORT", EPICS_PVA_SERVER_PORT);
        EPICS_PVAS_TLS_PORT = get("EPICS_PVAS_TLS_PORT", EPICS_PVAS_TLS_PORT);
        EPICS_PVAS_INTF_ADDR_LIST = get("EPICS_PVAS_INTF_ADDR_LIST", EPICS_PVAS_INTF_ADDR_LIST).trim();
        EPICS_PVA_BROADCAST_PORT = get("EPICS_PVA_BROADCAST_PORT", EPICS_PVA_BROADCAST_PORT);
        EPICS_PVAS_BROADCAST_PORT = get("EPICS_PVAS_BROADCAST_PORT", EPICS_PVAS_BROADCAST_PORT);
        EPICS_PVA_CONN_TMO = get("EPICS_PVA_CONN_TMO", EPICS_PVA_CONN_TMO);
        EPICS_PVA_TCP_SOCKET_TMO = get("EPICS_PVA_TCP_SOCKET_TMO", EPICS_PVA_TCP_SOCKET_TMO);
        EPICS_PVA_MAX_ARRAY_FORMATTING = get("EPICS_PVA_MAX_ARRAY_FORMATTING", EPICS_PVA_MAX_ARRAY_FORMATTING);
        EPICS_PVAS_TLS_KEYCHAIN = get("EPICS_PVAS_TLS_KEYCHAIN", EPICS_PVAS_TLS_KEYCHAIN);
        EPICS_PVAS_TLS_OPTIONS = get("EPICS_PVAS_TLS_OPTIONS", EPICS_PVAS_TLS_OPTIONS);
        require_client_cert =  EPICS_PVAS_TLS_OPTIONS.contains("client_cert=require");
        EPICS_PVA_TLS_KEYCHAIN = get("EPICS_PVA_TLS_KEYCHAIN", EPICS_PVA_TLS_KEYCHAIN);
        EPICS_PVA_SEND_BUFFER_SIZE = get("EPICS_PVA_SEND_BUFFER_SIZE", EPICS_PVA_SEND_BUFFER_SIZE);
        EPICS_PVA_FAST_BEACON_MIN = get("EPICS_PVA_FAST_BEACON_MIN", EPICS_PVA_FAST_BEACON_MIN);
        EPICS_PVA_FAST_BEACON_MAX = get("EPICS_PVA_FAST_BEACON_MAX", EPICS_PVA_FAST_BEACON_MAX);
        EPICS_PVA_MAX_BEACON_AGE = get("EPICS_PVA_MAX_BEACON_AGE", EPICS_PVA_MAX_BEACON_AGE);
        EPICS_PVA_ENABLE_IPV6 = get("EPICS_PVA_ENABLE_IPV6", EPICS_PVA_ENABLE_IPV6);
    }

    /** Get setting from property, environment or default
     *  @param name Name of setting
     *  @param default_value Default value
     *  @return Effective value
     */
    public static String get(final String name, final String default_value)
    {
        String value = System.getProperty(name);
        if (value != null)
        {
            logger.log(Level.CONFIG, name + " = " + value + " (from property)");
            return value;
        }
        value = System.getenv(name);
        if (value != null)
        {
            logger.log(Level.CONFIG, name + " = " + value + " (from environment)");
            return value;
        }
        logger.log(Level.CONFIG, name + " = " + default_value + " (default)");
        return default_value;
    }

    /** Get setting from property, environment or default
     *  @param name Name of setting
     *  @param default_value Default value
     *  @return Effective value
     */
    public static boolean get(final String name, final boolean default_value)
    {
        String text = get(name, default_value ? "YES" : "NO").toLowerCase();
        return Boolean.parseBoolean(text)  ||  "yes".equals(text);
    }

    /** Get setting from property, environment or default
     *  @param name Name of setting
     *  @param default_value Default value
     *  @return Effective value
     */
    public static int get(final String name, final int default_value)
    {
        return Integer.parseInt(get(name, Integer.toString(default_value)));
    }
}
