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
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import org.phoebus.ui.vtype.FormatOption;
import org.phoebus.ui.vtype.FormatOptionHandler;

/**
 * JavaFX‐side representation for {@link AutoCompleteWidget}.
 */
@SuppressWarnings("nls")
public class AutoCompleteRepresentation extends RegionBaseRepresentation<TextField, AutoCompleteWidget> {
    private final DirtyFlag dirty_style = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final DirtyFlag dirty_enable = new DirtyFlag();
    private final UntypedWidgetPropertyListener listener = this::propertyChanged;

    private List<String> items = List.of();
    private String currentValue = "";
    private Popup suggestionsPopup;
    private ListView<String> suggestionsListView;
    private ObservableList<String> suggestions;
    private boolean isNavigatingSuggestions = false;

    private javafx.beans.value.ChangeListener<String> textChangeListener;

    /**
     * Creates the JavaFX node (a {@link TextField}) and initializes internal suggestionsPopup.
     *
     * @return a configured {@link TextField} ready for user input
     * @throws Exception if the node cannot be created or styled
     */
    @Override
    public TextField createJFXNode() throws Exception {
        final TextField textField = new TextField();
        textField.setManaged(false);

        if (!toolkit.isEditMode()) {
            initPopup();
            setupEventHandlers(textField);
        }

        return textField;
    }

    /**
     * Initializes the suggestions popup and its ListView.
     * The popup will display a list of suggestions based on user input.
     */
    private void initPopup() {
        suggestionsPopup = new Popup();
        suggestionsPopup.setAutoHide(false);
        suggestionsPopup.setHideOnEscape(false);
        suggestionsPopup.setConsumeAutoHidingEvents(false);

        suggestions = FXCollections.observableArrayList();
        suggestionsListView = new ListView<>(suggestions);
        suggestionsListView.setFocusTraversable(false);

        suggestionsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle(isSelected() ? "-fx-text-fill: white;" : "-fx-background-color: transparent;");
            }
        });

        suggestionsListView.setStyle(
            "-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1px; " +
                "-fx-border-radius: 3px; -fx-background-radius: 3px; -fx-focus-color: transparent; " +
                "-fx-faint-focus-color: transparent; -fx-selection-bar-non-focused: #3498db;"
        );

        suggestionsListView.setOnMouseClicked(e -> {
            String selected = suggestionsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectSuggestion(selected);
            }
        });

        suggestionsPopup.getContent().add(suggestionsListView);
    }

    /**
     * Method to set up once all listeners and actions on the TextField.
     *
     * @param textField AutoComplete JavaFX TextField.
     */
    private void setupEventHandlers(TextField textField) {
        textChangeListener = (obs, old, text) -> updateSuggestions(text);
        textField.textProperty().addListener(textChangeListener);

        textField.setOnMouseClicked(e -> {
            if (!textField.isDisabled() && textField.isEditable() && e.getButton() == MouseButton.PRIMARY) {
                showSuggestions();
            }
        });

        textField.setOnKeyPressed(this::handleKeyPressed);

        textField.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) {
                showSuggestions();
            } else {
                suggestionsPopup.hide();
                textField.textProperty().removeListener(textChangeListener);
                textField.setText(currentValue);
                textField.textProperty().addListener(textChangeListener);
            }
        });
    }

    /**
     * Handles key presses in the TextField to navigate through suggestions.
     * - DOWN: Selects the next suggestion.
     * - UP: Selects the previous suggestion.
     * - ENTER: Selects the currently highlighted suggestion.
     * - ESCAPE: Hides the suggestions popup and resets the text field.
     *
     * @param event KeyEvent triggered by user input.
     */
    private void handleKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case DOWN -> {
                navigateSuggestions(1);
                event.consume();
            }
            case UP -> {
                navigateSuggestions(-1);
                event.consume();
            }
            case ENTER -> {
                if (model_widget.propCustom().getValue() && !isNavigatingSuggestions) {
                    String text = jfx_node.getText();
                    if (!text.isEmpty()) {
                        selectSuggestion(text);
                    }
                    event.consume();
                    return;
                }

                String selected = suggestionsListView.getSelectionModel().getSelectedItem();

                if (selected != null) {
                    selectSuggestion(selected);
                } else {
                    jfx_node.setText(currentValue);
                }

                event.consume();
            }
            case ESCAPE -> {
                jfx_node.getParent().requestFocus();
                suggestionsPopup.hide();
                event.consume();
            }
        }
    }

    /**
     * Navigate through the suggestions list based on the direction.
     * If direction is positive, it moves down; if negative, it moves up.
     *
     * @param direction 1 for down, -1 for up.
     */
    private void navigateSuggestions(int direction) {
        isNavigatingSuggestions = true;
        int currentIndex = suggestionsListView.getSelectionModel().getSelectedIndex();
        int newIndex = direction > 0 ?
            (currentIndex + 1) % suggestions.size() :
            currentIndex <= 0 ? suggestions.size() - 1 : currentIndex - 1;
        suggestionsListView.getSelectionModel().select(newIndex);
        suggestionsListView.scrollTo(newIndex);
    }

    /**
     * Method called when user is typing in the text field in order to update suggestions based
     * on filtering option.
     *
     * @param inputText new text input string.
     */
    private void updateSuggestions(String inputText) {
        isNavigatingSuggestions = false;
        if (inputText == null) {
            inputText = "";
        }

        if (inputText.length() < model_widget.propMinCharacters().getValue()) {
            suggestionsPopup.hide();
            return;
        }

        showFilteredSuggestions(model_widget.getFilteredSuggestions(inputText));
    }

    /**
     * Show suggestions based on the current text in the TextField.
     * If the TextField is empty, show all items.
     * If there are no items, hide the suggestions popup.
     */
    private void showSuggestions() {
        if (jfx_node.isDisabled() || !jfx_node.isEditable() || items.isEmpty()) return;

        String text = jfx_node.getText();
        List<String> toShow = (text == null || text.isEmpty()) ?
            items : model_widget.getFilteredSuggestions(text);

        showFilteredSuggestions(toShow);
    }

    /**
     * Show the filtered suggestions in the popup.
     *
     * @param filtered List of filtered suggestions to display.
     */
    private void showFilteredSuggestions(List<String> filtered) {
        suggestions.setAll(filtered);

        if (!suggestionsPopup.isShowing()) {
            showPopup(filtered.size());
        } else {
            updatePopupSize(filtered.size());
        }
    }

    /**
     * Show the suggestions popup below the text field.
     *
     * @param itemCount number of items to show in the popup.
     */
    private void showPopup(int itemCount) {
        Bounds bounds = jfx_node.localToScreen(jfx_node.getBoundsInLocal());

        updatePopupSize(itemCount);

        suggestionsPopup.show(jfx_node, bounds.getMinX(), bounds.getMaxY());
    }

    /**
     * Update the size of the suggestions popup based on the number of items.
     *
     * @param itemCount number of items in the suggestions list.
     */
    private void updatePopupSize(int itemCount) {
        double width = jfx_node.getWidth();
        suggestionsListView.setPrefWidth(width);
        suggestionsListView.setMaxWidth(width);
        suggestionsListView.setMinWidth(width);

        int visibleItems = Math.min(itemCount, model_widget.propMaxSuggestions().getValue());
        double maxHeight = Math.min(visibleItems * 23.5, 300);
        suggestionsListView.setPrefHeight(maxHeight);
        suggestionsListView.setMaxHeight(maxHeight);
    }

    /**
     * Selection of a suggestion in the dropdown.
     *
     * @param suggestion selected suggestion.
     */
    private void selectSuggestion(String suggestion) {
        isNavigatingSuggestions = false;
        jfx_node.textProperty().removeListener(textChangeListener);
        jfx_node.setText(suggestion);
        jfx_node.textProperty().addListener(textChangeListener);

        suggestionsPopup.hide();

        if (model_widget.runtimePropPVWritable().getValue()) {
            confirmValue(suggestion);
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

        Stream.of(
            model_widget.propWidth(), model_widget.propHeight(),
            model_widget.propForegroundColor(), model_widget.propBackgroundColor(),
            model_widget.propFont(), model_widget.propPlaceholder(),
            model_widget.runtimePropValue(), model_widget.propItemsFromPV(),
            model_widget.propItems(), model_widget.propEnabled(),
            model_widget.runtimePropPVWritable(), model_widget.propCustom()
        ).forEach(prop -> prop.addUntypedPropertyListener(listener));

        updateContent();
        updateStyle();
        updateEnable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void unregisterListeners() {
        Stream.of(
            model_widget.propWidth(), model_widget.propHeight(),
            model_widget.propForegroundColor(), model_widget.propBackgroundColor(),
            model_widget.propFont(), model_widget.propPlaceholder(),
            model_widget.runtimePropValue(), model_widget.propItemsFromPV(),
            model_widget.propItems(), model_widget.propEnabled(),
            model_widget.runtimePropPVWritable(), model_widget.propCustom()
        ).forEach(prop -> prop.removePropertyListener(listener));

        super.unregisterListeners();
    }

    /**
     * Confirm selected value and write it to PV or local variable.
     *
     * @param value Selected value by user.
     */
    private void confirmValue(String value) {
        if (!model_widget.runtimePropPVWritable().getValue() || value == null) return;

        if (!model_widget.propCustom().getValue()) {
            boolean valid = items.stream().anyMatch(item ->
                model_widget.propCaseSensitive().getValue() ?
                    item.equals(value) : item.equalsIgnoreCase(value));
            if (!valid) {
                jfx_node.setText(currentValue);
                return;
            }
        }

        if (model_widget.propConfirmDialog().getValue()) {
            String password = model_widget.propPassword().getValue();
            String message = model_widget.propConfirmMessage().getValue();

            if (!password.isEmpty()) {
                if (toolkit.showPasswordDialog(model_widget, message, password) == null) return;
            } else if (!toolkit.showConfirmationDialog(model_widget, message)) {
                return;
            }
        }

        try {
            VType currentPvValue = model_widget.runtimePropValue().getValue();
            Object mappedValue = (currentPvValue instanceof VEnum) ?
                FormatOptionHandler.parse(currentPvValue, value, FormatOption.DEFAULT) :
                parseValue(value);

            toolkit.fireWrite(model_widget, mappedValue);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error writing to PV", e);
        }
    }

    /**
     * Parses a string value into an appropriate type (Integer or Double).
     * If the value cannot be parsed, it returns the original string.
     *
     * @param value String value to parse.
     * @return Parsed value as Integer, Double, or original String if parsing fails.
     */
    private Object parseValue(String value) {
        try {
            return value.contains(".") ? Double.parseDouble(value) : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // This will handle String PVs, which are expected to be most likely
            VType converted = VType.toVType(value);
            return converted != null ? converted : value;
        }
    }

    /**
     * Handles property changes and marks the appropriate dirty flags.
     *
     * @param property The property that changed.
     * @param old      The old value of the property.
     * @param newVal   The new value of the property.
     */
    private void propertyChanged(WidgetProperty<?> property, Object old, Object newVal) {
        if (property == model_widget.propEnabled() || property == model_widget.runtimePropPVWritable()) {
            dirty_enable.mark();
        } else if (property == model_widget.runtimePropValue() ||
            property == model_widget.propItemsFromPV() ||
            property == model_widget.propItems() ||
            property == model_widget.propCustom()) {
            dirty_content.mark();
        } else {
            dirty_style.mark();
        }
        toolkit.scheduleUpdate(this);
    }

    /**
     * Update the enabled state of the TextField based on widget properties.
     * If the widget is not enabled or the PV is not writable, the TextField will be disabled and non-editable.
     */
    private void updateEnable() {
        boolean enabled = model_widget.propEnabled().getValue() && model_widget.runtimePropPVWritable().getValue();
        Styles.update(jfx_node, Styles.NOT_ENABLED, !enabled);
        jfx_node.setCursor(enabled ? Cursor.TEXT : Cursors.NO_WRITE);
        jfx_node.setEditable(enabled);
        jfx_node.setDisable(!enabled);
    }

    /**
     * Add a value to the list of suggestion items.
     *
     * @return List of computed items.
     */
    private List<String> computeItems() {
        VType value = model_widget.runtimePropValue().getValue();

        if (model_widget.propItemsFromPV().getValue() && value instanceof VEnum) {
            return ((VEnum) value).getDisplay().getChoices();
        }

        String itemsString = model_widget.propItems().getValue();
        if (itemsString == null || itemsString.trim().isEmpty()) {
            return List.of();
        }

        return Arrays.stream(itemsString.split("\n"))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .toList();
    }

    /**
     * Update the content of the TextField based on the widget properties.
     * This includes updating the items list and the current value displayed in the TextField.
     */
    private void updateContent() {
        items = computeItems();
        currentValue = VTypeUtil.getValueString(model_widget.runtimePropValue().getValue(), false);

        if (!toolkit.isEditMode()) {
            jfx_node.textProperty().removeListener(textChangeListener);
            model_widget.setItems(items);
            if (!jfx_node.getText().equals(currentValue)) {
                jfx_node.setText(currentValue);
            }
            jfx_node.textProperty().addListener(textChangeListener);
        }
    }

    /**
     * Update the style of the TextField based on widget properties.
     * This includes setting the background color, text color, font, and placeholder text.
     */
    private void updateStyle() {
        jfx_node.resize(model_widget.propWidth().getValue(), model_widget.propHeight().getValue());
        Font f = JFXUtil.convert(model_widget.propFont().getValue());

        jfx_node.setStyle(MessageFormat.format(
            "-fx-background-color: {0}; -fx-text-fill: {1}; -fx-font: {2} {3}px \"{4}\"; " +
                "-fx-border-color: derive({0}, -20%); -fx-border-width: 1px; " +
                "-fx-border-radius: 3px; -fx-background-radius: 3px;",
            JFXUtil.webRgbOrHex(model_widget.propBackgroundColor().getValue()),
            JFXUtil.webRgbOrHex(model_widget.propForegroundColor().getValue()),
            f.getStyle().toLowerCase().replace("regular", "normal"),
            f.getSize(), f.getFamily()
        ));

        jfx_node.setPromptText(model_widget.propPlaceholder().getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateChanges() {
        super.updateChanges();

        if (dirty_style.checkAndClear()) updateStyle();
        if (dirty_content.checkAndClear()) updateContent();
        if (dirty_enable.checkAndClear() && !toolkit.isEditMode()) updateEnable();

        jfx_node.layout();
    }
}
