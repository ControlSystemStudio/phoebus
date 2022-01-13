package org.phoebus.applications.alarm.messages;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phoebus.util.time.TimestampFormats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import static org.phoebus.applications.alarm.AlarmSystem.logger;
import static org.phoebus.applications.alarm.messages.AlarmMessageUtil.objectMapper;

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

    public AlarmCommandMessage() {
        super();
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Instant getMessage_time() {
        return message_time;
    }

    public void setMessage_time(Instant message_time) {
        this.message_time = message_time;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

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
