/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.csstudio.trends.databrowser3.Activator;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.AxisConfig;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.MenuItem;

/** Menu entry to delete value axes from model
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DeleteAxes extends MenuItem
{
    public DeleteAxes(final Node node, final Model model, final UndoableActionManager undo,
                      final List<AxisConfig> selection)
    {
        super(Messages.DeleteAxis, Activator.getIcon("delete_obj"));
        setOnAction(event ->
        {
            // Copy to avoid modification errors in case this is model.getAxes()
            final List<AxisConfig> axes = new ArrayList<>(selection);
            Collections.reverse(axes);
            for (AxisConfig axis : axes)
            {
                final ModelItem item = model.getFirstItemOnAxis(axis);
                if (item != null)
                {
                    final Alert dialog = new Alert(AlertType.WARNING);
                    dialog.setTitle(Messages.DeleteAxis);
                    dialog.setHeaderText(MessageFormat.format(Messages.DeleteAxisWarningFmt, axis.getName(), item.getName()));
                    dialog.setResizable(true);
                    DialogHelper.positionDialog(dialog, node, -200, -200);
                    dialog.showAndWait();
                    return;
                }
            }
            // Delete axes one by one.
            // While it would be almost trivial to remove many axes 'at once',
            // restoring them in the same order means keeping a copy of
            // the original axes array.
            // Doing it one by one, each DeleteAxisCommand only needs to remember
            // one axis position
            for (AxisConfig axis : axes)
                new DeleteAxisCommand(undo, model, axis);
        });
    }
}
