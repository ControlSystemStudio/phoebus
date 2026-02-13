package org.phoebus.applications.queueserver.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Generic <T> wrapper for virtually every Bluesky HTTP response. */
public record Envelope<T>(
        boolean success,
        @JsonProperty("msg")    String msg,
        @JsonAlias("detail")    String detail,
        T payload               // may be null
) {}
