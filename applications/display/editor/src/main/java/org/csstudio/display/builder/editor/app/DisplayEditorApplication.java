/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.app;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.List;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.util.ModelResourceUtil;
import org.csstudio.display.builder.representation.javafx.FilenameSupport;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.util.ResourceParser;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.SaveAsDialog;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.docking.DockStage;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

/** Display Runtime Application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayEditorApplication implements AppResourceDescriptor
{
    public static final String NAME = "display_editor";
    public static final String DISPLAY_NAME = "Display Editor";

    /** Last local file that was opened.
     *  Used to offer as a location for downloading remote files.
     */
    private static File last_local_file = null;

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public URL getIconURL()
    {
        return DisplayModel.class.getResource("/icons/display.png");
    }

    @Override
    public List<String> supportedFileExtentions()
    {
        return DisplayModel.FILE_EXTENSIONS;
    }

    @Override
    public DisplayEditorInstance create()
    {
        return new DisplayEditorInstance(this);
    }

    @Override
    public DisplayEditorInstance create(final URI resource)
    {
        // Turn URI into the actual file,
        // (which may be a download of a remote URL),
        // so that existing instance can be uniquely identified
        final URI file_resource = getFileResource(resource);
        if (file_resource == null)
            return null;

        // Check for existing instance with that input
        final DisplayEditorInstance instance;
        final DockItemWithInput existing = DockStage.getDockItemWithInput(NAME, file_resource);
        if (existing != null)
        {   // Found one, raise it
            instance = existing.getApplication();
            instance.raise();
        }
        else
        {   // Nothing found, create new one
            instance = create();
            instance.loadDisplay(file_resource);
        }
        return instance;
    }

    /** Prompt for a file name to "save".
     *
     *  <p>Used to download a remote file,
     *  or to create a new file.
     *
     *  <p>File extension will be enforced.
     *
     *  @param title Dialog title
     *  @return File with proper file extension, or <code>null</code>
     */
    static File promptForFilename(final String title)
    {
        final Window window = DockPane.getActiveDockPane().getScene().getWindow();
        // Prevent error in dialog when it cannot navigate to the 'initial' file
        if (last_local_file != null)
        {
            if (! last_local_file.canRead())
                last_local_file = null;
        }
        File file = new SaveAsDialog().promptForFile(window, title, last_local_file, FilenameSupport.file_extensions);
        if (file == null)
            return null;
        file = ModelResourceUtil.enforceFileExtension(file, DisplayModel.FILE_EXTENSION);
        last_local_file = file;
        return file;
    }

    private URI getFileResource(final URI original_resource)
    {
        try
        {
            // Strip query from resource, because macros etc.
            // are only relevant to runtime, not to editor
            final URI resource = new URI(original_resource.getScheme(), original_resource.getUserInfo(),
                                         original_resource.getHost(), original_resource.getPort(),
                                         original_resource.getPath(), null, null);

            // Does the resource already point to a local file?
            File file = ModelResourceUtil.getFile(resource);
            if (file != null)
            {
                last_local_file = file;
                return file.toURI();
            }

            // Does user want to download into local file?
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            DialogHelper.positionDialog(alert, DockPane.getActiveDockPane(), -200, -100);
            alert.setResizable(true);
            alert.setTitle(Messages.DownloadTitle);
            alert.setHeaderText(MessageFormat.format(Messages.DownloadPromptFMT,
                                                     resource.toString()));
            // Setting "Yes", "No" buttons
            alert.getButtonTypes().clear();
            alert.getButtonTypes().add(ButtonType.YES);
            alert.getButtonTypes().add(ButtonType.NO);
            if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.NO)
                return null;

            // Prompt for local file
            final File local_file = promptForFilename(Messages.DownloadTitle);
            if (local_file == null)
                return null;

            // In background thread, ..
            JobManager.schedule(Messages.DownloadTitle, monitor ->
            {
                monitor.beginTask("Download " + resource);
                // .. download resource into local file ..
                try
                (
                    final InputStream read = ModelResourceUtil.openResourceStream(resource.toString());
                )
                {
                    Files.copy(read, local_file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                // .. and then, back on UI thread, open editor for the local file
                Platform.runLater(() -> create(ResourceParser.getURI(local_file)));
            });

            // For now, return null, no editor is opened right away.
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError("Error", "Cannot load " + original_resource, ex);
        }
        return null;
    }
}
