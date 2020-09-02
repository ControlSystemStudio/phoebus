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
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import org.phoebus.applications.saveandrestore.ApplicationContextProvider;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.service.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.BrowserTreeCell;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * {@link Node} selection dialog controller.
 *
 * A version with UI design complying the original version
 * :All {@link NodeType} are shown in a {@link TreeView}
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class SaveSetSelectionController extends BaseSaveSetSelectionController implements Initializable {

    private SaveAndRestoreService saveAndRestoreService = (SaveAndRestoreService) ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().getBean("saveAndRestoreService");

    @FXML
    private TreeView<Node> treeView;

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

        Node rootNode = saveAndRestoreService.getRootNode();
        TreeItem<Node> rootItem = createNode(rootNode);

        rootItem.setExpanded(true);

        RecursiveAddNode(rootItem);

        treeView.setRoot(rootItem);

        treeView.getSelectionModel().selectedItemProperty().addListener((observableValue, nodeTreeItem, selectedTreeItem) -> {
            if (selectedTreeItem == null) {
                return;
            }

            Node selectedNode = selectedTreeItem.getValue();
            if (selectedNode.getUniqueId().equals(saveAndRestoreService.getRootNode().getUniqueId())
                    || selectedNode.getNodeType().equals(NodeType.FOLDER)) {
                chooseButton.setDisable(true);
            } else {
                chooseButton.setDisable(false);
            }

            if (selectedNode.getNodeType().equals(NodeType.FOLDER)) {
                createFolderButton.setDisable(false);
            } else {
                createFolderButton.setDisable(true);
            }
        });

        treeView.getSelectionModel().selectFirst();

        createFolderButton.setOnAction(action -> createNewFolder(treeView.getSelectionModel().getSelectedItem()));

        chooseButton.setDefaultButton(true);
        chooseButton.setOnAction(action -> {
            TreeItem<Node> treeItem = treeView.getSelectionModel().getSelectedItem();

            if (treeItem.getValue().getUniqueId().equals("0")) {
                selectedNode = treeItem.getParent().getValue();
            } else {
                selectedNode = treeItem.getValue();
            }

            close();
        });
    }

    private void RecursiveAddNode(TreeItem<Node> parentItem) {
        List<Node> childNodes = saveAndRestoreService.getChildNodes(parentItem.getValue());
        List<TreeItem<Node>> childItems = childNodes.stream()
                .filter(node -> isDisabledSavesetSelection ? !(node.getNodeType().equals(NodeType.CONFIGURATION) || node.getNodeType().equals(NodeType.SNAPSHOT)) : !node.getNodeType().equals(NodeType.SNAPSHOT) )
                .map(node -> {
                    TreeItem<Node> treeItem = createNode(node);
                    if (node.getNodeType().equals(NodeType.FOLDER)) {
                        treeItem.getChildren().add(createCreateANewSaveset());
                    }
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

    private TreeItem<Node> createCreateANewSaveset() {
        Node newSaveset = Node.builder()
                .nodeType(NodeType.CONFIGURATION)
                .name("Create a new saveset")
                .uniqueId("0")
                .build();

        return createNode(newSaveset);
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
                TreeItem<Node> treeItem = createNode(newTreeNode);
                treeItem.getChildren().add(createCreateANewSaveset());
                parentTreeItem.getChildren().add(treeItem);
                parentTreeItem.setExpanded(true);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Action failed");
                alert.setHeaderText(e.getMessage());
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void close() {
        ((Stage) treeView.getScene().getWindow()).close();
    }
}
