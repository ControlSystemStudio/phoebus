package org.phoebus.framework.selection;

import java.util.List;
import java.util.Optional;

/**
 * 
 * @author Kunal Shroff
 * @param <T>
 *
 */
public interface SelectionChangeListener {

    public void selectionChanged(Object source, Selection oldValue, Selection newValue);
}
