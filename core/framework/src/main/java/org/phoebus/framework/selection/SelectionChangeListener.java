package org.phoebus.framework.selection;

/**
 * A listener which is notified when a viewer's selection changes.
 *
 * @author Kunal Shroff
 *
 */
public interface SelectionChangeListener {

    /**
     * This method gets notified when the selection changes.
     * @param source the source of the new selection 
     * @param oldValue the previous selection 
     * @param newValue the new selection
     */
    public void selectionChanged(Object source, Selection oldValue, Selection newValue);
}
