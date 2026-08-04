/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.Direction;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.geometry.Point2D;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TabsRepresentation extends JFXBaseRepresentation<TabPane, TabsWidget>
{
    private final DirtyFlag dirty_layout = new DirtyFlag();

    private final AtomicBoolean changing_active_tab = new AtomicBoolean();

    private volatile Font tab_font;

    private final WidgetPropertyListener<String> tab_title_listener = (property, old_value, new_value) ->
    {
        final List<TabItemProperty> model_tabs = model_widget.propTabs().getValue();
        for (int i=0; i<model_tabs.size(); ++i)
            if (model_tabs.get(i).name() == property)
            {
                final Tab tab = jfx_node.getTabs().get(i);
                final Label label = (Label) tab.getGraphic();
                label.setText(property.getValue());
                break;
            }
    };

    private final WidgetPropertyListener<List<Widget>> tab_children_listener = (property, removed, added) ->
    {
        final List<TabItemProperty> model_tabs = model_widget.propTabs().getValue();
        int index;
        for (index = model_tabs.size() - 1;  index >= 0; --index)
            if (model_tabs.get(index).children() == property)
                break;
        if (index < 0)
            throw new IllegalStateException("Cannot locate tab children " + property + " in " + model_widget);

        if (removed != null)
            for (Widget removed_widget : removed)
            {
                toolkit.execute(() -> toolkit.disposeWidget(removed_widget));
            }

        if (added != null)
            addChildren(index, added);
    };

    @Override
    public TabPane createJFXNode() throws Exception
    {
        final TabPane tabs = new TabPane();

        // See 'twiddle' below
        tabs.setStyle("-fx-background-color: lightgray;");
        tabs.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);

        tabs.setMinSize(TabPane.USE_PREF_SIZE, TabPane.USE_PREF_SIZE);
        tabs.setTabMinHeight(model_widget.propTabHeight().getValue());

        // In edit mode, we want to support 'rubberbanding' inside the active tab.
        // This means mouse clicks need to be passed up to the 'model_root'
        // where the rubber band handler can then see them.
        if (toolkit.isEditMode())
            tabs.addEventFilter(MouseEvent.MOUSE_PRESSED, event ->
            {
                // As in 'group' widget, only do this when ALT is pressed,
                // so ALT-rubberband will select inside the tab.
                // Plain click still directly reaches the child widget in the current tab,
                // or - if we hit the background of the tab - the tab widget itself.
                // If we passed any click up to the model root, you could
                // longer click on widgets inside the tab or the tab itself.
                if (event.isAltDown())
                    for (Parent parent = tabs.getParent();  parent != null;  parent = parent.getParent())
                        if (JFXRepresentation.MODEL_ROOT_ID.equals(parent.getId()))
                        {
                            parent.fireEvent(event);
                            break;
                        }
            });

        return tabs;
    }

    private final UntypedWidgetPropertyListener layoutListener = this::layoutChanged;
    private final WidgetPropertyListener<List<TabItemProperty>> tabsListener = this::tabsChanged;
    // Update UI when model selects a tab
    final WidgetPropertyListener<Integer> track_active_model_tab = (p, old, value) ->
    {
        if (! changing_active_tab.compareAndSet(false, true))
            return;
        if (value == null)
            value = p.getValue();
        if (value >= jfx_node.getTabs().size())
            value = jfx_node.getTabs().size() - 1;
        jfx_node.getSelectionModel().select(value);
        changing_active_tab.set(false);
    };

    @Override
    protected void registerListeners()
    {
        super.registerListeners();

        // Create initial tabs and their children
        addTabs(model_widget.propTabs().getValue());

        model_widget.propWidth().addUntypedPropertyListener(layoutListener);
        model_widget.propHeight().addUntypedPropertyListener(layoutListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(layoutListener);
        model_widget.propFont().addUntypedPropertyListener(layoutListener);
        model_widget.propTabs().addPropertyListener(tabsListener);
        model_widget.propDirection().addUntypedPropertyListener(layoutListener);
        model_widget.propTabHeight().addUntypedPropertyListener(layoutListener);

        // Select initial tab
        track_active_model_tab.propertyChanged(model_widget.propActiveTab(), null, null);
        model_widget.propActiveTab().addPropertyListener(track_active_model_tab);

        // Update model when UI selects a tab
        jfx_node.getSelectionModel().selectedIndexProperty().addListener((t, o, selected) ->
        {
            if (! changing_active_tab.compareAndSet(false, true))
                return;
            model_widget.propActiveTab().setValue(selected.intValue());
            changing_active_tab.set(false);
        });

        // Initial update of font, size
        layoutChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        removeTabs(model_widget.propTabs().getValue());

        model_widget.propWidth().removePropertyListener(layoutListener);
        model_widget.propHeight().removePropertyListener(layoutListener);
        model_widget.propBackgroundColor().removePropertyListener(layoutListener);
        model_widget.propFont().removePropertyListener(layoutListener);
        model_widget.propTabs().removePropertyListener(tabsListener);
        model_widget.propDirection().removePropertyListener(layoutListener);
        model_widget.propTabHeight().removePropertyListener(layoutListener);
        model_widget.propActiveTab().addPropertyListener(track_active_model_tab);

        super.unregisterListeners();
    }

    private void tabsChanged(final WidgetProperty<List<TabItemProperty>> property,
            final List<TabItemProperty> removed,
            final List<TabItemProperty> added)
    {
        if (removed != null)
            removeTabs(removed);
        if (added != null)
            addTabs(added);
    }

    private void addTabs(final List<TabItemProperty> added)
    {
        for (TabItemProperty item : added)
        {
            final String name = item.name().getValue();
            final Pane content = new Pane();

            // 'Tab's are added with a Label as 'graphic'
            // because that label allows setting the font.

            // Quirk: Tabs will not show the label unless there's also a non-empty text
            final Tab tab = new Tab(" ", content);
            final Label label = new Label(name);
            tab.setGraphic(label);
            tab.setClosable(false); // !!
            tab.setUserData(item);

            final int index = jfx_node.getTabs().size();
            jfx_node.getTabs().add(tab);

            addChildren(index, item.children().getValue());

            item.name().addPropertyListener(tab_title_listener);
            item.children().addPropertyListener(tab_children_listener);
        }
    }

    private void removeTabs(final List<TabItemProperty> removed)
    {
        for (TabItemProperty item : removed)
        {
            item.children().removePropertyListener(tab_children_listener);
            item.name().removePropertyListener(tab_title_listener);
            for (Tab tab : jfx_node.getTabs())
                if (tab.getUserData() == item)
                {
                    jfx_node.getTabs().remove(tab);
                    break;
                }
        }
    }

    private void addChildren(final int index, final List<Widget> added)
    {
        final Pane parent_item = (Pane) jfx_node.getTabs().get(index).getContent();
        for (Widget added_widget : added)
        {
            final Optional<Widget> parent = added_widget.getParent();
            if (! parent.isPresent())
                throw new IllegalStateException("Cannot locate parent widget for " + added_widget);
            toolkit.execute(() -> toolkit.representWidget(parent_item, added_widget));
        }
    }

    private void layoutChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        tab_font = JFXUtil.convert(model_widget.propFont().getValue());
        dirty_layout.mark();
        toolkit.scheduleUpdate(this);
    }

    /** Compute 'insets'
     *
     *  <p>Determines where the tabs' content Pane
     *  is located relative to the bounds of the TabPane
     */
    private void computeInsets()
    {
        // Called with delay by refreshHack, may be invoked when already disposed
        if (jfx_node == null)
            return;
        // There is always at least one tab. All tabs have the same size.
        final Pane pane = (Pane)jfx_node.getTabs().get(0).getContent();
        final Point2D tabs_bounds = jfx_node.localToScene(0.0, 0.0);
        final Point2D pane_bounds = pane.localToScene(0.0, 0.0);
        final int[] insets = new int[] { (int)(pane_bounds.getX() - tabs_bounds.getX()),
                (int)(pane_bounds.getY() - tabs_bounds.getY()) };
        // logger.log(Level.INFO, "Insets: " + Arrays.toString(insets));
        if (insets[0] < 0  ||  insets[1] < 0)
        {
            logger.log(Level.WARNING, "Inset computation failed: TabPane at " + tabs_bounds + ", content pane at " + pane_bounds);
            insets[0] = insets[1] = 0;
        }
        model_widget.runtimePropInsets().setValue(insets);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();

        if (dirty_layout.checkAndClear())
        {
            final String style = JFXUtil.shadedStyle(model_widget.propBackgroundColor().getValue());

            final Background background = new Background(new BackgroundFill(JFXUtil.convert(model_widget.propBackgroundColor().getValue()), null, null));

            for (Tab tab : jfx_node.getTabs())
            {   // Set the font of the 'graphic' that's used to represent the tab
                final Label label = (Label) tab.getGraphic();
                label.setFont(tab_font);

                // Set colors
                tab.setStyle(style);

                final Pane content  = (Pane) tab.getContent();
                content.setBackground(background);
            }

            final Integer width = model_widget.propWidth().getValue();
            final Integer height = model_widget.propHeight().getValue();
            jfx_node.setPrefSize(width, height);
            jfx_node.setTabMinHeight(model_widget.propTabHeight().getValue());

            refreshHack();
        }
    }

    /** Force TabPane refresh */
    private void refreshHack()
    {
        // Imperfect; works most of the time.
        // See org.csstudio.display.builder.representation.javafx.sandbox.TabDemo
        // for the setSide hack.
        // In addition, if TabPane is the _only_ widget, it will not show
        // unless the background style is initially set to something.
        // OK to then clear the style later, i.e. in here.
        final Runnable twiddle = () ->
        {
            // Called with delay, may happen after disposal
            if (jfx_node == null)
                return;
            jfx_node.setStyle("");
            jfx_node.setSide(Side.BOTTOM);
            if (model_widget.propDirection().getValue() == Direction.HORIZONTAL)
                jfx_node.setSide(Side.TOP);
            else
                jfx_node.setSide(Side.LEFT);

            // Insets computation only possible once TabPane is properly displayed.
            // Until then, content Pane will report position as 0,0.
            toolkit.schedule(this::computeInsets, 500, TimeUnit.MILLISECONDS);
        };
        toolkit.schedule(twiddle, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void dispose()
    {
        for (TabItemProperty tab : model_widget.propTabs().getValue())
            for (Widget child : tab.children().getValue())
                toolkit.execute(() -> toolkit.disposeWidget(child));

        jfx_node.getTabs().clear();
        super.dispose();
    }
}
