package org.csstudio.display.builder.util;

import org.csstudio.display.builder.runtime.app.SelectionInfo;
import org.phoebus.applications.email.EmailEntry;
import org.phoebus.framework.adapter.AdapterFactory;
import org.phoebus.logbook.AttachmentImpl;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.ui.javafx.Screenshot;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.phoebus.logbook.LogEntryImpl.LogEntryBuilder.log;

/**
 * A factory which adapts {@link SelectionInfo}s to {@link EmailEntry}s
 *
 * @author Kunal Shroff
 *
 */
public class DisplayBuilderAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes = Arrays.asList(EmailEntry.class, LogEntry.class);
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
        SelectionInfo selectionInfo = ((SelectionInfo) adaptableObject);

        if (adapterType.isAssignableFrom(EmailEntry.class))
        {
            EmailEntry emailEntry = new EmailEntry();

            StringBuffer title = new StringBuffer();
            title.append("Display Screenshot for : " + selectionInfo.getName());
            emailEntry.setSubject(title.toString());
            emailEntry.setBody(getBody(selectionInfo));
            emailEntry.setImages(List.of(selectionInfo.getImage()));

            return Optional.of(adapterType.cast(emailEntry));
        }
        else if (adapterType.isAssignableFrom(LogEntry.class))
        {
            LogEntryBuilder log = log().title(LogbookPreferences.auto_title ?
                    "Display Screenshot for : " + selectionInfo.getName() : "")
                                       .appendDescription(getBody(selectionInfo));
            try
            {
                final File image_file = selectionInfo.getImage() == null ? null : new Screenshot(selectionInfo.getImage()).writeToTempfile("image");
                log.attach(AttachmentImpl.of(image_file));
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            return Optional.of(adapterType.cast(log.build()));
        }
        return Optional.ofNullable(null);
    }


    private String getBody(SelectionInfo selectionInfo)
    {
        StringBuffer body = new StringBuffer();
        body.append("Display Screenshot for the resource :" + System.lineSeparator());
        body.append(selectionInfo.toURI());
        body.append(System.lineSeparator());
        return body.toString();
    }

}
