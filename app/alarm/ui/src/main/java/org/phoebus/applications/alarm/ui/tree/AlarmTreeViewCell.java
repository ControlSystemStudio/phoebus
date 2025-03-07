/*******************************************************************************
 * Copyright (c) 2018-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.tree;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import javafx.util.Pair;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.applications.alarm.ui.Messages;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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
            final SeverityLevel severity;
            if (item instanceof AlarmClientLeaf)
            {
                final AlarmClientLeaf leaf = (AlarmClientLeaf) item;
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
                        String enabledDateString = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(enabledDate);
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
                final AlarmClientNode node = (AlarmClientNode) item;

                Optional<Pair<LeavesDisabledStatus, Boolean>> maybeLeavesDisabledStatusBooleanPair = leavesDisabledStatus(node);
                if (maybeLeavesDisabledStatusBooleanPair.isPresent() && !maybeLeavesDisabledStatusBooleanPair.get().getKey().equals(LeavesDisabledStatus.AllEnabled)) {
                    Pair<LeavesDisabledStatus, Boolean> leavesDisabledStatusBooleanPair = maybeLeavesDisabledStatusBooleanPair.get();

                    if (leavesDisabledStatusBooleanPair.getKey().equals(LeavesDisabledStatus.AllDisabled)) {
                        if (leavesDisabledStatusBooleanPair.getValue()) {
                            disabledTimerIndicator.setText("(" + Messages.disabled + "; " + Messages.timer + ")");
                        }
                        else {
                            disabledTimerIndicator.setText("(" + Messages.disabled + ")");
                        }
                    }
                    else if (leavesDisabledStatusBooleanPair.getKey().equals(LeavesDisabledStatus.SomeEnabledSomeDisabled)) {
                        if (leavesDisabledStatusBooleanPair.getValue()) {
                            disabledTimerIndicator.setText("(" + Messages.partlyDisabled + "; " + Messages.timer + ")");
                        }
                        else {
                            disabledTimerIndicator.setText("(" + Messages.partlyDisabled + ")");
                        }
                    }
                }
                else {
                    disabledTimerIndicator.setText("");
                }

                String labelText = item.getName();
                label.setText(labelText);

                severity = node.getState().severity;
                if (maybeLeavesDisabledStatusBooleanPair.isPresent() && maybeLeavesDisabledStatusBooleanPair.get().getKey().equals(LeavesDisabledStatus.AllDisabled)) {
                    label.setTextFill(Color.GRAY);
                }
                else {
                    label.setTextFill(AlarmUI.getColor(severity));
                }
                label.setBackground(AlarmUI.getBackground(severity));
                image.setImage(AlarmUI.getIcon(severity));
            }
            // Profiler showed small advantage when skipping redundant 'setGraphic' call
            if (getGraphic() != content)
                setGraphic(content);
        }
    }

    private boolean isLeafDisabled(AlarmClientLeaf alarmClientLeaf) {
        return !alarmClientLeaf.isEnabled() || alarmClientLeaf.getState().isDynamicallyDisabled();
    }

    private enum LeavesDisabledStatus {
        AllEnabled,
        SomeEnabledSomeDisabled,
        AllDisabled,
    }

    // leavesDisabledStatus() optionally returns a pair.
    //
    // If a pair is _not_ returned, it means that there exist no leaves
    // in 'alarmClientNode', and the disabled status is undefined.
    //
    // When a pair _is_ returned, the first component describes
    // whether all leaves are disabled, all leaves are enabled, or whether
    // some leaves are enabled and some are disabled, and the second component
    // indicates whether one or more disabled leaves have a timer associated
    // with them ('true'), at the end of which they will automatically become
    // enabled again. When the second component is 'false' there is no
    // associated timer.
    private Optional<Pair<LeavesDisabledStatus, Boolean>> leavesDisabledStatus(AlarmClientNode alarmClientNode) {
        List<Pair<LeavesDisabledStatus, Boolean>> leavesDisabledStatusList = new LinkedList<>();
        for (var child : alarmClientNode.getChildren()) {
            if (child instanceof AlarmClientLeaf alarmClientLeaf) {

                if (isLeafDisabled(alarmClientLeaf)) {
                    boolean timer = alarmClientLeaf.getEnabled().enabled_date != null;
                    leavesDisabledStatusList.add(new Pair<>(LeavesDisabledStatus.AllDisabled, timer));
                }
                else {
                    leavesDisabledStatusList.add(new Pair<>(LeavesDisabledStatus.AllEnabled, false));
                }
            }
            else if (child instanceof AlarmClientNode alarmClientNode1 && !alarmClientNode1.getChildren().isEmpty()) {
                if (leavesDisabledStatus(alarmClientNode1).isPresent()) {
                    leavesDisabledStatusList.add(leavesDisabledStatus(alarmClientNode1).get());
                }
                // If leavesDisabledStatus(alarmClientNode1).isPresent() evaluates to false, there are no leaves and therefore no result.
            }
            else if (child instanceof AlarmClientNode alarmClientNode1 && alarmClientNode1.getChildren().isEmpty()) {
                // Don't add any LeavesDisabledStatus, since there are no leaves
            }
            else {
                throw new RuntimeException("Missing case: " + child.getClass().getName());
            }
        }

        Optional<Pair<LeavesDisabledStatus, Boolean>> leavesDisabledStatus = leavesDisabledStatusList.stream().reduce((status1, status2) -> {
            if (status1.getKey().equals(status2.getKey())) {
                return new Pair<>(status1.getKey(), status1.getValue() || status2.getValue());
            }
            else {
                return new Pair<>(LeavesDisabledStatus.SomeEnabledSomeDisabled, status1.getValue() || status2.getValue());
            }
        });

        return leavesDisabledStatus;
    }
}
