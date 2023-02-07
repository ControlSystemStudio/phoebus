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
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.DirectoryUtilities;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.Preferences;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil.Keys;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.applications.saveandrestore.ui.HelpViewer;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.util.time.TimestampFormats;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@link SearchWindowController} class provides the controller for SearchWindow.fxml
 *
 * @author <a href="mailto:changj@frib.msu.edu">Genie Jhang</a>
 */

public class SearchWindowController implements Initializable {

    private SearchAndFilterViewController searchAndFilterViewController;
    private List<Node> tableEntries = new ArrayList<>();

    @FXML
    private TextField filterNameTextField;

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
    private TableColumn<Node, Date> lastUpdatedColumn;

    @FXML
    private TableColumn<Node, String> userColumn;

    @FXML
    private Pagination pagination;

    @FXML
    private TextField pageSizeTextField;

    @FXML
    private Label queryLabel;

    @FXML
    private Button saveFilterButton;

    private SaveAndRestoreService saveAndRestoreService;

    private final SimpleIntegerProperty pageSizeProperty =
            new SimpleIntegerProperty(Preferences.search_result_page_size);

    private static final Logger LOG = Logger.getLogger(SearchWindowController.class.getName());

    private final SimpleIntegerProperty hitCountProperty = new SimpleIntegerProperty(0);
    private final SimpleIntegerProperty pageCountProperty = new SimpleIntegerProperty(0);

    private final SimpleStringProperty query = new SimpleStringProperty();

    private final SimpleStringProperty filterNameProperty = new SimpleStringProperty();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        saveAndRestoreService = SaveAndRestoreService.getInstance();
        queryLabel.textProperty().bind(query);

        filterNameTextField.textProperty().bindBidirectional(filterNameProperty);
        saveFilterButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                filterNameProperty.get() == null || filterNameProperty.get().isEmpty(), filterNameProperty));

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
                            searchAndFilterViewController.locateNode(copiedStack);
                        }
                    });
                }
            }
        });

        typeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getImageView(cell.getValue())));
        nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
        commentColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getDescription()));
        lastUpdatedColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper(TimestampFormats.SECONDS_FORMAT.format(cell.getValue().getCreated().toInstant())));
        userColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getUserName()));

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

        query.addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                search();
            }
        });
    }

    public void setSearchAndFilterViewController(SearchAndFilterViewController searchAndFilterViewController) {
        this.searchAndFilterViewController = searchAndFilterViewController;
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

    public void search(String queryString) {
        clearSearchResult();
        if (queryString == null || queryString.isEmpty()) {

            query.set(null);
        } else {
            query.set(queryString);
        }
    }

    private void clearSearchResult() {
        resultTableView.getItems().setAll(Collections.emptyList());
        Platform.runLater(() -> {
            hitCountProperty.set(0);
            pageCountProperty.set(0);
        });
    }

    public void search() {

        if (query.get() == null) {
            return;
        }

        Map<String, String> params =
                SearchQueryUtil.parseHumanReadableQueryString(query.get());

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
                    });
                } else {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.INFORMATION);
                        alert.setTitle(Messages.searchNoResultsTitle);
                        alert.setHeaderText(Messages.searchNoResult);
                        DialogHelper.positionDialog(alert, resultTableView, -300, -200);
                        alert.show();
                        clearSearchResult();
                    });
                }
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError(
                        resultTableView,
                        Messages.errorGeneric,
                        Messages.searchErrorBody,
                        e
                );
                clearSearchResult();
            }
        });
    }

    private Comparator<Node> nodeComparator() {
        return Comparator.comparing(Node::getNodeType)
                .thenComparing((Node n) -> n.getName().toLowerCase());
    }


    @FXML
    public void saveFilter() {

        // Check if the filter name is already used.
        JobManager.schedule("Get All Filters", monitor -> {
            List<Filter> allFilters = saveAndRestoreService.getAllFilters();
            if (allFilters.stream().anyMatch(f -> filterNameProperty.get().equalsIgnoreCase(f.getName()))) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.CONFIRMATION);
                    alert.setTitle(Messages.saveFilter);
                    alert.setHeaderText(null);
                    alert.setContentText(MessageFormat.format(Messages.saveFilterConfirmOverwrite, filterNameProperty.get()));
                    ButtonType buttonTypeOverwrite = new ButtonType(Messages.overwrite);
                    ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(buttonTypeOverwrite, buttonTypeCancel);
                    Optional<ButtonType> overwrite = alert.showAndWait();
                    if (overwrite.isPresent() && overwrite.get() == buttonTypeOverwrite) {
                        saveFilter(filterNameProperty.get());
                    }
                });
            } else {
                saveFilter(filterNameProperty.get());
            }
        });
    }

    private void saveFilter(String name) {
        Filter filter = new Filter();
        filter.setName(name);
        filter.setQueryString(queryLabel.getText());
        try {
            JobManager.schedule("Save Filter", monitor -> {
                saveAndRestoreService.saveFilter(filter);
                searchAndFilterViewController.filterAddedOrUpdated(filter);
            });
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to save filter." + (e.getMessage() != null ? ("Cause: " + e.getMessage()) : ""));
            Platform.runLater(() -> ExceptionDetailsErrorDialog.openError(Messages.errorGeneric, Messages.failedSaveFilter, e));
        }
    }

    public void setFilter(Filter filter) {
        query.set(filter.getQueryString());
        filterNameProperty.set(filter.getName());
    }

    public void clearFilter(Filter filter) {
        if (filterNameProperty.get() != null && filterNameProperty.get().equals(filter.getName())) {
            filterNameProperty.set(null);
        }
    }
}

