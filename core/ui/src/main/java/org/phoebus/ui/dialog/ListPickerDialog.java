/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.dialog;

import java.util.Collection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;

/** Dialog that allows user to pick an item from a list
 *
 *  <p>Similar to javafx.scene.control.ChoiceDialog,
 *  but that one uses a ComboBox, so user has to
 *  open the drop-down for the combo, then select an item.
 *
 *  <p>This dialog presents a list, so typically one
 *  double-click on a list item is all that's needed,
 *  compared to click to open combo, click on item, click on OK.
 *
 *  <p>Can call {@link Dialog#setTitle(String)} and
 *  {@link Dialog#setHeaderText(String)} to customize.
 *
 *  @author Kay Kasemir
 */
public class ListPickerDialog extends Dialog<String>
{
    final ListView<String> list;

    /** Create list picker dialog
     *  @param parent Parent next to which the dialog will be positioned
     *  @param choices Options to show
     *  @param initial Initial selection (null will select first item)
     */
    public ListPickerDialog(final Node parent, final Collection<String> choices, final String initial)
    {
        this(choices, initial);
        initOwner(parent.getScene().getWindow());
        final Bounds bounds = parent.localToScreen(parent.getBoundsInLocal());
        setX(bounds.getMinX());
        setY(bounds.getMinY());
    }

    /** Create list picker dialog
     *  @param choices Options to show
     *  @param initial Initial selection (null will select first item)
     */
    public ListPickerDialog(final Collection<String> choices, final String initial)
    {
        final ObservableList<String> items = FXCollections.observableArrayList(choices);
        list = new ListView<>(items);

        int sel = initial != null ? items.indexOf(initial) : -1;
        list.selectionModelProperty().get().clearAndSelect(sel > 0 ? sel : 0);

        // Size list to number of items
        // .. based on guess of about 30 pixels per item ..
        list.setPrefHeight(items.size() * 30.0);

        list.setOnMouseClicked(event ->
        {
            if (event.getClickCount() >= 2)
            {
                setResult(list.getSelectionModel().getSelectedItem());
                close();
            }
        });

        getDialogPane().setContent(new BorderPane(list));
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setResizable(true);

        setResultConverter(button ->
        {
            return button == ButtonType.OK ? list.getSelectionModel().getSelectedItem() : null;
        });
    }
}
