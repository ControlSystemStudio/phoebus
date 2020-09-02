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

import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.internal.MementoHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

/** Menu item and helper to save layout
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
class SaveLayoutMenuItem extends MenuItem
{
    private final PhoebusApplication phoebus;
    private final List<String> memento_files;

    /** Save layout menu item */
    public SaveLayoutMenuItem(final PhoebusApplication phoebus, final List<String> memento_files)
    {
        super(Messages.SaveLayoutAs, ImageCache.getImageView(ImageCache.class, "/icons/new_layout.png"));
        this.phoebus = phoebus;
        this.memento_files = memento_files;
        setOnAction(event ->  saveLayout());
    }

    /** Validate the filename. Only [A-Z][a-z]_[0-9]. are allowed. */
    private boolean validateFilename(final String filename)
    {
        return filename.matches("[\\w -]+");
    }

    /** Save the layout. Prompt for a new filename, validate, possibly confirm an overwrite, and then save.
     *  @return <code>true</code> if layout save has been initiated (may take some time to complete)
     */
    private boolean saveLayout()
    {
        final TextInputDialog prompt = new TextInputDialog();
        prompt.setTitle(getText());
        prompt.setHeaderText(Messages.SaveDlgHdr);
        positionDialog(prompt);

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
            if (saveState(filename))
                return true;
        }
    }

    private void positionDialog(final Dialog<?> dialog)
    {
        final List<Stage> stages = DockStage.getDockStages();
        DialogHelper.positionDialog(dialog, stages.get(0).getScene().getRoot(), -100, -100);
        dialog.setResizable(true);
        dialog.getDialogPane().setMinSize(280, 160);
    }

    /** Save the state of the phoebus application with the given filename.
     *
     *  <p> If the file already exists, alert the user and prompt for file overwrite confirmation.
     *
     *  @param phoebus Phoebus application
     *  @param layout Memento name
     *  @return <code>true</code> if saved, <code>false</code> when not overwriting existing file
     */
    private boolean saveState(final String layout)
    {
        final String memento_filename = layout + ".memento";
        final File memento_file = new File(Locations.user(), memento_filename);
        // File.exists() is blocking in nature.
        // To combat this the phoebus application maintains a list of *.memento files that are in the default directory.
        // Check if the file name is in the list, and confirm a file overwrite with the user.
        if (memento_files.contains(layout))
        {
            final Alert fileExistsAlert = new Alert(AlertType.CONFIRMATION);
            fileExistsAlert.setHeaderText(MessageFormat.format(Messages.FileExists, layout));
            positionDialog(fileExistsAlert);
            if (fileExistsAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK)
                return false;
        }

        // Save in background thread
        JobManager.schedule("Save " + memento_filename, monitor ->
        {
            MementoHelper.saveState(memento_file, null, null, phoebus.isMenuVisible(), phoebus.isToolbarVisible(), phoebus.isStatusbarVisible());

            // After the layout has been saved,
            // update menu to include the newly saved layout
            phoebus.createLoadLayoutsMenu();
        });
        return true;
    }
}
