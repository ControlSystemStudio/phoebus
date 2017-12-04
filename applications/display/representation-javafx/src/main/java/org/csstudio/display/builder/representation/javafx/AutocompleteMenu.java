/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import org.csstudio.display.builder.representation.javafx.autocomplete.Suggestion;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/** Creates a menu for use with auto-completion. Fields which edit
 *  auto-completeable properties should use attachField on the related
 *  TextInputControl to attach the menu, and removeField to unregister listeners.
 *  For meaningful auto-completion, an {@link AutocompleteMenuUpdater} should be
 *  given which implements methods to request result entries for a given value,
 *  calling setResults as results arrive, and to update the history of the menu.
 *
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class AutocompleteMenu
{
    public static void attachStylesheet(final Scene scene)
    {
        scene.getStylesheets().add(AutocompleteMenu.class.getResource("opibuilder.css").toExternalForm());
    }


    private final ContextMenu menu = new ContextMenu();
    private AutocompleteMenuUpdater updater = null;
    private List<ControlWrapper> fields = new ArrayList<ControlWrapper>();
    private TextInputControl current_field = null;

    private class ControlWrapper
    {
        private final TextInputControl field;
        private final ChangeListener<Boolean> focused_listener;
        private final ChangeListener<String> text_listener;
        private final EventHandler<KeyEvent> key_handler;
        private final EventHandler<KeyEvent> submit_handler;

        ControlWrapper(final TextInputControl field)
        {
            this.field = field;

            // Toggle menu on Ctrl/Command-Space
            key_handler = event ->
            {
                if (event.isShortcutDown()  &&  event.getCode() == KeyCode.SPACE)
                    if (menu.isShowing())
                        menu.hide();
                    else
                        menu.show(field, Side.BOTTOM, 0, 0);
            };

            focused_listener = (obs, oldval, newval) ->
            {
                menu.hide();
                if (newval)
                    current_field = field;
            };

            text_listener = (obs, oldval, text) ->
            {
                if (! field.isFocused())
                    return;
                if (updater != null)
                    updater.requestEntries(text);
                if (!menu.isShowing())
                    menu.show(field, Side.BOTTOM, 0, 0);
            };

            submit_handler = (event) ->
            {
                if (event.getCode() == KeyCode.ENTER)
                    updateHistory(field.getText());
            };

            field.addEventFilter(KeyEvent.KEY_PRESSED, key_handler);
            field.focusedProperty().addListener(focused_listener);
            field.addEventHandler(KeyEvent.KEY_RELEASED, submit_handler);
            field.textProperty().addListener(text_listener);
        }

        protected void unbind()
        {
            field.textProperty().removeListener(text_listener);
            field.removeEventHandler(KeyEvent.KEY_RELEASED, submit_handler);
            field.focusedProperty().removeListener(focused_listener);
            field.removeEventFilter(KeyEvent.KEY_PRESSED, key_handler);
        }
    }

    private class Result
    {
        private final CustomMenuItem label;
        private final List<String> results;
        protected int expected; //expected index of results (in result_list)

        protected Result(final String label, final List<String> results, final int expected)
        {
            this.label = createHeaderItem(label);
            this.results = new ArrayList<String>(results);
            this.expected = expected;
        }

        protected void addItemsTo(final List<MenuItem> items)
        {
            items.add(label);
            for (String result : results)
                items.add(createMenuItem(result));
        }

        protected boolean textIs(final String str)
        {
            return ((Text) label.getContent()).getText().equals(str);
        }

        @Override
        public String toString()
        {
            return ((Text) label.getContent()).getText() + " at " + expected + " (" + results.size() + "): "
                    + results.toString();
        }
    }

    private final List<Result> result_list = new LinkedList<Result>();

    public AutocompleteMenu()
    {
        // The drop-down menu which lists suggestions happens
        // to capture ENTER keys.
        //
        // When user types PV name into current_field and presses ENTER,
        // the menu captures that ENTER key and uses it to hide the menu.
        // --> Need to send ENTER down to current_field.
        //
        // If user selects one of the suggested menu items
        // and presses enter, menu item will update the current_field
        // --> Need to send ENTER down to current_field _after_
        //     the menu item updated current_field.
        menu.addEventFilter(KeyEvent.KEY_PRESSED, event ->
        {
            if (event.getCode() == KeyCode.ENTER && current_field != null)
                Platform.runLater(() ->
                {
                    if (current_field != null)
                        Event.fireEvent(current_field, event);
                });
        });
    }

    /**
     * Attach a field to the menu (add a field to the menu's list of monitored
     * controls)
     *
     * @param field Control to add
     */
    public void attachField(final TextInputControl field)
    {
        fields.add(new ControlWrapper(field));
    }

    /**
     * Remove the auto-completed field from the menu's list of monitored
     * controls
     */
    public void removeField(final TextInputControl control)
    {
        if (current_field != null && current_field.equals(control))
        {
            menu.hide();
            current_field = null;
        }
        synchronized (fields)
        {
            for (ControlWrapper cw : fields)
            {
                if (cw.field.equals(control))
                {
                    cw.unbind();
                    fields.remove(cw);
                    break;
                }
            }
        }
    }

    public void removeFields()
    {
        menu.hide();
        current_field = null;
        synchronized (fields)
        {
            for (ControlWrapper cw : fields)
            {
                cw.unbind();
                fields.remove(cw);
            }
        }
    }

    /**
     * Set updater interface which is used to update the menu as text changes or
     * values are submitted.
     *
     * @param results_updater
     */
    public void setUpdater(final AutocompleteMenuUpdater results_updater)
    {
        updater = results_updater;
    }

    /** To be called from {@link AutocompleteMenuUpdater}
     *
     *  @param label Label for this result set, for example "History"
     *  @param entries Results
     */
    // TODO Delete, use next one with List<Suggestion>
    public void setResults(final String label, final List<Suggestion> suggestions)
    {
        final List<String> entries = suggestions.stream().map(Suggestion::getValue).collect(Collectors.toList());
        setResults(label, entries, 0);
    }

    /**
     * Set the results for the provider with the given label at the given index.
     *
     * @param label Label for results provider or category
     * @param results List of results to be shown
     * @param index Expected index (with respect to labels) of results
     */
    // TODO use 'priority' to place result in order
    public void setResults(final String label, final List<String> results, int index)
    {
        if (label == null)
            return;

        //System.out.println("results for " + label + " at " + index);

        final List<MenuItem> items = new LinkedList<MenuItem>();

        synchronized (result_list)
        {
            final ListIterator<Result> it = result_list.listIterator();
            Result result;
            if (it.hasNext())
            {
                result = it.next();
                while (result.expected < index)
                {
                    if (result.textIs(label))
                        it.remove();
                    else
                        result.addItemsTo(items);
                    if (it.hasNext())
                        result = it.next();
                    else
                        break;
                }
                if (result.expected >= index && it.hasPrevious())
                    it.previous();
                else
                    result.addItemsTo(items);
            }
            result = new Result(label, results, index);
            it.add(result);
            result.addItemsTo(items);
            while (it.hasNext())
            {
                result = it.next();
                if (result.expected <= index)
                    result.expected++;
                if (result.expected >= index)
                    index++;
                if (result.textIs(label))
                    it.remove();
                else
                    result.addItemsTo(items);
            }
        }

        //for (Result result : result_list)
        //System.out.println(result);

        // Must make changes to JavaFX ContextMenu object from JavaFX thread
        Platform.runLater(() -> menu.getItems().setAll(items));
    }

    /**
     * Add the given history to the entry (uses AutocompleteMenuUpdater if
     * non-null).
     *
     * @param entry
     */
    public void updateHistory(final String entry)
    {
        if (updater != null)
            updater.updateHistory(entry);
        else
        {   // add entry to top of menu items (for the particular autocomplete menu instance)
            // (Currently, there are two instances of this class in the editor: one for the inline editor, one for the palette)
            final List<MenuItem> items = menu.getItems();
            // remove entry if present, to avoid duplication
            items.removeIf((item) -> item.getText().equals(entry));
            items.add(0, createMenuItem(entry));
        }
    }

    private final CustomMenuItem createHeaderItem(final String header)
    {
        // Use CustomMenuItem so that 'selecting' this item doesn't hide the menu.
        final CustomMenuItem item = new CustomMenuItem(new Text(header), false);
        item.getStyleClass().add("ac-menu-label");
        item.setMnemonicParsing(false);
        item.setDisable(true);
        return item;
    }

    private final MenuItem createMenuItem(final String text)
    {
        // TODO Use actual comment from Suggestion
        final Text comment = new Text("Comment");
        comment.setFill(Color.BLUE);
        final TextFlow flow = new TextFlow(new Text(text), comment);
        final MenuItem item = new MenuItem("", flow);
        item.setOnAction((event) ->
        {
            if (current_field == null)
                return;
            current_field.setText(text);
            // Menu's key_pressed handler will send ENTER on to current_field
        });
        item.getStyleClass().add("ac-menu-item");
        item.setMnemonicParsing(false);
        return item;
    }
}
