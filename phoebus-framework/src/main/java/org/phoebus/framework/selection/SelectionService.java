package org.phoebus.framework.selection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.phoebus.framework.workbench.MenubarEntryService;

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

    public void addListener(SelectionChangeListener selectionListner) {
        listeners.add(selectionListner);
    }

    public void removeListener(SelectionChangeListener selectionListner) {
        listeners.remove(selectionListner);
    }

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
