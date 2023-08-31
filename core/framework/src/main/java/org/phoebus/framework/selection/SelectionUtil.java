package org.phoebus.framework.selection;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author Kunal Shroff
 *
 */
public class SelectionUtil {
    private static final Selection EMPTY = createSelection(Collections.emptyList());

    // Allow only static access
    private SelectionUtil() {

    }

    /**
     * Empty selection
     * @return selection
     */
    public static Selection emptySelection() {
        return EMPTY;
    }

    /**
     * Create selection
     * @param <T>	Type of selection
     * @param selection selected Objects
     * @return selection
     */
    public static <T> Selection createSelection(List<T> selection) {

        return new Selection() {
            private final List<T> selected = selection;

            @SuppressWarnings("unchecked")
            @Override
            public List<T> getSelections() {
                return this.selected;
            }

        };
    }

}
