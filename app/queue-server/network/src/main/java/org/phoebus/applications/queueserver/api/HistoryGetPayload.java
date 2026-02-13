package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record HistoryGetPayload(
        @JsonProperty("items") List<QueueItem> items,
        @JsonProperty("plan_history_uid") String planHistoryUid
) {}
