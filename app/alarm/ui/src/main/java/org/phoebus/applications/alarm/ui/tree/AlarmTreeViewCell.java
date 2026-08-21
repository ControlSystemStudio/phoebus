/*******************************************************************************
 * Copyright (c) 2018-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.applications.alarm.ui.Messages;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.util.time.TimestampFormats;

import java.time.LocalDateTime;

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
    private final Label disabledTimerIndicator = new Label("");
    private final HBox content = new HBox(image, label, disabledTimerIndicator);

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

    public AlarmTreeViewCell() {
        content.setAlignment(Pos.CENTER_LEFT);
        disabledTimerIndicator.setTextFill(Color.GRAY);
    }

    @Override
    protected void updateItem(final AlarmTreeItem<?> item, final boolean empty)
    {
        super.updateItem(item, empty);

        if (empty  ||  item == null)
            setGraphic(null);
        else
        {
            if (item instanceof AlarmClientLeaf leaf)
            {
                final ClientState state = leaf.getState();

                final StringBuilder text = new StringBuilder();
                text.append("PV: ").append(leaf.getName());

                if (!isLeafDisabled(leaf))
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

                    disabledTimerIndicator.setText("");
                } else {
                    if (leaf.getEnabled().enabled_date != null) {
                        LocalDateTime enabledDate = leaf.getEnabled().enabled_date;
                        String enabledDateString = TimestampFormats.SECONDS_FORMAT.format(enabledDate);
                        disabledTimerIndicator.setText("(" + Messages.disabledUntil + " " + enabledDateString + ")");
                    } else {
                        disabledTimerIndicator.setText("(" + Messages.disabled + ")");
                    }

                    label.setTextFill(Color.GRAY);
                    label.setBackground(Background.EMPTY);
                    image.setImage(AlarmUI.disabled_icon);
                }

                label.setText(text.toString());
            }
            else
            {
                // To get the information to display on non-leaf nodes one will need to walk a potentially deep
                // tree structure, so this is done off the UI thread.
                JobManager.schedule("Get Tree Node Info", monitor -> {
                    TreeNodeInfo info = AlarmTreeHelper.getTreeNodeInfo(item);
                    Platform.runLater(() -> {
                        String labelText = item.getName();
                        label.setText(labelText);
                        SeverityLevel severityLevel = item.getState().severity;
                        disabledTimerIndicator.setText(AlarmTreeHelper.treeNodeInfoToString(info));
                        if(info.disabled() + info.disabledWithEnableDate() == info.leaves().size()){
                            label.setTextFill(Color.GRAY);
                        }
                        else{
                            label.setTextFill(AlarmUI.getColor(severityLevel));
                        }
                        label.setBackground(AlarmUI.getBackground(severityLevel));
                        image.setImage(AlarmUI.getIcon(severityLevel));
                    });
                });
            }
            // Profiler showed small advantage when skipping redundant 'setGraphic' call
            if (getGraphic() != content)
                setGraphic(content);
        }
    }

    private boolean isLeafDisabled(AlarmClientLeaf alarmClientLeaf) {
        return !alarmClientLeaf.isEnabled() || alarmClientLeaf.getState().isDynamicallyDisabled();
    }
}
