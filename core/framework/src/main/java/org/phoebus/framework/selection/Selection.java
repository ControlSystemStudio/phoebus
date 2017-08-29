package org.phoebus.framework.selection;

import java.util.List;

/**
 * Describes a selection provided by individual applications
 * @author Kunal Shroff
 *
 */
public interface Selection {

    /**
     * get the current list of selected objects
     * @return
     */
    public <T> List<T> getSelections();
}
