package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** Plan or instruction that can sit in the queue/history. */
public record QueueItem(
        @JsonProperty("item_type")  String itemType,   // "plan" | "instruction"
        String name,                                   // "count", "sleep", ...
        List<Object> args,
        Map<String,Object> kwargs,
        @JsonProperty("item_uid") String itemUid,
        String user,
        @JsonProperty("user_group") String userGroup,
        Map<String,Object> result
) {}
