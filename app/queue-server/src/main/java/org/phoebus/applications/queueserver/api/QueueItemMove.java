package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueItemMove(
        String uid,
        @JsonProperty("before_uid") String beforeUid,
        @JsonProperty("after_uid")  String afterUid) {

    public static QueueItemMove before(String uid, String ref) {
        return new QueueItemMove(uid, ref, null);
    }
    public static QueueItemMove after(String uid, String ref) {
        return new QueueItemMove(uid, null, ref);
    }
}
