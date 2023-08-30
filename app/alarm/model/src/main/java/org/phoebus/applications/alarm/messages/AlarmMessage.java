package org.phoebus.applications.alarm.messages;

import static org.phoebus.applications.alarm.AlarmSystem.logger;
import static org.phoebus.applications.alarm.messages.AlarmMessageUtil.objectConfigMapper;
import static org.phoebus.applications.alarm.messages.AlarmMessageUtil.objectStateMapper;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.phoebus.applications.alarm.model.EnabledState;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * A message which describes both state and configuration events
 *
 * @author Kunal Shroff
 *
 */
@JsonInclude(Include.NON_NULL)
public class AlarmMessage implements Serializable{

    private static final long serialVersionUID = 7936081175563008693L;

    /** Data */
    private String user;
    /** Data */
    private String host;
    /** Data */
    private String description;

    /** Data */
    private Map<String, String> time;
    /** Data */
    @JsonIgnore
    private Instant alarmTime;

    // config message
    /** Data */
    private EnabledState enabled = new EnabledState(true);
    /** Data */
    private boolean latching = true;
    /** Data */
    private boolean annunciating = true;
    /** Data */
    private int delay = 0;
    /** Data */
    private int count = 0;
    /** Data */
    private String filter;
    /** Data */
    private List<AlarmDetail> guidance;
    /** Data */
    private List<AlarmDetail> displays;
    /** Data */
    private List<AlarmDetail> commands;
    /** Data */
    private List<AlarmDetail> actions;

    /** Data */
    private String delete;


    // state message
    /** Data */
    private String severity;
    /** Data */
    private String message;
    /** Data */
    private String value;
    /** Data */
    private String current_severity;
    /** Data */
    private String current_message;
    /** Data */
    private String mode;
    /** Data */
    private boolean notify = true;
    /** Data */
    private boolean latch;


    // The following fields encapsulate additional information for simplifying processing
    // Flag describing if the message is a configuration message or a state update message
    /** Data */
    private String key;


    /** Constructor */
    public AlarmMessage() {
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

    /** @return Value */
    public String getDescription() {
        return description;
    }

    /** @param description New value */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return Value */
    public Map<String, String> getTime() {
        return time;
    }

    /** @param time New value */
    public void setTime(Map<String, String> time) {
        this.time = time;
    }

    /** @return Value */
    public EnabledState getEnabled() {
        return enabled;
    }

    /** @param enabled New value */
    public void setEnabled(EnabledState enabled) {
        this.enabled = enabled;
    }

    /** @return Value */
    public boolean isLatching() {
        return latching;
    }

    /** @param latching New value */
    public void setLatching(boolean latching) {
        this.latching = latching;
    }

    /** @return Value */
    public boolean isAnnunciating() {
        return annunciating;
    }

    /** @param annunciating New value */
    public void setAnnunciating(boolean annunciating) {
        this.annunciating = annunciating;
    }

    /** @return Value */
    public int getDelay() {
        return delay;
    }

    /** @param delay New value */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    /** @return Value */
    public int getCount() {
        return count;
    }

    /** @param count New value */
    public void setCount(int count) {
        this.count = count;
    }

    /** @return Value */
    public String getFilter() {
        return filter;
    }

    /** @param filter New value */
    public void setFilter(String filter) {
        this.filter = filter;
    }

    /** @return Value */
    public List<AlarmDetail> getGuidance() {
        return guidance;
    }

    /** @param guidance New value */
    public void setGuidance(List<AlarmDetail> guidance) {
        this.guidance = guidance;
    }

    /** @return Value */
    public List<AlarmDetail> getDisplays() {
        return displays;
    }

    /** @param displays New value */
    public void setDisplays(List<AlarmDetail> displays) {
        this.displays = displays;
    }

    /** @return Value */
    public List<AlarmDetail> getCommands() {
        return commands;
    }

    /** @param commands New value */
    public void setCommands(List<AlarmDetail> commands) {
        this.commands = commands;
    }

    /** @return Value */
    public List<AlarmDetail> getActions() {
        return actions;
    }

    /** @param actions New value */
    public void setActions(List<AlarmDetail> actions) {
        this.actions = actions;
    }

    /** @return Value */
    public String getSeverity() {
        return severity;
    }

    /** @param severity New value */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /** @return Value */
    public String getMessage() {
        return message;
    }

    /** @param message New value */
    public void setMessage(String message) {
        this.message = message;
    }

    /** @return Value */
    public String getValue() {
        return value;
    }

    /** @param value New value */
    public void setValue(String value) {
        this.value = value;
    }

    /** @return Value */
    public String getCurrent_severity() {
        return current_severity;
    }

    /** @param current_severity New value */
    public void setCurrent_severity(String current_severity) {
        this.current_severity = current_severity;
    }

    /** @return Value */
    public String getCurrent_message() {
        return current_message;
    }

    /** @param current_message New value */
    public void setCurrent_message(String current_message) {
        this.current_message = current_message;
    }

    /** @return Value */
    public String getMode() {
        return mode;
    }

    /** @param mode New value */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /** @return Value */
    public boolean getNotify() {
        return notify;
    }

    /** @param notify New value */
    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    /** @return Value */
    public String getKey() {
        return this.key;
    }

    /** @return Value */
    public boolean isLatch() {
        return latch;
    }

    /** @param latch New value */
    public void setLatch(boolean latch) {
        this.latch = latch;
    }

    /** @param key New value */
    public void setKey(String key) {
        this.key = key;
    }

    /** @return Is this a 'config' message? */
    private boolean isConfig() {
        if(key != null && !key.isEmpty()) {
            return key.startsWith("config");
        }
        return false;
    }

    /** @return Is this a 'state' update message? */
    private boolean isState() {
        if(key != null && !key.isEmpty()) {
            return key.startsWith("state");
        }
        return false;
    }


    /** @return Config message */
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

    /** @return State update message */
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

    /** @return Timestamp */
    @JsonIgnore
    public Instant getAlarmTime() {
        return Instant.ofEpochSecond(Long.parseLong(time.get("seconds")), Long.parseLong(time.get("nano")));
    }

    /** @param alarmTime New value */
    @JsonIgnore
    public void setAlarmTime(Instant alarmTime) {
        this.time = new HashMap<>();
        this.time.put("seconds", String.valueOf(alarmTime.getEpochSecond()));
        this.time.put("nano", String.valueOf(alarmTime.getNano()));
    }

    /** @return Delete flag */
    @JsonIgnore
    public String getDelete() {
        return delete;
    }

    /** @param delete New value */
    @JsonIgnore
    public void setDelete(String delete) {
        this.delete = delete;
    }

    /**
     * @return json string representation of this object
     *
     * @throws JsonProcessingException on error
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
