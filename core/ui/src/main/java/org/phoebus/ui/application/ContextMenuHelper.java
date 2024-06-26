/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.ui.application;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javafx.scene.control.Menu;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.selection.SelectionUtil;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Helper for selection-based additions to context menu
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ContextMenuHelper
{
    /** Add entries suitable for the current selection
     *
     *  <p>Invoke inside 'setOnContextMenuRequested' handler,
     *  after adding application specific menu entries,
     *  to add entries based on the current selection.
     *
     *  @param setFocus Sets the correct focus (typically on a DockPane or Window) before running the action associated with a menu entry
     *  @param contextMenu Menu where selection-based entries will be added
     *  @return <code>true</code> if a supported entry was added.
     */
    public static boolean addSupportedEntries(Runnable setFocus, final ContextMenu contextMenu)
    {
        final List<ContextMenuEntry> entries = ContextMenuService.getInstance().listSupportedContextMenuEntries();
        if (entries.isEmpty())
            return false;

        Menu menu = new Menu();
        Runnable hideContextMenu = () -> contextMenu.hide();
        addEntriesToMenu(entries, menu, hideContextMenu, setFocus);
        menu.getItems().forEach(item -> contextMenu.getItems().add(item));

        return true;
    }

    private static void addEntriesToMenu(List<ContextMenuEntry> entries,
                                         Menu menu,
                                         Runnable hideContextMenu,
                                         Runnable setFocus) {
        for (ContextMenuEntry entry : entries)
        {
            final MenuItem item;
            if (entry.getChildren().isEmpty()) {
                item = new MenuItem();

                item.setOnAction(e ->
                {
                    setFocus.run();
                    try
                    {
                        List<Object> selection = new ArrayList<>();
                        SelectionService.getInstance().getSelection()
                                .getSelections().stream().forEach(s -> {
                                    AdapterService.adapt(s, entry.getSupportedType())
                                            .ifPresent(found -> selection.add(found));
                                });
                        entry.call(SelectionUtil.createSelection(selection));
                        hideContextMenu.run();
                    }
                    catch (Exception ex)
                    {
                        logger.log(Level.WARNING, "Cannot invoke context menu", ex);
                    }
                });
            }
            else {
                Menu subMenu = new Menu();
                addEntriesToMenu(entry.getChildren(), subMenu, hideContextMenu, setFocus);
                item = subMenu;
            }

            item.setText(entry.getName());
            final Image icon = entry.getIcon();
            if (icon != null)
                item.setGraphic(new ImageView(icon));
            menu.getItems().add(item);
        }
    }
}
