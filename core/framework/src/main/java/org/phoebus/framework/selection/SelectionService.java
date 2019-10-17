package org.phoebus.framework.selection;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A service for allowing applications and tools to publish user selection/s
 * within their views. The services also allows for other modules to register
 * listeners so as to be notified when published selection/s are changed.
 *
 * @author Kunal Shroff
 *
 */
public class SelectionService {

    private static final SelectionService selectionService = new SelectionService();

    private static List<SelectionChangeListener> listeners = new CopyOnWriteArrayList<>();

    private static AtomicReference<Selection> selection = new AtomicReference<>(SelectionUtil.emptySelection());

    // Singleton
    private SelectionService() {
    }

    /**
     * Get an instance of the selection service
     *
     * @return An instance of the selection service
     */
    public static SelectionService getInstance() {
        return selectionService;
    }

    /**
     * Add a selection change listener
     *
     * @param selectionListner
     */
    public void addListener(SelectionChangeListener selectionListner) {
        listeners.add(selectionListner);
    }

    /**
     * Remove a selection change listener
     *
     * @param selectionListner
     */
    public void removeListener(SelectionChangeListener selectionListner) {
        listeners.remove(selectionListner);
    }

    /**
     * Get the current selection
     *
     * @return the current selection
     */
    public Selection getSelection() {
        return selection.get();
    }

    /**
     * Set the selection
     *
     * @param source    the source of the new selection
     * @param selection the new selection
     */
    public <T> void setSelection(Object source, List<T> selection) {
        final Selection newValue = SelectionUtil.createSelection(selection);
        final Selection oldValue = SelectionService.selection.getAndSet(newValue);
        for (SelectionChangeListener listener : listeners)
            listener.selectionChanged(source, oldValue, newValue);
    }
}
