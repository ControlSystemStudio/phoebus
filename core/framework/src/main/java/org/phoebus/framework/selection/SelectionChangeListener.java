package org.phoebus.framework.selection;

/**
 * 
 * @author Kunal Shroff
 *
 */
public interface SelectionChangeListener {

    /**
     * 
     * @param source
     * @param oldValue
     * @param newValue
     */
    public void selectionChanged(Object source, Selection oldValue, Selection newValue);
}
