PVA Client and Server
=====================

PV Access client and server for Java, based on the 
[PV Access Protocol Description](https://github.com/epics-base/pvAccessCPP/wiki/protocol),
consulting the
[Reference Implementation](https://github.com/epics-base/epicsCoreJava)
to clarify details,
and tracking enhancements of the latest
[C++ implementation](https://github.com/mdavidsaver/pvxs).

Original motivation was understanding the protocol and implementing it based on the standard Java library,
taking advantage of for example functional interfaces and concurrency classes,
instead of requiring API compatibility to the C++ implementation.

Network compatibility with all other PVA servers and clients is desired.

Implementation is focused on the requirements of clients like CS-Studio,
covering the majority of PV Access features but not all of them.
Primary goal is support for the "normative types" as served by IOCs
and images as served by the area detector.
For these, client aims to reach or exceed the CPU and memory performance
of the original Java client implementation.

Also includes a PVA Server implementation, which was mostly created
to again better understand the protocol and to allow closed-loop tests.
A 'proxy' combines server and client into a 'gateway' type application,
again mostly to test if the implementations can handle common data types.
The suggested gateway for production setups is the
[C++ gateway implementation](https://mdavidsaver.github.io/p4p/gw.html).



Prerequisites
-------------
`JAVA_HOME` set to JDK 8 or higher.

While CS-Studio generally targets JDK 9 or higher, the core-pva library
remains for the time being compatible with JDK 8 to allow using it
with MATLAB.

Build
-----
Both Ant and Maven are supported:

    ant clean core-pva
    mvn clean install javadoc:javadoc

Configuration
-------------

Similar to the C++ library, `EPICS_PVA_ADDR_LIST` can be set as an environment variable.
In addition, the Java library reads a Java property of the same name.
If both the environment variable and the Java property are defined,
the latter is used.

Key configuration parameters:

`EPICS_PVA_ADDR_LIST`: Space-separated list of host names or IP addresses. Each may be followed by ":port", otherwise defaulting to `EPICS_PVA_BROADCAST_PORT`.  When empty, local subnet is used.

`EPICS_PVA_AUTO_ADDR_LIST`: 'YES' (default) or 'NO'. 

`EPICS_PVA_NAME_SERVERS`: Space-separated list of TCP name servers, provided as IP address followed by optional ":port". Client will connect to each address and send name searches before using the `EPICS_PVA_ADDR_LIST` for UDP searches.
Set `EPICS_PVA_ADDR_LIST` to empty and `EPICS_PVA_AUTO_ADDR_LIST=NO` to use only the TCP name servers and avoid all UDP traffic. This is a client-side option. Server will always allow search messages via its TCP port.

`EPICS_PVA_BROADCAST_PORT`: PVA client UDP port (default 5076) for sending name searches and receiving beacons.

`EPICS_PVAS_BROADCAST_PORT`: PVA server UDP port (default 5076) for name searches and beacons.

`EPICS_PVAS_INTF_ADDR_LIST`: Interface where server listens to name searches. When empty (default), a wildcard address is used, i.e., server listens on all local interfaces. Can be set to a specific IP address to restrict the server to one interface.

`EPICS_PVA_SERVER_PORT`: First PVA TCP port used by server, defaults to 5075.

See `PVASettings` source code for complete settings.

Network Details
---------------

The protocol uses UDP port 5076, then TCP port 5075 for the first server,
and randomly chosen TCP ports for additional servers on the same host.

By default, IPv4 is used, and local PV name resolution also joins the multicast group 224.0.0.128.
(These defaults can be changed via configuration settings, see `PVASettings`.)

To debug connection issues on Linux, it can be helpful to disable the firewall:

    sudo systemctl stop firewalld

To enable access to the first PVA server on a Linux host and list resulting settings:

    # Depending on Linux release, similar to this..
    sudo firewall-cmd --add-port=5075/tcp
    sudo firewall-cmd --add-port=5076/udp
    
    # .. or this
    sudo firewall-cmd --direct --add-rule ipv4 filter IN_public_allow 0 -m udp -p udp --dport 5076 -j ACCEPT
    sudo firewall-cmd --direct --add-rule ipv4 filter IN_public_allow 0 -m tcp -p tcp --dport 5075 -j ACCEPT
    sudo firewall-cmd --direct --get-rules ipv4 filter IN_public_allow
    
Use `--remove-rule` to revert, add `--permanent` to persist the setting over firewall restarts.

When running more than one PVA server on a host, these use an unpredictable TCP port,
so firewall needs to allow all TCP access.

IPv6 Support
------------

Both the server and client support IPv6, which at this time needs to be enabled
by configuring the `EPICS_PVAS_INTF_ADDR_LIST` of the server respectively the
`EPICS_PVA_ADDR_LIST` and/or `EPICS_PVA_NAME_SERVERS` of the client to provide the desired IPv6 addresses. To completely disable any IPv6 functionality, set `EPICS_PVA_ENABLE_IPV6` to false.

See Javadoc of `EPICS_PVAS_INTF_ADDR_LIST`, , `EPICS_PVA_ADDR_LIST`, `EPICS_PVA_NAME_SERVERS` and `EPICS_PVA_ENABLE_IPV6` in `PVASettings`
for details.

Command-line Example
--------------------

Start the example database: 

    softIocPVA -m N='' -d src/test/resources/demo.db 

Then access it via

    pvaclient
    pvaclient info ramp
    pvaclient get ramp saw
    pvaclient put ramp 5
    pvaclient monitor ramp rnd

To test the server, run `ServerDemo` in the IDE of your choice and then access it via
the tools provided by EPICS base:

    pvinfo demo
    pvget demo
    pvmonitor demo

Protocol Test Tools
-------------------

`pvasearchmonitor` or invoking the phoebus command line with `-main org.epics.pva.server.PVASearchMonitorMain`
starts a tool that periodically lists received search requests.

`pvaclient beacons` or invoking the phoebus command line with `-main org.epics.pva.client.PVAClientMain beacons`
starts a tool that lists received beacons.
 
    
API Documentation
-----------------

Both Ant and Maven generate javadoc in target/site/apidocs/index.html

See `ClientDemo` for an example client, `ServerDemo` for an example server.

Implementation Status
---------------------

PVA Client:

 * PVA Server list
 * Maintains pool of PVs
 * Registers new PVs with ChannelSearch, which supports UDP and TCP searches,
   i.e. search via broadcast/multicast/unicase or via a name server
 * ChannelSearch: Linear backup of 1, 2, 3, .. seconds between repeated 
   searches, setting to searching once every 30 seconds after about 7 minutes
 * Clients monitor beacons. If the search has settled to once every 30 seconds,
   any new beacon restarts the linear backup to facilitate faster reconnect.
   Beacons are not required for a reconnect, but they may accelerate it
 * Forward unicast searches to local multicast group
 * Creates TCPHandler when channel found
 * Support "anonymous" or "ca"
   (with user from "user.name" property and host from InetAddress.getLocalHost().getHostName())
 * Echo test when no new data for a while,
   supporting both V1 (no content) and V2 (testing for matching content in reply)
 * Reset channel to search when TCP connection closed
 * Monitor beacons, boost search for missing channels
 * CreateChannelRequest
 * ChannelListener for notification about connect, disconnect
 * Get: Init, get structure, get value, destroy
 * Monitor: Init, get structure, subscribe, get changes, stop/destroy
 * Put: Init, get structure, update field, write, destroy
 * RPC: Send request structure, get response structure
 * Decode data sent by IOC and 'image' demo
 * Handle 'segmented' messages
 * Close (destroy) channel
 * Close client
 * Info/get/monitor/put command line tool
 * IPv6 support
 
PVA Server:

 * Maintains pool of PVs
 * Responds to searches
 * Forward unicast searches to local multicast group
 * Reply to 'list' search with GUID
 * Reply to 'info'
 * Reply to 'get'
 * Support 'monitor'
 * Support RPC
 * IPv6 support
   
TODO:

 * Testing
 * Implement beacons in server
 * Handle fixed size or bounded arrays?
