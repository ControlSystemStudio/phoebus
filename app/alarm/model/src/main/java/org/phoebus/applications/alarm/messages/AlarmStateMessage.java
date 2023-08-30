package org.phoebus.applications.alarm.messages;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

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
 * A bean representing a alarm state message
 *
 * @author Kunal Shroff
 *
 */
@JsonInclude(Include.NON_NULL)
public class AlarmStateMessage {

    private static DateTimeFormatter formatter = TimestampFormats.MILLI_FORMAT;

    private String severity;
    private String message;
    private String value;
    private Map<String, String> time;
    private String current_severity;
    private String current_message;
    private String mode;
    private boolean notify = true;

    // The following fields are for logging purposes
    private Instant message_time;
    private boolean latch;

    private String config;
    private String pv;

    /** @return data */
    public String getConfig() {
        return config;
    }

    /** @param config New value */
    public void setConfig(String config) {
        this.config = config;
    }

    /** @return data */
    public String getPv() {
        return pv;
    }

    /** @param pv New value */
    public void setPv(String pv) {
        this.pv = pv;
    }

    /** @return data */
    public String getSeverity() {
        return severity;
    }

    /** @param severity New value */
    public void setSeverity(String severity) {
        this.severity = severity;
    }

    /** @return data */
    public boolean isLatch() {
        return latch;
    }

    /** @param latch New value */
    public void setLatch(boolean latch) {
        this.latch = latch;
    }

    /** @return data */
    public String getMessage() {
        return message;
    }

    /** @param message New value */
    public void setMessage(String message) {
        this.message = message;
    }

    /** @return data */
    public String getValue() {
        return value;
    }

    /** @param value New value */
    public void setValue(String value) {
        this.value = value;
    }

    /** @return data */
    public Map<String, String> getTime() {
        return time;
    }

    /** @param time New value */
    public void setTime(Map<String, String> time) {
        this.time = time;
    }

    /** @return data */
    public String getCurrent_severity() {
        return current_severity;
    }

    /** @param current_severity New value */
    public void setCurrent_severity(String current_severity) {
        this.current_severity = current_severity;
    }

    /** @return data */
    public String getCurrent_message() {
        return current_message;
    }

    /** @param current_message New value */
    public void setCurrent_message(String current_message) {
        this.current_message = current_message;
    }

    /** @return data */
    public String getMode() {
        return mode;
    }

    /** @param mode New value */
    public void setMode(String mode) {
        this.mode = mode;
    }

    /** @return data */
    public boolean getNotify() {
        return notify;
    }

    /** @param notify New value */
    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    /** @return data */
    public Instant getMessage_time() {
        return message_time;
    }

    /** @param message_time New value */
    public void setMessage_time(Instant message_time) {
        this.message_time = message_time;
    }

    /** @return data */
    @JsonIgnore
    public Instant getInstant() {
        return Instant.ofEpochSecond(Long.parseLong(time.get("seconds")), Long.parseLong(time.get("nano")));
    }

    /** @param instant New value */
    @JsonIgnore
    public void setInstant(Instant instant) {
        this.time = new HashMap<>();
        this.time.put("seconds", String.valueOf(instant.getEpochSecond()));
        this.time.put("nano", String.valueOf(instant.getNano()));
    }

    /** @return Is this a leaf node, i.e. a PV? */
    @JsonIgnore
    public boolean isLeaf() {
        return value != null && message != null && time != null && current_severity != null && current_message != null;
    }

    /** @return Map of original data */
    @JsonIgnore
    public Map<String, String> sourceMap() {
        Map<String, String> map = new HashMap<>();
        map.put("config", getConfig());
        map.put("pv", getPv());
        map.put("severity", getSeverity());
        map.put("latch", Boolean.toString(isLatch()));
        map.put("message", getMessage());
        map.put("value", getValue());
        map.put("time", formatter.withZone(ZoneId.of("UTC")).format(getInstant()));
        map.put("message_time", formatter.withZone(ZoneId.of("UTC")).format(getMessage_time()));
        map.put("current_severity", getCurrent_severity());
        map.put("current_message", getCurrent_message());
        map.put("mode", getMode());
        map.put("notify", Boolean.toString(getNotify()));
        return map;
    }

    @Override
    public String toString() {
        try {
            return AlarmMessageUtil.objectStateMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "failed to parse the alarm state message ", e);
        }
        return "";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((current_message == null) ? 0 : current_message.hashCode());
        result = prime * result + ((current_severity == null) ? 0 : current_severity.hashCode());
        result = prime * result + ((message == null) ? 0 : message.hashCode());
        result = prime * result + ((mode == null) ? 0 : mode.hashCode());
        result = prime * result + ((severity == null) ? 0 : severity.hashCode());
        result = prime * result + ((time == null) ? 0 : time.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AlarmStateMessage other = (AlarmStateMessage) obj;
        if (current_message == null) {
            if (other.current_message != null)
                return false;
        } else if (!current_message.equals(other.current_message))
            return false;
        if (current_severity == null) {
            if (other.current_severity != null)
                return false;
        } else if (!current_severity.equals(other.current_severity))
            return false;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        if (mode == null) {
            if (other.mode != null)
                return false;
        } else if (!mode.equals(other.mode))
            return false;
        if (severity == null) {
            if (other.severity != null)
                return false;
        } else if (!severity.equals(other.severity))
            return false;
        if (time == null) {
            if (other.time != null)
                return false;
        } else if (!time.equals(other.time))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

}
