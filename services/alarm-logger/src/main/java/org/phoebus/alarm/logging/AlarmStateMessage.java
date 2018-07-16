package org.phoebus.alarm.logging;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A bean representing a alarm state message
 * @author Kunal Shroff
 *
 */
public class AlarmStateMessage {

    public String severity;
    public String message;
    public String value;
    public Map<String, String> time;
    public String current_severity;
    public String current_message;
    public String mode;

    public AlarmStateMessage() {
        super();
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

    @JsonIgnore
    public Instant getInstant() {
        return Instant.ofEpochSecond(Long.parseLong(time.get("seconds")), Long.parseLong(time.get("nano")));
    }

    @JsonIgnore
    public boolean isLeaf() {
        return value != null && message != null && time != null && current_severity != null && current_message != null;
    }
    @Override
    public String toString() {
        return "AlarmStateMessage [severity=" + severity + ", message=" + message + ", value=" + value + ", time="
                + time + ", current_severity=" + current_severity + ", current_message=" + current_message + ", mode="
                + mode + "]";
    }

}
