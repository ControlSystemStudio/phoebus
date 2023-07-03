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

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.common.Utilities;
import org.phoebus.applications.saveandrestore.common.VTypePair;
import org.phoebus.applications.saveandrestore.model.Snapshot;

import java.time.Instant;
import java.util.List;


public class SnapshotTableViewController extends BaseSnapshotTableViewController {

    @FXML
    private TooltipTableColumn<Instant> timeColumn;

    @FXML
    private TooltipTableColumn<VType> storedReadbackColumn;

    @FXML
    private TooltipTableColumn<VType> liveReadbackColumn;

    @FXML
    private TableColumn<TableEntry, ?> readbackColumn;

    private final SimpleBooleanProperty selectionInverted = new SimpleBooleanProperty(false);

    private final SimpleBooleanProperty showReadbacks = new SimpleBooleanProperty(false);
    private boolean showDeltaPercentage;

    @FXML
    public void initialize() {

        super.initialize();


        selectedColumn.setCellFactory(column -> new SelectionCell());

        CheckBox selectAllCheckBox = new CheckBox();
        selectAllCheckBox.selectedProperty().set(true);
        selectAllCheckBox.setTooltip(new Tooltip(Messages.includeThisPV));
        selectAllCheckBox.selectedProperty().addListener((ob, o, n) ->
                snapshotTableView.getItems().stream().filter(te -> te.readOnlyProperty().not().get())
                        .forEach(te -> te.selectedProperty().set(n)));
        selectedColumn.setGraphic(selectAllCheckBox);

        selectionInverted.addListener((ob, o, n) -> {
            snapshotTableView.getItems().stream().filter(te -> te.readOnlyProperty().not().get())
                    .forEach(te -> {
                        te.selectedProperty().set(te.selectedProperty().not().get());
                    });
        });

        MenuItem inverseMI = new MenuItem(Messages.inverseSelection);
        inverseMI.setOnAction(e -> {
            selectionInverted.set(selectionInverted.not().get());
        });
        final ContextMenu contextMenu = new ContextMenu(inverseMI);
        selectAllCheckBox.setContextMenu(contextMenu);

        timeColumn.setCellFactory(c -> new SnapshotTableViewController.TimestampTableCell());

        deltaColumn.setCellValueFactory(e -> e.getValue().valueProperty());
        deltaColumn.setCellFactory(e -> {
            VDeltaCellEditor<VTypePair> vDeltaCellEditor = new VDeltaCellEditor<>();
            if (showDeltaPercentage) {
                vDeltaCellEditor.setShowDeltaPercentage();
            }
            return vDeltaCellEditor;
        });
        deltaColumn.setComparator((pair1, pair2) -> {
            Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
            Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);
            return Double.compare(vtc1.getAbsoluteDelta(), vtc2.getAbsoluteDelta());
        });

        liveReadbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
        storedReadbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
        readbackColumn.visibleProperty().bind(showReadbacks);
    }

    public void setSnapshotController(SnapshotController snapshotController) {
        this.snapshotController = snapshotController;
    }

    public void updateTable(List<TableEntry> entries, List<Snapshot> snapshots, boolean showLiveReadback, boolean showDeltaPercentage) {
        uiSnapshots.clear();
        // we should always know if we are showing the stored readback or not, to properly extract the selection
        this.showReadbacks.set(showLiveReadback);
        this.showDeltaPercentage = showDeltaPercentage;
        uiSnapshots.addAll(snapshots);
        updateTable(entries);
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
            e.selectedProperty().addListener((ob, o, n) -> {
                System.out.println(e.pvNameProperty().get() + " " + n);
            });
        });
    }

    /**
     * {@link CheckBoxTableCell} handling the selection checkboxes for each row in the table.
     * Some logic is needed in case a PV is defined as read-only in the configuration.
     * For such PVs the checkbox is not rendered. It would be confusing since a read-only
     * PV must not be part of a restore operation.
     */
    private class SelectionCell extends CheckBoxTableCell<TableEntry, Boolean> {

        @Override
        public void updateItem(final Boolean item, final boolean empty) {
            super.updateItem(item, empty);
            TableRow<TableEntry> tableRow = getTableRow();
            if (tableRow != null && tableRow.getItem() != null && tableRow.getItem().readOnlyProperty().get()) {
                setGraphic(null);
            }
        }
    }
}
