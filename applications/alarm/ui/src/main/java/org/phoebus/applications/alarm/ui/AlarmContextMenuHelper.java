/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.javafx.ImageCache;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/** Helper for adding guidance, displays, commands to context menu
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmContextMenuHelper
{
    /** Track entries that have already been added.
     *
     *  <p>For a given entry, the guidance, display links etc. are
     *  added to the context menu.
     *  Then the items from the parent are added.
     *
     *  <p>When multiple items are selected, they may have the same
     *  parent, which would result in duplicated menu entries
     *  for the guidance etc. inherited from the parent.
     *
     *  <p>This set is used to track which entries have already been added.
     */
    private final Set<TitleDetail> added = new HashSet<>();

    /** Add guidance etc. based on selected items
     *  @param node Node for positioning dialog
     *  @param model {@link AlarmClient}
     *  @param menu Menu to extend
     *  @param selection Selected alarm tree items
     */
    public void addSupportedEntries(final Node node,
                                    final AlarmClient model,
                                    final ContextMenu menu,
                                    final List<AlarmTreeItem<?>> selection)
    {
        final List<MenuItem> menu_items = menu.getItems();
        final List<AlarmTreeItem<?>> active = new ArrayList<>();
        final List<AlarmTreeItem<?>> acked = new ArrayList<>();
        final List<ProcessVariable> pvnames = new ArrayList<>();
        for (AlarmTreeItem<?> item : selection)
        {
            final SeverityLevel sev = item.getState().severity;
            if (sev.ordinal() > SeverityLevel.OK.ordinal())
            {
                if (sev.isActive())
                    active.add(item);
                else
                    acked.add(item);
            }
            if (item instanceof AlarmTreeLeaf)
                pvnames.add(new ProcessVariable(item.getName()));
        }

        // TODO Initial context menu item for alarm info, if in alarm

        // TODO Somehow indicate the origin of guidance, display, command.
        // On one hand it's nice that the context menu inherits all the entries
        // up the alarm tree, but when trying to edit an entry,
        // this means it takes some time to figure out on which item contributed the entry.
        for (AlarmTreeItem<?> item : selection)
            addGuidance(node, menu_items, item);
        added.clear();

        for (AlarmTreeItem<?> item : selection)
            addDisplays(node, menu_items, item);
        added.clear();

        for (AlarmTreeItem<?> item : selection)
            addCommands(node, menu_items, item);
        added.clear();

        if (AlarmUI.mayAcknowledge())
        {
            if (active.size() > 0)
                menu_items.add(new AcknowledgeAction(model, active));
            if (acked.size() > 0)
                menu_items.add(new UnAcknowledgeAction(model, acked));
        }

        // Add context menu actions for PVs
        if (pvnames.size() > 0)
        {
            menu_items.add(new SeparatorMenuItem());
            SelectionService.getInstance().setSelection("AlarmUI", pvnames);
            ContextMenuHelper.addSupportedEntries(node, menu);
        }
    }

    private void addGuidance(final Node node,
                             final List<MenuItem> menu_items,
                             final AlarmTreeItem<?> item)
    {
        for (TitleDetail guidance : item.getGuidance())
            if (added.add(guidance))
                menu_items.add(new ShowGuidanceAction(node, item, guidance));

        if (item.getParent() != null)
            addGuidance(node, menu_items, item.getParent());
    }

    private void addDisplays(final Node node,
                             final List<MenuItem> menu_items,
                             final AlarmTreeItem<?> item)
    {
        // TODO Create a new OpenRelatedDisplayAction(..) which opens the resource
        for (TitleDetail display : item.getDisplays())
            if (added.add(display))
                menu_items.add(new MenuItem(display.title, ImageCache.getImageView(AlarmSystem.class, "/icons/related_display.png")));

        if (item.getParent() != null)
            addDisplays(node, menu_items, item.getParent());
    }

    private void addCommands(final Node node,
                             final List<MenuItem> menu_items,
                             final AlarmTreeItem<?> item)
    {
        // TODO Create a new ExecuteCommandAction(..) which executes the command
        for (TitleDetail command : item.getCommands())
            if (added.add(command))
                menu_items.add(new MenuItem(command.title, ImageCache.getImageView(AlarmSystem.class, "/icons/exec_command.png")));

        if (item.getParent() != null)
            addCommands(node, menu_items, item.getParent());
    }
}
