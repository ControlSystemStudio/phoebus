package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record QueueGetPayload(

        @JsonProperty("items")
        List<QueueItem> queue,

        @JsonProperty("running_item")  QueueItem runningItem,
        @JsonProperty("plan_queue_uid")   String planQueueUid,
        @JsonProperty("running_item_uid") String runningItemUid
) {}