/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */
package org.phoebus.applications.saveandrestore.ui;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
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

/**
 * {@link Node} selection dialog controller. This can be used whenever user needs a UI to select a {@link Node}
 * in the save-and-restore data.
 * <p>
 * A version with UI design complying the original version all
 * {@link NodeType}s are shown in a {@link TreeView}.
 * </p>
 * <p>
 * By default all {@link NodeType}s are shown in the {@link TreeView}, but this may be overridden by calling
 * {@link #setHiddenNodeTypes(List)}.
 * </p>
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

    private final SimpleObjectProperty<Node> selectedNodeProperty = new SimpleObjectProperty<>();

    /**
     * Specifies which {@link NodeType}s to hide from the tree view.
     */
    private final List<NodeType> hiddenNodeTypes = new ArrayList<>();

    public Node getSelectedNode() {
        return selectedNodeProperty.get();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        treeView.setShowRoot(true);
        treeView.setCellFactory(cell -> new BrowserTreeCell());
        treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        treeView.getSelectionModel().selectedItemProperty().addListener((observableValue, nodeTreeItem, selectedTreeItem) -> selectedNodeProperty.set(selectedTreeItem.getValue()));

        createFolderButton.setOnAction(action -> createNewFolder(treeView.getSelectionModel().getSelectedItem()));
        createFolderButton.disableProperty().bind(Bindings.createBooleanBinding(() -> selectedNodeProperty.get() != null && !selectedNodeProperty.get().getNodeType().equals(NodeType.FOLDER),
                selectedNodeProperty));

        chooseButton.setDefaultButton(true);
        chooseButton.disableProperty().bind(Bindings.createBooleanBinding(() -> selectedNodeProperty.get() == null, selectedNodeProperty));

        initializeTreeView();
    }

    private void initializeTreeView() {
        JobManager.schedule("Initialize tree view", monitor -> {
            Node rootNode = saveAndRestoreService.getRootNode();
            TreeItem<Node> rootItem = createNode(rootNode);
            Platform.runLater(() -> {
                rootItem.setExpanded(true);
                treeView.setRoot(rootItem);
                recursiveAddNode(rootItem);
                dialogContent.disableProperty().set(false);
                progressIndicator.visibleProperty().set(false);
            });
        });
    }
    
    private void recursiveAddNode(TreeItem<Node> parentItem) {
        List<Node> childNodes = saveAndRestoreService.getChildNodes(parentItem.getValue());
        List<TreeItem<Node>> childItems = childNodes.stream()
                .filter(node -> !hiddenNodeTypes.contains(node.getNodeType()))
                .map(node -> {
                    TreeItem<Node> treeItem = createNode(node);
                    recursiveAddNode(treeItem);
                    return treeItem;
                }).toList();
        List<TreeItem<Node>> sorted = childItems.stream().sorted(Comparator.comparing(TreeItem::getValue)).toList();
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
                        .toList();

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

    @SuppressWarnings("unused")
    @FXML
    private void close() {
        ((Stage) treeView.getScene().getWindow()).close();
    }

    /**
     * Sets the {@link NodeType}s that should be visible in the {@link TreeView}. If
     * {@link NodeType#FOLDER} is included in the {@link List}, it will be removed.
     * @param nodeTypes {@link List} of {@link NodeType}s. May be <code>null</code>
     *                              for fault tolerance reasons.
     */
    public void setHiddenNodeTypes(List<NodeType> nodeTypes) {
        if(nodeTypes != null){
            hiddenNodeTypes.addAll(nodeTypes.stream().filter(nt -> !nt.equals(NodeType.FOLDER)).toList());
        }
    }

    /**
     * Sets the handler of the select button.
     * @param actionEventEventHandler An event handler...
     */
    public void addOkButtonActionHandler(EventHandler<ActionEvent> actionEventEventHandler){
        chooseButton.setOnAction(actionEventEventHandler);
    }
}
