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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
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
     *
     *  <p>User data is set to the field that 'show()'s the menu.
     */
    private final ContextMenu menu = new ContextMenu();

    private boolean selected_menu_item = false;


    /** Toggle menu. On 'ENTER', add the manually entered value to history */
    private final EventHandler<KeyEvent> key_pressed_filter = event ->
    {
        // System.out.println("Text field Key press " + event.getCode() + " on " + event.getSource());
        toggleMenu(event);
        if (event.isConsumed())
            return;
        final TextField field = (TextField) event.getSource();
        final KeyCode code = event.getCode();
        if (code == KeyCode.ENTER)
        {
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
        // System.out.println("Text field Key release " + event.getCode() + " on " + event.getSource());
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
            // System.out.println("Menu Key press " + event.getCode() + " on " + event.getSource());
            if (event.getCode() == KeyCode.ENTER)
            {   // The menu will see 'ENTER' keys.
                selected_menu_item = false;
                // Let menu react, then check later
                Platform.runLater(() ->
                {
                    // If that key invokes a menu item -> Done.
                    // Sometimes, however, when no menu item is active,
                    // the ENTER key goes nowhere.
                    // In that case, simulate 'enter' on the text field.
                    if (selected_menu_item == false)
                        invokeAction((TextInputControl) menu.getUserData());
                });
            }
            else
            {
                toggleMenu(event);
                // Pressing space in the active TextInputControl
                // would be caught by the menu and activate the first menu item?!
                // --> Filter plain SPACE
                if (! event.isConsumed()  &&  event.getCode() == KeyCode.SPACE)
                    event.consume();
            }
        });

        menu.setOnHidden(event ->
        {
            // Speed GC by releasing items, since menu stays in memory forever
            menu.getItems().clear();
            menu.setUserData(null);
        });
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
        menu.setUserData(field);

        // Same functionality as
        //    menu.show(field, Side.BOTTOM, 0, 0);
        // but preventing the menu from keeping an `ownerNode` reference
        // to the field, which in turn keeps a reference to the UI
        // and thus data of the attached field.
        final Bounds bounds = field.localToScreen(field.getLayoutBounds());
        menu.show(field.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY());
        // Alas, the menu.focused property can still hold on for a while ..
        // So best is for all UI to release application data when closed,
        // since complete disposal of the UI can take some time.
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

        // Assume the user is typing in the text field.
        // When the menu is shown, the first item will _sometimes_ be auto-selected.
        // When user presses ENTER, the text that the user just entered in the text field
        // is replaced by the value of the first menu item, because it had been unwittingly invoked.
        // By starting the menu with a bogus no-op item, that problem is averted.
        // By making the text of that item match what the user entered,
        // this almost appears to be on-purpose.
        final MenuItem bogus = new MenuItem(null, new Text(text));
        items.add(bogus);

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
                showMenuForField(field);
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
            selected_menu_item = true;
            final String value = proposal.apply(text);
            field.setText(value);
            field.positionCaret(value.length());
            invokeAction(field);
        });

        return item;
    }

    /** Try to invoke 'onAction' handler
     *  @param field Potential {@link TextField}
     */
    private void invokeAction(final TextInputControl field)
    {
        if (field != null)
            proposal_service.addToHistory(field.getText());
        if (field instanceof TextField)
        {
            final EventHandler<ActionEvent> action = ((TextField)field).getOnAction();
            if (action != null)
                action.handle(new ActionEvent(field, null));
        }
    }
}
