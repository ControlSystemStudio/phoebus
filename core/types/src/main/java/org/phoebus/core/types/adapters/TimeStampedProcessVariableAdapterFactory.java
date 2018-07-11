package org.phoebus.core.types.adapters;

import static org.phoebus.logbook.LogEntryImpl.LogEntryBuilder.log;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.phoebus.core.types.TimeStampedProcessVariable;
import org.phoebus.framework.adapter.AdapterFactory;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogService;

/**
 * Provides a factory for converting a {@link TimeStampedProcessVariable} to either a {@link LogEntry} or {@link String}
 * @author Kunal Shroff
 *
 */
public class TimeStampedProcessVariableAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes = Arrays.asList(String.class, LogEntry.class);

    public Class getAdaptableObject() {
        return TimeStampedProcessVariable.class;
    }

    public Optional getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType.isAssignableFrom(LogEntry.class)) {
            TimeStampedProcessVariable tpv = ((TimeStampedProcessVariable) adaptableObject);
            LogEntry log = log().description("PV name: " + tpv.getName() + " " + tpv.getTime()).build();
            LogService.getInstance().createLogEntry(log);
            return Optional.of(log);
        } else if (adapterType.isAssignableFrom(String.class)) {
            TimeStampedProcessVariable tpv = ((TimeStampedProcessVariable) adaptableObject);
            return Optional.of("PV name: " + tpv.getName() + " " + tpv.getTime());
        }
        return Optional.ofNullable(null);
    }

    public List<? extends Class> getAdapterList() {
        return adaptableTypes;
    }

}
