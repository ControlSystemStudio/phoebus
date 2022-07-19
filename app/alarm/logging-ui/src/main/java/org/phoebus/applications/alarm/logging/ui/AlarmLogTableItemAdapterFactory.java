package org.phoebus.applications.alarm.logging.ui;

import org.phoebus.core.types.ProcessVariable;
import org.phoebus.core.types.TimeStampedProcessVariable;
import org.phoebus.framework.adapter.AdapterFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A factory which adapts {@link AlarmLogTableItem}s to {@link TimeStampedProcessVariable} and {@link ProcessVariable}s
 * 
 * @author Kunal Shroff
 */
public class AlarmLogTableItemAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes =
            Arrays.asList(ProcessVariable.class, TimeStampedProcessVariable.class);

    @Override
    public Class getAdaptableObject()
    {
        return AlarmLogTableItem.class;
    }

    @Override
    public List<? extends Class> getAdapterList()
    {
        return adaptableTypes;
    }

    @Override
    public <T> Optional<T> adapt(Object adaptableObject, Class<T> adapterType)
    {
        AlarmLogTableItem alarmLogTableItem = ((AlarmLogTableItem) adaptableObject);
        if (adapterType.isAssignableFrom(TimeStampedProcessVariable.class))
        {
            return Optional.of(adapterType.cast(
                    new TimeStampedProcessVariable(alarmLogTableItem.getPv(), alarmLogTableItem.getMessage_time())));
        } else if (adapterType.isAssignableFrom(ProcessVariable.class))
        {
            return Optional.of(adapterType.cast(new ProcessVariable(alarmLogTableItem.getPv())));
        } else if (adapterType.isAssignableFrom(String.class))
        {
            return Optional.ofNullable(adapterType.cast("PV name : " + alarmLogTableItem.getPv()));
        }
        return Optional.ofNullable(null);
    }

}
