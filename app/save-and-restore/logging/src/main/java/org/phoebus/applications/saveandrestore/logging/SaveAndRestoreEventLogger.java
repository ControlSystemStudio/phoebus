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

package org.phoebus.applications.saveandrestore.logging;

import javafx.application.Platform;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.event.SaveAndRestoreEventReceiver;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogbookPreferences;

import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Implementation of the {@link SaveAndRestoreEventReceiver} with the purpose of logging the events
 * (new snapshot or restore of a snapshot) to the electronic logbook, if one is configured. When notified it will launch
 * an available logbook app with a {@link LogEntry} holding information about the user invoked
 * operation. The logbook app is expected to render the log entry editor UI for the user so that additional
 * information can be entered by the user before the log entry is submitted.
 */
@SuppressWarnings("unused")
public class SaveAndRestoreEventLogger implements SaveAndRestoreEventReceiver {

    private static final Logger logger = Logger.getLogger(SaveAndRestoreEventLogger.class.getName());

    /**
     * Called when a snapshot has been created and persisted by the save-and-restore service.
     *
     * @param node         The created and persisted {@link Node}
     * @param errorHandler An error handler callback.
     */
    @Override
    public void snapshotSaved(Node node, Consumer<String> errorHandler) {
        if (!LogbookPreferences.is_supported) {
            return;
        }

        SaveSnapshotActionInfo saveSnapshotActionInfo = new SaveSnapshotActionInfo();
        saveSnapshotActionInfo.setSnapshotUniqueId(node.getUniqueId());
        saveSnapshotActionInfo.setSnapshotCreatedDate(node.getCreated());
        saveSnapshotActionInfo.setActionPerformedBy(System.getProperty("user.name"));
        saveSnapshotActionInfo.setComment(node.getDescription());
        saveSnapshotActionInfo.setSnapshotName(node.getName());
        SelectionService.getInstance().setSelection("SaveAndRestoreLogging", List.of(saveSnapshotActionInfo));
        Platform.runLater(() -> ApplicationService.createInstance("logbook"));
    }

    /**
     * Called when a snapshot has been restored.
     *
     * @param node         The restored snapshot {@link Node}
     * @param failedPVs    List of PV names that could not be restored. Empty if all PVs in the configuration were
     *                     restored successfully.
     * @param errorHandler An error handler callback.
     */
    @Override
    public void snapshotRestored(Node node, List<String> failedPVs, Consumer<String> errorHandler) {

        if (!LogbookPreferences.is_supported) {
            return;
        }

        RestoreSnapshotActionInfo restoreSnapshotActionInfo = new RestoreSnapshotActionInfo();
        restoreSnapshotActionInfo.setSnapshotUniqueId(node.getUniqueId());
        restoreSnapshotActionInfo.setSnapshotCreatedDate(node.getCreated());
        restoreSnapshotActionInfo.setActionPerformedBy(System.getProperty("user.name"));
        restoreSnapshotActionInfo.setComment(node.getDescription());
        restoreSnapshotActionInfo.setGolden(node.hasTag(Tag.GOLDEN));
        restoreSnapshotActionInfo.setSnapshotName(node.getName());
        restoreSnapshotActionInfo.setFailedPVs(failedPVs);
        SelectionService.getInstance().setSelection("SaveAndRestoreLogging", List.of(restoreSnapshotActionInfo));
        Platform.runLater(() -> ApplicationService.createInstance("logbook"));
    }
}
