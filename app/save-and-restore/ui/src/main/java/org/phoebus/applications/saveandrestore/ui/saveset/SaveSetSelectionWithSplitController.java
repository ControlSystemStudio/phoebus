/**
 * Copyright (C) 2020 Facility for Rare Isotope Beams
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact Information: Facility for Rare Isotope Beam,
 *                      Michigan State University,
 *                      East Lansing, MI 48824-1321
 *                      http://frib.msu.edu
 */
package org.phoebus.applications.saveandrestore.ui.saveset;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import org.phoebus.applications.saveandrestore.ApplicationContextProvider;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.BrowserTreeCell;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * {@link Node} selection dialog controller.
 *
 * A version with FRIB-preferred UI design
 * :Snapshots are separated as list from the other {@link NodeType}.
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class SaveSetSelectionWithSplitController extends BaseSaveSetSelectionController implements Initializable {

    private SaveAndRestoreService saveAndRestoreService = (SaveAndRestoreService) ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().getBean("saveAndRestoreService");

    @FXML
    private TreeView<Node> treeView;

    @FXML
    private ListView<Node> listView;

    @FXML
    private Button createFolderButton;

    @FXML
    private Button chooseButton;

    private Node selectedNode = null;

    @Override
    public Node getSelectedNode() {
        return selectedNode;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        treeView.setShowRoot(true);
        treeView.setCellFactory(cell -> new BrowserTreeCell(null, null, null, null));
        listView.setCellFactory(cell -> new SavesetListCell());

        Node rootNode = saveAndRestoreService.getRootNode();
        TreeItem<Node> rootItem = createNode(rootNode);

        rootItem.setExpanded(true);

        RecursiveAddNode(rootItem);

        treeView.setRoot(rootItem);

        treeView.getSelectionModel().selectedItemProperty().addListener((observableValue, nodeTreeItem, selectedTreeItem) -> {
            listView.getItems().clear();

            Node selectedNode = selectedTreeItem.getValue();
            if (selectedNode.getNodeType()!= NodeType.FOLDER || selectedNode.getUniqueId().equals(saveAndRestoreService.getRootNode().getUniqueId())) {
                return;
            }

            List<Node> savesets = saveAndRestoreService.getChildNodes(selectedNode).stream()
                    .filter(node -> node.getNodeType().equals(NodeType.CONFIGURATION))
                    .collect(Collectors.toList());

            Node newSaveset = Node.builder()
                    .nodeType(NodeType.CONFIGURATION)
                    .name("Create a new saveset")
                    .uniqueId("0")
                    .build();

            listView.getItems().add(newSaveset);
            listView.getSelectionModel().selectFirst();

            if (!savesets.isEmpty() && !isDisabledSavesetSelection) {
                listView.getItems().addAll(savesets);
            }
        });

        treeView.getSelectionModel().selectedItemProperty().addListener((observableValue, nodeTreeItem, selectedTreeItem) -> {
            if (selectedTreeItem == null) {
                return;
            }

            Node selectedNode = selectedTreeItem.getValue();
            if (selectedNode.getUniqueId().equals(saveAndRestoreService.getRootNode().getUniqueId())) {
                chooseButton.setDisable(true);
            } else {
                chooseButton.setDisable(false);
            }
        });

        treeView.getSelectionModel().selectFirst();

        createFolderButton.setOnAction(action -> createNewFolder(treeView.getSelectionModel().getSelectedItem()));

        chooseButton.setDefaultButton(true);
        chooseButton.setOnAction(action -> {
            Node node = listView.getSelectionModel().getSelectedItem();

            if (node != null && node.getUniqueId().equals("0")) {
                selectedNode = treeView.getSelectionModel().getSelectedItem().getValue();
            } else {
                selectedNode = node;
            }

            close();
        });
    }

    private void RecursiveAddNode(TreeItem<Node> parentItem) {
        List<Node> childNodes = saveAndRestoreService.getChildNodes(parentItem.getValue());
        List<TreeItem<Node>> childItems = childNodes.stream()
                .filter(node -> node.getNodeType().equals(NodeType.FOLDER))
                .map(node -> {
                    TreeItem<Node> treeItem = createNode(node);
                    RecursiveAddNode(treeItem);
                    return treeItem;
                }).collect(Collectors.toList());
        parentItem.getChildren().addAll(childItems);
    }

    private TreeItem<Node> createNode(final Node node){
        return new TreeItem<>(node){
            @Override
            public boolean isLeaf(){
                return getChildren().isEmpty();
            }
        };
    }

    private void createNewFolder(TreeItem<Node> parentTreeItem) {
        List<String> existingFolderNames =
                parentTreeItem.getChildren().stream()
                        .filter(item -> item.getValue().getNodeType().equals(NodeType.FOLDER))
                        .map(item -> item.getValue().getName())
                        .collect(Collectors.toList());

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(Messages.contextMenuNewFolder);
        dialog.setContentText(Messages.promptNewFolder);
        dialog.setHeaderText(null);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

        dialog.getEditor().textProperty().addListener((observable, oldValue, newValue) -> {
            String value = newValue.trim();
            dialog.getDialogPane().lookupButton(ButtonType.OK)
                    .setDisable(existingFolderNames.contains(value) || value.isEmpty());
        });

        Optional<String> result = dialog.showAndWait();

        if (result.isPresent()) {
            Node newFolderNode = Node.builder()
                    .name(result.get())
                    .build();
            try {
                Node newTreeNode = saveAndRestoreService
                        .createNode(parentTreeItem.getValue().getUniqueId(), newFolderNode);
                parentTreeItem.getChildren().add(createNode(newTreeNode));
                parentTreeItem.setExpanded(true);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Action failed");
                alert.setHeaderText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private class SavesetListCell extends ListCell<Node> {

        private javafx.scene.Node saveSetBox;

        public SavesetListCell() {

            FXMLLoader loader = new FXMLLoader();

            try {
                loader.setLocation(getClass().getClassLoader().getResource("org/phoebus/applications/saveandrestore/ui/TreeCellGraphic.fxml"));
                javafx.scene.Node rootNode = loader.load();
                saveSetBox = rootNode.lookup("#saveset");
            } catch (IOException e) {
                e.printStackTrace();
            }

            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        public void updateItem(Node node, boolean empty) {
            super.updateItem(node, empty);
            if (empty) {
                setGraphic(null);
                setText(null);
                return;
            }

            ((Label) saveSetBox.lookup("#savesetLabel")).setText(node.getName());
            setGraphic(saveSetBox);
            String description = node.getProperty("description");
            if(description != null && !description.isEmpty()) {
                setTooltip(new Tooltip(description));
            }
        }
    }

    @FXML
    private void close() {
        ((Stage) treeView.getScene().getWindow()).close();
    }
}
