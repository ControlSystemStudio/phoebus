/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.search;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.RestoreUtil;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.RestoreMode;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.contextmenu.LoginMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.RestoreFromClientMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.RestoreFromServiceMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.TagGoldenMenuItem;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagUtil;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagWidget;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.util.time.TimestampFormats;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for the search result table.
 */
public class SearchResultTableViewController implements Initializable {

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<Node, ImageView> typeColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<Node, String> nameColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<Node, String> commentColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<Node, String> tagsColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<Node, String> lastUpdatedColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<Node, String> createdColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableColumn<Node, String> userColumn;

    @SuppressWarnings("unused")
    @FXML
    private TableView<Node> resultTableView;

    private final ObservableList<Node> selectedItemsProperty = FXCollections.observableArrayList();
    private final SimpleBooleanProperty disableUi = new SimpleBooleanProperty();
    protected final SimpleStringProperty userIdentity = new SimpleStringProperty();
    private final ObservableList<Node> tableEntries = FXCollections.observableArrayList();

    private SaveAndRestoreService saveAndRestoreService;
    private SaveAndRestoreController saveAndRestoreController;


    public SearchResultTableViewController(SaveAndRestoreController saveAndRestoreController) {
        this.saveAndRestoreController = saveAndRestoreController;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        saveAndRestoreService = SaveAndRestoreService.getInstance();

        resultTableView.getStylesheets().add(getClass().getResource("/save-and-restore-style.css").toExternalForm());

        resultTableView.setRowFactory(tableView -> new TableRow<>() {
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
                            try {
                                ApplicationService.createInstance(SaveAndRestoreApplication.NAME, new URI("file:/" + node.getUniqueId() + "?action=open_sar_node"));
                            } catch (URISyntaxException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            }
        });

        typeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getImageView(cell.getValue())));
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        nameColumn.getStyleClass().add("leftAlignedTableColumnHeader");

        commentColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getDescription()));
        commentColumn.getStyleClass().add("leftAlignedTableColumnHeader");
        createdColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(TimestampFormats.SECONDS_FORMAT.format(cell.getValue().getCreated().toInstant())));
        createdColumn.getStyleClass().add("leftAlignedTableColumnHeader");
        lastUpdatedColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(TimestampFormats.SECONDS_FORMAT.format(cell.getValue().getLastModified().toInstant())));
        lastUpdatedColumn.getStyleClass().add("leftAlignedTableColumnHeader");
        userColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUserName()));
        userColumn.getStyleClass().add("leftAlignedTableColumnHeader");

        tagsColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getTags() == null ?
                "" :
                cell.getValue().getTags().stream().map(Tag::getName).filter(name -> !name.equals(Tag.GOLDEN)).collect(Collectors.joining(System.lineSeparator()))));
        tagsColumn.getStyleClass().add("leftAlignedTableColumnHeader");

        ContextMenu contextMenu = new ContextMenu();
        MenuItem loginMenuItem =
                new LoginMenuItem(saveAndRestoreController, selectedItemsProperty, () -> ApplicationService.createInstance("credentials_management"));
        MenuItem tagGoldenMenuItem = new TagGoldenMenuItem(saveAndRestoreController, selectedItemsProperty);

        ImageView snapshotTagsIconImage = new ImageView(new Image(SearchAndFilterViewController.class.getResource("/icons/save-and-restore/snapshot-add_tag.png").toExternalForm()));
        Menu tagMenuItem = new Menu(Messages.contextMenuTags, snapshotTagsIconImage);

        MenuItem addTagMenuItem = TagWidget.AddTagMenuItem();
        addTagMenuItem.setOnAction(event -> TagUtil.addTag(resultTableView.getSelectionModel().getSelectedItems()));
        tagMenuItem.getItems().add(addTagMenuItem);

        RestoreFromClientMenuItem restoreFromClientMenuItem = new RestoreFromClientMenuItem(saveAndRestoreController, selectedItemsProperty,
                () -> {
                    disableUi.set(true);
                    RestoreUtil.restore(RestoreMode.CLIENT_RESTORE, saveAndRestoreService, selectedItemsProperty.get(0), () -> disableUi.set(false));
                });

        RestoreFromServiceMenuItem restoreFromServiceMenuItem = new RestoreFromServiceMenuItem(saveAndRestoreController, selectedItemsProperty,
                () -> {
                    disableUi.set(true);
                    RestoreUtil.restore(RestoreMode.SERVICE_RESTORE, saveAndRestoreService, selectedItemsProperty.get(0), () -> disableUi.set(false));
                });

        contextMenu.setOnShowing(event -> {
            selectedItemsProperty.setAll(resultTableView.getSelectionModel().getSelectedItems());
            // Empty result table -> hide menu and return
            if (selectedItemsProperty.isEmpty()) {
                Platform.runLater(contextMenu::hide);
                return;
            }
            tagMenuItem.disableProperty().set(userIdentity.isNull().get() ||
                    selectedItemsProperty.size() != 1 ||
                    (!selectedItemsProperty.get(0).getNodeType().equals(NodeType.SNAPSHOT) &&
                            !selectedItemsProperty.get(0).getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)));
            NodeType selectedItemType = resultTableView.getSelectionModel().getSelectedItem().getNodeType();
            if (selectedItemType.equals(NodeType.SNAPSHOT) || selectedItemType.equals(NodeType.COMPOSITE_SNAPSHOT)) {
                TagUtil.tag(tagMenuItem,
                        resultTableView.getSelectionModel().getSelectedItems(),
                        updatedNodes -> { // Callback, any extra handling added here
                        });
                TagUtil.configureGoldenItem(resultTableView.getSelectionModel().getSelectedItems(), tagGoldenMenuItem);
            }

            restoreFromClientMenuItem.configure();
            restoreFromServiceMenuItem.configure();

            resultTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            resultTableView.setContextMenu(contextMenu);
        });

        contextMenu.getItems().addAll(loginMenuItem, tagGoldenMenuItem, tagMenuItem, restoreFromClientMenuItem, restoreFromServiceMenuItem);

        resultTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        resultTableView.setContextMenu(contextMenu);

        // Bind search result table to tableEntries observable
        Property<ObservableList<Node>> tableListProperty = new SimpleObjectProperty<>(tableEntries);
        resultTableView.itemsProperty().bind(tableListProperty);

        resultTableView.setOnDragDetected(e -> {
            List<Node> selectedNodes = resultTableView.getSelectionModel().getSelectedItems();
            if (selectedNodes.stream().anyMatch(n ->
                    !n.getNodeType().equals(NodeType.SNAPSHOT) &&
                            !n.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT))) {
                return;
            }
            final ClipboardContent content = new ClipboardContent();
            final List<Node> nodes = new ArrayList<>(resultTableView.getSelectionModel().getSelectedItems());
            content.put(SaveAndRestoreApplication.NODE_SELECTION_FORMAT, nodes);
            resultTableView.startDragAndDrop(TransferMode.LINK).setContent(content);
            e.consume();
        });
    }

    private ImageView getImageView(Node node) {
        switch (node.getNodeType()) {
            case SNAPSHOT:
                if (node.hasTag(Tag.GOLDEN)) {
                    return new ImageView(ImageRepository.GOLDEN_SNAPSHOT);
                } else {
                    return new ImageView(ImageRepository.SNAPSHOT);
                }
            case COMPOSITE_SNAPSHOT:
                return new ImageView(ImageRepository.COMPOSITE_SNAPSHOT);
            case FOLDER:
                return new ImageView(ImageRepository.FOLDER);
            case CONFIGURATION:
                return new ImageView(ImageRepository.CONFIGURATION);
        }
        return null;
    }

    public void nodeChanged(Node updatedNode) {
        for (Node node : resultTableView.getItems()) {
            if (node.getUniqueId().equals(updatedNode.getUniqueId())) {
                node.setTags(updatedNode.getTags());
                resultTableView.refresh();
            }
        }
    }

    public void setTableEntries(List<Node> entries) {
        tableEntries.setAll(entries);
    }
}
