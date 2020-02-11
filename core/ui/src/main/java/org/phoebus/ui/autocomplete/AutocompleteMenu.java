/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.autocomplete;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.framework.autocomplete.MatchSegment;
import org.phoebus.framework.autocomplete.Proposal;
import org.phoebus.framework.autocomplete.ProposalService;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
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
@SuppressWarnings("nls")
public class AutocompleteMenu
{
    // First implementation used a ContextMenu.
    // The menu, however, tends to automatically select the item under the mouse.
    // When simply typing text and pressing 'enter',
    // that would accidentally pick the unexpectedly selected menu item
    // instead of simply confirming what's entered.
    // A popup window with list allows more control.
    public static final Logger logger = Logger.getLogger(AutocompleteMenu.class.getPackageName());

    /** Font used to highlight section of proposal */
    private final Font header_font, highlight_font;

    private final ProposalService proposal_service;

    /** Result of a lookup, packaged with header for display in list */
    private class Result
    {
        final Label header;
        final int priority;
        final List<Proposal> proposals;

        Result(final String name, final int priority, final List<Proposal> proposals)
        {
            header = new Label(name);
            header.setFont(header_font);

            this.priority = priority;
            this.proposals = proposals;
        }
    }

    /** All results. SYNC on access */
    private final TreeSet<Result> results = new TreeSet<>((a, b) -> a.priority - b.priority);

    /** Popup, shared by all attached fields,
     *  because shown for at most one field at a time.
     */
    private final AutocompletePopup menu = new AutocompletePopup();

    /** Toggle menu. On 'ENTER', add the manually entered value to history */
    private final EventHandler<KeyEvent> key_pressed_filter = event ->
    {
        logger.log(Level.FINE, () -> "Text field Key press " + event.getCode() + " on " + event.getSource());
        toggleMenu(event);
        if (event.isConsumed())
            return;
        final TextField field = (TextField) event.getSource();
        final KeyCode code = event.getCode();
        if (code == KeyCode.ENTER)
        {
            logger.log(Level.FINE, () -> "Add to history: " + field.getText());
            updateHistory(field.getText());
            menu.hide();
        }
    };

    /** Trigger lookup for most keys
     *
     *  .. _after_ they're released, and the field has the updated text.
     *
     *  Listening to the 'text' of a text field would trigger
     *  the lookup and thus menu whenever the text is set,
     *  but want to only perform lookup when user enters text,
     *  i.e. when keys are pressed/released.
     */
    private final EventHandler<KeyEvent> key_released_filter = event ->
    {
        logger.log(Level.FINE, () -> "Text field Key release " + event.getCode() + " on " + event.getSource());
        final TextField field = (TextField) event.getSource();
        final KeyCode code = event.getCode();
        if (code != KeyCode.SPACE    &&
            code != KeyCode.ENTER    &&
            code != KeyCode.ESCAPE   &&
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
            synchronized (results)
            {
                results.clear();
            }
        }
    };

    /** Create autocomplete menu */
    public AutocompleteMenu(final ProposalService service)
    {
        this.proposal_service = service;

        final Font default_font = Font.getDefault();
        header_font = Font.font(default_font.getFamily(), FontWeight.BOLD, default_font.getSize());
        highlight_font = Font.font(default_font.getFamily(), FontWeight.BOLD, default_font.getSize());
    }

    /** Attach the completion menu to a text field
     *
     *  @param field Field for which completion is requested
     */
    public void attachField(final TextInputControl field)
    {
        field.addEventFilter(KeyEvent.KEY_PRESSED, key_pressed_filter);
        field.addEventFilter(KeyEvent.KEY_RELEASED, key_released_filter);
        field.focusedProperty().addListener(focused_listener);
        XPasteBuffer.addMiddleClickPaste(field);
    }

    /** Detach a previously attached field from the completion menu.
     *
     *  <p>In the rare case where the completion is no longer desired,
     *  while the text field is still in the UI,
     *  call this method.
     *
     *  <p>It is <b>not</b> necessary to 'detach' whenever the text field vanishes,
     *  for example when a panel is closed.
     *  In that case, the vanished text field will simply no longer send events
     *  that trigger the menu.
     *
     *  @param field Field for which completion is no longer desired
     */
    public void detachField(final TextInputControl field)
    {
        field.focusedProperty().removeListener(focused_listener);
        field.removeEventFilter(KeyEvent.KEY_RELEASED, key_released_filter);
        field.removeEventFilter(KeyEvent.KEY_PRESSED, key_pressed_filter);
        XPasteBuffer.removeMiddleClickPaste(field);
    }

    /** Toggle menu on Ctrl-Space
     *
     *  <p>Called by both the text field and the context menu
     */
    private void toggleMenu(final KeyEvent event)
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
                final TextInputControl field = (TextInputControl) event.getSource();
                // Show menu with current content,
                // in case we were hiding and are now showing the menu
                // for the same field, not loosing focus,
                // menu already populated
                showMenuForField(field);
                // Certainly need to perform lookup if menu is empty.
                // Otherwise, cannot hurt to 'refresh'
                lookup(field);
            }
            event.consume();
        }
    }

    private void showMenuForField(final TextInputControl field)
    {
        menu.show(field);
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

    /** Latest list of menu items to show.
     *  Used to schedule only one 'RunLater'
     */
    private final AtomicReference<List<AutocompleteItem>> menu_items = new AtomicReference<>();

    private void handleLookupResult(final TextInputControl field, final String text, final String name, final int priority, final List<Proposal> proposals)
    {
        final List<AutocompleteItem> items = new ArrayList<>();

        synchronized (results)
        {
            // Merge proposals
            results.add(new Result(name, priority, proposals));

            // Create menu items: Header for each result,
            // then list proposals
            for (Result result : results)
            {
                // Pressing 'Enter' on header simply forwards the enter to the text field
                items.add(new AutocompleteItem(result.header, () -> invokeAction(field)));
                for (Proposal proposal : result.proposals)
                    items.add(createItem(field, text, proposal));
            }
        }

        // Update and show menu on UI thread
        if (menu_items.getAndSet(items) == null)
            Platform.runLater(() ->
            {
                final List<AutocompleteItem> current_items = menu_items.getAndSet(null);
                menu.setItems(current_items);
                if (! menu.isShowing())
                    showMenuForField(field);
            });
        // else: already pending, will use the updated 'menu_items'
    }

    /** @param field Field where user entered text
     *  @param text Text entered by user
     *  @param proposal Matching proposal
     *  @return AutocompleteItem that will apply the proposal
     */
    private AutocompleteItem createItem(final TextInputControl field,
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

        return new AutocompleteItem(markup, () ->
        {
            final String value = proposal.apply(text);
            field.setText(value);
            field.positionCaret(value.length());
            invokeAction(field);
        });
    }

    /** Try to invoke 'onAction' handler
     *  @param field Potential {@link TextField}
     */
    private void invokeAction(final TextInputControl field)
    {
        if (field instanceof TextField)
        {
            logger.log(Level.FINE, () -> "InvokeAction: Selected " + field.getText());
            proposal_service.addToHistory(field.getText());
            final EventHandler<ActionEvent> action = ((TextField)field).getOnAction();
            if (action != null)
                action.handle(new ActionEvent(field, null));
        }
        menu.hide();
    }
}
