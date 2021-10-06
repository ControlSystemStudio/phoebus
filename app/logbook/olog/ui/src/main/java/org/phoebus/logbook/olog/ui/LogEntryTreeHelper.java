/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.logbook.olog.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.Property;
import org.phoebus.olog.es.api.model.LogGroupProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class LogEntryTreeHelper {

    /**
     * Constructs a hierarchy of {@link TreeItem}s based on the specified list
     * of {@link LogEntry}s. The purpose of the hierarchy and the way it is ordered is
     * to be able to support a UI showing a "log entry thread" view of the entries.
     *
     * @param logEntries A set of {@link LogEntry}s where each item may or may not belong
     *                   to a log entry group as identified by the special purpose
     *                   property {@link LogGroupProperty#NAME}.
     * @param ascendingOrder Specifies whether tree view items should be ordered in ascending created date order.
     * @return An {@link ObservableList} of {@link TreeItem}s where each {@link TreeItem} node
     * may or may not contain child {@link TreeItem}s. The top-level items are ordered
     * in descending date order (as determined by {@link LogEntry#getCreatedDate()}),
     * while the {@link TreeItem} child nodes of a top-level node will be ordered in
     * ascending time order.
     */
    public static ObservableList<TreeItem<LogEntry>> createTree(List<LogEntry> logEntries, boolean ascendingOrder) {
        Map<String, List<LogEntry>> logEntryGroups = new HashMap<>();
        List<LogEntry> nonGroupedItems = new ArrayList<>();
        for (LogEntry logEntry : logEntries) {
            Optional<Property> logGroupId = LogGroupProperty.getLogGroupProperty(logEntry);
            if (logGroupId.isPresent()) {
                String groupId = logGroupId.get().getAttributes().get(LogGroupProperty.ATTRIBUTE_ID);
                if (groupId == null || groupId.isEmpty()) {
                    continue;
                }
                List<LogEntry> logEntryGroup = logEntryGroups.get(groupId);
                if (logEntryGroup == null) {
                    List<LogEntry> entries = new ArrayList<>();
                    entries.add(logEntry);
                    logEntryGroups.put(groupId, entries);
                } else {
                    logEntryGroup.add(logEntry);
                }
            } else {
                nonGroupedItems.add(logEntry);
            }
        }
        // At this point log entries are either in the non-grouped list,
        // or contained in a map in one of the elements in the grouped entries.

        // Now put all non-grouped items in a list of TreeItems
        List<TreeItem<LogEntry>> treeItems = new ArrayList<>();
        List<TreeItem<LogEntry>> nonGrouped =
                nonGroupedItems.stream().map(l -> new TreeItem<LogEntry>(l)).collect(Collectors.toList());
        treeItems.addAll(nonGrouped);

        // Next create tree nodes for each of the log entry groups and add them.
        List<List<LogEntry>> grouped = new ArrayList(logEntryGroups.values());
        for (List<LogEntry> group : grouped) {
            treeItems.add(getGroupedTreeNode(group, ascendingOrder));
        }

        if(ascendingOrder){
            treeItems.sort((t1, t2) -> t1.getValue().getCreatedDate().compareTo(t2.getValue().getCreatedDate()));
        }
        else{
            treeItems.sort((t1, t2) -> t2.getValue().getCreatedDate().compareTo(t1.getValue().getCreatedDate()));
        }
        return FXCollections.observableList(treeItems);
    }

    private static TreeItem<LogEntry> getGroupedTreeNode(List<LogEntry> group, boolean ascendingOrder) {
        if(ascendingOrder){
            group.sort((o1, o2) -> o1.getCreatedDate().compareTo(o2.getCreatedDate()));
        }
        else{
            group.sort((o1, o2) -> o2.getCreatedDate().compareTo(o1.getCreatedDate()));
        }
        // Use the first element to create the parent tree node
        TreeItem<LogEntry> parent = new TreeItem<>(group.get(0));
        int size = group.size();
        for (int i = 1; i < size; i++) {
            TreeItem<LogEntry> child = new TreeItem<>(group.get(i));
            parent.getChildren().add(child);
        }
        parent.expandedProperty().set(true);
        return parent;
    }
}
