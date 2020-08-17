/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.macros.MacroHandler;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfo.ActionType;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo;
import org.csstudio.display.builder.model.properties.OpenDisplayActionInfo.Target;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.representation.javafx.widgets.JFXBaseRepresentation;
import org.csstudio.display.builder.runtime.ActionUtil;
import org.csstudio.display.builder.runtime.Messages;
import org.csstudio.display.builder.runtime.RuntimeAction;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.phoebus.applications.email.actions.SendEmailAction;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.email.EmailPreferences;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.logbook.ui.LogbookUiPreferences;
import org.phoebus.logbook.ui.menu.SendLogbookAction;
import org.phoebus.security.authorization.AuthorizationService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.application.SaveSnapshotAction;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.PrintAction;
import org.phoebus.ui.javafx.Screenshot;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
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

        final ToolkitListener tkl = new ToolkitListener()
        {
            @Override
            public void handleContextMenu(final Widget widget, final int screen_x, final int screen_y)
            {
                final Node node = JFXBaseRepresentation.getJFXNode(widget);
                fillMenu(node, widget);
                // Use window, not node, to show menu for two reasons:
                // 1) menu.show(node, ..) means menu is attached to node,
                //    inheriting styles of nodes. For widgets that change background color
                //    via style, those colors would then apply to the menu items as well.
                // 2) Clicking outside the menu will close the menu, while it would remain
                //    open when attached to the node.
                menu.show(node.getScene().getWindow(), screen_x, screen_y);
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
                    // Expand macros in action description
                    String desc;
                    try
                    {
                        desc = MacroHandler.replace(widget.getEffectiveMacros(), open_info.getDescription());
                    }
                    catch (Exception ex)
                    {
                        logger.log(Level.WARNING, "Cannot expand macros in action description '" + open_info.getDescription() + "'", ex);
                        desc = open_info.getDescription();
                    }
                    // Mention non-default targets
                    if (target != Target.REPLACE)
                        desc += " (" + target + ")";
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
        String pv_name = "";
        // Does widget have a PV name?
        final Optional<WidgetProperty<String>> name_prop = widget.checkProperty(CommonWidgetProperties.propPVName);
        if (name_prop.isPresent())
            pv_name = name_prop.get().getValue();
        else
        {   // See if there are runtime PVs, pick the first one
            for (RuntimePV pv : runtime.getPVs())
            {
                pv_name = pv.getName();
                break;
            }
        }
        if (!pv_name.isEmpty())
        {
            // Set the 'selection' to the PV of this widget
            SelectionService.getInstance().setSelection(DisplayRuntimeApplication.NAME, List.of(new ProcessVariable(pv_name)));
            // Add PV-based menu entries
            ContextMenuHelper.addSupportedEntries(node, menu);
            items.add(new SeparatorMenuItem());
        }

        // If toolbar is hidden, offer forward/backward navigation
        if (!instance.isToolbarVisible())
        {
            boolean navigate = false;
            final DisplayNavigation navigation = instance.getNavigation();
            if (navigation.getBackwardDisplays().size() > 0)
            {
                final MenuItem item = new MenuItem(Messages.NavigateBack_TT, new ImageView(NavigationAction.backward));
                item.setOnAction(event -> instance.loadDisplayFile(navigation.goBackward(1)));
                items.add(item);
                navigate = true;
            }
            if (navigation.getForwardDisplays().size() > 0)
            {
                final MenuItem item = new MenuItem(Messages.NavigateForward_TT, new ImageView(NavigationAction.forward));
                item.setOnAction(event -> instance.loadDisplayFile(navigation.goForward(1)));
                items.add(item);
                navigate = true;
            }
            if (navigate)
                items.add(new SeparatorMenuItem());
        }


        final Parent model_parent = instance.getRepresentation().getModelParent();
        items.add(new PrintAction(model_parent));

        items.add(new SaveSnapshotAction(model_parent));

        String display_info;
        try
        {
            final DisplayModel model = widget.getDisplayModel();
            final String name = model.getDisplayName();
            final String input =  model.getUserData(DisplayModel.USER_DATA_INPUT_FILE);
            if (Objects.equals(name, input))
                display_info = "Display '" + name + "'";
            else
                display_info = "Display '" + name + "' (" + input + ")";
        }
        catch (Exception ex)
        {
            display_info = "See attached display";
        }
        if (EmailPreferences.isEmailSupported())
            items.add(new SendEmailAction(model_parent, "Display Screenshot", display_info, () ->  Screenshot.imageFromNode(model_parent)));
        if (LogbookUiPreferences.is_supported)
            items.add(new SendLogbookAction(model_parent, "Display Screenshot", display_info, () ->  Screenshot.imageFromNode(model_parent)));
        items.add(new SeparatorMenuItem());

        items.add(new DisplayToolbarAction(instance));

        // If the editor is available, add "Open in Editor"
        final AppResourceDescriptor editor = ApplicationService.findApplication("display_editor");
        if (editor != null && AuthorizationService.hasAuthorization("edit_display"))
            items.add(new OpenInEditorAction(editor, widget));

        items.add(new SeparatorMenuItem());

        items.add(new ReloadDisplayAction(instance));
    }

    private static MenuItem createMenuItem(final Widget widget, final ActionInfo info)
    {
        // Expand macros in action description
        String desc;
        try
        {
            desc = MacroHandler.replace(widget.getEffectiveMacros(), info.getDescription());
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot expand macros in action description '" + info.getDescription() + "'", ex);
            desc = info.getDescription();
        }

        final ImageView icon = new ImageView(new Image(info.getType().getIconURL().toExternalForm()));
        final MenuItem item = new MenuItem(desc, icon);
        item.setOnAction(event -> ActionUtil.handleAction(widget, info));
        return item;
    }
}
