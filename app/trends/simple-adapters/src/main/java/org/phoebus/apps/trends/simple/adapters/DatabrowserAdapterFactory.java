package org.phoebus.apps.trends.simple.adapters;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.csstudio.trends.databrowser3.ui.selection.DatabrowserSelection;
import org.phoebus.applications.email.EmailEntry;
import org.phoebus.framework.adapter.AdapterFactory;
import org.phoebus.logbook.AttachmentImpl;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.ui.javafx.Screenshot;

import static org.phoebus.logbook.LogEntryImpl.LogEntryBuilder.*;
import static org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;

/**
 * A factory which adapts {@link DatabrowserSelection}s to {@link EmailEntry}s
 * 
 * @author Kunal Shroff
 *
 */
public class DatabrowserAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes = Arrays.asList(EmailEntry.class, LogEntry.class);

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
        } else if (adapterType.isAssignableFrom(LogEntry.class)) {
            LogEntryBuilder log = log().title(LogbookPreferences.auto_title ? Messages.ActionLogbookTitle : "")
                                       .appendDescription(Messages.ActionLogbookBody);
            try {
                final File image_file = databrowserSelection.getPlot() == null ? null : new Screenshot(databrowserSelection.getPlot()).writeToTempfile("image");
                log.attach(AttachmentImpl.of(image_file));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Optional.of(adapterType.cast(log.build()));
        }
        return Optional.ofNullable(null);
    }

}
