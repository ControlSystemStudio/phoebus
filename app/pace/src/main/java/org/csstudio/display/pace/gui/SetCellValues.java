/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pace.gui;

import java.text.MessageFormat;
import java.util.List;

import org.csstudio.display.pace.Messages;
import org.csstudio.display.pace.model.Cell;
import org.phoebus.ui.dialog.DialogHelper;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;

/** Menu item to set values of selected cells
 *  @author Kay Kasemir
 */
public class SetCellValues extends MenuItem
{
    public SetCellValues(final Node node, final List<Cell> cells)
    {
        super(Messages.SetValue);
        boolean readonly = true;
        for (Cell cell : cells)
            if (! cell.isReadOnly())
            {
                readonly = false;
                break;
            }
        if (cells.isEmpty() || readonly)
            setDisable(true);
        else
            setOnAction(event -> Platform.runLater(() -> setCells(node, cells)));
    }

    private void setCells(final Node node, final List<Cell> cells)
    {
        // Check if there are some read-only cells
        boolean writable = true;
        for (Cell cell : cells)
            if (!cell.isPVWriteAllowed())
            {
                writable = false;
                break;
            }

        final String message = MessageFormat.format(
            writable
            ? Messages.SetValue_Msg
            : Messages.SetValue_Msg_WithReadonlyWarning,
            cells.get(0).getColumn().getName());

        // Using value of first selected cell as suggestion,
        // prompt for value to be put into all selected cells
        final TextInputDialog dialog = new TextInputDialog(cells.get(0).getObservable().getValue());
        dialog.setTitle(Messages.SetValue_Title);
        dialog.setHeaderText(message);
        DialogHelper.positionDialog(dialog, node, -200, -100);
        final String user_value = dialog.showAndWait().orElse(null);

        // Update cells
        if (user_value != null)
            for (Cell cell : cells)
                cell.setUserValue(user_value);
    }
}
