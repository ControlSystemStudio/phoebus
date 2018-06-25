/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.application;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.internal.MementoHelper;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

/** Menu item and helper to save layout
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class SaveLayoutMenuItem extends MenuItem
{
    private final Alert fileExistsAlert = new Alert(AlertType.CONFIRMATION);    
    
    /** Save layout menu item */
    public SaveLayoutMenuItem(final PhoebusApplication phoebus)
    {
        super(Messages.SaveLayoutAs);
        setOnAction(event ->
        {
            saveLayout(phoebus);
            // Update menu to include the newly saved layout
            phoebus.createLoadLayoutsMenu();
        });
    }

    /** Validate the filename. Only [A-Z][a-z]_[0-9]. are allowed. */
    private boolean validateFilename(final String filename)
    {
        return filename.matches("[\\w -]+");
    }

    /** Save the layout. Prompt for a new filename, validate, possibly confirm an overwrite, and then save. */
    private void saveLayout(final PhoebusApplication phoebus)
    {
        final TextInputDialog prompt = new TextInputDialog();
        prompt.setTitle(getText());
        prompt.setHeaderText("Enter a file name to save the layout as.");
        final List<Stage> stages = DockStage.getDockStages(); 
        DialogHelper.positionDialog(prompt, stages.get(0).getScene().getRoot(), -100, -100);

        while (true)
        {
            final String filename = prompt.showAndWait().orElse(null);
            
            // Canceled?
            if (filename == null)
                return;
            // OK to save?
            if (! validateFilename(filename))
            {
                // Ask again
                prompt.setHeaderText("Name must only contain alphanumeric characters, space, underscore or '-'.\nEnter a valid layout name.");
                continue;
            }
            else
            {
                prompt.setHeaderText("Enter a file name to save the layout as.");   
            }
            
            // Done if save succeeded.
            if (saveState(phoebus, filename))
                break;
        }
    }

    /**
     * Save the state of the phoebus application with the given filename.
     * <p> If the file already exists, alert the user and prompt for file overwrite confirmation.
     * @param phoebus Phoebus application
     * @param filename Memento file
     * @return
     */
    private boolean saveState(PhoebusApplication phoebus, String filename)
    {
        final String memento_filename = filename + ".memento";
        final File memento_file = new File(Locations.user(), memento_filename);
        // File.exists() is blocking in nature.
        // To combat this the phoebus application maintains a list of *.memento files that are in the default directory.
        // Check if the file name is in the list, and confirm a file overwrite with the user.
        if (phoebus.getMementoFiles().contains(memento_filename))
        {
            fileExistsAlert.setHeaderText("File \"" + filename + "\" already exists. Do you want to overwite it?");
            Optional<ButtonType> result = fileExistsAlert.showAndWait();
            if (! result.isPresent() || result.get() != ButtonType.OK)
            {
                return false;
            }   
        }

        MementoHelper.saveState(memento_file, null, null, phoebus.isToolbarVisible());
        return true;
    }
}
