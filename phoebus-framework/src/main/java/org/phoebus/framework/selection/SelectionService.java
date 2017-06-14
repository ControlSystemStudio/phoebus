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

    private static SelectionService selectionService = new SelectionService();

    @SuppressWarnings("rawtypes")
    private static List<SelectionChangeListener> listeners = Collections
            .synchronizedList(new ArrayList<SelectionChangeListener>());

    private static Selection selection = SelectionUtil.emptySelection();

    private SelectionService() {
    }

    public static SelectionService getInstance() {
        return selectionService;
    }

    public static void addListener(SelectionChangeListener selectionListner) {
        listeners.add(selectionListner);
    }

    public static void removeListener(SelectionChangeListener selectionListner) {
        listeners.remove(selectionListner);
    }

    @SuppressWarnings("unchecked")
    public static <T> void setSelection(Object source, List<T> selection) {
        Selection oldValue = SelectionService.selection;
        SelectionService.selection = SelectionUtil.createSelection(selection);
        listeners.forEach((s) -> {
            s.selectionChanged(source, oldValue, SelectionService.selection);
        });
    }
}
