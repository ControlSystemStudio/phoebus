package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * WebSocket message from /status/ws endpoint.
 * Format: {"time": timestamp, "msg": {"status": {...}}}
 */
public record StatusWsMessage(
        @JsonProperty("time") double time,
        @JsonProperty("msg")  Map<String, Object> msg
) {
    /**
     * Get timestamp as milliseconds since epoch.
     */
    public long timestampMillis() {
        return (long) (time * 1000);
    }

    /**
     * Get the status payload from the message.
     * Returns null if not present.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> status() {
        if (msg == null) return null;
        return (Map<String, Object>) msg.get("status");
    }
}
