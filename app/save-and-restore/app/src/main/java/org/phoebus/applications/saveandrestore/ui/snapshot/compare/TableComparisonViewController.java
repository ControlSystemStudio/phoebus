/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;


import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.epics.util.array.IteratorNumber;
import org.epics.util.array.ListBoolean;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VBooleanArray;
import org.epics.vtype.VByte;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VFloat;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLong;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VShort;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.ui.VTypePair;
import org.phoebus.applications.saveandrestore.ui.snapshot.VDeltaCellEditor;
import org.phoebus.applications.saveandrestore.ui.snapshot.VTypeCellEditor;
import org.phoebus.core.vtypes.VDisconnectedData;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TableComparisonViewController {

    @SuppressWarnings("unused")
    @FXML
    private TableView<ComparisonData> comparisonTable;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<ComparisonData, Integer> indexColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<ComparisonData, VType> storedValueColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<ComparisonData, VType> liveValueColumn;


    @SuppressWarnings("unused")
    @FXML
    private TableColumn<ComparisonData, VTypePair> deltaColumn;

    @SuppressWarnings("unused")
    @FXML
    private Label pvName;

    private final StringProperty pvNameProperty = new SimpleStringProperty();

    /**
     * The time between updates of dynamic data in the table, in ms.
     */
    private static final long TABLE_UPDATE_INTERVAL = 500;

    @FXML
    public void initialize() {
        comparisonTable.getStylesheets().add(TableComparisonViewController.class.getResource("/save-and-restore-style.css").toExternalForm());
        pvName.textProperty().bind(pvNameProperty);
        storedValueColumn.setCellValueFactory(cell ->
                cell.getValue().getColumnEntries().get(cell.getValue().indexProperty().get()).storedValueProperty());
        storedValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        liveValueColumn.setCellValueFactory(cell ->
                cell.getValue().getColumnEntries().get(cell.getValue().indexProperty().get()).liveValueProperty());
        liveValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        deltaColumn.setCellValueFactory(cell ->
                cell.getValue().getColumnEntries().get(cell.getValue().indexProperty().get()).getDelta());
        deltaColumn.setComparator(Comparator.comparingDouble(VTypePair::getAbsoluteDelta));

        deltaColumn.setCellFactory(e -> new VDeltaCellEditor<>());
    }

    public void loadDataAndConnect(VType data, String pvName) {

        pvNameProperty.set(pvName);
        List<ColumnEntry> columnEntries = new ArrayList<>();
        if (data instanceof VNumberArray) {
            int index = 0;
            IteratorNumber iteratorNumber = ((VNumberArray) data).getData().iterator();
            if (data instanceof VDoubleArray) {
                while (iteratorNumber.hasNext()) {
                    double value = iteratorNumber.nextDouble();
                    ColumnEntry columnEntry = new ColumnEntry(VDouble.of(value, Alarm.none(), Time.now(), Display.none()));
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            } else if (data instanceof VFloatArray) {
                while (iteratorNumber.hasNext()) {
                    float value = iteratorNumber.nextFloat();
                    ColumnEntry columnEntry = new ColumnEntry(VFloat.of(value, Alarm.none(), Time.now(), Display.none()));
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            } else if (data instanceof VIntArray) {
                while (iteratorNumber.hasNext()) {
                    int value = iteratorNumber.nextInt();
                    ColumnEntry columnEntry = new ColumnEntry(VInt.of(value, Alarm.none(), Time.now(), Display.none()));
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            } else if (data instanceof VLongArray) {
                while (iteratorNumber.hasNext()) {
                    long value = iteratorNumber.nextLong();
                    ColumnEntry columnEntry = new ColumnEntry(VLong.of(value, Alarm.none(), Time.now(), Display.none()));
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            } else if (data instanceof VShortArray) {
                while (iteratorNumber.hasNext()) {
                    short value = iteratorNumber.nextShort();
                    ColumnEntry columnEntry = new ColumnEntry(VShort.of(value, Alarm.none(), Time.now(), Display.none()));
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            } else if (data instanceof VByteArray) {
                while (iteratorNumber.hasNext()) {
                    byte value = iteratorNumber.nextByte();
                    ColumnEntry columnEntry = new ColumnEntry(VByte.of(value, Alarm.none(), Time.now(), Display.none()));
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            }
        } else if (data instanceof VBooleanArray) {
            ListBoolean listBoolean = ((VBooleanArray) data).getData();
            for (int i = 0; i < listBoolean.size(); i++) {
                boolean value = listBoolean.getBoolean(i);
                ColumnEntry columnEntry = new ColumnEntry(VBoolean.of(value, Alarm.none(), Time.now()));
                addRow(i, columnEntries, columnEntry);
            }
        } else if (data instanceof VEnumArray) {
            List<String> enumValues = ((VEnumArray) data).getData();
            for (int i = 0; i < enumValues.size(); i++) {
                ColumnEntry columnEntry = new ColumnEntry(VString.of(enumValues.get(i), Alarm.none(), Time.now()));
                addRow(i, columnEntries, columnEntry);
            }
        }

        connect();

    }

    private void addRow(int index, List<ColumnEntry> columnEntries, ColumnEntry columnEntry) {
        columnEntries.add(columnEntry);
        ComparisonData comparisonData = new ComparisonData(index, columnEntries);
        comparisonTable.getItems().add(index, comparisonData);
    }

    public void connect() {
        try {
            PV pv = PVPool.getPV(pvNameProperty.get());
            pv.onValueEvent().throttleLatest(TABLE_UPDATE_INTERVAL, TimeUnit.MILLISECONDS)
                    .subscribe(value -> updateTable(PV.isDisconnected(value) ? VDisconnectedData.INSTANCE : value));
        } catch (Exception e) {
            Logger.getLogger(TableComparisonViewController.class.getName()).log(Level.INFO, "Error connecting to PV", e);
        }
    }

    private void updateTable(VType liveData) {
        if (liveData.equals(VDisconnectedData.INSTANCE)) {
            comparisonTable.getItems().forEach(i -> i.getColumnEntries().get(0).setLiveVal(VDisconnectedData.INSTANCE));
        } else {
            comparisonTable.getItems().forEach(i -> {
                int index = i.indexProperty().get();
                ColumnEntry columnEntry = i.getColumnEntries().get(index);
                if (liveData instanceof VDoubleArray array) {
                    columnEntry.setLiveVal(VDouble.of(array.getData().getDouble(index), Alarm.none(), Time.now(), Display.none()));
                } else if (liveData instanceof VIntArray) {
                    VIntArray array = (VIntArray) liveData;
                    columnEntry.setLiveVal(VInt.of(array.getData().getInt(index), Alarm.none(), Time.now(), Display.none()));
                } else if (liveData instanceof VLongArray) {
                    VLongArray array = (VLongArray) liveData;
                    columnEntry.setLiveVal(VLong.of(array.getData().getLong(index), Alarm.none(), Time.now(), Display.none()));
                } else if (liveData instanceof VFloatArray) {
                    VFloatArray array = (VFloatArray) liveData;
                    columnEntry.setLiveVal(VFloat.of(array.getData().getFloat(index), Alarm.none(), Time.now(), Display.none()));
                } else if (liveData instanceof VShortArray) {
                    VShortArray array = (VShortArray) liveData;
                    columnEntry.setLiveVal(VShort.of(array.getData().getShort(index), Alarm.none(), Time.now(), Display.none()));
                } else if (liveData instanceof VBooleanArray) {
                    VBooleanArray array = (VBooleanArray)liveData;
                    columnEntry.setLiveVal(VBoolean.of(array.getData().getBoolean(index), Alarm.none(), Time.now()));
                } else if (liveData instanceof VEnumArray) {
                    VEnumArray array = (VEnumArray) liveData;
                    i.getColumnEntries().get(index).setLiveVal(VString.of(array.getData().get(index), Alarm.none(), Time.now()));
                } else if (liveData instanceof VStringArray) {
                    VStringArray array = (VStringArray) liveData;
                    i.getColumnEntries().get(index).setLiveVal(VString.of(array.getData().get(index), Alarm.none(), Time.now()));
                }
            });
        }
    }
}
