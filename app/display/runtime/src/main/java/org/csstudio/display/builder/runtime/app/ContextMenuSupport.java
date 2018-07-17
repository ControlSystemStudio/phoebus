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
import org.csstudio.display.builder.runtime.RuntimeAction;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.logbook.ui.menu.SendLogbookAction;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.collections.ObservableList;
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
@SuppressWarnings("nls")
class ContextMenuSupport
{
    private final DisplayRuntimeInstance instance;
    private final ContextMenu menu = new ContextMenu();

    /** Connect context menu to toolkit's `handleContextMenu` */
    ContextMenuSupport(final DisplayRuntimeInstance instance)
    {
        this.instance = instance;
        menu.setAutoHide(true);

        // Menu inherits styling of widget's node.
        // Some widgets update the background color
        // (TextEntryRepresentation).
        // No way to prevent inheritance, so reset to the modena.css default:
        menu.setStyle("-fx-control-inner-background: derive(-fx-base,80%);");

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
        final ObservableList<MenuItem> items = menu.getItems();
        items.setAll(new WidgetInfoAction(widget));

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
                    items.add(createMenuItem(widget,
                                   new OpenDisplayActionInfo(desc, open_info.getFile(),
                                                             open_info.getMacros(), target)));
                }
            }
            else
                items.add(createMenuItem(widget, info));
        }

        // Actions of the widget runtime
        final WidgetRuntime<Widget> runtime = RuntimeUtil.getRuntime(widget);
        if (runtime == null)
            throw new NullPointerException("Missing runtime for " + widget);
        for (RuntimeAction info : runtime.getRuntimeActions())
        {
            // Load image for action from that action's class loader
            final ImageView icon = ImageCache.getImageView(info.getClass(), info.getIconPath());
            final MenuItem item = new MenuItem(info.getDescription(), icon);
            item.setOnAction(event -> info.run());
            items.add(item);
        }

        items.add(new SeparatorMenuItem());

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
            items.add(new SeparatorMenuItem());
        }

        final Scene scene = node.getScene();
        final Parent model_parent = instance.getRepresentation().getModelParent();
        items.add(new SaveSnapshotAction(model_parent));
        items.add(new PrintAction(model_parent));
        // TODO SendEmail
        items.add(new SendLogbookAction(model_parent, null));

        items.add(new SeparatorMenuItem());

        items.add(new DisplayToolbarAction(instance));
        items.add(new FullScreenAction(scene));

        // If the editor is available, add "Open in Editor"
        final AppResourceDescriptor editor = ApplicationService.findApplication("display_editor");
        if (editor != null)
            items.add(new OpenInEditorAction(editor, widget));

        items.add(new SeparatorMenuItem());

        items.add(new ReloadDisplayAction(instance));
    }

    private static MenuItem createMenuItem(final Widget widget, final ActionInfo info)
    {
        final ImageView icon = new ImageView(new Image(info.getType().getIconURL().toExternalForm()));
        final MenuItem item = new MenuItem(info.getDescription(), icon);
        item.setOnAction(event -> ActionUtil.handleAction(widget, info));
        return item;
    }
}
