/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import java.util.List;
import java.util.Optional;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfo.ActionType;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo.Target;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.representation.javafx.widgets.JFXBaseRepresentation;
import org.csstudio.display.builder.runtime.ActionUtil;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

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
                fillMenu(node, widget);
                menu.show(node, screen_x, screen_y);
            }
        };

        instance.getRepresentation().addListener(tkl);
    }

    /** Fill context menu with items for widget
     *  @param node
     *  @param widget
     */
    private void fillMenu(final Node node, final Widget widget)
    {
        menu.getItems().setAll(new WidgetInfoAction(widget));

        // Widget actions
        for (ActionInfo info : widget.propActions().getValue().getActions())
        {
            if (info.getType() == ActionType.OPEN_DISPLAY)
            {
                // Add variant for all the available Target types: Replace, new Tab, ...
                final OpenDisplayActionInfo open_info = (OpenDisplayActionInfo) info;
                for (Target target : Target.values())
                {
                    if (target == Target.STANDALONE)
                        continue;
                    final String desc = target == Target.REPLACE
                                      ? open_info.getDescription()
                                      : open_info.getDescription() + " (" + target + ")";
                          menu.getItems().add(createMenuItem(widget,
                                   new OpenDisplayActionInfo(desc, open_info.getFile(),
                                                             open_info.getMacros(), target)));
                }
            }
            else
                menu.getItems().add(createMenuItem(widget, info));
        }

        menu.getItems().add(new SeparatorMenuItem());

        // Add PV-based contributions
        final Optional<WidgetProperty<String>> name_prop = widget.checkProperty(CommonWidgetProperties.propPVName);
        if (name_prop.isPresent())
        {
            final String pv_name = name_prop.get().getValue();
            if (!pv_name.isEmpty())
            {
                // Set the 'selection' to the PV of this widget
                SelectionService.getInstance().setSelection(DisplayRuntimeApplication.NAME, List.of(new ProcessVariable(pv_name)));
                // Add PV-based menu entries
                ContextMenuHelper.addSupportedEntries(node, menu);
            }
        }

        menu.getItems().add(new SeparatorMenuItem());

        final Scene scene = node.getScene();
        final Parent model_parent = instance.getRepresentation().getModelParent();
        
        menu.getItems().add(new SaveSnapshotAction(model_parent));
        menu.getItems().add(new PrintAction(model_parent));
        // TODO SendEmail
        // TODO SendToLogbook
        menu.getItems().add(new FullScreenAction(scene));
        // TODO Allow editor to add "Open in Editor"
        menu.getItems().add(new ReloadDisplayAction(instance));
    }

    private static MenuItem createMenuItem(final Widget widget, final ActionInfo info)
    {
        final ImageView icon = new ImageView(new Image(info.getType().getIconURL().toExternalForm()));
        final MenuItem item = new MenuItem(info.getDescription(), icon);
        item.setOnAction(event -> ActionUtil.handleAction(widget, info));
        return item;
    }
}
