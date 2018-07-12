/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.csstudio.display.builder.model.properties.Direction;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/** Navigation Tabs
 *
 *  <p>Similar to a tab pane in look,
 *  but only has one 'body' for content.
 *
 *  <p>Selecting one of the tabs invokes listener,
 *  whi can then update the content.
 *  In comparison, a tab pane results in a scene graph
 *  where the content of all tabs is always present.
 *
 *  <p>Tabs with empty text are invisible, creating a gap
 *  in the lineup of tabs.
 *
 *  <p>While the {@link NavigationTabs} are a {@link BorderPane},
 *  that is an implementation detail which might change.
 *  Code that uses the {@link NavigationTabs} should only
 *  access the public methods of the class itself
 *  and otherwise treat it as a {@link Node}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NavigationTabs extends BorderPane
{
    /** Listener to {@link NavigationTabs} */
    @FunctionalInterface
    public static interface Listener
    {
        /** User selected a tab
         *  @param tab_index Index 0, .. of the selected tab.
         */
        void tabSelected(int tab_index);
    }

    /** CSS pseudoclass to select HORIZONTAL resp. vertical type of tab buttons */
    private final static PseudoClass HORIZONTAL = PseudoClass.getPseudoClass("horizontal");

    /** HBox or VBox for tab buttons */
    private Pane buttons = new VBox();

    /** 'content', body */
    private final Pane body = new Pane();

    /** Labels for the tabs */
    private final List<String> tabs = new CopyOnWriteArrayList<>();

    private int tab_width = 100, tab_height = 50, tab_spacing = 2;

    /** Direction of tabs */
    private Direction direction = Direction.VERTICAL;

    /** Color of selected vs. de-selected tab button */
    private Color selected = Color.rgb(236, 236, 236),
                  deselected = Color.rgb(200, 200, 200);

    private Font font = null;

    /** Listener to selected tab
     *
     *  <p>At this time only supporting one
     */
    private volatile Listener listener;


    /** Constructor */
    public NavigationTabs()
    {
        // Scroll pane in case body exceeds size of this, the other BorderPane
        final ScrollPane scroll = new ScrollPane(body);
        scroll.getStyleClass().add("navtab_scroll");

        // Inner border pane to auto-resize 'body' and add border + padding via style sheet
        final BorderPane border_wrapper = new BorderPane(scroll);
        border_wrapper.getStyleClass().add("navtab_body");

        setCenter(border_wrapper);
    }

    /** @param listener Listener to notify when tab is selected */
    public void addListener(final Listener listener)
    {
        if (this.listener != null)
            throw new IllegalStateException("Only one listener supported");
        this.listener = listener;
    }

    /** @param listener Listener to remove */
    public void removeListener(final Listener listener)
    {
        if (this.listener != listener)
            throw new IllegalStateException("Unknown listener");
        this.listener = null;
    }

    /** @param tabs Tab labels */
    public void setTabs(final List<String> tabs)
    {
        this.tabs.clear();
        this.tabs.addAll(tabs);
        updateTabs();
    }

    /** @return Index of the selected tab. -1 if there are no buttons or nothing selected */
    public int getSelectedTab()
    {
        final ObservableList<Node> siblings = buttons.getChildren();
        for (int i=0; i<siblings.size(); ++i)
            if (((ToggleButton) siblings.get(i)).isSelected())
                return i;
        return -1;
    }

    /** Select a tab
     *
     *  <p>Does not invoke listener.
     *
     *  @param index Index of tab to select */
    public void selectTab(int index)
    {
        final ObservableList<Node> siblings = buttons.getChildren();
        if (index < 0)
            index = 0;
        if (index >= siblings.size())
            index = siblings.size() - 1;
        if (index < 0)
            return; // No buttons, index is -1
        handleTabSelection((ToggleButton)siblings.get(index), false);
    }

    /** @return Pane for the 'body' */
    public Pane getBodyPane()
    {
        return body;
    }

    /** @return Direction of tabs, horizontal (on top) or vertical (on left) */
    public Direction getDirection()
    {
        return direction;
    }

    /** @param direction Direction of tabs, horizontal (on top) or vertical (on left) */
    public void setDirection(final Direction direction)
    {
        if (this.direction == direction)
            return;
        final int active = getSelectedTab();
        this.direction = direction;
        updateTabs();
        if (active >= 0)
            selectTab(active);
    }

    /** @param width Width and ..
     *  @param height height of tabs
     */
    public void setTabSize(final int width, final int height)
    {
        if (tab_width == width  &&  tab_height == height)
            return;
        tab_width = width;
        tab_height = height;
        updateTabs();
    }

    /** @param spacing Spacing between tabs */
    public void setTabSpacing(final int spacing)
    {
        if (tab_spacing == spacing)
            return;
        tab_spacing = spacing;
        updateTabs();
    }

    /** @param color Color for selected tab */
    public void setSelectedColor(final Color color)
    {
        if (selected.equals(color))
            return;
        selected = color;
        updateTabs();
    }

    /** @param color Color for de-selected tabs */
    public void setDeselectedColor(final Color color)
    {
        if (deselected.equals(color))
            return;
        deselected = color;
        updateTabs();
    }

    public void setFont(final Font font)
    {
        if (Objects.equals(this.font, font))
            return;
        this.font = font;
        updateTabs();
    }

    /** Re-create all tab buttons */
    private void updateTabs()
    {
        if (direction == Direction.VERTICAL)
        {
            setTop(null);
            final VBox box = new VBox(tab_spacing);
            setLeft(box);
            buttons = box;
        }
        else
        {
            setLeft(null);
            final HBox box = new HBox(tab_spacing);
            setTop(box);
            buttons = box;
        }

        buttons.getStyleClass().add("navtab_tabregion");

        // Create button for each tab
        for (int i=0; i<tabs.size(); ++i)
        {
            final ToggleButton button = new ToggleButton(tabs.get(i));
            // Buttons without text vanish, creating a gap in the tab lineup.
            if (button.getText().isEmpty())
                button.setVisible(false);
            if (direction == Direction.HORIZONTAL)
                button.pseudoClassStateChanged(HORIZONTAL, true);

            // base color, '-fx-color', is either selected or deselected
            button.setStyle("-fx-color: " + JFXUtil.webRGB(deselected));
            button.getStyleClass().add("navtab_button");
            button.setMinSize(ButtonBase.USE_PREF_SIZE, ButtonBase.USE_PREF_SIZE);
            button.setPrefSize(tab_width, tab_height);
            if (font != null)
                button.setFont(font);
            buttons.getChildren().add(button);
            button.setOnAction(e -> handleTabSelection(button, true));
        }
    }

    /** Indicate the active tab, notify listeners
     *  @param pressed Button that was pressed
     */
    private void handleTabSelection(final ToggleButton pressed, final boolean notify)
    {
        final ObservableList<Node> siblings = buttons.getChildren();
        int i = 0, selected_tab = -1;
        for (Node sibling : siblings)
        {
            final ToggleButton button = (ToggleButton) sibling;
            if (button == pressed)
            {
                // If user clicked a button that was already selected,
                // it would now be de-selected, leaving nothing selected.
                if (! pressed.isSelected())
                {   // Re-select!
                    pressed.setSelected(true);
                }
                // Highlight active tab by setting it to the 'selected' color
                pressed.setStyle("-fx-color: " + JFXUtil.webRGB(selected));
                selected_tab = i;
            }
            else if (button.isSelected())
            {
                // Radio-button behavior: De-select other tabs
                button.setSelected(false);
                button.setStyle("-fx-color: " + JFXUtil.webRGB(deselected));
            }
            ++i;
        }

        final Listener safe_copy = listener;
        if (selected_tab >= 0  &&  notify  &&  safe_copy != null)
            safe_copy.tabSelected(selected_tab);
    }
}