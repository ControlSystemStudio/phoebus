package org.phoebus.applications.queueserver.api;

public record ConsoleOutputText(
        boolean success,
        String  msg,
        String  text
) {}