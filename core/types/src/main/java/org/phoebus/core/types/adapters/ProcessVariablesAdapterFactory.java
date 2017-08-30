package org.phoebus.core.types.adapters;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.adapter.AdapterFactory;
import org.phoebus.logging.LogEntry;
import org.phoebus.logging.LogEntryFactory;

/**
 * Provide a factory to adapt a {@link ProcessVariable} to {@link LogEntry} or {@link String}
 * @author Kunal Shroff
 *
 */
public class ProcessVariablesAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes = Arrays.asList(String.class, LogEntry.class);

    public Class getAdaptableObject() {
        return ProcessVariable.class;
    }

    public Optional getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType.isAssignableFrom(LogEntry.class)) {
            ProcessVariable tpv = ((ProcessVariable) adaptableObject);
            return Optional.of(LogEntryFactory.buildLogEntry("PV name : " + tpv.getName()).create());
        } else if (adapterType.isAssignableFrom(String.class)) {
            ProcessVariable tpv = ((ProcessVariable) adaptableObject);
            return Optional.of("PV name : " + tpv.getName());
        }
        return Optional.ofNullable(null);
    }

    public List<? extends Class> getAdapterList() {
        return adaptableTypes;
    }
}
