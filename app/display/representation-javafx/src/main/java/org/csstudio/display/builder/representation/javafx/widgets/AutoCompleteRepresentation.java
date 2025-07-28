/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.AutoCompleteWidget;
import org.csstudio.display.builder.representation.javafx.Cursors;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.VEnum;
import org.epics.vtype.VType;
import org.phoebus.ui.javafx.Styles;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import org.phoebus.ui.vtype.FormatOption;
import org.phoebus.ui.vtype.FormatOptionHandler;

/**
 * JavaFX‐side representation for {@link AutoCompleteWidget}.
 */
@SuppressWarnings("nls")
public class AutoCompleteRepresentation extends RegionBaseRepresentation<TextField, AutoCompleteWidget> {
    private volatile boolean active = false;
    private volatile boolean enabled = false;
    private volatile boolean updating_suggestions = false;

    private final DirtyFlag dirty_style = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final DirtyFlag dirty_enable = new DirtyFlag();
    private final UntypedWidgetPropertyListener contentChangedListener = this::contentChanged;
    private final UntypedWidgetPropertyListener enableChangedListener = this::enableChanged;
    private final UntypedWidgetPropertyListener styleChangedListener = this::styleChanged;

    private volatile List<String> items = Collections.emptyList();
    private volatile String currentValue = "";

    private Popup suggestionsPopup;
    private ListView<String> suggestionsListView;
    private ObservableList<String> suggestionsData;

    private static class SuggestionCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setStyle("");
            } else {
                setText(item);
                setStyle(
                    "-fx-padding: 8 12 8 12; " +
                        "-fx-alignment: center-left; " +
                        "-fx-border-color: transparent; " +
                        "-fx-background-color: transparent;"
                );
            }
        }
    }

    private void updateSuggestionsDisplay(List<String> suggestions) {
        updating_suggestions = true;

        try {
            if (suggestions.isEmpty()) {
                suggestionsPopup.hide();
                return;
            }

            suggestionsData.setAll(suggestions);

            if (!suggestionsPopup.isShowing()) {
                showPopup(suggestions.size());
            } else {
                updatePopupSize(suggestions.size());
            }

            suggestionsListView.getSelectionModel().clearSelection();
            suggestionsListView.scrollTo(0);

        } finally {
            updating_suggestions = false;
        }
    }

    private void showPopup(int itemCount) {
        Bounds bounds = jfx_node.localToScreen(jfx_node.getBoundsInLocal());

        updatePopupSize(itemCount);

        suggestionsPopup.show(jfx_node, bounds.getMinX(), bounds.getMaxY());
    }

    private void updatePopupSize(int itemCount) {
        double textFieldWidth = jfx_node.getWidth();
        suggestionsListView.setPrefWidth(textFieldWidth);
        suggestionsListView.setMaxWidth(textFieldWidth);
        suggestionsListView.setMinWidth(textFieldWidth);

        final int maxVisibleSuggestions = model_widget.propMaxSuggestions().getValue();
        double itemHeight = 35.0;
        int visibleItems = Math.min(itemCount, maxVisibleSuggestions);
        double maxHeight = Math.min(visibleItems * itemHeight, 300);

        suggestionsListView.setPrefHeight(maxHeight);
        suggestionsListView.setMaxHeight(maxHeight);
    }

    private void showAllSuggestions() {
        if (!enabled || items.isEmpty()) {
            return;
        }

        String currentText = jfx_node.getText();
        if (currentText == null) {
            currentText = "";
        }

        List<String> suggestions;
        if (currentText.isEmpty() || currentText.length() < model_widget.propMinCharacters().getValue()) {
            suggestions = items;
        } else {
            suggestions = model_widget.getFilteredSuggestions(currentText);
        }

        updateSuggestionsDisplay(suggestions);
    }

    /**
     * Creates the JavaFX node (a {@link TextField}) and initializes internal popup.
     *
     * @return a configured {@link TextField} ready for user input
     * @throws Exception if the node cannot be created or styled
     */
    @Override
    public TextField createJFXNode() throws Exception {
        final TextField textField = new TextField();

        if (!toolkit.isEditMode()) {
            initializePopup(textField);
            setupTextFieldEventHandlers(textField);
        }

        textField.setManaged(false);

        enableChanged(null, null, null);
        contentChanged(null, null, null);

        return textField;
    }

    /**
     * Suggestion popup initialization.
     *
     * @param textField AutoComplete JavaFX TextField.
     */
    private void initializePopup(TextField textField) {
        suggestionsPopup = new Popup();
        suggestionsPopup.setAutoHide(true);
        suggestionsPopup.setAutoFix(true);
        suggestionsPopup.setHideOnEscape(true);
        suggestionsPopup.setConsumeAutoHidingEvents(false);

        suggestionsData = FXCollections.observableArrayList();
        suggestionsListView = new ListView<>(suggestionsData);

        suggestionsListView.setCellFactory(listView -> new SuggestionCell());

        suggestionsListView.setStyle(
            "-fx-background-color: white; " +
                "-fx-border-color: #cccccc; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 3px; " +
                "-fx-background-radius: 3px; " +
                "-fx-focus-color: transparent; " +
                "-fx-faint-focus-color: transparent;"
        );

        suggestionsListView.setOnMouseClicked(event -> {
            String selectedItem = suggestionsListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null && event.getClickCount() == 1) {
                selectSuggestion(selectedItem);
            }
        });

        suggestionsPopup.getContent().add(suggestionsListView);

        textField.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            if (suggestionsPopup.isShowing()) {
                updatePopupSize(suggestionsData.size());
            }
        });

        suggestionsListView.setFocusTraversable(false);
        suggestionsListView.setMouseTransparent(false);
    }

    /**
     * Method to set up once all listeners and actions on the TextField.
     *
     * @param textField AutoComplete JavaFX TextField.
     */
    private void setupTextFieldEventHandlers(TextField textField) {
        ChangeListener<String> textChangeListener = (observable, oldValue, newValue) -> {
            if (active || updating_suggestions) {
                return;
            }

            updateSuggestions(newValue);

            if (!model_widget.propCustom().getValue() && newValue != null && !newValue.isEmpty()) {
                validateInput(newValue);
            }
        };
        textField.textProperty().addListener(textChangeListener);

        textField.setOnMouseClicked(event -> {
            if (enabled && event.getButton() == MouseButton.PRIMARY) {
                showAllSuggestions();
            }
        });

        textField.setOnKeyPressed(this::handleKeyPressed);

        textField.setOnAction(event -> {
            if (suggestionsPopup.isShowing()) {
                suggestionsPopup.hide();
            }
            String text = textField.getText();
            if (text != null && !text.trim().isEmpty()) {
                confirmValue(text);
            }
        });

        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && enabled) {
                Platform.runLater(this::showAllSuggestions);
            } else if (!newVal) {
                if (suggestionsPopup.isShowing()) {
                    Platform.runLater(() -> suggestionsPopup.hide());
                }

                String currentText = textField.getText();
                if (currentText != null && !currentText.equals(currentValue)) {
                    Platform.runLater(() -> textField.setText(currentValue));
                }
            }
        });

        textField.addEventFilter(MouseEvent.ANY, e -> {
            if (e.getButton() != MouseButton.NONE && !enabled) {
                e.consume();
            }
        });
    }

    /**
     * Method to validate user input when
     *
     * @param input User input.
     */
    private void validateInput(String input) {
        if (!model_widget.propCustom().getValue()) {
            return;
        }

        boolean isValidItem = items.stream()
            .anyMatch(item -> {
                if (model_widget.propCaseSensitive().getValue()) {
                    return item.startsWith(input);
                } else {
                    return item.toLowerCase().startsWith(input.toLowerCase()) ||
                        item.equalsIgnoreCase(input);
                }
            });

        if (isValidItem || input.isEmpty()) {
            Platform.runLater(() -> styleChanged(null, null, null));
        }
    }

    /**
     * Method to handle user interactions with text field.
     *
     * @param event Key input event.
     */
    private void handleKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case UP -> {
                int currentIndex = suggestionsListView.getSelectionModel().getSelectedIndex();
                if (currentIndex > 0) {
                    suggestionsListView.getSelectionModel().select(currentIndex - 1);
                    suggestionsListView.scrollTo(currentIndex - 1);
                } else if (currentIndex == -1 && !suggestionsData.isEmpty()) {
                    suggestionsListView.getSelectionModel().select(0);
                    suggestionsListView.scrollTo(0);
                }
                event.consume();
            }
            case DOWN -> {
                int currentIndex = suggestionsListView.getSelectionModel().getSelectedIndex();
                if (currentIndex < suggestionsData.size() - 1) {
                    suggestionsListView.getSelectionModel().select(currentIndex + 1);
                    suggestionsListView.scrollTo(currentIndex + 1);
                } else if (currentIndex == -1 && !suggestionsData.isEmpty()) {
                    suggestionsListView.getSelectionModel().select(0);
                    suggestionsListView.scrollTo(0);
                }
                event.consume();
            }
            case ESCAPE -> {
                suggestionsPopup.hide();
                event.consume();
            }
        }
    }

    /**
     * Method called when user is typing in the text field in order to update suggestions based
     * on filtering option.
     *
     * @param inputText new text input string.
     */
    private void updateSuggestions(String inputText) {
        if (inputText == null) {
            inputText = "";
        }

        if (inputText.length() < model_widget.propMinCharacters().getValue()) {
            suggestionsPopup.hide();
            return;
        }

        List<String> suggestions = model_widget.getFilteredSuggestions(inputText);
        updateSuggestionsDisplay(suggestions);
    }

    /**
     * Selection of a suggestion in the dropdown.
     *
     * @param suggestion selected suggestion.
     */
    private void selectSuggestion(String suggestion) {
        active = true;

        try {
            jfx_node.setText(suggestion);
            suggestionsPopup.hide();

            if (model_widget.runtimePropPVWritable().getValue()) {
                confirmValue(suggestion);
            }
        } finally {
            active = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isFilteringEditModeClicks() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerListeners() {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(styleChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(styleChangedListener);
        model_widget.propForegroundColor().addUntypedPropertyListener(styleChangedListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(styleChangedListener);
        model_widget.propFont().addUntypedPropertyListener(styleChangedListener);
        model_widget.propPlaceholder().addUntypedPropertyListener(styleChangedListener);

        model_widget.runtimePropValue().addUntypedPropertyListener(contentChangedListener);
        model_widget.propItemsFromPV().addUntypedPropertyListener(contentChangedListener);
        model_widget.propItems().addUntypedPropertyListener(contentChangedListener);
        model_widget.propEnabled().addUntypedPropertyListener(enableChangedListener);
        model_widget.runtimePropPVWritable().addUntypedPropertyListener(enableChangedListener);
        model_widget.propCustom().addUntypedPropertyListener(contentChangedListener);

        styleChanged(null, null, null);
        contentChanged(null, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void unregisterListeners() {
        model_widget.propWidth().removePropertyListener(styleChangedListener);
        model_widget.propHeight().removePropertyListener(styleChangedListener);
        model_widget.propForegroundColor().removePropertyListener(styleChangedListener);
        model_widget.propBackgroundColor().removePropertyListener(styleChangedListener);
        model_widget.propFont().removePropertyListener(styleChangedListener);
        model_widget.propPlaceholder().removePropertyListener(styleChangedListener);
        model_widget.runtimePropValue().removePropertyListener(contentChangedListener);
        model_widget.propItemsFromPV().removePropertyListener(contentChangedListener);
        model_widget.propItems().removePropertyListener(contentChangedListener);
        model_widget.propEnabled().removePropertyListener(enableChangedListener);
        model_widget.runtimePropPVWritable().removePropertyListener(enableChangedListener);
        model_widget.propCustom().removePropertyListener(contentChangedListener);

        super.unregisterListeners();
    }

    /**
     * Confirm selected value and write it to PV or local variable.
     *
     * @param value Selected value by user.
     */
    private void confirmValue(final String value) {
        if (!model_widget.runtimePropPVWritable().getValue()) {
            return;
        }

        if (!model_widget.propCustom().getValue()) {
            boolean isValidItem = items.stream()
                .anyMatch(item -> {
                    if (model_widget.propCaseSensitive().getValue()) {
                        return item.equals(value);
                    } else {
                        return item.equalsIgnoreCase(value);
                    }
                });

            if (!isValidItem) {
                Platform.runLater(() -> jfx_node.setText(currentValue));
                return;
            }
        }

        if (model_widget.propConfirmDialog().getValue()) {
            final String message = model_widget.propConfirmMessage().getValue();
            final String password = model_widget.propPassword().getValue();
            if (!password.isEmpty()) {
                if (toolkit.showPasswordDialog(model_widget, message, password) == null) {
                    return;
                }
            } else if (!toolkit.showConfirmationDialog(model_widget, message)) {
                return;
            }
        }

        try {
            final VType currentPvValue = model_widget.runtimePropValue().getValue();
            Object mappedValue;

            if (currentPvValue instanceof VEnum) {
                mappedValue = FormatOptionHandler.parse(currentPvValue, value,
                    FormatOption.DEFAULT);
            } else {
                try {
                    if (value.contains(".")) {
                        mappedValue = Double.parseDouble(value);
                    } else {
                        mappedValue = Integer.parseInt(value);
                    }
                } catch (NumberFormatException e) {
                    // This will handle String PVs, which are expected to be most likely
                    VType convertedValue = VType.toVType(value);
                    mappedValue = (convertedValue != null) ? convertedValue : value;
                }
            }

            toolkit.fireWrite(model_widget, mappedValue);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error writing to PV", e);
        }
    }

    /**
     * Method called when a modification of autocomplete widget is listened.
     *
     * @param property Property of the widget to modify.
     * @param old_value Old value of the property.
     * @param new_value New value of the property.
     */
    private void styleChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value) {
        dirty_style.mark();
        toolkit.scheduleUpdate(this);
    }

    /**
     * Update enable status of the widget.
     *
     * @param property Enable property.
     * @param old_value Old status.
     * @param new_value New status.
     */
    private void enableChanged(final WidgetProperty<?> property, final Object old_value,
        final Object new_value) {
        enabled = model_widget.propEnabled().getValue() &&
            model_widget.runtimePropPVWritable().getValue();
        dirty_enable.mark();
        toolkit.scheduleUpdate(this);
    }

    /**
     * Add a value to the list of suggestion items.
     *
     * @param value Value to compute.
     * @param fromPV Is value from the PV. True if it's from a PV, false otherwise.
     * @return List of computed items.
     */
    private List<String> computeItems(final VType value, final boolean fromPV) {
        if (fromPV && value instanceof VEnum) {
            return ((VEnum) value).getDisplay().getChoices();
        } else {
            final String itemsString = model_widget.propItems().getValue();
            if (itemsString == null || itemsString.trim().isEmpty()) {
                return List.of();
            }

            return Arrays.stream(itemsString.split("\n"))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(java.util.stream.Collectors.toList());
        }
    }

    /**
     * Method called if an item is listened in items list.
     *
     * @param property Content property.
     * @param old_value Old item value.
     * @param new_value New item value.
     */
    private void contentChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value) {
        VType value = model_widget.runtimePropValue().getValue();
        boolean fromPV = model_widget.propItemsFromPV().getValue() && value instanceof VEnum;
        items = computeItems(value, fromPV);

        currentValue = VTypeUtil.getValueString(value, false);

        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateChanges() {
        super.updateChanges();

        if (dirty_style.checkAndClear()) {
            jfx_node.resize(model_widget.propWidth().getValue(),
                model_widget.propHeight().getValue());

            Font f = JFXUtil.convert(model_widget.propFont().getValue());

            jfx_node.setStyle(MessageFormat.format(
                "-fx-background-color: {0}; " +
                    "-fx-text-fill: {1}; " +
                    "-fx-font: {2} {3}px \"{4}\"; " +
                    "-fx-border-color: derive({0}, -20%); " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 3px; " +
                    "-fx-background-radius: 3px;",
                JFXUtil.webRgbOrHex(model_widget.propBackgroundColor().getValue()),
                JFXUtil.webRgbOrHex(model_widget.propForegroundColor().getValue()),
                f.getStyle().toLowerCase().replace("regular", "normal"),
                f.getSize(),
                f.getFamily()
            ));

            jfx_node.setPromptText(model_widget.propPlaceholder().getValue());
        }

        if (dirty_content.checkAndClear() && !toolkit.isEditMode()) {
            active = true;
            try {
                model_widget.setItems(items);

                if (!jfx_node.getText().equals(currentValue)) {
                    jfx_node.setText(currentValue);
                }
            } finally {
                active = false;
            }
        }

        if (dirty_enable.checkAndClear() && !toolkit.isEditMode()) {
            Styles.update(jfx_node, Styles.NOT_ENABLED, !enabled);
            jfx_node.setCursor(enabled ? Cursor.TEXT : Cursors.NO_WRITE);
            jfx_node.setEditable(enabled);
        }

        jfx_node.layout();
    }
}
