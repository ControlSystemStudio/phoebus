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
 *
 */

package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import org.phoebus.saveandrestore.util.Utilities;
import org.phoebus.applications.saveandrestore.ui.VTypePair;

import java.util.List;


public class CompareSnapshotsTableViewController extends BaseSnapshotTableViewController {

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<TableEntry, VTypePair> deltaColumn;

    @FXML
    public void initialize() {

        super.initialize();


        deltaColumn.setCellValueFactory(e -> e.getValue().valueProperty());
        deltaColumn.setCellFactory(e -> new VDeltaCellEditor<>());
        deltaColumn.setComparator((pair1, pair2) -> {
            Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
            Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);
            return Double.compare(vtc1.getAbsoluteDelta(), vtc2.getAbsoluteDelta());
        });

    }

    /**
     * Sets new table entries for this table, but do not change the structure of the table.
     *
     * @param entries the entries to set
     */
    public void updateTable(List<TableEntry> entries) {
        final ObservableList<TableEntry> items = snapshotTableView.getItems();
        final boolean notHide = !snapshotController.isHideEqualItems();
        items.clear();
        entries.forEach(e -> {
            // there is no harm if this is executed more than once, because only one line is allowed for these
            // two properties (see SingleListenerBooleanProperty for more details)
            e.liveStoredEqualProperty().addListener((a, o, n) -> {
                if (snapshotController.isHideEqualItems()) {
                    if (n) {
                        snapshotTableView.getItems().remove(e);
                    } else {
                        snapshotTableView.getItems().add(e);
                    }
                }
            });
            if (notHide || !e.liveStoredEqualProperty().get()) {
                items.add(e);
            }
        });
    }
}
