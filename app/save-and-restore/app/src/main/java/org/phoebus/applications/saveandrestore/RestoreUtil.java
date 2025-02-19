/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.RestoreResult;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.ui.RestoreMode;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.saveandrestore.util.SnapshotUtil;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utility class for clients code in need of invoking a restore operation.
 */
public class RestoreUtil {

    /**
     *
     * @param restoreMode Determines if restore is to be done on from client or from service.
     * @param saveAndRestoreService {@link SaveAndRestoreService} API
     * @param node A {@link Node} selected by user in the client UI.
     * @param completionHandler Callback once restore operation has completed. Allows client to perform clean-up.
     */
    public static void restore(RestoreMode restoreMode, SaveAndRestoreService saveAndRestoreService, Node node, Runnable completionHandler) {
        JobManager.schedule("Restore Snapshot \"" + node.getName() + "\"", monitor -> {
            try {
                switch (node.getNodeType()) {
                    case SNAPSHOT -> {
                        SnapshotData snapshotData = saveAndRestoreService.getSnapshot(node.getUniqueId());
                        restoreSnapshotItems(restoreMode, node, snapshotData.getSnapshotItems(), completionHandler);
                    }
                    case COMPOSITE_SNAPSHOT -> {
                        List<SnapshotItem> snapshotItems = saveAndRestoreService.getCompositeSnapshotItems(node.getUniqueId());
                        restoreSnapshotItems(restoreMode, node, snapshotItems, completionHandler);
                    }
                    default ->
                            Logger.getLogger(RestoreUtil.class.getName()).log(Level.WARNING, "Restore from client invoked on non-snapshot node. Should not happen...");
                }
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(Messages.restoreFailed, "", e);
            } finally {
                completionHandler.run();
            }
        });
    }

    private static void restoreSnapshotItems(RestoreMode restoreMode,
                                            Node node,
                                            List<SnapshotItem> snapshotItems,
                                            Runnable completionHandler) {
        List<RestoreResult> restoreResultList = Collections.emptyList();
        switch (restoreMode) {
            case CLIENT_RESTORE -> {
                SnapshotUtil snapshotUtil = new SnapshotUtil();
                restoreResultList = snapshotUtil.restore(snapshotItems);
            }
            case SERVICE_RESTORE -> {
                try {
                    restoreResultList = SaveAndRestoreService.getInstance().restore(snapshotItems);
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle(Messages.errorActionFailed);
                        alert.setContentText(e.getMessage());
                        alert.setHeaderText(Messages.restoreFailed);
                        alert.showAndWait();
                    });
                }
            }
        }
        showAndLogRestore(node, restoreResultList);
        completionHandler.run();
    }

    private static void showAndLogRestore(Node node, List<RestoreResult> restoreResultList) {
        if (restoreResultList.isEmpty()) {
            Logger.getLogger(RestoreUtil.class.getName()).log(Level.FINE, "Restored snapshot \"{0}\"", node.getName());
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(restoreResultList.stream()
                    .map(r -> r.getSnapshotItem().getConfigPv().getPvName()).collect(Collectors.joining(System.lineSeparator())));
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(Messages.restoreFailedPVs);
                alert.setContentText(stringBuilder.toString());
                alert.show();
            });
            Logger.getLogger(RestoreUtil.class.getName()).log(Level.WARNING,
                    "Not all PVs could be restored for \"{0}\": . " + Messages.restoreFailedPVs + "\n{1}",
                    new Object[]{node.getName(), stringBuilder.toString()});
        }
    }
}
