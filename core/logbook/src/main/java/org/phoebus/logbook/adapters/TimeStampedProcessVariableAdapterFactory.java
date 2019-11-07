package org.phoebus.logbook.adapters;

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

    public List<? extends Class> getAdapterList() {
        return adaptableTypes;
    }

    @Override
    public <T> Optional<T> adapt(Object adaptableObject, Class<T> adapterType) {
        if (adapterType.isAssignableFrom(LogEntry.class)) {
            TimeStampedProcessVariable tpv = ((TimeStampedProcessVariable) adaptableObject);
            LogEntry log = log().description("PV name: " + tpv.getName() + " " + tpv.getTime()).build();
            LogService.getInstance().createLogEntry(log, null);
            return Optional.of(adapterType.cast(log));
        } else if (adapterType.isAssignableFrom(String.class)) {
            TimeStampedProcessVariable tpv = ((TimeStampedProcessVariable) adaptableObject);
            return Optional.of(adapterType.cast("PV name : " + tpv.getName()));
        }
        return Optional.ofNullable(null);
    }
}
