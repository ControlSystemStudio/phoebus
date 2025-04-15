/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui.search;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.applications.saveandrestore.RestoreUtil;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.RestoreMode;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreBaseController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.contextmenu.LoginMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.RestoreFromClientMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.RestoreFromServiceMenuItem;
import org.phoebus.applications.saveandrestore.ui.contextmenu.TagGoldenMenuItem;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagUtil;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagWidget;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.util.time.TimestampFormats;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Controller for the search result table.
 */
public class SearchResultTableViewController extends SaveAndRestoreBaseController implements Initializable {

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

    @SuppressWarnings("unused")
    @FXML
    private Pagination pagination;

    @SuppressWarnings("unused")
    @FXML
    private TextField pageSizeTextField;

    @SuppressWarnings("unused")
    @FXML
    private VBox tableUi;

    @SuppressWarnings("unused")
    @FXML
    private VBox progressIndicator;

    private final ObservableList<Node> selectedItemsProperty = FXCollections.observableArrayList();
    private final SimpleBooleanProperty disableUi = new SimpleBooleanProperty();
    private final ObservableList<Node> tableEntries = FXCollections.observableArrayList();
    private final SimpleIntegerProperty hitCountProperty = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty pageCountProperty = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty pageSizeProperty =
            new SimpleIntegerProperty(Preferences.search_result_page_size);
    private String queryString;
    private static final Logger LOGGER = Logger.getLogger(SearchResultTableViewController.class.getName());

    private SaveAndRestoreService saveAndRestoreService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        saveAndRestoreService = SaveAndRestoreService.getInstance();

        tableUi.disableProperty().bind(disableUi);
        progressIndicator.visibleProperty().bind(disableUi);

        pagination.getStylesheets().add(this.getClass().getResource("/pagination.css").toExternalForm());
        resultTableView.getStylesheets().add(getClass().getResource("/save-and-restore-style.css").toExternalForm());

        resultTableView.setRowFactory(tableView -> new TableRow<>() {
            @Override
            protected void updateItem(Node node, boolean empty) {
                super.updateItem(node, empty);
                if (node == null || empty) {
                    setTooltip(null);
                    setOnMouseClicked(null);
                } else {
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
                new LoginMenuItem(this, selectedItemsProperty, () -> ApplicationService.createInstance("credentials_management"));
        MenuItem tagGoldenMenuItem = new TagGoldenMenuItem(this, selectedItemsProperty);

        ImageView snapshotTagsIconImage = new ImageView(new Image(SearchAndFilterViewController.class.getResource("/icons/save-and-restore/snapshot-add_tag.png").toExternalForm()));
        Menu tagMenuItem = new Menu(Messages.contextMenuTags, snapshotTagsIconImage);

        MenuItem addTagMenuItem = TagWidget.AddTagMenuItem();
        addTagMenuItem.setOnAction(event -> TagUtil.addTag(resultTableView.getSelectionModel().getSelectedItems()));
        tagMenuItem.getItems().add(addTagMenuItem);

        RestoreFromClientMenuItem restoreFromClientMenuItem = new RestoreFromClientMenuItem(this, selectedItemsProperty,
                () -> {
                    disableUi.set(true);
                    RestoreUtil.restore(RestoreMode.CLIENT_RESTORE, saveAndRestoreService, selectedItemsProperty.get(0), () -> disableUi.set(false));
                });

        RestoreFromServiceMenuItem restoreFromServiceMenuItem = new RestoreFromServiceMenuItem(this, selectedItemsProperty,
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

        pagination.currentPageIndexProperty().addListener((a, b, c) -> search());
        // Hide the pagination widget if hit count == 0 or page count < 2
        pagination.visibleProperty().bind(Bindings.createBooleanBinding(() -> hitCountProperty.get() > 0 && pagination.pageCountProperty().get() > 1,
                hitCountProperty, pagination.pageCountProperty()));
        pagination.pageCountProperty().bind(pageCountProperty);

        pageCountProperty.bind(Bindings.createIntegerBinding(() ->
                        (int) Math.ceil(Double.valueOf(hitCountProperty.get()) / Double.valueOf(pageSizeProperty.get())),
                hitCountProperty, pageSizeProperty));

        pageSizeTextField.setText(Integer.toString(pageSizeProperty.get()));
        Pattern DIGIT_PATTERN = Pattern.compile("\\d*");
        // This is to accept numerical input only, and at most 3 digits (maximizing search to 999 hits).
        pageSizeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (DIGIT_PATTERN.matcher(newValue).matches()) {
                if (newValue.isEmpty()) {
                    pageSizeProperty.set(Preferences.search_result_page_size);
                } else if (newValue.length() > 3) {
                    pageSizeTextField.setText(oldValue);
                } else {
                    pageSizeProperty.set(Integer.parseInt(newValue));
                }
            } else {
                pageSizeTextField.setText(oldValue);
            }
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

    public void clearTable() {
        tableEntries.clear();
        hitCountProperty.set(0);
    }

    @FXML
    public void search() {
        search(queryString);
    }

    public void search(final String query) {
        queryString = query;
        Map<String, String> searchParams =
                SearchQueryUtil.parseHumanReadableQueryString(queryString);

        LOGGER.log(Level.INFO, "searchParams initial: " + searchParams);

        // Add pagination parameters to the search parameters
        searchParams.put(SearchQueryUtil.Keys.SIZE.getName(), Integer.toString(
                pageSizeProperty.get()));
        searchParams.put(SearchQueryUtil.Keys.FROM.getName(), Integer.toString(
                pageSizeProperty.get() *  pagination.getCurrentPageIndex()));

        LOGGER.log(Level.INFO, "searchParams default: " + searchParams);

        JobManager.schedule("Save-and-restore Search", monitor -> {
            MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
            searchParams.forEach(map::add);
            try {
                LOGGER.log(Level.INFO, "search map default: " + map);

                // Get individual search words from "Description/Comment" and
                // "Node Name" fields.
                // Alter the search parameters, prepare to perform AND search with
                // all words in their respective fields.
                String[] descSearchWords;
                String descSearchKey = SearchQueryUtil.Keys.DESC.getName();
                descSearchWords = splitSearchWords(map, descSearchKey);
                map = alterSearchParams(map, descSearchKey, descSearchWords);

                String[] nameSearchWords;
                String nameSearchKey = SearchQueryUtil.Keys.NAME.getName();
                nameSearchWords = splitSearchWords(map, nameSearchKey);
                map = alterSearchParams(map, nameSearchKey, nameSearchWords);

                LOGGER.log(Level.INFO, "search map altered: " + map);

                // Call the REST API search
                SearchResult searchResult = saveAndRestoreService.search(map);
                LOGGER.log(Level.INFO, "Initial hitCount: " + searchResult.getHitCount());

                // Perform an additional search of the results, matching all words in
                // each field
                SearchResult descSearchResult = performSubSearchAND(searchResult,
                        descSearchKey,
                        descSearchWords);
                LOGGER.log(Level.INFO, "desc hitCount: " + descSearchResult.getHitCount());

                SearchResult nameSearchResult = performSubSearchAND(descSearchResult,
                        nameSearchKey,
                        nameSearchWords);
                LOGGER.log(Level.INFO, "name hitCount: " + nameSearchResult.getHitCount());

                SearchResult finalSearchResult;
                if (descSearchWords.length > 1 || nameSearchWords.length > 1) {
                    finalSearchResult = fillResultsPages(nameSearchResult);
                    LOGGER.log(Level.INFO, "Final hitCount: " +
                            finalSearchResult.getHitCount());
                } else {
                    finalSearchResult = nameSearchResult;
                }

                // Fill the results table, the pages should automatically update
                if (finalSearchResult.getHitCount() > 0) {
                    Platform.runLater(() -> {
                        tableEntries.setAll(finalSearchResult.getNodes());
                        hitCountProperty.set(finalSearchResult.getHitCount());
                        LOGGER.log(Level.INFO, "Page count: " + pageCountProperty.get());
                    });
                } else {
                    Platform.runLater(() -> tableEntries.setAll(Collections.emptyList()));
                }
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(
                        Messages.errorGeneric,
                        Messages.searchErrorBody,
                        e
                );
                tableEntries.setAll(Collections.emptyList());
            }
        });
    }

    String[] splitSearchWords(MultivaluedMap<String, String> searchParams,
                String searchKey) {

        String[] searchWords;
        if (searchParams.containsKey(searchKey)) {
            LOGGER.log(Level.INFO, "splitSearchWords");
            searchWords = searchParams.getFirst(searchKey).split(",");
            LOGGER.log(Level.INFO, "  searchWords.length: " + searchWords.length);
        } else {
            searchWords = new String[0];
        }

        return searchWords;
    }

    String getLongestWord(String[] multipleWords) {
        // Choose the longest word to initially search with, will hopefully reduce
        // number of returned results
        String longestWord = multipleWords[0];
        Integer longestLength = longestWord.length();
        for (String searchWord : multipleWords) {
            // LOGGER.log(Level.INFO, "   * searchWord: " + searchWord);
            // LOGGER.log(Level.INFO, "   --> length:   " + searchWord.length());
            if (searchWord.length() > longestLength) {
                longestWord = searchWord;
                longestLength = searchWord.length();
            }
        }

        LOGGER.log(Level.INFO, "   * longestWord: " + longestWord);
        return longestWord;
    }

    MultivaluedMap<String, String> alterSearchParams(MultivaluedMap<String, String> searchParams,
                                          String searchKey,
                                          String[] searchWords) {
        if (searchWords.length > 0) {
            // Choose just the longest word to query the REST search with, otherwise
            // all words are searched for as an OR
            String initialSearchWord = getLongestWord(searchWords);

            // Add wildcard before and after search word - the REST search can then
            // search for partial words
            initialSearchWord = "*".concat(initialSearchWord.concat("*"));
            searchParams.put(searchKey, Collections.singletonList(initialSearchWord));
        }

        // There seems to be a default search result limit of 100, set to something big
        // when searching for multiple words
        if (searchWords.length > 1) {
            searchParams.put(SearchQueryUtil.Keys.SIZE.getName(),
                    Collections.singletonList("10000"));
            searchParams.put(SearchQueryUtil.Keys.FROM.getName(),
                    Collections.singletonList("0"));
        }

        return searchParams;
    }

    SearchResult performSubSearchAND(SearchResult searchResult,
                                     String searchKey, String[] searchWords) {

        LOGGER.log(Level.INFO, "searchResult:  " + searchResult);
        LOGGER.log(Level.INFO, "searchKey:     " + searchKey);
        LOGGER.log(Level.INFO, "searchWords:   " + searchWords);

        if (searchWords.length > 1) {
            List<Node> matchingNodes = new ArrayList<>(List.of());
            Integer newHitCount = 0;

            // Loop over each search result
            for (Node node : searchResult.getNodes()) {

                Boolean goodMatch = true;
                String description;
                if (searchKey == SearchQueryUtil.Keys.DESC.getName()) {
                    description = node.getDescription();
                } else if (searchKey == SearchQueryUtil.Keys.NAME.getName()) {
                    description = node.getName();
                } else {
                    description = "";
                }
                LOGGER.log(Level.INFO, "--> description: " + description);

                // Loop over each search word
                for (String searchWord : searchWords) {
                    if (!description.toLowerCase().contains(searchWord)) {
                        goodMatch = false;
                    }
                }
                LOGGER.log(Level.INFO, "       --> goodMatch: " + goodMatch);

                if (goodMatch) {
                    matchingNodes.add(node);
                    newHitCount++;
                }
            }
            LOGGER.log(Level.INFO, "newHitCount: " + newHitCount);

            for (Node node : matchingNodes) {
                String description;
                if (searchKey == SearchQueryUtil.Keys.DESC.getName()) {
                    description = node.getDescription();
                } else if (searchKey == SearchQueryUtil.Keys.NAME.getName()) {
                    description = node.getName();
                } else {
                    description = "";
                }
                LOGGER.log(Level.INFO, "*** Final search results for: " + searchKey +
                        ": " + description);
            }

            searchResult.setNodes(matchingNodes);
            searchResult.setHitCount(newHitCount);
        }

        return searchResult;
    }

    SearchResult fillResultsPages(SearchResult searchResult) {

        Integer pageSize = pageSizeProperty.get();
        Integer pageIndex = pagination.getCurrentPageIndex();
        Integer resultFrom = pageIndex * pageSize;
        LOGGER.log(Level.INFO, "pageSize:   " + pageSize);
        LOGGER.log(Level.INFO, "pageIndex:  " + pageIndex);
        LOGGER.log(Level.INFO, "resultFrom: " + resultFrom);

        List<Node> matchingNodes = new ArrayList<>(List.of());
        Integer newHitCount = 0;
        for (Node node : searchResult.getNodes()) {
            if (newHitCount >= resultFrom && newHitCount < resultFrom + pageSize) {
                matchingNodes.add(node);
            }
            newHitCount++;
        }
        LOGGER.log(Level.INFO, "newHitCount: " + newHitCount);

        // Only this subset of the matching nodes will be displayed in the
        // results table
        searchResult.setNodes(matchingNodes);
        searchResult.setHitCount(newHitCount);

        return searchResult;
    }

    /**
     * Search with a unique ID
     * Results will be 0 or 1 entry
     * Fill results table
     */
    void uniqueIdSearch(final String uniqueIdString) {
        LOGGER.log(Level.INFO, "uniqueIdSearch() called with: uniqueIdString = " + uniqueIdString);
        try {
            /* Search with the uniqueID */
            Node uniqueIdNode = SaveAndRestoreService.getInstance().getNode(uniqueIdString);
            LOGGER.log(Level.INFO, "uniqueIDNode: " + uniqueIdNode);

            /* Check that there are results, then fill table - should be at most one result */
            if (uniqueIdNode != null) {
                LOGGER.log(Level.INFO, "uniqueIdNode.getName(): " + uniqueIdNode.getName());
                Platform.runLater(() -> {
                    tableEntries.setAll(List.of(uniqueIdNode));
                    hitCountProperty.set(1);
                });
            /* Clear the results table if no record returned */
            } else {
                Platform.runLater(tableEntries::clear);
                hitCountProperty.set(0);
            }
        } catch (Exception e) {
            ExceptionDetailsErrorDialog.openError(
                    resultTableView,
                    Messages.errorGeneric,
                    Messages.searchErrorBody,
                    e
            );
            /* Clear the results table if there's an error */
            tableEntries.clear();
            hitCountProperty.set(0);
        }
    }

    /**
     * Retrieves a filter from the service and loads then performs a search for matching {@link Node}s. If
     * the filter does not exist, or if retrieval fails, an error dialog is shown.
     * @param filterId Unique id of an existing {@link Filter}.
     */
    public void loadFilter(String filterId){
        try {
            List<Filter> filters = saveAndRestoreService.getAllFilters();
            Optional<Filter> filter = filters.stream().filter(f -> f.getName().equals(filterId)).findFirst();
            if(filter.isPresent()){
                search(filter.get().getQueryString());
            }
            else{
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(Messages.errorGeneric);
                alert.setHeaderText(MessageFormat.format(Messages.failedGetSpecificFilter, filterId));
                DialogHelper.positionDialog(alert, resultTableView, -100, -100);
                alert.show();
            }
        } catch (Exception e) {
            ExceptionDetailsErrorDialog.openError(resultTableView, Messages.errorGeneric, MessageFormat.format(Messages.failedGetSpecificFilter, filterId), e);
        }
    }
}
