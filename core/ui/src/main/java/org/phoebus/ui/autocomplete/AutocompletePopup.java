/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.autocomplete;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import javafx.geometry.Bounds;
import javafx.scene.control.ListView;
import javafx.scene.control.PopupControl;
import javafx.scene.control.TextInputControl;
import javafx.stage.Window;

/** Popup for {@link AutocompleteItem}s
 *  @author Kay Kasemir
 */
class AutocompletePopup extends PopupControl
{
    private final AutocompletePopupSkin skin;
    private WeakReference<TextInputControl> active_field;

    public AutocompletePopup()
    {
        skin = new AutocompletePopupSkin(this);
        setSkin(skin);

        // Speed GC by releasing items, since menu stays in memory forever
        setOnHidden(event ->
        {
            clear();
            active_field = null;
        });
    }

    TextInputControl getActiveField()
    {
        if (active_field == null)
            return null;
        return active_field.get();
    }

    public void clear()
    {
        setItems(Collections.emptyList());
    }

    public void setItems(final List<AutocompleteItem> items)
    {
        skin.setItems(items);
    }

    public void show(final TextInputControl field)
    {
        active_field = new WeakReference<>(field);

        // Back when using a ContextMenu,
        //   menu.show(field, Side.BOTTOM, 0, 0);
        // held an `ownerNode` reference,
        // so not passing the field sped up GC.
        final Bounds bounds = field.localToScreen(field.getLayoutBounds());

        // Min. width of 620 is useful for long sim PVs
        ((ListView<?>) skin.getNode()).setPrefWidth(Math.max(620, bounds.getWidth()));

        final Window window = field.getScene().getWindow();
        show(window, bounds.getMinX(), bounds.getMaxY());
    }
}
