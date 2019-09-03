PVA Client
==========

PV Access client for Java, based on the 
[PV Access Protocol Description](https://github.com/epics-base/pvAccessCPP/wiki/protocol),
consulting the [Reference Implementation](https://github.com/epics-base/epicsCoreJava)
to clarify details.
Purpose is to better understand the protocol
and to implement it in pure Java, using concurrent classes
from the standard library and taking advantage of for example
functional interfaces because compatibility to the C++ implementation
is not required.
Implementation is focused on the requirements of clients like CS-Studio,
covering the majority of PV Access features but not all of them.

Prerequisites
-------------
`JAVA_HOME` set to JDK 9 or higher

Build
-----
Both Ant and Maven are supported:

    ant clean core-pva
    mvn clean install

Configuration
-------------

`EPICS_PVA_ADDR_LIST` can be set as environment variable
or Java property.
See `PVASettings` source code for additional settings.

Network Details
---------------

The protocol uses UDP port 5076, then TCP port 5075 for first server,
and randomly chosen TCP ports for additional servers on the same host.
Local PV name resolution also uses the multicast group 224.0.0.128.

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

To test the server, run ServerDemo in the IDE of your choice and then access it via
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
 * Server: Support Put
 * Handle fixed size or bounded arrays?
