package org.phoebus.applications.queueserver.util;

import org.phoebus.applications.queueserver.api.QueueItem;
import org.phoebus.applications.queueserver.view.TabSwitchEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QueueItemSelectionEvent {

    private static final QueueItemSelectionEvent INSTANCE = new QueueItemSelectionEvent();

    private final List<Consumer<QueueItem>> listeners = new CopyOnWriteArrayList<>();
    private static final Logger logger = Logger.getLogger(QueueItemSelectionEvent.class.getPackageName());

    private QueueItemSelectionEvent() {}

    public static QueueItemSelectionEvent getInstance() {
        return INSTANCE;
    }

    public void addListener(Consumer<QueueItem> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<QueueItem> listener) {
        listeners.remove(listener);
    }

    public void notifySelectionChanged(QueueItem selectedItem) {
        for (Consumer<QueueItem> listener : listeners) {
            try {
                listener.accept(selectedItem);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in queue selection listener", e);
            }
        }
    }
}