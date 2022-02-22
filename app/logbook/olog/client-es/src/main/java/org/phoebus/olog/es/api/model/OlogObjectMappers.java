/**
 * 
 */
package org.phoebus.olog.es.api.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * A Utility class which provides {@link ObjectMapper}s for Olog-es entities.
 * @author Kunal Shroff
 */
public class OlogObjectMappers {

    public static ObjectMapper logEntryDeserializer = new ObjectMapper().registerModule(new JavaTimeModule());
    public static ObjectMapper logEntrySerializer = new ObjectMapper().registerModule(new JavaTimeModule());
    
    static SimpleModule module = new SimpleModule("CustomModel", Version.unknownVersion());
    static SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();

    /**
     * A json deserializer which maps the new olog properties to {@link OlogProperty}
     * @author Kunal Shroff
     */
    static class PropertyDeserializer extends JsonDeserializer<OlogProperty> {

        @Override
        public OlogProperty deserialize(JsonParser jp, DeserializationContext ctxt)
          throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            // TODO throw error if either the property or attribute names are null
            String name = node.get("name").asText();
            String owner = node.get("owner").isNull()? "" : node.get("owner").asText();
            String state = node.get("state").isNull()? "" : node.get("state").asText();
            Map<String, String> attributes = new HashMap<String, String>();
            node.get("attributes").iterator().forEachRemaining(n -> {
                attributes.put(
                        n.get("name").asText(),
                        n.get("value").isNull()? "" : n.get("value").asText()
                );
            });
            return new OlogProperty(name, attributes);
        }
    }

    /**
     * A json serializer which maps the new olog properties to {@link OlogProperty}
     * @author Kunal Shroff
     */
    static class PropertySerializer extends JsonSerializer<Property> {

        @Override
        public void serialize(Property value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            gen.writeStringField("name", value.getName());
            gen.writeArrayFieldStart("attributes");

            value.getAttributes().entrySet().stream().forEach(entry -> {
                        try {
                            gen.writeStartObject();
                            gen.writeStringField("name", entry.getKey());
                            gen.writeStringField("value", entry.getValue() == null ? "" : entry.getValue());
                            gen.writeEndObject();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            );
            gen.writeEndArray();
            gen.writeEndObject();

        }
    }

    /**
     * A json deserializer which maps the new attachment to {@link OlogAttachment}
     * @author Kunal Shroff
     */
    static class AttachmentDeserializer extends JsonDeserializer<OlogAttachment> {

        @Override
        public OlogAttachment deserialize(JsonParser jp, DeserializationContext ctxt)
          throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            String id = node.get("id").asText();
            String filename = node.get("filename").asText();
            String fileMetadataDescription = node.get("fileMetadataDescription").asText();
            OlogAttachment a = new OlogAttachment();
            a.setFileName(filename);
            a.setContentType(fileMetadataDescription);
            return a;
        }
    }

    static {
        resolver.addMapping(Logbook.class, OlogLogbook.class);
        resolver.addMapping(Tag.class, OlogTag.class);
        resolver.addMapping(Property.class, OlogProperty.class);
        resolver.addMapping(Attachment.class, OlogAttachment.class);
        module.setAbstractTypes(resolver);

        module.addDeserializer(OlogProperty.class, new PropertyDeserializer());
        module.addDeserializer(OlogAttachment.class, new AttachmentDeserializer());
        logEntryDeserializer.registerModule(module);
        logEntryDeserializer.addMixIn(Attachment.class, AttachmentMixIn.class);
        logEntryDeserializer.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    static {
        resolver.addMapping(Logbook.class, OlogLogbook.class);
        resolver.addMapping(Tag.class, OlogTag.class);
        resolver.addMapping(Property.class, OlogProperty.class);
        resolver.addMapping(Attachment.class, OlogAttachment.class);
        module.setAbstractTypes(resolver);
        module.addSerializer(Property.class, new PropertySerializer());

        logEntrySerializer.registerModule(module);
        logEntrySerializer.addMixIn(Attachment.class, AttachmentMixIn.class);
        logEntrySerializer.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        logEntrySerializer.configure(Feature.AUTO_CLOSE_SOURCE, true);
    }
    

    public interface AttachmentMixIn {

        @JsonProperty("filename")
        public String getName();

        @JsonProperty("file")
        public File getFile();

        @JsonProperty("fileMetadataDescription")
        public String getContentType();

        @JsonProperty("filename")
        public void setName(String name);

        @JsonProperty("file")
        public void setFile(File file);

        @JsonProperty("fileMetadataDescription")
        public void setContentType(String contentType);
    }
}
