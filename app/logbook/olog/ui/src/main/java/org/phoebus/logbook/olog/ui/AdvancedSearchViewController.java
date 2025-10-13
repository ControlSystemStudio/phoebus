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

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntryLevel;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.Tag;
import org.phoebus.ui.dialog.ListSelectionPopOver;
import org.phoebus.ui.dialog.PopOver;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.time.TimeRelativeIntervalPane;
import org.phoebus.util.text.Strings;
import org.phoebus.util.time.TimeParser;
import org.phoebus.util.time.TimeRelativeInterval;
import org.phoebus.util.time.TimestampFormats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.phoebus.logbook.olog.ui.LogbookQueryUtil.Keys;
import static org.phoebus.ui.time.TemporalAmountPane.Type.TEMPORAL_AMOUNTS_AND_NOW;

/**
 * Controller for the advanced search UI in the log applications, i.e. log table view and log calendar view.
 */
public class AdvancedSearchViewController {

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
    ListSelectionPopOver logbookSearchPopover;

    @FXML
    TextField searchTags;
    ListSelectionPopOver tagSearchPopover;

    private final LogClient logClient;

    @SuppressWarnings("unused")
    @FXML
    private AnchorPane advancedSearchPane;

    @SuppressWarnings("unused")
    @FXML
    private RadioButton sortDescRadioButton;

    @SuppressWarnings("unused")
    @FXML
    private RadioButton sortAscRadioButton;

    @SuppressWarnings("unused")
    @FXML
    private TextField attachmentTypes;

    @SuppressWarnings("unused")
    @FXML
    private TextField selectedLevelsField;

    @SuppressWarnings("unused")
    @FXML
    private ToggleButton levelsToggleButton;

    @SuppressWarnings("unused")
    @FXML
    private GridPane gridPane;

    private final ContextMenu levelsContextMenu = new ContextMenu();

    private final SearchParameters searchParameters;

    private final SimpleObjectProperty<SortOrder> sortOrderProperty =
            new SimpleObjectProperty<>(SortOrder.DESCENDING);
    private final ObservableList<LevelSelection> levelSelections = FXCollections.observableArrayList();
    private final SimpleStringProperty selectedLevelsString = new SimpleStringProperty();
    private final List<String> levelsList = new ArrayList<>();

    private Runnable searchCallback = () -> {
        throw new IllegalStateException("Search callback is not set on AdvancedSearchViewController!");
    };

    public AdvancedSearchViewController(LogClient logClient,
                                        SearchParameters searchParameters) {
        this.logClient = logClient;
        this.searchParameters = searchParameters;
    }

    public void setSearchCallback(Runnable searchCallback) {
        this.searchCallback = searchCallback;
    }

    @FXML
    public void initialize() {

        searchTitle.textProperty().bindBidirectional(this.searchParameters.titleProperty());
        searchTitle.setOnKeyReleased(this::searchOnEnter);
        searchText.textProperty().bindBidirectional(this.searchParameters.textProperty());
        searchText.setOnKeyReleased(this::searchOnEnter);
        searchAuthor.textProperty().bindBidirectional(this.searchParameters.authorProperty());
        searchAuthor.setOnKeyReleased(this::searchOnEnter);
        selectedLevelsField.textProperty().bind(selectedLevelsString);
        levelsToggleButton.setGraphic(new ImageView(ImageCache.getImage(AdvancedSearchViewController.class, "/icons/down_triangle.png")));
        levelsToggleButton.focusedProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (!newVal && !levelsContextMenu.isShowing())
                levelsToggleButton.setSelected(false);
        });

        selectedLevelsString.addListener((obs, o, n) -> {
            this.searchParameters.levelsProperty().setValue(selectedLevelsString.get());
            searchCallback.run();
        });
        searchTags.textProperty().bindBidirectional(this.searchParameters.tagsProperty());
        searchParameters.tagsProperty().addListener(searchOnTextChange);
        searchLogbooks.textProperty().bindBidirectional(this.searchParameters.logbooksProperty());
        searchParameters.logbooksProperty().addListener(searchOnTextChange);
        startTime.textProperty().bindBidirectional(this.searchParameters.startTimeProperty());
        startTime.setOnKeyReleased(this::searchOnEnter);
        endTime.textProperty().bindBidirectional(this.searchParameters.endTimeProperty());
        endTime.setOnKeyReleased(this::searchOnEnter);
        searchParameters.addListener((observable, oldValue, newValue) -> updateControls(newValue));

        attachmentTypes.textProperty().bindBidirectional(this.searchParameters.attachmentsProperty());
        attachmentTypes.setOnKeyReleased(this::searchOnEnter);

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
        apply.setOnAction((event) -> Platform.runLater(() -> {
            TimeRelativeInterval interval = timeSelectionPane.getInterval();
            if (interval.isStartAbsolute()) {
                searchParameters.startTimeProperty().setValue(TimestampFormats.MILLI_FORMAT.format(interval.getAbsoluteStart().get()));
            } else {
                searchParameters.startTimeProperty().setValue(TimeParser.format(interval.getRelativeStart().get()));
            }
            if (interval.isEndAbsolute()) {
                searchParameters.endTimeProperty().setValue(TimestampFormats.MILLI_FORMAT.format(interval.getAbsoluteEnd().get()));
            } else {
                searchParameters.endTimeProperty().setValue(TimeParser.format(interval.getRelativeEnd().get()));
            }
            if (timeSearchPopover.isShowing())
                timeSearchPopover.hide();
            searchCallback.run();
        }));
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

        logbookSearchPopover = ListSelectionPopOver.create(
                (logbooks, popover) -> {
                    String logbooksValue = String.join(",", logbooks);
                    searchParameters.logbooksProperty().setValue(logbooksValue);
                    if (popover.isShowing()) {
                        popover.hide();
                    }
                },
                (logbooks, popover) -> {
                    if (popover.isShowing()) {
                        popover.hide();
                    }
                }
        );

        tagSearchPopover = ListSelectionPopOver.create(
                (tags, popover) -> {
                    String tagsValue = String.join(",", tags);
                    searchParameters.tagsProperty().setValue(tagsValue);
                    if (popover.isShowing()) {
                        popover.hide();
                    }
                },
                (tags, popover) -> {
                    if (popover.isShowing()) {
                        popover.hide();
                    }
                }
        );

        searchTags.setOnMouseClicked(mouseEvent -> {
            if (tagSearchPopover.isShowing()) {
                tagSearchPopover.hide();
            } else {
                List<String> selectedTags = Arrays.stream(searchParameters.tagsProperty().getValueSafe().split(","))
                        .map(String::trim)
                        .filter(it -> !it.isEmpty())
                        .collect(Collectors.toList());
                List<String> availableTags = logClient.listTags().stream()
                        .map(Tag::getName)
                        .sorted()
                        .collect(Collectors.toList());
                tagSearchPopover.setAvailable(availableTags, selectedTags);
                tagSearchPopover.setSelected(selectedTags);
                tagSearchPopover.show(searchTags);
            }
        });

        searchLogbooks.setOnMouseClicked(mouseEvent -> {
            if (logbookSearchPopover.isShowing()) {
                logbookSearchPopover.hide();
            } else {
                List<String> selectedLogbooks = Arrays.stream(searchParameters.logbooksProperty().getValueSafe().split(","))
                        .map(String::trim)
                        .filter(it -> !it.isEmpty())
                        .collect(Collectors.toList());
                List<String> availableLogbooks = logClient.listLogbooks().stream()
                        .map(Logbook::getName)
                        .sorted()
                        .collect(Collectors.toList());
                logbookSearchPopover.setAvailable(availableLogbooks, selectedLogbooks);
                logbookSearchPopover.setSelected(selectedLogbooks);
                logbookSearchPopover.show(searchLogbooks);
            }
        });

        // Fetch levels from service on separate thread
        JobManager.schedule("Get logbook levels", monitor -> {
            levelsList.addAll(logClient.listLevels().stream().map(LogEntryLevel::name).sorted().toList());
            levelsList.forEach(level -> {
                LevelSelection levelSelection = new LevelSelection(level, false);
                levelSelections.add(levelSelection);
                CheckBox checkBox = new CheckBox(level);
                LevelSelectionMenuItem levelSelectionMenuItem = new LevelSelectionMenuItem(checkBox);
                levelSelectionMenuItem.setHideOnClick(false);
                checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                    levelSelection.selected = newValue;
                    setSelectedLevelsString();
                });
                levelsContextMenu.getItems().add(levelSelectionMenuItem);
            });
        });

        sortOrderProperty.addListener(searchOnSortChange);

        sortDescRadioButton.setUserData(SortOrder.DESCENDING);
        sortAscRadioButton.setUserData(SortOrder.ASCENDING);

        ToggleGroup toggleGroup = new ToggleGroup();
        toggleGroup.getToggles().addAll(sortDescRadioButton, sortAscRadioButton);
        toggleGroup.selectToggle(sortDescRadioButton);
        toggleGroup.selectedToggleProperty().addListener((obs, o, n) ->
                sortOrderProperty.set((SortOrder) n.getUserData()));

        gridPane.setOnMouseClicked(e -> levelsToggleButton.setSelected(false));

        // Make sure controls are updated based on search string entered by user
        updateControls(searchParameters.getValue());
    }

    public AnchorPane getPane() {
        return advancedSearchPane;
    }

    /**
     * Updates non-text field controls so that search parameter values are correctly rendered.
     *
     * @param queryString Query string containing search terms and values
     */
    private void updateControls(String queryString) {
        JobManager.schedule("Update controls from server data", monitor -> {
            Map<String, String> queryStringParameters = LogbookQueryUtil.parseHumanReadableQueryString(queryString);
            queryStringParameters.entrySet().forEach(entry -> {
                Keys keys = Keys.findKey(entry.getKey());
                if (keys != null) {
                    if (keys.equals(Keys.LEVEL)) {
                        List<String> validatedLevels = getValidatedLevelsSelection(entry.getValue());
                        if (validatedLevels.isEmpty()) {
                            searchParameters.levelsProperty().setValue(null);
                        } else {
                            String selectedLevels =
                                    String.join(",", validatedLevels);
                            searchParameters.levelsProperty().setValue(selectedLevels);
                        }
                        levelsContextMenu.getItems().forEach(mi -> {
                            LevelSelectionMenuItem levelSelectionMenuItem =
                                    (LevelSelectionMenuItem) mi;
                            levelSelectionMenuItem.setSelected(validatedLevels.contains(levelSelectionMenuItem.getCheckBox().getText()));
                        });
                    } else if (keys.equals(Keys.LOGBOOKS)) {
                        List<String> validatedLogbookNames = getValidatedLogbooksSelection(entry.getValue());
                        if (validatedLogbookNames.isEmpty()) {
                            searchParameters.logbooksProperty().setValue(null);
                        } else {
                            String selectedLogbooks =
                                    String.join(",", validatedLogbookNames);
                            searchParameters.logbooksProperty().setValue(selectedLogbooks);
                        }
                        logbookSearchPopover.setSelected(validatedLogbookNames);
                    } else if (keys.equals(Keys.TAGS)) {
                        List<String> validatedTagsNames = getValidatedTagsSelection(entry.getValue());
                        if (validatedTagsNames.isEmpty()) {
                            searchParameters.tagsProperty().setValue(null);
                        } else {
                            String selectedTags = String.join(",", validatedTagsNames);
                            searchParameters.tagsProperty().setValue(selectedTags);
                        }
                        tagSearchPopover.setSelected(validatedTagsNames);
                    }
                }
            });
        });
    }

    protected List<String> getValidatedLogbooksSelection(String logbooks) {
        if (Strings.isNullOrEmpty(logbooks)) {
            return Collections.emptyList();
        }
        List<String> validLogbookNames =
                logClient.listLogbooks().stream().map(Logbook::getName).sorted().toList();
        List<String> logbooksFromQueryString =
                Arrays.stream(logbooks.split(",")).map(String::trim).toList();
        return logbooksFromQueryString.stream().filter(validLogbookNames::contains).collect(Collectors.toList());
    }

    protected List<String> getValidatedTagsSelection(String tags) {
        if (Strings.isNullOrEmpty(tags)) {
            return Collections.emptyList();
        }
        List<String> validTagsNames =
                logClient.listTags().stream().map(Tag::getName).sorted().toList();
        List<String> logbooksFromQueryString =
                Arrays.stream(tags.split(",")).map(String::trim).toList();
        return logbooksFromQueryString.stream().filter(validTagsNames::contains).collect(Collectors.toList());
    }

    protected List<String> getValidatedLevelsSelection(String levels) {
        if (Strings.isNullOrEmpty(levels)) {
            return Collections.emptyList();
        }
        List<String> levelsFromQueryString =
                Arrays.stream(levels.split(",")).map(String::trim).toList();
        return levelsFromQueryString.stream().filter(levelsList::contains).collect(Collectors.toList());
    }

    public boolean getSortAscending() {
        return sortOrderProperty.get().equals(SortOrder.ASCENDING);
    }

    private void searchOnEnter(KeyEvent e) {
        if (e.getCode() == KeyCode.ENTER) {
            searchCallback.run();
        }
    }

    private final ChangeListener<? super String> searchOnTextChange = (options, oldValue, newValue) -> searchCallback.run();

    private final ChangeListener<? super SortOrder> searchOnSortChange =
            (options, oldValue, newValue) -> searchCallback.run();

    private void setSelectedLevelsString() {
        selectedLevelsString.set(levelSelections.stream().filter(LevelSelection::isSelected)
                .map(LevelSelection::getName).collect(Collectors.joining(",")));
    }

    @SuppressWarnings("unused")
    @FXML
    public void selectLevels() {
        if (levelsToggleButton.isSelected()) {
            levelsContextMenu.show(selectedLevelsField, Side.BOTTOM, 0, 0);
        } else {
            levelsContextMenu.hide();
        }
    }

    /**
     * Encapsulates data needed to construct the level selection context menu.
     */
    private static class LevelSelection {
        private final String name;
        private boolean selected;

        public LevelSelection(String name, boolean selected) {
            this.name = name;
            this.selected = selected;
        }

        public String getName() {
            return name;
        }

        public boolean isSelected() {
            return selected;
        }
    }

    /**
     * Encapsulates items needed to maintain the level selection context menu.
     */
    private static class LevelSelectionMenuItem extends CustomMenuItem {
        private final CheckBox checkBox;

        public LevelSelectionMenuItem(CheckBox checkBox) {
            super(checkBox);
            this.checkBox = checkBox;
        }

        public void setSelected(boolean selected) {
            checkBox.setSelected(selected);
        }

        public CheckBox getCheckBox() {
            return checkBox;
        }
    }

    private enum SortOrder {
        DESCENDING,
        ASCENDING
    }
}
