package org.phoebus.applications.saveandrestore.ui.saveset;

import java.util.EnumSet;

import org.phoebus.applications.saveandrestore.ui.model.EpicsProvider;
import org.phoebus.applications.saveandrestore.ui.model.ObservableSaveSetEntry;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;

/**
 *
 * @author Kunal Shroff
 *
 */
public class SaveSetEditorTable extends TableView<ObservableSaveSetEntry>  {

 
    public SaveSetEditorTable() {
        createTable();
    }

    @SuppressWarnings("unchecked")
    private void createTable() {
        TableColumn<ObservableSaveSetEntry, String> pvName = new TableColumn<>("PV Name");
        pvName.setCellValueFactory(new PropertyValueFactory<>("pvname"));
        pvName.setCellFactory(TextFieldTableCell.forTableColumn());
        pvName.setOnEditCommit((CellEditEvent<ObservableSaveSetEntry, String> t) -> {
            ((ObservableSaveSetEntry) t.getTableView().getItems().get(t.getTablePosition().getRow()))
                    .setPvname(t.getNewValue());
        });
        pvName.prefWidthProperty().bind(this.widthProperty().multiply(0.25));

//        TableColumn<ObservableSaveSetEntry, String> readback = new TableColumn<>("Readback");
//        readback.setCellValueFactory(new PropertyValueFactory<>("readback"));
//        readback.setCellFactory(TextFieldTableCell.forTableColumn());
//        readback.setOnEditCommit((CellEditEvent<ObservableSaveSetEntry, String> t) -> {
//            ((ObservableSaveSetEntry) t.getTableView().getItems().get(t.getTablePosition().getRow()))
//                    .setReadback(t.getNewValue());
//        });
//        readback.prefWidthProperty().bind(this.widthProperty().multiply(0.25));
//
//        TableColumn<ObservableSaveSetEntry, String> delta = new TableColumn<>("Delta");
//        delta.setCellValueFactory(new PropertyValueFactory<>("delta"));
//        delta.setCellFactory(TextFieldTableCell.forTableColumn());
//        delta.setOnEditCommit((CellEditEvent<ObservableSaveSetEntry, String> t) -> {
//            ((ObservableSaveSetEntry) t.getTableView().getItems().get(t.getTablePosition().getRow()))
//                    .setDelta(t.getNewValue());
//        });
//        delta.prefWidthProperty().bind(this.widthProperty().multiply(0.25));
        
        TableColumn<ObservableSaveSetEntry, EpicsProvider> readonly = new TableColumn<>("EPICS provider");
        
        readonly.setCellValueFactory(new PropertyValueFactory<ObservableSaveSetEntry, EpicsProvider>("provider"));

        readonly.setCellFactory((param) -> new RadioButtonCell<ObservableSaveSetEntry, EpicsProvider>(EnumSet.allOf(EpicsProvider.class)));

        readonly.setOnEditCommit(
                new EventHandler<CellEditEvent<ObservableSaveSetEntry, EpicsProvider>>() {
                    @Override
                    public void handle(CellEditEvent<ObservableSaveSetEntry, EpicsProvider> t) {
                        ((ObservableSaveSetEntry) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())).setEpicsProvider(t.getNewValue());
                    }
                }
            );
        readonly.prefWidthProperty().bind(this.widthProperty().multiply(0.25));

        this.getColumns().addAll(pvName, readonly);
        this.setEditable(true);
    }
    
    private static class RadioButtonCell<S,T extends Enum<T>> extends TableCell<S,T>{

        private EnumSet<T> enumeration;

        public RadioButtonCell(EnumSet<T> enumeration) {
            this.enumeration = enumeration;
        }

        @Override
        protected void updateItem(T item, boolean empty)
        {
            super.updateItem(item, empty);
            if (!empty) 
            {
                // gui setup
                HBox hb = new HBox(7);
                hb.setAlignment(Pos.CENTER);
                final ToggleGroup group = new ToggleGroup();

                // create a radio button for each 'element' of the enumeration
                for (Enum<T> enumElement : enumeration) {
                    RadioButton radioButton = new RadioButton(enumElement.toString());
                    radioButton.setUserData(enumElement);
                    radioButton.setToggleGroup(group);
                    hb.getChildren().add(radioButton);
                    if (enumElement.equals(item)) {
                        radioButton.setSelected(true);
                    }
                }

                // issue events on change of the selected radio button
                group.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {

                    @SuppressWarnings("unchecked")
                    @Override
                    public void changed(ObservableValue<? extends Toggle> observable,
                            Toggle oldValue, Toggle newValue) {
                        getTableView().edit(getIndex(), getTableColumn());
                        RadioButtonCell.this.commitEdit((T) newValue.getUserData());
                    }
                });
                setGraphic(hb);
            } 
        }
    }

}
