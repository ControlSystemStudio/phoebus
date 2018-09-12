/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import java.lang.ref.WeakReference;

import org.csstudio.display.builder.model.Widget;

import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;

/** Platform holds on to menu items long after the menu has been shown.
 *  Tried to 'clear' menu items, but ContextMenu.focused will still
 *  reference the item that was last invoked..
 *
 *  Fix: Have menu items only hold a weak reference to the widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WeakRefWidgetAction extends MenuItem
{
    protected  final WeakReference<Widget> weak_widget;

    public WeakRefWidgetAction(final String label, final ImageView icon, final Widget the_widget)
    {
        super(label, icon);
        weak_widget = new WeakReference<>(the_widget);
    }

    public Widget getWidget()
    {
        final Widget widget = weak_widget.get();
        if (widget == null)
            throw new Error("Cannot invoke menu for disposed display");
        return widget;
    }
}
