package org.phoebus.ui.application;

import javafx.collections.ObservableList;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.phoebus.framework.persistence.Memento;

import java.util.function.BiFunction;

public class TableHelper {

    /** Add context menu entries for showing / hiding columns
     * */
    public static boolean addContextMenuColumnVisibilityEntries(final TableView<?> table, final ContextMenu menu) {
        boolean added_item = false;
        Menu visibilitySubMenu = new Menu("Column Visibility");
        for (TableColumn<?, ?> col : table.getColumns()) {
            if (col.getText().isEmpty()) continue;
            CheckMenuItem item = new CheckMenuItem("Show " + col.getText());
            item.selectedProperty().bindBidirectional(col.visibleProperty());
            visibilitySubMenu.getItems().add(item);
            added_item = true;
        }
        if (added_item)
            menu.getItems().add(visibilitySubMenu);
        return added_item;
    }

    /** Save column visibilities to a memento
     * */
    public static <T> void saveColumnVisibilities(
            final TableView<T> table,
            final Memento memento,
            BiFunction<TableColumn<T, ?>, Integer, String> key
    ) {
        ObservableList<TableColumn<T, ?>> columns = table.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            TableColumn<T, ?> col = columns.get(i);
            if (col.getText().isEmpty()) continue;
            String k = key.apply(col, i);
            memento.setBoolean(k, col.isVisible());
        }
    }

    /** Restore column visibilities from a memento
     * */
    public static <T> void restoreColumnVisibilities(
            final TableView<T> table,
            final Memento memento,
            BiFunction<TableColumn<T, ?>, Integer, String> key
    ) {
        ObservableList<TableColumn<T, ?>> columns = table.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            TableColumn<T, ?> col = columns.get(i);
            if (col.getText().isEmpty()) continue;
            String k = key.apply(col, i);
            memento.getBoolean(k).ifPresent(col::setVisible);
        }
    }
}
