package org.phoebus.applications.alarm.messages;

import java.util.Map;

import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MessageParser<T> implements Serializer<T>, Deserializer<T> {

    private ObjectMapper objectMapper = new ObjectMapper();
    private Class<T> tClass;

    /**
     * Create a message parser for type tClass
     * 
     * @param tClass
     */
    public MessageParser(Class<T> tClass) {
        this.tClass = tClass;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public void configure(Map arg0, boolean arg1) {
    }

    @Override
    public byte[] serialize(String topic, T message) {
        if (message == null)
            return null;

        try {
            return objectMapper.writeValueAsBytes(message);
        } catch (Exception e) {
            throw new SerializationException("Error serializing message ", e);
        }
    }

    @Override
    public T deserialize(String arg0, byte[] bytes) {
        if (bytes == null)
            return null;

        T data;
        try {
            data = objectMapper.readValue(bytes, tClass);
        } catch (Exception e) {
            throw new SerializationException(e);
        }

        return data;
    }

}
