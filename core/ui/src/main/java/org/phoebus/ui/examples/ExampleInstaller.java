/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.examples;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.framework.workbench.FileHelper;
import org.phoebus.ui.Messages;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/** Helper for installing examples
 *
 *  <p>Prompts for an installation directory.
 *  Copies examples from a resource (local directory or folder inside a jar)
 *  into chosen installation directory,
 *  and then opens one of the installed files
 *  with given application.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ExampleInstaller implements Callable<Boolean>
{
    private static final Logger logger = Logger.getLogger(ExampleInstaller.class.getPackageName());

    private final String directory_prompt;
    private final URL examples_resource;
    private final String examples_directory;
    private final String example_to_open;
    private final String example_application;
    private final String completion_message_format;

    /** Prepare installer
     *  @param directory_prompt Prompt (title) used when requesting installation directory
     *  @param examples_resource URL for local file or folder in jar that contains examples
     *  @param examples_directory Name of new folder to create with installation directory for examples
     *  @param example_to_open Name of file in installed examples to open
     *  @param example_application Application to open the example_to_open
     *  @param completion_message_format Format for message invoked with installation directory when done
     */
    public ExampleInstaller(final String directory_prompt,
                            final URL examples_resource,
                            final String examples_directory,
                            final String example_to_open,
                            final String example_application,
                            final String completion_message_format)
    {
        this.directory_prompt = directory_prompt;
        this.examples_resource = examples_resource;
        this.examples_directory = examples_directory;
        this.example_to_open = example_to_open;
        this.example_application = example_application;
        this.completion_message_format = completion_message_format;
    }

    /** Run the installer
     *  @throws Exception on error
     *  @return true when installed, false when aborted
     */
    @Override
    public Boolean call() throws Exception
    {
        // Prompt for installation directory
        final Control pane = DockPane.getActiveDockPane();
        final Window window = pane.getScene().getWindow();
        final DirectoryChooser select = new DirectoryChooser();
        select.setTitle(directory_prompt);
        final File dir = select.showDialog(window);
        if (dir == null)
            return false;

        final File examples = new File(dir, examples_directory);

        // Prompt user: OK to overwrite?
        if (examples.exists())
        {
            final Alert confirm = new Alert(AlertType.CONFIRMATION);
            DialogHelper.positionDialog(confirm, pane, -300, -200);
            confirm.setTitle(Messages.InstallExamples);
            confirm.setHeaderText(MessageFormat.format(Messages.ReplaceExamplesWarningFMT, examples.toString()));
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                return false;
        }

        logger.log(Level.INFO, "Install from " + examples_resource + " into " + examples);

        // In background:
        JobManager.schedule(Messages.InstallExamples, monitor ->
        {
            if (examples.exists())
            {   // Delete existing directory
                logger.log(Level.INFO, "Deleting existing " + examples);
                monitor.beginTask("Delete " + examples);
                FileHelper.delete(examples);
            }

            // Install examples, which may be in local file system or JAR
            monitor.beginTask("Install " + examples);
            if (examples_resource.getProtocol().equals("jar"))
            {   // Resource is inside a jar:  file:/C:/Users/.../app-display-model.jar!/examples
                String path = examples_resource.getPath();
                // Split into path to jar file and folder within the jar
                final int sep = path.indexOf("!");
                final String jar_folder = path.substring(sep+2);
                path = path.substring(0, sep);
                logger.log(Level.INFO, "Jar file: " + path);
                logger.log(Level.INFO, "Folder in jar: " + jar_folder);

                // Go via URI and file to handle "file:/C:.." on windows
                final Path jar = new File(URI.create(path)).toPath();
                logger.log(Level.INFO, "Install " + jar + " into " + examples);
                // Open file system for jar
                try
                (
                    final FileSystem fs = FileSystems.newFileSystem(jar, (ClassLoader) null);
                )
                {
                    copy(1, fs.getPath(jar_folder), examples, monitor);
                }
            }
            else
            {   // Copy from local filesystem
                final Path resource_path = Paths.get(examples_resource.toURI());
                logger.log(Level.INFO, "Install " + resource_path + " into " + examples);
                copy(resource_path.getNameCount(), resource_path, examples, monitor);
            }
            // Open runtime on UI thread
            Platform.runLater(() ->
            {
                final URI uri = new File(examples, example_to_open).toURI();
                ApplicationService.createInstance(example_application, uri);

                // Tell user what was done
                final Alert info = new Alert(AlertType.INFORMATION);
                DialogHelper.positionDialog(info, pane, -300, -200);
                info.setTitle(Messages.InstallExamples);
                info.setHeaderText(MessageFormat.format(completion_message_format, examples.toString()));
                info.showAndWait();
            });
        });

        return true;
    }


    /** Copy all files, recursively
     *  @param strip Leading path segments to strip from source when creating destination path
     *  @param source Source directory in local file system or inside JAR file
     *  @param destination Destination directory
     *  @param monitor
     *  @throws Exception
     */
    private static void copy(final int strip, final Path source, final File destination, final JobMonitor monitor) throws Exception
    {
        try
        {
            monitor.updateTaskName("Install examples in " + destination);
            Files.walk(source, 1, FileVisitOption.FOLLOW_LINKS)
                 .forEach(item ->
                 {
                     try
                     {
                         if (Files.isDirectory(item))
                         {  // Prevent endless recursion on 'source' itself
                            if (! item.equals(source))
                                copy(strip, item, destination, monitor);
                         }
                         else
                         {
                             final File dest_file = new File(destination, item.subpath(strip, item.getNameCount()).toString());
                             dest_file.getParentFile().mkdirs();
                             Files.copy(item, dest_file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        }
                     }
                     catch (Throwable ex)
                     {
                         throw new Error("Cannot copy " + item + " to " + destination);
                     }
                 });
        }
        catch (Throwable ex)
        {
            throw new Exception("Cannot install examples", ex);
        }
    }

}
