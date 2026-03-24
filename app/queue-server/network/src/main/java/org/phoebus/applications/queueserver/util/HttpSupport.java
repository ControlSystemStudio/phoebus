// src/main/java/com/jbi/util/HttpSupport.java
package org.phoebus.applications.queueserver.util;

import java.net.http.HttpRequest;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HttpSupport {

    private HttpSupport() {}

    public static final Logger logger = Logger.getLogger(HttpSupport.class.getPackageName());

    /* ---------------- retry policy ----------- */
    public static final int    MAX_RETRIES        = 3;
    public static final long   INITIAL_BACKOFF_MS = 200;
    public static final double BACKOFF_MULTIPLIER = 2.0;

    // Only these methods are idempotent → safe to retry
    private static final Set<String> IDEMPOTENT = Set.of("GET", "DELETE");

    public static boolean isRetryable(HttpRequest req) {
        return IDEMPOTENT.contains(req.method());
    }

    public static long elapsed(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000;
    }

    public static void fine(String msg) {
        logger.log(Level.FINE, msg);
    }
}
