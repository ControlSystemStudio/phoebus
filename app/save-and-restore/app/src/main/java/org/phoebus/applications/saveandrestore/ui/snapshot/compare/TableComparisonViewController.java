/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
import org.phoebus.saveandrestore.util.VNoData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
                cell.getValue().getColumnEntries().get(0).storedValueProperty());
        storedValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        liveValueColumn.setCellValueFactory(cell ->
                cell.getValue().getColumnEntries().get(0).liveValueProperty());
        liveValueColumn.setCellFactory(e -> new VTypeCellEditor<>());
        deltaColumn.setCellValueFactory(cell ->
                cell.getValue().getColumnEntries().get(0).getDelta());
        deltaColumn.setComparator(Comparator.comparingDouble(VTypePair::getAbsoluteDelta));

        deltaColumn.setCellFactory(e -> new VDeltaCellEditor<>());
    }

    public void loadDataAndConnect(VType data, String pvName) {

        pvNameProperty.set(pvName);

        if (data instanceof VNumberArray) {
            int arraySize = ((VNumberArray) data).getData().size();
            for (int index = 0; index < arraySize; index++) {
                List<ColumnEntry> columnEntries = new ArrayList<>();
                if (data instanceof VDoubleArray) {
                    double value = ((VDoubleArray) data).getData().getDouble(index);
                    ColumnEntry columnEntry = new ColumnEntry(VDouble.of(value, Alarm.none(), Time.now(), Display.none()));
                    columnEntries.add(columnEntry);
                    ComparisonData comparisonData = new ComparisonData(index, columnEntries);
                    comparisonTable.getItems().add(index, comparisonData);
                } else if (data instanceof VFloatArray) {
                    float value = ((VFloatArray) data).getData().getFloat(index);
                    ColumnEntry columnEntry = new ColumnEntry(VFloat.of(value, Alarm.none(), Time.now(), Display.none()));
                    columnEntries.add(columnEntry);
                    ComparisonData comparisonData = new ComparisonData(index, columnEntries);
                    comparisonTable.getItems().add(index, comparisonData);
                } else if (data instanceof VIntArray) {
                    int value = ((VIntArray) data).getData().getInt(index);
                    ColumnEntry columnEntry = new ColumnEntry(VInt.of(value, Alarm.none(), Time.now(), Display.none()));
                    columnEntries.add(columnEntry);
                    ComparisonData comparisonData = new ComparisonData(index, columnEntries);
                    comparisonTable.getItems().add(index, comparisonData);
                } else if (data instanceof VLongArray) {
                    long value = ((VLongArray) data).getData().getLong(index);
                    ColumnEntry columnEntry = new ColumnEntry(VLong.of(value, Alarm.none(), Time.now(), Display.none()));
                    columnEntries.add(columnEntry);
                    ComparisonData comparisonData = new ComparisonData(index, columnEntries);
                    comparisonTable.getItems().add(index, comparisonData);
                } else if (data instanceof VShortArray) {
                    short value = ((VShortArray) data).getData().getShort(index);
                    ColumnEntry columnEntry = new ColumnEntry(VShort.of(value, Alarm.none(), Time.now(), Display.none()));
                    columnEntries.add(columnEntry);
                    ComparisonData comparisonData = new ComparisonData(index, columnEntries);
                    comparisonTable.getItems().add(index, comparisonData);
                } else if (data instanceof VByteArray) {
                    byte value = ((VByteArray) data).getData().getByte(index);
                    ColumnEntry columnEntry = new ColumnEntry(VByte.of(value, Alarm.none(), Time.now(), Display.none()));
                    columnEntries.add(columnEntry);
                    ComparisonData comparisonData = new ComparisonData(index, columnEntries);
                    comparisonTable.getItems().add(index, comparisonData);
                }
            }
        } else if (data instanceof VBooleanArray) {
            ListBoolean listBoolean = ((VBooleanArray) data).getData();
            for (int index = 0; index < listBoolean.size(); index++) {
                List<ColumnEntry> columnEntries = new ArrayList<>();
                boolean value = listBoolean.getBoolean(index);
                ColumnEntry columnEntry = new ColumnEntry(VBoolean.of(value, Alarm.none(), Time.now()));
                columnEntries.add(columnEntry);
                ComparisonData comparisonData = new ComparisonData(index, columnEntries);
                comparisonTable.getItems().add(index, comparisonData);
            }
        } else if (data instanceof VEnumArray) {
            List<String> enumValues = ((VEnumArray) data).getData();
            for (int index = 0; index < enumValues.size(); index++) {
                List<ColumnEntry> columnEntries = new ArrayList<>();
                ColumnEntry columnEntry = new ColumnEntry(VString.of(enumValues.get(index), Alarm.none(), Time.now()));
                columnEntries.add(columnEntry);
                ComparisonData comparisonData = new ComparisonData(index, columnEntries);
                comparisonTable.getItems().add(index, comparisonData);
            }
        } else if (data instanceof VStringArray) {
            List<String> stringValues = ((VStringArray) data).getData();
            for (int index = 0; index < stringValues.size(); index++) {
                List<ColumnEntry> columnEntries = new ArrayList<>();
                ColumnEntry columnEntry = new ColumnEntry(VString.of(stringValues.get(index), Alarm.none(), Time.now()));
                columnEntries.add(columnEntry);
                ComparisonData comparisonData = new ComparisonData(index, columnEntries);
                comparisonTable.getItems().add(index, comparisonData);
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
            AtomicInteger liveDataArraySize = new AtomicInteger(0);
            comparisonTable.getItems().forEach(i -> {
                int index = i.indexProperty().get();
                ColumnEntry columnEntry = i.getColumnEntries().get(0);
                if (liveData instanceof VNumberArray) {
                    liveDataArraySize.set(((VNumberArray) liveData).getData().size());
                    if (index >= liveDataArraySize.get()) { // Live data has fewer elements than stored data
                        columnEntry.setLiveVal(VNoData.INSTANCE);
                    } else if (liveData instanceof VDoubleArray array) {
                        columnEntry.setLiveVal(VDouble.of(array.getData().getDouble(index), Alarm.none(), Time.now(), Display.none()));
                    } else if (liveData instanceof VIntArray array) {
                        columnEntry.setLiveVal(VInt.of(array.getData().getInt(index), Alarm.none(), Time.now(), Display.none()));
                    } else if (liveData instanceof VLongArray array) {
                        columnEntry.setLiveVal(VLong.of(array.getData().getLong(index), Alarm.none(), Time.now(), Display.none()));
                    } else if (liveData instanceof VFloatArray array) {
                        columnEntry.setLiveVal(VFloat.of(array.getData().getFloat(index), Alarm.none(), Time.now(), Display.none()));
                    } else if (liveData instanceof VShortArray array) {
                        columnEntry.setLiveVal(VShort.of(array.getData().getShort(index), Alarm.none(), Time.now(), Display.none()));
                    }
                } else if (liveData instanceof VBooleanArray array) {
                    liveDataArraySize.set(array.getData().size());
                    if (index >= array.getData().size()) { // Live data has fewer elements than stored data
                        columnEntry.setLiveVal(VNoData.INSTANCE);
                    } else {
                        columnEntry.setLiveVal(VBoolean.of(array.getData().getBoolean(index), Alarm.none(), Time.now()));
                    }

                } else if (liveData instanceof VEnumArray array) {
                    liveDataArraySize.set(array.getData().size());
                    if (index >= array.getData().size()) {  // Live data has fewer elements than stored data
                        columnEntry.setLiveVal(VNoData.INSTANCE);
                    } else {
                        i.getColumnEntries().get(index).setLiveVal(VString.of(array.getData().get(index), Alarm.none(), Time.now()));
                    }
                } else if (liveData instanceof VStringArray array) {
                    liveDataArraySize.set(array.getData().size());
                    if (index >= array.getData().size()) {  // Live data has fewer elements than stored data
                        columnEntry.setLiveVal(VNoData.INSTANCE);
                    } else {
                        i.getColumnEntries().get(index).setLiveVal(VString.of(array.getData().get(index), Alarm.none(), Time.now()));
                    }
                }
            });
            // Live data may have more elements than stored data
            if (liveDataArraySize.get() > comparisonTable.getItems().size()) {
                List<ColumnEntry> columnEntries = new ArrayList<>();
                if (liveData instanceof VNumberArray) {
                    if (liveData instanceof VDoubleArray) {
                        for (int index = comparisonTable.getItems().size(); index < liveDataArraySize.get(); index++) {
                            double value = ((VDoubleArray) liveData).getData().getDouble(index);
                            ColumnEntry columnEntry = new ColumnEntry(VNoData.INSTANCE);
                            columnEntry.setLiveVal(VDouble.of(value, Alarm.none(), Time.now(), Display.none()));
                            addRow(index, columnEntries, columnEntry);
                        }
                    } else if (liveData instanceof VFloatArray) {
                        for (int index = comparisonTable.getItems().size(); index < liveDataArraySize.get(); index++) {
                            float value = ((VFloatArray) liveData).getData().getFloat(index);
                            ColumnEntry columnEntry = new ColumnEntry(VNoData.INSTANCE);
                            columnEntry.setLiveVal(VFloat.of(value, Alarm.none(), Time.now(), Display.none()));
                            addRow(index, columnEntries, columnEntry);
                        }
                    } else if (liveData instanceof VIntArray) {
                        for (int index = comparisonTable.getItems().size(); index < liveDataArraySize.get(); index++) {
                            int value = ((VIntArray) liveData).getData().getInt(index);
                            ColumnEntry columnEntry = new ColumnEntry(VNoData.INSTANCE);
                            columnEntry.setLiveVal(VInt.of(value, Alarm.none(), Time.now(), Display.none()));
                            addRow(index, columnEntries, columnEntry);
                        }
                    } else if (liveData instanceof VLongArray) {
                        for (int index = comparisonTable.getItems().size(); index < liveDataArraySize.get(); index++) {
                            long value = ((VLongArray) liveData).getData().getLong(index);
                            ColumnEntry columnEntry = new ColumnEntry(VNoData.INSTANCE);
                            columnEntry.setLiveVal(VLong.of(value, Alarm.none(), Time.now(), Display.none()));
                            addRow(index, columnEntries, columnEntry);
                        }
                    } else if (liveData instanceof VShortArray) {
                        for (int index = comparisonTable.getItems().size(); index < liveDataArraySize.get(); index++) {
                            short value = ((VShortArray) liveData).getData().getShort(index);
                            ColumnEntry columnEntry = new ColumnEntry(VNoData.INSTANCE);
                            columnEntry.setLiveVal(VShort.of(value, Alarm.none(), Time.now(), Display.none()));
                            addRow(index, columnEntries, columnEntry);
                        }
                    } else if (liveData instanceof VByteArray) {
                        for (int index = comparisonTable.getItems().size(); index < liveDataArraySize.get(); index++) {
                            byte value = ((VByteArray) liveData).getData().getByte(index);
                            ColumnEntry columnEntry = new ColumnEntry(VNoData.INSTANCE);
                            columnEntry.setLiveVal(VByte.of(value, Alarm.none(), Time.now(), Display.none()));
                            addRow(index, columnEntries, columnEntry);
                        }
                    }
                } else if (liveData instanceof VBooleanArray) {
                    ListBoolean listBoolean = ((VBooleanArray) liveData).getData();
                    for (int i = 0; i < listBoolean.size(); i++) {
                        boolean value = listBoolean.getBoolean(i);
                        ColumnEntry columnEntry = new ColumnEntry(VNoData.INSTANCE);
                        columnEntry.setLiveVal(VBoolean.of(value, Alarm.none(), Time.now()));
                        addRow(i, columnEntries, columnEntry);
                    }
                } else if (liveData instanceof VEnumArray) {
                    List<String> enumValues = ((VEnumArray) liveData).getData();
                    for (int i = 0; i < enumValues.size(); i++) {
                        ColumnEntry columnEntry = new ColumnEntry(VNoData.INSTANCE);
                        columnEntry.setLiveVal(VString.of(enumValues.get(i), Alarm.none(), Time.now()));
                        addRow(i, columnEntries, columnEntry);
                    }
                } else if (liveData instanceof VStringArray) {
                    List<String> stringValues = ((VStringArray) liveData).getData();
                    for (int i = 0; i < stringValues.size(); i++) {
                        ColumnEntry columnEntry = new ColumnEntry(VNoData.INSTANCE);
                        columnEntry.setLiveVal(VString.of(stringValues.get(i), Alarm.none(), Time.now()));
                        addRow(i, columnEntries, columnEntry);
                    }
                }
            }
        }
    }
}
