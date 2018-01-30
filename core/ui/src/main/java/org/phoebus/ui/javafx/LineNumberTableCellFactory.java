/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.phoebus.ui.javafx;


import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;


/**
 * A generic cell factory you can simply use to display the row number in a
 * table.
 *
 * @param T The type of the TableView generic type (i.e. T == TableView<T>)
 * @param E The type of the content in all cells in this TableColumn.
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 24 Jan 2018
 */
public class LineNumberTableCellFactory<T, E> implements Callback<TableColumn<T, E>, TableCell<T, E>> {

    private final boolean startFromZero;

    /**
     * @param startFromZero {@code true} if row numbers must be shown starting
     *            from 0, {@code false} starting from 1.
     */
    public LineNumberTableCellFactory ( boolean startFromZero ) {
        this.startFromZero = startFromZero;
    }

    @Override
    public TableCell<T, E> call ( TableColumn<T, E> param ) {
        return new TableCell<T, E>() {

            /* Instance initializer. */
            {
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem ( E item, boolean empty ) {

                super.updateItem(item, empty);

                if ( !empty ) {
                    setText(String.valueOf(getTableRow().getIndex() + ( startFromZero ? 0 : 1 )));
                } else {
                    setText("");
                }

            }

        };
    }

}
