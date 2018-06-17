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

import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.internal.MementoHelper;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

/** Menu item and helper to save layout
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class SaveLayoutMenuItem extends MenuItem
{
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

    /** Save the layout. Prompt for a new filename, validate, and then save. */
    private void saveLayout(final PhoebusApplication phoebus)
    {
        final TextInputDialog prompt = new TextInputDialog();
        prompt.setTitle(getText());
        prompt.setHeaderText("Enter a file name to save the layout as.");
        final List<Stage> stages = DockStage.getDockStages();
        DialogHelper.positionDialog(prompt, stages.get(0).getScene().getRoot(), -100, -100);
        String filename;
        while (true)
        {
            filename = prompt.showAndWait().orElse(null);
            // Canceled?
            if (filename == null)
                return;
            // OK to save?
            if (validateFilename(filename))
                break;
            // Ask again
            prompt.setHeaderText("Name must only contain alphanumeric characters, space, underscore or '-'.\nEnter a valid layout name.");
        }
        final File memento_file = new File(Locations.user(), filename + ".memento");
        MementoHelper.saveState(memento_file, null, null, phoebus.isToolbarVisible());
    }
}
