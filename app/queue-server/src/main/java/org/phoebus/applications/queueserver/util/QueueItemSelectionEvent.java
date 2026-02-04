package org.phoebus.applications.queueserver.util;

import org.phoebus.applications.queueserver.api.QueueItem;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QueueItemSelectionEvent {

    private static final QueueItemSelectionEvent INSTANCE = new QueueItemSelectionEvent();

    private final List<Consumer<QueueItem>> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> selectAndRefreshListeners = new CopyOnWriteArrayList<>();
    private static final Logger logger = Logger.getLogger(QueueItemSelectionEvent.class.getPackageName());

    /** The last selected item's UID (for inserting after) */
    private volatile String lastSelectedUid = null;

    /** Pending selection request - UID that should be selected on next queue refresh */
    private volatile String pendingSelectionUid = null;

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
        // Track the last selected UID for insert-after operations
        lastSelectedUid = (selectedItem != null) ? selectedItem.itemUid() : null;

        for (Consumer<QueueItem> listener : listeners) {
            try {
                listener.accept(selectedItem);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in queue selection listener", e);
            }
        }
    }

    /**
     * Get the UID of the last selected item.
     * Returns null if no item is selected, meaning items should be added at the back.
     */
    public String getLastSelectedUid() {
        return lastSelectedUid;
    }

    /**
     * Request that a specific UID be selected after refreshing the queue.
     * This directly notifies listeners to refresh and select the item.
     */
    public void requestSelection(String uid) {
        pendingSelectionUid = uid;
        // Directly tell the queue controller to refresh and select this UID
        notifySelectAndRefreshListeners(uid);
    }

    /**
     * Add a listener that gets called with a UID to select after refreshing.
     * The listener should fetch the queue, rebuild, and select the item with that UID.
     */
    public void addSelectAndRefreshListener(Consumer<String> listener) {
        selectAndRefreshListeners.add(listener);
    }

    /**
     * Remove a select-and-refresh listener.
     */
    public void removeSelectAndRefreshListener(Consumer<String> listener) {
        selectAndRefreshListeners.remove(listener);
    }

    private void notifySelectAndRefreshListeners(String uid) {
        for (Consumer<String> listener : selectAndRefreshListeners) {
            try {
                listener.accept(uid);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in select-and-refresh listener", e);
            }
        }
    }

    /**
     * Get the pending selection UID (does not clear it).
     */
    public String getPendingSelection() {
        return pendingSelectionUid;
    }

    /**
     * Confirm that the pending selection was applied. Clears the pending selection.
     */
    public void confirmSelection() {
        pendingSelectionUid = null;
    }

    /** Reset state for app restart */
    public void reset() {
        listeners.clear();
        selectAndRefreshListeners.clear();
        lastSelectedUid = null;
        pendingSelectionUid = null;
    }
}
