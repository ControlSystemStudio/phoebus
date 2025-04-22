/*******************************************************************************
 * Copyright (c) 2018-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.FocusUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
    public boolean addSupportedEntries(final Node node,
                                    final AlarmClient model,
                                    final ContextMenu menu,
                                    final List<AlarmTreeItem<?>> selection)
    {
        final List<MenuItem> menu_items = menu.getItems();
        final int itemsBefore = menu_items.size();
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

        if (active.size() == 1  &&  active.get(0) instanceof AlarmClientLeaf)
            menu_items.add(new AlarmInfoAction(node, (AlarmClientLeaf) active.get(0)));
        if (acked.size() == 1  &&  acked.get(0) instanceof AlarmClientLeaf)
            menu_items.add(new AlarmInfoAction(node, (AlarmClientLeaf) acked.get(0)));

        // Somehow indicate the origin of guidance, display, command?
        // On one hand it's nice that the context menu inherits all the entries
        // up the alarm tree, but when trying to edit an entry,
        // this means it takes some time to figure out on which item contributed the entry.
        //
        // Considered tool tip, but unclear how to attach TT to menu item.
        final AtomicInteger count = new AtomicInteger();
        for (AlarmTreeItem<?> item : selection)
        {
            addGuidance(node, menu_items, item, count);
            if (count.get() >= AlarmSystem.alarm_menu_max_items)
                break;
        }
        added.clear();
        count.set(0);

        for (AlarmTreeItem<?> item : selection)
        {
            addDisplays(node, menu_items, item, count);
            if (count.get() >= AlarmSystem.alarm_menu_max_items)
                break;
        }
        added.clear();
        count.set(0);

        for (AlarmTreeItem<?> item : selection)
        {
            addCommands(node, menu_items, item, count);
            if (count.get() >= AlarmSystem.alarm_menu_max_items)
                break;
        }
        added.clear();
        count.set(0);

        if (AlarmUI.mayAcknowledge(model))
        {
            if (active.size() > 0)
                menu_items.add(0, new AcknowledgeAction(model, active));
            if (acked.size() > 0)
                menu_items.add(0, new UnAcknowledgeAction(model, acked));
        }
        // Add context menu actions for PVs
        if (pvnames.size() > 0)
        {
            menu_items.add(new SeparatorMenuItem());
            SelectionService.getInstance().setSelection("AlarmUI", pvnames);
            ContextMenuHelper.addSupportedEntries(FocusUtil.setFocusOn(node), menu);
        }
        else
        {
            // search for other context menu actions registered for AlarmTreeItem
            SelectionService.getInstance().setSelection("AlarmUI", selection);
            ContextMenuHelper.addSupportedEntries(FocusUtil.setFocusOn(node), menu);
        }
        return itemsBefore != menu_items.size();
    }

    private static MenuItem createSkippedEntriesHint(final Node node, final String type)
    {
        final MenuItem more = new MenuItem("...");
        more.setOnAction(event ->
        {
            final Alert dialog = new Alert(AlertType.INFORMATION);
            dialog.setHeaderText("Since too many entries were selected,\n" +
                                 "some " + type + " were suppressed.");
            DialogHelper.positionDialog(dialog, node, 0, 0);
            dialog.showAndWait();
        });
        return more;
    }

    private void addGuidance(final Node node,
                             final List<MenuItem> menu_items,
                             final AlarmTreeItem<?> item,
                             final AtomicInteger count)
    {
        for (TitleDetail guidance : item.getGuidance())
            if (added.add(guidance))
                if (count.incrementAndGet() <= AlarmSystem.alarm_menu_max_items)
                    menu_items.add(new ShowGuidanceAction(node, item, guidance));
                else
                {
                    menu_items.add(createSkippedEntriesHint(node, "guidance messages"));
                    return;
                }

        if (item.getParent() != null)
            addGuidance(node, menu_items, item.getParent(), count);
    }

    private void addDisplays(final Node node,
                             final List<MenuItem> menu_items,
                             final AlarmTreeItem<?> item,
                             final AtomicInteger  count)
    {
        for (TitleDetail display : item.getDisplays())
            if (added.add(display))
                if (count.incrementAndGet() <= AlarmSystem.alarm_menu_max_items)
                    menu_items.add(new OpenDisplayAction(node, item, display));
                else
                {
                    menu_items.add(createSkippedEntriesHint(node, "display links"));
                    return;
                }

        if (item.getParent() != null)
            addDisplays(node, menu_items, item.getParent(), count);
    }

    private void addCommands(final Node node,
                             final List<MenuItem> menu_items,
                             final AlarmTreeItem<?> item,
                             final AtomicInteger  count)
    {
        for (TitleDetail command : item.getCommands())
            if (added.add(command))
                if (count.incrementAndGet() <= AlarmSystem.alarm_menu_max_items)
                    menu_items.add(new ExecuteCommandAction(item, command));
                else
                {
                    menu_items.add(createSkippedEntriesHint(node, "commands"));
                    return;
                }

        if (item.getParent() != null)
            addCommands(node, menu_items, item.getParent(), count);
    }
}
