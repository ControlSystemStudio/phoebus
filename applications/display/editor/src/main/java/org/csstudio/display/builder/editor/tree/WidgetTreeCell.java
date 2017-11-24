/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.tree;

import org.csstudio.display.builder.editor.DisplayEditor;
import org.csstudio.display.builder.editor.util.WidgetIcons;
import org.csstudio.display.builder.model.Widget;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Tree cell that displays {@link WidgetOrTab} (name, icon, ..)
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class WidgetTreeCell extends TreeCell<WidgetOrTab>
{
    private Image tab_icon = ImageCache.getImage(DisplayEditor.class, "/icons/tab_item.png");

    @Override
    public void updateItem(final WidgetOrTab item, final boolean empty)
    {
        super.updateItem(item, empty);
        if (empty || item == null)
        {
            setText(null);
            setGraphic(null);
        }
        else if (item.isWidget())
        {
            final Widget widget = item.getWidget();
            final String type = widget.getType();
            setText(widget.getName());
            final Image icon = WidgetIcons.getIcon(type);
            if (icon != null)
                setGraphic(new ImageView(icon));
            else
                setGraphic(null);
        }
        else
        {
            setText(item.getTab().name().getValue());
            if (tab_icon != null)
                setGraphic(new ImageView(tab_icon));
            else
                setGraphic(null);
        }
    }
}