package org.phoebus.applications.alarm.messages;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.phoebus.util.time.TimestampFormats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A message which describes both state and configuration events
 * 
 * @author kunal
 *
 */
@JsonInclude(Include.NON_NULL)
public class AlarmMessage {

    private boolean config = true;

    private String user;
    private String host;
    private String description;

    private Instant alarmTime;
    private Instant msgTime;

    // config
    private boolean enabled = true;
    private boolean latching = true;
    private boolean annunciating = true;
    private int delay;
    private int count;
    private String filter;
    private List<AlarmDetail> guidance;
    private List<AlarmDetail> displays;
    private List<AlarmDetail> commands;
    private List<AlarmDetail> actions;

    // State
    private String severity;
    private String message;
    private String value;
    private String current_severity;
    private String current_message;
    private String mode;

    public AlarmMessage() {

    }

    public boolean isConfig() {
        return config;
    }

    public void setConfig(boolean config) {
        this.config = config;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(Instant alarmTime) {
        this.alarmTime = alarmTime;
    }

    public Instant getMsgTime() {
        return msgTime;
    }

    public void setMsgTime(Instant msgTime) {
        this.msgTime = msgTime;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLatching() {
        return latching;
    }

    public void setLatching(boolean latching) {
        this.latching = latching;
    }

    public boolean isAnnunciating() {
        return annunciating;
    }

    public void setAnnunciating(boolean annunciating) {
        this.annunciating = annunciating;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public List<AlarmDetail> getGuidance() {
        return guidance;
    }

    public void setGuidance(List<AlarmDetail> guidance) {
        this.guidance = guidance;
    }

    public List<AlarmDetail> getDisplays() {
        return displays;
    }

    public void setDisplays(List<AlarmDetail> displays) {
        this.displays = displays;
    }

    public List<AlarmDetail> getCommands() {
        return commands;
    }

    public void setCommands(List<AlarmDetail> commands) {
        this.commands = commands;
    }

    public List<AlarmDetail> getActions() {
        return actions;
    }

    public void setActions(List<AlarmDetail> actions) {
        this.actions = actions;
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

    
    // The methods and classes below this are examples for handling the combined alarm state and config message
    @JsonIgnore
    private static final ObjectMapper objectStateMapper = new ObjectMapper();
    static {
        objectStateMapper.addMixIn(AlarmMessage.class, AlarmStateJsonMessage.class);
    }
    @JsonIgnore
    private static final ObjectMapper objectConfigMapper = new ObjectMapper();
    static {
        objectConfigMapper.addMixIn(AlarmMessage.class, AlarmConfigJsonMessage.class);
    }

    @JsonIgnore
    private static DateTimeFormatter formatter = TimestampFormats.MILLI_FORMAT;

    /**
     * Returns the json string representation of this object
     * 
     * @throws JsonProcessingException
     */
    public String toJson() throws JsonProcessingException {
        if (isConfig()) {
            return objectConfigMapper.writeValueAsString(this);
        } else {
            return objectStateMapper.writeValueAsString(this);
        }
    }

    private static class AlarmConfigJsonMessage {
        @JsonIgnore
        private String severity;
        @JsonIgnore
        private String message;
        @JsonIgnore
        private String value;
    }

    private static class AlarmStateJsonMessage {
        @JsonIgnore
        private boolean enabled;
        @JsonIgnore
        private boolean latching;
        @JsonIgnore
        private boolean annunciating;
        @JsonIgnore
        private int delay;
        @JsonIgnore
        private int count;
        @JsonIgnore
        private String filter;
        @JsonIgnore
        private List<AlarmDetail> guidance;
        @JsonIgnore
        private List<AlarmDetail> displays;
        @JsonIgnore
        private List<AlarmDetail> commands;
        @JsonIgnore
        private List<AlarmDetail> actions;
    }
}
