/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.logging;

import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.framework.adapter.AdapterFactory;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl;
import org.phoebus.logbook.PropertyImpl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.phoebus.logbook.LogEntryImpl.LogEntryBuilder.log;

/**
 * Adapts a selection of save&restore {@link Node} to a log entry. The {@link Node} is
 * added as a resource property.
 */
public class NodeSelectionAdapterFactory implements AdapterFactory {

    @Override
    public Class getAdaptableObject() {
        return Node.class;
    }

    @Override
    public List<? extends Class> getAdapterList() {
        return Arrays.asList(LogEntry.class);
    }

    @Override
    public <T> Optional<T> adapt(Object adaptableObject, Class<T> adapterType) {
        Node selectedNode = (Node) adaptableObject;
        Map<String, String> map = new HashMap<>();
        map.put("file", "file:/" + selectedNode.getUniqueId() + "?app=saveandrestore");
        map.put("name", selectedNode.getName());

        LogEntryImpl.LogEntryBuilder log = log().description("")
                .appendProperty(PropertyImpl.of("resource", map));
        return Optional.of(adapterType.cast(log.build()));
    }
}
