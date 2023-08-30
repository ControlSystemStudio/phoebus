package org.phoebus.olog.es.api.model;


import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OlogObjectMapperTest {

    @Test
    public void propertySerializationTest() {

        OlogProperty property = new OlogProperty();
        property.setName("TestProperty");
        property.setAttributes(new HashMap<>());
        property.getAttributes().put("Attribute1", "value1");
        property.getAttributes().put("Attribute2", "value2");
        String expectedJson = "{\"name\":\"TestProperty\",\"attributes\":[{\"name\":\"Attribute2\",\"value\":\"value2\"},{\"name\":\"Attribute1\",\"value\":\"value1\"}]}";
        try {
            StringWriter writer = new StringWriter();
            OlogObjectMappers.logEntrySerializer.writeValue(writer, property);
            assertEquals(writer.toString(), expectedJson);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
