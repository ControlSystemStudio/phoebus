/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.autocomplete;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.phoebus.framework.autocomplete.MatchSegment;
import org.phoebus.framework.autocomplete.Proposal;
import org.phoebus.framework.autocomplete.ProposalService;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextFlow;

/** Menu for use with auto-completion.
 *
 *  <p>Fields call <code>attachField</code> to add
 *  a context menu with proposals.
 *
 *  @author Amanda Carpenter - Original version in Display Builder
 *  @author Kay Kasemir
 */
public class AutocompleteMenu
{
    /** Result of a lookup, packaged for display in menu. */
    private class Result
    {
        final SeparatorMenuItem header;
        final int priority;
        final List<Proposal> proposals;

        Result(final String name, final int priority, final List<Proposal> proposals)
        {
            // Create non-selectable menu item
            // CustomMenuItem seems natural, but it's traversed when using menu.
            // http://tiwulfx.panemu.com/2013/01/02/creating-custom-menu-separator-in-javafx
            // describes use of SeparatorMenuItem.setContent().
            final Label label = new Label(name);
            label.setFont(header_font);
            header = new SeparatorMenuItem();
            header.setContent(label);

            this.priority = priority;
            this.proposals = proposals;
        }
    }

    private final ProposalService proposal_service;

    private final TreeSet<Result> results = new TreeSet<>((a, b) -> a.priority - b.priority);

    /** Context menu, shared by all attached fields,
     *  because shown for at most one field at a time.
     */
    private final ContextMenu menu = new ContextMenu();

    /** Toggle menu on Ctrl-Space */
    private final EventHandler<KeyEvent> key_press_filter = event ->
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

    /** On 'ENTER', add the manually entered value to history.
     *  Most other keys trigger a lookup.
     *  Listening to the 'text' of a text field would trigger
     *  the lookup and thus menu whenever the text is set,
     *  but want to only perform lookup when user enters text,
     *  i.e. when keys are pressed.
     */
    private final EventHandler<KeyEvent> key_release_handler = event ->
    {
        if (! (event.getSource() instanceof TextField))
            return;
        final TextField field = (TextField) event.getSource();

        final KeyCode code = event.getCode();
        if (code == KeyCode.ENTER)
        {
            updateHistory(field.getText());
            menu.hide();
        }
        else if (code != KeyCode.ESCAPE   &&
                 ! code.isArrowKey()      &&
                 ! code.isFunctionKey()   &&
                 ! code.isMediaKey()      &&
                 ! code.isModifierKey()   &&
                 ! code.isNavigationKey()
                )
            lookup(field);
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

    /** Font used to highlight section of proposal */
    private final Font header_font, highlight_font;

    /** Create autocomplete menu */
    public AutocompleteMenu(final ProposalService service)
    {
        this.proposal_service = service;

        final Font default_font = Font.getDefault();
        header_font = Font.font(default_font.getFamily(), FontWeight.EXTRA_BOLD, default_font.getSize()+1);
        highlight_font = Font.font(default_font.getFamily(), FontWeight.EXTRA_BOLD, default_font.getSize());

        menu.addEventFilter(KeyEvent.KEY_PRESSED, event ->
        {
            // Toggle on Ctrl-space
            key_press_filter.handle(event);
            // Pressing space in the active TextInputControl
            // would be caught by the menu and activate the first menu item?!
            // --> Filter plain SPACE
            if (! event.isConsumed()  &&  event.getCode() == KeyCode.SPACE)
                event.consume();
        });
    }

    /** @param field Field for which autocompletion is requested */
    public void attachField(final TextInputControl field)
    {
        field.addEventFilter(KeyEvent.KEY_PRESSED, key_press_filter);
        field.addEventHandler(KeyEvent.KEY_RELEASED, key_release_handler);
        field.focusedProperty().addListener(focused_listener);
    }

    /** @param field Field for which autocompletion is no longer desired */
    public void detachField(final TextInputControl field)
    {
        field.focusedProperty().removeListener(focused_listener);
        field.removeEventHandler(KeyEvent.KEY_RELEASED, key_release_handler);
        field.removeEventFilter(KeyEvent.KEY_PRESSED, key_press_filter);
    }

    private void updateHistory(final String text)
    {
        proposal_service.addToHistory(text);
    }

    private void lookup(final TextInputControl field)
    {
        final String text = field.getText();

        synchronized (results)
        {
            results.clear();
        }
        proposal_service.lookup(text, (name, priority, proposals) -> handleLookupResult(field, text, name, priority, proposals));
    }

    private void handleLookupResult(final TextInputControl field, final String text, final String name, final int priority, final List<Proposal> proposals)
    {
        final List<MenuItem> items = new ArrayList<>();

        synchronized (results)
        {
            // Merge proposals
            results.add(new Result(name, priority, proposals));

            // Create menu items: Header for each result,
            // then list proposals
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
        final TextFlow markup = new TextFlow();
        for (MatchSegment seg: proposal.getMatch(text))
        {
            final Label match = new Label(seg.getDescription());
            switch (seg.getType())
            {
            case MATCH:
                match.setTextFill(Color.BLUE);
                match.setFont(highlight_font);
                break;
            case COMMENT:
                match.setTextFill(Color.GRAY);
                match.setFont(highlight_font);
                break;
            case NORMAL:
                default:
            }
            markup.getChildren().add(match);
        }
        final MenuItem item = new MenuItem(null, markup);
        item.setOnAction(event ->
        {
            final String value = proposal.apply(text);
            field.setText(value);
            field.positionCaret(value.length());
            // Menu's key_pressed handler will send ENTER on to current_field
        });

        return item;
    }
}
