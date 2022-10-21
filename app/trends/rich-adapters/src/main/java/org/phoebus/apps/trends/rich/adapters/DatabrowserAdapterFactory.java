package org.phoebus.apps.trends.rich.adapters;

import static org.phoebus.logbook.LogEntryImpl.LogEntryBuilder.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.csstudio.trends.databrowser3.ui.selection.DatabrowserSelection;
import org.phoebus.applications.email.EmailEntry;
import org.phoebus.framework.adapter.AdapterFactory;
import org.phoebus.logbook.AttachmentImpl;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryImpl.LogEntryBuilder;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.ui.javafx.Screenshot;

/**
 * A factory which adapts {@link DatabrowserSelection}s to {@link EmailEntry}s
 *
 * @author Kunal Shroff
 *
 */
public class DatabrowserAdapterFactory implements AdapterFactory {

    private static final List<? extends Class> adaptableTypes = Arrays.asList(EmailEntry.class, LogEntry.class);
    private static final Logger logger = Logger.getLogger(DatabrowserAdapterFactory.class.getName());

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

        if (adapterType.isAssignableFrom(EmailEntry.class))
        {
            EmailEntry emailEntry = new EmailEntry();

            StringBuffer title = new StringBuffer();
            title.append("Databrowser Plot");
            databrowserSelection.getPlotTitle().ifPresent(title::append);
            emailEntry.setSubject(title.toString());
            emailEntry.setBody(getBody(databrowserSelection));
            emailEntry.setImages(List.of(databrowserSelection.getPlot()));

            Optional<File> plotFile = getDatabrowserFile(databrowserSelection);
            if(plotFile.isPresent())
            {
                emailEntry.setFiles(List.of(plotFile.get()));
            }
            return Optional.of(adapterType.cast(emailEntry));
        }
        else if (adapterType.isAssignableFrom(LogEntry.class))
        {
            LogEntryBuilder log = log().title(LogbookPreferences.auto_title ? "Databrowser Plot" : "")
                                       .appendDescription(getBody(databrowserSelection));
            try
            {
                final File image_file = databrowserSelection.getPlot() == null ? null : new Screenshot(databrowserSelection.getPlot()).writeToTempfile("image");
                if(image_file != null){
                    log.attach(AttachmentImpl.of(image_file));
                }
                Optional<File> plotFile = getDatabrowserFile(databrowserSelection);
                if(plotFile.isPresent())
                {
                    log.attach(AttachmentImpl.of(plotFile.get(), "plt", false));
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to initiate log entry from adapter", e);
            }
            return Optional.of(adapterType.cast(log.build()));
        }
        return Optional.ofNullable(null);
    }

    /**
     * Formats the body of the log entry.
     *
     * List of PVs is separated both by two blanks and a line separator. This is needed in order to get both the
     * log entry editor and the Markdown text -> html conversion to render one PV per line.
     * @param databrowserSelection The data selected from Data Browser
     * @return The contents of the body text.
     */
    private String getBody(DatabrowserSelection databrowserSelection)
    {
        StringBuffer body = new StringBuffer();
        databrowserSelection.getPlotTitle().ifPresent(body::append);
        body.append("databrowser plot for the following pvs:  " + System.lineSeparator());
        body.append(databrowserSelection.getPlotPVs().stream().collect(Collectors.joining("  " + System.lineSeparator())));
        body.append("  " + System.lineSeparator());
        body.append("Over the time period: " +  databrowserSelection.getPlotTime().toAbsoluteInterval().toString());
        return body.toString();
    }

    private Optional<File> getDatabrowserFile(DatabrowserSelection databrowserSelection)
    {
        File file = null;
        try
        {
            // Create file name for a temp file
            file = Files.createTempFile("phoebus-db-email", System.currentTimeMillis() + ".plt").toFile();
            // Arrange for the file to be deleted on exit of JVM
            // (Hard to delete earlier since we don't know when the email submission completes)
            file.deleteOnExit();
            try (FileOutputStream fileOutputStream = new FileOutputStream(file);)
            {
                databrowserSelection.writePlotFile(fileOutputStream);
            }
        } catch (IOException e)
        {
            logger.log(Level.WARNING, "failed to attach databrowser file", e);
        }
        return Optional.ofNullable(file);
    }
}
