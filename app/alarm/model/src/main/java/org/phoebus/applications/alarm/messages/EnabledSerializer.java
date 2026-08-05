package org.phoebus.applications.alarm.messages;
import org.phoebus.applications.alarm.model.EnabledState;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

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
        EnabledState enabled_state, JsonGenerator jgen, SerializationContext provider)
      throws JacksonException {

        jgen.writeStartObject();
        if (enabled_state.enabled_date != null) {
            jgen.writeStringProperty("enabled", enabled_state.getDateString());
        }
        else {
            jgen.writeBooleanProperty("enabled", enabled_state.enabled);
        }
        jgen.writeEndObject();
    }
}
