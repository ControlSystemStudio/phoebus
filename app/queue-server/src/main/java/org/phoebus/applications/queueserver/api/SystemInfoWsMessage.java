package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * WebSocket message from /info/ws endpoint.
 * Format: {"time": timestamp, "msg": {msg-class: msg-content}}
 * Currently includes status messages and may include additional system info in the future.
 */
public record SystemInfoWsMessage(
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
     * Get the status payload if this is a status message.
     * Returns null if not a status message.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> status() {
        if (msg == null) return null;
        return (Map<String, Object>) msg.get("status");
    }
}
