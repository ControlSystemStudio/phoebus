package org.phoebus.apps.trends.simple.context;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.csstudio.trends.databrowser3.ui.selection.DatabrowserSelection;
import org.phoebus.applications.email.EmailEntry;
import org.phoebus.framework.adapter.AdapterFactory;

/**
 * A factory which adapts {@link DatabrowserSelection}s to {@link EmailEntry}s
 * 
 * @author Kunal Shroff
 *
 */
public class DatabrowserAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes = Arrays.asList(EmailEntry.class);

    @Override
    public Class getAdaptableObject()
    {
        return DatabrowserSelection.class;
    }

    @Override
    public List<? extends Class> getAdapterList()
    {
        return adaptableTypes;
    }

    @Override
    public <T> Optional<T> adapt(Object adaptableObject, Class<T> adapterType)
    {
        DatabrowserSelection databrowserSelection = ((DatabrowserSelection) adaptableObject);
        if (adapterType.isAssignableFrom(EmailEntry.class)) {
            return Optional.of(adapterType.cast(new EmailEntry(Messages.ActionEmailTitle,
                                                               Messages.ActionEmailBody,
                                                               List.of(databrowserSelection.getPlot()))));
        }
        return Optional.ofNullable(null);
    }

}
