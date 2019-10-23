Alarm System
============

Update of the alarm system that originally used RDB for configuration,
JMS for updates, RDB for persistence of most recent state.
 
This development uses Kafka to handle both, using "Compacted Topics".
For an "Accelerator" configuration, a topic of that name holds the configuration and state changes.
When clients subscribe, they receive the most recent configuration and state, and from then on updates. 


Kafka Installation
------------------

Download Kafka as described on https://kafka.apache.org/quickstart.

The following describes a setup under the `examples` folder
of the source tree, which allows using the other scripts in examples
for a quick start.
A production setup on for example Linux would typically install
kafka in `/opt/kafka`.

    # The 'examples' folder of this project contains some example scripts
    # that can be used with a kafka server in the same directory
    cd examples
    
    # Use wget, 'curl -O', or web browser
    wget http://ftp.wayne.edu/apache/kafka/2.3.0/kafka_2.12-2.3.0.tgz
    tar -vzxf kafka_2.12-2.3.0.tgz
    ln -s kafka_2.12-2.3.0 kafka
     
Check `config/zookeeper.properties` and `config/server.properties`.
By default these contain settings for keeping data in `/tmp/`, which works for initial tests,
but risks that Linux will delete the data.
For a production setup, change `zookeeper.properties`:

    # Suggest to change this to a location outside of /tmp,
    # for example /var/zookeeper-logs or /home/controls/zookeeper-logs
    dataDir=/tmp/zookeeper
  
Similarly, change the directory setting in `server.properties`
    
    # Suggest to change this to a location outside of /tmp,
    # for example /var/kafka-logs or /home/controls/kafka-logs
    log.dirs=/tmp/kafka-logs


Kafka depends on Zookeeper. By default, Kafka will quit if it cannot connect to Zookeeper within 6 seconds.
When the Linux host boots up, this may not be long enough to allow Zookeeper to start.

	# Timeout in ms for connecting to zookeeper defaults to 6000ms.
	# Suggest a much longer time (5 minutes)
	zookeeper.connection.timeout.ms=300000

By default, Kafka will automatically create topics.
This means you could accidentally start an alarm server for a non-existing configuration.
The Kafka topics will automatically be created, but they will lack the desired
settings for compaction etc.
Best disable auto-topic-creation, create topics on purpose with the correct settings,
and have alarm tools that try to access a non-existing configuration fail.

    # Suggest to add this to prevent automatic topic creation,
	auto.create.topics.enable=false

If the following "First steps" generate errors of the type

    WARN Error while fetching metadata with correlation id 39 : .. LEADER_NOT_AVAILABLE
    
then define the host name in  `config/server.properties`.
For tests, you can use localhost:

    listeners=PLAINTEXT://localhost:9092
    advertised.host.name = localhost
    advertised.listeners=PLAINTEXT://localhost:9092

Start local instance. The following describes a manual startup which is useful
for initial tests:

    # Zookeeper must be started first.
    sh start_zookeeper.sh

    # Then, in other terminal, start Kafka
    sh start_kafka.sh

    # If kafka is started first, with the default zookeeper.connection.timeout of only 6 seconds,
    # it will fail to start and close with a null pointer exception. 
    # Simply start kafka after zookeeper is running to recover.


For a Linux-based production setup, consider using the `*.service` scripts
for running Zookeeper, Kafka and the alarm server as Linux services:

    sudo cp *.service /etc/systemd/system

    # Start manually
    sudo systemctl start zookeeper.service
    sudo systemctl start kafka.service
    sudo systemctl start alarm_server.service

    # Enable startup when host boots
    sudo systemctl enable zookeeper.service
    sudo systemctl enable kafka.service
    sudo systemctl enable alarm_server.service

    
Kafka Demo
----------

This is a Kafka message demonstration using a 'test' topic.
It is not required for the alarm system setup
but simply meant to learn about Kafka or to test connectivity.

    # Create new topic
    kafka/bin/kafka-topics.sh  --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic test
    
    # Topic info
    kafka/bin/kafka-topics.sh  --zookeeper localhost:2181 --list
    kafka/bin/kafka-topics.sh  --zookeeper localhost:2181 --describe
    kafka/bin/kafka-topics.sh  --zookeeper localhost:2181 --describe --topic test
    kafka/bin/kafka-configs.sh --zookeeper localhost:2181 --entity-type topics --describe
    
    # Produce messages for topic (no key) 
    kafka/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test
    Message 1
    Message 2
    <Ctrl-D>

    # Trace messages for topic
    kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test --from-beginning
    <Wait until messages are dumped, then Ctrl-C>

    # .. with key:
    kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --property print.key=true --property key.separator=": " --topic test --from-beginning

    # Delete topic
    kafka/bin/kafka-topics.sh  --zookeeper localhost:2181 --delete --topic test


Stop local instance:

    # Either <Ctrl-C> in the kafka terminal, then in the zookeeper terminal
    
    # Or:
    sh stop_all.sh
    
For more, see https://kafka.apache.org/documentation.html


Configure Alarm Topics
----------------------

Run this to create the topics for an "Accelerator" configuration:

     sh create_alarm_topics.sh Accelerator

It will create these topics:

 * "Accelerator": Alarm configuration and state (compacted)
 * "AcceleratorCommand": Commands like "acknowledge" from UI to the alarm server (deleted)
 * "AcceleratorTalk": Annunciations (deleted)
 
The command messages are unidirectional from the alarm UI to the alarm server.
The talk messages are unidirectional from the alarm server to the alarm annunciator.
Both command and talk topics are configured to delete older messages, because only new messages are relevant.

The primary topic is bidirectional. The alarm server receives configuration messages
and sends state updates. Both types of messages are combined in one topic to assert
that their order is preserved. If they were sent via separate, unidirectional topics,
both the server and the client would need to merge the messages based on their time
stamp, which adds unnecessary complexity. Instead, we communicate both configuration
and state updates in one topic, distinguishing the message type via the `key` as
described below.
The primary topic is compacted, i.e. it uses `cleanup.policy=compact` to keep
"at least the last known value".
In practice, the topic will retain more messages because the log cleaner only runs
every 15 seconds, it only acts when the ratio of old to new messages is sufficient,
and it only cleans inactive segments.

More on this in http://www.shayne.me/blog/2015/2015-06-25-everything-about-kafka-part-2/

You can track the log cleaner runs via

    tail -f logs/log-cleaner.log
    
    
Start Alarm Server
------------------

You need to run one alarm server for each configuration.
For the "Accelerator" configuration on localhost, simply start the alarm server service.
Otherwise run `-help` to see options for selecting another configuration.

In the alarm server console you can view the configuration
and for example list active alarms or disconnected PVs.
To edit the configuration use either the Alarm Tree GUI,
or the alarm server `-export` and `-import` command line options,
which create respectively read an XML-based configuration file.


User Interface
--------------

Open the Alarm Tree to show and edit the configuration.

Open Alarm Table as the primary runtime user interface to see and acknowledge active alarms.

Based on preference settings, the alarm user interface can be configured to
always look at just one alarm configuration, for example "Accelerator",
or it can be configured to select between several alarm confirgurations at runtime.


Message Formats
---------------

All messages use a `key` that starts with the message type, followed by
the path to the alarm tree item.
Messages sent by the UI also include the user name and host name to help identify the origin of the message.

_______________

- Type `config:`, Config Topic:

The messages in the config topic consist of a path to the alarm tree item that is being configured along with a JSON of its configuration.
Example key:

    config:/Accelerator/Vacuum/SomePV
    
The message always contains the user name and host name of who is changing the configuration. 

The full config topic JSON format for a alarm tree leaf:

    {
        "user":        String,
        "host":        String,
        "description": String,
        "delay":       Integer,
        "count":       Integer,
        "filter":      String,
        "guidance": [{"title": String, "details": String}],
        "displays": [{"title": String, "details": String}],
        "commands": [{"title": String, "details": String}],
        "actions":  [{"title": String, "details": String}]
    }

The full config topic JSON format for a alarm tree node:

    {
        "user":        String,
        "host":        String,
        "guidance": [{"title": String, "details": String}],
        "displays": [{"title": String, "details": String}],
        "commands": [{"title": String, "details": String}],
        "actions":  [{"title": String, "details": String}]
    }

The configuration of an alarm tree leaf, i.e. a PV, will
always contain the "description", but otherwise the actual JSON
format will only contain the used elements.

For example, a PV that has no guidance, displays, commands, actions will look like this:

    config:/path/to/pv : {"user":"user name", "host":"host name", "description":"This is a PV. Believe it or not."}

- Deletions in the Config Topic

Deleting an item consists of marking a path with a value of null. This "tombstone" notifies Kafka that when compaction occurs this message can be deleted.

For example:

    config:/path/to/pv : null
    
This process variable is now marked as deleted. However, there is an issue. We do not know why, or by whom it was deleted. To address this, a message including the missing relevant information is sent before the tombstone is set.
This message consists of a user name, host name, and a delete message.
The delete message may offer details on why the item was deleted.

The config delete message JSON format:

    {
        "user":   String,
        "host":   String,
        "delete": String
    }
    
The above example of deleting a PV would then look like this:

    config:/path/to/pv : {"user":"user name", "host":"host name", "delete": "Deleting"}
    config:/path/to/pv : null
    
The message about who deleted the PV would obviously be compacted and deleted itself, but it would be aggregated into the long term topic beforehand thus preserving a record of the deletion.
______________
- Type `state:`, State Topic:

The messages in the state topic consist of a path to the alarm tree item that's state is being updated along with a JSON of its new state.

The state topic JSON format for an alarm tree leaf:

    {
        "severity": String,
        "latch": Boolean,
        "message":  String,
        "value":    String,
        "time": {
                    "seconds": Long,
                    "nano":    Long
                },
        "current_severity": String,
        "current_message":  String
        "mode":     String,
    }

The state topic JSON format for an alarm tree node:

    {
        "severity": String
        "mode":     String,
    }

At minimum, state updates this always contain a "severity". 

The "latch" entry will only be present when an alarm that
is configured to latch is actually latching, i.e. entering an alarm severity
at which it will stay until acknowledged.

The "mode" will contain "maintenance" while the alarm server is in maintenance mode.
For normal operational mode, the "mode" tag is omitted.

Example messages that could appear in a state topic:

    state:/path/to/pv :{"severity":"MAJOR","latch":true,"message":"LOLO","value":"0.0","time":{"seconds":123456789,"nano":123456789},"current_severity":"MAJOR","current_message":"LOLO"}
    state:/path/to/pv :{"severity":"MAJOR","message":"LOLO","value":"0.0","time":{"seconds":123456789,"nano":123456789},"current_severity":"MINOR","current_message":"LOW"}

In this example, the first message is issued when the alarm latches to the MAJOR severity.
The following update indicates that the PV's current severity dropped to MINOR, while the alarm severity, message, time and value
continue to reflect the latched state.
 
________________
- Type `command:`, Command Topic:

The messages in the command topic consist of a path to the alarm tree item that is the subject of the command along with a JSON of the command. The JSON always contains the user name and host name of who is issuing the command.

The command topic JSON format:

    {
        "user":    String,
        "host":    String,
        "command": String
    }
    
An example message that could appear in a command topic:

    command:/path/to/pv : {"user":"user name", "host":"host name", "command":"acknowledge"}

____________
- Type `talk:`, Talk Topic:

The messages in the talk topic consist of a path to the alarm tree item being referenced along with a JSON. The JSON contains the alarm severity, a boolean value to indicate if the message should always be annunciated, and the message to annunciate.

The talk topic JSON format:

    {
        "severity": String,
        "standout": boolean,
        "talk":     String
    }

An example message that could appear in a talk topic:

    talk:/path/to/pv : {"severity":"MAJOR", "standout":true, "message":"We are out of potato salad!"}


__________

When aggregating all messages into a long-term topic to preserve a history of all alarm system operations over time,
the talk, command and state messages can be identified by the type code at the start of the message key,
i.e. `config:` for configuration, `state:` for state, `command:` for commands (actions) or `talk:` for talk messages.

__________________
Demos
-----

`examples/create_alarm_topics.sh Accelerator`
Run to create the topics used by the following demos.

`AlarmConfigProducerDemo`: Run to create demo configuration.
Loading a demo config with a total of 100000 PVs arranged into several sub and sub-sub sections
takes less than 5 seconds.
(A 2011 test of the RDB-based alarm system needed about 5 minutes to load a setup with 50000 PVs)

`AlarmClientModelDemo`: Run to dump the demo configuration

`AlarmStateProducerDemo`: Run to generate fake alarm state updates.
May run concurrently with `AlarmTreeUIDemo` to show the updates.

`AlarmTreeUIDemo`: Run to show alarm tree and state updates.
Can be used to configure the alarm tree.

Loads the 100k PV example in about 10 seconds.
Once loaded, the UI is responsive with the 100000 PV example configuration,
including about 500 alarm updates per second, generated by 1000 PVs that change state every 2 seconds.
(A 2011 test used about 10 alarms per second average, generated as 400 alarm changes every 40 seconds).

When browsing the alarm tree while the large config is loaded,
it can lock the UI while the internal TreeView code gets to traverse all 'siblings' of a newly added item.
This has been observed if there are 10000 or more siblings, i.e. direct child nodes to one node of the alarm tree.
It can be avoided by for example adding sub-nodes.



Issues
------

In earlier versions of Kafka, the log cleaner sometimes failed to compact the log.
The file `kafka/logs/log-cleaner.log` would not show any log-cleaner action.
The workaround was to top the alarm server, alarm clients, kafka, then restart them.
When functional, the file `kafka/logs/log-cleaner.log` shows periodic compaction like this:
 
	[2018-06-01 15:01:01,652] INFO Starting the log cleaner (kafka.log.LogCleaner)
	[2018-06-01 15:01:16,697] INFO Cleaner 0: Beginning cleaning of log Accelerator-0. (kafka.log.LogCleaner)
    ...
		Start size: 0.1 MB (414 messages)
		End size: 0.1 MB (380 messages)
		8.9% size reduction (8.2% fewer messages)
	
