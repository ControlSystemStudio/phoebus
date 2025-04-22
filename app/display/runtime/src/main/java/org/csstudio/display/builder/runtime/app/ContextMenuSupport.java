/*******************************************************************************
 * Copyright (c) 2017-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.app;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propConfirmDialog;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPassword;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.ImageView;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.representation.ToolkitListener;
import org.csstudio.display.builder.representation.javafx.widgets.JFXBaseRepresentation;
import org.csstudio.display.builder.runtime.Messages;
import org.csstudio.display.builder.runtime.RuntimeAction;
import org.csstudio.display.builder.runtime.RuntimeUtil;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.security.authorization.AuthorizationService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.application.ContextMenuService;
import org.phoebus.ui.application.SaveSnapshotAction;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.PrintAction;
import org.phoebus.ui.spi.ContextMenuEntry;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

/**
 * Context menu for a widget or the display
 *
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ContextMenuSupport {
    private final DisplayRuntimeInstance instance;
    private final ContextMenu menu = new ContextMenu();

    /**
     * Connect context menu to toolkit's `handleContextMenu`
     */
    ContextMenuSupport(final DisplayRuntimeInstance instance) {
        this.instance = instance;
        menu.setAutoHide(true);

        final ToolkitListener tkl = new ToolkitListener() {
            @Override
            public void handleContextMenu(final Widget widget, final int screen_x, final int screen_y) {
                final Node node = JFXBaseRepresentation.getJFXNode(widget);

                Runnable setFocus;
                {
                    // Set the DockPane that contains 'widget' as the active
                    // DockPane, so that applications are launched in the
                    // same DockPane:
                    DisplayModel displayModel;
                    try {
                        displayModel = widget.getTopDisplayModel();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    DisplayRuntimeInstance displayRuntimeInstance = DisplayRuntimeInstance.ofDisplayModel(displayModel);
                    DockItem dockItem = displayRuntimeInstance.getDockItem();
                    DockPane dockPane = dockItem.getDockPane();
                    setFocus = () -> DockPane.setActiveDockPane(dockPane);
                }

                fillMenu(setFocus, widget);
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

    /**
     * Fill context menu with items for widget
     *
     * @param setFocus
     * @param widget
     */
    private void fillMenu(Runnable setFocus, final Widget widget) {
        final ObservableList<MenuItem> items = menu.getItems();
        items.setAll(new WidgetInfoAction(widget));

        // Add menu item to get info for the "top level" widget, but only if widget is not the
        // top level widget. In this manner user may right-click on any portion of the OPI to
        // launch the info dialog for the entire OPI.
        try {
            Widget topWidget = widget.getTopDisplayModel();
            if (!topWidget.equals(widget)) {
                items.add(new SeparatorMenuItem());
                items.add(new WidgetInfoAction(widget.getTopDisplayModel()));
            }
        } catch (Exception exception) {
            logger.log(Level.WARNING, "Unable to get top display model", exception);
        }

        // Widget actions
        // Skip if widget requires password or confirmation dialog,
        // because in here we would invoke actions without those constraints
        final Optional<WidgetProperty<String>> pass = widget.checkProperty(propPassword);
        final Optional<WidgetProperty<Boolean>> prompt = widget.checkProperty(propConfirmDialog);
        final boolean need_dialog = (pass.isPresent()  &&  !pass.get().getValue().isBlank())  ||
                                  (prompt.isPresent()  &&   prompt.get().getValue());

        if (! need_dialog)
            for (ActionInfo info : widget.propActions().getValue().getActions()) {
                List<MenuItem> actionMenuItems = info.getContextMenuItems(RuntimeUtil.getExecutor(), widget);
                if (actionMenuItems != null) {
                    items.addAll(actionMenuItems);
                }
            }

        // Actions of the widget runtime
        final WidgetRuntime<Widget> runtime = RuntimeUtil.getRuntime(widget);
        if (runtime == null)
            throw new NullPointerException("Missing runtime for " + widget);
        for (RuntimeAction info : runtime.getRuntimeActions()) {
            // Load image for action from that action's class loader
            final ImageView icon = ImageCache.getImageView(info.getClass(), info.getIconPath());
            final MenuItem item = new MenuItem(info.getDescription(), icon);
            item.setOnAction(event -> info.run());
            items.add(item);
        }

        items.add(new SeparatorMenuItem());

        // Does widget have a PV name?
        final Optional<WidgetProperty<String>> name_prop = widget.checkProperty(CommonWidgetProperties.propPVName);
        List<ProcessVariable> processVariables;
        if (name_prop.isPresent()) {
            processVariables = List.of(new ProcessVariable(name_prop.get().getValue()));
        } else {   // Add all PVs referenced by the widget.
            Collection<RuntimePV> runtimePvs = runtime.getPVs();
            processVariables =
                    runtimePvs.stream().map(runtimePV -> new ProcessVariable(runtimePV.getName())).collect(Collectors.toList());
        }
        if (!processVariables.isEmpty()) {
            // Set the 'selection' to the PV of this widget
            SelectionService.getInstance().setSelection(DisplayRuntimeApplication.NAME, processVariables);
            // Add PV-based menu entries
            ContextMenuHelper.addSupportedEntries(setFocus, menu);
            items.add(new SeparatorMenuItem());
        }

        // If toolbar is hidden, offer forward/backward navigation
        if (!instance.isToolbarVisible()) {
            boolean navigate = false;
            final DisplayNavigation navigation = instance.getNavigation();
            if (navigation.getBackwardDisplays().size() > 0) {
                final MenuItem item = new MenuItem(Messages.NavigateBack_TT, new ImageView(NavigationAction.backward));
                item.setOnAction(event -> instance.loadDisplayFile(navigation.goBackward(1)));
                items.add(item);
                navigate = true;
            }
            if (navigation.getForwardDisplays().size() > 0) {
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

        try {
            final DisplayModel model = widget.getDisplayModel();

            // Add context menu actions based on the selection (i.e. email, logbook, etc...)
            final Selection originalSelection = SelectionService.getInstance().getSelection();
            final List<SelectionInfo> newSelection = Arrays.asList(SelectionInfo.forModel(model, model_parent));
            SelectionService.getInstance().setSelection(DisplayRuntimeApplication.NAME, newSelection);
            List<ContextMenuEntry> supported = ContextMenuService.getInstance().listSupportedContextMenuEntries();
            supported.stream().forEach(action -> {
                MenuItem menuItem = new MenuItem(action.getName(), new ImageView(action.getIcon()));
                menuItem.setOnAction((e) -> {
                    try {
                        SelectionService.getInstance().setSelection(DisplayRuntimeApplication.NAME, newSelection);
                        action.call(model_parent, SelectionService.getInstance().getSelection());
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, "Failed to execute " + action.getName() + " from display builder.", ex);
                    }
                });
                items.add(menuItem);
            });
            SelectionService.getInstance().setSelection(DisplayRuntimeApplication.NAME, originalSelection);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Failed to construct context menu actions", ex);
        }

        items.add(new SeparatorMenuItem());

        items.add(new DisplayToolbarAction(instance));

        // If the editor is available, add "Open in Editor"
        final AppResourceDescriptor editor = ApplicationService.findApplication("display_editor");
        if (editor != null && AuthorizationService.hasAuthorization("edit_display"))
            items.add(new OpenInEditorAction(editor, widget));

        items.add(new SeparatorMenuItem());

        items.add(new ReloadDisplayAction(instance));
    }
}
