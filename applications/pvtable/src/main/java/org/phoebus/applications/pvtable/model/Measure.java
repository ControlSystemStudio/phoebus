package org.phoebus.applications.pvtable.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author A.PHILIPPE, L.PHILIPPE GANIL/FRANCE
 */
public class Measure {
    @SuppressWarnings("unused")
    private Configuration config = null;
    private List<PVTableItem> items = new ArrayList<PVTableItem>();

    /**
     * Initialize
     *
     * @param conf
     */
    public Measure(Configuration conf) {
        this.config = conf;
    }

    /**
     * @return The item list of this measure.
     */
    public List<PVTableItem> getItems() {
        return items;
    }
}
