/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.chartbrowser.view.cells;

import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import org.phoebus.applications.chartbrowser.model.PVTableEntry;

/**
 * TableCell that provides an editable TextField for configuring a String
 * property of a PVTableEntry within the Chart Browser table view.
 */
public class StringInputTableCell extends TableCell<PVTableEntry, String> {
    private final TextField textField;

    /**
     * Constructs a new StringInputTableCell and initializes its TextField.
     */
    public StringInputTableCell(String placeholder) {
        this.textField = new TextField();
        textField.setEditable(true);
        textField.setPrefWidth(100);

        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            PVTableEntry entry = getTableRow().getItem();
            if (entry != null) {
                entry.meanValue(newValue);
            }
        });

        textField.setOnAction(evt -> {
            commitEdit(textField.getText());
            evt.consume();
        });

        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                commitEdit(textField.getText());
            }
        });

        textField.setPromptText(placeholder);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            setText(null);
        } else {
            String current = textField.getText();
            if (!current.equals(item)) {
                int caret = textField.getCaretPosition();
                textField.setText(item);
                textField.positionCaret(Math.min(caret, item.length()));
            }
            setGraphic(textField);
            setText(null);
        }
    }

    @Override
    public void startEdit() {
        super.startEdit();

        if (isEmpty()) {
            return;
        }

        textField.setText(getItem());
        textField.selectAll();
        setGraphic(textField);
        textField.requestFocus();
    }
}
