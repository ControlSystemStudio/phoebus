package org.phoebus.applications.queueserver.util;

import org.phoebus.applications.queueserver.api.StatusResponse;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A single instance that always holds the most-recent /api/status result
 * (or {@code null} when the server is offline).
 *
 * Widgets that need live status call
 * <pre> StatusBus.latest().addListener((obs,oldVal,newVal) -> { ... }) </pre>
 * or bind directly to the property.
 */
public final class StatusBus {

    private static final Logger logger = Logger.getLogger(StatusBus.class.getPackageName());
    private static final ObjectProperty<StatusResponse> LATEST = new SimpleObjectProperty<>(null);
    private static final List<ChangeListener<? super StatusResponse>> listeners = new ArrayList<>();

    private StatusBus() {}

    public static ObjectProperty<StatusResponse> latest() {
        return LATEST;
    }

    /**
     * Add a listener and track it for cleanup during reset.
     * Use this instead of latest().addListener() for proper lifecycle management.
     */
    public static void addListener(ChangeListener<? super StatusResponse> listener) {
        listeners.add(listener);
        LATEST.addListener(listener);
    }

    /**
     * Remove a tracked listener.
     */
    public static void removeListener(ChangeListener<? super StatusResponse> listener) {
        listeners.remove(listener);
        LATEST.removeListener(listener);
    }

    public static void push(StatusResponse s) {
        if (s != null) {
            logger.log(Level.FINEST, "Status update: " + s.managerState());
        } else {
            logger.log(Level.FINE, "Status cleared (server offline)");
        }

        if (Platform.isFxApplicationThread()) {
            LATEST.set(s);
        } else {
            Platform.runLater(() -> LATEST.set(s));
        }
    }

    /** Reset state for app restart - clears listeners first, then value (to avoid firing listeners during shutdown) */
    public static void reset() {
        Runnable doReset = () -> {
            // Remove all tracked listeners FIRST to prevent them from firing when we set null
            for (ChangeListener<? super StatusResponse> listener : listeners) {
                LATEST.removeListener(listener);
            }
            listeners.clear();
            // Now safe to set null - no listeners will fire
            LATEST.set(null);
        };

        if (Platform.isFxApplicationThread()) {
            doReset.run();
        } else {
            Platform.runLater(doReset);
        }
    }
}
