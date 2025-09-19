/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.chartbrowser.view.cells;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import org.phoebus.applications.chartbrowser.model.PVTableEntry;

/**
 * TableCell that provides an editable Spinner for configuring the buffer size
 * property of a PVTableEntry within the Chart Browser table view.
 */
public class BufferSizeTableCell extends TableCell<PVTableEntry, Integer> {
    private final Spinner<Integer> spinner;

    /**
     * Constructs a new BufferSizeTableCell and initializes its Spinner with
     * a value factory and default settings.
     */
    public BufferSizeTableCell() {
        spinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 1_000_000, 5_000, 100));
        spinner.setEditable(true);
        spinner.setPrefWidth(80);
        spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
            PVTableEntry entry = getTableRow().getItem();
            if (entry != null) {
                entry.setBufferSize(newValue);
            }
        });
    }

    /**
     * Updates the cell's item and graphic. When not empty, sets the spinner's value
     * to the current buffer size and displays it; otherwise clears text and graphic.
     *
     * @param item  the current buffer size value for this cell
     * @param empty true if this cell should be rendered as empty
     */
    @Override
    protected void updateItem(Integer item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            spinner.getValueFactory().setValue(item);
            setGraphic(spinner);
        }
    }
}
