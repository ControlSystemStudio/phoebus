/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.util;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.RecursiveTask;

import org.csstudio.display.builder.editor.WidgetSelectionHandler;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.ArrayWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.VisibleWidget;

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.shape.Rectangle;

/** Helper for locating 'parent' of widgets
 *
 *  <p>Used to locate the parent of one or more widgets in the model,
 *  to highlight GroupWidget, TabWidget, or ArrayWidget on move-over,
 *  to move widgets in and out of a group or tab.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ParentHandler
{
    public static final int PARALLEL_THRESHOLD = 10;

    private volatile DisplayModel model = null;

    private final WidgetSelectionHandler selection;

    private final Rectangle parent_highlight = new Rectangle();

    /** The 'children' property of the parent that holds the selected widgets */
    private volatile ChildrenProperty active_parent_children = null;

    /** Parent that was found to surround a region on the screen
     *
     *  <p>Instead of holding the widget, it tracks the
     *  'children' property of the widget.
     *  For a GroupWidget or ArrayWidget, that's its single 'children' property.
     *  For a Tabwidget, that's the 'children' property of the
     *  _selected_ tab.
     *  In either case it's the 'children' property where
     *  the selected widgets would be added in a 'drop'.
     */
    private static class ParentSearchResult
    {
        /** 'children' property or null */
        volatile ChildrenProperty children = null;

        /** Depth in display model hierarchy where parent resides */
        volatile int depth = 0;

        /** Update if parent is 'deeper' */
        void update(final ChildrenProperty children, final int depth)
        {
            if (children != null  &&  depth >= this.depth)
                this.children = children;
        }

        /** Update if other result has 'deeper' group */
        void update(final ParentSearchResult other)
        {
            update(other.children, other.depth);
        }
    }

    /** Brute-force, parallel search for a Parent widget that surrounds a given region of the screen. */
    private class ParentWidgetSearch extends RecursiveTask<ParentSearchResult>
    {
        private static final long serialVersionUID = -5074784016726334794L;
        private final Rectangle2D bounds;
        private final List<Widget> widgets, ignore;
        private final int depth;

        /** Create search for a parent
         *  @param bounds Region of screen
         *  @param model Display model
         *  @param ignore Widgets to ignore in the search
         */
        ParentWidgetSearch(final Rectangle2D bounds, final DisplayModel model, List<Widget> ignore)
        {
            this(bounds, model.getChildren(), ignore, 1);
        }

        private ParentWidgetSearch(final Rectangle2D bounds, final List<Widget> widgets, final List<Widget> ignore, final int depth)
        {
            this.bounds = bounds;
            this.widgets = widgets;
            this.ignore = ignore;
            this.depth = depth;
        }

        @Override
        protected ParentSearchResult compute()
        {
            final int N = widgets.size();
            if (N > PARALLEL_THRESHOLD)
            {
                // System.out.println("Splitting the search");
                final int split = N / 2;
                final ParentWidgetSearch sub = new ParentWidgetSearch(bounds, widgets.subList(0, split), ignore, depth);
                sub.fork();
                final ParentSearchResult result = findParent(widgets.subList(split,  N));
                result.update(sub.join());
                return result;
            }
            else
                return findParent(widgets);
        }

        private ParentSearchResult findParent(final List<Widget> children)
        {
            // System.out.println("Searching for surrounding parent in " + children.size() + " widgets on " + Thread.currentThread().getName());
            final ParentSearchResult result = new ParentSearchResult();
            for (Widget widget : children)
            {
                if (ignore.contains(widget))
                    continue;
                final ChildrenProperty child_prop;
                if (widget instanceof GroupWidget)
                    child_prop = ((GroupWidget) widget).runtimeChildren();
                else if (widget instanceof TabsWidget)
                {   // Check children of _selected_ Tab
                    final TabsWidget tabwid = (TabsWidget) widget;
                    final int selected = tabwid.propActiveTab().getValue();
                    child_prop = tabwid.propTabs().getValue().get(selected).children();
                }
                else if (widget instanceof ArrayWidget)
                {
                    List<Widget> widgets = ((ArrayWidget) widget).runtimeChildren().getValue();
                    if (widgets.isEmpty() || (!ignore.isEmpty() && widgets.get(0).getType().equals(ignore.get(0).getType())))
                        child_prop = ((ArrayWidget) widget).runtimeChildren();
                    else
                        continue;
                }
                else
                {
                    final Optional<WidgetProperty<List<Widget>>> widget_children = widget.checkProperty(ChildrenProperty.DESCRIPTOR);
                    if (widget_children.isPresent())
                        child_prop = (ChildrenProperty)widget_children.get();
                    else
                        continue;
                }
                // block accepting drop into parent if parent is not visible - skip invisible widgets
                // widget in edit pane is always a VisibleWidget
                boolean isVisible = true;
                Widget w = widget;
                while (w instanceof VisibleWidget)
                    if (!((VisibleWidget)w).propVisible().getValue())
                    {
                        isVisible = false;
                        break;
                    }
                    else
                        w = w.getParent().get();
                if (checkIfWidgetWithinBounds(widget) && isVisible)
                    result.update(child_prop, depth);
                result.update(new ParentWidgetSearch(bounds, child_prop.getValue(), ignore, depth + 1).compute());
            }
            return result;
        }

        private boolean checkIfWidgetWithinBounds(final Widget widget)
        {
            final Rectangle2D widget_bounds = GeometryTools.getDisplayBounds(widget);
            return widget_bounds.contains(bounds);
        }
    };

    /** Construct parent handler
     *  @param parent Parent for rectangle that highlights active group
     *  @param selection Current selection
     */
    public ParentHandler(final Group parent, final WidgetSelectionHandler selection)
    {
        this.selection = selection;
        parent_highlight.getStyleClass().add("parent_highlight");
        parent_highlight.setMouseTransparent(true);
        parent.getChildren().add(0, parent_highlight);
    }

    /** @param model Model in which to search for parent */
    public void setModel(final DisplayModel model)
    {
        this.model = model;
        active_parent_children = null;
        hide();
    }

    /** Locate parent for region of display.
     *
     *  <p>If there is a group or tab that contains the region, it is highlighted.
     *
     *  <p>The widgets in the current selection themselves are ignored
     *  in the search to prevent having a group that's moved locate itself.
     *
     *  @param x
     *  @param y
     *  @param width
     *  @param height
     *  @see #getActiveParentChildren()
     */
    public void locateParent(final double x, final double y, final double width, final double height)
    {
        final Rectangle2D bounds = new Rectangle2D(x, y, width, height);
        final List<Widget> selected_widgets = selection.getSelection();
        final ParentSearchResult res = new ParentWidgetSearch(bounds, model, selected_widgets).compute();
        final ChildrenProperty parent = res.children;
        if (parent == null)
            hide();
        else
        {
            final Rectangle2D group_bounds = GeometryTools.getDisplayBounds(parent.getWidget());
            parent_highlight.setX(group_bounds.getMinX());
            parent_highlight.setY(group_bounds.getMinY());
            parent_highlight.setWidth(group_bounds.getWidth());
            parent_highlight.setHeight(group_bounds.getHeight());
            parent_highlight.setVisible(true);
        }
        active_parent_children = parent;
    }

    /** @return Active Widget's 'children', may be <code>null</code> */
    public ChildrenProperty getActiveParentChildren()
    {
        return active_parent_children;
    }

    /** Hide the group highlight (in case it's visible) */
    public void hide()
    {
        parent_highlight.setVisible(false);
    }
}
