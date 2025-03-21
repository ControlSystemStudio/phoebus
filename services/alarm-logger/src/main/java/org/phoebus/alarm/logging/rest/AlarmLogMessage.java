package org.phoebus.alarm.logging.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AlarmLogMessage {
    private static final Logger logger = Logger.getLogger(AlarmLogMessage.class.getName());

    private String severity;
    private String message;
    private String value;
    @JsonDeserialize(using = AlarmInstantDeserializer.class)
    private Instant time;
    private String current_severity;
    private String current_message;
    private String mode;
    @JsonDeserialize(using = AlarmInstantDeserializer.class)
    private Instant message_time;
    private String config;
    private String config_msg;
    private String pv;
    private String user;
    private String host;
    private String command;
    @JsonDeserialize(using = EnabledFieldDeserializer.class)
    private boolean enabled;

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getPv() {
        return pv;
    }

    public void setPv(String pv) {
        this.pv = pv;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public String getCurrent_severity() {
        return current_severity;
    }

    public void setCurrent_severity(String current_severity) {
        this.current_severity = current_severity;
    }

    public String getCurrent_message() {
        return current_message;
    }

    public void setCurrent_message(String current_message) {
        this.current_message = current_message;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Instant getMessage_time() {
        return message_time;
    }

    public void setMessage_time(Instant message_time) {
        this.message_time = message_time;
    }

    public String getConfig_msg() {
        return config_msg;
    }

    public void setConfig_msg(String config_msg) {
        this.config_msg = config_msg;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static class AlarmInstantDeserializer extends JsonDeserializer<Instant> {

        private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("UTC"));

        public AlarmInstantDeserializer() {
        }

        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return Instant.from(formatter.parse(p.getText()));
        }
    }

    public static class EnabledFieldDeserializer extends JsonDeserializer<Boolean> {

        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public Boolean deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            String text = p.getText();
            try {
                return Boolean.parseBoolean(text);
            } catch (Exception e) {
                try {
                    LocalDateTime.parse(text, formatter);
                    return false; // A date string means the alarm is disabled
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Failed to parse enabled field: " + text, ex);
                    return null;
                }
            }
        }
    }
}
