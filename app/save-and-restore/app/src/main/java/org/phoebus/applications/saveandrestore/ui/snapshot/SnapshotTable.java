/*
 * This software is Copyright by the Board of Trustees of Michigan
 * State University (c) Copyright 2016.
 *
 * Contact Information:
 *   Facility for Rare Isotope Beam
 *   Michigan State University
 *   East Lansing, MI 48824-1321
 *   http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.common.Utilities;
import org.phoebus.applications.saveandrestore.common.VDisconnectedData;
import org.phoebus.applications.saveandrestore.common.VNoData;
import org.phoebus.applications.saveandrestore.common.VTypePair;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.ui.MultitypeTableCell;
import org.phoebus.core.types.TimeStampedProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.javafx.JFXUtil;
import org.phoebus.ui.pv.SeverityColors;
import org.phoebus.util.time.TimestampFormats;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;

class SnapshotTable extends TableView<TableEntry> {

    protected static boolean resizePolicyNotInitialized = true;

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



    private final List<Snapshot> uiSnapshots = new ArrayList<>();
    private boolean showReadbacks;
    private boolean showDeltaPercentage;
    private final SnapshotController controller;

    private CheckBox selectAllCheckBox;

    /**
     * Constructs a new table.
     *
     * @param controller the controller
     */
    SnapshotTable(SnapshotController controller) {
        if (resizePolicyNotInitialized) {
            AccessController.doPrivileged(resizePolicyAction);
        }
        this.controller = controller;
        setEditable(true);
        getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        setMaxWidth(Double.MAX_VALUE);
        setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        getStylesheets().add(SnapshotTable.class.getResource("/save-and-restore-style.css").toExternalForm());

        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.SPACE) {
                return;
            }

            ObservableList<TableEntry> selections = getSelectionModel().getSelectedItems();

            if (selections == null) {
                return;
            }

            selections.stream().filter(item -> !item.readOnlyProperty().get()).forEach(item -> item.selectedProperty().setValue(!item.selectedProperty().get()));

            // Somehow JavaFX TableView handles SPACE pressed event as going into edit mode of the cell.
            // Consuming event prevents NullPointerException.
            event.consume();
        });

        setRowFactory(tableView -> new TableRow<>() {
            final ContextMenu contextMenu = new ContextMenu();

            @Override
            protected void updateItem(TableEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setOnContextMenuRequested(null);
                } else {
                    setOnContextMenuRequested(event -> {
                        List<TimeStampedProcessVariable> selectedPVList = getSelectionModel().getSelectedItems().stream()
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
                        toggle.setText(item.readOnlyProperty().get() ? "Make restorable" : "Make readonly");
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

    private int measureStringWidth(String text, Font font) {
        Text mText = new Text(text);
        if (font != null) {
            mText.setFont(font);
        }
        return (int) mText.getLayoutBounds().getWidth();
    }


    private void createTableForSingleSnapshot(boolean showLiveReadback, boolean showStoredReadback) {
        List<TableColumn<TableEntry, ?>> snapshotTableEntries = new ArrayList<>(12);

        TableColumn<TableEntry, Boolean> selectedColumn = new SelectionTableColumn(this);
        snapshotTableEntries.add(selectedColumn);

        int width = measureStringWidth("000", Font.font(20));
        TableColumn<TableEntry, Integer> idColumn = new TooltipTableColumn<>("#",
                Messages.toolTipTableColumIndex, width, width, false);
        idColumn.setCellValueFactory(cell -> {
            int idValue = cell.getValue().idProperty().get();
            idColumn.setPrefWidth(Math.max(idColumn.getWidth(), measureStringWidth(String.valueOf(idValue), Font.font(20))));
            return new ReadOnlyObjectWrapper(idValue);
        });
        snapshotTableEntries.add(idColumn);

        TableColumn<TableEntry, ConfigPv> pvNameColumn = new TooltipTableColumn<>("PV Name",
                Messages.toolTipTableColumnPVName, 100);

        pvNameColumn.setCellValueFactory(new PropertyValueFactory<>("pvName"));
        snapshotTableEntries.add(pvNameColumn);

        if (showLiveReadback) {
            TableColumn<TableEntry, ConfigPv> readbackPVName = new TooltipTableColumn<>("Readback\nPV Name",
                    Messages.toolTipTableColumnReadbackPVName, 100);
            readbackPVName.setCellValueFactory(new PropertyValueFactory<>("readbackName"));
            snapshotTableEntries.add(readbackPVName);
        }

        width = measureStringWidth("MM:MM:MM.MMM MMM MM M", null);

        /********************************************************************************************************/

        TableColumn<TableEntry, ?> storedDataTableColumn = new TooltipTableColumn<>("Stored", "Stored Data", -1);

        TableColumn<TableEntry, Instant> timestampStoredColumn = new TooltipTableColumn<>("Time",
                Messages.toolTipTableColumnTimestamp, width, width, true);
        timestampStoredColumn.setCellValueFactory(new PropertyValueFactory<>("storedTimestamp"));
        timestampStoredColumn.setCellFactory(c -> new TimestampTableCell());
        timestampStoredColumn.getStyleClass().add("timestamp-column");
        timestampStoredColumn.setPrefWidth(width);

        TableColumn<TableEntry, String> statusStoredColumn = new TooltipTableColumn<>("Status",
                Messages.toolTipTableColumnAlarmStatus, 100, 100, true);
        statusStoredColumn.setCellValueFactory(new PropertyValueFactory<>("storedStatus"));

        TableColumn<TableEntry, String> severityStoredColumn = new TooltipTableColumn<>("Severity",
                Messages.toolTipTableColumnAlarmSeverity, 100, 100, true);
        severityStoredColumn.setCellValueFactory(new PropertyValueFactory<>("storedSeverity"));
        severityStoredColumn.setCellFactory(alarmLogTableTypeStringTableColumn -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setStyle("-fx-text-fill: black;  -fx-background-color: transparent");
                    setText(null);
                }
                else if(item == null || "---".equals(item)){
                    setStyle("-fx-text-fill: black;  -fx-background-color: transparent");
                    setText("---");
                } else {
                    setText(item);
                    AlarmSeverity alarmSeverity = AlarmSeverity.valueOf(item);
                    setStyle("-fx-alignment: center; -fx-border-color: transparent; -fx-border-width: 2 0 2 0; -fx-background-insets: 2 0 2 0; -fx-text-fill: " +
                            JFXUtil.webRGB(SeverityColors.getTextColor(alarmSeverity)) + ";  -fx-background-color: " + JFXUtil.webRGB(SeverityColors.getBackgroundColor(alarmSeverity)));
                }
            }
        });

        TableColumn<TableEntry, VType> storedValueColumn = new TooltipTableColumn<>(
                "Stored Setpoint",
                Messages.toolTipTableColumnSetpointPVValue, 100);
        storedValueColumn.setCellValueFactory(new PropertyValueFactory<>("snapshotVal"));
        storedValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        storedValueColumn.setEditable(true);
        storedValueColumn.setOnEditCommit(e -> {
            VType updatedValue = e.getRowValue().readOnlyProperty().get() ? e.getOldValue() : e.getNewValue();

            ObjectProperty<VTypePair> value = e.getRowValue().valueProperty();
            value.setValue(new VTypePair(value.get().base, updatedValue, value.get().threshold));
            controller.updateLoadedSnapshot(0, e.getRowValue(), updatedValue);
        });

        storedDataTableColumn.getColumns().addAll(timestampStoredColumn, statusStoredColumn, severityStoredColumn, storedValueColumn);

        snapshotTableEntries.add(storedDataTableColumn);

        TableColumn<TableEntry, VTypePair> delta = new TooltipTableColumn<>(
                Utilities.DELTA_CHAR + " Live Setpoint",
                "", 100);
        delta.setCellValueFactory(e -> e.getValue().valueProperty());
        delta.setCellFactory(e -> {
            VDeltaCellEditor vDeltaCellEditor = new VDeltaCellEditor<>();
            if (showDeltaPercentage) {
                vDeltaCellEditor.setShowDeltaPercentage();
            }

            return vDeltaCellEditor;
        });
        delta.setEditable(false);

        delta.setComparator((pair1, pair2) -> {
            Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
            Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);

            return Double.compare(vtc1.getAbsoluteDelta(), vtc2.getAbsoluteDelta());
        });

        snapshotTableEntries.add(delta);


        TableColumn<TableEntry, ?> liveDataTableColumn = new TooltipTableColumn<>("Live", "Live Data", -1);

        TableColumn<TableEntry, Instant> timestampLiveColumn = new TooltipTableColumn<>("Time",
                Messages.toolTipTableColumnTimestamp, width, width, true);
        timestampLiveColumn.setCellValueFactory(new PropertyValueFactory<>("liveTimestamp"));
        timestampLiveColumn.setCellFactory(c -> new TimestampTableCell());
        timestampLiveColumn.getStyleClass().add("timestamp-column");
        timestampLiveColumn.setPrefWidth(width);

        TableColumn<TableEntry, String> statusLiveColumn = new TooltipTableColumn<>("Status",
                Messages.toolTipTableColumnAlarmStatus, 100, 100, true);
        statusLiveColumn.setCellValueFactory(new PropertyValueFactory<>("liveStatus"));

        TableColumn<TableEntry, String> severityLiveColumn = new TooltipTableColumn<>("Severity",
                Messages.toolTipTableColumnAlarmSeverity, 100, 100, true);
        severityLiveColumn.setCellValueFactory(new PropertyValueFactory<>("liveSeverity"));
        severityLiveColumn.setCellFactory(alarmLogTableTypeStringTableColumn -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setStyle("-fx-text-fill: black;  -fx-background-color: transparent");
                    setText(null);
                }
                else if(item == null){
                    setStyle("-fx-text-fill: black;  -fx-background-color: transparent");
                    setText("---");
                } else {
                    setText(item);
                    AlarmSeverity alarmSeverity = AlarmSeverity.valueOf(item);
                    setStyle("-fx-alignment: center; -fx-border-color: transparent; -fx-border-width: 2 0 2 0; -fx-background-insets: 2 0 2 0; -fx-text-fill: " +
                            JFXUtil.webRGB(SeverityColors.getTextColor(alarmSeverity)) + ";  -fx-background-color: " + JFXUtil.webRGB(SeverityColors.getBackgroundColor(alarmSeverity)));
                }
            }
        });

        TableColumn<TableEntry, VType> liveValueColumn = new TooltipTableColumn<>("Live Setpoint", "Current PV Value",
                100);
        liveValueColumn.setCellValueFactory(new PropertyValueFactory<>("liveValue"));
        liveValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        liveValueColumn.setEditable(false);


        liveDataTableColumn.getColumns().addAll(timestampLiveColumn, statusLiveColumn, severityLiveColumn, liveValueColumn);

        snapshotTableEntries.add(liveDataTableColumn);


        /********************************************************************************************************/


        if (showStoredReadback) {
            TableColumn<TableEntry, VType> storedReadbackColumn = new TooltipTableColumn<>(
                    "Stored Readback\n(" + Utilities.DELTA_CHAR + " Stored Setpoint)", "Stored Readback Value", 100);
            storedReadbackColumn.setCellValueFactory(new PropertyValueFactory<>("storedReadback"));
            storedReadbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
            storedReadbackColumn.setEditable(false);
            snapshotTableEntries.add(storedReadbackColumn);
        }



        if (showLiveReadback) {
            TableColumn<TableEntry, VType> readbackColumn = new TooltipTableColumn<>(
                    "Live Readback\n(" + Utilities.DELTA_CHAR + " Live Setpoint)", "Current Readback Value", 100);
            readbackColumn.setCellValueFactory(new PropertyValueFactory<>("liveReadback"));
            readbackColumn.setCellFactory(e -> new VTypeCellEditor<>());
            readbackColumn.setEditable(false);
            snapshotTableEntries.add(readbackColumn);
        }

        getColumns().addAll(snapshotTableEntries);
    }

    /**
     * Updates the table by setting new content, including the structure. The table is always recreated, even if the new
     * structure is identical to the old one. This is slightly more expensive; however, this method is only invoked per
     * user request (button click).
     *
     * @param entries             the table entries (rows) to set on the table
     * @param snapshots           the snapshots which are currently displayed
     * @param showLiveReadback    true if readback column should be visible or false otherwise
     * @param showStoredReadback  true if the stored readback value columns should be visible or false otherwise
     * @param showDeltaPercentage true if delta percentage should be be visible or false otherwise
     */
    public void updateTable(List<TableEntry> entries, List<Snapshot> snapshots, boolean showLiveReadback, boolean showStoredReadback, boolean showDeltaPercentage) {
        getColumns().clear();
        uiSnapshots.clear();
        // we should always know if we are showing the stored readback or not, to properly extract the selection
        this.showReadbacks = showLiveReadback;
        this.showDeltaPercentage = showDeltaPercentage;
        uiSnapshots.addAll(snapshots);
        if (snapshots.size() == 1) {
            createTableForSingleSnapshot(showLiveReadback, showStoredReadback);
        } else {
            createTableForMultipleSnapshots(snapshots);
        }
        updateTableColumnTitles();
        updateTable(entries);
    }

    /**
     * Sets new table entries for this table, but do not change the structure of the table.
     *
     * @param entries the entries to set
     */
    public void updateTable(List<TableEntry> entries) {
        final ObservableList<TableEntry> items = getItems();
        final boolean notHide = !controller.isHideEqualItems();
        items.clear();
        entries.forEach(e -> {
            // there is no harm if this is executed more than once, because only one line is allowed for these
            // two properties (see SingleListenerBooleanProperty for more details)
            e.liveStoredEqualProperty().addListener((a, o, n) -> {
                if (controller.isHideEqualItems()) {
                    if (n) {
                        getItems().remove(e);
                    } else {
                        getItems().add(e);
                    }
                }
            });
            if (notHide || !e.liveStoredEqualProperty().get()) {
                items.add(e);
            }
        });
    }

    /**
     * Update the table column titles, by putting an asterisk to non saved snapshots or remove asterisk from saved
     * snapshots.
     */
    private void updateTableColumnTitles() {
        // add the * to the title of the column if the snapshot is not saved
        /*
        if (uiSnapshots.size() == 1) {
            ((TooltipTableColumn<?>) getColumns().get(6)).setSaved(true);
        } else {
            TableColumn<TableEntry, ?> column = getColumns().get(4);
            for (int i = 0; i < uiSnapshots.size(); i++) {
                TableColumn tableColumn = column.getColumns().get(i);
                if (tableColumn instanceof DividerTableColumn) {
                    continue;
                }
                ((TooltipTableColumn<?>) tableColumn).setSaved(true);
            }
        }

         */
    }

    /**
     * SnapshotTable cell renderer styled to fit the {@link DividerTableColumn}
     */
    private class DividerCell extends TableCell {
        @Override
        protected void updateItem(final Object object, final boolean empty) {
            super.updateItem(object, empty);
            getStyleClass().add("divider");
        }
    }

    /**
     * A table column styled to act as a divider between other columns.
     */
    protected class DividerTableColumn extends TableColumn {

        public DividerTableColumn() {
            setPrefWidth(10);
            setMinWidth(10);
            setMaxWidth(50);
            setCellFactory(c -> new DividerCell());
        }
    }

    private void createTableForMultipleSnapshots(List<Snapshot> snapshots) {
        List<TableColumn<TableEntry, ?>> list = new ArrayList<>(7);
        TableColumn<TableEntry, Boolean> selectedColumn = new SelectionTableColumn(this);
        list.add(selectedColumn);

        int width = measureStringWidth("000", Font.font(20));
        TableColumn<TableEntry, Integer> idColumn = new TooltipTableColumn<>("#",
                Messages.toolTipTableColumIndex, width, width, false);
        idColumn.setCellValueFactory(cell -> {
            int idValue = cell.getValue().idProperty().get();
            idColumn.setPrefWidth(Math.max(idColumn.getWidth(), measureStringWidth(String.valueOf(idValue), Font.font(20))));

            return new ReadOnlyObjectWrapper(idValue);
        });
        list.add(idColumn);

        TableColumn<TableEntry, String> setpointPVName = new TooltipTableColumn<>(Messages.pvName,
                Messages.toolTipUnionOfSetpointPVNames, 100);
        setpointPVName.setCellValueFactory(new PropertyValueFactory<>("pvName"));
        list.add(setpointPVName);

        list.add(new DividerTableColumn());

        TableColumn<TableEntry, ?> storedValueColumn = new TooltipTableColumn<>(Messages.storedValues,
                Messages.toolTipTableColumnPVValues, -1);
        storedValueColumn.getStyleClass().add("toplevel");

        String baseSnapshotTimeStamp = snapshots.get(0).getSnapshotNode().getCreated() == null ?
                "" :
                " (" + TimestampFormats.SECONDS_FORMAT.format(snapshots.get(0).getSnapshotNode().getCreated().toInstant()) + ")";
        String snapshotName = snapshots.get(0).getSnapshotNode().getName() + baseSnapshotTimeStamp;

        TableColumn<TableEntry, ?> baseCol = new TooltipTableColumn<>(
                snapshotName,
                Messages.toolTipTableColumnSetpointPVValue, 33);
        baseCol.getStyleClass().add("second-level");

        TableColumn<TableEntry, VType> storedBaseSetpointValueColumn = new TooltipTableColumn<>(
                Messages.baseSetpoint,
                Messages.toolTipTableColumnBaseSetpointValue, 100);

        storedBaseSetpointValueColumn.setCellValueFactory(new PropertyValueFactory<>("snapshotVal"));
        storedBaseSetpointValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        storedBaseSetpointValueColumn.setEditable(true);
        storedBaseSetpointValueColumn.setOnEditCommit(e -> {
            VType updatedValue = e.getRowValue().readOnlyProperty().get() ? e.getOldValue() : e.getNewValue();

            ObjectProperty<VTypePair> value = e.getRowValue().valueProperty();
            value.setValue(new VTypePair(value.get().base, updatedValue, value.get().threshold));
            controller.updateLoadedSnapshot(0, e.getRowValue(), updatedValue);

            for (int i = 1; i < snapshots.size(); i++) {
                ObjectProperty<VTypePair> compareValue = e.getRowValue().compareValueProperty(i);
                compareValue.setValue(new VTypePair(updatedValue, compareValue.get().value, compareValue.get().threshold));
            }
        });

        baseCol.getColumns().add(storedBaseSetpointValueColumn);

        // show deltas in separate column
        TableColumn<TableEntry, VTypePair> delta = new TooltipTableColumn<>(
                Utilities.DELTA_CHAR + " Live Setpoint",
                "", 100);

        delta.setCellValueFactory(e -> e.getValue().valueProperty());
        delta.setCellFactory(e -> {
            VDeltaCellEditor vDeltaCellEditor = new VDeltaCellEditor<>();
            if (showDeltaPercentage) {
                vDeltaCellEditor.setShowDeltaPercentage();
            }

            return vDeltaCellEditor;
        });
        delta.setEditable(false);
        delta.setComparator((pair1, pair2) -> {
            Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
            Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);

            if (!vtc1.isWithinThreshold() && vtc2.isWithinThreshold()) {
                return -1;
            } else if (vtc1.isWithinThreshold() && !vtc2.isWithinThreshold()) {
                return 1;
            } else {
                return 0;
            }
        });
        baseCol.getColumns().add(delta);

        storedValueColumn.getColumns().addAll(baseCol, new DividerTableColumn());

        for (int i = 1; i < snapshots.size(); i++) {
            final int snapshotIndex = i;

            snapshotName = snapshots.get(snapshotIndex).getSnapshotNode().getName() + " (" +
                    TimestampFormats.SECONDS_FORMAT.format(snapshots.get(snapshotIndex).getSnapshotNode().getCreated().toInstant()) + ")";


            TooltipTableColumn<VTypePair> baseSnapshotCol = new TooltipTableColumn<>(snapshotName,
                    "Setpoint PV value when the " + snapshotName + " snapshot was taken", 100);
            baseSnapshotCol.getStyleClass().add("second-level");

            TooltipTableColumn<VTypePair> setpointValueCol = new TooltipTableColumn<>(
                    Messages.setpoint,
                    "Setpoint PV value when the " + snapshotName + " snapshot was taken", 66);


            setpointValueCol.setCellValueFactory(e -> e.getValue().compareValueProperty(snapshotIndex));
            setpointValueCol.setCellFactory(e -> new VTypeCellEditor<>());
            setpointValueCol.setEditable(false);

            baseSnapshotCol.getColumns().add(setpointValueCol);

            TooltipTableColumn<VTypePair> deltaCol = new TooltipTableColumn<>(
                    Utilities.DELTA_CHAR + Messages.baseSetpoint,
                    "Setpoint PVV value when the " + snapshotName + " snapshot was taken", 50);
            deltaCol.setCellValueFactory(e -> e.getValue().compareValueProperty(snapshotIndex));
            deltaCol.setCellFactory(e -> {
                VDeltaCellEditor vDeltaCellEditor = new VDeltaCellEditor<>();
                if (showDeltaPercentage) {
                    vDeltaCellEditor.setShowDeltaPercentage();
                }

                return vDeltaCellEditor;
            });
            deltaCol.setEditable(false);

            deltaCol.setComparator((pair1, pair2) -> {
                Utilities.VTypeComparison vtc1 = Utilities.valueToCompareString(pair1.value, pair1.base, pair1.threshold);
                Utilities.VTypeComparison vtc2 = Utilities.valueToCompareString(pair2.value, pair2.base, pair2.threshold);

                if (!vtc1.isWithinThreshold() && vtc2.isWithinThreshold()) {
                    return -1;
                } else if (vtc1.isWithinThreshold() && !vtc2.isWithinThreshold()) {
                    return 1;
                } else {
                    return 0;
                }
            });
            baseSnapshotCol.getColumns().addAll(deltaCol);
            storedValueColumn.getColumns().addAll(baseSnapshotCol, new DividerTableColumn());
        }
        list.add(storedValueColumn);

        TableColumn<TableEntry, VType> liveValueColumn = new TooltipTableColumn<>(Messages.liveSetpoint,
                Messages.currentSetpointValue, 100);

        liveValueColumn.setCellValueFactory(new PropertyValueFactory<>("liveValue"));
        liveValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        liveValueColumn.setEditable(false);
        list.add(liveValueColumn);

        getColumns().addAll(list);
    }

    /**
     * <code>TimestampTableCell</code> is a table cell for rendering the {@link Instant} objects in the table.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     */
    private static class TimestampTableCell extends TableCell<TableEntry, Instant> {
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
