/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.service.saveandrestore.filterselection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FilterSelectorTestConfig.class)
public class FilterSelectorTest {

    @Autowired
    private FilterSelector testFilterSelector;

    @Autowired
    private FilterSelectionHandler filterSelectionHandler;

    @Test
    public void testDiscoverFilterSelectors() {
        assertNotNull(testFilterSelector);
    }

    @Test
    public void setTestFilterSelectorNames() {
        assertEquals("a", testFilterSelector.getSupportedFilterNames().get(0));
        assertEquals("b", testFilterSelector.getSupportedFilterNames().get(1));
    }

    @Test
    public void testFilterSelectorHandler(){
        assertEquals(2, filterSelectionHandler.getSelectorFilterNames().size());
    }
}
