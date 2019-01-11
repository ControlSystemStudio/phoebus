package org.phoebus.applications.alarm.logging.ui;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.phoebus.util.time.TimestampFormats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.phoebus.applications.alarm.messages.AlarmStateMessage;
import org.phoebus.applications.alarm.messages.AlarmCommandMessage;
import org.phoebus.applications.alarm.messages.AlarmConfigMessage;

@JsonInclude(Include.NON_NULL)
public class AlarmLogTableType {
	private String severity;
    private String message;
    private String value;
    private Map<String, String> time;
    private String current_severity;
    private String current_message;
    private String mode;
    private Instant message_time;
    private String config;
    private String pv;
    private String user;
    private String host;
    private String command;
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

    public Map<String, String> getTime() {
        return time;
    }

    public void setTime(Map<String, String> time) {
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

    @JsonIgnore
    public Instant getInstant() {
        return Instant.ofEpochSecond(Long.parseLong(time.get("seconds")), Long.parseLong(time.get("nano")));
    }

    @JsonIgnore
    public void setInstant(Instant instant) {
        this.time = new HashMap<>();
        this.time.put("seconds", String.valueOf(instant.getEpochSecond()));
        this.time.put("nano", String.valueOf(instant.getNano()));
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
}