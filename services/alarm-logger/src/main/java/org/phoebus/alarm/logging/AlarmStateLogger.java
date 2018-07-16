package org.phoebus.alarm.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class AlarmStateLogger implements Runnable {

    private final String topic;
    Map<String, Object> serdeProps;
    final Serializer<AlarmStateMessage> alarmStateMessageSerializer;
    final Deserializer<AlarmStateMessage> alarmStateMessageDeserializer;
    final Serde<AlarmStateMessage> alarmStateMessageSerde;

    public AlarmStateLogger(String topic) {
        super();
        this.topic = topic;
        this.serdeProps = new HashMap<>();
        alarmStateMessageSerializer = new Serializer<>() {
            private ObjectMapper objectMapper = new ObjectMapper();

            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {
                objectMapper.registerModule(new JavaTimeModule());
            }

            @Override
            public byte[] serialize(String topic, AlarmStateMessage alarmStateMessage) {
                if (alarmStateMessage == null)
                    return null;

                try {
                    return objectMapper.writeValueAsBytes(alarmStateMessage);
                } catch (Exception e) {
                    throw new SerializationException("Error serializing AlarmStateMessage ", e);
                }
            }

            @Override
            public void close() {
                // TODO Auto-generated method stub

            }
        };
        serdeProps.put("AlarmStateMessage", AlarmStateMessage.class);
        alarmStateMessageSerializer.configure(serdeProps, false);

        alarmStateMessageDeserializer = new Deserializer<>() {
            private ObjectMapper objectMapper = new ObjectMapper();

            @Override
            public void configure(Map<String, ?> configs, boolean isKey) {
                objectMapper.registerModule(new JavaTimeModule());
            }

            @Override
            public AlarmStateMessage deserialize(String topic, byte[] data) {
                if (data == null)
                    return null;

                AlarmStateMessage alarmStateMessage;
                try {
                    alarmStateMessage = objectMapper.readValue(data, AlarmStateMessage.class);
                } catch (Exception e) {
                    throw new SerializationException(e);
                }

                return alarmStateMessage;
            }

            @Override
            public void close() {
                // TODO Auto-generated method stub

            }

        };
        serdeProps.put("AlarmStateMessage", AlarmStateMessage.class);
        alarmStateMessageDeserializer.configure(serdeProps, false);

        alarmStateMessageSerde = Serdes.serdeFrom(alarmStateMessageSerializer, alarmStateMessageDeserializer);
    }

    @Override
    public void run() {
        System.out.println("Starting the stream consumer");

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-wordcount");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "130.199.219.152:9092");

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, AlarmStateMessage> alarms = builder.stream("AcceleratorState",
                Consumed.with(Serdes.String(), alarmStateMessageSerde));
        alarms.foreach((k, v) -> {
            System.out.println("RAW Key: " + k + " Value: " + v);
        });
        // Filter the alarms to only
        KStream<String, AlarmStateMessage> filteredAlarms = alarms.filter((k, v) -> {
            return v != null ? v.isLeaf() : true;
        });
        filteredAlarms.foreach((k, v) -> {
            System.out.println("Filtered Key: " + k + " Value: " + v);
        });
        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-alarm-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

}
