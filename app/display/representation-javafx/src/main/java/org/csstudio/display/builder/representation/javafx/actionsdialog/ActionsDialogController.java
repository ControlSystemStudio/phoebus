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
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.ActionInfos;
import org.csstudio.display.builder.model.spi.ActionInfo;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

/**
 * FXML Controller
 */
public class ActionsDialogController {

    @SuppressWarnings("unused")
    @FXML
    private MenuButton addButton;
    @SuppressWarnings("unused")
    @FXML
    private Button removeButton;
    @SuppressWarnings("unused")
    @FXML
    private Button upButton;
    @SuppressWarnings("unused")
    @FXML
    private Button downButton;
    @SuppressWarnings("unused")
    @FXML
    private ListView<ActionsDialogActionItem> actionsListView;
    @SuppressWarnings("unused")
    @FXML
    private CheckBox executeAllCheckBox;
    @SuppressWarnings("unused")
    @FXML
    private StackPane detailsPane;

    private final Widget widget;

    private final IntegerProperty selectionIndex = new SimpleIntegerProperty(-1);
    private final IntegerProperty listSize = new SimpleIntegerProperty(0);
    private final BooleanProperty executeAll = new SimpleBooleanProperty();

    /**
     * Actions edited by the dialog
     */
    private final ObservableList<ActionsDialogActionItem> actionList = FXCollections.observableArrayList();

    /**
     * @param widget Widget
     */
    public ActionsDialogController(Widget widget) {
        this.widget = widget;
    }

    /**
     * ListView cell for {@link ActionInfo}, shows title if possible
     */
    private static class ActionInfoCell extends ListCell<ActionsDialogActionItem> {
        @Override
        protected void updateItem(final ActionsDialogActionItem actionsDialogActionItem, final boolean empty) {
            super.updateItem(actionsDialogActionItem, empty);
            try {
                if (actionsDialogActionItem == null) {
                    setText("");
                    setGraphic(null);
                } else {
                    setText(actionsDialogActionItem.getDescription());
                    setGraphic(new ImageView(actionsDialogActionItem.getImage()));
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Error displaying " + actionsDialogActionItem, ex);
            }
        }
    }

    /**
     * Initialize
     */
    @FXML
    public void initialize() {

        addButton.setGraphic(JFXUtil.getIcon("add.png"));
        removeButton.setGraphic(JFXUtil.getIcon("delete.png"));
        upButton.setGraphic(JFXUtil.getIcon("up.png"));
        downButton.setGraphic(JFXUtil.getIcon("down.png"));

        ServiceLoader<ActionInfo> actionInfos = ServiceLoader.load(ActionInfo.class);

        actionInfos.stream()
                .sorted(Comparator.comparing(actionInfo -> actionInfo.get().getPriority()))
                .forEach(actionInfoProvider -> {
                    ActionInfo actionInfo = actionInfoProvider.get();
                    try {
                        ActionsDialogActionItem actionsDialogActionItem = new ActionsDialogActionItem(widget, actionInfo);
                        final ImageView icon = new ImageView(actionInfo.getImage());
                        final MenuItem item = new MenuItem(actionInfo.toString(), icon);
                        item.setOnAction(event ->
                        {
                            actionList.add(actionsDialogActionItem);
                            actionsListView.setItems(actionList);
                            actionsListView.getSelectionModel().select(actionsDialogActionItem);
                        });
                        addButton.getItems().add(item);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to add action " + actionInfo.getType());
                    }
                });

        actionsListView.setCellFactory(view -> new ActionInfoCell());

        executeAllCheckBox.selectedProperty().bindBidirectional(executeAll);

        // Disable remove button if nothing is selected
        removeButton.disableProperty().bind(Bindings.isEmpty(actionsListView.getSelectionModel().getSelectedItems()));

        selectionIndex.bind(actionsListView.getSelectionModel().selectedIndexProperty());

        // Add listener to item selection
        actionsListView.getSelectionModel().selectedItemProperty().addListener((l, old, action) -> {
            listSize.setValue(actionsListView.getItems().size());
            detailsPane.getChildren().clear();
            if (listSize.get() == 0) {
                return;
            }
            ActionsDialogActionItem actionsDialogActionItem = actionsListView.getSelectionModel().getSelectedItem();
            if (actionsDialogActionItem == null) {
                return;
            }
            detailsPane.getChildren().add(actionsDialogActionItem.getActionEditor().getEditorUi());
        });

        // Disable down button if no selection is made or if last item is selected
        downButton.disableProperty()
                .bind(Bindings
                        .createBooleanBinding(() -> selectionIndex.get() >= listSize.get() - 1 || selectionIndex.get() < 0,
                                selectionIndex, listSize));

        // Disable up button if the top item is selected
        upButton.disableProperty().bind(Bindings.greaterThan(1, selectionIndex));
    }

    /**
     * Creates {@link ActionInfos} objects and one editor per item. The editors are added to the
     * {@link StackPane} and the top most item in the action list is selected.
     *
     * @param actionInfos ActionInfos
     */
    public void setActionInfos(final ActionInfos actionInfos) {
        if (actionInfos == null || actionInfos.getActions() == null || actionInfos.getActions().isEmpty()) {
            return;
        }
        actionList.addAll(actionInfos.getActions()
                .stream().map(ai -> {
                    ActionsDialogActionItem actionsDialogActionItem = new ActionsDialogActionItem(widget, ai);
                    return actionsDialogActionItem;
                }).collect(Collectors.toList()));
        actionsListView.setItems(actionList);
        actionsListView.getSelectionModel().select(0);
        executeAll.setValue(actionInfos.isExecutedAsOne());
    }


    /**
     * @return ActionInfos containing list of {@link ActionInfo} data as edited by user.
     */
    public ActionInfos getActionInfos() {
        return new ActionInfos(actionList.stream().map(a -> a.getActionInfo()).collect(Collectors.toList()),
                executeAll.get());
    }

    /**
     * Remove action
     */
    @SuppressWarnings("unused")
    @FXML
    public void removeAction() {
        int index = selectionIndex.get();
        detailsPane.getChildren().remove(0);
        actionsListView.getItems().remove(index);
    }

    /**
     * Move action
     */
    @SuppressWarnings("unused")
    @FXML
    public void moveUp() {
        final ActionsDialogActionItem item = actionList.remove(selectionIndex.get());
        actionList.add(selectionIndex.get(), item);
        actionsListView.getSelectionModel().select(item);
    }

    /**
     * Move action
     */
    @SuppressWarnings("unused")
    @FXML
    public void moveDown() {
        int targetIndex = selectionIndex.get() + 1;
        final ActionsDialogActionItem item = actionList.remove(selectionIndex.get());
        actionList.add(targetIndex, item);
        actionsListView.getSelectionModel().select(item);
    }
}
