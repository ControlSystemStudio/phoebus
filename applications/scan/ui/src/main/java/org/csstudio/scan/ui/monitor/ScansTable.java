package org.csstudio.scan.ui.monitor;

import java.util.List;

import org.csstudio.scan.info.ScanInfo;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class ScansTable extends VBox
{
    private static class ScanInfoProxy
    {
        SimpleStringProperty id = new SimpleStringProperty();
        SimpleStringProperty name = new SimpleStringProperty();

        public ScanInfoProxy(final ScanInfo info)
        {
            id = new SimpleStringProperty(Long.toString(info.getId()));
            name = new SimpleStringProperty(info.getName());
        }

        void updateFrom(final ScanInfo info)
        {
            id.set(Long.toString(info.getId()));
            name.set(info.getName());
        }
    };

    private final TableView<ScanInfoProxy> scan_table = new TableView<>();

    public ScansTable()
    {
        createTable();
        getChildren().add(scan_table);
    }

    private void createTable()
    {
        // TODO Auto-generated method stub
        TableColumn<ScanInfoProxy, String> col = new TableColumn<>("ID");
        col.setCellValueFactory(cell -> cell.getValue().id);
        scan_table.getColumns().add(col);

        col = new TableColumn<>("Created");
        scan_table.getColumns().add(col);

        col = new TableColumn<>("Name");
        col.setCellValueFactory(cell -> cell.getValue().name);
        scan_table.getColumns().add(col);

        col = new TableColumn<>("State");
        scan_table.getColumns().add(col);

        col = new TableColumn<>("%");
        scan_table.getColumns().add(col);

        col = new TableColumn<>("Runtime");
        scan_table.getColumns().add(col);

        col = new TableColumn<>("Finish");
        scan_table.getColumns().add(col);

        col = new TableColumn<>("Command");
        scan_table.getColumns().add(col);

        col = new TableColumn<>("Error");
        scan_table.getColumns().add(col);

        scan_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public void update(List<ScanInfo> infos)
    {
        final ObservableList<ScanInfoProxy> items = scan_table.getItems();
        int i;
        for (i=0; i<infos.size(); ++i)
        {
            if (i < items.size())
                items.get(i).updateFrom(infos.get(i));
            else
                items.add(new ScanInfoProxy(infos.get(i)));
        }
        i = items.size();
        while (i > infos.size())
            items.remove(--i);
    }
}
