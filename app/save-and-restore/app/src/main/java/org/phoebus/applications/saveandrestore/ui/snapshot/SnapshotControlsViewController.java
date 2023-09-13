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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.util.converter.DoubleStringConverter;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.event.SaveAndRestoreEventReceiver;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.AuthenticationScope;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.docking.DockPane;
import org.phoebus.util.time.TimestampFormats;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SnapshotControlsViewController {

    private SnapshotController snapshotController;

    @FXML
    protected TextField snapshotName;

    @FXML
    protected TextArea snapshotComment;

    @FXML
    protected Button saveSnapshotButton;

    @FXML
    private Button saveSnapshotAndCreateLogEntryButton;

    @FXML
    private Label createdBy;

    @FXML
    private Label createdDate;

    @FXML
    private Label snapshotLastModifiedLabel;

    @FXML
    private Button takeSnapshotButton;

    @FXML
    private Button restoreButton;

    @FXML
    private Button restoreAndLogButton;

    @FXML
    private Spinner<Double> thresholdSpinner;

    @FXML
    private Spinner<Double> multiplierSpinner;

    @FXML
    private TextField filterTextField;

    @FXML
    private CheckBox preserveSelectionCheckBox;

    @FXML
    protected ToggleButton showLiveReadbackButton;

    @FXML
    private ToggleButton showDeltaPercentageButton;

    @FXML
    protected ToggleButton hideEqualItemsButton;

    @FXML
    private ToolBar filterToolbar;

    @FXML
    private Label authenticatedUserId;

    private List<List<Pattern>> regexPatterns = new ArrayList<>();

    protected final SimpleStringProperty snapshotNameProperty = new SimpleStringProperty();
    private final SimpleStringProperty snapshotCommentProperty = new SimpleStringProperty();
    private final SimpleStringProperty createdByTextProperty = new SimpleStringProperty();
    private final SimpleStringProperty createdDateTextProperty = new SimpleStringProperty();
    private final SimpleStringProperty lastModifiedDateTextProperty = new SimpleStringProperty();
    private final SimpleBooleanProperty snapshotRestorableProperty = new SimpleBooleanProperty(false);

    protected final SimpleBooleanProperty showLiveReadbackProperty = new SimpleBooleanProperty(false);

    protected final SimpleBooleanProperty showDeltaPercentageProperty = new SimpleBooleanProperty(false);

    private final SimpleBooleanProperty hideEqualItemsProperty = new SimpleBooleanProperty(false);

    /**
     * Property used to indicate if there is new snapshot data to save, or if snapshot metadata
     * has changed (e.g. user wants to rename the snapshot or update the comment).
     */
    protected final SimpleBooleanProperty snapshotDataDirty = new SimpleBooleanProperty(false);

    private final SimpleObjectProperty<Node> snapshotNodeProperty = new SimpleObjectProperty();

    private final SimpleBooleanProperty userIsAuthenticated = new SimpleBooleanProperty();

    private final SimpleStringProperty authenticatedUserProperty =
            new SimpleStringProperty(Messages.authenticatedUserNone);

    public void setSnapshotController(SnapshotController snapshotController) {
        this.snapshotController = snapshotController;
    }

    @FXML
    public void initialize() {

        snapshotName.textProperty().bindBidirectional(snapshotNameProperty);
        snapshotName.disableProperty().bind(userIsAuthenticated.not());
        snapshotComment.textProperty().bindBidirectional(snapshotCommentProperty);
        snapshotComment.disableProperty().bind(userIsAuthenticated.not());
        createdBy.textProperty().bind(createdByTextProperty);
        createdDate.textProperty().bind(createdDateTextProperty);
        snapshotLastModifiedLabel.textProperty().bind(lastModifiedDateTextProperty);

        takeSnapshotButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                (snapshotNodeProperty.isNotNull().get() && !snapshotNodeProperty.get().getNodeType().equals(NodeType.SNAPSHOT)) ||
                userIsAuthenticated.not().get(), snapshotNodeProperty, userIsAuthenticated));

        snapshotNameProperty.addListener(((observableValue, oldValue, newValue) ->
                snapshotDataDirty.set(newValue != null && (snapshotNodeProperty.isNull().get() || snapshotNodeProperty.isNotNull().get() && !newValue.equals(snapshotNodeProperty.get().getName())))));
        snapshotCommentProperty.addListener(((observableValue, oldValue, newValue) ->
                snapshotDataDirty.set(newValue != null && (snapshotNodeProperty.isNull().get() || snapshotNodeProperty.isNotNull().get() && !newValue.equals(snapshotNodeProperty.get().getDescription())))));

        saveSnapshotButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        snapshotDataDirty.not().get() ||
                                snapshotNameProperty.isEmpty().get() ||
                                snapshotCommentProperty.isEmpty().get() ||
                                userIsAuthenticated.not().get(),
                snapshotDataDirty, snapshotNameProperty, snapshotCommentProperty, userIsAuthenticated));

        saveSnapshotAndCreateLogEntryButton.disableProperty().bind(Bindings.createBooleanBinding(() -> (
                        snapshotDataDirty.not().get()) ||
                        snapshotNameProperty.isEmpty().get() ||
                        snapshotCommentProperty.isEmpty().get() ||
                        userIsAuthenticated.not().get(),
                snapshotDataDirty, snapshotNameProperty, snapshotCommentProperty, userIsAuthenticated));

        // Do not show the create log entry button if no event receivers have been registered
        saveSnapshotAndCreateLogEntryButton.visibleProperty().set(ServiceLoader.load(SaveAndRestoreEventReceiver.class).iterator().hasNext());

        restoreButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                snapshotRestorableProperty.not().get() ||
                userIsAuthenticated.not().get(), snapshotRestorableProperty, userIsAuthenticated));
        restoreAndLogButton.disableProperty().bind(Bindings.createBooleanBinding(() ->
                snapshotRestorableProperty.not().get() ||
                userIsAuthenticated.not().get(), snapshotRestorableProperty, userIsAuthenticated));

        SpinnerValueFactory<Double> thresholdSpinnerValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 999.0, 0.0, 0.01);
        thresholdSpinnerValueFactory.setConverter(new DoubleStringConverter());
        thresholdSpinner.setValueFactory(thresholdSpinnerValueFactory);
        thresholdSpinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
        thresholdSpinner.getEditor().getStylesheets().add(getClass().getResource("/save-and-restore-style.css").toExternalForm());
        thresholdSpinner.getEditor().textProperty().addListener((a, o, n) -> parseAndUpdateThreshold(n));

        SpinnerValueFactory<Double> multiplierSpinnerValueFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 999.0, 1.0, 0.01);
        multiplierSpinnerValueFactory.setConverter(new DoubleStringConverter());
        multiplierSpinner.setValueFactory(multiplierSpinnerValueFactory);
        multiplierSpinner.getEditor().setAlignment(Pos.CENTER_RIGHT);
        multiplierSpinner.getEditor().getStylesheets().add(getClass().getResource("/save-and-restore-style.css").toExternalForm());
        multiplierSpinner.getEditor().textProperty()
                .addListener((a, o, n) -> {
                    multiplierSpinner.getEditor().getStyleClass().remove("input-error");
                    multiplierSpinner.setTooltip(null);
                    snapshotRestorableProperty.set(true);
                    double parsedNumber;
                    try {
                        parsedNumber = Double.parseDouble(n.trim());
                        snapshotController.updateSnapshotValues(parsedNumber);
                        //parseAndUpdateThreshold(thresholdSpinner.getEditor().getText().trim());
                    } catch (NumberFormatException e) {
                        multiplierSpinner.getEditor().getStyleClass().add("input-error");
                        multiplierSpinner.setTooltip(new Tooltip(Messages.toolTipMultiplierSpinner));
                        snapshotRestorableProperty.set(false);
                    }
                });

        DockPane.getActiveDockPane().addEventFilter(KeyEvent.ANY, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.F) {
                if (!filterTextField.isFocused()) {
                    filterTextField.requestFocus();
                }
            }
        });

        String filterShortcutName = (new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN)).getDisplayText();
        filterTextField.setPromptText("* for all matching and , as or separator, & as and separator. Start with / for regex. All if empty. (" + filterShortcutName + ")");

        filterTextField.addEventHandler(KeyEvent.ANY, event -> {
            String filterText = filterTextField.getText().trim();

            List<String> filters = Arrays.asList(filterText.split(","));
            regexPatterns = filters.stream()
                    .map(item -> {
                        if (item.startsWith("/")) {
                            return List.of(Pattern.compile(item.substring(1, item.length() - 1).trim()));
                        } else {
                            return Arrays.stream(item.split("&"))
                                    .map(andItem -> andItem.replaceAll("\\*", ".*"))
                                    .map(andItem -> Pattern.compile(andItem.trim()))
                                    .collect(Collectors.toList());
                        }
                    }).collect(Collectors.toList());

            snapshotController.applyFilter(filterText, preserveSelectionCheckBox.isSelected(), regexPatterns);
        });

        preserveSelectionCheckBox.selectedProperty().addListener((observableValue, aBoolean, isSelected) -> snapshotController.applyPreserveSelection(isSelected));

        showLiveReadbackButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/show_live_readback_column.png"))));
        showLiveReadbackProperty.bind(showLiveReadbackButton.selectedProperty());
        showLiveReadbackButton.selectedProperty()
                .addListener((a, o, n) ->
                        snapshotController.showReadback(showLiveReadbackProperty.get()));

        ImageView showHideDeltaPercentageButtonImageView = new ImageView(new Image(getClass().getResourceAsStream("/icons/show_hide_delta_percentage.png")));
        showHideDeltaPercentageButtonImageView.setFitWidth(16);
        showHideDeltaPercentageButtonImageView.setFitHeight(16);

        showDeltaPercentageButton.setGraphic(showHideDeltaPercentageButtonImageView);
        showDeltaPercentageProperty.bind(showDeltaPercentageButton.selectedProperty());
        showDeltaPercentageButton.selectedProperty()
                .addListener((a, o, n) ->
                        snapshotController.showDeltaPercentage(n));

        hideEqualItemsButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/hide_show_equal_items.png"))));
        hideEqualItemsProperty.bind(hideEqualItemsButton.selectedProperty());
        hideEqualItemsButton.selectedProperty()
                .addListener((a, o, n) ->
                        snapshotController.applyHideEqualItems());

        snapshotNodeProperty.addListener((ob, old, node) -> {
            if (node != null) {
                Platform.runLater(() -> {
                    snapshotNameProperty.set(node.getName());
                    snapshotCommentProperty.set(node.getDescription());
                    createdDateTextProperty.set(node.getCreated() != null ? TimestampFormats.SECONDS_FORMAT.format(node.getCreated().toInstant()) : null);
                    lastModifiedDateTextProperty.set(node.getLastModified() != null ? TimestampFormats.SECONDS_FORMAT.format(node.getLastModified().toInstant()) : null);
                    createdByTextProperty.set(node.getUserName());
                    filterToolbar.disableProperty().set(node.getName() == null);
                });
            }
        });

        authenticatedUserId.textProperty().bind(authenticatedUserProperty);

        // Initialize userIsAuthenticated property

        try {
            SecureStore secureStore = new SecureStore();
            ScopedAuthenticationToken token =
                    secureStore.getScopedAuthenticationToken(AuthenticationScope.SAVE_AND_RESTORE);
            if (token != null) {
                userIsAuthenticated.set(true);
                authenticatedUserProperty.set(token.getUsername());
            }
        } catch (Exception e) {
            Logger.getLogger(SnapshotControlsViewController.class.getName()).log(Level.WARNING, "Unable to retrieve authentication token for " +
                    AuthenticationScope.SAVE_AND_RESTORE.getName() + " scope", e);
        }
    }

    public SimpleStringProperty getSnapshotNameProperty() {
        return snapshotNameProperty;
    }

    public SimpleStringProperty getSnapshotCommentProperty() {
        return snapshotCommentProperty;
    }

    @FXML
    public void takeSnapshot() {
        snapshotDataDirty.set(true);
        snapshotRestorableProperty.set(false);
        snapshotController.takeSnapshot();
    }

    @FXML
    public void saveSnapshot(ActionEvent event) {
        snapshotController.saveSnapshot(event);
    }

    @FXML
    public void restore(ActionEvent event) {
        snapshotController.restore(event);
    }

    public SimpleBooleanProperty getSnapshotRestorableProperty() {
        return snapshotRestorableProperty;
    }

    public void setNameAndCommentDisabled(boolean disabled) {
        snapshotName.disableProperty().set(disabled);
        snapshotComment.disableProperty().set(disabled);
    }

    public void setSnapshotNode(Node node) {
        snapshotNodeProperty.set(node);
    }

    private void parseAndUpdateThreshold(String value) {
        thresholdSpinner.getEditor().getStyleClass().remove("input-error");
        thresholdSpinner.setTooltip(null);

        double parsedNumber;
        try {
            parsedNumber = Double.parseDouble(value.trim());
            snapshotController.updateThreshold(parsedNumber);
        } catch (Exception e) {
            thresholdSpinner.getEditor().getStyleClass().add("input-error");
            thresholdSpinner.setTooltip(new Tooltip(Messages.toolTipMultiplierSpinner));
        }
    }

    public SimpleBooleanProperty getHideEqualItemsProperty() {
        return hideEqualItemsProperty;
    }

    public void setFilterToolbarDisabled(boolean disabled) {
        filterToolbar.disableProperty().set(disabled);
    }

    public void setSnapshotRestorableProperty(boolean restorable) {
        snapshotRestorableProperty.set(restorable);
    }

    public void secureStoreChanged(List<ScopedAuthenticationToken> validTokens) {

        Optional<ScopedAuthenticationToken> token =
                validTokens.stream()
                        .filter(t -> t.getAuthenticationScope().equals(AuthenticationScope.SAVE_AND_RESTORE)).findFirst();
        if (token.isPresent()) {
            userIsAuthenticated.set(true);
            authenticatedUserProperty.set(token.get().getUsername());
        } else {
            userIsAuthenticated.set(false);
            authenticatedUserProperty.set(Messages.authenticatedUserNone);
        }
    }
}
