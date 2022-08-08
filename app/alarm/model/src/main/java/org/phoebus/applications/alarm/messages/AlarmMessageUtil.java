package org.phoebus.applications.alarm.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.phoebus.applications.alarm.model.EnabledState;

import java.io.Serializable;
import java.util.List;

/**
 * A utility class with object mapper for the various alarm messages
 * 
 * @author Kunal Shroff
 *
 */


@JsonInclude(Include.NON_NULL)
public class AlarmMessageUtil implements Serializable{

    // Object mapper for the alarm state messages
   @JsonIgnore
    static final ObjectMapper objectStateMapper = new ObjectMapper();
    static {
        objectStateMapper.registerModule(new JavaTimeModule());
        objectStateMapper.addMixIn(AlarmMessageUtil.class, AlarmStateJsonMessage.class);
    }

    // Object mapper for the alarm config messages
    @JsonIgnore
    static final ObjectMapper objectConfigMapper = new ObjectMapper();
    static {
        SimpleModule simple_module = new SimpleModule();
        simple_module.addSerializer(new EnabledSerializer());

        objectConfigMapper.registerModule(new JavaTimeModule());
        objectConfigMapper.registerModule(simple_module);
        objectConfigMapper.addMixIn(AlarmMessageUtil.class, AlarmConfigJsonMessage.class);
    }

    // Object mapper for all other alarm messages
    @JsonIgnore
    static final ObjectMapper objectMapper = new ObjectMapper();

    private static class AlarmStateJsonMessage {
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

    private static class AlarmConfigJsonMessage {
        @JsonIgnore
        private EnabledState enabled;
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
