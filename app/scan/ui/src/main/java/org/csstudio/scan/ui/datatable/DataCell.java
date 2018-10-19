/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.scan.ui.datatable;

import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 * Cell representation for Scan Data Table
 * @author Evan Smith
 */
public class DataCell extends TableCell<DataRow, String>
{
    /** Cell value and timestamp seperator for the tooltip. */
    private static final String SEP = " / ";
    
    /** Column index for this cell. */
    private final int col_idx;
    
    /** This cell's tooltip. */
    private final Tooltip tooltip;
    
    public DataCell(final int col_idx)
    {
        this.col_idx = col_idx;
        
        tooltip = new Tooltip();
        tooltip.setShowDelay(Duration.millis(250));
        tooltip.setShowDuration(Duration.seconds(30));
        setTooltip(tooltip);
    }
    
    @Override
    public void updateItem(String item, boolean empty)
    {        
        DataRow row = getTableRow().getItem();
        if (empty)
        {
            setText(null);
            setTooltip(null);
            tooltip.setText(null);
        }
        else
        {
            setText(item);
            tooltip.setText(item + SEP + row.getDataTimestamp(col_idx).get());
            setTooltip(tooltip);
        }
    }
}
