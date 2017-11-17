/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import java.util.Optional;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.representation.javafx.widgets.JFXBaseRepresentation;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;

/** Context menu for a widget or the display
 *  @author Kay Kasemir
 */
class ContextMenuSupport
{
    private final DisplayRuntimeInstance instance;
    private final ContextMenu menu = new ContextMenu();

    /** Connect context menu to toolkit's `handleContextMenu` */
    ContextMenuSupport(final DisplayRuntimeInstance instance)
    {
        this.instance = instance;
        menu.setAutoHide(true);

        final ToolkitListener tkl = new ToolkitListener()
        {
            @Override
            public void handleContextMenu(final Widget widget, final int screen_x, final int screen_y)
            {
                final Node node = JFXBaseRepresentation.getJFXNode(widget);
                fillMenu(widget);
                menu.show(node, screen_x, screen_y);
            }
        };

        instance.getRepresentation().addListener(tkl);
    }

    /** Fill context menu with items for widget
     *  @param widget
     */
    private void fillMenu(final Widget widget)
    {
        // TODO Auto-generated method stub
        menu.getItems().setAll(new WidgetInfoAction(widget));

        final Optional<WidgetProperty<String>> name_prop = widget.checkProperty(CommonWidgetProperties.propPVName);
        if (name_prop.isPresent())
        {
            final String pv_name = name_prop.get().getValue();
            if (!pv_name.isEmpty())
                System.out.println("TODO: Set selection to PV " + pv_name);
        }
        // Set selection to PV of the widget
        // SelectionService.getInstance().setSelection(source, selection);

        // TODO Many more entrys, see RCP's ContextMenuSupport

        menu.getItems().add(new ReloadDisplayAction(instance));
    }
}
