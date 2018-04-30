Alarm System
------------

Update of the alarm system that originally used RDB for configuration,
JMS for updates, RDB for persistence of most recent state.
 
This development uses Kafka to handle both, using "Compacted Topics".
For an "Accelerator" configuration, a topic of that name holds the configuration,
and an "AcceleratorState" topic holds the state changes.
Clients subscribe to both topics.
They receive the most recent configuration and state, and from then on updates. 


Setup
-----

Download Kafka as described on https://kafka.apache.org/quickstart

    # The 'examples' folder of this project contains some example scripts
    # that can be used with a kafka server in the same directory
    cd examples
    wget http://mirrors.gigenet.com/apache/kafka/1.1.0/kafka_2.11-1.1.0.tgz

    tar -vzxf kafka_2.11-1.1.0.tgz
    ln -s kafka_2.11-1.1.0 kafka
    
Check `config/server.properties`. By default it contains this, which "works",
but means data will periodically get deleted by Linux:

    log.dirs=/tmp/kafka-logs
    
Start local instance:
    
    cd kafka
    bin/zookeeper-server-start.sh config/zookeeper.properties
    # Other terminal
    bin/kafka-server-start.sh config/server.properties
    
First steps:

    # Create new topic
    bin/kafka-topics.sh  --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic test
    
    # Topic info
    bin/kafka-topics.sh  --zookeeper localhost:2181 --list
    bin/kafka-topics.sh  --zookeeper localhost:2181 --describe
    bin/kafka-topics.sh  --zookeeper localhost:2181 --describe --topic test
    bin/kafka-configs.sh --zookeeper localhost:2181 --entity-type topics --describe
    
    # Produce messages for topic (no key) 
    bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test
    Message 1
    Message 2
    <Ctrl-D>

    # Trace messages for topic
    bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test --from-beginning
    <Wait until messages are dumped, then Ctrl-C>

    # .. with key:
    bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --property print.key=true --property key.separator=": " --topic test --from-beginning

    # Delete topic
    bin/kafka-topics.sh  --zookeeper localhost:2181 --delete --topic test


Stop local instance:

    bin/kafka-server-stop.sh 
    bin/zookeeper-server-stop.sh
    
For more, see https://kafka.apache.org/documentation.html
    

Configure 'compact' alarm topics
--------------------------------

Fundamentally, a topic with `cleanup.policy=compact` will be compacted to keep "at least the last known value".
In practice, the topic will retain many more messages because the log cleaner only runs
every 15 seconds, it only acts when the ratio of old to new messages is sufficient.
Further, it only cleans inactive segments.
If the last segment contains many messages, they will not be compacted until
writing at least one more message after the "segment.ms" to create a new segment.

More on this in http://www.shayne.me/blog/2015/2015-06-25-everything-about-kafka-part-2/

The following settings request a new segment every 10 seconds with an aggressive cleanup ratio for the 'Accelerator' config topic:

    bin/kafka-topics.sh  --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic Accelerator

    # Configure (existing) topic to be compact, and start a new segment every 10 seconds
    bin/kafka-configs.sh --zookeeper localhost:2181 --entity-type topics --alter --entity-name Accelerator \
           --add-config cleanup.policy=compact,segment.ms=10000,min.cleanable.dirty.ratio=0.01

You can track the log cleaner runs via

    tail -f logs/log-cleaner.log



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
Once loaded, the UI is responsive with the 100000 PV example configuration.

When browsing the alarm tree while the large config is loaded,
it can lock the UI while the internal TreeView code gets to traverse all 'siblings' of a newly added item.
This has been observed if there are 10000 or more siblings, i.e. direct child nodes to one node of the alarm tree.
It can be avoided by for example adding sub-nodes.



Next
----

Alarm Server.

Alarm Table UI.

Alarm Area Panel.

Import/export XML format used by original alarm server into/out of Kafka.

Annunciator.
