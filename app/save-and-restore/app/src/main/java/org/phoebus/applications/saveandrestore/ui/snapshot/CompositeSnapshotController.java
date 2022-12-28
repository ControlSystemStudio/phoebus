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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CompositeSnapshotController {

    @FXML
    private StackPane root;

    @FXML
    BorderPane borderPane;

    @FXML
    private TableColumn<Node, Node> snapshotNameColumn;

    @FXML
    private TableColumn<Node, Date> snapshotDateColumn;

    @FXML
    private TableView<Node> snapshotTable;

    @FXML
    private TextArea descriptionTextArea;

    @FXML
    private Button saveButton;

    @FXML
    private TextField compositeSnapshotNameField;
    @FXML
    private Label compositeSnapshotCreatedDateField;

    @FXML
    private Label compositeSnapshotLastModifiedDateField;
    @FXML
    private Label createdByField;

    private SaveAndRestoreService saveAndRestoreService;

    private final SimpleBooleanProperty dirty = new SimpleBooleanProperty(false);

    private final ObservableList<Node> snapshotEntries = FXCollections.observableArrayList();

    private final SimpleBooleanProperty selectionEmpty = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty singelSelection = new SimpleBooleanProperty(false);
    private final SimpleStringProperty compositeSnapshotDescriptionProperty = new SimpleStringProperty();
    private final SimpleStringProperty compositeSnapshotNameProperty = new SimpleStringProperty();
    private Node parentFolder;

    private final SimpleObjectProperty<Node> compositeSnapshotNode = new SimpleObjectProperty<>();

    private final CompositeSnapshotTab compositeSnapshotTab;

    private final Logger logger = Logger.getLogger(CompositeSnapshotController.class.getName());

    private final SimpleBooleanProperty disabledUi = new SimpleBooleanProperty(false);

    private final SaveAndRestoreController saveAndRestoreController;

    @FXML
    private VBox progressIndicator;

    public CompositeSnapshotController(CompositeSnapshotTab compositeSnapshotTab, SaveAndRestoreController saveAndRestoreController) {
        this.compositeSnapshotTab = compositeSnapshotTab;
        this.saveAndRestoreController = saveAndRestoreController;
    }

    @FXML
    public void initialize() {

        saveAndRestoreService = SaveAndRestoreService.getInstance();

        snapshotTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        snapshotTable.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> selectionEmpty.set(nv == null));

        snapshotTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Node>) c -> singelSelection.set(c.getList().size() == 1));

        MenuItem deleteMenuItem = new MenuItem(Messages.menuItemDeleteSelectedPVs,
                new ImageView(ImageCache.getImage(SaveAndRestoreController.class, "/icons/delete.png")));
        deleteMenuItem.setOnAction(ae -> {
            snapshotEntries.removeAll(snapshotTable.getSelectionModel().getSelectedItems());
            snapshotTable.refresh();
        });

        deleteMenuItem.disableProperty().bind(Bindings.createBooleanBinding(() -> snapshotTable.getSelectionModel().getSelectedItems().isEmpty(),
                snapshotTable.getSelectionModel().getSelectedItems()));

        snapshotDateColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Node, Date> call(TableColumn param) {
                return new TableCell<>() {
                    @Override
                    public void updateItem(Date item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText("");
                        } else {
                            setText(TimestampFormats.SECONDS_FORMAT.format(getItem().toInstant()));
                        }
                    }
                };
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem removeMenuItem = new MenuItem("Remove Selected");
        removeMenuItem.setOnAction(event -> {
            List<Node> selected = snapshotTable.getSelectionModel().getSelectedItems();
            snapshotEntries.removeAll(selected);
        });
        contextMenu.getItems().add(removeMenuItem);

        snapshotTable.setContextMenu(contextMenu);
        snapshotTable.setOnContextMenuRequested(event -> {
            if (snapshotTable.getSelectionModel().getSelectedItems().size() == 0) {
                contextMenu.hide();
                event.consume();
            }
        });

        snapshotTable.setRowFactory(tableView -> new TableRow<>() {
            @Override
            protected void updateItem(Node node, boolean empty) {
                super.updateItem(node, empty);
                if (node == null || empty) {
                    setTooltip(null);
                    setOnMouseClicked(null);
                } else {
                    setTooltip(new Tooltip(Messages.searchEntryToolTip));
                    setOnMouseClicked(action -> {
                        if (action.getClickCount() == 2) {
                            Stack<Node> copiedStack = new Stack<>();
                            DirectoryUtilities.CreateLocationStringAndNodeStack(node, false).getValue().forEach(copiedStack::push);
                            saveAndRestoreController.locateNode(copiedStack);
                            saveAndRestoreController.nodeDoubleClicked(node);
                        }
                    });
                }
            }
        });

        snapshotNameColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue()));
        snapshotNameColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Node, Node> call(TableColumn param) {
                return new TableCell<>() {
                    @Override
                    public void updateItem(Node item, boolean empty) {
                        super.updateItem(item, empty);
                        selectionEmpty.set(empty);
                        if (empty) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            setText(getItem().getName());
                            NodeType nodeType = getItem().getNodeType();
                            boolean golden = getItem().getTags() != null &&
                                    getItem().getTags().stream().filter(t -> t.getName().equals(Tag.GOLDEN)).findFirst().isPresent();
                            if (nodeType.equals(NodeType.SNAPSHOT)) {
                                setGraphic(new ImageView(golden ?
                                        ImageRepository.GOLDEN_SNAPSHOT :
                                        ImageRepository.SNAPSHOT));
                            } else {
                                setGraphic(new ImageView(ImageRepository.COMPOSITE_SNAPSHOT));
                            }
                        }
                    }
                };
            }
        });

        compositeSnapshotNameField.textProperty().bindBidirectional(compositeSnapshotNameProperty);
        descriptionTextArea.textProperty().bindBidirectional(compositeSnapshotDescriptionProperty);

        snapshotEntries.addListener((ListChangeListener<Node>) change -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasRemoved()) {
                    FXCollections.sort(snapshotEntries);
                    dirty.set(true);
                }
            }
        });

        compositeSnapshotNameProperty.addListener((observableValue, oldValue, newValue) -> dirty.set(!newValue.equals(compositeSnapshotNode.getName())));
        compositeSnapshotDescriptionProperty.addListener((observable, oldValue, newValue) -> dirty.set(!newValue.equals(compositeSnapshotNode.get().getDescription())));

        saveButton.disableProperty().bind(Bindings.createBooleanBinding(() -> dirty.not().get() ||
                        compositeSnapshotDescriptionProperty.isEmpty().get() ||
                        compositeSnapshotNameProperty.isEmpty().get(),
                dirty, compositeSnapshotDescriptionProperty, compositeSnapshotNameProperty));

        // Update UI when the composite snapshot node has been set or updated.
        compositeSnapshotNode.addListener(observable -> {
            if (observable != null) {
                SimpleObjectProperty<Node> simpleObjectProperty = (SimpleObjectProperty<Node>) observable;
                Node newValue = simpleObjectProperty.get();
                compositeSnapshotNameProperty.set(newValue.getName());
                compositeSnapshotCreatedDateField.textProperty().set(newValue.getCreated() != null ?
                        TimestampFormats.SECONDS_FORMAT.format(Instant.ofEpochMilli(newValue.getCreated().getTime())) : null);
                compositeSnapshotLastModifiedDateField.textProperty().set(newValue.getLastModified() != null ?
                        TimestampFormats.SECONDS_FORMAT.format(Instant.ofEpochMilli(newValue.getLastModified().getTime())) : null);
                createdByField.textProperty().set(newValue.getUserName());
                compositeSnapshotDescriptionProperty.set(compositeSnapshotNode.get().getDescription());
            }
        });


        snapshotTable.setOnDragOver(event -> {
            if (event.getDragboard().hasContent(SaveAndRestoreApplication.NODE_SELECTION_FORMAT)) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        snapshotTable.setOnDragDropped(event -> {
            List<Node> sourceNodes = (List<Node>) event.getDragboard().getContent(SaveAndRestoreApplication.NODE_SELECTION_FORMAT);
            if (!mayDrop(sourceNodes)) {
                return;
            }
            disabledUi.set(true);
            checkForDuplicatePVs(sourceNodes, duplicates -> {
                disabledUi.set(false);
                if (duplicates.isEmpty()) {
                    snapshotEntries.addAll(sourceNodes);
                } else {
                    int maxItems = 10;
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < Math.min(duplicates.size(), maxItems); i++) {
                        stringBuilder.append(duplicates.get(i)).append(System.lineSeparator());
                    }
                    if (duplicates.size() > maxItems) {
                        stringBuilder.append(".").append(System.lineSeparator())
                                .append(".").append(System.lineSeparator())
                                .append(".").append(System.lineSeparator());
                        stringBuilder.append(MessageFormat.format(Messages.duplicatePVNamesAdditionalItems, duplicates.size() - maxItems));
                    }

                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle(Messages.errorGeneric);
                        alert.setHeaderText(Messages.duplicatePVNamesFoundInSelection);
                        alert.setContentText(stringBuilder.toString());
                        alert.showAndWait();
                    });
                }
            });
        });

        progressIndicator.visibleProperty().bind(disabledUi);
        disabledUi.addListener((observable, oldValue, newValue) -> borderPane.setDisable(newValue));

    }

    @FXML
    public void save() {
        doSave(compositeSnapshot -> {
            loadCompositeSnapshot(compositeSnapshot.getCompositeSnapshotNode());
        });
    }

    private void doSave(Consumer<CompositeSnapshot> completion) {
        disabledUi.set(true);
        JobManager.schedule("Save/update composite snapshot", monitor -> {
            try {
                compositeSnapshotNode.get().setName(compositeSnapshotNameProperty.get());
                compositeSnapshotNode.get().setDescription(compositeSnapshotDescriptionProperty.get());
                CompositeSnapshot compositeSnapshot = new CompositeSnapshot();
                compositeSnapshot.setCompositeSnapshotNode(compositeSnapshotNode.get());
                CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
                compositeSnapshotData
                        .setReferencedSnapshotNodes(snapshotEntries.stream().map(Node::getUniqueId).collect(Collectors.toList()));
                compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);

                if (compositeSnapshotNode.get().getUniqueId() == null) { // New composite snapshot
                    compositeSnapshot = saveAndRestoreService.saveCompositeSnapshot(parentFolder,
                            compositeSnapshot);
                    compositeSnapshotTab.setId(compositeSnapshot.getCompositeSnapshotNode().getUniqueId());
                    compositeSnapshotTab.updateTabTitle(compositeSnapshot.getCompositeSnapshotNode().getName());
                } else {
                    compositeSnapshotData.setUniqueId(compositeSnapshotNode.get().getUniqueId());
                    compositeSnapshot = saveAndRestoreService.updateCompositeSnapshot(compositeSnapshot);
                }
                dirty.set(false);
                completion.accept(compositeSnapshot);
            } catch (Exception e1) {
                ExceptionDetailsErrorDialog.openError(snapshotTable,
                        Messages.errorActionFailed,
                        Messages.errorCreateConfigurationFailed,
                        e1);
            }
            finally {
                disabledUi.set(false);
            }
        });
    }


    /**
     * Sets the (existing) composite snapshot {@link Node} and retrieves the referenced {@link Node}s to populate the
     * table view with entries.
     *
     * @param node An existing {@link Node} of type {@link NodeType#COMPOSITE_SNAPSHOT}.
     */
    public void loadCompositeSnapshot(final Node node) {
        Platform.runLater(() -> compositeSnapshotNode.setValue(node));
        snapshotEntries.clear();
        disabledUi.set(true);
        JobManager.schedule("Load composite snapshot data", monitor -> {
            try {
                List<Node> referencedNodes = saveAndRestoreService.getCompositeSnapshotNodes(node.getUniqueId());
                snapshotEntries.addAll(referencedNodes);
                Collections.sort(snapshotEntries);
                snapshotTable.setItems(snapshotEntries);
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(root, Messages.errorGeneric, Messages.errorUnableToRetrieveData, e);
            }
            finally {
                disabledUi.set(false);
            }
        });
    }

    public boolean handleCompositeSnapshotTabClosed() {
        if (dirty.get()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(Messages.closeTabPrompt);
            alert.setContentText(Messages.closeCompositeSnapshotWarning);
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get().equals(ButtonType.OK);
        }
        return true;
    }

    /**
     * Configures the controller to create a new composite snapshot.
     *
     * @param parentNode The parent {@link Node} for the new composite, i.e. must be a
     *                   {@link Node} of type {@link NodeType#FOLDER}.
     */
    public void newCompositeSnapshot(Node parentNode) {
        parentFolder = parentNode;
        compositeSnapshotNode.setValue(Node.builder().nodeType(NodeType.COMPOSITE_SNAPSHOT).build());
        snapshotEntries.clear();
        snapshotEntries.addAll(new ArrayList<>());
        snapshotTable.setItems(snapshotEntries);
        Platform.runLater(() -> compositeSnapshotNameField.requestFocus());
        dirty.set(false);
    }

    /**
     * Checks that dropped source {@link Node}s may be dropped. Only {@link Node}s of type
     * {@link NodeType#COMPOSITE_SNAPSHOT} and {@link NodeType#SNAPSHOT} may be dropped.
     *
     * @param sourceNodes List of {@link Node}s, e.g. selected in UI from tree view.
     * @return <code>true</code> if the source {@link Node}s may be added.
     */
    private boolean mayDrop(List<Node> sourceNodes) {
        if (sourceNodes.stream().filter(n -> !n.getNodeType().equals(NodeType.SNAPSHOT) &&
                !n.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)).findFirst().isPresent()) {
            return false;
        }
        return true;
    }

    /**
     * Calls service to determine if the list of PV names found in referenced snapshots contains duplicates.
     *
     * @param droppedSnapshots The snapshot {@link Node}s dropped by user into the editor tab.
     * @param completion       Callback receiving a list of duplicate PV names.
     */
    private void checkForDuplicatePVs(List<Node> droppedSnapshots, Consumer<List<String>> completion) {
        JobManager.schedule("Check snapshot PV duplicates", monitor -> {
            List<String> allSnapshotIds = snapshotEntries.stream().map(Node::getUniqueId).collect(Collectors.toList());
            allSnapshotIds.addAll(droppedSnapshots.stream().map(Node::getUniqueId).collect(Collectors.toList()));
            List<String> duplicates = null;
            try {
                duplicates = saveAndRestoreService.checkCompositeSnapshotConsistency(allSnapshotIds);
            } catch (Exception e) {
                disabledUi.set(false);
                ExceptionDetailsErrorDialog.openError(Messages.errorGeneric,
                        Messages.duplicatePVNamesCheckFailed,
                        e);
            }
            completion.accept(duplicates);
        });
    }
}
