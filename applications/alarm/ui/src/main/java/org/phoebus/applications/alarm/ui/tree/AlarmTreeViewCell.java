/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.AlarmUI;

import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

/** TreeCell for AlarmTreeItem
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class AlarmTreeViewCell extends TreeCell<AlarmTreeItem<?>>
{
    @Override
    protected void updateItem(final AlarmTreeItem<?> item, final boolean empty)
    {
        super.updateItem(item, empty);
        // Note: Cannot use background because that's used by style sheet and selection/cursor
        if (empty  ||  item == null)
        {
            setText(null);
            setGraphic(null);
        }
        else
        {
            final SeverityLevel severity;
            if (item instanceof AlarmClientLeaf)
            {
                final AlarmClientLeaf leaf = (AlarmClientLeaf) item;
                final ClientState state = leaf.getState();

                final StringBuilder text = new StringBuilder();
                final Image icon;
                text.append("PV: ").append(leaf.getName());

                if (leaf.isEnabled()  &&  !state.isDynamicallyDisabled())
                {   // Add alarm info
                    if (state.severity != SeverityLevel.OK)
                    {
                        text.append(" - ")
                            .append(state.severity).append('/').append(state.message);
                        // Show current severity if different
                        if (state.current_severity != state.severity)
                            text.append(" (")
                                .append(state.current_severity).append('/').append(state.current_message)
                                .append(")");
                    }
                    setTextFill(AlarmUI.getColor(state.severity));
                    icon = AlarmUI.getIcon(state.severity);
                }
                else
                {
                    text.append(" (disabled)");
                    setTextFill(Color.GRAY);
                    icon = AlarmUI.disabled_icon;
                }
                setText(text.toString());
                setGraphic(icon == null ? null : new ImageView(icon));
            }
            else
            {
                final AlarmClientNode node = (AlarmClientNode) item;
                setText(item.getName());

                severity = node.getState().severity;
                setTextFill(AlarmUI.getColor(severity));
                final Image icon = AlarmUI.getIcon(severity);
                setGraphic(icon == null ? null : new ImageView(icon));
            }
        }
    }
}
