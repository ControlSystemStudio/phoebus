/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.service.saveandrestore.filterselection;

import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.websocket.MessageType;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@link Component} responsible for managing {@link FilterSelector}s and expose methods
 * interacting with these from (for instance) {@link org.springframework.stereotype.Controller}s.
 */
@Component
public class FilterSelectionHandler {

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Autowired
    private NodeDAO nodeDAO;

    private final List<FilterSelector> filterSelectors;

    private Set<String> autoSelectFilterNames;

    public FilterSelectionHandler(List<FilterSelector> filterSelectors) {
        this.filterSelectors = filterSelectors;

    }

    @SuppressWarnings("unused")
    @PostConstruct
    public void initialize() {
        autoSelectFilterNames = new HashSet<>();
        this.filterSelectors.forEach(s -> {
            s.setFilterSelectedCallback(this::filterSelected);
            s.setFilterUnselectedCallback(this::filterUnselected);
            autoSelectFilterNames.addAll(s.getSupportedFilterNames());
        });
    }

    /**
     * @return A {@link Set} of unique {@link Filter} names supported
     * by all {@link FilterSelector} implementations known to the service.
     * Note that a {@link Filter} may be supported by multiple {@link FilterSelector}s,
     * but the list of names should not contain duplicates.
     */
    public Set<String> getSelectorFilterNames() {
        return autoSelectFilterNames;
    }

    /**
     * @return The name of a {@link Filter} currently selected by a {@link FilterSelector} implementations,
     * or <code>null</code> if no {@link Filter} has been selected.
     * Note that if multiple {@link FilterSelector}s select a {@link Filter}, then this method will return
     * the first one encountered.
     */
    public String getSelectedFilter() {
        for (FilterSelector filterSelector : filterSelectors) {
            if (filterSelector.getSelectedFilter() != null) {
                return filterSelector.getSelectedFilter();
            }
        }
        return null;
    }

    private void filterSelected(String filterName) {
        List<Filter> allFilters = nodeDAO.getAllFilters();
        Optional<Filter> filterOptional = allFilters.stream().filter(f -> f.getName().equals(filterName)).findFirst();
        filterOptional.ifPresent(filter -> webSocketHandler.sendMessage(new SaveAndRestoreWebSocketMessage(MessageType.FILTER_SELECTED, filter)));
    }

    private void filterUnselected(String filterName) {
        List<Filter> allFilters = nodeDAO.getAllFilters();
        Optional<Filter> filterOptional = allFilters.stream().filter(f -> f.getName().equals(filterName)).findFirst();
        filterOptional.ifPresent(filter -> webSocketHandler.sendMessage(new SaveAndRestoreWebSocketMessage(MessageType.FILTER_UNSELECTED, filter)));
    }

}
