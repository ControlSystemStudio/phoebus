package org.csstudio.display.builder.util;

import org.csstudio.display.builder.runtime.app.SelectionInfo;
import org.phoebus.applications.email.EmailEntry;
import org.phoebus.framework.adapter.AdapterFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * A factory which adapts {@link SelectionInfo}s to {@link EmailEntry}s
 *
 * @author Kunal Shroff
 *
 */
public class DisplayBuilderAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes = Arrays.asList(EmailEntry.class);
    private static final Logger logger = Logger.getLogger(DisplayBuilderAdapterFactory.class.getName());

    @Override
    public Class getAdaptableObject()
    {
        return SelectionInfo.class;
    }

    @Override
    public List<? extends Class> getAdapterList()
    {
        return adaptableTypes;
    }

    @Override
    public <T> Optional<T> adapt(Object adaptableObject, Class<T> adapterType)
    {

        if (adapterType.isAssignableFrom(EmailEntry.class))
        {
            EmailEntry emailEntry = new EmailEntry();

            SelectionInfo selectionInfo = ((SelectionInfo) adaptableObject);
            StringBuffer title = new StringBuffer();
            title.append("Display Screenshot for : " + selectionInfo.getName());
            emailEntry.setSubject(title.toString());

            StringBuffer body = new StringBuffer();
            body.append("Display Screenshot for the resource :" + System.lineSeparator());
            body.append(selectionInfo.toURI());
            body.append(System.lineSeparator());
            emailEntry.setBody(body.toString());

            emailEntry.setImages(List.of(selectionInfo.getImage()));

            return Optional.of(adapterType.cast(emailEntry));
        }
        return Optional.ofNullable(null);
    }

}
