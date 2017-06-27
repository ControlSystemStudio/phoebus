package org.phoebus.framework.selection;

import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Kunal Shroff
 *
 */
public class SelectionUtil {

    private SelectionUtil() {

    }

    public static Selection emptySelection() {
        return new Selection() {

            @Override
            public List getSelections() {
                return Collections.emptyList();
            }

        };
    }
    
    public static Selection createSelection(List<?> selection) {
        
        return new Selection() {
            private final List selected = selection;

            @Override
            public List getSelections() {
                return this.selected;
            }

        };
    }

}
