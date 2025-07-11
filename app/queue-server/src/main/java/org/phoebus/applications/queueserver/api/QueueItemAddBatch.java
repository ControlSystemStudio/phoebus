package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;


public record QueueItemAddBatch(

        @JsonProperty("items")       List<QueueItem> items,
        @JsonProperty("user")        String          user,
        @JsonProperty("user_group")  String          userGroup
) {}
