/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.autocomplete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import org.phoebus.framework.autocomplete.Proposal;
import org.phoebus.framework.autocomplete.SimProposal;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/** Menu for use with auto-completion.
 *
 *  <p>Fields call <code>attachField</code> to add
 *  a context menu with proposals.
 *
 *  @author Amanda Carpenter - Original version in Display Builder
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AutocompleteMenu
{
    /** Result of a lookup, packaged for display in menu. */
    private class Result
    {
        final MenuItem header;
        final int priority;
        final List<Proposal> proposals;

        Result(final String name, final int priority, final List<Proposal> proposals)
        {
            // Ideally, use item that can not be selected, like SeparatorMenuItem..
            final Text label = new Text(name);
            header = new CustomMenuItem(label, false);
            label.setFont(header_font);
            label.setUnderline(true);
            this.priority = priority;
            this.proposals = proposals;
        }
    }

    private final TreeSet<Result> results = new TreeSet<>((a, b) -> a.priority - b.priority);

    /** Context menu, shared by all attached fields,
     *  because shown for at most one field at a time.
     */
    private final ContextMenu menu = new ContextMenu();

    /** Toggle menu on Ctrl-Space */
    private final EventHandler<KeyEvent> key_handler = event ->
    {
        // For Mac, isShortcutDown() to detect Command-SPACE
        // seemed natural, but that is already captured by OS
        // for 'Spotlight Search',
        // so use Ctrl-Space on all OS
        if (event.isControlDown()  &&  event.getCode() == KeyCode.SPACE)
        {
            if (menu.isShowing())
                menu.hide();
            else if (event.getSource() instanceof TextField)
            {
                final TextField field = (TextField) event.getSource();
                // Show menu with current content,
                // in case we were hiding and how showing the menu
                // for the same field, not loosing focus,
                // menu already populated
                menu.show(field, Side.BOTTOM, 0, 0);
                // If menu is empty, otherwise to 'refresh', start a lookup
                lookup(field);
            }
            event.consume();
        }
    };

    /** Hide and clear menu as well as results when field looses focus */
    private final ChangeListener<Boolean> focused_listener = (p, old, focus) ->
    {
        if (! focus)
        {
            menu.hide();
            menu.getItems().clear();
            synchronized (results)
            {
                results.clear();
            }
        }
    };

    /** On 'ENTER', add the manually entered value to history */
    private final EventHandler<KeyEvent> submit_handler = event ->
    {
        if (event.getCode() == KeyCode.ENTER  &&
            event.getSource() instanceof TextField)
        {
            final TextField field = (TextField) event.getSource();
            updateHistory(field.getText());
            menu.hide();
        }
    };

    /** Handler for autocompletion on one field
     *
     *  Since not all event handlers receive the 'field',
     *  this field-specific wrapper is required.
     */
    private class FieldHandler
    {
        private TextInputControl field;

        /** As user types, fetch proposals */
        private final InvalidationListener text_listener = prop -> lookup(field);

        FieldHandler(final TextInputControl field)
        {
            this.field = field;
            field.addEventFilter(KeyEvent.KEY_PRESSED, key_handler);
            field.addEventHandler(KeyEvent.KEY_RELEASED, submit_handler);
            field.textProperty().addListener(text_listener);
            field.focusedProperty().addListener(focused_listener);
        }

        void unbind()
        {
            field.focusedProperty().removeListener(focused_listener);
            field.textProperty().removeListener(text_listener);
            field.removeEventHandler(KeyEvent.KEY_RELEASED, submit_handler);
            field.removeEventFilter(KeyEvent.KEY_PRESSED, key_handler);
        }
    }

    /** Map of handlers for known fields */
    private final Map<TextInputControl, FieldHandler> handlers = new HashMap<>();

    /** Font used to highlight section of proposal */
    private final Font header_font, highlight_font;

    /** Create autocomplete menu */
    public AutocompleteMenu()
    {
        final Font default_font = Font.getDefault();
        header_font = Font.font(default_font.getFamily(), FontWeight.EXTRA_BOLD, default_font.getSize()+1);
        highlight_font = Font.font(default_font.getFamily(), FontWeight.EXTRA_BOLD, default_font.getSize());

        menu.addEventFilter(KeyEvent.KEY_PRESSED, key_handler);
    }

    /** @param field Field for which autocompletion is requested */
    public void attachField(final TextInputControl field)
    {
        final FieldHandler previous = handlers.put(field, new FieldHandler(field));
        if (previous != null)
            throw new IllegalStateException();
    }

    /** @param field Field for which autocompletion is no longer desired */
    public void detachField(final TextInputControl field)
    {
        Objects.requireNonNull(handlers.get(field), "Unknown field").unbind();
    }

    private void updateHistory(final String text)
    {
        // TODO
        System.out.println("Add to history: " + text);
    }

    private void lookup(final TextInputControl field)
    {
        // TODO Actual lookup...


        synchronized (results)
        {
            // Merge proposals
            results.add(new Result("History", 3, List.of(
                new Proposal("Ene"),
                new Proposal("Mene"))));
            results.add(new Result("sim", 1, List.of(
                new SimProposal("sim://sine", "min", "max", "update_seconds"),
                new SimProposal("sim://sine", "min", "max", "steps", "update_seconds"),
                new Proposal("sim://noise"))));
            results.add(new Result("Local", 2, List.of(
                new Proposal("Uno"),
                new Proposal("Due"))));
        }

        // Create menu items: Header for each result,
        // then list proposals
        final String text = field.getText();
        final List<MenuItem> items = new ArrayList<>();
        synchronized (results)
        {
            for (Result result : results)
            {
                items.add(result.header);
                for (Proposal proposal : result.proposals)
                    items.add(createMenuItem(field, text, proposal));
            }
        }

        // Update and show menu on UI thread
        Platform.runLater(() ->
        {
            menu.getItems().setAll(items);
            if (! menu.isShowing())
                menu.show(field, Side.BOTTOM, 0, 0);
        });
    }

    /** @param field Field where user entered text
     *  @param text Text entered by user
     *  @param proposal Matching proposal
     *  @return MenuItem that will apply the proposal
     */
    private MenuItem createMenuItem(final TextInputControl field,
                                    final String text, final Proposal proposal)
    {
        // Determine which section of proposal matches text
        final String description = proposal.getDescription();
        final int pos = description.indexOf(text);

        final MenuItem item;
        // Text does not match the verbatim proposal?!
        if (pos < 0)
            item = new MenuItem(null, new Label(description));
        else
        {   // Create formatted text

            // Highlight the matching section
            // start of proposal .. matching text .. rest of proposal
            final TextFlow markup = new TextFlow();
            if (pos > 0)
                markup.getChildren().add(new Label(description.substring(0, pos)));

            final Label match = new Label(text);
            match.setTextFill(Color.BLUE);
            match.setFont(highlight_font);
            markup.getChildren().add(match);

            final int rest = pos + text.length();
            if (description.length() > rest)
                markup.getChildren().add(new Label(description.substring(rest)));

            // TODO Add parameter info

            item = new MenuItem(null, markup);
        }

        item.setOnAction(event ->
        {
            field.setText(proposal.apply(text));
            // Menu's key_pressed handler will send ENTER on to current_field
        });

        return item;
    }
}
