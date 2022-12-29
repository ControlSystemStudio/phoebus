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
package org.phoebus.applications.saveandrestore.ui.search;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.applications.saveandrestore.ui.HelpViewer;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.search.SearchQueryUtil.Keys;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.util.time.TimestampFormats;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@link SearchController} class provides the controller for SearchWindow.fxml
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class SearchController implements Initializable {

    private SaveAndRestoreController callerController;
    private List<Node> tableEntries = new ArrayList<>();

    @FXML
    private ComboBox<SearchQuery> queryComboBox;

    @FXML
    private TableView<Node> resultTableView;

    @FXML
    private TableColumn<Node, ImageView> typeColumn;

    @FXML
    private TableColumn<Node, String> nameColumn;

    @FXML
    private TableColumn<Node, String> commentColumn;

    @FXML
    private TableColumn<Node, String> tagsColumn;

    @FXML
    private TableColumn<Node, Date> createdColumn;

    @FXML
    private TableColumn<Node, String> creatorColumn;

    @FXML
    private Pagination pagination;

    @FXML
    private TextField pageSizeTextField;

    private SaveAndRestoreService saveAndRestoreService;

    private final SimpleIntegerProperty pageSizeProperty =
            new SimpleIntegerProperty(Preferences.search_result_page_size);

    private static final Logger LOG = Logger.getLogger(SearchController.class.getName());

    private final SimpleIntegerProperty hitCountProperty = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty pageCountProperty = new SimpleIntegerProperty(0);

    //private final MultivaluedMap<String, String> searchParameters = new MultivaluedHashMap<>();

    private SearchQueryManager searchQueryManager;

    private final ObservableList<SearchQuery> searchQueries = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        saveAndRestoreService = SaveAndRestoreService.getInstance();
        searchQueryManager = SearchQueryManager.getInstance();

        configureComboBox();

        resultTableView.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        pagination.getStylesheets().add(this.getClass().getResource("/pagination.css").toExternalForm());

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
                            Stack<Node> copiedStack = new Stack<>();
                            DirectoryUtilities.CreateLocationStringAndNodeStack(node, false).getValue().forEach(copiedStack::push);
                            callerController.locateNode(copiedStack);
                        }
                    });
                }
            }
        });

        typeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getImageView(cell.getValue())));
        typeColumn.setStyle("-fx-alignment: TOP-CENTER;");
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        nameColumn.setStyle("-fx-alignment: TOP-LEFT;");
        commentColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getDescription()));
        createdColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper(TimestampFormats.SECONDS_FORMAT.format(cell.getValue().getCreated().toInstant())));
        createdColumn.setStyle("-fx-alignment: TOP-RIGHT;");
        createdColumn.getStyleClass().add("timestamp-column");

        creatorColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUserName()));
        creatorColumn.setStyle("-fx-alignment: TOP-RIGHT;");

        tagsColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getTags() == null ?
                "" :
                cell.getValue().getTags().stream().map(Tag::getName).collect(Collectors.joining(System.lineSeparator()))));

        pageSizeTextField.setText(Integer.toString(pageSizeProperty.get()));
        Pattern DIGIT_PATTERN = Pattern.compile("\\d*");
        // This is to accept numerical input only, and at most 3 digits (maximizing search to 999 hits).
        pageSizeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (DIGIT_PATTERN.matcher(newValue).matches()) {
                if ("".equals(newValue)) {
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

        pagination.currentPageIndexProperty().addListener((a, b, c) -> search());
        // Hide the pagination widget if hit count == 0 or page count < 2
        pagination.visibleProperty().bind(Bindings.createBooleanBinding(() -> hitCountProperty.get() > 0 && pagination.pageCountProperty().get() > 1,
                hitCountProperty, pagination.pageCountProperty()));
        pagination.pageCountProperty().bind(pageCountProperty);
        pagination.maxPageIndicatorCountProperty().bind(pageCountProperty);

        queryComboBox.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                search();
            }
        });

        searchQueries.setAll(searchQueryManager.getQueries());
        queryComboBox.itemsProperty().bind(new SimpleObjectProperty<>(searchQueries));
        queryComboBox.getSelectionModel().select(searchQueries.get(0));
        queryComboBox.getEditor().setText(searchQueries.get(0).getQuery());
        search();
    }

    public void setCallerController(SaveAndRestoreController callerController) {
        this.callerController = callerController;
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

    @FXML
    public void showHelp() {
        new HelpViewer().show();
    }

    @FXML
    public void search() {

        String searchQuery = queryComboBox.getEditor().getText();

        Map<String, String> params =
                SearchQueryUtil.parseHumanReadableQueryString(searchQueryManager.getOrAddQuery(searchQuery).getQuery());

        params.put(Keys.FROM.getName(), Integer.toString(pagination.getCurrentPageIndex() * pageSizeProperty.get()));
        params.put(Keys.SIZE.getName(), Integer.toString(pageSizeProperty.get()));

        JobManager.schedule("Save-and-restore Search", monitor -> {
            MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
            params.forEach(map::add);
            try {
                SearchResult searchResult = saveAndRestoreService.search(map);
                if (searchResult.getHitCount() > 0) {
                    Platform.runLater(() -> {
                        hitCountProperty.set(searchResult.getHitCount());
                        pageCountProperty.set(1 + (hitCountProperty.get() / pageSizeProperty.get()));
                        tableEntries.clear();
                        tableEntries.addAll(searchResult.getNodes());
                        tableEntries = tableEntries.stream().sorted(nodeComparator()).collect(Collectors.toList());
                        resultTableView.getItems().setAll(tableEntries);
                        List<SearchQuery> queries = searchQueryManager.getQueries();
                        searchQueries.setAll(queries);
                        queryComboBox.getSelectionModel().select(searchQueries.get(0));
                    });
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.INFORMATION);
                        alert.setTitle(Messages.searchNoResultsTitle);
                        alert.setHeaderText(Messages.searchNoResult);
                        DialogHelper.positionDialog(alert, resultTableView, -300, -200);
                        alert.show();
                    });
                }
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(
                        resultTableView,
                        Messages.errorGeneric,
                        Messages.searchErrorBody,
                        e
                );
            }
        });
    }

    private Comparator<Node> nodeComparator() {
        return Comparator.comparing(Node::getNodeType)
                .thenComparing((Node n) -> n.getName().toLowerCase());
    }

    private void configureComboBox() {
        Font defaultQueryFont = Font.font("Liberation Sans", FontWeight.BOLD, 12);
        Font defaultQueryFontRegular = Font.font("Liberation Sans", FontWeight.NORMAL, 12);
        queryComboBox.setVisibleRowCount(SearchQueryManager.getInstance().getQueryListSize());
        // Needed to customize item rendering, e.g. default query rendered in bold.
        queryComboBox.setCellFactory(
                new Callback<>() {
                    @Override
                    public ListCell<SearchQuery> call(ListView<SearchQuery> param) {
                        return new ListCell<>() {
                            @Override
                            public void updateItem(SearchQuery item,
                                                   boolean empty) {
                                super.updateItem(item, empty);
                                if (item != null) {
                                    setText(item.getQuery().isEmpty() ? "<empty>" : item.getQuery());
                                    if (item.isDefaultQuery()) {
                                        setFont(defaultQueryFont);
                                    } else {
                                        setFont(defaultQueryFontRegular);
                                    }
                                }
                            }
                        };
                    }
                });

        // This is needed for the "editor" part of the ComboBox
        queryComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(SearchQuery query) {
                        if (query == null) {
                            return "";
                        } else {
                            return query.getQuery();
                        }
                    }

                    @Override
                    public SearchQuery fromString(String s) {
                        return new SearchQuery(s);
                    }
                });

    }
}

