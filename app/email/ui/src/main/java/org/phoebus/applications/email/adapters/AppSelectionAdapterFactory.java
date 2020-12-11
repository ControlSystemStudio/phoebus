package org.phoebus.applications.email.adapters;

import org.phoebus.applications.email.EmailEntry;
import org.phoebus.framework.adapter.AdapterFactory;
import org.phoebus.ui.selection.AppSelection;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class AppSelectionAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes = Arrays.asList(EmailEntry.class);
    private static final Logger logger = Logger.getLogger(AppSelectionAdapterFactory.class.getName());

    @Override
    public Class getAdaptableObject()
    {
        return AppSelection.class;
    }

    @Override
    public List<? extends Class> getAdapterList()
    {
        return adaptableTypes;
    }

    @Override
    public <T> Optional<T> adapt(Object adaptableObject, Class<T> adapterType)
    {
        AppSelection selectionInfo = ((AppSelection) adaptableObject);

        if (adapterType.isAssignableFrom(EmailEntry.class))
        {
            EmailEntry emailEntry = new EmailEntry();

            StringBuffer title = new StringBuffer();
            title.append("Display Screenshot for : " + selectionInfo.getTitle());
            emailEntry.setSubject(title.toString());
            emailEntry.setBody(selectionInfo.getBody());
            emailEntry.setImages(List.of(selectionInfo.getImage().get()));

            return Optional.of(adapterType.cast(emailEntry));
        }
        return Optional.ofNullable(null);
    }

}
