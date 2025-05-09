/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.logbook;

/**
 * Encapsulates &quot;level&quot; data managed on the server side.
 * @param name The name, presented in UI and contained on a log entry
 * @param defaultLevel Indicates if the instantiated object is the
 *                     default level. Server will ensure only one single
 *                     level is maintained as the default level.
 */
public record LogEntryLevel(String name, boolean defaultLevel) {
}
