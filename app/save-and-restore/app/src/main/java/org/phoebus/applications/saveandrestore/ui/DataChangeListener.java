/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui;

import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.search.Filter;

public interface DataChangeListener {

    default void nodeAddedOrRemoved(String parentNodeId){
    }

    default void nodeChanged(Node node){
    }

    default void filterAddedOrUpdated(Filter filter){
    }

    default void filterRemoved(String filterName){
    }
}
