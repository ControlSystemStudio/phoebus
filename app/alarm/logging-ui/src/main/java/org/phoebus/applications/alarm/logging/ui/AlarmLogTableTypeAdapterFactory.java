package org.phoebus.applications.alarm.logging.ui;

import org.phoebus.core.types.ProcessVariable;
import org.phoebus.core.types.TimeStampedProcessVariable;
import org.phoebus.framework.adapter.AdapterFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A factory which adapts {@link AlarmLogTableType}s to {@link TimeStampedProcessVariable} and {@link ProcessVariable}s
 * 
 * @author Kunal Shroff
 */
public class AlarmLogTableTypeAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes =
            Arrays.asList(ProcessVariable.class, TimeStampedProcessVariable.class);

    @Override
    public Class getAdaptableObject()
    {
        return AlarmLogTableType.class;
    }

    @Override
    public List<? extends Class> getAdapterList()
    {
        return adaptableTypes;
    }

    @Override
    public <T> Optional<T> adapt(Object adaptableObject, Class<T> adapterType)
    {
        AlarmLogTableType alarmLogTableType = ((AlarmLogTableType) adaptableObject);
        if (adapterType.isAssignableFrom(TimeStampedProcessVariable.class))
        {
            return Optional.of(adapterType.cast(
                    new TimeStampedProcessVariable(alarmLogTableType.getPv(), alarmLogTableType.getMessage_time())));
        } else if (adapterType.isAssignableFrom(ProcessVariable.class))
        {
            return Optional.of(adapterType.cast(new ProcessVariable(alarmLogTableType.getPv())));
        } else if (adapterType.isAssignableFrom(String.class))
        {
            return Optional.ofNullable(adapterType.cast("PV name : " + alarmLogTableType.getPv()));
        }
        return Optional.ofNullable(null);
    }

}
