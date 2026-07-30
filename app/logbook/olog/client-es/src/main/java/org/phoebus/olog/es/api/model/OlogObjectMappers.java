/**
 *
 */
package org.phoebus.olog.es.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonParser.Feature;
import tools.jackson.core.Version;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonDeserializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.JsonSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.module.SimpleAbstractTypeResolver;
import tools.jackson.databind.module.SimpleModule;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Property;
import org.phoebus.logbook.Tag;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    static class PropertyDeserializer extends JsonDeserializer<OlogProperty> {

        @Override
        public OlogProperty deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
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
            gen.writeStringProperty("name", value.getName());
            gen.writeArrayPropertyStart("attributes");

            value.getAttributes().entrySet().stream().forEach(entry -> {
                        try {
                            gen.writeStartObject();
                            gen.writeStringProperty("name", entry.getKey());
                            gen.writeStringProperty("value", entry.getValue() == null ? "" : entry.getValue());
                            gen.writeEndObject();
                        } catch (IOException e) {
                            Logger.getLogger(OlogObjectMappers.class.getName()).log(Level.WARNING, "Failed to serialize property", e);
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
                throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            String id = node.get("id").asText();
            String filename = node.get("filename").asText();
            String fileMetadataDescription = node.get("fileMetadataDescription").asText();
            OlogAttachment a = new OlogAttachment();
            a.setUniqueFilename(filename);
            a.setId(id);
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
