package org.phoebus.applications.alarm.messages;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.phoebus.util.time.TimestampFormats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

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
    private int delay;
    private int count;
    private String filter;
    private List<Map<String, String>> guidance;
    private List<Map<String, String>> displays;
    private List<Map<String, String>> commands;
    private List<Map<String, String>> actions;
    private String delete;

    // The following fields are for logging purposes
    private String config;
    private Instant message_time;

    public AlarmConfigMessage() {
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

    public List<Map<String, String>> getGuidance() {
        return guidance;
    }

    public void setGuidance(List<Map<String, String>> guidance) {
        this.guidance = guidance;
    }

    public List<Map<String, String>> getDisplays() {
        return displays;
    }

    public void setDisplays(List<Map<String, String>> displays) {
        this.displays = displays;
    }

    public List<Map<String, String>> getCommands() {
        return commands;
    }

    public void setCommands(List<Map<String, String>> commands) {
        this.commands = commands;
    }

    public List<Map<String, String>> getActions() {
        return actions;
    }

    public void setActions(List<Map<String, String>> actions) {
        this.actions = actions;
    }

    public String getDelete() {
        return delete;
    }

    public void setDelete(String delete) {
        this.delete = delete;
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
    public boolean isLeaf() {
        return this.description != null;
    }

    @JsonIgnore
    @Override
    public String toString() {
        return "AlarmConfigMessage [user=" + user + ","
                + " host=" + host +
                ", description=" + description +
                ", delay=" + delay +
                ", count=" + count +
                ", filter=" + filter +
                ", guidance=" + guidance +
                ", displays=" + displays +
                ", commands=" + commands +
                ", actions=" + actions +
                ", delete=" + delete + "]";
    }

    @JsonIgnore
    public Map<String, String> sourceMap() {
        Map<String, String> map = new HashMap<>();
        map.put("config", getConfig());
        map.put("user", getUser());
        map.put("host", getHost());
        map.put("config_msg", toString());
        map.put("message_time", formatter.format(getMessage_time()));
        return map;
    }

}
