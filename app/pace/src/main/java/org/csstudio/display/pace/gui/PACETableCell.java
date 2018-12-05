/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pace.gui;

import org.csstudio.display.pace.model.Cell;
import org.csstudio.display.pace.model.Instance;

import javafx.geometry.Insets;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.util.converter.DefaultStringConverter;

/** Table cell
 *
 *  Indicates edited or disabled cell state
 *  @author Kay Kasemir
 */
public class PACETableCell extends TextFieldTableCell<Instance, String>
{
    private static final Border EDITED = new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.DASHED, CornerRadii.EMPTY, BorderStroke.THIN, new Insets(1.5)));

    public PACETableCell()
    {
        super(new DefaultStringConverter());
    }

    @Override
    public void updateItem(final String item, final boolean empty)
    {
        super.updateItem(item, empty);

        if (empty  ||  item == null  ||  getTableRow() == null)
        {
            setBorder(null);
            setTooltip(null);
        }
        else
        {
            final Instance instance = getTableRow().getItem();
            final int col = getTableView().getColumns().indexOf(getTableColumn());
            // Col 0 lists "System", no Cell
            if (col > 0)
            {
                final Cell cell = instance.getCell(col-1);
                setBorder(cell.isEdited() ? EDITED : null);

                Tooltip tooltip = getTooltip();
                // Don't update an active tool tip  because that would hide it
                // and thus never show any for cell with rapidly changing value
                if (tooltip == null  ||  !tooltip.isShowing())
                {
                    tooltip = new Tooltip(cell.getInfo());
                    tooltip.setHideDelay(Duration.seconds(10));
                    setTooltip(tooltip);
                }
            }
        }
    }
}
