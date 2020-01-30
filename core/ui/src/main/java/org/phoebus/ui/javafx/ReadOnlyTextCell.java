/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;

/** Table cell that allows copying the value, but can't edit
 *  @author Kay Kasemir
 *  @param <T> Cell data type
 */
public class ReadOnlyTextCell<T> extends TableCell<T, String>
{
    /** Text field used instead of the default Label to allow selecting & copying the text */
    protected final TextField text = new TextField();

    public ReadOnlyTextCell()
    {
        text.setEditable(false);
    }

    @Override
    protected void updateItem(final String item, final boolean empty)
    {
        super.updateItem(item, empty);
        if (empty)
            setGraphic(null);
        else
        {
            text.setText(item);
            setGraphic(text);
        }
    }
}