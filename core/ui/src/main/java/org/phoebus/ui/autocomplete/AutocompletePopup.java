/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.autocomplete;

import static org.phoebus.ui.autocomplete.AutocompleteMenu.logger;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.phoebus.ui.javafx.ScreenUtil;

import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ListView;
import javafx.scene.control.PopupControl;
import javafx.scene.control.TextInputControl;
import javafx.stage.Window;

/** Popup for {@link AutocompleteItem}s
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class AutocompletePopup extends PopupControl
{
    private final AutocompletePopupSkin skin;
    private WeakReference<TextInputControl> active_field;

    public AutocompletePopup()
    {
        skin = new AutocompletePopupSkin(this);
        setSkin(skin);
        // AutoFix will keep the popup inside bounds of screen,
        // but unfortunately often covers the field
        // that triggers the popup.
        setAutoFix(false);

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
        if (bounds == null)
        {
            logger.log(Level.WARNING, "Cannot show popup", new Exception("No window"));
            return;
        }

        // Min. width of 620 is useful for long sim PVs
        final double width = Math.max(620, bounds.getWidth());
        ((ListView<?>) skin.getNode()).setPrefWidth(width);

        // Position popup under the field
        double x = bounds.getMinX();
        double y = bounds.getMaxY();

        // But check if that would move it off-screen
        Rectangle2D screen_bounds = ScreenUtil.getScreenBounds(x, y);
        if (screen_bounds != null)
        {
            // Move left to avoid dropping off right screen edge
            if (x + width > screen_bounds.getMaxX())
                x = screen_bounds.getMaxX() - width;
            // Currently not adjusting Y coordinate.
            // Doing that would require listening to height changes
            // as the list is populated.
            // So if list is too long, it will drop below the bottom
            // of the screen.
            // User can still see the text field, and as more is entered,
            // the list tends to show better matches i.e. fewer entries.
        }

        final Window window = field.getScene().getWindow();
        show(window, x, y);
    }

    @Override
    public void hide()
    {
        active_field = null;
        super.hide();
    }
}
