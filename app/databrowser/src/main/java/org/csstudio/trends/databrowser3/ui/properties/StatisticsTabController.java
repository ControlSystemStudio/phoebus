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

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import org.csstudio.trends.databrowser3.Messages;
import org.csstudio.trends.databrowser3.model.Model;
import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.ModelListener;


public class StatisticsTabController {
    private Model model;

    @FXML
    private Button refreshAll;

    @FXML
    private TableView<ModelItem> tracesTable;

    @FXML
    private TableColumn<ModelItem, Button> buttonColumn;

    @FXML
    private TableColumn<ModelItem, String> displayNameColumn;

    @FXML
    private TableColumn<ModelItem, String> countColumn;

    public StatisticsTabController(Model model){
        this.model = model;
    }


    @FXML
    public void initialize() {
        refreshAll.setText(Messages.RefreshAll);
        tracesTable.setPlaceholder(new Label(Messages.TraceTableEmpty));
        createTable();



        model.addListener(new ModelListener() {
            @Override
            public void itemAdded(ModelItem item) {
                tracesTable.getItems().setAll(model.getItems());
            }
        });
    }


    @FXML
    public void refreshAll(){

    }

    private void createTable(){

        displayNameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getResolvedName()));
        displayNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        buttonColumn.setCellValueFactory(column -> {
            Button button = new Button(Messages.Refresh);
            button.setOnAction(e -> refresh(column.getValue()));
            button.setGraphic(new ColorIndicator(column.getValue().getPaintColor()));
            return new SimpleObjectProperty<>(button);
        });
        buttonColumn.setCellFactory(column -> new TableCell<>(){
            @Override
            public void updateItem(final Button button, final boolean empty){
                super.updateItem(button, empty);
                setGraphic(empty ? null : button);
            }
        });

        countColumn.setCellFactory(TextFieldTableCell.forTableColumn());
    }

    private void refresh(ModelItem modelItem){
       
    }

    private class ColorIndicator extends Rectangle{
        public ColorIndicator(Color color){
            super();
            setX(0);
            setY(0);
            setWidth(10);
            setHeight(10);
            setFill(color);
        }
    }
}
