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
    private static final Logger logger = Logger.getLogger(QueueItemSelectionEvent.class.getPackageName());

    /** The last selected item's UID (for inserting after) */
    private volatile String lastSelectedUid = null;

    /**
     * Exact UIDs to select on the next queue refresh.
     * Set by controllers after a successful add/copy HTTP call returns the new item UIDs.
     * The queue controller checks this on every refresh and selects matching rows.
     */
    private volatile List<String> pendingSelectUids = null;

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

    /**
     * Set the UID of the last selected item (for insert-after operations).
     */
    public void setLastSelectedUid(String uid) {
        this.lastSelectedUid = uid;
    }

    /**
     * Get the UID of the last selected item.
     * Returns null if no item is selected, meaning items should be added at the back.
     */
    public String getLastSelectedUid() {
        return lastSelectedUid;
    }

    /**
     * Request that the queue controller select specific UIDs on the next refresh.
     * Called from background threads after a successful add/copy HTTP response.
     */
    public void requestSelectByUids(List<String> uids) {
        pendingSelectUids = List.copyOf(uids);
    }

    /**
     * Get the UIDs to select, or null if there are none pending.
     */
    public List<String> getPendingSelectUids() {
        return pendingSelectUids;
    }

    /**
     * Clear pending selection (called after UIDs were found and selected,
     * or when the operation failed).
     */
    public void clearPendingSelect() {
        pendingSelectUids = null;
    }

    /**
     * Check if there are UIDs waiting to be selected.
     */
    public boolean hasPendingSelect() {
        return pendingSelectUids != null;
    }

    /** Reset state for app restart */
    public void reset() {
        listeners.clear();
        lastSelectedUid = null;
        pendingSelectUids = null;
    }
}
