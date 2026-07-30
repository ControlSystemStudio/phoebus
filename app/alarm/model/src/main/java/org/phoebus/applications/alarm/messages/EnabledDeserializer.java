package org.phoebus.applications.alarm.messages;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonProcessingException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.core.ObjectCodec;
import tools.jackson.databind.JsonNode;

import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.util.logging.Level;

import org.phoebus.applications.alarm.model.EnabledState;

/**
 * Deserializer used for writing varied date string/boolean enabled values
 *
 * @author Jacqueline Garrahan
 *
 */
public class EnabledDeserializer extends StdDeserializer<EnabledState> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** Constructor */
    public EnabledDeserializer() {
        this(null);
    }

    /** @param t Initial state */
    public EnabledDeserializer(Class<EnabledState> t) {
        super(t);
    }

    @Override
    public EnabledState deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectCodec oc = jp.getCodec();
        JsonNode jn = oc.readTree(jp);

        // use pattern matching to determine whether boolean or datetime string
        if (jn != null) {
            Pattern pattern = Pattern.compile("true|false", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(jn.asText());

            if(matcher.matches()) {
                return new EnabledState(jn.asBoolean());
             } else {
                 try {
                    LocalDateTime enabled_date = LocalDateTime.parse(jn.asText(), formatter);
                    return new EnabledState(enabled_date);
                 }
                 catch (Exception ex) {
                    logger.log(Level.WARNING, "Bypass date incorrectly formatted." + jn.asText() + "'");
                }
            }
        }
        return new EnabledState(true);
    }
}
