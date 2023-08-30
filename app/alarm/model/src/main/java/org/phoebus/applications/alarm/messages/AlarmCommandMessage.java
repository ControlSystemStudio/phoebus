package org.phoebus.applications.alarm.messages;

import static org.phoebus.applications.alarm.AlarmSystem.logger;
import static org.phoebus.applications.alarm.messages.AlarmMessageUtil.objectMapper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.phoebus.util.time.TimestampFormats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 * A bean representing an alarm command message
 *
 * @author Kunal Shroff
 *
 */
@JsonInclude(Include.NON_NULL)
public class AlarmCommandMessage {

    private static DateTimeFormatter formatter = TimestampFormats.MILLI_FORMAT;

    private String user;
    private String host;
    private String command;

    // The following fields are for logging purposes
    private String config;
    private Instant message_time;

    /** Constructor */
    public AlarmCommandMessage() {
        super();
    }

    /** @return User */
    public String getUser() {
        return user;
    }

    /** @param user New value */
    public void setUser(String user) {
        this.user = user;
    }

    /** @return Host */
    public String getHost() {
        return host;
    }

    /** @param host New value */
    public void setHost(String host) {
        this.host = host;
    }

    /** @return Command */
    public String getCommand() {
        return command;
    }

    /** @param command New value */
    public void setCommand(String command) {
        this.command = command;
    }

    /** @return Message time */
    public Instant getMessage_time() {
        return message_time;
    }

    /** @param message_time New value */
    public void setMessage_time(Instant message_time) {
        this.message_time = message_time;
    }

    /** @return Config */
    public String getConfig() {
        return config;
    }

    /** @param config New value */
    public void setConfig(String config) {
        this.config = config;
    }

    /** @return Map of original data */
    @JsonIgnore
    public Map<String, String> sourceMap() {
        Map<String, String> map = new HashMap<>();
        map.put("config", getConfig());
        map.put("user", getUser());
        map.put("host", getHost());
        map.put("command", getCommand());
        map.put("message_time", formatter.withZone(ZoneId.of("UTC")).format(getMessage_time()));
        return map;
    }

    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "failed to parse the alarm command message ", e);
        }
        return "";
    }
}
