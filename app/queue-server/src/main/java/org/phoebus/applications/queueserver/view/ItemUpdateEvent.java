package org.phoebus.applications.queueserver.view;

import org.phoebus.applications.queueserver.api.QueueItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ItemUpdateEvent {
    private static final ItemUpdateEvent instance = new ItemUpdateEvent();
    private final List<Consumer<QueueItem>> listeners = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(ItemUpdateEvent.class.getPackageName());

    private ItemUpdateEvent() {}

    public static ItemUpdateEvent getInstance() {
        return instance;
    }

    public void addListener(Consumer<QueueItem> listener) {
        listeners.add(listener);
    }

    public void notifyItemUpdated(QueueItem updatedItem) {
        for (Consumer<QueueItem> listener : listeners) {
            try {
                listener.accept(updatedItem);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Item update listener error", e);
            }
        }
    }
}