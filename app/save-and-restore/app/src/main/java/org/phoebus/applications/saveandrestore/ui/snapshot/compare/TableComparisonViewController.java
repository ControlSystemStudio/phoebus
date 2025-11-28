/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.snapshot.compare;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.epics.util.array.IteratorNumber;
import org.epics.util.array.ListBoolean;
import org.epics.vtype.VBooleanArray;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VType;

import java.util.ArrayList;
import java.util.List;

public class TableComparisonViewController {

    @SuppressWarnings("unused")
    @FXML
    private TableView<ComparisonData> comparisonTable;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<ComparisonData, ?> valueColumn;

    @FXML
    public void initialize(){
        comparisonTable.getStylesheets().add(TableComparisonViewController.class.getResource("/save-and-restore-style.css").toExternalForm());

        valueColumn.setCellValueFactory(cell ->
            cell.getValue().getColumnEntries().get(cell.getValue().indexProperty().get()).getSnapshotValue());
    }

    public void loadDataAndConnect(VType data, String pvName){

        if(data instanceof VNumberArray){
            IteratorNumber iteratorNumber = ((VNumberArray)data).getData().iterator();
            List<ColumnEntry> columnEntries = new ArrayList<>();
            int index = 0;
            if(data instanceof VDoubleArray){
                while(iteratorNumber.hasNext()){
                    double value = iteratorNumber.nextDouble();
                    ColumnEntry<Double> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            }
            else if(data instanceof VFloatArray){
                while(iteratorNumber.hasNext()){
                    float value = iteratorNumber.nextFloat();
                    ColumnEntry<Float> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            }
            else if(data instanceof VIntArray){
                while(iteratorNumber.hasNext()){
                    int value = iteratorNumber.nextInt();
                    ColumnEntry<Integer> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            }
            else if(data instanceof VLongArray){
                while(iteratorNumber.hasNext()){
                    long value = iteratorNumber.nextLong();
                    ColumnEntry<Long> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            }
            else if(data instanceof VShortArray){
                while(iteratorNumber.hasNext()){
                    short value = iteratorNumber.nextShort();
                    ColumnEntry<Short> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            }
            else if(data instanceof VByteArray){
                while(iteratorNumber.hasNext()){
                    byte value = iteratorNumber.nextByte();
                    ColumnEntry<Byte> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                    index++;
                }
            }
            else if(data instanceof VBooleanArray){
                ListBoolean listBoolean = ((VBooleanArray)data).getData();
                for(int i = 0; i < listBoolean.size(); i++){
                    boolean value = listBoolean.getBoolean(i);
                    ColumnEntry<Boolean> columnEntry = new ColumnEntry<>(value);
                    addRow(index, columnEntries, columnEntry);
                }
            }
        }

    }

    private void addRow(int index, List<ColumnEntry> columnEntries, ColumnEntry columnEntry){
        columnEntries.add(columnEntry);
        ComparisonData comparisonData = new ComparisonData(index, columnEntries);
        comparisonTable.getItems().add(index, comparisonData);
    }


}
