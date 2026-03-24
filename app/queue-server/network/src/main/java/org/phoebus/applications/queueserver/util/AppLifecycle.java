package org.phoebus.applications.queueserver.util;

import org.phoebus.applications.queueserver.view.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages application lifecycle with reference counting.
 * Multiple queue-server apps (monitor, edit-control) can be open simultaneously.
 * Shared state is only reset when the last app is closed.
 */
public final class AppLifecycle {

    private static final Logger logger = Logger.getLogger(AppLifecycle.class.getPackageName());
    private static final List<Runnable> shutdownCallbacks = new ArrayList<>();
    private static final AtomicInteger activeApps = new AtomicInteger(0);

    private AppLifecycle() {}

    /** Called when a queue-server app is opened. */
    public static void registerApp() {
        int count = activeApps.incrementAndGet();
        logger.log(Level.FINE, "App registered, active count: " + count);
    }

    /**
     * Called when a queue-server app tab is closed.
     * Shared state is reset only when the last app closes.
     */
    public static void unregisterApp() {
        int count = activeApps.decrementAndGet();
        logger.log(Level.FINE, "App unregistered, active count: " + count);
        if (count <= 0) {
            activeApps.set(0);
            shutdown();
        }
    }

    /** Register a shutdown callback. */
    public static void registerShutdown(Runnable shutdown) {
        shutdownCallbacks.add(shutdown);
    }

    /** Reset all static state. Called when the last app closes. */
    private static void shutdown() {
        try {
            logger.log(Level.INFO, "Last queue-server app closed - resetting state");

            for (Runnable callback : shutdownCallbacks) {
                try {
                    callback.run();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error during shutdown callback", e);
                }
            }
            shutdownCallbacks.clear();

            // Shut down connection manager
            try { ConnectionManager.getInstance().shutdown(); } catch (Exception e) { /* ignore */ }

            // Reset all event buses
            try { StatusBus.reset(); } catch (Exception e) { /* ignore */ }
            try { QueueItemSelectionEvent.getInstance().reset(); } catch (Exception e) { /* ignore */ }
            try { ItemUpdateEvent.getInstance().reset(); } catch (Exception e) { /* ignore */ }
            try { PlanEditEvent.getInstance().reset(); } catch (Exception e) { /* ignore */ }
            try { TabSwitchEvent.getInstance().reset(); } catch (Exception e) { /* ignore */ }
            try { UiSignalEvent.reset(); } catch (Exception e) { /* ignore */ }
            try { PlansCache.reset(); } catch (Exception e) { /* ignore */ }

            // Reset Python converter
            try { PythonParameterConverter.resetShared(); } catch (Exception e) { /* ignore */ }

            logger.log(Level.INFO, "Queue Server state reset complete");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during app shutdown", e);
        }
    }
}
