/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;


import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.util.converter.DoubleStringConverter;
import org.epics.util.array.ListBoolean;
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
import org.epics.vtype.VUByte;
import org.epics.vtype.VUByteArray;
import org.epics.vtype.VUInt;
import org.epics.vtype.VUIntArray;
import org.epics.vtype.VULong;
import org.epics.vtype.VULongArray;
import org.epics.vtype.VUShort;
import org.epics.vtype.VUShortArray;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.ui.VTypePair;
import org.phoebus.applications.saveandrestore.ui.snapshot.VDeltaCellEditor;
import org.phoebus.applications.saveandrestore.ui.snapshot.VTypeCellEditor;
import org.phoebus.core.vtypes.VDisconnectedData;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.saveandrestore.util.Utilities;
import org.phoebus.saveandrestore.util.VNoData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class for the comparison table view.
 */
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
    private Spinner<Double> thresholdSpinner;

    @SuppressWarnings("unused")
    @FXML
    private Label pvName;

    @SuppressWarnings("unused")
    @FXML
    private Label dimensionStored;

    @SuppressWarnings("unused")
    @FXML
    private Label dimensionLive;

    @SuppressWarnings("unused")
    @FXML
    private Label nonEqualCount;


    private final StringProperty pvNameProperty = new SimpleStringProperty();
    private final StringProperty dimensionStoredProperty = new SimpleStringProperty();
    private final StringProperty dimensionLiveProperty = new SimpleStringProperty();
    private final StringProperty nonEqualCountProperty = new SimpleStringProperty("0");

    private PV pv;

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

        SpinnerValueFactory<Double> thresholdSpinnerValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 999.0, 0.0, 0.01);
        thresholdSpinnerValueFactory.setConverter(new DoubleStringConverter());
        thresholdSpinner.setValueFactory(thresholdSpinnerValueFactory);
        thresholdSpinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
        thresholdSpinner.getEditor().textProperty().addListener((a, o, n) -> parseAndUpdateThreshold(n));

        dimensionLive.textProperty().bind(dimensionLiveProperty);
        dimensionStored.textProperty().bind(dimensionStoredProperty);
        nonEqualCount.textProperty().bind(nonEqualCountProperty);
    }

    /**
     * Loads snapshot data and then connects to the corresponding PV.
     *
     * @param data   Data as stored in a {@link org.phoebus.applications.saveandrestore.model.Snapshot}
     * @param pvName The name of the PV.
     */
    public void loadDataAndConnect(VType data, String pvName) {

        pvNameProperty.set(pvName);

        int arraySize = VTypeHelper.getArraySize(data);
        for (int index = 0; index < arraySize; index++) {
            List<ColumnEntry> columnEntries = new ArrayList<>();
            ColumnEntry columnEntry = null;
            if (data instanceof VNumberArray) {
                if (data instanceof VDoubleArray array) {
                    double value = array.getData().getDouble(index);
                    columnEntry = new ColumnEntry(VDouble.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                } else if (data instanceof VFloatArray array) {
                    float value = array.getData().getFloat(index);
                    columnEntry = new ColumnEntry(VFloat.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                } else if (data instanceof VIntArray array) {
                    int value = array.getData().getInt(index);
                    columnEntry = new ColumnEntry(VInt.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                } else if (data instanceof VUIntArray array) {
                    int value = array.getData().getInt(index);
                    columnEntry = new ColumnEntry(VUInt.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                } else if (data instanceof VLongArray array) {
                    long value = array.getData().getLong(index);
                    columnEntry = new ColumnEntry(VLong.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                } else if (data instanceof VULongArray array) {
                    long value = array.getData().getLong(index);
                    columnEntry = new ColumnEntry(VULong.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                } else if (data instanceof VShortArray array) {
                    short value = array.getData().getShort(index);
                    columnEntry = new ColumnEntry(VShort.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                } else if (data instanceof VUShortArray array) {
                    short value = array.getData().getShort(index);
                    columnEntry = new ColumnEntry(VUShort.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                } else if (data instanceof VByteArray array) {
                    byte value = array.getData().getByte(index);
                    columnEntry = new ColumnEntry(VByte.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                } else if (data instanceof VUByteArray array) {
                    byte value = array.getData().getByte(index);
                    columnEntry = new ColumnEntry(VUByte.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                }
            }
            else if (data instanceof VBooleanArray array) {
                ListBoolean listBoolean = array.getData();
                boolean value = listBoolean.getBoolean(index);
                columnEntry = new ColumnEntry(VBoolean.of(value, array.getAlarm(), array.getTime()));
            } else if (data instanceof VEnumArray array) {
                List<String> enumValues = array.getData();
                columnEntry = new ColumnEntry(VString.of(enumValues.get(index), array.getAlarm(), array.getTime()));
            } else if (data instanceof VStringArray array) {
                List<String> stringValues = array.getData();
                columnEntry = new ColumnEntry(VString.of(stringValues.get(index), array.getAlarm(), array.getTime()));
            }
            if(columnEntry != null){
                addRow(index, columnEntries, columnEntry);
            }
        }

        // Hard coded column count until we support VTable
        dimensionStoredProperty.set(arraySize + " x 1");
        connect();
    }

    private void addRow(int index, List<ColumnEntry> columnEntries, ColumnEntry columnEntry) {
        columnEntries.add(columnEntry);
        ComparisonData comparisonData = new ComparisonData(index, columnEntries);
        comparisonTable.getItems().add(index, comparisonData);
    }

    /**
     * Attempts to connect to the PV.
     */
    private void connect() {
        try {
            pv = PVPool.getPV(pvNameProperty.get());
            pv.onValueEvent().throttleLatest(TABLE_UPDATE_INTERVAL, TimeUnit.MILLISECONDS)
                    .subscribe(value -> updateTable(PV.isDisconnected(value) ? VDisconnectedData.INSTANCE : value));
        } catch (Exception e) {
            Logger.getLogger(TableComparisonViewController.class.getName()).log(Level.INFO, "Error connecting to PV", e);
        }
    }

    /**
     * Returns PV to pool, e.g. when UI is dismissed.
     */
    public void cleanUp() {
        if (pv != null) {
            PVPool.releasePV(pv);
        }
    }

    /**
     * Updates the {@link TableView} from the live data acquired through a PV monitor event.
     * Differences in data sizes between stored and live data is considered.
     *
     * @param liveData EPICS data from the connected PV, or {@link VDisconnectedData#INSTANCE}.
     */
    private void updateTable(VType liveData) {
        if (liveData.equals(VDisconnectedData.INSTANCE)) {
            comparisonTable.getItems().forEach(i -> i.getColumnEntries().get(0).setLiveVal(VDisconnectedData.INSTANCE));
        } else {
            int liveDataArraySize = VTypeHelper.getArraySize(liveData);
            comparisonTable.getItems().forEach(i -> {
                int index = i.indexProperty().get();
                ColumnEntry columnEntry = i.getColumnEntries().get(0);
                if (liveData instanceof VNumberArray) {
                    if (index >= liveDataArraySize) { // Live data has fewer elements than stored data
                        columnEntry.setLiveVal(VNoData.INSTANCE);
                    } else if (liveData instanceof VDoubleArray array) {
                        columnEntry.setLiveVal(VDouble.of(array.getData().getDouble(index), array.getAlarm(), array.getTime(), array.getDisplay()));
                    } else if (liveData instanceof VShortArray array) {
                        columnEntry.setLiveVal(VShort.of(array.getData().getShort(index), array.getAlarm(), array.getTime(), array.getDisplay()));
                    } else if (liveData instanceof VIntArray array) {
                        columnEntry.setLiveVal(VInt.of(array.getData().getInt(index), array.getAlarm(), array.getTime(), array.getDisplay()));
                    } else if (liveData instanceof VUIntArray array) {
                        columnEntry.setLiveVal(VUInt.of(array.getData().getInt(index), array.getAlarm(), array.getTime(), array.getDisplay()));
                    } else if (liveData instanceof VLongArray array) {
                        columnEntry.setLiveVal(VLong.of(array.getData().getLong(index), array.getAlarm(), array.getTime(), array.getDisplay()));
                    } else if (liveData instanceof VULongArray array) {
                        columnEntry.setLiveVal(VULong.of(array.getData().getLong(index), array.getAlarm(), array.getTime(), array.getDisplay()));
                    } else if (liveData instanceof VFloatArray array) {
                        columnEntry.setLiveVal(VFloat.of(array.getData().getFloat(index), array.getAlarm(), array.getTime(), array.getDisplay()));
                    } else if (liveData instanceof VShortArray array) {
                        columnEntry.setLiveVal(VUShort.of(array.getData().getShort(index), array.getAlarm(), array.getTime(), array.getDisplay()));
                    } else if (liveData instanceof VUShortArray array) {
                        columnEntry.setLiveVal(VShort.of(array.getData().getShort(index), array.getAlarm(), array.getTime(), array.getDisplay()));
                    } else if (liveData instanceof VByteArray array) {
                        columnEntry.setLiveVal(VByte.of(array.getData().getShort(index), array.getAlarm(), array.getTime(), array.getDisplay()));
                    } else if (liveData instanceof VUByteArray array) {
                        columnEntry.setLiveVal(VUByte.of(array.getData().getShort(index), array.getAlarm(), array.getTime(), array.getDisplay()));
                    }
                } else if (liveData instanceof VBooleanArray array) {
                    if (index >= array.getData().size()) { // Live data has fewer elements than stored data
                        columnEntry.setLiveVal(VNoData.INSTANCE);
                    } else {
                        columnEntry.setLiveVal(VBoolean.of(array.getData().getBoolean(index), array.getAlarm(), array.getTime()));
                    }
                } else if (liveData instanceof VEnumArray array) {
                    if (index >= array.getData().size()) {  // Live data has fewer elements than stored data
                        columnEntry.setLiveVal(VNoData.INSTANCE);
                    } else {
                        columnEntry.setLiveVal(VString.of(array.getData().get(index), array.getAlarm(), array.getTime()));
                    }
                } else if (liveData instanceof VStringArray array) {
                    if (index >= array.getData().size()) {  // Live data has fewer elements than stored data
                        columnEntry.setLiveVal(VNoData.INSTANCE);
                    } else {
                        columnEntry.setLiveVal(VString.of(array.getData().get(index), array.getAlarm(), array.getTime()));
                    }
                }
            });
            // Live data may have more elements than stored data
            if (liveDataArraySize > comparisonTable.getItems().size()) {
                List<ColumnEntry> columnEntries = new ArrayList<>();
                for (int index = comparisonTable.getItems().size(); index < liveDataArraySize; index++) {
                    ColumnEntry columnEntry = new ColumnEntry(VNoData.INSTANCE);
                    if (liveData instanceof VNumberArray) {
                        if (liveData instanceof VDoubleArray array) {
                            double value = array.getData().getDouble(index);
                            columnEntry.setLiveVal(VDouble.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                        } else if (liveData instanceof VFloatArray array) {
                            float value = array.getData().getFloat(index);
                            columnEntry.setLiveVal(VFloat.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                        } else if (liveData instanceof VIntArray array) {
                            int value = array.getData().getInt(index);
                            columnEntry.setLiveVal(VInt.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                        } else if (liveData instanceof VUIntArray array) {
                            int value = array.getData().getInt(index);
                            columnEntry.setLiveVal(VUInt.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                        } else if (liveData instanceof VLongArray array) {
                            long value = array.getData().getLong(index);
                            columnEntry.setLiveVal(VLong.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                        } else if (liveData instanceof VULongArray array) {
                            long value = array.getData().getLong(index);
                            columnEntry.setLiveVal(VULong.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                        } else if (liveData instanceof VShortArray array) {
                            short value = array.getData().getShort(index);
                            columnEntry.setLiveVal(VShort.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                        } else if (liveData instanceof VUShortArray array) {
                            short value = array.getData().getShort(index);
                            columnEntry.setLiveVal(VUShort.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                        } else if (liveData instanceof VByteArray array) {
                            byte value = array.getData().getByte(index);
                            columnEntry.setLiveVal(VByte.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                        } else if (liveData instanceof VUByteArray array) {
                            byte value = array.getData().getByte(index);
                            columnEntry.setLiveVal(VUByte.of(value, array.getAlarm(), array.getTime(), array.getDisplay()));
                        }
                    }
                    else if (liveData instanceof VBooleanArray array) {
                        ListBoolean listBoolean = array.getData();
                        boolean value = listBoolean.getBoolean(index);
                        columnEntry.setLiveVal(VBoolean.of(value, array.getAlarm(), array.getTime()));
                    } else if (liveData instanceof VEnumArray array) {
                        List<String> enumValues = array.getData();
                        columnEntry.setLiveVal(VString.of(enumValues.get(index), array.getAlarm(), array.getTime()));
                    } else if (liveData instanceof VStringArray array) {
                        List<String> stringValues = array.getData();
                        columnEntry.setLiveVal(VString.of(stringValues.get(index), array.getAlarm(), array.getTime()));
                    }
                    addRow(index, columnEntries, columnEntry);
                }
            }
            // Hard coded column count until we support VTable
            dimensionLiveProperty.set(liveDataArraySize + " x 1");
            computeNonEqualCount();
        }

    }

    private void parseAndUpdateThreshold(String value) {
        thresholdSpinner.getEditor().getStyleClass().remove("input-error");
        thresholdSpinner.setTooltip(null);
        try {
            double parsedNumber = Double.parseDouble(value.trim());
            updateThreshold(parsedNumber);
        } catch (Exception e) {
            thresholdSpinner.getEditor().getStyleClass().add("input-error");
            thresholdSpinner.setTooltip(new Tooltip(Messages.toolTipMultiplierSpinner));
        }
    }

    /**
     * Computes thresholds on the individual elements. The threshold is used to indicate that a delta value within threshold
     * should not decorate the delta column.
     *
     * @param threshold Threshold in percent
     */
    private void updateThreshold(double threshold) {
        double ratio = threshold / 100;

        comparisonTable.getItems().forEach(comparisonData -> {
            comparisonData.setThreshold(ratio);
        });

        computeNonEqualCount();
    }

    private void computeNonEqualCount(){
        AtomicInteger nonEqualCount = new AtomicInteger(0);
        comparisonTable.getItems().forEach(comparisonData -> {
            comparisonData.getColumnEntries().forEach(columnEntry -> {
                if(!Utilities.areValuesEqual(columnEntry.liveValueProperty().get(), columnEntry.storedValueProperty().get(), columnEntry.getDelta().get().threshold)){
                    nonEqualCount.incrementAndGet();
                }
            });
        });

        nonEqualCountProperty.set(nonEqualCount.toString());
    }
}
