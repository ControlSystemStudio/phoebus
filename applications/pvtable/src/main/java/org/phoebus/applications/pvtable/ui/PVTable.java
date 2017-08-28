package org.phoebus.applications.pvtable.ui;

import org.phoebus.applications.pvtable.model.PVTableItem;
import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.model.PVTableModelListener;
import org.phoebus.applications.pvtable.model.TimestampHelper;
import org.phoebus.applications.pvtable.model.VTypeHelper;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

public class PVTable extends BorderPane
{
    private final TableView<PVTableItem> table;

    private final PVTableModelListener model_listener = new PVTableModelListener()
    {
        @Override
        public void tableItemSelectionChanged(PVTableItem item)
        {
            // TODO Make default
        }

        @Override
        public void tableItemChanged(PVTableItem item)
        {
            // TODO Auto-generated method stub
            System.out.println(item + " changed");
            table.refresh();
        }

        @Override
        public void tableItemsChanged()
        {
            // TODO Auto-generated method stub
            System.out.println("Table items changed");
        }

        @Override
        public void modelChanged()
        {
            // TODO Auto-generated method stub
            System.out.println("Model changed");
        }
    };

    public PVTable(final PVTableModel model)
    {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        // Select complete rows
        table.getSelectionModel().setCellSelectionEnabled(false);

        createTableColumns();

        table.setItems(FXCollections.observableList(model.getItems()));

        setTop(new Label("TODO:  TOOLBAR?"));
        setCenter(table);

        model.addListener(model_listener);
    }

    private void createTableColumns()
    {
        // PV Name
        TableColumn<PVTableItem, String> col = new TableColumn<>(Messages.PV);
        col.setCellValueFactory(cell_data_features -> new SimpleStringProperty(cell_data_features.getValue().getName()));
        table.getColumns().add(col);


        // Time Stamp
        col = new TableColumn<>(Messages.Time);
        col.setCellValueFactory(cell_data_features -> new SimpleStringProperty(TimestampHelper.format(VTypeHelper.getTimestamp(cell_data_features.getValue().getValue()))));
        table.getColumns().add(col);


        // Editable value
        col = new TableColumn<>(Messages.Value);
        col.setCellValueFactory(cell_data_features -> new SimpleStringProperty(VTypeHelper.toString(cell_data_features.getValue().getValue())));
        table.getColumns().add(col);
    }
}
