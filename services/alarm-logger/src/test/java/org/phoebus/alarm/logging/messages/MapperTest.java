package org.phoebus.alarm.logging.messages;


import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import org.junit.jupiter.api.Test;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapperTest {

    @Test
    public void AlarmStateMessageTest() {
        String expectedJsonString = "{\"severity\":\"OK\",\"message\":\"OK\",\"value\":\"-2.5614483916185438\",\"time\":{\"seconds\":\"1531143702\",\"nanos\":\"487182900\"},\"current_severity\":\"OK\",\"current_message\":\"NONE\",\"notify\":false,\"latch\":false}";

        AlarmStateMessage message = new AlarmStateMessage();
        message.setValue("-2.5614483916185438");
        message.setSeverity("OK");
        message.setMessage("OK");
        message.setCurrent_severity("OK");
        message.setCurrent_message("NONE");
        HashMap<String, String> timeMap = new HashMap<>();
        timeMap.put("seconds", "1531143702");
        timeMap.put("nanos", "487182900");
        message.setTime(timeMap);
        message.setMode(null);
	message.setNotify(false);

        ObjectMapper objectMapper = new ObjectMapper();

        SimpleBeanPropertyFilter emptyFilter = SimpleBeanPropertyFilter.serializeAll();
        FilterProvider filters = new SimpleFilterProvider().addFilter("timeFilter", emptyFilter);

        final String actualJsonString = objectMapper.writer(filters).writeValueAsString(message);
        final byte[] actualJsonBytes = objectMapper.writer(filters).writeValueAsBytes(message);

        // Verify object -> JSON string content regardless of property ordering.
        assertEquals(objectMapper.readTree(expectedJsonString),
                objectMapper.readTree(actualJsonString),
                "Failed to map the AlarmStateMessage");
        // Verify object -> JSON byte[] content regardless of property ordering.
        assertEquals(objectMapper.readTree(expectedJsonString),
                objectMapper.readTree(actualJsonBytes),
                "Failed to parse AlarmStateMessage to byte[] ");
        // Keep explicit coverage that byte[] serialization represents the same JSON text.
        assertEquals(actualJsonString,
                new String(actualJsonBytes, StandardCharsets.UTF_8),
                "String and byte[] serialization differ");

        // Verify JSON string -> object deserialization.
        assertEquals(message,
                objectMapper.readValue(expectedJsonString, AlarmStateMessage.class),
                "Failed to map the AlarmStateMessage");
        // Verify JSON byte[] -> object deserialization.
        assertEquals(message,
                objectMapper.readValue(expectedJsonString.getBytes(), AlarmStateMessage.class),
                "Failed to parse AlarmStateMessage to byte[] ");

    }

}
