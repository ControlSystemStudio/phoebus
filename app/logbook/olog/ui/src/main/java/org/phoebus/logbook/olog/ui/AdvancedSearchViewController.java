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

package org.phoebus.logbook.olog.ui;

import com.google.common.base.Strings;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.ui.dialog.PopOver;
import org.phoebus.ui.time.TimeRelativeIntervalPane;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;
import org.phoebus.util.time.TimestampFormats;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.logbook.olog.ui.LogbookQueryUtil.Keys;
import static org.phoebus.ui.time.TemporalAmountPane.Type.TEMPORAL_AMOUNTS_AND_NOW;

/**
 * Controller for the advanced search UI in the log applications, i.e. log table view and log calendar view.
 */
public class AdvancedSearchViewController {

    static final Logger logger = Logger.getLogger(AdvancedSearchViewController.class.getName());

    @FXML
    Label levelLabel;

    @FXML
    GridPane timePane;
    @FXML
    TextField startTime;
    @FXML
    TextField endTime;
    @FXML
    TextField searchTitle;
    @FXML
    TextField searchAuthor;

    PopOver timeSearchPopover;

    @FXML
    TextField searchText;

    @FXML
    TextField searchLogbooks;
    PopOver logbookSearchPopover;

    @FXML
    ComboBox<String> levelSelector;

    @FXML
    TextField searchTags;
    PopOver tagSearchPopover;

    private final LogClient logClient;

    private ListSelectionController tagController;
    private ListSelectionController logbookController;

    // Search parameters
    ObservableMap<Keys, String> searchParameters;

    List<String> logbookNames;
    List<String> tagNames;

    @FXML
    private AnchorPane advancedSearchPane;

    public AdvancedSearchViewController(LogClient logClient) {
        this.logClient = logClient;
    }

    @FXML
    public void initialize() {

        levelLabel.setText(LogbookUIPreferences.level_field_name);

        advancedSearchPane.minWidthProperty().set(0);
        advancedSearchPane.maxWidthProperty().set(0);

        VBox timeBox = new VBox();

        TimeRelativeIntervalPane timeSelectionPane = new TimeRelativeIntervalPane(TEMPORAL_AMOUNTS_AND_NOW);

        // TODO needs to be initialized from the values in the search parameters
        TimeRelativeInterval initial = TimeRelativeInterval.of(java.time.Duration.ofHours(8), java.time.Duration.ZERO);
        timeSelectionPane.setInterval(initial);

        HBox hbox = new HBox();
        hbox.setSpacing(5);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        Button apply = new Button();
        apply.setText(Messages.Apply);
        apply.setPrefWidth(80);
        apply.setOnAction((event) -> {
            Platform.runLater(() -> {
                TimeRelativeInterval interval = timeSelectionPane.getInterval();
                if (interval.isStartAbsolute()) {
                    searchParameters.put(Keys.STARTTIME,
                            TimestampFormats.MILLI_FORMAT.format(interval.getAbsoluteStart().get()));
                } else {
                    searchParameters.put(Keys.STARTTIME, TimeParser.format(interval.getRelativeStart().get()));
                }
                if (interval.isEndAbsolute()) {
                    searchParameters.put(Keys.ENDTIME,
                            TimestampFormats.MILLI_FORMAT.format(interval.getAbsoluteEnd().get()));
                } else {
                    searchParameters.put(Keys.ENDTIME, TimeParser.format(interval.getRelativeEnd().get()));
                }
                if (timeSearchPopover.isShowing())
                    timeSearchPopover.hide();
            });
        });
        Button cancel = new Button();
        cancel.setText("Cancel");
        cancel.setPrefWidth(80);
        cancel.setOnAction((event) -> {
            if (timeSearchPopover.isShowing())
                timeSearchPopover.hide();
        });
        hbox.getChildren().addAll(apply, cancel);
        timeBox.getChildren().addAll(timeSelectionPane, hbox);
        timeSearchPopover = new PopOver(timeBox);

        startTime.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                    if (newPropertyValue) {
                        timeSearchPopover.show(timePane);
                    } else if (timeSearchPopover.isShowing()) {
                        timeSearchPopover.hide();
                    }
                });



        endTime.focusedProperty().addListener(
                (ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                    if (newPropertyValue) {
                        timeSearchPopover.show(timePane);
                    } else if (timeSearchPopover.isShowing()) {
                        timeSearchPopover.hide();
                    }
                });

        FXMLLoader logbookSelectionLoader = new FXMLLoader();
        logbookSelectionLoader.setLocation(this.getClass().getResource("ListSelection.fxml"));
        try {
            logbookSelectionLoader.load();
            logbookController = logbookSelectionLoader.getController();
            logbookController.setOnApply((List<String> t) -> {
                Platform.runLater(() -> {
                    if (t.isEmpty()) {
                        searchParameters.remove(Keys.LOGBOOKS);
                    } else {
                        searchParameters.put(Keys.LOGBOOKS, t.stream().collect(Collectors.joining(",")));
                    }
                    if (logbookSearchPopover.isShowing())
                        logbookSearchPopover.hide();
                });
                return true;
            });
            logbookController.setOnCancel((List<String> t) -> {
                if (logbookSearchPopover.isShowing())
                    logbookSearchPopover.hide();
                return true;
            });
            logbookSearchPopover = new PopOver(logbookSelectionLoader.getRoot());
        } catch (IOException e) {
            logger.log(Level.WARNING, "failed to open logbook search dialog", e);
        }

        FXMLLoader tagSelectionLoader = new FXMLLoader();
        tagSelectionLoader.setLocation(this.getClass().getResource("ListSelection.fxml"));
        try {
            tagSelectionLoader.load();
            tagController = tagSelectionLoader.getController();
            tagController.setOnApply((List<String> t) -> {
                Platform.runLater(() -> {
                    if (t.isEmpty()) {
                        searchParameters.remove(Keys.TAGS);
                    } else {
                        searchParameters.put(Keys.TAGS, t.stream().collect(Collectors.joining(",")));
                    }
                    if (tagSearchPopover.isShowing())
                        tagSearchPopover.hide();
                });
                return true;
            });
            tagController.setOnCancel((List<String> t) -> {
                if (tagSearchPopover.isShowing())
                    tagSearchPopover.hide();
                return true;
            });
            tagSearchPopover = new PopOver(tagSelectionLoader.getRoot());
        } catch (IOException e) {
            logger.log(Level.WARNING, "failed to open tag search dialog", e);
        }

        searchTags.setOnMouseClicked(mouseEvent -> {
            if (tagSearchPopover.isShowing()) {
                tagSearchPopover.hide();
            } else {
                tagNames = logClient.listTags().stream().map(Tag::getName).sorted().collect(Collectors.toList());
                tagController.setAvailable(tagNames);
                tagSearchPopover.show(searchTags);
            }
        });

        searchLogbooks.setOnMouseClicked(mouseEvent -> {
            if (logbookSearchPopover.isShowing()) {
                logbookSearchPopover.hide();
            } else {
                logbookNames = logClient.listLogbooks().stream().map(Logbook::getName).sorted().collect(Collectors.toList());
                logbookController.setAvailable(logbookNames);
                logbookSearchPopover.show(searchLogbooks);
            }
        });

        searchText.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                searchParameters.remove(Keys.SEARCH);
            } else {
                searchParameters.put(Keys.SEARCH, newValue);
            }
        });

        searchAuthor.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                searchParameters.remove(Keys.AUTHOR);
            } else {
                searchParameters.put(Keys.AUTHOR, newValue);
            }
        });

        searchTitle.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                searchParameters.remove(Keys.TITLE);
            } else {
                searchParameters.put(Keys.TITLE, newValue);
            }
        });

        List<String> levelList = logClient.listLevels().stream().collect(Collectors.toList());
        levelSelector.getItems().add("");
        levelSelector.getItems().addAll(levelList);
    }

    @FXML
    public void setLevel() {
        if (levelSelector.getSelectionModel().getSelectedItem().isEmpty()) {
            searchParameters.remove(Keys.LEVEL);
        } else {
            searchParameters.put(Keys.LEVEL, levelSelector.getSelectionModel().getSelectedItem());
        }
    }

    public void setSearchParameters(ObservableMap<Keys, String> params) {
        searchParameters = params;
        searchParameters.addListener((MapChangeListener<Keys, String>) change -> Platform.runLater(() -> {
            searchLogbooks.setText(searchParameters.get(Keys.LOGBOOKS));
            searchTags.setText(searchParameters.get(Keys.TAGS));
            String levelValue = searchParameters.get(Keys.LEVEL);
            levelSelector.getSelectionModel().select(levelValue == null ? "" : levelValue);
        }));

        startTime.textProperty().bind(Bindings.valueAt(searchParameters, Keys.STARTTIME));
        endTime.textProperty().bind(Bindings.valueAt(searchParameters, Keys.ENDTIME));
    }

    public AnchorPane getPane() {
        return advancedSearchPane;
    }

    /**
     * Updates the search parameters map based on the query string in the text input field. The purpose is to:
     * to populate the UI elements of the advanced search view using the parameter values inf the query string.
     * At the same time some validation is applied to the level, logbooks and tags parameters. For instance:
     * any invalid logbook name specified by user in the query string will be removed from the list of "selected"
     * logbooks.
     * <p>
     * NOTE: Since this method updates the model, and since the query string text input field is updated based
     * on this model, as a side effect this method may change the text entered by user.
     *
     * @param queryString
     */
    public void updateSearchParamsFromQueryString(String queryString) {
        Map<String, String> queryStringParameters = LogbookQueryUtil.parseHumanReadableQueryString(queryString);
        queryStringParameters.entrySet().stream().forEach(entry -> {
            Keys keys = Keys.findKey(entry.getKey());
            if (keys != null) {
                if (keys.equals(Keys.LEVEL)) {
                    List<String> levels = logClient.listLevels().stream().collect(Collectors.toList());
                    if (levels.contains(entry.getValue())) {
                        searchParameters.put(Keys.LEVEL, entry.getValue());
                        levelSelector.getSelectionModel().select(entry.getValue());
                    } else {
                        searchParameters.remove(Keys.LEVEL);
                        levelSelector.getSelectionModel().select("");
                    }
                } else if (keys.equals(Keys.LOGBOOKS)) {
                    List<String> validatedLogbookNames = getValidatedLogbooksSelection(entry.getValue());
                    if (validatedLogbookNames.isEmpty()) {
                        searchParameters.remove(Keys.LOGBOOKS);
                    } else {
                        searchParameters.put(Keys.LOGBOOKS, validatedLogbookNames.stream().collect(Collectors.joining(",")));
                    }
                    logbookController.setSelected(validatedLogbookNames);
                } else if (keys.equals(Keys.TAGS)) {
                    List<String> validatedTagsNames = getValidatedTagsSelection(entry.getValue());
                    if (validatedTagsNames.isEmpty()) {
                        searchParameters.remove(Keys.TAGS);
                    } else {
                        searchParameters.put(Keys.TAGS, validatedTagsNames.stream().collect(Collectors.joining(",")));
                    }
                    tagController.setSelected(validatedTagsNames);
                } else {
                    searchParameters.put(Keys.findKey(entry.getKey()), entry.getValue());
                }
            }
        });

        // Now remove any search parameters that are not found in the query string, e.g. user has deleted
        // a search parameter when editing the search query text field.
        removedUnwantedSearchParameters(searchParameters, queryStringParameters);
    }

    protected List<String> getValidatedLogbooksSelection(String logbooks) {
        if (Strings.isNullOrEmpty(logbooks)) {
            return Collections.emptyList();
        }
        List<String> validLogbookNames =
                logClient.listLogbooks().stream().map(Logbook::getName).sorted().collect(Collectors.toList());
        List<String> logbooksFromQueryString =
                Arrays.stream(logbooks.split(",")).map(s -> s.trim()).collect(Collectors.toList());
        List<String> validatedLogbookNames =
                logbooksFromQueryString.stream().filter(logbookName -> validLogbookNames.contains(logbookName)).collect(Collectors.toList());
        return validatedLogbookNames;
    }

    protected List<String> getValidatedTagsSelection(String tags) {
        if (Strings.isNullOrEmpty(tags)) {
            return Collections.emptyList();
        }
        List<String> validTagsNames =
                logClient.listTags().stream().map(Tag::getName).sorted().collect(Collectors.toList());
        List<String> logbooksFromQueryString =
                Arrays.stream(tags.split(",")).map(s -> s.trim()).collect(Collectors.toList());
        List<String> validatedLogbookNames =
                logbooksFromQueryString.stream().filter(logbookName -> validTagsNames.contains(logbookName)).collect(Collectors.toList());
        return validatedLogbookNames;
    }

    protected void removedUnwantedSearchParameters(ObservableMap<Keys, String> searchParameters, Map<String, String> queryStringParameters){
        Arrays.stream(Keys.values()).forEach(keys -> {
            if (!queryStringParameters.containsKey(keys.getName())) {
                searchParameters.remove(keys);
            }
        });
    }
}
