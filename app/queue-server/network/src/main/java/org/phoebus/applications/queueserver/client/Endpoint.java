package org.phoebus.applications.queueserver.client;

public record Endpoint(HttpMethod method, String path) {}
