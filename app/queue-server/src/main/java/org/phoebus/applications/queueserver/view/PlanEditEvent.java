package org.phoebus.applications.queueserver.view;

import org.phoebus.applications.queueserver.api.QueueItem;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlanEditEvent {

    private static final PlanEditEvent INSTANCE = new PlanEditEvent();

    private final List<Consumer<QueueItem>> listeners = new CopyOnWriteArrayList<>();
    private static final Logger logger = Logger.getLogger(PlanEditEvent.class.getPackageName());

    private PlanEditEvent() {}

    public static PlanEditEvent getInstance() {
        return INSTANCE;
    }

    public void addListener(Consumer<QueueItem> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<QueueItem> listener) {
        listeners.remove(listener);
    }

    public void notifyEditRequested(QueueItem itemToEdit) {
        for (Consumer<QueueItem> listener : listeners) {
            try {
                listener.accept(itemToEdit);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in plan edit listener", e);
            }
        }
    }
}