/*******************************************************************************
 * Copyright (c) 2018-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.AlarmUI;

/** TreeCell for AlarmTreeItem
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class AlarmTreeViewCell extends TreeCell<AlarmTreeItem<?>>
{
    // Originally, the tree cell "graphics" were used for the icon,
    // and the built-in label/text that can be controlled via
    // setText and setBackground for the text.
    // But using that built-in label/text background intermittently removes
    // the "triangle" for expanding/collapsing subtrees
    // as well as the "cursor" for selecting tree cells or navigating
    // cells via cursor keys.
    // So we add our own "graphics" to hold an icon and text
    private final Label label = new Label();
    private final ImageView image = new ImageView();
    private final HBox content = new HBox(image, label);

    // TreeCell optimizes redraws by suppressing updates
    // when old and new values match.
    // Since we use the model item as a value,
    // the cell sees no change, in fact an identical reference.
    // In the fullness of time, a complete redesign might be useful
    // to present changing values to the TreeCell, but also note
    // the issue shown in org.phoebus.applications.alarm.TreeItemUpdateDemo
    //
    // So for now we simply force redraws by always pretending a change.
    // This seems bad for performance, but profiling the alarm GUI for
    // a large configuration like org.phoebus.applications.alarm.AlarmConfigProducerDemo
    // with 1000 'sections' of 10 subsections of 10 PVs,
    // the time spent in updateItem is negligible.
    @Override
    protected boolean isItemChanged(final AlarmTreeItem<?> before, final AlarmTreeItem<?> after)
    {
        return true;
    }

    @Override
    protected void updateItem(final AlarmTreeItem<?> item, final boolean empty)
    {
        super.updateItem(item, empty);

        if (empty  ||  item == null)
            setGraphic(null);
        else
        {
            final SeverityLevel severity;
            if (item instanceof AlarmClientLeaf)
            {
                final AlarmClientLeaf leaf = (AlarmClientLeaf) item;
                final ClientState state = leaf.getState();

                final StringBuilder text = new StringBuilder();
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
                    label.setTextFill(AlarmUI.getColor(state.severity));
                    label.setBackground(AlarmUI.getBackground(state.severity));
                    image.setImage(AlarmUI.getIcon(state.severity));
                }
                else
                {
                    text.append(" (disabled)");
                    label.setTextFill(Color.GRAY);
                    label.setBackground(Background.EMPTY);
                    image.setImage(AlarmUI.disabled_icon);
                }
                label.setText(text.toString());
            }
            else
            {
                final AlarmClientNode node = (AlarmClientNode) item;
                label.setText(item.getName());

                severity = node.getState().severity;
                label.setTextFill(AlarmUI.getColor(severity));
                label.setBackground(AlarmUI.getBackground(severity));
                image.setImage(AlarmUI.getIcon(severity));
            }
            // Profiler showed small advantage when skipping redundant 'setGraphic' call
            if (getGraphic() != content)
                setGraphic(content);
        }
    }
}
