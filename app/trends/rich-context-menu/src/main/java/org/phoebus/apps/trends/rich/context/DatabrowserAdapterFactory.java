package org.phoebus.apps.trends.rich.context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

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

        if (adapterType.isAssignableFrom(EmailEntry.class))
        {
            EmailEntry emailEntry = new EmailEntry();

            DatabrowserSelection databrowserSelection = ((DatabrowserSelection) adaptableObject);
            StringBuffer title = new StringBuffer();
            title.append("Databrowser Plot: " + databrowserSelection.getPlotTitle().get());
            emailEntry.setTitle(title.toString());

            StringBuffer body = new StringBuffer();
            body.append(databrowserSelection.getPlotTitle().get());
            body.append("for the time period: " +  databrowserSelection.getPlotTime().toAbsoluteInterval().toString());
            emailEntry.setBody(body.toString());

            emailEntry.setImages(List.of(databrowserSelection.getPlot()));

            try
            {
                File file = Files.createTempFile("phoebus", "-db-email.plt").toFile();
                try (FileOutputStream fileOutputStream = new FileOutputStream(file);)
                {
                    databrowserSelection.getPlotFile(fileOutputStream);
                    emailEntry.setFiles(List.of(file));
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "failed to attach databrowser file", e);
            }

            return Optional.of(adapterType.cast(emailEntry));
        }
        return Optional.ofNullable(null);
    }

}
