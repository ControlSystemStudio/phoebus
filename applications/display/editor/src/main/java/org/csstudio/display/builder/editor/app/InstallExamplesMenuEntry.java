/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.app;

import static org.csstudio.display.builder.editor.Plugin.logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.logging.Level;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.model.DisplayModel;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.jobs.JobMonitor;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.docking.DockPane;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

/** Menu entry for installing example displays
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class InstallExamplesMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return Messages.InstallExamples;
    }

    @Override
    public String getMenuPath()
    {
        return "Examples";
    }

    @Override
    public Void call() throws Exception
    {
        // Prompt for installation directory
        final Window window = DockPane.getActiveDockPane().getScene().getWindow();
        final DirectoryChooser select = new DirectoryChooser();
        select.setTitle(Messages.InstallExamplesTitle);
        final File dir = select.showDialog(window);
        if (dir == null)
            return null;

        final File examples = new File(dir, "Display Builder");
        if (examples.exists())
        {   // Prompt user: OK to overwrite?
            final Alert confirm = new Alert(AlertType.CONFIRMATION);
            confirm.setTitle(Messages.InstallExamples);
            confirm.setHeaderText(MessageFormat.format(Messages.ReplaceExamplesWarningFMT, examples.toString()));
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                return null;
        }

        // In background:
        JobManager.schedule(Messages.InstallExamples, monitor ->
        {
            if (examples.exists())
            {   // Delete existing directory
                monitor.beginTask("Delete " + examples);
                Files.walkFileTree(examples.toPath(), new SimpleFileVisitor<>()
                {
                    @Override
                    public FileVisitResult visitFile(final Path file,
                                                     final BasicFileAttributes attr) throws IOException
                    {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(final Path dir,
                                                              final IOException ex) throws IOException
                    {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }

            // Install examples, which may be in local file system or JAR
            monitor.beginTask("Install " + examples);
            final URL resource = DisplayModel.class.getResource("/examples");
            if (resource.getProtocol().equals("jar"))
            {   // Open JAR as filesystem
                final Path jar = Paths.get(resource.getPath()
                                                   .replace("file:", "")
                                                   .replaceFirst("\\!/.*", ""));
                logger.log(Level.INFO, "Install " + jar + " into " + examples);
                try
                (
                    final FileSystem fs = FileSystems.newFileSystem(jar, null);
                )
                {
                    copy(1, fs.getPath("examples"), examples, monitor);
                }
            }
            else
            {   // Copy from local filesystem
                final Path resource_path = Paths.get(resource.toURI());
                logger.log(Level.INFO, "Install " + resource_path + " into " + examples);
                copy(resource_path.getNameCount(), resource_path, examples, monitor);
            }
            // Open editor on UI thread
            Platform.runLater(() ->
            {
                final AppResourceDescriptor editor = ApplicationService.findApplication(DisplayEditorApplication.NAME);
                editor.create(new File(examples, "01_main.bob").toURI());
            });
        });

        return null;
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
