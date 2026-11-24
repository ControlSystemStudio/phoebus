/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 */

package org.phoebus.saveandrestore.util;

/**
 * Determines if a restore operation should be performed in a threaded/asynchronous manner,
 * or one PV at a time. With channel access (ca) asynchronous writes may trigger a deadlock in
 * JCA, wich can be worked around with synchronous write operations.
 */
public enum RestorePolicy {
    ASYNCHRONOUS,
    SYNCHRONOUS,
}
