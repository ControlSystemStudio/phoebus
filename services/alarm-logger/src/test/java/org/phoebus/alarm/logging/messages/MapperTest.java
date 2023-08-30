package org.phoebus.alarm.logging.messages;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.junit.jupiter.api.Test;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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

        try {
            // Parsing object to json string
            assertEquals(expectedJsonString,
                    objectMapper.writer(filters).writeValueAsString(message),
                    "Failed to map the AlarmStateMessage");
            // Serializing object to byte[]
            assertArrayEquals(expectedJsonString.getBytes(),
                    objectMapper.writer(filters).writeValueAsBytes(message),
                    "Failed to parse AlarmStateMessage to byte[] ");

            // Check the pasrsing json string to object
            // bjectMapper.
            AlarmStateMessage state = objectMapper.readValue(expectedJsonString, AlarmStateMessage.class);
            assertEquals(message,
                    objectMapper.readValue(expectedJsonString, AlarmStateMessage.class),
                    "Failed to map the AlarmStateMessage");
            // Check sdeserializing byte[] to object
            assertEquals(message,
                    objectMapper.readValue(expectedJsonString.getBytes(), AlarmStateMessage.class),
                    "Failed to parse AlarmStateMessage to byte[] ");

        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

}
