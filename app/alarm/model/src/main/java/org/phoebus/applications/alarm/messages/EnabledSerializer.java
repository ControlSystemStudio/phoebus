package org.phoebus.applications.alarm.messages;
import org.phoebus.applications.alarm.model.EnabledState;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

public class EnabledSerializer extends StdSerializer<EnabledState> {
    
    public EnabledSerializer() {
        this(null);
    }
  
    public EnabledSerializer(Class<EnabledState> t) {
        super(t);
    }

    @Override
    public void serialize(
        EnabledState enabled_state, JsonGenerator jgen, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
 
        jgen.writeStartObject();
        if (enabled_state.enabled_date != null) {
            jgen.writeStringField("enabled", enabled_state.getDateString());
        }
        else {
            jgen.writeBooleanField("enabled", enabled_state.enabled);
        }
        jgen.writeEndObject();
    }
}