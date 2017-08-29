package org.phoebus.applications.pvtable.ui;

import org.phoebus.applications.pvtable.model.PVTableItem;
import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.model.PVTableModelListener;
import org.phoebus.applications.pvtable.model.TimestampHelper;
import org.phoebus.applications.pvtable.model.VTypeHelper;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.util.converter.DefaultStringConverter;

public class PVTable extends BorderPane
{
    private final PVTableModel model;
    private final TableView<PVTableItem> table;
    private boolean editing = false;

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
            if (editing)
            {
                System.out.println("Suppressing updates while editing...");
                return;
            }
            // TODO Replace linear lookup of row w/ member variable in PVTableItem?
            final int row = model.getItems().indexOf(item);
            // System.out.println(item + " changed in row " + row + " on " + Thread.currentThread().getName());
            table.getItems().set(row, item);
        }

        @Override
        public void tableItemsChanged()
        {
            System.out.println("Table items changed");
            table.refresh();
        }

        @Override
        public void modelChanged()
        {
            System.out.println("Model changed");
            table.refresh();
        }
    };

    private static class SelectedTableCell extends TableCell<PVTableItem, Boolean>
    {
        private final CheckBox checkbox = new CheckBox();

        @Override
        protected void updateItem(Boolean selected, boolean empty)
        {
            super.updateItem(selected, empty);
            final int row = getIndex();
            final ObservableList<PVTableItem> items = getTableView().getItems();
            final PVTableItem item = row >= 0 ? items.get(row) : null;
            if (empty  ||  (item != null && item.isComment()))
                setGraphic(null);
            else
            {
                setGraphic(checkbox);
                checkbox.setSelected(selected);
                checkbox.setOnAction(event ->
                {
                    System.out.println("You toggled selection of " + item.getName());
                    item.setSelected(checkbox.isSelected());
                });
            }
        }
    }

    private static class PVNameTableCell extends TextFieldTableCell<PVTableItem, String>
    {
        public PVNameTableCell()
        {
            super(new DefaultStringConverter());
        }

        @Override
        public void updateItem(final String name, final boolean empty)
        {
            super.updateItem(name, empty);
            if (empty)
            {
                setText("");
            }
            else
            {
                final PVTableItem item = getTableView().getItems().get(getIndex());
                if (item.isComment())
                {
                    setStyle("-fx-text-fill: blue;");
                    setText(item.getComment());
                }
                else
                {
                    setStyle(null);
                    setText(name);
                }
            }
        }
    }

    public PVTable(final PVTableModel model)
    {
        this.model = model;
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        // Select complete rows
        table.getSelectionModel().setCellSelectionEnabled(false);

        createTableColumns();

        table.setItems(FXCollections.observableList(model.getItems()));
        table.setEditable(true);

        setTop(new Label("TODO:  TOOLBAR?"));
        setCenter(table);

        model.addListener(model_listener);
    }

    private void createTableColumns()
    {
        // Selected column
        final TableColumn<PVTableItem, Boolean> sel_col = new TableColumn<>(Messages.Selected);
        sel_col.setCellValueFactory(cell_data_features -> new SimpleBooleanProperty(cell_data_features.getValue().isSelected()));
        sel_col.setCellFactory(column -> new SelectedTableCell());
        table.getColumns().add(sel_col);

        // PV Name
        TableColumn<PVTableItem, String> col = new TableColumn<>(Messages.PV);
        col.setPrefWidth(250);
        col.setCellValueFactory(cell_data_features -> new SimpleStringProperty(cell_data_features.getValue().getName()));
        col.setCellFactory(column -> new PVNameTableCell());
        col.setOnEditStart(event -> editing = true);
        col.setOnEditCommit(event ->
        {
            editing = false;
            final PVTableItem item = event.getRowValue();
            System.out.println("Updating " + item.getName() + " to " + event.getNewValue());
            item.updateName(event.getNewValue());
            table.refresh();
        });
        col.setOnEditCancel(event ->
        {
            editing = false;
            table.refresh();
        });
        table.getColumns().add(col);




        // Time Stamp
        col = new TableColumn<>(Messages.Time);
        col.setCellValueFactory(cell ->
        {
            final PVTableItem item = cell.getValue();
            if (item.isComment())
                return new SimpleStringProperty();
            return new SimpleStringProperty(TimestampHelper.format(VTypeHelper.getTimestamp(item.getValue())));
        });
        table.getColumns().add(col);


        // Editable value
        col = new TableColumn<>(Messages.Value);
        col.setCellValueFactory(cell ->
        {
            final PVTableItem item = cell.getValue();
            if (item.isComment())
                return new SimpleStringProperty();
            return new SimpleStringProperty(VTypeHelper.toString(cell.getValue().getValue()));
        });
        table.getColumns().add(col);
    }
}
