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
import java.util.Optional;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.util.CreateNewDisplayJob;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.WidgetClassSupport;
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
import org.phoebus.security.authorization.AuthorizationService;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import javafx.util.Duration;
import org.phoebus.util.FileExtensionUtil;

/** Display Runtime Application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DisplayEditorApplication implements AppResourceDescriptor
{
    private static final List<String> FILE_EXTENSIONS = List.of(DisplayModel.FILE_EXTENSION, DisplayModel.LEGACY_FILE_EXTENSION, WidgetClassSupport.FILE_EXTENSION);
    public static final String NAME = "display_editor";
    public static final String DISPLAY_NAME = Messages.DisplayApplicationName;

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
        return FILE_EXTENSIONS;
    }

    @Override
    public DisplayEditorInstance create()
    {
        if (!AuthorizationService.hasAuthorization("edit_display"))
        {
            // User does not have a permission to start editor
            final Alert alert = new Alert(Alert.AlertType.WARNING);
            DialogHelper.positionDialog(alert, DockPane.getActiveDockPane(), -200, -100);
            alert.initOwner(DockPane.getActiveDockPane().getScene().getWindow());
            alert.setResizable(true);
            alert.setTitle(DISPLAY_NAME);
            alert.setHeaderText(Messages.DisplayApplicationMissingRight);
            // Autohide in some seconds, also to handle the situation after
            // startup without edit_display rights but opening editor from memento
            PauseTransition wait = new PauseTransition(Duration.seconds(7));
            wait.setOnFinished((e) -> {
                Button btn = (Button)alert.getDialogPane().lookupButton(ButtonType.OK);
                btn.fire();
            });
            wait.play();
            
            alert.showAndWait();
            return null;
        }
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
            if (instance == null) return null;
            instance.loadDisplay(file_resource);
        }
        return instance;
    }

    /**
     *  Prompt for a file name to "save".
     *
     *  There are some corner cases to consider.
     *  <ol>
     *      <li>If user selects an existing file (irrespective of its file name extension), the native file
     *      chooser will prompt for overwrite. If user accepts to overwrite, a {@link File} object for
     *      that file will be returned.</li>
     *      <li>If user specifies a file name corresponding to an existing file when the .bob extension
     *      has been added, user will be presented with a prompt to confirm overwrite or cancel. If overwrite
     *      is selected, a {@link File} object for the existing file will be returned. If user does not wish
     *      to overwrite, <code>null</code> is returned.</li>
     *  </ol>
     *
     *  @param title Dialog title
     *  @return A {@link File} object, or <code>null</code> (e.g. if file selection was cancelled).
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
        if (file == null) {
            return null;
        }
        // Check if file exists on the file system. This is true only if user selects to overwrite an existing file
        // when prompted by the native file chooser.
        if(file.exists()){
            return file;
        }
        file = FileExtensionUtil.enforceFileExtension(file, DisplayModel.FILE_EXTENSION);
        // Check if the file exists on the file system when .bob extension has been enforced.
        // If it does, prompt user to cancel or overwrite.
        if(file.exists()){
            final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(Messages.NewDisplayOverwriteExistingTitle);
            alert.setHeaderText(MessageFormat.format(Messages.NewDisplayOverwriteExisting, file.getName(), file.getParentFile().getName()));
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get().equals(ButtonType.CANCEL)) {
                // User selects Cancel, or dismisses prompt
                return null;
            }
        }

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
            alert.initOwner(DockPane.getActiveDockPane().getScene().getWindow());
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
