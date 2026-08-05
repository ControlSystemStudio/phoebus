package org.phoebus.applications.alarm.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
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
    static final ObjectMapper objectStateMapper = JsonMapper.builder()
            .addMixIn(AlarmMessageUtil.class, AlarmStateJsonMessage.class)
            .build();

    // Object mapper for the alarm config messages
    @JsonIgnore
    static final ObjectMapper objectConfigMapper;
    static {
        SimpleModule simple_module = new SimpleModule();
        simple_module.addSerializer(new EnabledSerializer());

        objectConfigMapper = JsonMapper.builder()
                .addModule(simple_module)
                .addMixIn(AlarmMessageUtil.class, AlarmConfigJsonMessage.class)
                .build();
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
