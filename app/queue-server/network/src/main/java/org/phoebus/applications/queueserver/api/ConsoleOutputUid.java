package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConsoleOutputUid(
        boolean success,
        String  msg,
        @JsonProperty("console_output_uid")
        String  uid
) {}