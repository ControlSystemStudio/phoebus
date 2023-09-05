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
	 * @param <T> Object Type
	 * @return selected objects
	 */
    public <T> List<T> getSelections();
}
