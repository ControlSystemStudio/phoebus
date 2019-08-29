/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.util;

import static org.csstudio.display.builder.editor.Plugin.logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.logging.Level;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.InsetsWidgetProperty;
import org.csstudio.display.builder.model.widgets.TabsWidget;
import org.csstudio.display.builder.model.widgets.TabsWidget.TabItemProperty;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;

/** Helpers for handling geometry
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class GeometryTools
{
    /** Get offset of widget inside the display.
     *
     *  <p>Widgets that are inside a container
     *  are positioned relative to the container.
     *
     *  @param widget Model widget, must already be in the model, i.e. have a parent
     *  @return {@link Point2D} Offset of the widget relative to the display model
     */
    public static Point2D getDisplayOffset(final Widget widget)
    {
        return getContainerOffset(widget.getParent().orElse(null));
    }

    /** Get offset of widgets inside a container from display root
     *
     *  <p>Widgets that are inside a container
     *  are positioned relative to the container.
     *
     *  @param container Container, i.e. GroupWidget, TabWidget or DisplayModel root
     *  @return {@link Point2D} Offset of the widgets inside that container
     */
    public static Point2D getContainerOffset(Widget container)
    {
        int dx = 0, dy = 0;

        while (container != null)
        {   // Ignore the x/y location of the display model,
            // it's used as origin coordinate for standalone window,
            // not as offset within a display
            if (! (container instanceof DisplayModel))
            {
                dx += container.propX().getValue();
                dy += container.propY().getValue();
            }
            final int[] insets = InsetsWidgetProperty.getInsets(container);
            if (insets != null)
            {
                dx += insets[0];
                dy += insets[1];
            }
            container = container.getParent().orElse(null);
        }

        return new Point2D(dx, dy);
    }

    /** Get bounds of widget, relative to container
     *  @param widget Model widget
     *  @return {@link Rectangle2D}
     */
    public static Rectangle2D getBounds(final Widget widget)
    {
        return new Rectangle2D(widget.propX().getValue(),
                               widget.propY().getValue(),
                               widget.propWidth().getValue(),
                               widget.propHeight().getValue());
    }

    /** Get bounds of widgets relative to container
     *  @param widgets Model widgets
     *  @return {@link Rectangle2D}
     */
    public static Rectangle2D getBounds(final Collection<Widget> widgets)
    {
        return widgets.stream()
                      .map(GeometryTools::getBounds)
                      .reduce(null, GeometryTools::join);
    }

    /** Get bounds of widget relative to display model
     *  @param widget Model widget
     *  @return {@link Rectangle2D}
     */
    public static Rectangle2D getDisplayBounds(final Widget widget)
    {
        final Point2D offset = getDisplayOffset(widget);
        try
        {
            return new Rectangle2D(offset.getX() + widget.propX().getValue(),
                                   offset.getY() + widget.propY().getValue(),
                                   widget.propWidth().getValue(),
                                   widget.propHeight().getValue());
        }
        catch (IllegalArgumentException ex)
        {
            logger.log(Level.WARNING, "Widget has invalid size " + widget, ex);
            return new Rectangle2D(offset.getX() + widget.propX().getValue(),
                                   offset.getY() + widget.propY().getValue(),
                                   1, 1);
        }
    }

    /** Get bounds of widgets relative to display model
     *  @param widgets Model widgets
     *  @return {@link Rectangle2D}
     */
    public static Rectangle2D getDisplayBounds(final Collection<Widget> widgets)
    {
        return widgets.stream()
                      .map(GeometryTools::getDisplayBounds)
                      .reduce(null, GeometryTools::join);
    }

    /** Compute bounding rectangle
     *  @param one One rect, may be <code>null</code>
     *  @param other Other rect, may be <code>null</code>
     *  @return Bounding rectangle of one and other.
     *          <code>null</code> if both inputs are <code>null</code>.
     */
    public static Rectangle2D join(final Rectangle2D one, final Rectangle2D other)
    {
        if (one == null)
            return other;
        if (other == null)
            return one;
        final double x = Math.min(one.getMinX(), other.getMinX());
        final double y = Math.min(one.getMinY(), other.getMinY());
        final double x2 = Math.max(one.getMaxX(), other.getMaxX());
        final double y2 = Math.max(one.getMaxY(), other.getMaxY());
        return new Rectangle2D(x, y, x2-x, y2-y);
    }

    /** Parallel search for widgets within a region */
    private static class WidgetSearch extends RecursiveTask<List<Widget>>
    {
        private static final long serialVersionUID = 1L;
        private static final int THRESHOLD = 50; // XXX Find suitable threshold
        private final List<Widget> widgets;
        private final Rectangle2D region;

        public WidgetSearch(final List<Widget> widgets, final Rectangle2D region)
        {
            this.widgets = widgets;
            this.region = region;
        }

        @Override
        protected List<Widget> compute()
        {
            if (widgets.size() > THRESHOLD)
            {   // Spawn sub-task for first half
                final int half = widgets.size() / 2;
                final WidgetSearch sub = new WidgetSearch(widgets.subList(0, half), region);
                sub.fork();
                // Search second half of list in this thread
                final List<Widget> found = searchWidgetList(widgets.subList(half, widgets.size()));
                // Return combined result
                found.addAll(sub.join());
                return found;
            }
            else // Search complete list in this thread
                return searchWidgetList(widgets);
        }

        private List<Widget> searchWidgetList(final List<Widget> widgets)
        {
            // System.out.println("Searching in " + Thread.currentThread().getName() + ": " + widgets);
            final List<Widget> found = new ArrayList<>();
            for (final Widget widget : widgets)
            {
                // Select widget itself _or_ its children,
                // not both to prevent selecting e.g. a Group
                // and its content.
                if (region.contains(getDisplayBounds(widget)))
                    found.add(widget);
                else
                {
                    final ChildrenProperty children = ChildrenProperty.getChildren(widget);
                    if (children != null)
                    {
                        // Search 'children' of group
                        final WidgetSearch sub = new WidgetSearch(children.getValue(), region);
                        found.addAll(sub.compute());
                    }
                    else if (widget instanceof TabsWidget)
                    {
                        // Search visible tab
                        final TabsWidget tabs_widget = (TabsWidget) widget;
                        final List<TabItemProperty> tabs = tabs_widget.propTabs().getValue();
                        final int active = tabs_widget.propActiveTab().getValue();
                        if (active >= 0  &&  active < tabs.size())
                        {
                            final WidgetSearch sub = new WidgetSearch(tabs.get(active).children().getValue(), region);
                            found.addAll(sub.compute());
                        }
                    }
                }
            }
            return found;
        }
    }

    /** Find widgets inside a region
     *  @param model Model to search
     *  @param region Region in which to locate widgets
     *  @return Widgets within the region
     */
    public static List<Widget> findWidgets(final DisplayModel model,
                                           final Rectangle2D region)
    {
        final WidgetSearch search = new WidgetSearch(model.getChildren(), region);
        return search.compute();
    }

    /** Move widgets to a new location
     *  @param x Desired location of upper-left widget
     *  @param y
     *  @param widgets Widgets to move
     */
    public static void moveWidgets(final int x, final int y, final List<Widget> widgets)
    {
        // Find upper left corner of dropped widgets
        int min_x = Integer.MAX_VALUE, min_y = Integer.MAX_VALUE;
        for (Widget widget : widgets)
        {
            min_x = Math.min(widget.propX().getValue(), min_x);
            min_y = Math.min(widget.propY().getValue(), min_y);
        }
        // Move upper left corner to desired location
        final int dx = x - Math.max(0, min_x);
        final int dy = y - Math.max(0, min_y);
        for (Widget widget : widgets)
        {
            widget.propX().setValue(widget.propX().getValue() + dx);
            widget.propY().setValue(widget.propY().getValue() + dy);
        }
    }
}
