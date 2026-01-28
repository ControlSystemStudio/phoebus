package org.phoebus.applications.queueserver.util;

import org.phoebus.applications.queueserver.api.StatusResponse;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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

    private StatusBus() {}

    public static ObjectProperty<StatusResponse> latest() {
        return LATEST;
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
}
