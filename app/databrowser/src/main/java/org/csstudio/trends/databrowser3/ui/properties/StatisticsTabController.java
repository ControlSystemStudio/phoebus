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
 */

package org.csstudio.trends.databrowser3.ui.properties;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;


public class StatisticsTabController {
    private Model model;

    @FXML
    private Button refreshAll;

    @FXML
    private TableView<ModelItem> tracesTable;

    @FXML
    private TableColumn<ModelItem, Void> buttonColumn;

    @FXML
    private TableColumn<ModelItem, String> displayNameColumn;


    @FXML
    public void initialize() {
        refreshAll.setText(Messages.RefreshAll);
        tracesTable.setPlaceholder(new Label(Messages.TraceTableEmpty));
        createTable();
    }

    public void setModel(Model model){
        this.model = model;
    }

    @FXML
    public void refreshAll(){

    }

    private void createTable(){

        displayNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getResolvedName()));
        displayNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        Callback<TableColumn<ModelItem, Void>, TableCell<ModelItem, Void>> cellFactory = new Callback<TableColumn<ModelItem, Void>, TableCell<ModelItem, Void>>() {
            @Override
            public TableCell<ModelItem, Void> call(final TableColumn<ModelItem, Void> param) {
                final TableCell<ModelItem, Void> cell = new TableCell<ModelItem, Void>() {

                    private final Button btn = new Button("Refresh");

                    {
                        btn.setOnAction((ActionEvent event) -> {
                            // TODO
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        buttonColumn.setCellFactory(cellFactory);
    }
}
