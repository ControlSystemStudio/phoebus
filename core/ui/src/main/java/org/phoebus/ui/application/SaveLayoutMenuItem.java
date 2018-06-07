/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.application;

import java.io.File;

import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.internal.MementoHelper;

import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;

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
            saveLayout();
            phoebus.createLoadLayoutsMenu();
        });
    }

    /** Validate the filename. Only [A-Z][a-z]_[0-9]. are allowed. */
    private boolean validateFilename(final String filename)
    {
        if (filename.matches("[\\w -]+"))
            return true;
        return false;
    }

    /** Save the layout. Prompt for a new filename, validate, and then save. */
    public void saveLayout()
    {
        final TextInputDialog prompt = new TextInputDialog();
        prompt.setTitle(getText());
        prompt.setHeaderText("Enter a file name to save the layout as.");
        String filename;
        while (true)
        {
            filename = prompt.showAndWait().orElse(null);

            if (filename == null)
                return;

            if (validateFilename(filename))
                break;

            prompt.setHeaderText("Name must only contain alphanumeric characters, space, underscore or '-'.\nEnter a valid layout name.");
        }
        final File memento_file = new File(Locations.user(), filename + ".memento");
        MementoHelper.saveState(memento_file, null, null);
    }
}
