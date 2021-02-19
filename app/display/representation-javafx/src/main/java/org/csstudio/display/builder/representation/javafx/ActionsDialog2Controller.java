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

package org.csstudio.display.builder.representation.javafx;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextAlignment;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfo.ActionType;

import java.util.List;
import java.util.logging.Level;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

public class ActionsDialog2Controller {

    @FXML
    private MenuButton addButton;

    @FXML
    private Button removeButton;

    @FXML
    private Button upButton;

    @FXML
    private Button downButton;

    @FXML
    private ListView actionsListView;

    /** Actions edited by the dialog */
    private final ObservableList<ActionInfo> actions = FXCollections.observableArrayList();

    /** ListView cell for ActionInfo, shows title if possible */
    private static class ActionInfoCell extends ListCell<ActionInfo>
    {
        @Override
        protected void updateItem(final ActionInfo action, final boolean empty)
        {
            super.updateItem(action, empty);
            try
            {
                if (action == null)
                {
                    setText("");
                    setGraphic(null);
                }
                else
                {
                    setText(action.toString());
                    setGraphic(new ImageView(new Image(action.getType().getIconURL().toExternalForm())));
                }
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error displaying " + action, ex);
            }
        }
    };


    @FXML
    public void initialize() {
        addButton.setGraphic(JFXUtil.getIcon("add.png"));
        removeButton.setGraphic(JFXUtil.getIcon("delete.png"));
        upButton.setGraphic(JFXUtil.getIcon("up.png"));
        downButton.setGraphic(JFXUtil.getIcon("down.png"));

        for (ActionType type : ActionType.values())
        {
            final ImageView icon = new ImageView(new Image(type.getIconURL().toExternalForm()));
            final MenuItem item = new MenuItem(type.toString(), icon);
            item.setOnAction(event ->
            {
                final ActionInfo new_action = ActionInfo.createAction(type);
                actions.add(new_action);
                actionsListView.getSelectionModel().select(new_action);
            });
            addButton.getItems().add(item);
        }

        actionsListView.setCellFactory(view -> new ActionInfoCell());
        removeButton.disableProperty().bind(Bindings.isEmpty(actionsListView.getSelectionModel().getSelectedItems()));
        upButton.disableProperty().bind(new BooleanBinding() {
            @Override
            protected boolean computeValue() {
                boolean empty = actionsListView.getSelectionModel().getSelectedItems().isEmpty();
                int idx = actionsListView.getSelectionModel().getSelectedIndex();
                return  empty ||
                        idx == 0;
            }
        });
    }

    public void setActions(List<ActionInfo> actions){
        actionsListView.getItems().addAll(actions);
    }

    @FXML
    public void removeAction(){
        actionsListView.getItems().remove(actionsListView.getSelectionModel().getSelectedIndex());
    }
}
