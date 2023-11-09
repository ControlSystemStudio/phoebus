package org.csstudio.trends.databrowser3.ui.sampleview;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import static org.csstudio.trends.databrowser3.Messages.*;

public class ItemSampleViewFilter {
    public enum FilterType {
        NO_FILTER(SampleView_ItemFilter_NO_FILTER),
        ALARM_UP(SampleView_ItemFilter_ALARM_UP),
        ALARM_CHANGES(SampleView_ItemFilter_ALARM_CHANGES),
        THRESHOLD_UP(SampleView_ItemFilter_THRESHOLD_UP),
        THRESHOLD_CHANGES(SampleView_ItemFilter_THRESHOLD_CHANGES),
        THRESHOLD_DOWN(SampleView_ItemFilter_THRESHOLD_DOWN);

        private final String label;

        FilterType(String label) {
            this.label = label;
        }

        /** @return Array of display names for all Filter types */
        public static String[] getDisplayNames() {
            final FilterType[] types = FilterType.values();
            final String[] names = new String[types.length];
            for (int i=0; i<names.length; ++i)
                names[i] = types[i].label;
            return names;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    private FilterType state;
    private final DoubleProperty filterValue = new SimpleDoubleProperty();

    public ItemSampleViewFilter() {
        this.state = FilterType.NO_FILTER;
        this.filterValue.set(0.0d);
    }

    public ItemSampleViewFilter(FilterType type) {
        this.state = type;
        this.filterValue.set(0.0d);
    }

    public ItemSampleViewFilter(ItemSampleViewFilter source) {
        this.state = source.getFilterType();
        this.filterValue.set(source.getFilterValue());
    }

    public FilterType getFilterType() {
        return state;
    }

    public void setFilterType(FilterType state) {
        this.state = state;
    }

    public Double getFilterValue() {
        return filterValue.get();
    }

    public DoubleProperty filterValueProperty() {
        return filterValue;
    }

    public void setFilterValue(double filterValue) {
        this.filterValue.set(filterValue);
    }
}
