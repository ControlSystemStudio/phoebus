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
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.ui.VTypePair;
import org.phoebus.core.types.TimeStampedProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.util.time.TimestampFormats;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Base controller class for the snapshot table view. It handles common items (UI components, methods) needed
 * by all subclasses.
 */
public abstract class BaseSnapshotTableViewController {

    /**
     * {@link Map} of {@link TableEntry} items corresponding to the snapshot data, i.e.
     * one per PV as defined in the snapshot's configuration. This map is used to
     * populate the {@link TableView}, but other parameters (e.g. hideEqualItems) may
     * determine which elements in the {@link Map} to actually represent.
     */
    protected final Map<String, TableEntry> tableEntryItems = new LinkedHashMap<>();

    protected final Map<String, SaveAndRestorePV> pvs = new HashMap<>();

    @FXML
    protected TableView<TableEntry> snapshotTableView;

    @FXML
    protected TableColumn<TableEntry, Boolean> selectedColumn;

    @FXML
    protected TooltipTableColumn<Integer> idColumn;

    @FXML
    protected TooltipTableColumn<VType> storedValueColumn;

    @FXML
    protected TooltipTableColumn<VType> liveValueColumn;

    @FXML
    protected TableColumn<TableEntry, VTypePair> deltaColumn;

    @FXML
    protected TableColumn<TableEntry, VTypePair> deltaReadbackColumn;

    protected SnapshotController snapshotController;

    protected static boolean resizePolicyNotInitialized = true;

    protected static final Logger LOGGER = Logger.getLogger(BaseSnapshotTableViewController.class.getName());

    @FXML
    protected TableColumn<TableEntry, ?> statusColumn;

    @FXML
    protected TableColumn<TableEntry, ?> severityColumn;

    @FXML
    protected TableColumn<TableEntry, ?> valueColumn;

    @FXML
    protected TableColumn firstDividerColumn;

    //@FXML
    protected TableColumn<TableEntry, ?> compareColumn;

    @FXML
    protected TableColumn<TableEntry, ?> baseSnapshotColumn;

    @FXML
    protected TooltipTableColumn<VType> baseSnapshotValueColumn;

    @FXML
    protected TableColumn<TableEntry, VTypePair> baseSnapshotDeltaColumn;

    /**
     * List of snapshots used managed in this controller. Index 0 is always the base snapshot,
     * all others are snapshots added in the compare use-case.
     */
    protected List<Snapshot> snapshots = new ArrayList<>();

    public BaseSnapshotTableViewController() {
        if (resizePolicyNotInitialized) {
            AccessController.doPrivileged(resizePolicyAction);
        }
    }

    protected static PrivilegedAction<Object> resizePolicyAction = () -> {
        try {
            // Java FX bugfix: the table columns are not properly resized for the first table
            Field f = TableView.CONSTRAINED_RESIZE_POLICY.getClass().getDeclaredField("isFirstRun");
            f.setAccessible(true);
            f.set(TableView.CONSTRAINED_RESIZE_POLICY, Boolean.FALSE);
        } catch (NoSuchFieldException | IllegalAccessException | RuntimeException e) {
            // ignore
        }
        // Even if failed to set the policy, pretend that it was set. In such case the UI will be slightly dorked the
        // first time, but will be OK in all other cases.
        resizePolicyNotInitialized = false;
        return null;
    };

    public void initialize() {
        snapshotTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        snapshotTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        snapshotTableView.getStylesheets().add(SnapshotTableViewController.class.getResource("/save-and-restore-style.css").toExternalForm());

        CheckBoxTableCell checkBoxTableCell = new CheckBoxTableCell<TableEntry, Boolean>();
        checkBoxTableCell.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> System.out.println(snapshotTableView.getItems().get(0).selectedProperty()));
        selectedColumn.setCellFactory(col -> checkBoxTableCell);

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
                                    if (tableEntry.timestampProperty().getValue() != null) {
                                        time = tableEntry.timestampProperty().getValue();
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
                        disableProperty().set(item.readOnlyProperty().get());
                    });
                }
            }
        });

        //selectedColumn.configure(snapshotTableView);

        int width = measureStringWidth("000", Font.font(20));
        idColumn.setPrefWidth(width);
        idColumn.setMinWidth(width);
        idColumn.setCellValueFactory(cell -> {
            int idValue = cell.getValue().idProperty().get();
            idColumn.setPrefWidth(Math.max(idColumn.getWidth(), measureStringWidth(String.valueOf(idValue), Font.font(20))));
            return new ReadOnlyObjectWrapper<>(idValue);
        });

        storedValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        storedValueColumn.setOnEditCommit(e -> {
            VType updatedValue = e.getRowValue().readOnlyProperty().get() ? e.getOldValue() : e.getNewValue();
            ObjectProperty<VTypePair> value = e.getRowValue().valueProperty();
            value.setValue(new VTypePair(value.get().base, updatedValue, value.get().threshold));
            snapshotController.updateLoadedSnapshot( e.getRowValue(), updatedValue);
        });

        liveValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        // TODO: uncomment!
        //baseSnapshotValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
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
    protected static class TimestampTableCell extends TableCell<TableEntry, Instant> {
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

    protected void setSelectionColumnVisible(boolean visible) {
        selectedColumn.visibleProperty().set(visible);
    }

    protected String getPVKey(String pvName, boolean isReadonly) {
        return pvName + "_" + isReadonly;
    }

    protected void showSnapshotInTable(Snapshot snapshot){
        if(snapshots.isEmpty()){
            snapshots.add(snapshot);
        }
        else{
            snapshots.set(0, snapshot);
        }
        AtomicInteger counter = new AtomicInteger(0);
        snapshot.getSnapshotData().getSnapshotItems().forEach(entry -> {
            TableEntry tableEntry = new TableEntry();
            String name = entry.getConfigPv().getPvName();
            tableEntry.idProperty().setValue(counter.incrementAndGet());
            tableEntry.pvNameProperty().setValue(name);
            tableEntry.setConfigPv(entry.getConfigPv());
            tableEntry.setSnapshotValue(entry.getValue(), 0);
            tableEntry.setStoredReadbackValue(entry.getReadbackValue(), 0);
            tableEntry.setReadbackValue(entry.getReadbackValue());
            String key = getPVKey(name, entry.getConfigPv().isReadOnly());
            tableEntry.readbackNameProperty().set(entry.getConfigPv().getReadbackPvName());
            tableEntry.readOnlyProperty().set(entry.getConfigPv().isReadOnly());
            tableEntryItems.put(key, tableEntry);
        });

        connectPVs();
        updateTable(null);
    }

    /**
     * Sets new table entries for this table, but do not change the structure of the table.
     *
     * @param entries the entries to set
     */
    public void updateTable(List<TableEntry> entries) {
        final ObservableList<TableEntry> items = snapshotTableView.getItems();
        final boolean notHide = !snapshotController.isHideEqualItems();
        snapshotTableView.getItems().clear();
        tableEntryItems.entrySet().forEach(e -> {
            // there is no harm if this is executed more than once, because only one line is allowed for these
            // two properties (see SingleListenerBooleanProperty for more details)
            e.getValue().liveStoredEqualProperty().addListener((a, o, n) -> {
                if (snapshotController.isHideEqualItems()) {
                    if (n) {
                        snapshotTableView.getItems().remove(e.getValue());
                    } else {
                        snapshotTableView.getItems().add(e.getValue());
                    }
                }
            });
            if (notHide || !e.getValue().liveStoredEqualProperty().get()) {
                items.add(e.getValue());
            }
        });
    }

    protected void connectPVs() {
        tableEntryItems.values().forEach(e -> {
            SaveAndRestorePV pv = pvs.get(getPVKey(e.getConfigPv().getPvName(), e.getConfigPv().isReadOnly()));
            if (pv == null) {
                pvs.put(getPVKey(e.getConfigPv().getPvName(), e.getConfigPv().isReadOnly()), new SaveAndRestorePV(e));
            } else {
                pv.setSnapshotTableEntry(e);
            }
        });
    }
}
