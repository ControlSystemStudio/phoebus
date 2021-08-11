package org.phoebus.logbook.olog.ui.adapters;

import org.phoebus.framework.adapter.AdapterFactory;
import org.phoebus.logbook.AttachmentImpl;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.logbook.olog.ui.LogbookUIPreferences;
import org.phoebus.ui.javafx.Screenshot;
import org.phoebus.ui.selection.AppSelection;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.phoebus.logbook.LogEntryImpl.LogEntryBuilder.log;


public class AppSelectionAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes = Arrays.asList(LogEntry.class);
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

        if (adapterType.isAssignableFrom(LogEntry.class))
        {
            LogEntryBuilder log = log().title(LogbookPreferences.auto_title ? selectionInfo.getTitle() : "")
                                       .appendDescription(selectionInfo.getBody());
            try
            {
                final File image_file = selectionInfo.getImage().get() == null ? null : new Screenshot(selectionInfo.getImage().get()).writeToTempfile("image");
                if(image_file != null)
                {
                    log.attach(AttachmentImpl.of(image_file));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Optional.of(adapterType.cast(log.build()));
        }
        return Optional.ofNullable(null);
    }
}
