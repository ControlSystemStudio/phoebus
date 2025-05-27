/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.service.saveandrestore.filterselection;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * A {@link FilterSelector} is a class that selects or unselects a {@link org.phoebus.applications.saveandrestore.model.search.Filter}.
 * When the business logic evaluates to selection of a {@link org.phoebus.applications.saveandrestore.model.search.Filter},
 * a registered callback is invoked. Conversely, when the business logic no longer evaluates to selection of a
 * {@link org.phoebus.applications.saveandrestore.model.search.Filter}, a registered callback is invoked.
 *
 * <p>
 *     Implementations must be Spring {@link org.springframework.stereotype.Component}s such that the {@link FilterSelectionHandler}
 *     can discover them. The Java SPI approach is <b>not</b> used.
 * </p>
 *
 * <p>
 *     <b>NOTE:</b> Implementations must make sure to not select {@link org.phoebus.applications.saveandrestore.model.search.Filter}s
 *     at the same time. Only one implementation at a given point in time should select a {@link org.phoebus.applications.saveandrestore.model.search.Filter}.
 * </p>
 *
 * <p>Example: based on one or several PV values an implementation can set up conditions to perform a selection
 * on different {@link org.phoebus.applications.saveandrestore.model.search.Filter}s.</p>
 *
 * <p>The {@link org.phoebus.service.saveandrestore.web.controllers.FilterController} API will ensure that if a
 * {@link org.phoebus.applications.saveandrestore.model.search.Filter} is supported by any of the {@link FilterSelector}
 * implementations, deletion of that {@link org.phoebus.applications.saveandrestore.model.search.Filter} is blocked.</p>
 */
public abstract class FilterSelector {

    private Consumer<String> filterSelectedCallback;
    private Consumer<String> filterUnselectedCallback;
    protected String selectedFilter;

    /**
     *
     * @return A list of {@link org.phoebus.applications.saveandrestore.model.search.Filter} names supported by
     * all {@link FilterSelector} implementations.
     */
    public List<String> getSupportedFilterNames() {
        return Collections.emptyList();
    }

    /**
     * Registers the callback invoked when a {@link org.phoebus.applications.saveandrestore.model.search.Filter} is
     * selected.
     * @param callback The callback method.
     */
    public void setFilterSelectedCallback(Consumer<String> callback) {
        this.filterSelectedCallback = callback;
    }

    /**
     * Registers the callback invoked when a {@link org.phoebus.applications.saveandrestore.model.search.Filter} is
     * unselected.
     * @param callback The callback method.
     */
    public void setFilterUnselectedCallback(Consumer<String> callback) {
        this.filterUnselectedCallback = callback;
    }

    /**
     * Invokes callback when a {@link org.phoebus.applications.saveandrestore.model.search.Filter} is selected.
     * @param filterName Unique name of a {@link org.phoebus.applications.saveandrestore.model.search.Filter}
     */
    public void filterSelected(String filterName) {
        selectedFilter = filterName;
        if (filterSelectedCallback != null) {
            filterSelectedCallback.accept(filterName);
        }
    }

    /**
     * Invokes callback when a {@link org.phoebus.applications.saveandrestore.model.search.Filter} is unselected.
     * @param filterName Unique name of a {@link org.phoebus.applications.saveandrestore.model.search.Filter}
     */
    public void filterUnselected(String filterName) {
        selectedFilter = null;
        if (filterUnselectedCallback != null) {
            filterUnselectedCallback.accept(filterName);
        }
    }

    /**
     *
     * @return Name of a {@link org.phoebus.applications.saveandrestore.model.search.Filter} if selected by any
     * implementation, otherwise <code>null</code>.
     */
    public String getSelectedFilter(){
        return selectedFilter;
    }
}
