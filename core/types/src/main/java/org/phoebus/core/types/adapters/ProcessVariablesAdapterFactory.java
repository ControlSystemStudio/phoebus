package org.phoebus.core.types.adapters;

import static org.phoebus.logbook.LogEntryImpl.LogEntryBuilder.log;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.adapter.AdapterFactory;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogService;

/**
 * Provide a factory to adapt a {@link ProcessVariable} to {@link LogEntry} or {@link String}
 * @author Kunal Shroff
 *
 */
@SuppressWarnings("rawtypes")
public class ProcessVariablesAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes = Arrays.asList(String.class, LogEntry.class);

    public Class getAdaptableObject() {
        return ProcessVariable.class;
    }

    public Optional getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType.isAssignableFrom(LogEntry.class)) {
            ProcessVariable tpv = ((ProcessVariable) adaptableObject);
            LogEntry log = log().description("PV name : " + tpv.getName()).build();
            LogService.getInstance().createLogEntry(log, null);
            return Optional.of(log);
        } else if (adapterType.isAssignableFrom(String.class)) {
            ProcessVariable tpv = ((ProcessVariable) adaptableObject);
            return Optional.ofNullable("PV name : " + tpv.getName());
        }
        return Optional.ofNullable(null);
    }

    public List<? extends Class> getAdapterList() {
        return adaptableTypes;
    }
}
