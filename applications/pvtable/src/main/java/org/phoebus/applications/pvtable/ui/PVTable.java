package org.phoebus.applications.pvtable.ui;

import org.diirt.vtype.VType;
import org.phoebus.applications.pvtable.PVTableApplication;
import org.phoebus.applications.pvtable.model.PVTableItem;
import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.model.PVTableModelListener;
import org.phoebus.applications.pvtable.model.SavedValue;
import org.phoebus.applications.pvtable.model.TimestampHelper;
import org.phoebus.applications.pvtable.model.VTypeHelper;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.util.converter.DefaultStringConverter;

public class PVTable extends BorderPane
{
    private final PVTableModel model;
    private final TableView<PVTableItem> table;

    /** Flag to disable updates while editing */
    private boolean editing = false;

    /** Table cell for 'selected' column, selects/de-selects */
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
                checkbox.setOnAction(event -> item.setSelected(checkbox.isSelected()));
            }
        }
    }

    /** Table cell for 'name' column, colors comments */
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

    private static final String[] alarm_styles = new String[]
    {
        null,
        "-fx-text-fill: orange;",
        "-fx-text-fill: red;",
        "-fx-text-fill: purple;",
        "-fx-text-fill: pink;",
    };

    /** Table cell for 'alarm' column, colors alarm states */
    private static class AlarmTableCell extends TextFieldTableCell<PVTableItem, String>
    {
        public AlarmTableCell()
        {
            super(new DefaultStringConverter());
        }

        @Override
        public void updateItem(final String alarm_text, final boolean empty)
        {
            super.updateItem(alarm_text, empty);
            if (empty)
            {
                setText("");
            }
            else
            {
                final PVTableItem item = getTableView().getItems().get(getIndex());
                final VType value = item.getValue();
                if (value == null)
                    setText("");
                else
                {
                    setText(alarm_text);
                    setStyle(alarm_styles[VTypeHelper.getSeverity(value).ordinal()]);
                }
            }
        }
    }

    /** Listener to model changes */
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
            // In principle, just suppressing updates to the single row
            // that's being edited should be sufficient,
            // but JavaFX seems to update arbitrary rows beyond the requested
            // one, so suppress all updates while editing
            if (editing)
                return;

            // XXX Replace linear lookup of row w/ member variable in PVTableItem?
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

        setTop(createToolbar());
        setCenter(table);

        model.addListener(model_listener);
    }

    private Node createToolbar()
    {
        final ButtonBar toolbar = new ButtonBar();
        toolbar.setButtonMinWidth(50);

        toolbar.getButtons().addAll(
                createButton("snapshot.png", Messages.Snapshot_TT, event -> model.save()),
                createButton("restore.png", Messages.Restore_TT, event -> model.restore()));

        return toolbar;
    }

    private Button createButton(final String icon, final String tooltip, final EventHandler<ActionEvent> handler)
    {
        final Button button = new Button();
        button.setGraphic(new ImageView(PVTableApplication.getIcon(icon)));
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(handler);
        ButtonBar.setButtonData(button, ButtonData.LEFT);
        return button;
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
            item.updateName(event.getNewValue());
            // Since editing was suppressed, refresh table
            table.refresh();
        });
        col.setOnEditCancel(event ->
        {
            editing = false;
            // Since editing was suppressed, refresh table
            table.refresh();
        });
        table.getColumns().add(col);

        // Description
        col = new TableColumn<>(Messages.Description);
        col.setCellValueFactory(cell ->
        {
            final PVTableItem item = cell.getValue();
            if (item.isComment())
                return new SimpleStringProperty();
            return new SimpleStringProperty(item.getDescription());
        });
        table.getColumns().add(col);

        // Time Stamp
        col = new TableColumn<>(Messages.Time);
        col.setCellValueFactory(cell ->
        {
            final VType value = cell.getValue().getValue();
            if (value == null)
                return new SimpleStringProperty();
            return new SimpleStringProperty(TimestampHelper.format(VTypeHelper.getTimestamp(value)));
        });
        table.getColumns().add(col);

        // Editable value
        col = new TableColumn<>(Messages.Value);
        col.setCellValueFactory(cell ->
        {
            final VType value = cell.getValue().getValue();
            if (value == null)
                return new SimpleStringProperty();
            return new SimpleStringProperty(VTypeHelper.toString(value));
        });
        // TODO Edit value
        table.getColumns().add(col);

        // Alarm
        col = new TableColumn<>(Messages.Alarm);
        col.setCellValueFactory(cell ->
        {
            final VType value = cell.getValue().getValue();
            if (value == null)
                return new SimpleStringProperty();
            return new SimpleStringProperty(VTypeHelper.formatAlarm(value));
        });
        col.setCellFactory(column -> new AlarmTableCell());
        table.getColumns().add(col);

        // Saved value
        col = new TableColumn<>(Messages.Saved);
        col.setCellValueFactory(cell ->
        {
            final SavedValue value = cell.getValue().getSavedValue().orElse(null);
            if (value == null)
                return new SimpleStringProperty();
            return new SimpleStringProperty(value.toString());
        });
        table.getColumns().add(col);

        // Saved value's timestamp
        col = new TableColumn<>(Messages.Saved_Value_TimeStamp);
        col.setCellValueFactory(cell ->
        {
            final PVTableItem item = cell.getValue();
            return new SimpleStringProperty(item.getTime_saved());
        });
        table.getColumns().add(col);

        // TODO Completion checkbox
    }
}
