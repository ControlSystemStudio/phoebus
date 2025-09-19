/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.chartbrowser.view.cells;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import org.phoebus.applications.chartbrowser.model.PVTableEntry;

/**
 * TableCell that represents and edits a Boolean property of a PVTableEntry
 * using a CheckBox control within the chart browser table.
 */
public class CheckBoxTableCell extends TableCell<PVTableEntry, Boolean> {
    private final CheckBox checkBox;

    /**
     * Constructs a new CheckBoxTableCell, sets up the CheckBox control,
     * and binds its action to the underlying PVTableEntry model.
     */
    public CheckBoxTableCell() {
        checkBox = new CheckBox();
        setGraphic(checkBox);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        checkBox.setOnAction(event -> {
            PVTableEntry entry = getTableRow().getItem();
            if (entry != null) {
                if (getTableColumn().getText().contains("Archive")) {
                    entry.setUseArchive(checkBox.isSelected());
                } else if (getTableColumn().getText().contains("Raw")) {
                    entry.useRawDataProperty().set(checkBox.isSelected());
                }
                event.consume();
            }
        });
    }

    public CheckBox getCheckBox() {
        return checkBox;
    }

    /**
     * Updates the cell's item and graphic. When not empty, selects or deselects
     * the CheckBox based on the Boolean value; otherwise clears the graphic.
     *
     * @param item  the Boolean value for this cell
     * @param empty true if this cell should be rendered as empty
     */
    @Override
    protected void updateItem(Boolean item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            checkBox.setSelected(item);
            setGraphic(checkBox);
        }
    }
}
