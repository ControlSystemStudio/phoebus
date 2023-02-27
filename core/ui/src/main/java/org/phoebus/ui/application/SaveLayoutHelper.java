/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.application;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import javafx.scene.control.*;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.internal.MementoHelper;

import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

/** Helper to save layout
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class SaveLayoutHelper
{
    private SaveLayoutHelper()
    {
        ;
    }

    /** Validate the filename. Only [A-Z][a-z]_[0-9]. are allowed. */
    private static boolean validateFilename(final String filename)
    {
        return filename.matches("[\\w -]+");
    }

    /** Save the layout. Prompt for a new filename, validate, possibly confirm an overwrite, and then save.
     *  @return <code>true</code> if layout save has been initiated (may take some time to complete)
     */
    public static boolean saveLayout(List<Stage> stagesToSave, String titleText)
    {
        final TextInputDialog prompt = new TextInputDialog();
        prompt.setTitle(titleText);
        prompt.setHeaderText(Messages.SaveDlgHdr);
        positionDialog(prompt, stagesToSave.get(0));

        while (true)
        {
            final String filename = prompt.showAndWait().orElse(null);

            // Canceled?
            if (filename == null)
                return false;
            // OK to save?
            if (! validateFilename(filename))
            {
                // Ask again
                prompt.setHeaderText(Messages.SaveDlgErrHdr);
                continue;
            }
            else
                prompt.setHeaderText(Messages.SaveDlgHdr);

            // Done if save succeeded.
            if (saveState(stagesToSave, filename))
                return true;
        }
    }

    private static void positionDialog(final Dialog<?> dialog, Stage stage)
    {
        DialogHelper.positionDialog(dialog, stage.getScene().getRoot(), -100, -100);
        dialog.setResizable(true);
        dialog.getDialogPane().setMinSize(280, 160);
    }

    /** Save the state of the phoebus application with the given filename.
     *
     *  <p> If the file already exists, alert the user and prompt for file overwrite confirmation.
     *
     *  @param layout Memento name
     *  @return <code>true</code> if saved, <code>false</code> when not overwriting existing file
     */
    private static boolean saveState(List<Stage> stagesToSave, final String layout)
    {
        final String memento_filename = layout + ".memento";
        final File memento_file = new File(Locations.user(), memento_filename);
        // File.exists() is blocking in nature.
        // To combat this the phoebus application maintains a list of *.memento files that are in the default directory.
        // Check if the file name is in the list, and confirm a file overwrite with the user.
        if (PhoebusApplication.INSTANCE.memento_files.contains(layout))
        {
            final Alert fileExistsAlert = new Alert(AlertType.CONFIRMATION);
            fileExistsAlert.setHeaderText(MessageFormat.format(Messages.FileExists, layout));
            positionDialog(fileExistsAlert, stagesToSave.get(0));
            if (fileExistsAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                return false;
        }

        // Save in background thread
        JobManager.schedule("Save " + memento_filename, monitor ->
        {
            MementoHelper.saveState(stagesToSave, memento_file, null, null, PhoebusApplication.INSTANCE.isMenuVisible(), PhoebusApplication.INSTANCE.isToolbarVisible(), PhoebusApplication.INSTANCE.isStatusbarVisible());

            // After the layout has been saved,
            // update menu to include the newly saved layout
            PhoebusApplication.INSTANCE.createLoadLayoutsMenu();
        });
        return true;
    }
}
