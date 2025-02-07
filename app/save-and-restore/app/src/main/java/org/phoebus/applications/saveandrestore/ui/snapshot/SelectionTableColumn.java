/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.applications.saveandrestore.ui.snapshot;


import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.MouseButton;
import org.phoebus.saveandrestore.util.VNoData;

/**
 * <code>SelectionTableColumn</code> is the table column for the first column in the table, which displays
 * a checkbox, whether the PV should be selected or not.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 */
public class SelectionTableColumn extends TooltipTableColumn<Boolean> {

    private final CheckBox selectAllCheckBox = new CheckBox();

    /**
     * Needed by fxml
     */
    @SuppressWarnings("unused")
    public SelectionTableColumn() {

    }

    @Override
    public void setTooltip(String tooltip) {
        selectAllCheckBox.setTooltip(new Tooltip(tooltip));
    }

    @Override
    public String getTooltip() {
        return selectAllCheckBox.getTooltip().textProperty().get();
    }

    public SelectionTableColumn(TableView<TableEntry> tableView) {
        super("", "Include this PV when restoring values", 30, 30, false);
        configure(tableView);
    }

    public void configure(TableView<TableEntry> tableView) {
        //for those entries, which have a read-only property, disable the checkbox
        setCellFactory(column -> {
            TableCell<TableEntry, Boolean> cell = new CheckBoxTableCell<>(null, null);
            // initialize the checkbox
            updateCheckboxState(cell);
            cell.itemProperty().addListener((a, o, n) -> updateCheckboxState(cell));
            return cell;
        });
        setEditable(true);
        setSortable(false);
        selectAllCheckBox.setSelected(true);
        setGraphic(selectAllCheckBox);
        MenuItem inverseMI = new MenuItem("Inverse Selection");
        inverseMI.setOnAction(e -> tableView.getItems().stream().filter(te -> !te.readOnlyProperty().get())
                .forEach(te -> te.selectedProperty().setValue(!te.selectedProperty().get())));
        final ContextMenu contextMenu = new ContextMenu(inverseMI);
        selectAllCheckBox.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(selectAllCheckBox, e.getScreenX(), e.getScreenY());
            }
        });
    }

    private void updateCheckboxState(TableCell<TableEntry, Boolean> cell) {
        TableRow<?> row = cell.getTableRow();
        if (row != null) {
            TableEntry item = (TableEntry) row.getItem();
            if (item != null) {
                cell.setEditable(!item.readOnlyProperty().get());
                item.selectedProperty().set(!item.selectedProperty().get());
                cell.setDisable(item.valueProperty().get().value.equals(VNoData.INSTANCE));
                if (item.valueProperty().get().value.equals(VNoData.INSTANCE)) {
                    item.selectedProperty().set(false);
                }
            }
        }
    }
}
