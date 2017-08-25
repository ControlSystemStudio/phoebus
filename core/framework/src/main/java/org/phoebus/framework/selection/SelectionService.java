package org.phoebus.framework.selection;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
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
     * @return
     */
    public Selection getSelection() {
        return selection.get();
    }

    public <T> void setSelection(Object source, List<T> selection) {
        final Selection newValue = SelectionUtil.createSelection(selection);
        final Selection oldValue = SelectionService.selection.getAndSet(newValue);
        for (SelectionChangeListener listener : listeners)
            listener.selectionChanged(source, oldValue, newValue);
    }
}
