/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.logging;

import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.framework.adapter.AdapterFactory;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl;
import org.phoebus.logbook.PropertyImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.phoebus.logbook.LogEntryImpl.LogEntryBuilder.log;

/**
 * Adapts a selection of save&restore {@link Node}s to a log entry. Each selected {@link Node}
 * is added as a separate property with a name constructed from the {@link Node}'s name.
 */
public class NodeSelectionAdapterFactory implements AdapterFactory {

    @Override
    public Class getAdaptableObject() {
        return ArrayList.class;
    }

    @Override
    public List<? extends Class> getAdapterList() {
        return Arrays.asList(LogEntry.class);
    }

    @Override
    public <T> Optional<T> adapt(Object adaptableObject, Class<T> adapterType) {
        List<Node> selectedNodes = (List<Node>) adaptableObject;
        List<Map> properties = new ArrayList<>();
        selectedNodes.forEach(n -> {
            Map<String, String> map = new HashMap<>();
            map.put("file", "file:/" + n.getUniqueId() + "?app=saveandrestore");
            map.put("name", n.getName());
            properties.add(map);
        });

        LogEntryImpl.LogEntryBuilder log = log().description("");
        properties.forEach(m -> log.appendProperty(PropertyImpl.of("Save&restore resource " + m.get("name"), m)));
        return Optional.of(adapterType.cast(log.build()));
    }
}
