/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import org.phoebus.applications.alarm.model.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.ClientState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.AlarmUI;

import javafx.scene.control.TreeCell;
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
                text.append("PV: ").append(leaf.getName());

                if (leaf.isEnabled())
                {   // Add alarm info
                    if (state.severity != SeverityLevel.OK)
                    {
                        text.append(" (")
                            .append(state.severity).append('/').append(state.message);
                        // For ack'ed alarm show the current severity
                        if (state.severity.ordinal() <= SeverityLevel.UNDEFINED_ACK.ordinal())
                            text.append(", ")
                                .append(state.current_severity).append('/').append(state.current_message);
                        text.append(')');
                    }
                    setTextFill(AlarmUI.getColor(state.severity));
                }
                else
                {
                    text.append(" (disabled)");
                    setTextFill(Color.GRAY);
                }
                setText(text.toString());
                setGraphic(new ImageView(AlarmUI.getIcon(state.severity)));
            }
            else
            {
                final AlarmClientNode node = (AlarmClientNode) item;
                setText(item.getName());

                severity = node.getState().severity;
                setTextFill(AlarmUI.getColor(severity));
                setGraphic(new ImageView(AlarmUI.getIcon(severity)));
            }
        }
    }
}
