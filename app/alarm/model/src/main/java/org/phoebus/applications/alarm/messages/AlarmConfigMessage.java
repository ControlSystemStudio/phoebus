package org.phoebus.applications.alarm.messages;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.phoebus.applications.alarm.model.EnabledState;
import org.phoebus.util.time.TimestampFormats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 * A bean representing an alarm config message
 *
 * @author Kunal Shroff
 *
 */
@JsonInclude(Include.NON_NULL)
public class AlarmConfigMessage {

    private static DateTimeFormatter formatter = TimestampFormats.MILLI_FORMAT;

    private String user;
    private String host;
    private String description;
    private EnabledState enabled = new EnabledState(true);
    private boolean latching = true;
    private boolean annunciating = true;
    private int delay;
    private int count;
    private String filter;
    private List<AlarmDetail> guidance;
    private List<AlarmDetail> displays;
    private List<AlarmDetail> commands;
    private List<AlarmDetail> actions;

    private String delete;

    // The following fields are for logging purposes
    private String config;
    private Instant message_time;

    /** Constructor */
    public AlarmConfigMessage() {
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

    /** @return Description */
    public String getDescription() {
        return description;
    }

    /** @param description New value */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return Is alarm enabled? */
    public boolean isEnabled() {
        return enabled.enabled;
    }

    /** @param enabled New value */
    public void setEnabled(EnabledState enabled) {
        this.enabled = enabled;
    }

    /** @return Is alarm latching? */
    public boolean isLatching() {
        return latching;
    }

    /** @param latching New value */
    public void setLatching(boolean latching) {
        this.latching = latching;
    }

    /** @return Is alarm annunciated */
    public boolean isAnnunciating() {
        return annunciating;
    }

    /** @param annunciating New value */
    public void setAnnunciating(boolean annunciating) {
        this.annunciating = annunciating;
    }

    /** @return Delay */
    public int getDelay() {
        return delay;
    }

    /** @param delay New value */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    /** @return Chatter count */
    public int getCount() {
        return count;
    }

    /** @param count New value */
    public void setCount(int count) {
        this.count = count;
    }

    /** @return Filter expression */
    public String getFilter() {
        return filter;
    }

    /** @param filter New value */
    public void setFilter(String filter) {
        this.filter = filter;
    }

    /** @return Guidance */
    public List<AlarmDetail> getGuidance() {
        return guidance;
    }

    /** @param guidance New value */
    public void setGuidance(List<AlarmDetail> guidance) {
        this.guidance = guidance;
    }

    /** @return Displays */
    public List<AlarmDetail> getDisplays() {
        return displays;
    }

    /** @param displays New value */
    public void setDisplays(List<AlarmDetail> displays) {
        this.displays = displays;
    }

    /** @return Commands */
    public List<AlarmDetail> getCommands() {
        return commands;
    }

    /** @param commands New value */
    public void setCommands(List<AlarmDetail> commands) {
        this.commands = commands;
    }

    /** @return Actions */
    public List<AlarmDetail> getActions() {
        return actions;
    }

    /** @param actions New value */
    public void setActions(List<AlarmDetail> actions) {
        this.actions = actions;
    }

    /** @return Delete flag */
    public String getDelete() {
        return delete;
    }

    /** @param delete New value */
    public void setDelete(String delete) {
        this.delete = delete;
    }

    /** @return Timestamp */
    public Instant getMessage_time() {
        return message_time;
    }

    /** @param message_time New value */
    public void setMessage_time(Instant message_time) {
        this.message_time = message_time;
    }

    /** @return Config name */
    public String getConfig() {
        return config;
    }

    /** @param config New value */
    public void setConfig(String config) {
        this.config = config;
    }

    /** @return Leaf node, i.e. PV? */
    @JsonIgnore
    public boolean isLeaf() {
        return this.description != null;
    }

    @JsonIgnore
    @Override
    public String toString() {
        try {
            return AlarmMessageUtil.objectConfigMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "failed to parse the alarm config message ", e);
        }
        return "";
    }

    /** @return Map of original data */
    @JsonIgnore
    public Map<String, String> sourceMap() {

        Map<String, String> map = new HashMap<>();
        map.put("config", getConfig());
        map.put("user", getUser());
        map.put("host", getHost());
        map.put("enabled", enabled.toString());
        map.put("latching", Boolean.toString(isLatching()));
        map.put("config_msg", toString());
        map.put("message_time", formatter.withZone(ZoneId.of("UTC")).format(getMessage_time()));
        return map;
    }

}
