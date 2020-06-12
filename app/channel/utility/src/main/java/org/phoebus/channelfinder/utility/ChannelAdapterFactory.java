package org.phoebus.channelfinder.utility;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.phoebus.core.types.ProcessVariable;
import org.phoebus.channelfinder.Channel;
import org.phoebus.framework.adapter.AdapterFactory;

/**
 * A factory which adapts {@link Channel}s to {@link ProcessVariable}s
 * 
 * @author Kunal Shroff
 */
public class ChannelAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes = Arrays.asList(ProcessVariable.class, String.class);

    @Override
    public Class getAdaptableObject()
    {
        return Channel.class;
    }

    @Override
    public List<? extends Class> getAdapterList()
    {
        return adaptableTypes;
    }

    @Override
    public <T> Optional<T> adapt(Object adaptableObject, Class<T> adapterType)
    {

        Channel channel = ((Channel) adaptableObject);
        if (adapterType.isAssignableFrom(ProcessVariable.class))
        {
            return Optional.of(adapterType.cast(new ProcessVariable(channel.getName())));
        } else if (adapterType.isAssignableFrom(String.class))
        {
            return Optional.ofNullable(adapterType.cast("PV name : " + channel.getName()));
        }
        return Optional.ofNullable(null);
    }

}
