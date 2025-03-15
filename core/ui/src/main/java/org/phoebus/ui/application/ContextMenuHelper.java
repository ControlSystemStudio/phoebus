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
import java.util.Optional;
import java.util.logging.Level;

import javafx.scene.control.*;
import org.phoebus.framework.adapter.AdapterService;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.selection.SelectionUtil;
import org.phoebus.ui.docking.DockStage;
import org.phoebus.ui.spi.ContextMenuEntry;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.Window;

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
     *  @param menu Menu where selection-based entries will be added
     *  @return <code>true</code> if a supported entry was added.
     */
    public static boolean addSupportedEntries(Runnable setFocus, final ContextMenu menu)
    {
        final List<ContextMenuEntry> entries = ContextMenuService.getInstance().listSupportedContextMenuEntries();
        if (entries.isEmpty())
            return false;

        for (ContextMenuEntry entry : entries)
        {
            final MenuItem item = new MenuItem(entry.getName());

            final Image icon = entry.getIcon();
            if (icon != null)
                item.setGraphic(new ImageView(icon));
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
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot invoke context menu", ex);
                }
            });
            menu.getItems().add(item);
        }

        return true;
    }
}
