package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueItemAddBatch(

        @JsonProperty("items")       List<QueueItem> items,
        @JsonProperty("user")        String          user,
        @JsonProperty("user_group")  String          userGroup,
        @JsonProperty("after_uid")   String          afterUid
) {
    /** Convenience constructor – adds to end of queue. */
    public QueueItemAddBatch(List<QueueItem> items, String user, String userGroup) {
        this(items, user, userGroup, null);
    }
}
