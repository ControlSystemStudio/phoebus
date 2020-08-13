PVA Client and Server
=====================

PV Access client and server for Java, based on the 
[PV Access Protocol Description](https://github.com/epics-base/pvAccessCPP/wiki/protocol),
consulting the
[Reference Implementation](https://github.com/epics-base/epicsCoreJava)
to clarify details.

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


Prerequisites
-------------
`JAVA_HOME` set to JDK 8 or higher.

While Phoebus generally targets JDK 9 or higher, the core-pva library
remains for the time being compatible with JDK 8 to allow using it
with MATLAB.

Build
-----
Both Ant and Maven are supported:

    ant clean core-pva
    mvn clean install javadoc:javadoc
    

API Documentation
-----------------

Both Ant and Maven generate javadoc in target/site/apidocs/index.html

Configuration
-------------

Similar to the C++ library, `EPICS_PVA_ADDR_LIST` can be set as an environment variable.
In addition, the Java library reads a Java property of the same name.
If both the environment variable and the Java property are defined,
the latter is used.
See `PVASettings` source code for additional settings.

Network Details
---------------

The protocol uses UDP port 5076, then TCP port 5075 for first server,
and randomly chosen TCP ports for additional servers on the same host.
Local PV name resolution also uses the multicast group 224.0.0.128.
(These defaults can be changed via configuration settings, see `PVASettings`.)

To debug connection issues on Linux, it can be helpful to disable the firewall:

    sudo systemctl stop firewalld

To enable access to the first PVA server on a Linux host and list resulting settings:

    sudo firewall-cmd --direct --add-rule ipv4 filter IN_public_allow 0 -m udp -p udp --dport 5076 -j ACCEPT
    sudo firewall-cmd --direct --add-rule ipv4 filter IN_public_allow 0 -m tcp -p tcp --dport 5075 -j ACCEPT
    sudo firewall-cmd --direct --get-rules ipv4 filter IN_public_allow
    
Use `--remove-rule` to revert, add `--permanent` to persist the setting over firewall restarts.

When running more than one PVA server on a host, these use an unpredictable TCP port,
so firewall needs to allow all TCP access.

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


API
---

See `ClientDemo` for client, `ServerDemo` for server.

Implementation Status
---------------------

PVA Client:

 * PVA Server list
 * Maintains pool of PVs
 * Registers new PVs with ChannelSearch
 * ChannelSearch: Exponential backup to ~30 seconds
 * Forward unicast searches to local multicast group
 * Creates TCPHandler when channel found.
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
 
PVA Server:

 * Maintains pool of PVs
 * Responds to searches
 * Forward unicast searches to local multicast group
 * Reply to 'list' search with GUID
 * Reply to 'info'
 * Reply to 'get'
 * Support 'monitor'
 * Support RPC
   
TODO:

 * Testing
 * Handle fixed size or bounded arrays?
