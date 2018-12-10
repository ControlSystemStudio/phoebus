/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pace.gui;

import java.util.List;

import org.csstudio.display.pace.Messages;
import org.csstudio.display.pace.model.Cell;

import javafx.application.Platform;
import javafx.scene.control.MenuItem;

/** Menu item to restore values of selected cells
 *  @author Kay Kasemir
 */
public class RestoreCellValues extends MenuItem
{
    public RestoreCellValues(final List<Cell> cells)
    {
        super(Messages.RestoreCell);
        boolean any = false;
        for (Cell cell : cells)
            if (cell.isEdited())
            {
                any = true;
                break;
            }
        if (!any)
            setDisable(true);
        else
            setOnAction(event -> Platform.runLater(() ->
            {
                for (Cell cell : cells)
                    if (cell.isEdited())
                        cell.clearUserValue();
            }));
    }
}
