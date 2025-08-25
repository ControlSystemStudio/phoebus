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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;


import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.phoebus.logbook.olog.ui.LogbookUIPreferences;

import javafx.util.Callback;
import javafx.util.StringConverter;
import org.phoebus.framework.autocomplete.Proposal;
import org.phoebus.framework.autocomplete.ProposalProvider;
import org.phoebus.framework.autocomplete.ProposalService;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.logbook.LogClient;
import org.phoebus.logbook.LogEntry;
import org.phoebus.logbook.LogEntryLevel;
import org.phoebus.logbook.LogFactory;
import org.phoebus.logbook.LogService;
import org.phoebus.logbook.LogTemplate;
import org.phoebus.logbook.Logbook;
import org.phoebus.logbook.LogbookException;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.logbook.Tag;
import org.phoebus.logbook.olog.ui.HelpViewer;
import org.phoebus.logbook.olog.ui.Messages;
import org.phoebus.logbook.olog.ui.PreviewViewer;
import org.phoebus.olog.es.api.model.OlogLog;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.AuthenticationScope;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.security.tokens.SimpleAuthenticationToken;
import org.phoebus.ui.Preferences;
import org.phoebus.ui.autocomplete.AutocompleteMenu;
import org.phoebus.ui.dialog.ListSelectionPopOver;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Controller for the {@link LogEntryEditorStage}.
 */
public class LogEntryEditorController {

    private final Logger logger = Logger.getLogger(LogEntryEditorController.class.getName());

    @FXML
    @SuppressWarnings("unused")
    private VBox editorPane;
    @FXML
    @SuppressWarnings("unused")
    private VBox errorPane;
    @FXML
    @SuppressWarnings("unused")
    private Button submitButton;
    @FXML
    @SuppressWarnings("unused")
    private Button cancelButton;
    @FXML
    @SuppressWarnings("unused")
    private ProgressIndicator progressIndicator;
    @FXML
    @SuppressWarnings("unused")
    private Label completionMessageLabel;

    @SuppressWarnings("unused")
    @FXML
    private AttachmentsEditorController attachmentsEditorController;
    @SuppressWarnings("unused")
    @FXML
    private LogPropertiesEditorController logPropertiesEditorController;

    @FXML
    @SuppressWarnings("unused")
    private Label userFieldLabel;
    @FXML
    @SuppressWarnings("unused")
    private Label passwordFieldLabel;
    @FXML
    @SuppressWarnings("unused")
    private TextField userField;
    @FXML
    @SuppressWarnings("unused")
    private TextField dateField;
    @FXML
    @SuppressWarnings("unused")
    private PasswordField passwordField;
    @FXML
    @SuppressWarnings("unused")
    private Label levelLabel;
    @FXML
    @SuppressWarnings("unused")
    private ComboBox<String> levelSelector;
    @FXML
    @SuppressWarnings("unused")
    private Label titleLabel;
    @FXML
    @SuppressWarnings("unused")
    private TextField titleField;
    @FXML
    @SuppressWarnings("unused")
    private TextArea textArea;
    @FXML
    @SuppressWarnings("unused")
    private Button addLogbooks;
    @FXML
    @SuppressWarnings("unused")
    private Button addTags;
    @FXML
    @SuppressWarnings("unused")
    private ToggleButton logbooksDropdownButton;
    @FXML
    @SuppressWarnings("unused")
    private ToggleButton tagsDropdownButton;
    @FXML
    @SuppressWarnings("unused")
    private Label logbooksLabel;
    @FXML
    @SuppressWarnings("unused")
    private TextField logbooksSelection;
    @FXML
    @SuppressWarnings("unused")
    private TextField tagsSelection;
    @FXML
    @SuppressWarnings("unused")
    private HBox templateControls;
    @FXML
    @SuppressWarnings("unused")
    private ComboBox<LogTemplate> templateSelector;

    @FXML
    @SuppressWarnings("unused")
    private Node attachmentsPane;



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
    private final StringProperty selectedLevelProperty = new SimpleStringProperty();
    private final SimpleStringProperty usernameProperty = new SimpleStringProperty();
    private final SimpleStringProperty passwordProperty = new SimpleStringProperty();

    private final LogEntry logEntry;

    private final SimpleBooleanProperty updateCredentials = new SimpleBooleanProperty();
    private final ReadOnlyBooleanProperty updateCredentialsProperty;
    private final SimpleBooleanProperty inputValid = new SimpleBooleanProperty(false);

    private final LogFactory logFactory;

    private final SimpleBooleanProperty submissionInProgress =
            new SimpleBooleanProperty(false);

    private final LogEntry replyTo;

    /**
     * Indicates if user has started editing. Only title and body are used to define dirty state.
     */
    private boolean isDirty = false;

    /**
     * Used to determine if a log entry is dirty. For a new log entry, comparison is made to empty string, for
     * the reply case the original title is copied to this field.
     */
    private String originalTitle = "";

    private final EditMode editMode;

    /**
     * Result of a submission, caller may use this to take further action once a {@link LogEntry} has been created.
     */
    private Optional<LogEntry> logEntryResult = Optional.empty();

    private final ObservableList<LogTemplate> templatesProperty =
            FXCollections.observableArrayList();

    public LogEntryEditorController(LogEntry logEntry, LogEntry inReplyTo, EditMode editMode) {
        this.replyTo = inReplyTo;
        this.logFactory = LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory);
        this.editMode = editMode;
        updateCredentialsProperty = updateCredentials;

        // This is the reply case:
        if (inReplyTo != null) {
            OlogLog ologLog = new OlogLog();
            ologLog.setTitle(inReplyTo.getTitle());
            ologLog.setTags(inReplyTo.getTags());
            ologLog.setLogbooks(inReplyTo.getLogbooks());
            ologLog.setProperties(inReplyTo.getProperties());
            ologLog.setLevel(inReplyTo.getLevel());
            this.logEntry = ologLog;
            this.originalTitle = inReplyTo.getTitle();
        } else {
            this.logEntry = logEntry;
        }
    }

    @FXML
    public void initialize() {


        // Remote log service not reachable, so show error pane.
        if (!checkConnectivity()) {
            errorPane.visibleProperty().set(true);
            editorPane.disableProperty().set(true);
            return;
        }

        templateControls.managedProperty().bind(templateControls.visibleProperty());
        templateControls.visibleProperty().setValue(editMode.equals(EditMode.NEW_LOG_ENTRY));

        attachmentsPane.managedProperty().bind(attachmentsPane.visibleProperty());
        attachmentsPane.visibleProperty().setValue(editMode.equals(EditMode.NEW_LOG_ENTRY));

        // This could be configured in the fxml, but then these UI components would not be visible
        // in Scene Builder.
        completionMessageLabel.textProperty().set("");
        progressIndicator.visibleProperty().bind(submissionInProgress);

        submitButton.managedProperty().bind(submitButton.visibleProperty());
        submitButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        !inputValid.get() || submissionInProgress.get(),
                inputValid, submissionInProgress));
        completionMessageLabel.visibleProperty()
                .bind(Bindings.createBooleanBinding(() -> completionMessageLabel.textProperty().isNotEmpty().get() && !submissionInProgress.get(),
                        completionMessageLabel.textProperty(), submissionInProgress));

        cancelButton.disableProperty().bind(submissionInProgress);
        attachmentsEditorController.setTextArea(textArea);

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
        if (Preferences.save_credentials) {
            fetchStoredUserCredentials();
        }

        levelLabel.setText(LogbookUIPreferences.level_field_name);
        levelSelector.setItems(availableLevels);
        dateField.setText(TimestampFormats.DATE_FORMAT.format(Instant.now()));

        titleField.textProperty().bindBidirectional(titleProperty);
        titleProperty.addListener((changeListener, oldVal, newVal) ->
        {
            if (newVal == null || newVal.trim().isEmpty()) {
                titleLabel.setTextFill(Color.RED);
            } else {
                titleLabel.setTextFill(Color.BLACK);
            }
            if (newVal != null && !newVal.equals(originalTitle)) {
                isDirty = true;
            }
        });
        titleProperty.set(logEntry.getTitle());

        textArea.textProperty().bindBidirectional(descriptionProperty);
        // When editing an existing entry, set the description to the source of the entry.
        descriptionProperty.set(editMode.equals(EditMode.UPDATE_LOG_ENTRY) ?  logEntry.getSource() :
                (logEntry.getDescription() != null ? logEntry.getDescription() : ""));
        descriptionProperty.addListener((observable, oldValue, newValue) -> isDirty = true);

        Image tagIcon = ImageCache.getImage(LogEntryEditorController.class, "/icons/add_tag.png");
        Image logbookIcon = ImageCache.getImage(LogEntryEditorController.class, "/icons/logbook-16.png");
        Image downIcon = ImageCache.getImage(LogEntryEditorController.class, "/icons/down_triangle.png");

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
            if (change.getList() == null) {
                return;
            }
            List<String> newSelection = new ArrayList<>(change.getList());
            tagsPopOver.setAvailable(availableTagsAsStringList, newSelection);
            tagsPopOver.setSelected(newSelection);
            newSelection.forEach(t -> updateDropDown(tagDropDown, t, true));
        });

        AutocompleteMenu autocompleteMenu = new AutocompleteMenu(new ProposalService(new ProposalProvider() {
            @Override
            public String getName() {
                return Messages.AvailableTemplates;
            }

            @Override
            public List<Proposal> lookup(String text) {
                List<Proposal> proposals = new ArrayList<>();
                templatesProperty.forEach(template -> {
                    if (template.name().contains(text)) {
                        proposals.add(new Proposal(template.name()));
                    }
                });
                return proposals;
            }
        }));

        autocompleteMenu.attachField(templateSelector.getEditor());

        templateSelector.setCellFactory(new Callback<>() {
            @Override
            public ListCell<LogTemplate> call(ListView<LogTemplate> logTemplateListView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(LogTemplate item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(new Label(item.name()));
                        }
                    }
                };
            }
        });

        templateSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                templateSelector.getEditor().textProperty().set(newValue.name());
                if (!newValue.equals(oldValue)) {
                    loadTemplate(newValue);
                }
            } else {
                // E.g. if user specifies an invalid template name
                loadTemplate(null);
            }
        });

        // Hide autocomplete menu when user clicks drop-down button
        templateSelector.showingProperty().addListener((obs, wasShowing, isNowShowing) -> autocompleteMenu.hide());

        templateSelector.getEditor().textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isEmpty()) {
                Optional<LogTemplate> logTemplate =
                        templatesProperty.stream().filter(t -> t.name().equals(n)).findFirst();
                logTemplate.ifPresent(template -> templateSelector.valueProperty().setValue(template));
            }
        });

        templateSelector.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(LogTemplate template) {
                        if (template == null) {
                            return null;
                        } else {
                            return template.name();
                        }
                    }

                    /**
                     * Converts the user specified string to a {@link LogTemplate}
                     * @param name The name of an (existing) template
                     * @return A {@link LogTemplate} if <code>name</code> matches an existing one, otherwise <code>null</code>
                     */
                    @Override
                    public LogTemplate fromString(String name) {
                        Optional<LogTemplate> logTemplate =
                                templatesProperty.stream().filter(t -> t.name().equals(name)).findFirst();
                        return logTemplate.orElse(null);
                    }
                });

        templateSelector.itemsProperty().bind(new SimpleObjectProperty<>(templatesProperty));

        // Note: logbooks and tags are retrieved asynchronously from service
        getServerSideStaticData();

        setupTextAreaContextMenu();

    }

    /**
     * Sets up the context menu for the {@link TextArea}. While a {@link TextArea} comes with a default
     * context menu containing the standard items (copy, paste...), the ability to access this context
     * menu is not possible since Java9, see <a href="https://stackoverflow.com/questions/71053358/javafx-17-custom-textarea-textfield-right-click-menu">this post</a>.
     * Any customization means the whole context menu must be built from scratch.
     */
    private void setupTextAreaContextMenu() {
        // Create the context menu with default items
        ContextMenu contextMenu = new ContextMenu();

        // Standard text editing items
        MenuItem undo = new MenuItem(Messages.TextAreaContextMenuUndo);
        undo.setOnAction(e -> textArea.undo());

        MenuItem redo = new MenuItem(Messages.TextAreaContextMenuRedo);
        redo.setOnAction(e -> textArea.redo());

        MenuItem cut = new MenuItem(Messages.TextAreaContextMenuCut);
        cut.setOnAction(e -> textArea.cut());

        MenuItem copy = new MenuItem(Messages.TextAreaContextMenuCopy);
        copy.setOnAction(e -> textArea.copy());

        MenuItem paste = new MenuItem(Messages.TextAreaContextMenuPaste);
        paste.setOnAction(e -> textArea.paste());

        MenuItem delete = new MenuItem(Messages.TextAreaContextMenuDelete);
        delete.setOnAction(e -> textArea.replaceSelection(""));

        MenuItem selectAll = new MenuItem(Messages.TextAreaContextMenuSelectAll);
        selectAll.setOnAction(e -> textArea.selectAll());

        // Our custom menu item
        MenuItem pasteUrlItem = new MenuItem(Messages.TextAreaContextMenuPasteURLAsMarkdown);
        pasteUrlItem.setOnAction(event -> handleSmartPaste());
        pasteUrlItem.setAccelerator(new KeyCodeCombination(KeyCode.V,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));

        // Add all items to the menu
        contextMenu.getItems().addAll(
                undo,
                redo,
                new SeparatorMenuItem(),
                cut,
                copy,
                paste,
                delete,
                new SeparatorMenuItem(),
                selectAll,
                new SeparatorMenuItem(),
                pasteUrlItem
        );

        // Bind the menu items to the text area's state
        undo.disableProperty().bind(textArea.undoableProperty().not());
        redo.disableProperty().bind(textArea.redoableProperty().not());
        cut.disableProperty().bind(textArea.selectedTextProperty().isEmpty());
        copy.disableProperty().bind(textArea.selectedTextProperty().isEmpty());
        delete.disableProperty().bind(textArea.selectedTextProperty().isEmpty());
        contextMenu.setOnShowing(e -> {
            String clipboardContent = Clipboard.getSystemClipboard().getString();
            pasteUrlItem.setDisable(clipboardContent == null || !clipboardContent.toLowerCase().startsWith("http"));
        });
        // Set the context menu on the text area
        textArea.setContextMenu(contextMenu);
    }

    private void handleSmartPaste() {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        if (!clipboard.hasString()) {
            return;
        }

        String clipboardText = clipboard.getString();
        String ologUrl = extractOlogUrl(clipboardText);
        String selectedText = textArea.getSelectedText();

        if (ologUrl != null) {
            // It's an Olog URL
            String logNumber = extractLogNumber(ologUrl);
            if (logNumber != null) {
                if (selectedText != null && !selectedText.isEmpty()) {
                    // Use selected text as link text for the Olog reference
                    String markdownLink = String.format("[%s](%s)", selectedText, ologUrl);
                    textArea.replaceSelection(markdownLink);
                } else {
                    // No selection - create a standard log entry reference
                    String markdownLink = String.format("[%s](%s)", logNumber, ologUrl);
                    textArea.replaceSelection(markdownLink);
                }
            }
            return;
        }

        // Try to identify if clipboard content is a regular URL
        try {
            new URL(clipboardText);

            if (selectedText != null && !selectedText.isEmpty()) {
                // Replace selection with markdown link using selected text
                String markdownLink = String.format("[%s](%s)", selectedText, clipboardText);
                textArea.replaceSelection(markdownLink);
            } else {
                // No selection - use URL as both link text and target
                String markdownLink = String.format("[%s](%s)", clipboardText, clipboardText);
                textArea.replaceSelection(markdownLink);
            }
        } catch (MalformedURLException e) {
            // Not a URL - do nothing
        }
    }

    private String extractOlogUrl(String text) {
        String rootUrl = LogbookUIPreferences.web_client_root_URL;
        if (rootUrl == null || rootUrl.isEmpty()) {
            return null;
        }

        if (text.toLowerCase().contains("olog") &&
                text.matches(".*?/logs/\\d+/?$")) {
            return text;
        }
        return null;
    }

    private String extractLogNumber(String url) {
        if (url == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("/logs/(\\d+)/?$");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }


    /**
     * Handler for Cancel button. Note that any selections in the {@link SelectionService} are
     * cleared to prevent next launch of {@link org.phoebus.logbook.olog.ui.menu.SendToLogBookApp}
     * to pick them up.
     */
    @FXML
    public void cancel() {
        // Need to clear selections.
        SelectionService.getInstance().clearSelection("");
        ((LogEntryEditorStage) cancelButton.getScene().getWindow()).handleCloseEditor(isDirty, editorPane);
    }

    @FXML
    @SuppressWarnings("unused")
    public void showHelp() {
        new HelpViewer(LogbookUIPreferences.markup_help).show();
    }

    /**
     * Handler for HTML preview button
     */
    @FXML
    @SuppressWarnings("unused")
    public void showHtmlPreview() {
        new PreviewViewer(getDescription(), attachmentsEditorController.getAttachments()).show();
    }


    @FXML
    @SuppressWarnings("unused")
    public void submit() {

        submissionInProgress.set(true);

        JobManager.schedule("Submit Log Entry", monitor -> {
            OlogLog ologLog = new OlogLog();
            ologLog.setTitle(getTitle());
            ologLog.setDescription(getDescription());
            ologLog.setLevel(selectedLevelProperty.get());
            ologLog.setLogbooks(getSelectedLogbooks());
            ologLog.setTags(getSelectedTags());
            if (editMode.equals(EditMode.NEW_LOG_ENTRY)) {
                ologLog.setAttachments(attachmentsEditorController.getAttachments());
            }
            else{
                ologLog.setId(logEntry.getId());
            }
            ologLog.setProperties(logPropertiesEditorController.getProperties());

            LogClient logClient =
                    logFactory.getLogClient(new SimpleAuthenticationToken(usernameProperty.get(), passwordProperty.get()));
            try {
                if(editMode.equals(EditMode.NEW_LOG_ENTRY)){
                    if (replyTo == null) {
                        logEntryResult = Optional.of(logClient.set(ologLog));
                    } else {
                        logEntryResult = Optional.of(logClient.reply(ologLog, replyTo));
                    }
                }
                else{
                    logEntryResult = Optional.of(logClient.update(ologLog));
                }

                // Not dirty anymore...
                isDirty = false;

                // Set username and password in secure store if submission of log entry completes successfully
                if (Preferences.save_credentials) {
                    // Get the SecureStore. Store username and password.
                    try {
                        SecureStore store = new SecureStore();
                        ScopedAuthenticationToken scopedAuthenticationToken =
                                new ScopedAuthenticationToken(AuthenticationScope.LOGBOOK, usernameProperty.get(), passwordProperty.get());
                        store.setScopedAuthentication(scopedAuthenticationToken);
                    } catch (Exception ex) {
                        logger.log(Level.WARNING, "Secure Store file not found.", ex);
                    }
                }
                attachmentsEditorController.deleteTemporaryFiles();
                // This will close the editor
                Platform.runLater(this::cancel);

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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
                .toList();
        List<String> removedTags = existing.stream()
                .filter(tag -> !proposed.contains(tag))
                .toList();
        addedTags.forEach(addFunction);
        removedTags.forEach(removeFunction);
    }

    @FXML
    @SuppressWarnings("unused")
    public void selectLogbooks() {
        if (logbooksDropdownButton.isSelected()) {
            logbookDropDown.show(logbooksSelection, Side.BOTTOM, 0, 0);
        } else {
            logbookDropDown.hide();
        }
    }

    @FXML
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
     * This method also retrieves server side information, e.g. max file size and max request size.
     */
    private void getServerSideStaticData() {
        JobManager.schedule("Fetch Logbooks and Tags", monitor ->
        {
            LogClient logClient =
                    LogService.getInstance().getLogFactories().get(LogbookPreferences.logbook_factory).getLogClient();
            availableLogbooks = logClient.listLogbooks();
            availableLogbooksAsStringList =
                    FXCollections.observableArrayList(availableLogbooks.stream().map(Logbook::getName).collect(Collectors.toList()));
            Collections.sort(availableLogbooksAsStringList);

            List<String> preSelectedLogbooks =
                    logEntry.getLogbooks().stream().map(Logbook::getName).toList();
            List<String> defaultLogbooks = Arrays.asList(LogbookUIPreferences.default_logbooks);
            for (String logbook : availableLogbooksAsStringList) {
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
            }

            availableTags = logClient.listTags();
            availableTagsAsStringList =
                    FXCollections.observableArrayList(availableTags.stream().map(Tag::getName).collect(Collectors.toList()));
            Collections.sort(availableLogbooksAsStringList);

            List<String> preSelectedTags =
                    logEntry.getTags().stream().map(Tag::getName).toList();
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

            String serverInfo = logClient.serviceInfo();
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode jsonNode = objectMapper.readTree(serverInfo);
                attachmentsEditorController.setSizeLimits(jsonNode.get("serverConfig").get("maxFileSize").asText(),
                        jsonNode.get("serverConfig").get("maxRequestSize").asText());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to get or parse response from server info request", e);
            }

            templatesProperty.setAll(logClient.getTemplates().stream().toList());

            Collection<LogEntryLevel> levels = logClient.listLevels();
            availableLevels.setAll(levels.stream().map(LogEntryLevel::name).sorted().toList());
            Optional<LogEntryLevel> optionalLevel = levels.stream().filter(LogEntryLevel::defaultLevel).findFirst();
            String defaultLevel = null;
            if(optionalLevel.isPresent()){
                // One level value *should* be the default level
               defaultLevel = optionalLevel.get().name();
            }
            selectedLevelProperty.set(logEntry.getLevel() != null ? logEntry.getLevel() : defaultLevel);
            Platform.runLater(() -> levelSelector.getSelectionModel().select(selectedLevelProperty.get()));
        });
    }

    public void fetchStoredUserCredentials() {
        // Perform file IO on background thread.
        JobManager.schedule("Access Secure Store", monitor ->
        {
            // Get the SecureStore. Retrieve username and password.
            try {
                SecureStore store = new SecureStore();
                ScopedAuthenticationToken scopedAuthenticationToken = store.getScopedAuthenticationToken(AuthenticationScope.LOGBOOK);
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
            Logger.getLogger(LogEntryEditorController.class.getName()).warning("Failed to query logbook service, it may be off-line.");
            return false;
        }
    }

    public Optional<LogEntry> getLogEntryResult() {
        return logEntryResult;
    }

    /**
     * Loads template to configure UI elements.
     *
     * @param logTemplate A {@link LogTemplate} selected by user. If <code>null</code>, all log entry elements
     *                    will be cleared, except the LogEntryLevel selector, which will be set to the top-most item.
     */
    private void loadTemplate(LogTemplate logTemplate) {
        if (logTemplate != null) {
            titleProperty.set(logTemplate.title());
            descriptionProperty.set(logTemplate.source());
            logPropertiesEditorController.setProperties(logTemplate.properties());
            selectedTags.setAll(logTemplate.tags().stream().map(Tag::getName).toList());
            selectedLogbooks.setAll(logTemplate.logbooks().stream().map(Logbook::getName).toList());
            levelSelector.getSelectionModel().select(logTemplate.level());
            selectedTags.forEach(t -> updateDropDown(tagDropDown, t, true));
            selectedLogbooks.forEach(l -> updateDropDown(logbookDropDown, l, true));
        } else {
            titleProperty.set(null);
            descriptionProperty.set(null);
            logPropertiesEditorController.clearSelectedProperties();
            attachmentsEditorController.clearAttachments();
            selectedTags.setAll(Collections.emptyList());
            selectedLogbooks.setAll(Collections.emptyList());
            levelSelector.getSelectionModel().select(0);
            selectedTags.forEach(t -> updateDropDown(tagDropDown, t, false));
            selectedLogbooks.forEach(l -> updateDropDown(logbookDropDown, l, false));
        }
    }
}
