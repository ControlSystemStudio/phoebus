package org.phoebus.applications.alarm.messages;

import static org.phoebus.applications.alarm.AlarmSystem.logger;
import static org.phoebus.applications.alarm.messages.AlarmMessageUtil.objectMapper;

import java.util.logging.Level;

import com.fasterxml.jackson.core.JsonProcessingException;

/** Alarm detail */
public class AlarmDetail {

    private String title;
    private String details;
    private int delay;

    /** Constructor */
    public AlarmDetail() {
        super();
    }

    /** @param title Title
     *  @param action Action
     */
    public AlarmDetail(String title, String action) {
        super();
        this.title = title;
        this.details = action;
    }

    /** @return Title */
    public String getTitle() {
        return title;
    }

    /** @param title New value */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @return Details */
    public String getDetails() {
        return details;
    }

    /** @param details New value */
    public void setDetails(String details) {
        this.details = details;
    }

    /** @return Delay */
    public int getDelay() {
        return delay;
    }

    /** @param delay New value */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "failed to parse the alarm detail message ", e);
        }
        return "";
    }

}
