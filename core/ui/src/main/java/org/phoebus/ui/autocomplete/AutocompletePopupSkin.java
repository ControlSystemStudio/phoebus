/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.autocomplete;

import static org.phoebus.ui.autocomplete.AutocompleteMenu.logger;

import java.util.List;
import java.util.logging.Level;

import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

/** Skin for for {@link AutocompletePopup}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class AutocompletePopupSkin implements Skin<AutocompletePopup>
{
    private final AutocompletePopup popup;
    private final ListView<AutocompleteItem> list = new ListView<>();

    private class AutocompleteCell extends ListCell<AutocompleteItem>
    {
        @Override
        protected void updateItem(final AutocompleteItem item, boolean empty)
        {
            super.updateItem(item, empty);
            if (item == null  || empty)
                setGraphic(null);
            else
                setGraphic(item.representation);
        }
    }

    public AutocompletePopupSkin(final AutocompletePopup popup)
    {
        this.popup = popup;
        list.setCellFactory(view -> new AutocompleteCell());

        list.setOnMouseClicked(this::handleClick);
        list.setOnKeyPressed(this::handleKey);
    }

    public void setItems(final List<AutocompleteItem> items)
    {
        list.getItems().setAll(items);

        // Size list to show items
        list.setPrefHeight(items.size() * 24 + 5);
    }

    private void handleClick(final MouseEvent event)
    {
        if (event.getButton() == MouseButton.PRIMARY)
        {
            logger.log(Level.FINE, () -> "Clicked in list");
            runSelectedAction();
        }
    }

    private void handleKey(final KeyEvent event)
    {
        final TextInputControl field = popup.getActiveField();
        switch (event.getCode())
        {
        case ESCAPE:
            logger.log(Level.FINE, () -> "Pressed escape in list");
            popup.hide();
            break;
        case ENTER:
            logger.log(Level.FINE, () -> "Pressed enter in list");
            runSelectedAction();
            break;
        case LEFT:
            // Forward left/right from list which ignores them anyway
            // to text field where user might want to move the cursor.
            // Unclear why the caretposition is already updated
            // in the 'shift' case, but still necessary to trigger selection.
            if (field != null)
            {
                if (event.isShiftDown())
                    field.selectPositionCaret(field.getCaretPosition());
                else
                    field.backward();
            }
            break;
        case RIGHT:
            if (field != null)
            {
                if (event.isShiftDown())
                    field.selectPositionCaret(field.getCaretPosition());
                else
                    field.forward();
            }
            break;
        default:
        }
    }

    private void runSelectedAction()
    {
        final AutocompleteItem selected = list.getSelectionModel().getSelectedItem();
        if (selected != null  &&  selected.action != null)
            selected.action.run();
    }

    @Override
    public AutocompletePopup getSkinnable()
    {
        return popup;
    }

    @Override
    public Node getNode()
    {
        return list;
    }

    @Override
    public void dispose()
    {
        list.getItems().clear();
    }
}
