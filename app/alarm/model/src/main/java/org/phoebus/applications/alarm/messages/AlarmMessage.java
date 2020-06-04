package org.phoebus.applications.alarm.messages;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.phoebus.applications.alarm.AlarmSystem.logger;
/**
 * A message which describes both state and configuration events
 * 
 * @author Kunal Shroff
 *
 */
@JsonInclude(Include.NON_NULL)
public class AlarmMessage implements Serializable{

    private static final long serialVersionUID = 7936081175563008693L;

    private String user;
    private String host;
    private String description;

    private Map<String, String> time;
    @JsonIgnore
    private Instant alarmTime;

    // config message
    private boolean enabled = true;
    private boolean latching = true;
    private boolean annunciating = true;
    private int delay = 0;
    private int count = 0;
    private String filter;
    private List<AlarmDetail> guidance;
    private List<AlarmDetail> displays;
    private List<AlarmDetail> commands;
    private List<AlarmDetail> actions;

    private String delete;

    // state message
    private String severity;
    private String message;
    private String value;
    private String current_severity;
    private String current_message;
    private String mode;
    private boolean notify = true;
    private boolean latch;

    // The following fields encapsulate additional information for simplifying processing
    // Flag describing if the message is a configuration message or a state update message
    private String key;

    public AlarmMessage() {
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

    public Map<String, String> getTime() {
        return time;
    }

    public void setTime(Map<String, String> time) {
        this.time = time;
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

    public boolean getNotify() {
        return notify;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    public String getKey() {
        return this.key;
    }

    public boolean isLatch() {
        return latch;
    }

    public void setLatch(boolean latch) {
        this.latch = latch;
    }

    public void setKey(String key) {
        this.key = key;
    }

    private boolean isConfig() {
        if(key != null && !key.isEmpty()) {
            return key.startsWith("config");
        }
        return false;
    }

    private boolean isState() {
        if(key != null && !key.isEmpty()) {
            return key.startsWith("state");
        }
        return false;
    }


    @JsonIgnore
    public AlarmConfigMessage getAlarmConfigMessage() {
        if (isConfig()) {
            AlarmConfigMessage configMessage = new AlarmConfigMessage();
            configMessage.setUser(user);
            configMessage.setHost(host);
            configMessage.setDescription(description);
            configMessage.setEnabled(enabled);
            configMessage.setLatching(latching);
            configMessage.setAnnunciating(annunciating);
            configMessage.setDelay(delay);
            configMessage.setCount(count);
            configMessage.setFilter(filter);

            configMessage.setGuidance(guidance);
            configMessage.setDisplays(displays);
            configMessage.setCommands(commands);
            configMessage.setActions(actions);
            return configMessage;
        } else {
            return null;
        }
    }

    @JsonIgnore
    public AlarmStateMessage getAlarmStateMessage() {
        if (isState()) {
            AlarmStateMessage stateMessage = new AlarmStateMessage();
            stateMessage.setSeverity(severity);
            stateMessage.setMessage(message);
            stateMessage.setValue(value);
            stateMessage.setTime(time);
            stateMessage.setCurrent_severity(current_severity);
            stateMessage.setCurrent_message(current_message);
            stateMessage.setMode(mode);
	    stateMessage.setNotify(notify);
            stateMessage.setLatch(latch);
            return stateMessage;
        } else {
            return null;
        }
    }

    @JsonIgnore
    public Instant getAlarmTime() {
        return Instant.ofEpochSecond(Long.parseLong(time.get("seconds")), Long.parseLong(time.get("nano")));
    }

    @JsonIgnore
    public void setAlarmTime(Instant alarmTime) {
        this.time = new HashMap<>();
        this.time.put("seconds", String.valueOf(alarmTime.getEpochSecond()));
        this.time.put("nano", String.valueOf(alarmTime.getNano()));
    }

    @JsonIgnore
    public String getDelete() {
        return delete;
    }

    @JsonIgnore
    public void setDelete(String delete) {
        this.delete = delete;
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

    /**
     * Returns the json string representation of this object
     * 
     * @throws JsonProcessingException
     */
    public String toJson() throws JsonProcessingException {
        if (isConfig()) {
            return objectConfigMapper.writeValueAsString(this);
        } else if (isState()){
            return objectStateMapper.writeValueAsString(this);
        } else {
            return objectStateMapper.writeValueAsString("");
        }
    }


    private static class AlarmConfigJsonMessage {
        @JsonIgnore
        private String severity;
        @JsonIgnore
        private String message;
        @JsonIgnore
        private String value;
        @JsonIgnore
        private String current_severity;
        @JsonIgnore
        private String current_message;
        @JsonIgnore
        private String mode;
	@JsonIgnore
        private boolean notify;
        @JsonIgnore
        private boolean latch;
    }

    private static class AlarmStateJsonMessage {
        @JsonIgnore
        private boolean enabled;
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

    @Override
    public String toString() {
        try {
            return toJson();
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "failed to parse the alarm message ", e);
        }
        return "";
    }
}
