package org.phoebus.applications.queueserver.util;

import org.phoebus.applications.queueserver.client.RunEngineService;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared cache for allowed plans data. Fetches once and notifies all listeners.
 * This avoids multiple controllers making redundant HTTP calls for the same data.
 */
public final class PlansCache {

    private static final Logger logger = Logger.getLogger(PlansCache.class.getPackageName());

    private static final ObjectProperty<Map<String, Map<String, Object>>> PLANS = new SimpleObjectProperty<>(new HashMap<>());
    private static final List<ChangeListener<? super Map<String, Map<String, Object>>>> listeners = new ArrayList<>();
    private static final AtomicBoolean loading = new AtomicBoolean(false);
    private static final AtomicBoolean loaded = new AtomicBoolean(false);

    private PlansCache() {}

    /**
     * Get the plans property for binding or adding listeners.
     */
    public static ObjectProperty<Map<String, Map<String, Object>>> plans() {
        return PLANS;
    }

    /**
     * Get the current plans map (never null, may be empty).
     */
    public static Map<String, Map<String, Object>> get() {
        Map<String, Map<String, Object>> p = PLANS.get();
        return p != null ? p : new HashMap<>();
    }

    /**
     * Check if plans have been loaded.
     */
    public static boolean isLoaded() {
        return loaded.get();
    }

    /**
     * Add a listener and track it for cleanup during reset.
     */
    public static void addListener(ChangeListener<? super Map<String, Map<String, Object>>> listener) {
        listeners.add(listener);
        PLANS.addListener(listener);
    }

    /**
     * Remove a tracked listener.
     */
    public static void removeListener(ChangeListener<? super Map<String, Map<String, Object>>> listener) {
        listeners.remove(listener);
        PLANS.removeListener(listener);
    }

    /**
     * Load plans from the server. Only makes one HTTP call even if called multiple times.
     * After loading, all registered listeners are notified.
     */
    public static void loadIfNeeded() {
        // Already loaded or currently loading - skip
        if (loaded.get() || !loading.compareAndSet(false, true)) {
            return;
        }

        logger.log(Level.FINE, "Loading allowed plans (shared cache)");

        new Thread(() -> {
            try {
                RunEngineService svc = new RunEngineService();
                Map<String, Object> responseMap = svc.plansAllowedRaw();

                Map<String, Map<String, Object>> newPlans = new HashMap<>();
                if (responseMap != null && Boolean.TRUE.equals(responseMap.get("success"))) {
                    if (responseMap.containsKey("plans_allowed")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Map<String, Object>> plans =
                            (Map<String, Map<String, Object>>) responseMap.get("plans_allowed");
                        if (plans != null) {
                            newPlans.putAll(plans);
                        }
                    }
                }

                final Map<String, Map<String, Object>> result = newPlans;
                Platform.runLater(() -> {
                    PLANS.set(result);
                    loaded.set(true);
                    loading.set(false);
                    logger.log(Level.FINE, "Loaded " + result.size() + " allowed plans into shared cache");
                });

            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to load allowed plans", e);
                loading.set(false);
            }
        }, "PlansCache-Loader").start();
    }

    /**
     * Force reload of plans (e.g., after reconnection).
     */
    public static void reload() {
        loaded.set(false);
        loading.set(false);
        loadIfNeeded();
    }

    /**
     * Reset state for app restart.
     */
    public static void reset() {
        Runnable doReset = () -> {
            for (ChangeListener<? super Map<String, Map<String, Object>>> listener : listeners) {
                PLANS.removeListener(listener);
            }
            listeners.clear();
            PLANS.set(new HashMap<>());
            loaded.set(false);
            loading.set(false);
        };

        if (Platform.isFxApplicationThread()) {
            doReset.run();
        } else {
            Platform.runLater(doReset);
        }
    }
}
