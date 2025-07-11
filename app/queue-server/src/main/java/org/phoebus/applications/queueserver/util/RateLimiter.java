package org.phoebus.applications.queueserver.util;

import java.util.concurrent.TimeUnit;

/**
 * Thread-safe token-bucket limiter.<br>
 * Default - 10 permits / second.
 */
public final class RateLimiter {

    private final long   capacity;          // max tokens in the bucket
    private final double refillPerNanos;    // tokens added per nanosecond
    private double tokens;
    private long   lastRefill;              // nanoTime()

    public RateLimiter(double permitsPerSecond) {
        if (permitsPerSecond <= 0) throw new IllegalArgumentException("permitsPerSecond must be > 0");
        this.capacity       = Math.max(1, Math.round(permitsPerSecond));
        this.refillPerNanos = permitsPerSecond / TimeUnit.SECONDS.toNanos(1);
        this.tokens         = capacity;
        this.lastRefill     = System.nanoTime();
    }

    /** Blocks until a token is available, then consumes one. */
    public void acquire() throws InterruptedException {
        synchronized (this) {
            refill();
            while (tokens < 1.0) {
                long sleepNanos = (long) Math.ceil((1.0 - tokens) / refillPerNanos);
                TimeUnit.NANOSECONDS.timedWait(this, sleepNanos);
                refill();                     // wake-up → try again
            }
            tokens -= 1.0;
        }
    }

    private void refill() {
        long now = System.nanoTime();
        double add = (now - lastRefill) * refillPerNanos;
        if (add > 0) {
            tokens = Math.min(capacity, tokens + add);
            lastRefill = now;
        }
    }
}
