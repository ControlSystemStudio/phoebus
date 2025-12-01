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
import org.epics.util.array.IteratorNumber;
import org.epics.util.array.ListBoolean;
import org.epics.vtype.VBooleanArray;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VDisconnectedData;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import java.util.ArrayList;
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
    private TableColumn<ComparisonData, ?> valueColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<ComparisonData, ?> liveValueColumn;

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
        valueColumn.setCellValueFactory(cell ->
                cell.getValue().getColumnEntries().get(cell.getValue().indexProperty().get()).getSnapshotValue());
        liveValueColumn.setCellValueFactory(cell ->
                cell.getValue().getColumnEntries().get(cell.getValue().indexProperty().get()).getLiveValue());
    }

    public void loadDataAndConnect(VType data, String pvName) {

        pvNameProperty.set(pvName);

        if (data instanceof VNumberArray) {
            IteratorNumber iteratorNumber = ((VNumberArray) data).getData().iterator();
            List<ColumnEntry> columnEntries = new ArrayList<>();
            int index = 0;
            if (data instanceof VDoubleArray) {
                while (iteratorNumber.hasNext()) {
                    double value = iteratorNumber.nextDouble();
                    ColumnEntry<Double> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            } else if (data instanceof VFloatArray) {
                while (iteratorNumber.hasNext()) {
                    float value = iteratorNumber.nextFloat();
                    ColumnEntry<Float> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            } else if (data instanceof VIntArray) {
                while (iteratorNumber.hasNext()) {
                    int value = iteratorNumber.nextInt();
                    ColumnEntry<Integer> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            } else if (data instanceof VLongArray) {
                while (iteratorNumber.hasNext()) {
                    long value = iteratorNumber.nextLong();
                    ColumnEntry<Long> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            } else if (data instanceof VShortArray) {
                while (iteratorNumber.hasNext()) {
                    short value = iteratorNumber.nextShort();
                    ColumnEntry<Short> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            } else if (data instanceof VByteArray) {
                while (iteratorNumber.hasNext()) {
                    byte value = iteratorNumber.nextByte();
                    ColumnEntry<Byte> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            } else if (data instanceof VBooleanArray) {
                ListBoolean listBoolean = ((VBooleanArray) data).getData();
                for (int i = 0; i < listBoolean.size(); i++) {
                    boolean value = listBoolean.getBoolean(i);
                    ColumnEntry<Boolean> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                }
            } else if (data instanceof VEnumArray) {
                List<String> enumValues = ((VEnumArray) data).getData();
                for (int i = 0; i < enumValues.size(); i++) {
                    ColumnEntry<String> columnEntry = new ColumnEntry<>(enumValues.get(i));
                    addRow(index, columnEntries, columnEntry);
                }
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
                if (liveData instanceof VDoubleArray) {
                    VDoubleArray array = (VDoubleArray) liveData;
                    i.getColumnEntries().get(index).setLiveVal(array.getData().getDouble(index));
                } else if (liveData instanceof VIntArray) {
                    VIntArray array = (VIntArray) liveData;
                    i.getColumnEntries().get(index).setLiveVal(array.getData().getInt(index));
                } else if (liveData instanceof VLongArray) {
                    VLongArray array = (VLongArray) liveData;
                    i.getColumnEntries().get(index).setLiveVal(array.getData().getLong(index));
                } else if (liveData instanceof VFloatArray) {
                    VFloatArray array = (VFloatArray) liveData;
                    i.getColumnEntries().get(index).setLiveVal(array.getData().getFloat(index));
                } else if (liveData instanceof VShortArray) {
                    VShortArray array = (VShortArray) liveData;
                    i.getColumnEntries().get(index).setLiveVal(array.getData().getShort(index));
                } else if (liveData instanceof VBooleanArray) {
                    VBooleanArray array = (VBooleanArray)liveData;
                    i.getColumnEntries().get(index).setLiveVal(array.getData().getBoolean(index));
                } else if (liveData instanceof VEnumArray) {
                    VEnumArray array = (VEnumArray) liveData;
                    i.getColumnEntries().get(index).setLiveVal(array.getData().get(index));
                } else if (liveData instanceof VStringArray) {
                    VStringArray array = (VStringArray) liveData;
                    i.getColumnEntries().get(index).setLiveVal(array.getData().get(index));
                }
            });
        }
    }
}
