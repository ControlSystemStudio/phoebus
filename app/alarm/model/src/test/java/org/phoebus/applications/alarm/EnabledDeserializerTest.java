package org.phoebus.applications.alarm;


import java.io.IOException;
import org.phoebus.applications.alarm.model.EnabledState;
import org.phoebus.applications.alarm.messages.EnabledDeserializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

public class EnabledDeserializerTest {
    public static void DeserializerTest() throws IOException {

        String json = "{\n" +
                "    \"enabled\": 2021-09-22T09:30:00}\n" +
                "}";
        SimpleModule module = new SimpleModule();
        module.addDeserializer(EnabledState.class, new EnabledDeserializer());
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(module)
                .build();
        EnabledState readValue = mapper.readValue(json, EnabledState.class);
    }
}
