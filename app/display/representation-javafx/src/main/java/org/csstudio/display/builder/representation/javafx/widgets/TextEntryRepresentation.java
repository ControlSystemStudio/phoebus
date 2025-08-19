/*******************************************************************************
 * Copyright (c) 2015-2024 Oak Ridge National Laboratory.
 * Copyright (c) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import java.util.stream.Stream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.stage.Popup;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.widgets.PVWidget;
import org.csstudio.display.builder.model.widgets.TextEntryWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.Alarm;
import org.epics.vtype.VEnum;
import org.epics.vtype.VType;
import org.phoebus.ui.vtype.FormatOption;
import org.phoebus.ui.vtype.FormatOptionHandler;

import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Region;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 *  @author Thales
 */
@SuppressWarnings("nls")
public class TextEntryRepresentation extends RegionBaseRepresentation<TextInputControl, TextEntryWidget>
{
    /** Is user actively editing the content, so updates should be suppressed?
     *
     *  <p>Only updated on the UI thread,
     *  but also read when receiving new value
     */
    private boolean active = false;
    private volatile boolean enabled = true;

    private final DirtyFlag dirty_size = new DirtyFlag();
    private final DirtyFlag dirty_style = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final UntypedWidgetPropertyListener sizeListener = this::sizeChanged;
    private final UntypedWidgetPropertyListener styleListener = this::styleChanged;
    private final UntypedWidgetPropertyListener contentListener = this::contentChanged;
    private final WidgetPropertyListener<String> pvNameListener = this::pvnameChanged;
    private volatile String value_text = "<?>";

    // autocomplete
    private List<String> items = List.of();
    private Popup suggestionsPopup;
    private ListView<String> suggestionsListView;
    private ObservableList<String> suggestions;
    private javafx.beans.value.ChangeListener<String> textChangeListener;

    private static final WidgetColor active_color = WidgetColorService.getColor(NamedWidgetColors.ACTIVE_TEXT);

    private volatile Pos pos;

    /**
     * Pseudo class used to right align text in multi-line {@link TextArea} widget,
     * see opibuilder.css.
     */
    private final PseudoClass rightAlignedText = PseudoClass.getPseudoClass("right");
    /**
     * Pseudo class used to center text in multi-line {@link TextArea} widget,
     * see opibuilder.css.
     */
    private final PseudoClass centeredText = PseudoClass.getPseudoClass("center");
    /**
     * Pseudo class used to left align text in multi-line {@link TextArea} widget,
     * see opibuilder.css.
     */
    private final PseudoClass leftAlignedText = PseudoClass.getPseudoClass("left");

    @Override
    public TextInputControl createJFXNode() throws Exception
    {
    	value_text = computeText(null);

    	// Note implementation choice:
    	// "multi_line" and "wrap_words" cannot change at runtime.
    	// In editor, there is no visible difference,
    	// and at runtime changes are simply not supported.
    	final TextInputControl text;
        if (model_widget.propMultiLine().getValue())
        {
            final TextArea area = new TextArea();
            area.setWrapText(model_widget.propWrapWords().getValue());
            text = area;
        }
        else
            text = new TextField();
        text.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        text.getStyleClass().add("text_entry");

        if (! toolkit.isEditMode())
        {
            initPopup();
            setupEventHandlers(text);
        }

        // Non-managed widget reduces expensive Node.notifyParentOfBoundsChange() calls.
        // Code below is prepared to handle non-managed widget,
        // but multi-line version behaves oddly when not managed:
        // Cursor not shown, selection not shown,
        // unclear where entered text will appear.
        // Even when _only_ the single-line version is unmanaged,
        // the multi-line version will get into this state.
        // -> Keep managed.
        //        if (!isMultiLine())
        //            text.setManaged(false);

        return text;
    }

    @Override
    protected boolean isFilteringEditModeClicks()
    {
        return true;
    }


    /**
     * Initializes the suggestions popup and its ListView.
     * The popup will display a list of suggestions based on user input.
     */
    private void initPopup()
    {
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
    private void setupEventHandlers(TextInputControl textField)
    {
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
                // For multi-line, submit on exit because users
                // cannot remember Ctrl-Enter.
                // For plain text field, require Enter to submit
                // and cancel editing when focus is lost.
                if (isMultiLine())
                    submit();
                setActive(false);

                suggestionsPopup.hide();
                textField.textProperty().removeListener(textChangeListener);
                restore();
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
    private void handleKeyPressed(KeyEvent event)
    {
        switch (event.getCode()) {
            case TAB -> {
                // For multiline, it's like any other entered key.
                if (model_widget.propMultiLine().getValue() && !event.isShiftDown())
                    setActive(true);
                // Otherwise results in lost focus and is handled as thus
                event.consume();
            }
            case DOWN -> {
                if (isMultiLine())
                    return;

                navigateSuggestions(1);
                event.consume();
            }
            case UP -> {
                if (isMultiLine())
                    return;

                navigateSuggestions(-1);
                event.consume();
            }
            case ENTER -> {
                if (suggestionsPopup.isShowing()) {
                    String selected = suggestionsListView.getSelectionModel().getSelectedItem();

                    if (selected == null && !suggestionsListView.getItems().isEmpty()) {
                        int idx = suggestionsListView.getSelectionModel().getSelectedIndex();
                        if (idx < 0) idx = 0;
                        suggestionsListView.getSelectionModel().select(idx);
                        selected = suggestionsListView.getSelectionModel().getSelectedItem();
                    }

                    if (selected != null) {
                        selectSuggestion(selected);
                        event.consume();
                        return;
                    }
                }

                if (isMultiLine() && !event.isShortcutDown()) {
                    suggestionsPopup.hide();
                    setActive(true);
                    event.consume();
                    return;
                }

                if (!model_widget.propCustom().getValue() && !suggestions.isEmpty())
                    restore();
                submit();
                setActive(false);
                event.consume();
            }
            case ESCAPE -> {
                jfx_node.getParent().requestFocus();
                suggestionsPopup.hide();
                restore();
                setActive(false);
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
    private void navigateSuggestions(int direction)
    {
        if (suggestions == null || suggestions.isEmpty())
            return;

        final int size = suggestions.size();
        final MultipleSelectionModel<String> sm = suggestionsListView.getSelectionModel();
        int currentIndex = sm.getSelectedIndex();

        if (currentIndex < 0) {
            currentIndex = (direction > 0) ? 0 : size - 1;
        } else {
            if (direction > 0) {
                currentIndex = (currentIndex + 1) % size;
            } else {
                currentIndex = (currentIndex - 1 + size) % size;
            }
        }

        sm.select(currentIndex);
        if (currentIndex >= 0 && currentIndex < size) {
            suggestionsListView.scrollTo(currentIndex);
        }
    }

    /**
     * Method called when user is typing in the text field in order to update suggestions based
     * on filtering option.
     *
     * @param inputText new text input string.
     */
    private void updateSuggestions(String inputText)
    {
        if (inputText == null) {
            inputText = "";
        }

        int min = model_widget.propMinCharacters().getValue();

        if (inputText.length() < min) {
            showSuggestions();
            return;
        }

        showFilteredSuggestions(model_widget.getFilteredSuggestions(inputText));
    }

    /**
     * Show suggestions based on the current text in the TextField.
     * If the TextField is empty, show all items.
     * If there are no items, hide the suggestions popup.
     */
    private void showSuggestions()
    {
        if (!jfx_node.isFocused()) {
            suggestionsPopup.hide();
            return;
        }
        if (jfx_node.isDisabled() || !jfx_node.isEditable() || items.isEmpty()) return;

        String text = jfx_node.getText();
        List<String> toShow = (text == null || text.isEmpty())
            ? items
            : model_widget.getFilteredSuggestions(text);

        showFilteredSuggestions(toShow);
    }

    /**
     * Show the filtered suggestions in the popup.
     *
     * @param filtered List of filtered suggestions to display.
     */
    private void showFilteredSuggestions(List<String> filtered)
    {
        if (!jfx_node.isFocused()) {
            suggestionsPopup.hide();
            return;
        }

        if (filtered.isEmpty()) {
            suggestionsPopup.hide();
            return;
        }

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
    private void updatePopupSize(int itemCount)
    {
        double width = jfx_node.getWidth();
        suggestionsListView.setPrefWidth(width);
        suggestionsListView.setMaxWidth(width);
        suggestionsListView.setMinWidth(width);

        double maxHeight = Math.min(itemCount * 23.5, 300);
        suggestionsListView.setPrefHeight(maxHeight);
        suggestionsListView.setMaxHeight(maxHeight);
    }

    /**
     * Selection of a suggestion in the dropdown.
     *
     * @param suggestion selected suggestion.
     */
    private void selectSuggestion(String suggestion)
    {
        jfx_node.textProperty().removeListener(textChangeListener);
        jfx_node.setText(suggestion);
        jfx_node.textProperty().addListener(textChangeListener);

        suggestionsPopup.hide();

        if (model_widget.runtimePropPVWritable().getValue()) {
            confirmValue(suggestion);
        }
    }

    /**
     * Confirm selected value and write it to PV or local variable.
     *
     * @param value Selected value by user.
     */
    private void confirmValue(String value)
    {
        if (!model_widget.runtimePropPVWritable().getValue() || value == null) return;

        if (!model_widget.propCustom().getValue()) {
            boolean valid = items.stream().anyMatch(item ->
                model_widget.propCaseSensitive().getValue() ?
                    item.equals(value) : item.equalsIgnoreCase(value));
            if (!valid) {
                restore();
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
    private Object parseValue(String value)
    {
        try {
            return value.contains(".") ? Double.parseDouble(value) : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            VType converted = VType.toVType(value);
            return converted != null ? converted : value;
        }
    }

    private void setActive(final boolean active)
    {
        if (this.active == active)
            return;

        // When activated, start by selecting all in a plain text.
        // For multi-line, leave it to the user to click or cursor around,
        // because when all is selected, there's a larger risk of accidentally
        // replacing some long, carefully crafted text.
        if (active  &&  !isMultiLine())
            jfx_node.selectAll();

        // Don't enable when widget is disabled
        if (active  &&  !model_widget.propEnabled().getValue())
            return;
        this.active = active;
        dirty_style.mark();
        updateChanges();
    }

    /** @return Using the multi-line TextArea? */
    private boolean isMultiLine()
    {
        return jfx_node instanceof TextArea;
    }

    /** Restore representation to last known value,
     *  replacing what user might have entered
     */
    private void restore()
    {
        jfx_node.setText(value_text);
    }

    /** Submit value entered by user */
    private void submit()
    {
        if (enabled) {
            // Strip 'units' etc. from text
            final String text = jfx_node.getText();
    
            final Object value = FormatOptionHandler.parse(model_widget.runtimePropValue().getValue(), text,
                                                           model_widget.propFormat().getValue());
            logger.log(Level.FINE, "Writing '" + text + "' as " + value + " (" + value.getClass().getName() + ")");
            toolkit.fireWrite(model_widget, value);
    
            // Wrote value. Expected is either
            // a) PV receives that value, PV updates to
            //    submitted value or maybe a 'clamped' value
            // --> We'll receive contentChanged() and display PV's latest.
            // b) PV doesn't receive the value and never sends
            //    an update. JFX control is stuck with the 'text'
            //    the user entered, not reflecting the actual PV
            // --> Request an update to the last known 'value_text'.
            //
            // This could result in a little flicker:
            // User enters "new_value".
            // We send that, but restore "old_value" to handle case b)
            // PV finally sends "new_value", and we show that.
            //
            // In practice, this rarely happens because we only schedule an update.
            // By the time it executes, we already have case a.
            // If it does turn into a problem, could introduce toolkit.scheduleDelayedUpdate()
            // so that case b) only restores the old 'value_text' after some delay,
            // increasing the chance of a) to happen.
            dirty_content.mark();
            toolkit.scheduleUpdate(this);
        }
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();

        pos = JFXUtil.computePos(model_widget.propHorizontalAlignment().getValue(),
                model_widget.propVerticalAlignment().getValue());
        model_widget.propWidth().addUntypedPropertyListener(sizeListener);
        model_widget.propHeight().addUntypedPropertyListener(sizeListener);

        model_widget.propForegroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propFont().addUntypedPropertyListener(styleListener);
        model_widget.propEnabled().addUntypedPropertyListener(styleListener);
        model_widget.runtimePropPVWritable().addUntypedPropertyListener(styleListener);

        model_widget.propFormat().addUntypedPropertyListener(contentListener);
        model_widget.propPrecision().addUntypedPropertyListener(contentListener);
        model_widget.propShowUnits().addUntypedPropertyListener(contentListener);
        model_widget.runtimePropValue().addUntypedPropertyListener(contentListener);

        model_widget.propPVName().addPropertyListener(pvNameListener);
        model_widget.propHorizontalAlignment().addUntypedPropertyListener(styleListener);
        model_widget.propVerticalAlignment().addUntypedPropertyListener(styleListener);

        Stream.of(
            model_widget.propPlaceholder(), model_widget.propItemsFromPV(),
            model_widget.propItems(), model_widget.propCustom()
        ).forEach(prop -> prop.addUntypedPropertyListener(contentListener));

        contentChanged(null, null, null);
    }

    /**
     * Add a value to the list of suggestion items.
     *
     * @return List of computed items.
     */
    private List<String> computeItems()
    {
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

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizeListener);
        model_widget.propHeight().removePropertyListener(sizeListener);
        model_widget.propForegroundColor().removePropertyListener(styleListener);
        model_widget.propBackgroundColor().removePropertyListener(styleListener);
        model_widget.propFont().removePropertyListener(styleListener);
        model_widget.propEnabled().removePropertyListener(styleListener);
        model_widget.runtimePropPVWritable().removePropertyListener(styleListener);
        model_widget.propFormat().removePropertyListener(contentListener);
        model_widget.propPrecision().removePropertyListener(contentListener);
        model_widget.propShowUnits().removePropertyListener(contentListener);
        model_widget.runtimePropValue().removePropertyListener(contentListener);
        model_widget.propPVName().removePropertyListener(pvNameListener);
        model_widget.propHorizontalAlignment().removePropertyListener(styleListener);
        model_widget.propVerticalAlignment().removePropertyListener(styleListener);

        Stream.of(
            model_widget.propPlaceholder(), model_widget.propItemsFromPV(),
            model_widget.propItems(), model_widget.propCustom()
        ).forEach(prop -> prop.removePropertyListener(contentListener));

        super.unregisterListeners();
    }

    @Override
    protected void attachTooltip()
    {
        // Use the formatted text for "$(pv_value)"
        TooltipSupport.attach(jfx_node, model_widget.propTooltip(), () -> value_text);
    }

    private void sizeChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    private void styleChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        pos = JFXUtil.computePos(model_widget.propHorizontalAlignment().getValue(),
                model_widget.propVerticalAlignment().getValue());
        dirty_style.mark();
        toolkit.scheduleUpdate(this);
    }

    /** @param value Current value of PV
     *  @return Text to show, "<pv name>" if disconnected (no value)
     */
    private String computeText(final VType value)
    {
        Alarm alarm = Alarm.alarmOf(value);
        if (value == null || alarm.equals(Alarm.disconnected()))
            return "<" + model_widget.propPVName().getValue() + ">";
        if (value == PVWidget.RUNTIME_VALUE_NO_PV)
            return "";
        return FormatOptionHandler.format(value,
                                          model_widget.propFormat().getValue(),
                                          model_widget.propPrecision().getValue(),
                                          model_widget.propShowUnits().getValue());
    }

    private void pvnameChanged(final WidgetProperty<String> property, final String old_value, final String new_value)
    {   // PV name typically changes in edit mode.
        // -> Show new PV name.
        // Runtime could deal with disconnect/reconnect for new PV name
        // -> Also OK to show disconnected state until runtime
        //    subscribes to new PV, so we eventually get values from new PV.
        value_text = computeText(null);
        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    private void contentChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        value_text = computeText(model_widget.runtimePropValue().getValue());
        items = computeItems();

        if (!toolkit.isEditMode()) {
            model_widget.setItems(items);
        }

        dirty_content.mark();
        if (! active)
            toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_size.checkAndClear())
        {
            if (jfx_node.isManaged())
                jfx_node.setPrefSize(model_widget.propWidth().getValue(),
                                     model_widget.propHeight().getValue());
            else
                jfx_node.resize(model_widget.propWidth().getValue(),
                                model_widget.propHeight().getValue());
        }
        if (dirty_style.checkAndClear())
        {
            final StringBuilder style = new StringBuilder(100);
            style.append("-fx-text-fill:");
            JFXUtil.appendWebRGB(style, model_widget.propForegroundColor().getValue()).append(";");

            // http://stackoverflow.com/questions/27700006/how-do-you-change-the-background-color-of-a-textfield-without-changing-the-border
            final WidgetColor back_color = active ? active_color : model_widget.propBackgroundColor().getValue();
            style.append("-fx-control-inner-background: ");
            JFXUtil.appendWebRGB(style, back_color).append(";");
            jfx_node.setStyle(style.toString());

            jfx_node.setFont(JFXUtil.convert(model_widget.propFont().getValue()));

            // Enable if enabled by user and there's write access
            enabled = model_widget.propEnabled().getValue()  &&
                                    model_widget.runtimePropPVWritable().getValue();
            // Don't disable the widget, because that would also remove the
            // context menu etc.
            // Just apply a style that matches the disabled look.
            jfx_node.setEditable(enabled);
            setDisabledLook(enabled, jfx_node.getChildrenUnmodifiable());


            if(jfx_node instanceof TextField){
                ((TextField)jfx_node).setAlignment(pos);
            }
            else if(jfx_node instanceof TextArea){
                // First remove pseudo classes...
                jfx_node.pseudoClassStateChanged(rightAlignedText, false);
                jfx_node.pseudoClassStateChanged(leftAlignedText, false);
                jfx_node.pseudoClassStateChanged(centeredText, false);
                // ... the add the pseudo class that corresponds to horizontal alignment property value
                String alignment = model_widget.propHorizontalAlignment().getValue().toString().toLowerCase();
                PseudoClass alignmentClass = PseudoClass.getPseudoClass(alignment);
                jfx_node.pseudoClassStateChanged(alignmentClass, true);
            }
        }
        if (! active)
        {
            if (dirty_content.checkAndClear())
            {
                // For middle-aligned multi-line text, keep the scroll position
                final TextArea area = jfx_node instanceof TextArea ? (TextArea) jfx_node : null;
                final VerticalAlignment align = model_widget.propVerticalAlignment().getValue();
                double pos = 0;
                if (area != null  &&  align == VerticalAlignment.MIDDLE)
                    pos = area.getScrollTop();

                jfx_node.setText(value_text);

                if (area != null  &&  pos != 0)
                    area.setScrollTop(pos);
                // For bottom scroll detail, see comments in TextUpdateRepresentation
                if (area != null && align == VerticalAlignment.BOTTOM)
                {
                    area.selectRange(0, 1);
                    toolkit.schedule(() -> area.selectRange(value_text.length(), value_text.length()), 500, TimeUnit.MILLISECONDS);
                }
            }
        }
        // When not managed, trigger layout
        if (!jfx_node.isManaged())
            jfx_node.layout();
    }
}
