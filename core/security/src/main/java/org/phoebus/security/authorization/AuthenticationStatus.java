/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.security.authorization;

/**
 * Enum indicating the current or updated authentication status.
 */
public enum AuthenticationStatus {
    UNDETERMINED,       // Not set, e.g. user not yet logged in
    AUTHENTICATED,      // Successfully authenticated after login call to service
    CACHED,             // User authenticated based on cached credentials
    BAD_CREDENTIALS,    // Not authenticated by service
    SERVICE_OFFLINE,    // Service not reachable
    UNKNOWN_ERROR       // Authentication with service failes for unknown reason
}
