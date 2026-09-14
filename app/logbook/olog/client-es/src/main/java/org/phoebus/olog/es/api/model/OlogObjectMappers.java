/**
 *
 */
package org.phoebus.olog.es.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JacksonException;
import tools.jackson.core.Version;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleAbstractTypeResolver;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A Utility class which provides {@link ObjectMapper}s for Olog-es entities.
 * @author Kunal Shroff
 */
public class OlogObjectMappers {

    public static ObjectMapper logEntryDeserializer = new ObjectMapper();
    public static ObjectMapper logEntrySerializer = new ObjectMapper();

    static SimpleModule module = new SimpleModule("CustomModel", Version.unknownVersion());
    static SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();

    /**
     * A json deserializer which maps the new olog properties to {@link OlogProperty}
     * @author Kunal Shroff
     */
    static class PropertyDeserializer extends StdDeserializer<OlogProperty> {

        PropertyDeserializer() {
            super(OlogProperty.class);
        }

        @Override
        public OlogProperty deserialize(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
            try {
                JsonNode node = ctxt.readTree(jp);
                // TODO throw error if either the property or attribute names are null
                String name = node.get("name").asText();
                Map<String, String> attributes = new HashMap<>();
                node.get("attributes").iterator().forEachRemaining(n -> {
                    attributes.put(
                            n.get("name").asText(),
                            n.get("value").isNull() ? "" : n.get("value").asText()
                    );
                });
                return new OlogProperty(name, attributes);
            } catch (JacksonException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * A json serializer which maps the new olog properties to {@link OlogProperty}
     * @author Kunal Shroff
     */
    static class PropertySerializer extends StdSerializer<Property> {

        PropertySerializer() {
            super(Property.class);
        }

        @Override
        public void serialize(Property value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {
            try {
                gen.writeStartObject();
                gen.writeStringProperty("name", value.getName());
                gen.writeArrayPropertyStart("attributes");

                for (Map.Entry<String, String> entry : value.getAttributes().entrySet()) {
                    gen.writeStartObject();
                    gen.writeStringProperty("name", entry.getKey());
                    gen.writeStringProperty("value", entry.getValue() == null ? "" : entry.getValue());
                    gen.writeEndObject();
                }
                gen.writeEndArray();
                gen.writeEndObject();
            } catch (JacksonException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * A json deserializer which maps the new attachment to {@link OlogAttachment}
     * @author Kunal Shroff
     */
    static class AttachmentDeserializer extends StdDeserializer<OlogAttachment> {

        AttachmentDeserializer() {
            super(OlogAttachment.class);
        }

        @Override
        public OlogAttachment deserialize(JsonParser jp, DeserializationContext ctxt) throws JacksonException {
            try {
                JsonNode node = ctxt.readTree(jp);
                String id = node.get("id").asText();
                String filename = node.get("filename").asText();
                String fileMetadataDescription = node.get("fileMetadataDescription").asText();
                OlogAttachment a = new OlogAttachment();
                a.setUniqueFilename(filename);
                a.setId(id);
                a.setContentType(fileMetadataDescription);
                return a;
            } catch (JacksonException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
        logEntryDeserializer = JsonMapper.builder()
                .addModule(module)
                .addMixIn(Attachment.class, AttachmentMixIn.class)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    static {
        resolver.addMapping(Logbook.class, OlogLogbook.class);
        resolver.addMapping(Tag.class, OlogTag.class);
        resolver.addMapping(Property.class, OlogProperty.class);
        resolver.addMapping(Attachment.class, OlogAttachment.class);
        module.setAbstractTypes(resolver);
        module.addSerializer(Property.class, new PropertySerializer());

        logEntrySerializer = JsonMapper.builder()
                .addModule(module)
                .addMixIn(Attachment.class, AttachmentMixIn.class)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }


    public interface AttachmentMixIn {

        @JsonProperty("filename")
        String getName();

        @JsonProperty("file")
        File getFile();

        @JsonProperty("fileMetadataDescription")
        String getContentType();

        @JsonProperty("filename")
        void setName(String name);

        @JsonProperty("file")
        void setFile(File file);

        @JsonProperty("fileMetadataDescription")
        void setContentType(String contentType);
    }
}
