package org.phoebus.applications.queueserver.util;

import org.phoebus.applications.queueserver.view.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages application lifecycle - especially cleanup when the app is closed
 * so that reopening within Phoebus gets a fresh state.
 */
public final class AppLifecycle {

    private static final Logger logger = Logger.getLogger(AppLifecycle.class.getPackageName());

    private static final List<Runnable> shutdownCallbacks = new ArrayList<>();

    private AppLifecycle() {}

    /**
     * Register a shutdown callback.
     * Called by controllers during initialization.
     */
    public static void registerShutdown(Runnable shutdown) {
        shutdownCallbacks.add(shutdown);
    }

    /**
     * Reset all static state for app restart.
     * Called by QueueServerInstance when the app tab is closed.
     */
    public static void shutdown() {
        try {
            logger.log(Level.INFO, "Queue Server app shutting down - resetting state");

            // Run all registered shutdown callbacks
            for (Runnable callback : shutdownCallbacks) {
                try {
                    callback.run();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error during shutdown callback", e);
                }
            }
            shutdownCallbacks.clear();

            // Reset all event buses - these just clear listeners, no callbacks fired
            try { StatusBus.reset(); } catch (Exception e) { /* ignore */ }
            try { QueueItemSelectionEvent.getInstance().reset(); } catch (Exception e) { /* ignore */ }
            try { ItemUpdateEvent.getInstance().reset(); } catch (Exception e) { /* ignore */ }
            try { PlanEditEvent.getInstance().reset(); } catch (Exception e) { /* ignore */ }
            try { TabSwitchEvent.getInstance().reset(); } catch (Exception e) { /* ignore */ }
            try { UiSignalEvent.reset(); } catch (Exception e) { /* ignore */ }
            try { PlansCache.reset(); } catch (Exception e) { /* ignore */ }

            // Reset Python converter so it can be re-initialized on next app open
            try { PythonParameterConverter.resetShared(); } catch (Exception e) { /* ignore */ }

            logger.log(Level.INFO, "Queue Server app state reset complete");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during app shutdown", e);
        }
    }
}
