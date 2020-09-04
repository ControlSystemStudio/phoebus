/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.dialog;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.phoebus.ui.Preferences;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

/** Dialog that prompts for a file name to 'save'
 *  @author Kay Kasemir
 */
public class SaveAsDialog
{
    /** Prompt for file name
     *
     *  <p>May be called from any thread.
     *
     *  @param window Parent window for the modal dialog
     *  @param title Title
     *  @param file Suggested file, may be <code>null</code>
     *  @param filters Filters, may be <code>null</code>
     *  @return
     */
    public File promptForFile(final Window window, final String title, final File file, final ExtensionFilter[] filters)
    {
        if (Platform.isFxApplicationThread())
            return doPromptForFile(window, title, file, filters);

        final AtomicReference<File> the_file = new AtomicReference<>();
        final CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() ->
        {
            the_file.set(doPromptForFile(window, title, file, filters));
            done.countDown();
        });

        try
        {
            done.await();
            return the_file.get();
        }
        catch (InterruptedException ex)
        {
            return null;
        }
    }

    private File doPromptForFile(final Window window, final String title, File file, final ExtensionFilter[] filters)
    {
        File initial_directory;
        final FileChooser dialog = new FileChooser();
        dialog.setTitle(title);

        if (file != null)
        {
            // Dialog will fail if the directory does not exist
            if (null != file.getParentFile() && file.getParentFile().exists())
                dialog.setInitialDirectory(file.getParentFile());
            dialog.setInitialFileName(file.getName());
        }
        if (filters != null)
            dialog.getExtensionFilters().addAll(filters);

        if (!Preferences.default_save_path.isEmpty()){
            initial_directory = new File(Preferences.default_save_path);
            dialog.setInitialDirectory(initial_directory);
        }
        return doExecuteDialog(window, dialog);
    }

    protected File doExecuteDialog(final Window window, final FileChooser dialog)
    {
        return dialog.showSaveDialog(window);
    }
}
