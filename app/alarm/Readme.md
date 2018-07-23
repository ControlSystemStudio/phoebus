Alarm System
============

Update of the alarm system that originally used RDB for configuration,
JMS for updates, RDB for persistence of most recent state.
 
This development uses Kafka to handle both, using "Compacted Topics".
For an "Accelerator" configuration, a topic of that name holds the configuration,
and an "AcceleratorState" topic holds the state changes.
Clients subscribe to both topics.
They receive the most recent configuration and state, and from then on updates. 


Kafka Installation
------------------

Download Kafka as described on https://kafka.apache.org/quickstart

    # The 'examples' folder of this project contains some example scripts
    # that can be used with a kafka server in the same directory
    cd examples
    
    # Use wget, 'curl -O', or web browser
    wget http://mirrors.gigenet.com/apache/kafka/1.1.0/kafka_2.11-1.1.0.tgz

    tar -vzxf kafka_2.11-1.1.0.tgz
    ln -s kafka_2.11-1.1.0 kafka
    
Check `config/server.properties`. By default it contains this, which "works",
but risks that Linux will delete the data:

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

If you are using Java 10 or newer, the Zookeeper start script may fail when it
checks for the Java Version, because it mistakes Java 10 as Java "1", resulting in the following error:

     kafka/bin/kafka-run-class.sh: line 252: .. syntax error in expression ..

If you are using Java 10, change kafka-run-class.sh line 252 from

     JAVA_MAJOR_VERSION=$($JAVA -version 2>&1 | sed -E -n 's/.* version "([^.-]*).*"/\1/p')
into

     JAVA_MAJOR_VERSION="10"


Start local instance:

    # Zookeeper must be started first.
    sh start_zookeeper.sh

    # Then, in other terminal, start Kafka
    sh start_kafka.sh

    # If kafka is started first, with the default zookeeper.connection.timeout of only 6 seconds,
    # it will fail to start and close with a null pointer exception. 
    # Simply start kafka after zookeeper is running to recover.

Refer to `*.service` scripts for installing Zookeeper and Kafka as Linux services.
    
Kafka Demo
----------

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

The alarm server includes a command-line option to `-create_topics`,
but the `create_alarm_topics.sh` scripts is likely most up-to-date
regarding the recommended settings.
     
If will create these topics:

 * "Accelerator": Alarm configuration (compacted)
 * "AcceleratorState": Alarm state (compacted)
 * "AcceleratorCommand": Commands like "acknowledge" from UI to the alarm server (deleted)
 * "AcceleratorTalk": Annunciations (deleted)
 
The deleted topics are configured to delete older messages, because only new messages are relevant.
 
The compacted topics use `cleanup.policy=compact` to keep "at least the last known value".
In practice, the topic will retain more messages because the log cleaner only runs
every 15 seconds, it only acts when the ratio of old to new messages is sufficient,
and it only cleans inactive segments.

More on this in http://www.shayne.me/blog/2015/2015-06-25-everything-about-kafka-part-2/

You can track the log cleaner runs via

    tail -f logs/log-cleaner.log
    
    
Start Alarm Server
------------------

Run the alarm server product.
For "Accelerator" configuration on localhost, simply start it.
Otherwise run `-help` to see options for importing or exporting configurations,
using other configurations etc.


User Interface
--------------

Open the Alarm Tree to show and edit the configuration.

Open Alarm Table as the primary runtime user interface to see and acknowledge active alarms.


Message Formats
---------------

In general, most messages use the path to the alarm tree item as the `key`.
Messages sent by the UI also include the user name and host name to help identify the origin of the message.

_______________

- Config Topic:

The messages in the config topic consist of a path to the alarm tree item that is being configured along with a JSON of its configuration.
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

    /path/to/pv : {"user":"user name", "host":"host name", "description":"This is a PV. Believe it or not."}

- Deletions in the Config Topic

Deleting an item consists of marking a path with a value of null. This "tombstone" notifies Kafka that when compaction occurs this message can be deleted.

For example:

    /path/to/pv : null
    
This process variable is now marked as deleted. However, there is an issue. We do not know when, why, or by whom it was deleted. To address this, a message including the missing relevant information is sent before the tombstone is set.
This message consists of a user name, host name, and a delete message.
The delete message may offer details on why the item was deleted.

The config delete message JSON format:

    {
        "user":   String,
        "host":   String,
        "delete": String
    }
    
The above example of deleting a PV would then look like this:

    /path/to/pv : {"user":"user name", "host":"host name", "delete": "Deleting"}
    /path/to/pv : null
    
The message about who deleted the PV would obviously be compacted and deleted itself, but it would be aggregated into the long term topic beforehand thus preserving a record of the deletion.
______________
- State Topic:

The messages in the state topic consist of a path to the alarm tree item that's state is being updated along with a JSON of its new state.

The state topic JSON format for an alarm tree leaf:

    {
        "severity": String,
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

The "mode" will contain "maintenance" while the alarm server is in maintenance mode.
For normal operational mode, the "mode" tag is omitted.

An example message that could appear in a state topic:

    /path/to/pv :{"severity":"MAJOR","message":"LOLO","value":"0.0","time":{"seconds":123456789,"nano":123456789},"current_severity":"MINOR","current_message":"LOW"}

________________
- Command Topic:

The messages in the command topic consist of a path to the alarm tree item that is the subject of the command along with a JSON of the command. The JSON always contains the user name and host name of who is issuing the command.

The command topic JSON format:

    {
        "user":    String,
        "host":    String,
        "command": String
    }
    
An example message that could appear in a command topic:

    /path/to/pv : {"user":"user name", "host":"host name", "command":"acknowledge"}

____________
- Talk Topic:

The messages in the talk topic consist of a path to the alarm tree item being referenced along with a JSON. The JSON contains the alarm severity, a boolean value to indicate if the message should always be annunciated, and the message to annunciate.

The talk topic JSON format:

    {
        "severity": String,
        "standout": boolean,
        "talk":     String
    }

An example message that could appear in a talk topic:

    /path/to/pv : {"severity":"MAJOR", "standout":true, "message":"We are out of potato salad!"}


__________

When aggregating all messages into a long-term topic to preserve a history of all alarm system operations over time,
the talk, command and state messages can be identified because they contain a "talk", "command" respectively "severity" element in their value.
The remaining messages are from the configuration topic. Their content can be as short as "null". 

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

The log cleaner often fails to compact the log.
This is an example configuration, as printed by Kafka on startup:

	INFO KafkaConfig values: 
	advertised.host.name = null
	advertised.listeners = null
	advertised.port = null
	alter.config.policy.class.name = null
	alter.log.dirs.replication.quota.window.num = 11
	alter.log.dirs.replication.quota.window.size.seconds = 1
	authorizer.class.name = 
	auto.create.topics.enable = false
	auto.leader.rebalance.enable = true
	background.threads = 10
	broker.id = 0
	broker.id.generation.enable = true
	broker.rack = null
	compression.type = producer
	connections.max.idle.ms = 600000
	controlled.shutdown.enable = true
	controlled.shutdown.max.retries = 3
	controlled.shutdown.retry.backoff.ms = 5000
	controller.socket.timeout.ms = 30000
	create.topic.policy.class.name = null
	default.replication.factor = 1
	delegation.token.expiry.check.interval.ms = 3600000
	delegation.token.expiry.time.ms = 86400000
	delegation.token.master.key = null
	delegation.token.max.lifetime.ms = 604800000
	delete.records.purgatory.purge.interval.requests = 1
	delete.topic.enable = true
	fetch.purgatory.purge.interval.requests = 1000
	group.initial.rebalance.delay.ms = 0
	group.max.session.timeout.ms = 300000
	group.min.session.timeout.ms = 6000
	host.name = 
	inter.broker.listener.name = null
	inter.broker.protocol.version = 1.1-IV0
	leader.imbalance.check.interval.seconds = 300
	leader.imbalance.per.broker.percentage = 10
	listener.security.protocol.map = PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL
	listeners = null
	log.cleaner.backoff.ms = 15000
	log.cleaner.dedupe.buffer.size = 134217728
	log.cleaner.delete.retention.ms = 86400000
	log.cleaner.enable = true
	log.cleaner.io.buffer.load.factor = 0.9
	log.cleaner.io.buffer.size = 524288
	log.cleaner.io.max.bytes.per.second = 1.7976931348623157E308
	log.cleaner.min.cleanable.ratio = 0.5
	log.cleaner.min.compaction.lag.ms = 0
	log.cleaner.threads = 1
	log.cleanup.policy = [delete]
	log.dir = /tmp/kafka-logs
	log.dirs = /tmp/kafka-logs
	log.flush.interval.messages = 9223372036854775807
	log.flush.interval.ms = null
	log.flush.offset.checkpoint.interval.ms = 60000
	log.flush.scheduler.interval.ms = 9223372036854775807
	log.flush.start.offset.checkpoint.interval.ms = 60000
	log.index.interval.bytes = 4096
	log.index.size.max.bytes = 10485760
	log.message.format.version = 1.1-IV0
	log.message.timestamp.difference.max.ms = 9223372036854775807
	log.message.timestamp.type = CreateTime
	log.preallocate = false
	log.retention.bytes = -1
	log.retention.check.interval.ms = 300000
	log.retention.hours = 168
	log.retention.minutes = null
	log.retention.ms = null
	log.roll.hours = 168
	log.roll.jitter.hours = 0
	log.roll.jitter.ms = null
	log.roll.ms = null
	log.segment.bytes = 1000000
	log.segment.delete.delay.ms = 60000
	max.connections.per.ip = 2147483647
	max.connections.per.ip.overrides = 
	max.incremental.fetch.session.cache.slots = 1000
	message.max.bytes = 1000012
	metric.reporters = []
	metrics.num.samples = 2
	metrics.recording.level = INFO
	metrics.sample.window.ms = 30000
	min.insync.replicas = 1
	num.io.threads = 8
	num.network.threads = 3
	num.partitions = 1
	num.recovery.threads.per.data.dir = 1
	num.replica.alter.log.dirs.threads = null
	num.replica.fetchers = 1
	offset.metadata.max.bytes = 4096
	offsets.commit.required.acks = -1
	offsets.commit.timeout.ms = 5000
	offsets.load.buffer.size = 5242880
	offsets.retention.check.interval.ms = 600000
	offsets.retention.minutes = 1440
	offsets.topic.compression.codec = 0
	offsets.topic.num.partitions = 50
	offsets.topic.replication.factor = 1
	offsets.topic.segment.bytes = 104857600
	password.encoder.cipher.algorithm = AES/CBC/PKCS5Padding
	password.encoder.iterations = 4096
	password.encoder.key.length = 128
	password.encoder.keyfactory.algorithm = null
	password.encoder.old.secret = null
	password.encoder.secret = null
	port = 9092
	principal.builder.class = null
	producer.purgatory.purge.interval.requests = 1000
	queued.max.request.bytes = -1
	queued.max.requests = 500
	quota.consumer.default = 9223372036854775807
	quota.producer.default = 9223372036854775807
	quota.window.num = 11
	quota.window.size.seconds = 1
	replica.fetch.backoff.ms = 1000
	replica.fetch.max.bytes = 1048576
	replica.fetch.min.bytes = 1
	replica.fetch.response.max.bytes = 10485760
	replica.fetch.wait.max.ms = 500
	replica.high.watermark.checkpoint.interval.ms = 5000
	replica.lag.time.max.ms = 10000
	replica.socket.receive.buffer.bytes = 65536
	replica.socket.timeout.ms = 30000
	replication.quota.window.num = 11
	replication.quota.window.size.seconds = 1
	request.timeout.ms = 30000
	reserved.broker.max.id = 1000
	sasl.enabled.mechanisms = [GSSAPI]
	sasl.jaas.config = null
	sasl.kerberos.kinit.cmd = /usr/bin/kinit
	sasl.kerberos.min.time.before.relogin = 60000
	sasl.kerberos.principal.to.local.rules = [DEFAULT]
	sasl.kerberos.service.name = null
	sasl.kerberos.ticket.renew.jitter = 0.05
	sasl.kerberos.ticket.renew.window.factor = 0.8
	sasl.mechanism.inter.broker.protocol = GSSAPI
	security.inter.broker.protocol = PLAINTEXT
	socket.receive.buffer.bytes = 102400
	socket.request.max.bytes = 104857600
	socket.send.buffer.bytes = 102400
	ssl.cipher.suites = []
	ssl.client.auth = none
	ssl.enabled.protocols = [TLSv1.2, TLSv1.1, TLSv1]
	ssl.endpoint.identification.algorithm = null
	ssl.key.password = null
	ssl.keymanager.algorithm = SunX509
	ssl.keystore.location = null
	ssl.keystore.password = null
	ssl.keystore.type = JKS
	ssl.protocol = TLS
	ssl.provider = null
	ssl.secure.random.implementation = null
	ssl.trustmanager.algorithm = PKIX
	ssl.truststore.location = null
	ssl.truststore.password = null
	ssl.truststore.type = JKS
	transaction.abort.timed.out.transaction.cleanup.interval.ms = 60000
	transaction.max.timeout.ms = 900000
	transaction.remove.expired.transaction.cleanup.interval.ms = 3600000
	transaction.state.log.load.buffer.size = 5242880
	transaction.state.log.min.isr = 1
	transaction.state.log.num.partitions = 50
	transaction.state.log.replication.factor = 1
	transaction.state.log.segment.bytes = 104857600
	transactional.id.expiration.ms = 604800000
	unclean.leader.election.enable = false
	zookeeper.connect = localhost:2181
	zookeeper.connection.timeout.ms = 6000
	zookeeper.max.in.flight.requests = 10
	zookeeper.session.timeout.ms = 6000
	zookeeper.set.acl = false
	zookeeper.sync.time.ms = 2000



The alarm topics are created by `examples/create_alarm_topics.sh` as follows (output of `examples/list_topics.sh`):

	Topic:Accelerator	PartitionCount:1	ReplicationFactor:1	Configs:cleanup.policy=compact,delete,segment.ms=10000,min.cleanable.dirty.ratio=0.01,delete.retention.ms=100
		Topic: Accelerator	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
	Topic:AcceleratorCommand	PartitionCount:1	ReplicationFactor:1	Configs:cleanup.policy=compact,delete,segment.ms=10000,min.cleanable.dirty.ratio=0.01,delete.retention.ms=100
		Topic: AcceleratorCommand	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
	Topic:AcceleratorState	PartitionCount:1	ReplicationFactor:1	Configs:cleanup.policy=compact,delete,segment.ms=10000,min.cleanable.dirty.ratio=0.01,delete.retention.ms=100
		Topic: AcceleratorState	Partition: 0	Leader: 0	Replicas: 0	Isr: 0

The file `kafka/logs/log-cleaner.log` will often not show any log-cleaner action:
 
	[2018-06-01 14:54:19,322] INFO Starting the log cleaner (kafka.log.LogCleaner)
	[2018-06-01 14:54:19,353] INFO [kafka-log-cleaner-thread-0]: Starting (kafka.log.LogCleaner)

Workaround: Stop alarm server, alarm clients, kafka.
Then start kafka, followed by the alarm server.
Now `kafka/logs/log-cleaner.log` shows periodic compaction:
 
	[2018-06-01 15:00:53,759] INFO Shutting down the log cleaner. (kafka.log.LogCleaner)
	[2018-06-01 15:00:53,759] INFO [kafka-log-cleaner-thread-0]: Shutting down (kafka.log.LogCleaner)
	[2018-06-01 15:00:53,759] INFO [kafka-log-cleaner-thread-0]: Stopped (kafka.log.LogCleaner)
	[2018-06-01 15:00:53,759] INFO [kafka-log-cleaner-thread-0]: Shutdown completed (kafka.log.LogCleaner)
	[2018-06-01 15:01:01,652] INFO Starting the log cleaner (kafka.log.LogCleaner)
	[2018-06-01 15:01:01,682] INFO [kafka-log-cleaner-thread-0]: Starting (kafka.log.LogCleaner)
	[2018-06-01 15:01:16,697] INFO Cleaner 0: Beginning cleaning of log AcceleratorState-0. (kafka.log.LogCleaner)
	[2018-06-01 15:01:16,697] INFO Cleaner 0: Building offset map for AcceleratorState-0... (kafka.log.LogCleaner)
	[2018-06-01 15:01:16,715] INFO Cleaner 0: Building offset map for log AcceleratorState-0 for 1 segments in offset range [0, 414). (kafka.log.LogCleaner)
	[2018-06-01 15:01:16,731] INFO Cleaner 0: Offset map for log AcceleratorState-0 complete. (kafka.log.LogCleaner)
	[2018-06-01 15:01:16,736] INFO Cleaner 0: Cleaning log AcceleratorState-0 (cleaning prior to Fri Jun 01 15:00:00 EDT 2018, discarding tombstones prior to Wed Dec 31 19:00:00 EST 1969)... (kafka.log.LogCleaner)
	[2018-06-01 15:01:16,739] INFO Cleaner 0: Cleaning segment 0 in log AcceleratorState-0 (largest timestamp Fri Jun 01 15:00:00 EDT 2018) into 0, retaining deletes. (kafka.log.LogCleaner)
	[2018-06-01 15:01:16,809] INFO Cleaner 0: Swapping in cleaned segment 0 for segment(s) 0 in log AcceleratorState-0 (kafka.log.LogCleaner)
	[2018-06-01 15:01:16,811] INFO [kafka-log-cleaner-thread-0]: 
		Log cleaner thread 0 cleaned log AcceleratorState-0 (dirty section = [0, 0])
		0.1 MB of log processed in 0.1 seconds (0.8 MB/sec).
		Indexed 0.1 MB in 0.0 seconds (2.5 Mb/sec, 31.3% of total time)
		Buffer utilization: 0.0%
		Cleaned 0.1 MB in 0.1 seconds (1.1 Mb/sec, 68.8% of total time)
		Start size: 0.1 MB (414 messages)
		End size: 0.1 MB (380 messages)
		8.9% size reduction (8.2% fewer messages)
	 (kafka.log.LogCleaner)

