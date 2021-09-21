package org.phoebus.applications.alarm.messages;

import org.phoebus.applications.alarm.model.EnabledState;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

/**
 * Deserializer used for writing varied date string/boolean enabled values
 *
 * @author Jacqueline Garrahan
 *
 */
public class EnabledDeserializer extends StdDeserializer<EnabledState> {
    
    public EnabledDeserializer() {
        this(null);
    }
  
    public EnabledDeserializer(Class<EnabledState> t) {
        super(t);
    }

    @Override
    public EnabledState deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec oc = jp.getCodec();
        JsonNode node = oc.readTree(jp);
        System.out.println("Deserializing enabled state");

        node.get("enabled");
        
        return new EnabledState(false);
    }
}