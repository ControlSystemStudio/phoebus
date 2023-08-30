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
package org.phoebus.applications.saveandrestore.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.framework.jobs.JobManager;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * {@link Node} selection dialog controller. This can be used whenever user needs a UI to select a {@link Node}
 * in the save-and-restore data.
 * <p>
 * A version with UI design complying the original version
 * :All {@link NodeType} are shown in a {@link TreeView}
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class NodeSelectionController implements Initializable {

    private final SaveAndRestoreService saveAndRestoreService = SaveAndRestoreService.getInstance();

    @FXML
    private TreeView<Node> treeView;

    @FXML
    private Button createFolderButton;

    @FXML
    private Button chooseButton;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private VBox dialogContent;

    private Node selectedNode = null;


    /**
     * Specifies which {@link NodeType}s to hide from the tree view.
     */
    private List<NodeType> hiddenNodeTypes = new ArrayList<>();

    public Node getSelectedNode() {
        return selectedNode;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        treeView.setShowRoot(true);
        treeView.setCellFactory(cell -> new BrowserTreeCell());

        treeView.getSelectionModel().selectedItemProperty().addListener((observableValue, nodeTreeItem, selectedTreeItem) -> {
            if (selectedTreeItem == null) {
                return;
            }
            Node selectedNode = selectedTreeItem.getValue();
            chooseButton.setDisable(selectedNode.getUniqueId().equals(saveAndRestoreService.getRootNode().getUniqueId()));
            createFolderButton.setDisable(!selectedNode.getNodeType().equals(NodeType.FOLDER));
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

        initializeTreeView();
    }

    private void initializeTreeView() {
        JobManager.schedule("Initialize tree view", monitor -> {
            Node rootNode = saveAndRestoreService.getRootNode();
            TreeItem<Node> rootItem = createNode(rootNode);
            Platform.runLater(() -> {
                rootItem.setExpanded(true);
                treeView.setRoot(rootItem);
                RecursiveAddNode(rootItem);
                dialogContent.disableProperty().set(false);
                progressIndicator.visibleProperty().set(false);
            });
        });
    }


    private void RecursiveAddNode(TreeItem<Node> parentItem) {
        List<Node> childNodes = saveAndRestoreService.getChildNodes(parentItem.getValue());
        List<TreeItem<Node>> childItems = childNodes.stream()
                .filter(node -> !hiddenNodeTypes.contains(node.getNodeType()))
                .map(node -> {
                    TreeItem<Node> treeItem = createNode(node);
                    RecursiveAddNode(treeItem);
                    return treeItem;
                }).collect(Collectors.toList());
        List<TreeItem<Node>> sorted = childItems.stream().sorted(Comparator.comparing(TreeItem::getValue)).collect(Collectors.toList());
        parentItem.getChildren().addAll(sorted);
        dialogContent.disableProperty().set(false);
        progressIndicator.visibleProperty().set(false);
    }

    private TreeItem<Node> createNode(final Node node) {
        return new TreeItem<>(node) {
            @Override
            public boolean isLeaf() {
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
                TreeItem<Node> treeItem = createNode(newTreeNode);
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

    public void setHiddenNodeTypes(List<NodeType> nodeTypes) {
        hiddenNodeTypes.addAll(nodeTypes);
    }
}
