package org.phoebus.framework.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Kunal Shroff
 *
 */
public class SelectionService {

    private static SelectionService selectionService;

    private static List<SelectionChangeListener> listeners = Collections
            .synchronizedList(new ArrayList<SelectionChangeListener>());

    private static Selection selection = SelectionUtil.emptySelection();

    private SelectionService() {
    }

    public static synchronized SelectionService getInstance() {
        if (selectionService == null) {
            selectionService = new SelectionService();
        }
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
    public synchronized Selection getSelection() {
        return selection;
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> void setSelection(Object source, List<T> selection) {
        Selection oldValue = SelectionService.selection;
        SelectionService.selection = SelectionUtil.createSelection(selection);
        listeners.forEach((s) -> {
            s.selectionChanged(source, oldValue, SelectionService.selection);
        });
    }
}
