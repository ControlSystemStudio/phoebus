package org.csstudio.display.builder.editor.app;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.Preferences;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * A context menu action for creating a new display in the selection folder
 */
public class NewDisplayContextMenuEntry implements ContextMenuEntry
{
    private static final Class<?> supportedType = File.class;

    @Override
    public String getName()
    {
        return Messages.NewDisplay;
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(DisplayModel.class, "/icons/display.png");
    }

    @Override
    public Class<?> getSupportedType()
    {
        return supportedType;
    }

    /**
     * TODO error handlind, checking the selection is not empty, the selected root folder is not empty
     * @param selection The selection to be used to execute this action
     * @throws Exception failed to create a new display in the given folder
     */
    @Override
    public void call(Selection selection) throws Exception
    {
        // A new file can only reside in one folder, use the first selected folder
        Optional<File> folder = AdapterService.adapt(SelectionService.getInstance().getSelection().getSelections().get(0), File.class);
        // Prompt for file
        File userfile = new SaveAsDialog().promptForFile(DockPane.getActiveDockPane().getScene().getWindow(),
                Messages.NewDisplay,
                new File(folder.get(), "new_display"),
                FilenameSupport.file_extensions);

        final File file = ModelResourceUtil.enforceFileExtension(userfile, DisplayModel.FILE_EXTENSION);
        if (file == null)
        {
            ExceptionDetailsErrorDialog.openError("Failed to create new Display", "selected file is invalid", null);
        }

        JobManager.schedule(Messages.NewDisplay, monitor ->
        {
            // Create file with example content
            InputStream content;
            try
            {
                content = ModelResourceUtil.openResourceStream(Preferences.new_display_template);
            }
            catch (Exception e)
            {
                content = ModelResourceUtil.openResourceStream("examples:/initial.bob");
            }
            Files.copy(content, file.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // Open editor on UI thread
            Platform.runLater(() ->
                    ApplicationService.createInstance(DisplayEditorApplication.NAME, file.toURI()));
        });

    }
}
