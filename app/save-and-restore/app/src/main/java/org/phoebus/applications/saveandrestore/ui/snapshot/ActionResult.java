/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot;

/**
 * UI helper enum indicating the result of a take snapshot or restore action.
 */
public enum ActionResult {
    PENDING, // User has not taken any action yet, or action in progress
    OK,
    FAILED
}
