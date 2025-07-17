/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.model.search;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * A {@link FilterActivator} is a class activating or de-activating a {@link Filter}.
 * When the business logic evaluates to activation of a {@link Filter},
 * a registered callback is invoked. Conversely, when the business logic no longer evaluates to activation of a
 * {@link Filter}, a registered callback is invoked.
 *
 * <p>
 *     <b>NOTE:</b> Implementations must make sure to not select {@link Filter}s
 *     at the same time. Only one implementation at a given point in time should activate a particular {@link Filter}.
 * </p>
 *
 * <p>Example: based on one or several PV values a {@link FilterActivator} implementation may set up conditions to perform
 * a activation/de-activation of {@link Filter}s.</p>
 */
public abstract class FilterActivator {

    private Consumer<String> filterActivatedCallback;
    private Consumer<String> filterDeactivatedCallback;
    protected String activatedFilter;
    /**
     *
     * @return A list of {@link Filter} names supported by
     * all {@link FilterActivator} implementations.
     */
    public List<String> getSupportedFilterNames() {
        return Collections.emptyList();
    }

    /**
     * Registers the callbacks invoked when a {@link Filter} is
     * activated or de-activated. Calls the {@link #start()} method to ensure that any initial
     * state in the implementation is not set before the callbacks have been registered.
     * @param filterActivatedCallback The callback method called when filter is activated.
     * @param filterDeactivatedCallback The callback method called when filter is de-activated.
     */
    public  void setCallbacks(Consumer<String> filterActivatedCallback, Consumer<String> filterDeactivatedCallback) {
        this.filterActivatedCallback = filterActivatedCallback;
        this.filterDeactivatedCallback = filterDeactivatedCallback;
        start();
    }
    /**
     * Invokes callback when a {@link Filter} is activated.
     * @param filterName Unique name of a {@link Filter}
     */
    public void filterActivated(String filterName) {
        activatedFilter = filterName;
        if (filterActivatedCallback != null) {
            filterActivatedCallback.accept(filterName);
        }
    }

    /**
     * Invokes callback when a {@link Filter} is de-activated.
     * @param filterName Unique name of a {@link Filter}
     */
    public void filterDeactivated(String filterName) {
        activatedFilter = null;
        if (filterDeactivatedCallback != null) {
            filterDeactivatedCallback.accept(filterName);
        }
    }

    /**
     *
     * @return Name of a {@link Filter} if activated by any
     * implementation, otherwise <code>null</code>.
     */
    public String getActivatedFilter(){
        return activatedFilter;
    }

    /**
     * Implementations must make sure this method starts whatever needed to deliver both
     * initial state and any subsequent updates.
     */
    public abstract void start();

    /**
     * Implementations should override this to perform cleanup.
     */
    public abstract void stop();
}
