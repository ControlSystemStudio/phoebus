package org.phoebus.applications.alarm.messages;
import java.io.IOException;

import org.phoebus.applications.alarm.model.EnabledState;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Serializer used for writing varied date string/boolean enabled values
 *
 * @author Jacqueline Garrahan
 *
 */
public class EnabledSerializer extends StdSerializer<EnabledState> {

    /** Constructor */
    public EnabledSerializer() {
        super(EnabledState.class);
    }

    @Override
    public void serialize(
        EnabledState enabled_state, JsonGenerator jgen, SerializerProvider provider)
      throws IOException {

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
