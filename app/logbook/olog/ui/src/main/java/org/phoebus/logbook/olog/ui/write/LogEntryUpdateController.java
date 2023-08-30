/*
 * Copyright (C) 2022 European Spallation Source ERIC.
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
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.logbook.Attachment;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogFactory;
import org.phoebus.logbook.LogService;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.olog.ui.AttachmentsViewController;
import org.phoebus.logbook.olog.ui.HelpViewer;
import org.phoebus.logbook.olog.ui.LogbookUIPreferences;
import org.phoebus.logbook.olog.ui.PreviewViewer;
import org.phoebus.logbook.olog.ui.SingleLogEntryDisplayController;
import org.phoebus.logbook.olog.ui.menu.SendToLogBookApp;
import org.phoebus.olog.es.api.OlogProperties;
import org.phoebus.olog.es.api.model.OlogAttachment;
import org.phoebus.olog.es.api.model.OlogLog;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.security.tokens.SimpleAuthenticationToken;
import org.phoebus.ui.dialog.ListSelectionPopOver;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controller for the {@link LogEntryUpdateStage}.
 */
public class LogEntryUpdateController {

    private final LogEntryCompletionHandler completionHandler;

    private final Logger logger = Logger.getLogger(LogEntryUpdateController.class.getName());

    @FXML
    private VBox editorPane;
    @FXML
    private VBox errorPane;
    @FXML
    private Button submitButton;
    @FXML
    private Button cancelButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label completionMessageLabel;

    @SuppressWarnings("unused")
    @FXML
    private AttachmentsViewController attachmentsViewController;
    @SuppressWarnings("unused")
    @FXML
    private LogPropertiesEditorController logPropertiesEditorController;

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

    private final ContextMenu logbookDropDown = new ContextMenu();
    private final ContextMenu tagDropDown = new ContextMenu();

    private final ObservableList<String> selectedLogbooks = FXCollections.observableArrayList();
    private final ObservableList<String> selectedTags = FXCollections.observableArrayList();

    private ObservableList<String> availableLogbooksAsStringList;
    private ObservableList<String> availableTagsAsStringList;
    private Collection<Logbook> availableLogbooks;
    private Collection<Tag> availableTags;

    private ListSelectionPopOver tagsPopOver;
    private ListSelectionPopOver logbooksPopOver;

    private final ObservableList<String> availableLevels = FXCollections.observableArrayList();
    private final SimpleStringProperty titleProperty = new SimpleStringProperty();
    private final SimpleStringProperty descriptionProperty = new SimpleStringProperty();
    private final SimpleStringProperty selectedLevelProperty = new SimpleStringProperty();
    private final SimpleStringProperty usernameProperty = new SimpleStringProperty();
    private final SimpleStringProperty passwordProperty = new SimpleStringProperty();

    private final LogEntry logEntry;

    private final SimpleBooleanProperty updateCredentials = new SimpleBooleanProperty();
    private final ReadOnlyBooleanProperty updateCredentialsProperty;
    private final SimpleBooleanProperty inputValid = new SimpleBooleanProperty(false);

    private final LogFactory logFactory;

    private final SimpleBooleanProperty submissionInProgress =
            new SimpleBooleanProperty(false);

    private final Long logId;

    /**
     * Indicates if user has started editing. Only title and body are used to define dirty state.
     */
    private boolean isDirty = false;

    /**
     * Used to determine if a log entry is dirty. For a new log entry, comparison is made to empty string, for
     * the reply case the original title is copied to this field.
     */
    private String originalTitle = "";


    public LogEntryUpdateController(LogEntry logEntry, LogEntryCompletionHandler logEntryCompletionHandler) {
        this.logId = logEntry.getId();
        this.completionHandler = logEntryCompletionHandler;
        this.logFactory = LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory);
        updateCredentialsProperty = updateCredentials;
        this.logEntry = logEntry;
    }

    @FXML
    public void initialize() {

        // This could be configured in the fxml, but then these UI components would not be visible
        // in Scene Builder.
        completionMessageLabel.textProperty().set("");
        progressIndicator.visibleProperty().bind(submissionInProgress);

        // Remote log service not reachable, so show error pane.
        if (!checkConnectivity()) {
            errorPane.visibleProperty().set(true);
            editorPane.disableProperty().set(true);
            return;
        }

        submitButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        !inputValid.get() || submissionInProgress.get(),
                inputValid, submissionInProgress));
        completionMessageLabel.visibleProperty()
                .bind(Bindings.createBooleanBinding(() -> completionMessageLabel.textProperty().isNotEmpty().get() && !submissionInProgress.get(),
                        completionMessageLabel.textProperty(), submissionInProgress));

        cancelButton.disableProperty().bind(submissionInProgress);

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
                if (userField.getText().isEmpty()) {
                    userField.requestFocus();
                } else if (passwordField.getText().isEmpty()) {
                    passwordField.requestFocus();
                } else if (titleField.getText() == null || titleField.getText().isEmpty()) {
                    titleField.requestFocus();
                } else {
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
            if (newVal.trim().isEmpty()) {
                titleLabel.setTextFill(Color.RED);
            } else {
                titleLabel.setTextFill(Color.BLACK);
            }
            if (!newVal.equals(originalTitle)) {
                isDirty = true;
            }
        });
        titleProperty.set(logEntry.getTitle());

        textArea.textProperty().bindBidirectional(descriptionProperty);
        descriptionProperty.set(logEntry.getDescription() != null ? logEntry.getDescription() : "");
        descriptionProperty.addListener((observable, oldValue, newValue) -> isDirty = true);

        Image tagIcon = ImageCache.getImage(LogEntryUpdateController.class, "/icons/add_tag.png");
        Image logbookIcon = ImageCache.getImage(LogEntryUpdateController.class, "/icons/logbook-16.png");
        Image downIcon = ImageCache.getImage(LogEntryUpdateController.class, "/icons/down_triangle.png");

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
            if (selectedLogbooks.isEmpty()) {
                return "";
            }
            StringBuilder stringBuilder = new StringBuilder();
            selectedLogbooks.forEach(l -> stringBuilder.append(l).append(", "));
            String text = stringBuilder.toString();
            return text.substring(0, text.length() - 2);
        }, selectedLogbooks));

        tagsSelection.textProperty().bind(Bindings.createStringBinding(() -> {
            if (selectedTags.isEmpty()) {
                return "";
            }
            StringBuilder stringBuilder = new StringBuilder();
            selectedTags.forEach(l -> stringBuilder.append(l).append(", "));
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

        inputValid.bind(Bindings.createBooleanBinding(() -> titleProperty.get() != null && !titleProperty.get().isEmpty() &&
                        usernameProperty.get() != null && !usernameProperty.get().isEmpty() &&
                        passwordProperty.get() != null && !passwordProperty.get().isEmpty() &&
                        !selectedLogbooks.isEmpty(),
                titleProperty, usernameProperty, passwordProperty, selectedLogbooks));

        tagsPopOver = ListSelectionPopOver.create(
                (tags, popOver) -> {
                    setSelectedTags(tags, selectedTags);
                    if (popOver.isShowing()) {
                        popOver.hide();
                    }
                },
                (tags, popOver) -> popOver.hide()
        );
        logbooksPopOver = ListSelectionPopOver.create(
                (logbooks, popOver) -> {
                    setSelectedLogbooks(logbooks, selectedLogbooks);
                    if (popOver.isShowing()) {
                        popOver.hide();
                    }
                },
                (logbooks, popOver) -> popOver.hide()
        );

        selectedTags.addListener((ListChangeListener<String>) change -> {
            List<String> newSelection = new ArrayList<>(change.getList());
            tagsPopOver.setAvailable(availableTagsAsStringList, newSelection);
            tagsPopOver.setSelected(newSelection);
        });

        selectedLogbooks.addListener((ListChangeListener<String>) change -> {
            List<String> newSelection = new ArrayList<>(change.getList());
            logbooksPopOver.setAvailable(availableLogbooksAsStringList, newSelection);
            logbooksPopOver.setSelected(newSelection);
        });

        // Note: logbooks and tags are retrieved asynchronously from service
        setupLogbooksAndTags();
        retrieveAttachments();
    }

    /**
     * Handler for Cancel button. Note that any selections in the {@link SelectionService} are
     * cleared to prevent next launch of {@link SendToLogBookApp}
     * to pick them up.
     */
    @FXML
    public void cancel() {
        // Need to clear selections.
        SelectionService.getInstance().clearSelection("");
        ((LogEntryUpdateStage) cancelButton.getScene().getWindow()).handleCloseEditor(isDirty, editorPane);
    }

    @FXML
    public void showHelp() {
        new HelpViewer(LogbookUIPreferences.markup_help).show();
    }

    /**
     * Handler for HTML preview button
     */
    @FXML
    public void showHtmlPreview() {
        new PreviewViewer(getDescription(), attachmentsViewController.getAttachments()).show();
    }


    @FXML
    public void submit() {

        submissionInProgress.set(true);

        JobManager.schedule("Submit Log Entry Update", monitor -> {
            OlogLog ologLog = new OlogLog();
            ologLog.setId(logId);
            ologLog.setTitle(getTitle());
            ologLog.setDescription(getDescription());
            ologLog.setLevel(selectedLevelProperty.get());
            ologLog.setLogbooks(getSelectedLogbooks());
            ologLog.setTags(getSelectedTags());
//            ologLog.setAttachments(attachmentsViewController.getAttachments());
            ologLog.setProperties(logPropertiesEditorController.getProperties());

            LogClient logClient =
                    logFactory.getLogClient(new SimpleAuthenticationToken(usernameProperty.get(), passwordProperty.get()));
            LogEntry result;
            try {
                result = logClient.update(ologLog);
                // Not dirty any more...
                isDirty = false;
                if (result != null) {
                    if (completionHandler != null) {
                        completionHandler.handleResult(result);
                    }
                    // Set username and password in secure store if submission of log entry completes successfully
                    if (LogbookUIPreferences.save_credentials) {
                        // Get the SecureStore. Store username and password.
                        try {
                            SecureStore store = new SecureStore();
                            ScopedAuthenticationToken scopedAuthenticationToken =
                                    new ScopedAuthenticationToken(LogService.AUTHENTICATION_SCOPE, usernameProperty.get(), passwordProperty.get());
                            store.setScopedAuthentication(scopedAuthenticationToken);
                        } catch (Exception ex) {
                            logger.log(Level.WARNING, "Secure Store file not found.", ex);
                        }
                    }
                    // This will close the editor
                    Platform.runLater(this::cancel);
                }
            } catch (LogbookException e) {
                logger.log(Level.WARNING, "Unable to submit log entry", e);
                Platform.runLater(() -> {
                    if (e.getCause() != null && e.getCause().getMessage() != null) {
                        completionMessageLabel.textProperty().setValue(e.getCause().getMessage());
                    } else if (e.getMessage() != null) {
                        completionMessageLabel.textProperty().setValue(e.getMessage());
                    } else {
                        completionMessageLabel.textProperty().setValue(org.phoebus.logbook.Messages.SubmissionFailed);
                    }
                });
            }
            submissionInProgress.set(false);
        });
    }

    @FXML
    public void setLevel() {
        selectedLevelProperty.set(levelSelector.getSelectionModel().getSelectedItem());
    }

    public String getTitle() {
        return titleProperty.get();
    }

    public String getDescription() {
        return descriptionProperty.get();
    }

    @FXML
    public void addLogbooks() {
        logbooksPopOver.show(addLogbooks);
    }

    private void addSelectedLogbook(String logbookName) {
        selectedLogbooks.add(logbookName);
        updateDropDown(logbookDropDown, logbookName, true);
    }

    private void removeSelectedLogbook(String logbookName) {
        selectedLogbooks.remove(logbookName);
        updateDropDown(logbookDropDown, logbookName, false);
    }

    private void setSelectedLogbooks(List<String> proposedLogbooks, List<String> existingLogbooks) {
        setSelected(proposedLogbooks, existingLogbooks, this::addSelectedLogbook, this::removeSelectedLogbook);
    }

    private void setSelected(List<String> proposed, List<String> existing, Consumer<String> addFunction, Consumer<String> removeFunction) {
        List<String> addedTags = proposed.stream()
                .filter(tag -> !existing.contains(tag))
                .collect(Collectors.toList());
        List<String> removedTags = existing.stream()
                .filter(tag -> !proposed.contains(tag))
                .collect(Collectors.toList());
        addedTags.forEach(addFunction);
        removedTags.forEach(removeFunction);
    }

    @FXML
    public void selectLogbooks() {
        if (logbooksDropdownButton.isSelected()) {
            logbookDropDown.show(logbooksSelection, Side.BOTTOM, 0, 0);
        } else {
            logbookDropDown.hide();
        }
    }

    @FXML
    public void addTags() {
        tagsPopOver.show(addTags);
    }

    private void addSelectedTag(String tagName) {
        selectedTags.add(tagName);
        updateDropDown(tagDropDown, tagName, true);
    }

    private void removeSelectedTag(String tagName) {
        selectedTags.remove(tagName);
        updateDropDown(tagDropDown, tagName, false);
    }

    private void setSelectedTags(List<String> proposedTags, List<String> existingTags) {
        setSelected(proposedTags, existingTags, this::addSelectedTag, this::removeSelectedTag);
    }

    @FXML
    public void selectTags() {
        if (tagsDropdownButton.isSelected()) {
            tagDropDown.show(tagsSelection, Side.BOTTOM, 0, 0);
        } else {
            tagDropDown.hide();
        }
    }

    public List<Logbook> getSelectedLogbooks() {
        return availableLogbooks.stream().filter(l -> selectedLogbooks.contains(l.getName())).collect(Collectors.toList());
    }

    public List<Tag> getSelectedTags() {
        return availableTags.stream().filter(t -> selectedTags.contains(t.getName())).collect(Collectors.toList());
    }

    /**
     * Retrieves logbooks and tags from service and populates all the data structures that depend
     * on the result. The call to the remote service is asynchronous.
     */
    private void setupLogbooksAndTags() {
        JobManager.schedule("Fetch Logbooks and Tags", monitor ->
        {
            LogClient logClient =
                    LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory).getLogClient();
            availableLogbooks = logClient.listLogbooks();
            availableLogbooksAsStringList =
                    FXCollections.observableArrayList(availableLogbooks.stream().map(Logbook::getName).collect(Collectors.toList()));
            Collections.sort(availableLogbooksAsStringList);

            List<String> preSelectedLogbooks =
                    logEntry.getLogbooks().stream().map(Logbook::getName).collect(Collectors.toList());
            List<String> defaultLogbooks = Arrays.asList(LogbookUIPreferences.default_logbooks);
            availableLogbooksAsStringList.forEach(logbook -> {
                CheckBox checkBox = new CheckBox(logbook);
                CustomMenuItem newLogbook = new CustomMenuItem(checkBox);
                newLogbook.setHideOnClick(false);
                checkBox.setOnAction(e -> {
                    CheckBox source = (CheckBox) e.getSource();
                    String text = source.getText();
                    if (source.isSelected()) {
                        selectedLogbooks.add(text);
                    } else {
                        selectedLogbooks.remove(text);
                    }
                });
                if (!preSelectedLogbooks.isEmpty() && preSelectedLogbooks.contains(logbook)) {
                    checkBox.setSelected(preSelectedLogbooks.contains(logbook));
                    selectedLogbooks.add(logbook);
                } else if (defaultLogbooks.contains(logbook) && selectedLogbooks.isEmpty()) {
                    checkBox.setSelected(defaultLogbooks.contains(logbook));
                    selectedLogbooks.add(logbook);
                }
                logbookDropDown.getItems().add(newLogbook);
            });

            availableTags = logClient.listTags();
            availableTagsAsStringList =
                    FXCollections.observableArrayList(availableTags.stream().map(Tag::getName).collect(Collectors.toList()));
            Collections.sort(availableLogbooksAsStringList);

            List<String> preSelectedTags =
                    logEntry.getTags().stream().map(Tag::getName).collect(Collectors.toList());
            availableTagsAsStringList.forEach(tag -> {
                CheckBox checkBox = new CheckBox(tag);
                CustomMenuItem newTag = new CustomMenuItem(checkBox);
                newTag.setHideOnClick(false);
                checkBox.setOnAction(e -> {
                    CheckBox source = (CheckBox) e.getSource();
                    String text = source.getText();
                    if (source.isSelected()) {
                        selectedTags.add(text);
                    } else {
                        selectedTags.remove(text);
                    }
                });
                checkBox.setSelected(preSelectedTags.contains(tag));
                if (preSelectedTags.contains(tag)) {
                    selectedTags.add(tag);
                }
                tagDropDown.getItems().add(newTag);
            });

            tagsPopOver.setAvailable(availableTagsAsStringList, selectedTags);
            tagsPopOver.setSelected(selectedTags);
            logbooksPopOver.setAvailable(availableLogbooksAsStringList, selectedLogbooks);
            logbooksPopOver.setSelected(selectedLogbooks);

        });
    }

    private void retrieveAttachments(){
        JobManager.schedule("Fetch attachment data", monitor -> {

            LogClient logClient =
                    LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory).getLogClient();
            Collection<Attachment> attachments = logEntry.getAttachments().stream()
                    .filter((attachment) -> attachment.getName() != null && !attachment.getName().isEmpty())
                    .map((attachment) -> {
                        OlogAttachment fileAttachment = new OlogAttachment();
                        fileAttachment.setContentType(attachment.getContentType());
                        fileAttachment.setThumbnail(false);
                        fileAttachment.setFileName(attachment.getName());
                        try {
                            Path temp = Files.createTempFile("phoebus", attachment.getName());
                            Files.copy(logClient.getAttachment(logEntry.getId(), attachment.getName()), temp, StandardCopyOption.REPLACE_EXISTING);
                            fileAttachment.setFile(temp.toFile());
                            temp.toFile().deleteOnExit();
                        } catch (LogbookException | IOException e) {
                            Logger.getLogger(SingleLogEntryDisplayController.class.getName())
                                    .log(Level.WARNING, "Failed to retrieve attachment " + fileAttachment.getFileName(), e);
                        }
                        return fileAttachment;
                    }).collect(Collectors.toList());
            // Update UI
            Platform.runLater(()->{
                attachmentsViewController.setAttachments(attachments);
            });
        });
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
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Secure Store file not found.", ex);
            }
        });
    }

    /**
     * Updates the logbooks or tags context menu to reflect the state of selected logbooks and tags.
     *
     * @param contextMenu  The context menu to update
     * @param itemName     The logbook or tag name identifying to a context menu item
     * @param itemSelected Indicates whether to select or deselect.
     */
    private void updateDropDown(ContextMenu contextMenu, String itemName, boolean itemSelected) {
        for (MenuItem menuItem : contextMenu.getItems()) {
            CustomMenuItem custom = (CustomMenuItem) menuItem;
            CheckBox check = (CheckBox) custom.getContent();
            if (check.getText().equals(itemName)) {
                check.setSelected(itemSelected);
                break;
            }
        }
    }

    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Checks connectivity to remote service by querying the info end-point. If connection fails,
     * connectionError property is set to <code>true</code>, which should set opacity of the editor pane, and
     * set visibility of error pane.
     */
    private boolean checkConnectivity() {
        LogClient logClient = LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory).getLogClient();
        try {
            logClient.serviceInfo();
            return true;
        } catch (Exception e) {
            Logger.getLogger(SendToLogBookApp.class.getName()).warning("Failed to query logbook service, it may be off-line.");
            return false;
        }
    }
}
