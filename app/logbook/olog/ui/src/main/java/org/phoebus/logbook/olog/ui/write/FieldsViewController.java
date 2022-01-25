/*
 * Copyright (C) 2019 European Spallation Source ERIC.
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

package org.phoebus.logbook.olog.ui.write;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogService;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.olog.ui.LogbookUIPreferences;
import org.phoebus.logbook.olog.ui.Messages;
import org.phoebus.olog.es.api.OlogProperties;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.application.PhoebusApplication;
import org.phoebus.ui.dialog.ListSelectionDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static org.phoebus.ui.application.PhoebusApplication.logger;

/**
 * Controller for the main part of the log entry editor. Here user specifies credentials,
 * title, body, logbooks, tags and level selections. Input is managed in observable objects and
 * offered through accessors to the {@link LogEntryEditorController}.
 */
public class FieldsViewController implements Initializable {

    public static final String USERNAME_TAG = "username";
    public static final String PASSWORD_TAG = "password";

    @FXML
    private VBox root;
    @FXML
    private Label userFieldLabel;
    @FXML
    private Label passwordFieldLabel;
    @FXML
    private TextField userField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label levelLabel;
    @FXML
    private TextField dateField;
    @FXML
    private ComboBox<String> levelSelector;
    @FXML
    private Label titleLabel;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea textArea;
    @FXML
    private Button addLogbooks;
    @FXML
    private Button addTags;
    @FXML
    private ToggleButton logbooksDropdownButton;
    @FXML
    private ToggleButton tagsDropdownButton;
    @FXML
    private Label logbooksLabel;
    @FXML
    private TextField logbooksSelection;
    @FXML
    private TextField tagsSelection;

    private ContextMenu logbookDropDown = new ContextMenu();
    private ContextMenu tagDropDown = new ContextMenu();

    private ObservableList<String> selectedLogbooks = FXCollections.observableArrayList();
    private ObservableList<String> selectedTags = FXCollections.observableArrayList();

    private ObservableList<String> availableLogbooksAsStringList;
    private ObservableList<String> availableTagsAsStringList;
    private Collection<Logbook> availableLogbooks;
    private Collection<Tag> availableTags;

    private ObservableList<String> availableLevels = FXCollections.observableArrayList();
    private SimpleStringProperty titleProperty = new SimpleStringProperty();
    private SimpleStringProperty descriptionProperty = new SimpleStringProperty();
    private SimpleStringProperty selectedLevelProperty = new SimpleStringProperty();
    private SimpleStringProperty usernameProperty = new SimpleStringProperty();
    private SimpleStringProperty passwordProperty = new SimpleStringProperty();

    private LogEntry logEntry;

    private final SimpleBooleanProperty updateCredentials = new SimpleBooleanProperty();
    private final ReadOnlyBooleanProperty updateCredentialsProperty;
    private SimpleBooleanProperty inputValid = new SimpleBooleanProperty(false);

    public FieldsViewController(LogEntry logEntry) {
        this.logEntry = logEntry;
        updateCredentialsProperty = updateCredentials;
    }

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        userField.textProperty().bindBidirectional(usernameProperty);
        userField.textProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (newVal.trim().isEmpty())
                userFieldLabel.setTextFill(Color.RED);
            else
                userFieldLabel.setTextFill(Color.BLACK);
        });

        passwordField.textProperty().bindBidirectional(passwordProperty);
        passwordField.textProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (newVal.trim().isEmpty())
                passwordFieldLabel.setTextFill(Color.RED);
            else
                passwordFieldLabel.setTextFill(Color.BLACK);
        });

        updateCredentialsProperty.addListener((changeListener, oldVal, newVal) ->
        {
            // This call back should be running on a background thread. Perform contents on JavaFX application thread.
            Platform.runLater(() ->
            {
                userField.setText(usernameProperty.get());
                passwordField.setText(passwordProperty.get());

                // Put focus on first required field that is empty.
                if (userField.getText().isEmpty()){
                    userField.requestFocus();
                }
                else if (passwordField.getText().isEmpty()){
                    passwordField.requestFocus();
                }
                else if(titleField.getText() == null || titleField.getText().isEmpty()){
                    titleField.requestFocus();
                }
                else{
                    textArea.requestFocus();
                }
            });
        });
        if (LogbookUIPreferences.save_credentials) {
            fetchStoredUserCredentials();
        }

        levelLabel.setText(LogbookUIPreferences.level_field_name);
        // Sites may wish to define a different meaning and name for the "level" field.
        OlogProperties ologProperties = new OlogProperties();
        String[] levelList = ologProperties.getPreferenceValue("levels").split(",");
        availableLevels.addAll(Arrays.asList(levelList));
        levelSelector.setItems(availableLevels);
        selectedLevelProperty.set(logEntry.getLevel() != null ? logEntry.getLevel() : availableLevels.get(0));

        levelSelector.getSelectionModel().select(selectedLevelProperty.get());

        dateField.setText(TimestampFormats.DATE_FORMAT.format(Instant.now()));

        titleField.textProperty().bindBidirectional(titleProperty);
        titleProperty.addListener((changeListener, oldVal, newVal) ->
        {
            if (newVal.trim().isEmpty())
                titleLabel.setTextFill(Color.RED);
            else
                titleLabel.setTextFill(Color.BLACK);
        });
        titleProperty.set(logEntry.getTitle());

        textArea.textProperty().bindBidirectional(descriptionProperty);
        descriptionProperty.set(logEntry.getDescription() != null ? logEntry.getDescription() : "");

        Image tagIcon = ImageCache.getImage(FieldsViewController.class, "/icons/add_tag.png");
        Image logbookIcon = ImageCache.getImage(FieldsViewController.class, "/icons/logbook-16.png");
        Image downIcon = ImageCache.getImage(FieldsViewController.class, "/icons/down_triangle.png");

        addLogbooks.setGraphic(new ImageView(logbookIcon));
        addTags.setGraphic(new ImageView(tagIcon));
        logbooksDropdownButton.setGraphic(new ImageView(downIcon));
        tagsDropdownButton.setGraphic(new ImageView(downIcon));

        logbooksSelection.textProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (newVal.trim().isEmpty())
                logbooksLabel.setTextFill(Color.RED);
            else
                logbooksLabel.setTextFill(Color.BLACK);
        });

        logbooksSelection.textProperty().bind(Bindings.createStringBinding(() -> {
            if(selectedLogbooks.isEmpty()){
                return "";
            }
            StringBuilder stringBuilder = new StringBuilder();
            selectedLogbooks.stream().forEach(l -> stringBuilder.append(l).append(", "));
            String text = stringBuilder.toString();
            return text.substring(0, text.length() - 2);
        }, selectedLogbooks));

        tagsSelection.textProperty().bind(Bindings.createStringBinding(() -> {
            if(selectedTags.isEmpty()){
                return "";
            }
            StringBuilder stringBuilder = new StringBuilder();
            selectedTags.stream().forEach(l -> stringBuilder.append(l).append(", "));
            String text = stringBuilder.toString();
            return text.substring(0, text.length() - 2);
        }, selectedTags));

        logbooksDropdownButton.focusedProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (!newVal && !tagDropDown.isShowing() && !logbookDropDown.isShowing())
                logbooksDropdownButton.setSelected(false);
        });

        tagsDropdownButton.focusedProperty().addListener((changeListener, oldVal, newVal) ->
        {
            if (!newVal && !tagDropDown.isShowing() && !tagDropDown.isShowing())
                tagsDropdownButton.setSelected(false);
        });

        inputValid.bind(Bindings.createBooleanBinding(() -> {
                    return titleProperty.get() != null && !titleProperty.get().isEmpty() &&
                            usernameProperty.get() != null && !usernameProperty.get().isEmpty() &&
                            passwordProperty.get() != null && !passwordProperty.get().isEmpty() &&
                            !selectedLogbooks.isEmpty();},
                titleProperty, usernameProperty, passwordProperty, selectedLogbooks));

        // Note: logbooks and tags are retrieved asynchronously from service
        setupLogbooksAndTags();
    }

    @FXML
    public void setLevel() {
        selectedLevelProperty.set(levelSelector.getSelectionModel().getSelectedItem());
    }

    public String getSelectedLevel(){
        return selectedLevelProperty.get();
    }

    public TextArea getTextArea() {
        return textArea;
    }

    public String getTitle(){
        return titleProperty.get();
    }

    public String getDescription(){
        return descriptionProperty.get();
    }

    @FXML
    public void addLogbooks(){
        ListSelectionDialog select =
                new ListSelectionDialog(root.getScene().getRoot(),
                        Messages.LogbooksTitle,
                        this::getAvailableLogbooksAsStringList,
                        this::getSelectedLogbooksAsStringList,
                        this::addSelectedLogbook,
                        this::removeSelectedLogbook);
        select.showAndWait();
    }

    private ObservableList<String> getAvailableLogbooksAsStringList(){
        return availableLogbooksAsStringList;
    }

    private ObservableList<String> getSelectedLogbooksAsStringList(){
        return selectedLogbooks;
    }

    private boolean addSelectedLogbook(String logbookName){
        selectedLogbooks.add(logbookName);
        updateDropDown(logbookDropDown, logbookName, true);
        return true;
    }

    private boolean removeSelectedLogbook(String logbookName){
        selectedLogbooks.remove(logbookName);
        updateDropDown(logbookDropDown, logbookName, false);
        return true;
    }

    @FXML
    public void selectLogbooks(){
        if (logbooksDropdownButton.isSelected()){
            logbookDropDown.show(logbooksSelection, Side.BOTTOM, 0, 0);
        }
        else{
            logbookDropDown.hide();
        }
    }

    @FXML
    public void addTags(){
        ListSelectionDialog select =
                new ListSelectionDialog(root.getScene().getRoot(),
                        Messages.TagsTitle,
                        this::getTagsAsStringList,
                        this::getSelectedTagsAsStringList,
                        this::addSelectedTag,
                        this::removeSelectedTag);
        select.showAndWait();
    }

    private ObservableList<String> getTagsAsStringList(){
        return availableTagsAsStringList;
    }

    private ObservableList<String> getSelectedTagsAsStringList(){
        return selectedTags;
    }

    private boolean addSelectedTag(String tagName){
        selectedTags.add(tagName);
        updateDropDown(tagDropDown, tagName, true);
        return true;
    }

    private boolean removeSelectedTag(String tagName){
        selectedTags.remove(tagName);
        updateDropDown(tagDropDown, tagName, false);
        return true;
    }

    @FXML
    public void selectTags(){
        if (tagsDropdownButton.isSelected()){
            tagDropDown.show(tagsSelection, Side.BOTTOM, 0, 0);
        }
        else{
            tagDropDown.hide();
        }
    }

    public List<Logbook> getSelectedLogbooks(){
        List<Logbook> logbooks =
                availableLogbooks.stream().filter(l -> selectedLogbooks.contains(l.getName())).collect(Collectors.toList());
        return logbooks;
    }

    public List<Tag> getSelectedTags(){
        List<Tag> tags =
                availableTags.stream().filter(t -> selectedTags.contains(t.getName())).collect(Collectors.toList());
        return tags;
    }

    /**
     * Retrieves logbooks and tags from service and populates all the data structures that depend
     * on the result. The call to the remote service is asynchronous.
     */
    private void setupLogbooksAndTags(){
        JobManager.schedule("Fetch Logbooks and Tags", monitor ->
        {
            LogClient logClient =
                    LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory).getLogClient();
            availableLogbooks = logClient.listLogbooks();
            availableLogbooksAsStringList =
                    FXCollections.observableArrayList(availableLogbooks.stream().map(logbook -> logbook.getName()).collect(Collectors.toList()));
            Collections.sort(availableLogbooksAsStringList);

            List<String> preSelectedLogbooks =
                    logEntry.getLogbooks().stream().map(l -> l.getName()).collect(Collectors.toList());
            List<String> defaultLogbooks = Arrays.asList(LogbookUIPreferences.default_logbooks);
            availableLogbooksAsStringList.forEach(logbook -> {
                CheckBox checkBox = new CheckBox(logbook);
                CustomMenuItem newLogbook = new CustomMenuItem(checkBox);
                newLogbook.setHideOnClick(false);
                checkBox.setOnAction(e -> {
                    CheckBox source = (CheckBox) e.getSource();
                    String text = source.getText();
                    if (source.isSelected())
                    {
                        selectedLogbooks.add(text);
                    }
                    else
                    {
                        selectedLogbooks.remove(text);
                    }
                });
                if(!preSelectedLogbooks.isEmpty() && preSelectedLogbooks.contains(logbook)){
                    checkBox.setSelected(preSelectedLogbooks.contains(logbook));
                    selectedLogbooks.add(logbook);
                }
                else if(defaultLogbooks.contains(logbook)){
                    checkBox.setSelected(defaultLogbooks.contains(logbook));
                    selectedLogbooks.add(logbook);
                }
                logbookDropDown.getItems().add(newLogbook);
            });

            availableTags = logClient.listTags();
            availableTagsAsStringList =
                    FXCollections.observableArrayList(availableTags.stream().map(tag -> tag.getName()).collect(Collectors.toList()));
            Collections.sort(availableLogbooksAsStringList);

            List<String> preSelectedTags =
                    logEntry.getTags().stream().map(t -> t.getName()).collect(Collectors.toList());
            availableTagsAsStringList.forEach(tag -> {
                CheckBox checkBox = new CheckBox(tag);
                CustomMenuItem newTag = new CustomMenuItem(checkBox);
                newTag.setHideOnClick(false);
                checkBox.setOnAction(e -> {
                    CheckBox source = (CheckBox) e.getSource();
                    String text = source.getText();
                    if (source.isSelected())
                    {
                        selectedTags.add(text);
                    }
                    else
                    {
                        selectedTags.remove(text);
                    }
                });
                checkBox.setSelected(preSelectedTags.contains(tag));
                if(preSelectedTags.contains(tag)){
                    selectedTags.add(tag);
                }
                tagDropDown.getItems().add(newTag);
            });
        });
    }

    public String getUsernameProperty(){
        return usernameProperty.get();
    }

    public String getPasswordProperty(){
        return passwordProperty.get();
    }

    public void fetchStoredUserCredentials() {
        // Perform file IO on background thread.
        JobManager.schedule("Access Secure Store", monitor ->
        {
            // Get the SecureStore. Retrieve username and password.
            try {
                SecureStore store = new SecureStore();
                ScopedAuthenticationToken scopedAuthenticationToken = store.getScopedAuthenticationToken(LogService.AUTHENTICATION_SCOPE);
                // Could be accessed from JavaFX Application Thread when updating, so synchronize.
                synchronized (usernameProperty) {
                    usernameProperty.set(scopedAuthenticationToken == null ? "" : scopedAuthenticationToken.getUsername());
                }
                synchronized (passwordProperty) {
                    passwordProperty.set(scopedAuthenticationToken == null ? "" : scopedAuthenticationToken.getPassword());
                }
                // Let anyone listening know that their credentials are now out of date.
                updateCredentials.set(true);
                //checkIfReadyToSubmit();
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Secure Store file not found.", ex);
            }
        });
    }

    public SimpleBooleanProperty getInputValidProperty(){
        return inputValid;
    }

    /**
     * Updates the logbooks or tags context menu to reflect the state of selected logbooks and tags.
     * @param contextMenu The context menu to update
     * @param itemName The logbook or tag name identifying to a context menu item
     * @param itemSelected Indicates whether to select or deselect.
     */
    private void updateDropDown(ContextMenu contextMenu, String itemName, boolean itemSelected){
        for (MenuItem menuItem : contextMenu.getItems())
        {
            CustomMenuItem custom = (CustomMenuItem) menuItem;
            CheckBox check = (CheckBox) custom.getContent();
            if(check.getText().equals(itemName)){
                check.setSelected(itemSelected);
                break;
            }
        }
    }
}
