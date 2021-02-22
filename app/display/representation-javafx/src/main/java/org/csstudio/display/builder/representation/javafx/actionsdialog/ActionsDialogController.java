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

package org.csstudio.display.builder.representation.javafx.actionsdialog;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.csstudio.display.builder.model.properties.ActionInfo;
import org.csstudio.display.builder.model.properties.ActionInfo.ActionType;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

public class ActionsDialogController {

    @FXML
    private MenuButton addButton;
    @FXML
    private Button removeButton;
    @FXML
    private Button upButton;
    @FXML
    private Button downButton;
    @FXML
    private ListView<ActionsDialogActionItem> actionsListView;
    @FXML
    private CheckBox executeAllCheckBox;
    @FXML
    private StackPane detailsPane;

    private IntegerProperty selectionIndex = new SimpleIntegerProperty(-1);
    private IntegerProperty listSize = new SimpleIntegerProperty(0);
    private BooleanProperty executeAll = new SimpleBooleanProperty();

    /** Actions edited by the dialog */
    private final ObservableList<ActionsDialogActionItem> actionList = FXCollections.observableArrayList();

    /** ListView cell for ActionInfo, shows title if possible */
    private static class ActionInfoCell extends ListCell<ActionsDialogActionItem>
    {
        @Override
        protected void updateItem(final ActionsDialogActionItem action, final boolean empty)
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
                    setText(action.getActionInfo().toString());
                    setGraphic(new ImageView(new Image(action.getActionInfo().getType().getIconURL().toExternalForm())));
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
                final ActionInfo action = ActionInfo.createAction(type);
                ActionsDialogActionItem actionsDialogActionItem =
                        new ActionsDialogActionItem(action);
                actionList.add(actionsDialogActionItem);
                actionsListView.getSelectionModel().select(actionsDialogActionItem);
            });
            addButton.getItems().add(item);
        }

        actionsListView.setCellFactory(view -> new ActionInfoCell());

        executeAllCheckBox.selectedProperty().bind(executeAll);

        // Disable remove button if nothing is selected
        removeButton.disableProperty().bind(Bindings.isEmpty(actionsListView.getSelectionModel().getSelectedItems()));

        // Add listener to item selection
        actionsListView.getSelectionModel().selectedItemProperty().addListener((l, old, action) -> {
            ActionsDialogActionItem actionsDialogActionItem = actionsListView.getSelectionModel().getSelectedItem();
            selectionIndex.setValue(actionsListView.getSelectionModel().getSelectedIndex());
            listSize.setValue(actionsListView.getItems().size());
            if(!detailsPane.getChildren().contains(actionsDialogActionItem.getActionInfoEditor())){
                detailsPane.getChildren().add(actionsDialogActionItem.getActionInfoEditor());
            }
            //actionsDialogActionItem.getActionInfoEditor().setVisible(true);
            setDetailsPaneVisibility(actionsDialogActionItem);
        });

        // Disable down button if no selection is made or if last item is selected
        downButton.disableProperty()
                .bind(Bindings
                        .createBooleanBinding(() -> selectionIndex.get() >= listSize.get() - 1 || selectionIndex.get() < 0,
                                selectionIndex, listSize));

        // Disable up button if the top item is selected
        upButton.disableProperty().bind(Bindings.greaterThan(1, selectionIndex));
    }

    public void setActionInfos(ActionInfos actionInfos){
        actionList.addAll(actionInfos.getActions()
                .stream().map(ai -> new ActionsDialogActionItem(ai)).collect(Collectors.toList()));
        actionsListView.setItems(actionList);
        executeAll.setValue(actionInfos.isExecutedAsOne());
    }

    public ActionInfos getActionInfos(){
        return new ActionInfos(actionList.stream().map(a -> a.getActionInfo()).collect(Collectors.toList()),
                executeAll.get());
    }

    @FXML
    public void removeAction(){
        actionsListView.getItems().remove(actionsListView.getSelectionModel().getSelectedIndex());
    }

    @FXML
    public void moveUp(){
        final ActionsDialogActionItem item = actionList.remove(selectionIndex.get());
        actionList.add(selectionIndex.get(), item);
        actionsListView.getSelectionModel().select(item);
    }

    @FXML
    public void moveDown(){
        final ActionsDialogActionItem item = actionList.remove(selectionIndex.get());
        actionList.add(selectionIndex.get() + 2, item);
        actionsListView.getSelectionModel().select(item);
    }

    private void setDetailsPaneVisibility(ActionsDialogActionItem visibleItem){
        
        actionList.stream().forEach(i -> {
            if(i.getActionInfoEditor() != null){
                i.getActionInfoEditor().setVisible(i == visibleItem);
            }
        });
    }
}
