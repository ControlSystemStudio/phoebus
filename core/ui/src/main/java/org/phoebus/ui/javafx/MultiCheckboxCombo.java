/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.javafx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

/** A drop-down list similar to a Combo that allows selecting multiple entries.
 *
 *  <p>Each entry is represented by a checkbox.
 *  @author Kay Kasemir
 */
public class MultiCheckboxCombo<T> extends MenuButton
{
    @SuppressWarnings("unchecked")
    private final ObservableSet<T> selection = FXCollections.observableSet();

    /** @param text Label of the button */
    public MultiCheckboxCombo(final String text)
    {
        super(text);
    }

    /** @param options Options to offer in the drop-down */
    public void setOptions(final Collection<T> options)
    {
        selection.clear();
        getItems().clear();

        // Could use CheckMenuItem instead of CheckBox-in-CustomMenuItem,
        // but that adds/removes a check mark.
        // When _not_ selected, there's just an empty space, not
        // immediately obvious that item _can_ be selected.
        // Adding/removing one CheckMenuItem closes the drop-down,
        // while this approach allows it to stay open.
        for (T item : options)
        {
            final CheckBox checkbox = new CheckBox(item.toString());
            checkbox.setUserData(item);
            checkbox.setOnAction(event ->
            {
                if (checkbox.isSelected())
                    selection.add(item);
                else
                    selection.remove(item);
            });
            final CustomMenuItem menuitem = new CustomMenuItem(checkbox);
            menuitem.setHideOnClick(false);
            getItems().add(menuitem);
        }
    }

    /** @return {@link ObservableSet} of currently selected options */
    public ObservableSet<T> selectedOptions()
    {
        return selection;
    }

    /** @return List of currently selected options */
    public List<T> getSelectedOptions()
    {
        return new ArrayList<>(selection);
    }

    /** Programmatically select options
     *
     *  <p>Options must be one of those provided in previous
     *  call to <code>setOptions</code>.
     *  Will trigger listeners to the <code>selectedOptions</code>.
     *
     *  @param options Options to select
     */
    public void selectOptions(final Collection<T> options)
    {
        selection.clear();
        selection.addAll(options);

        for (MenuItem mi : getItems())
        {
            final CheckBox checkbox = (CheckBox) ((CustomMenuItem)mi).getContent();
            checkbox.setSelected(selection.contains(checkbox.getUserData()));
        }
    }
}
