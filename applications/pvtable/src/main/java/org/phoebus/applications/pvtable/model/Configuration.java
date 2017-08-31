package org.phoebus.applications.pvtable.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author A.PHILIPPE, L.PHILIPPE GANIL/FRANCE
 */
public class Configuration {

    private List<PVTableItem> items = new ArrayList<PVTableItem>();
    private List<Measure> measures = new ArrayList<Measure>();

    /**
     * Initialize the configuration with the header
     *
     * @param header
     */
    public Configuration(PVTableItem header) {
        items.add(header);
    }

    /**
     * Add the item to the conf list
     *
     * @param item
     */
    public void addItem(PVTableItem item) {
        items.add(item);
    }

    /**
     * Remove the item from the list
     *
     * @param item
     */
    public void removeItem(PVTableItem item) {
        item.setConf(false);
        items.remove(item);
    }

    /**
     * @return the items list of the conf
     */
    public List<PVTableItem> getItems() {
        return items;
    }

    /**
     * Get the list measures.
     *
     * @return
     */
    public List<Measure> getMeasures() {
        return measures;
    }

    /**
     * Replace measures with a List<Measure>.
     *
     * @param measureList
     *            (If it is null, measures
     */
    public void setMeasures(List<Measure> measureList)
    {
        this.measures = Objects.requireNonNull(measureList);
    }

    /**
     * Add a Measure to measures
     *
     * @return the measure added
     */
    public Measure addMeasure() {
        Measure measure = new Measure(this);
        if (this.measures == null) {
            this.measures = new ArrayList<Measure>();
        }
        measures.add(measure);
        return measure;
    }

    /**
     * Get the measure at the index i into measures
     *
     * @param i
     */
    public void getMeasure(int i) {
        measures.get(i);
    }

    /**
     * Remove the Measure measure from measures
     *
     * @param measure
     */
    public void removeMeasure(Measure measure) {
        measures.remove(measure);
    }

    public void removeAllMeasures() {
        measures.clear();
    }
}
