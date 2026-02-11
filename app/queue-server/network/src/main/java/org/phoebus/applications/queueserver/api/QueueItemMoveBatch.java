package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueItemMoveBatch(
        List<String> uids,
        @JsonProperty("before_uid") String beforeUid,
        @JsonProperty("after_uid")  String afterUid) {

    public static QueueItemMoveBatch before(List<String> uids, String ref) {
        return new QueueItemMoveBatch(uids, ref, null);
    }
    public static QueueItemMoveBatch after(List<String> uids, String ref) {
        return new QueueItemMoveBatch(uids, null, ref);
    }
}
