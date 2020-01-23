/**
 * 
 */
package org.phoebus.olog.es.api;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

    static ObjectMapper logEntryDeserializer = new ObjectMapper().registerModule(new JavaTimeModule());

    static ObjectMapper logEntrySerializer = new ObjectMapper().registerModule(new JavaTimeModule());
    
    static SimpleModule module = new SimpleModule("CustomModel", Version.unknownVersion());
    static SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();

    /**
     * A json deserializer which maps the new olog properties to {@link XmlProperty}
     * @author Kunal Shroff
     */
    static class PropertyDeserializer extends JsonDeserializer<XmlProperty> {

        @Override
        public XmlProperty deserialize(JsonParser jp, DeserializationContext ctxt) 
          throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            String name = node.get("name").asText();
            String owner = node.get("owner").asText();
            String state = node.get("state").asText();
            Map<String, String> attributes = new HashMap<String, String>();
            node.get("attributes").iterator().forEachRemaining(n -> {
                attributes.put(n.get("name").asText(), n.get("value").asText());
            });
            return new XmlProperty(name, attributes);
        }
    }

    /**
     * A json deserializer which maps the new attachment to {@link XmlAttachment}
     * @author Kunal Shroff
     */
    static class AttachmentDeserializer extends JsonDeserializer<XmlAttachment> {

        @Override
        public XmlAttachment deserialize(JsonParser jp, DeserializationContext ctxt) 
          throws IOException, JsonProcessingException {
            JsonNode node = jp.getCodec().readTree(jp);
            String id = node.get("id").asText();
            String filename = node.get("filename").asText();
            String fileMetadataDescription = node.get("fileMetadataDescription").asText();
            XmlAttachment a = new XmlAttachment();
            a.setFileName(filename);
            a.setContentType(fileMetadataDescription);
            return a;
        }
    }

    static {
        resolver.addMapping(Logbook.class, XmlLogbook.class);
        resolver.addMapping(Tag.class, XmlTag.class);
        resolver.addMapping(Property.class, XmlProperty.class);
        resolver.addMapping(Attachment.class, XmlAttachment.class);
        module.setAbstractTypes(resolver);

        module.addDeserializer(XmlProperty.class, new PropertyDeserializer());
        module.addDeserializer(XmlAttachment.class, new AttachmentDeserializer());
        logEntryDeserializer.registerModule(module);
        logEntryDeserializer.addMixIn(Attachment.class, AttachmentMixIn.class);
        logEntryDeserializer.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    static {
        resolver.addMapping(Logbook.class, XmlLogbook.class);
        resolver.addMapping(Tag.class, XmlTag.class);
        resolver.addMapping(Property.class, XmlProperty.class);
        resolver.addMapping(Attachment.class, XmlAttachment.class);
        module.setAbstractTypes(resolver);

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
