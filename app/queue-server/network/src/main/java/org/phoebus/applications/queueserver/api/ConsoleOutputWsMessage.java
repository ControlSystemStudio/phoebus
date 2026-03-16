package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WebSocket message from /console_output/ws endpoint.
 * Format: {"time": timestamp, "msg": text}
 */
public record ConsoleOutputWsMessage(
        @JsonProperty("time") double time,
        @JsonProperty("msg")  String msg
) {
    /**
     * Get timestamp as milliseconds since epoch.
     */
    public long timestampMillis() {
        return (long) (time * 1000);
    }
}
