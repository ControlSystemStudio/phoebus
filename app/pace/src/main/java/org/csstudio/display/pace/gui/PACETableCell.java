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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
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

        if (empty  ||  item == null)
            setBorder(null);
        else
        {
            final int col = getTableView().getColumns().indexOf(getTableColumn()) - 1;
            final Instance instance = getTableRow().getItem();
            final Cell cell = instance.getCell(col);
            setDisable(cell.isReadOnly());

            if (cell.isEdited())
                setBorder(EDITED);
            else
                setBorder(null);
        }
    }
}
