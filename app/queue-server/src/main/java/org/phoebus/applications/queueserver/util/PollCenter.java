package org.phoebus.applications.queueserver.util;

import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

// Single ScheduledExecutor shared by all widgets that need periodic polling.
public final class PollCenter {

    private static final Logger logger = Logger.getLogger(PollCenter.class.getPackageName());
    private static final ScheduledExecutorService EXEC =
            Executors.newSingleThreadScheduledExecutor(r ->
                    new Thread(r, "JBI-poller"));

    private PollCenter() {}

    public static ScheduledFuture<?> every(long periodSec, Runnable task) {
        logger.log(Level.FINE, "Scheduling task with period: " + periodSec + " seconds");
        return EXEC.scheduleAtFixedRate(task, 0, periodSec, TimeUnit.SECONDS);
    }

    public static ScheduledFuture<?> everyMs(long periodMs, Runnable task) {
        logger.log(Level.FINE, "Scheduling task with period: " + periodMs + " milliseconds");
        return EXEC.scheduleAtFixedRate(task, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    public static ScheduledFuture<?> afterMs(long delayMs, Runnable task) {
        logger.log(Level.FINE, "Scheduling one-time task after: " + delayMs + " milliseconds");
        return EXEC.schedule(task, delayMs, TimeUnit.MILLISECONDS);
    }

    public static <T> ScheduledFuture<?> every(
            long periodSec,
            Supplier<T> supplier,
            java.util.function.Consumer<T> fxConsumer) {

        return every(periodSec, () -> {
            try {
                T t = supplier.get();
                javafx.application.Platform.runLater(() -> fxConsumer.accept(t));
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Polling task failed", ex);
            }
        });
    }

    public static <T> ScheduledFuture<?> everyMs(
            long periodMs,
            Supplier<T> supplier,
            java.util.function.Consumer<T> fxConsumer) {

        return everyMs(periodMs, () -> {
            try {
                T t = supplier.get();
                javafx.application.Platform.runLater(() -> fxConsumer.accept(t));
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Polling task failed", ex);
            }
        });
    }
}
