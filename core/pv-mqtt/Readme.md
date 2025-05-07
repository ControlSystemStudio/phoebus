MQTT PV Support
===============

MQTT is a broker-based protocol used in the Internet-of-Things (IoT) ecosystem.
See https://mqtt.org for details.
The `mqtt:...` PV support allows CS-Studio tools to read and write PVs via MQTT.


Example Broker Setup and first steps
------------------------------------

This example uses Eclipse Mosquitto on Linux.
See https://mqtt.org for links to several other MQTT brokers and clients. 

Download: Get source release `mosquitto-....tar.gz` from https://mosquitto.org/download

Unpack: `tar vzxf mosquitto-....tar.gz`

Build:  `make WITH_CJSON=no`

Install:

```
export LD_LIBRARY_PATH=`pwd`/lib
# Optionally, add the following to a `bin` folder that's on your $PATH
# src/mosquitto, client/mosquitto_sub, client/mosquitto_pub
```
     
Run broker:

```
# Allow remote access through firewall.
# Depending on Linux release, similar to this
sudo firewall-cmd --add-port=1883/tcp

# Create configuration file that allows remote access
echo "listener 1883"         >> mosquitto.conf
echo "allow_anonymous true"  >> mosquitto.conf

# Start broker with that configuration file
src/mosquitto -c mosquitto.conf
mosquitto version ... starting
Config loaded from mosquitto.conf.
...
Opening ipv4 listen socket on port 1883.
```

See https://mosquitto.org/documentation/authentication-methods
for a more secure configuration.

Subscribe to value updates: `client/mosquitto_sub -t sensors/temperature -q 1 `

Publish a value: `client/mosquitto_pub -t sensors/temperature -q 1 -m 42`

By default, the broker will not persist messages.
The subscribe command shown above will receive all newly
published messages. If you close the `mosquitto_sub` and then restart it,
it will show nothing until a new value is published.

To persist data on the broker, each client that publishes or subscribes
needs to connect with a unique client ID and an option to _not_ 'clean' the session,
using options like `--id 8765 --disable-clean-session` for the `.._sub` and `.._pub`
commands shown above.


MQTT PV Configuration
---------------------

By default, the MQTT PV will look for a broker on localhost
and the default MQTT broker port 1883.

To change this, add a variant of the following to your Phoebus settings:

```
# MQTT Broker
# All "mqtt://some/tag" PVs will use this broker
#org.phoebus.pv.mqtt/mqtt_broker=tcp://localhost:1883
org.phoebus.pv.mqtt/mqtt_broker=tcp://my_host.site.org:1883
```

The MQTT PV will create a unique internal ID to read persisted messages,
allowing the PV to start up with the last known value of an MQTT topic
without need to wait for the next update.


MQTT PV Syntax
--------------

To interface with the example MQTT tag shown above,
use the PV `mqtt://sensors/temperature`.

The general format is `mqtt://` followed by the MQTT topic,
for example `sensors/temperature`,
and an optional `<VType>`.

MQTT treats all tag data as text. By default, an MQTT PV expects
the text to contain a number, but the optional `<VType>` will
instruct the PV to parse the text in other ways.

| `VType`          | PV Value Parser                                                              |
| ---------------- | ---------------------------------------------------------------------------- |
| `<VDouble>`      | This is the default, expecting the topic to parse as a floating point number |
| `<VString>`      | PV reads text as string                                                      |
| `<VLong>`        | Parse as long integer                                                        |
| `<VDoubleArray>` | Parse as array of comma-separated floating point numbers                     |
| `<VStringArray>` | Parse text as array of comma-separated strings                               |

