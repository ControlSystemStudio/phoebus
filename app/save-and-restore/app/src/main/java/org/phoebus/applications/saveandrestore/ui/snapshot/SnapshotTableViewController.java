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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.common.Utilities;
import org.phoebus.applications.saveandrestore.common.VTypePair;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.core.types.TimeStampedProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.javafx.JFXUtil;
import org.phoebus.ui.pv.SeverityColors;
import org.phoebus.util.time.TimestampFormats;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class SnapshotTableViewController {

    @FXML
    private TableView<TableEntry> snapshotTableView;

    @FXML
    private SelectionTableColumn selectedColumn;

    @FXML
    private TooltipTableColumn<Integer> idColumn;

    @FXML
    private TooltipTableColumn<ConfigPv> pvNameColumn;

    @FXML
    private TooltipTableColumn<ConfigPv> readbackPVNameColumn;

    @FXML
    private TooltipTableColumn<Instant> timeColumn;

    @FXML
    private TooltipTableColumn<String> storedStatusColumn;

    @FXML
    private TooltipTableColumn<String> liveStatusColumn;

    @FXML
    private TooltipTableColumn<String> storedSeverityColumn;

    @FXML
    private TooltipTableColumn<String> liveSeverityColumn;

    @FXML
    private TooltipTableColumn<VType> storedSetpointColumn;

    @FXML
    private TooltipTableColumn<VTypePair> deltaColumn;

    @FXML
    private TooltipTableColumn<VType> liveValueColumn;

    @FXML
    private TableColumn<TableEntry, VType> storedReadbackColumn;

    @FXML
    private TableColumn<TableEntry, VType> liveReadbackColumn;

    private final List<Snapshot> uiSnapshots = new ArrayList<>();

    private boolean showReadbacks;
    private boolean showDeltaPercentage;

    @FXML
    public void initialize(){

        snapshotTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        snapshotTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        snapshotTableView.getStylesheets().add(SnapshotTableViewController.class.getResource("/save-and-restore-style.css").toExternalForm());

        snapshotTableView.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.SPACE) {
                return;
            }

            ObservableList<TableEntry> selections = snapshotTableView.getSelectionModel().getSelectedItems();

            if (selections == null) {
                return;
            }

            selections.stream().filter(item -> !item.readOnlyProperty().get()).forEach(item -> item.selectedProperty().setValue(!item.selectedProperty().get()));

            // Somehow JavaFX TableView handles SPACE pressed event as going into edit mode of the cell.
            // Consuming event prevents NullPointerException.
            event.consume();
        });

        snapshotTableView.setRowFactory(tableView -> new TableRow<>() {
            final ContextMenu contextMenu = new ContextMenu();

            @Override
            protected void updateItem(TableEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setOnContextMenuRequested(null);
                } else {
                    setOnContextMenuRequested(event -> {
                        List<TimeStampedProcessVariable> selectedPVList = snapshotTableView.getSelectionModel().getSelectedItems().stream()
                                .map(tableEntry -> {
                                    Instant time = Instant.now();
                                    if (tableEntry.liveTimestampProperty().getValue() != null) {
                                        time = tableEntry.liveTimestampProperty().getValue();
                                    }
                                    return new TimeStampedProcessVariable(tableEntry.pvNameProperty().get(), time);
                                })
                                .collect(Collectors.toList());

                        contextMenu.hide();
                        contextMenu.getItems().clear();
                        SelectionService.getInstance().setSelection(SaveAndRestoreApplication.NAME, selectedPVList);
                        ContextMenuHelper.addSupportedEntries(this, contextMenu);
                        contextMenu.getItems().add(new SeparatorMenuItem());
                        MenuItem toggle = new MenuItem();
                        toggle.setText(item.readOnlyProperty().get() ? Messages.makeRestorable : Messages.makeReadOnly);
                        CheckBox toggleIcon = new CheckBox();
                        toggleIcon.setFocusTraversable(false);
                        toggleIcon.setSelected(item.readOnlyProperty().get());
                        toggle.setGraphic(toggleIcon);
                        toggle.setOnAction(actionEvent -> {
                            item.readOnlyProperty().setValue(!item.readOnlyProperty().get());
                            item.selectedProperty().set(!item.readOnlyProperty().get());
                        });
                        contextMenu.getItems().add(toggle);
                        contextMenu.show(this, event.getScreenX(), event.getScreenY());
                    });
                }
            }
        });
    }

    private void createTableForSingleSnapshot(boolean showLiveReadback, boolean showStoredReadback){

        selectedColumn.configure(snapshotTableView);

        int width = measureStringWidth("000", Font.font(20));
        idColumn.setPrefWidth(width);
        idColumn.setMinWidth(width);
        //idColumn.setup("#", Messages.toolTipTableColumIndex, width, width, false);
        idColumn.setCellValueFactory(cell -> {
            int idValue = cell.getValue().idProperty().get();
            idColumn.setPrefWidth(Math.max(idColumn.getWidth(), measureStringWidth(String.valueOf(idValue), Font.font(20))));
            return new ReadOnlyObjectWrapper(idValue);
        });

        selectedColumn.configure(snapshotTableView);

        //pvNameColumn.setup(Messages.toolTipTableColumnPVName,
        //        Messages.toolTipTableColumnPVName, 100, -1, true);
        //pvNameColumn.setCellValueFactory(new PropertyValueFactory<>("pvName"));

        if (showLiveReadback) {
            //readbackPVNameColumn.setup(Messages.toolTipTableColumnReadbackPVName,
            //        Messages.toolTipTableColumnReadbackPVName, 100, -1 ,true);
            //readbackPVNameColumn.setCellValueFactory(new PropertyValueFactory<>("readbackName"));
            readbackPVNameColumn.visibleProperty().set(true);
            readbackPVNameColumn.setPreferredWidth(-1);
        }

        width = measureStringWidth("MM:MM:MM.MMM MMM MM M", null);
        timeColumn.setPrefWidth(width);
        timeColumn.setMinWidth(width);
        //timeColumn.setup(Messages.timestamp,
        //        Messages.toolTipTableColumnTimestamp, width, width, true);
        //timeColumn.setCellValueFactory(new PropertyValueFactory<>("storedTimestamp"));
        timeColumn.setCellFactory(c -> new TimestampTableCell());

        //storedStatusColumn.setup(Messages.status,
        //        Messages.toolTipTableColumnAlarmStatus, 100, 100, true);
        //storedStatusColumn.setCellValueFactory(new PropertyValueFactory<>("storedStatus"));

        //liveStatusColumn.setup(Messages.status,
        //        Messages.toolTipTableColumnAlarmStatus, 100, 100, true);
        //storedStatusColumn.setCellValueFactory(new PropertyValueFactory<>("liveStatus"));

        //storedSeverityColumn.setup(Messages.severity,
        //        Messages.toolTipTableColumnAlarmSeverity, 100, 100, true);
        //storedSeverityColumn.setCellValueFactory(new PropertyValueFactory<>("storedSeverity"));
        storedSeverityColumn.setCellFactory(alarmLogTableTypeStringTableColumn -> new AlarmSeverityCell());

        //liveSeverityColumn.setup(Messages.severity,
        //        Messages.toolTipTableColumnAlarmSeverity, 100, 100, true);
        //liveSeverityColumn.setCellValueFactory(new PropertyValueFactory<>("liveSeverity"));
        liveSeverityColumn.setCellFactory(alarmLogTableTypeStringTableColumn -> new AlarmSeverityCell());

        //storedSetpointColumn.setup(
        //        Messages.setpoint,
        //        Messages.toolTipTableColumnSetpointPVValue, -1, 100, true);
        //storedSetpointColumn.setCellValueFactory(new PropertyValueFactory<>("snapshotVal"));
        storedSetpointColumn.setCellFactory(e -> new VTypeCellEditor<>());
        storedSetpointColumn.setOnEditCommit(e -> {
            VType updatedValue = e.getRowValue().readOnlyProperty().get() ? e.getOldValue() : e.getNewValue();

            ObjectProperty<VTypePair> value = e.getRowValue().valueProperty();
            value.setValue(new VTypePair(value.get().base, updatedValue, value.get().threshold));
            // TODO: check the below controller call
            //controller.updateLoadedSnapshot(0, e.getRowValue(), updatedValue);
        });

        deltaColumn.setComparator((pair1, pair2) -> {
            Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
            Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);
            return Double.compare(vtc1.getAbsoluteDelta(), vtc2.getAbsoluteDelta());
        });

        liveValueColumn.setCellFactory(e -> new VTypeCellEditor<>());

    }

    public void updateTable(List<TableEntry> entries, List<Snapshot> snapshots, boolean showLiveReadback, boolean showStoredReadback, boolean showDeltaPercentage) {
        //snapshotTableView.getColumns().clear();
        uiSnapshots.clear();
        // we should always know if we are showing the stored readback or not, to properly extract the selection
        this.showReadbacks = showLiveReadback;
        this.showDeltaPercentage = showDeltaPercentage;
        uiSnapshots.addAll(snapshots);
        if (snapshots.size() == 1) {
            createTableForSingleSnapshot(showLiveReadback, showStoredReadback);
        } else {
            //createTableForMultipleSnapshots(snapshots);
        }
        //updateTableColumnTitles();
        updateTable(entries);
    }

    /**
     * Sets new table entries for this table, but do not change the structure of the table.
     *
     * @param entries the entries to set
     */
    public void updateTable(List<TableEntry> entries) {
        final ObservableList<TableEntry> items = snapshotTableView.getItems();
        final boolean notHide = false; // TODO: !controller.isHideEqualItems();
        items.clear();
        entries.forEach(e -> {
            // there is no harm if this is executed more than once, because only one line is allowed for these
            // two properties (see SingleListenerBooleanProperty for more details)
            e.liveStoredEqualProperty().addListener((a, o, n) -> {
                if (notHide) {
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

    private int measureStringWidth(String text, Font font) {
        Text mText = new Text(text);
        if (font != null) {
            mText.setFont(font);
        }
        return (int) mText.getLayoutBounds().getWidth();
    }

    /**
     * <code>TimestampTableCell</code> is a table cell for rendering the {@link Instant} objects in the table.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     */
    private class TimestampTableCell extends TableCell<TableEntry, Instant> {

        @Override
        protected void updateItem(Instant item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
            } else if (item == null) {
                setText("---");
            } else {
                setText(TimestampFormats.SECONDS_FORMAT.format((item)));
            }
        }
    }
}
