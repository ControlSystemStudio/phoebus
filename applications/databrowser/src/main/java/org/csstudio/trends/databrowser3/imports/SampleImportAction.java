/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.imports;

import java.io.File;
import java.text.MessageFormat;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.ArchiveDataSource;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.csstudio.trends.databrowser3.ui.AddModelItemCommand;
import org.csstudio.trends.databrowser3.ui.properties.AddAxisCommand;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.dialog.OpenFileDialog;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.control.MenuItem;

/** Action that performs a sample import
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SampleImportAction extends MenuItem
{
    final private UndoableActionManager op_manager;
    final private Model model;
    final private String type;

    public SampleImportAction(final Model model, final String type, final UndoableActionManager op_manager)
    {
        super(MessageFormat.format(Messages.ImportActionLabelFmt, type),
              Activator.getIcon("import"));
        this.op_manager = op_manager;
        this.model = model;
        this.type = type;
        setOnAction(event -> run());
    }

    private void run()
    {
        // Prompt for file
        final File the_file = new OpenFileDialog().promptForFile(getParentPopup().getOwnerWindow(), Messages.ImportTitle, null, null);
        if (the_file == null)
            return;
        try
        {
            // Add to first empty axis, or create new axis
            final AxisConfig axis = model.getEmptyAxis().orElseGet(() -> new AddAxisCommand(op_manager, model).getAxis());

            // Data source for "import:..." will load the file
            final String url = ImportArchiveReaderFactory.createURL(type, the_file.toString());
            final ArchiveDataSource imported = new ArchiveDataSource(url, type);
            // Add PV Item with data to model
            AddModelItemCommand.forPV(op_manager, model,
                                      type, Preferences.scan_period, axis, imported);
        }
        catch (Exception ex)
        {
            ExceptionDetailsErrorDialog.openError(Messages.Error, "Cannot import " + the_file, ex);
        }
    }
}
