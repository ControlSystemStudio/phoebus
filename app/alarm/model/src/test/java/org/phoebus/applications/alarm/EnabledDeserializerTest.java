package org.phoebus.applications.alarm;


import java.io.IOException;
import org.phoebus.applications.alarm.model.EnabledState;
import org.phoebus.applications.alarm.messages.EnabledDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class EnabledDeserializerTest {
    public static void DeserializerTest() throws IOException {

        String json = "{\n" +
                "    \"enabled\": 2021-09-22T09:30:00}\n" +
                "}";
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(EnabledState.class, new EnabledDeserializer());
        mapper.registerModule(module);
        EnabledState readValue = mapper.readValue(json, EnabledState.class);
    }
}
