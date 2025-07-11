package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;
import java.util.Map;

public record ConsoleOutputUpdate(
        boolean success,
        String  msg,
        @JsonProperty("console_output_msgs")
        List<Map<String,Object>> consoleOutputMsgs,
        @JsonProperty("last_msg_uid")
        String lastMsgUid
) {}
