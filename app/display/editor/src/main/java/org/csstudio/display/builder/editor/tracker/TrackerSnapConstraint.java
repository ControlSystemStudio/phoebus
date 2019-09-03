/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.tracker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.RecursiveTask;

import org.csstudio.display.builder.editor.util.GeometryTools;
import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.DisplayModel;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.representation.javafx.JFXRepresentation;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.shape.Line;

/** Constraint on the movement of the Tracker that snaps to other widgets
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TrackerSnapConstraint extends TrackerConstraint
{
    // Brute-force implementation that searches all widgets
    // for the closest match.
    //
    // Idea by Tom Pelaia:
    // Create lists of widgets sorted by coordinates.
    // Probably need one for X0, Y0, X1, Y1.
    // Then perform binary search in those lists for closest widget.
    // When moving tracker, start next search at last index in list to further minimize
    // the 'binary' search.

    /** If number of widgets to check exceeds this threshold,
     *  the search is parallelized into 2 sub-tasks.
     *
     *  When too small, creating of threads defeats the parallel gain.
     *
     *  XXX Preference setting for PARALLEL_THRESHOLD or find optimum value
     */
    public static final int PARALLEL_THRESHOLD = 50;

    private final JFXRepresentation toolkit;

    private final double snap_distance = 10;

    private DisplayModel model = null;
    private List<Widget>selected_widgets = Collections.emptyList();

    private final Line horiz_guide, vert_guide;



    /** Horizontal and/or vertical position to which we 'snapped' */
    private static class SnapResult
    {
        final static double INVALID = Double.NEGATIVE_INFINITY;

        /** Horizontal coordinate to which to 'snap', or INVALID */
        double horiz = INVALID;

        /** Distance at which the horizontal snap was found */
        double horiz_distance = Double.MAX_VALUE;

        /** Vertical coordinate to which to 'snap', or INVALID */
        double vert = INVALID;

        /** Distance at which the vertical snap was found */
        double vert_distance = Double.MAX_VALUE;

        /** @param other Other result from which to use horiz or vert if they're closer */
        public void updateFrom(final SnapResult other)
        {
            if (other.horiz_distance < horiz_distance)
            {
                horiz = other.horiz;
                horiz_distance = other.horiz_distance;
            }
            if (other.vert_distance < vert_distance)
            {
                vert = other.vert;
                vert_distance = other.vert_distance;
            }
        }
    }

    /** Parallel search for snap points in list of widgets */
    private class SnapSearch extends RecursiveTask<SnapResult>
    {
        private static final long serialVersionUID = 7120422764377430462L;
        private final List<Widget> widgets;
        private final double x;
        private final double y;

        /** @param widgets Widgets to search
         *  @param x Requested X position
         *  @param y Requested Y position
         */
        SnapSearch(final List<Widget> widgets, final double x, final double y)
        {
            this.widgets = widgets;
            this.x = x;
            this.y = y;
        }

        @Override
        protected SnapResult compute()
        {
            return checkWidgets(widgets);
        }

        private SnapResult checkWidgets(final List<Widget> widgets)
        {
            final SnapResult result;
            final int N = widgets.size();
            if (N > PARALLEL_THRESHOLD)
            {
                // System.out.println("Splitting the search");
                final int split = N / 2;
                final SnapSearch sub1 = new SnapSearch(widgets.subList(0, split), x, y);
                final SnapSearch sub2 = new SnapSearch(widgets.subList(split, N), x, y);
                // Spawn sub1, handle sub2 in this thread, then combine results
                sub1.fork();
                result = sub2.compute();
                result.updateFrom(sub1.join());
            }
            else
            {
                result = new SnapResult();
                for (final Widget child : widgets)
                {
                    final SnapResult sub_result = checkWidget(x, y, child);
                    result.updateFrom(sub_result);
                }
            }
            return result;
        }

        /** @param x Requested X position
         *  @param y Requested Y position
         *  @param widget Widget where corners are checked as snap candidates
         *  @return {@link SnapResult}
         */
        private SnapResult checkWidget(final double x, final double y, final Widget widget)
        {
            // System.out.println("Checking " + widget.getClass().getSimpleName() + " in " + Thread.currentThread().getName());
            final SnapResult result = new SnapResult();

            // Do _not_ snap to one of the active widgets,
            // because that would lock their coordinates.
            if (selected_widgets.contains(widget))
                return result;

            // Check all widget corners
            final Rectangle2D bounds = GeometryTools.getDisplayBounds(widget);
            updateSnapResult(result, x, y, bounds.getMinX(), bounds.getMinY());
            updateSnapResult(result, x, y, bounds.getMaxX(), bounds.getMinY());
            updateSnapResult(result, x, y, bounds.getMaxX(), bounds.getMaxY());
            updateSnapResult(result, x, y, bounds.getMinX(), bounds.getMaxY());

            final ChildrenProperty children = ChildrenProperty.getChildren(widget);
            if (children != null)
                result.updateFrom(checkWidgets(children.getValue()));
            return result;
        }

        /** @param result Result to update if this test point is closer
         *  @param x Requested X position
         *  @param y Requested Y position
         *  @param corner_x X coord of a widget corner
         *  @param corner_y Y coord of a widget corner
         */
        private void updateSnapResult(final SnapResult result,
                                      final double x, final double y,
                                      final double corner_x, final double corner_y)
        {
            // Determine distance of corner from requested point
            final double dx = Math.abs(corner_x - x);
            final double dy = Math.abs(corner_y - y);
            final double distance = dx*dx + dy*dy;

            // Horizontal snap, closer to what's been found before?
            if (dx < snap_distance  &&  distance < result.horiz_distance)
            {
                result.horiz = corner_x;
                result.horiz_distance = distance;
            }

            // Vertical snap, closer to what's been found before?
            if (dy < snap_distance  &&  distance < result.vert_distance)
            {
                result.vert = corner_y;
                result.vert_distance = distance;
            }
        }
    }

    /** @param toolkit
     *  @param group Group where snap lines are added
     */
    public TrackerSnapConstraint(final JFXRepresentation toolkit, final Group group)
    {
        this.toolkit = toolkit;
        horiz_guide = new Line();
        horiz_guide.getStyleClass().add("guide_line");
        horiz_guide.setVisible(false);
        horiz_guide.setManaged(false);

        vert_guide = new Line();
        vert_guide.getStyleClass().add("guide_line");
        vert_guide.setVisible(false);
        vert_guide.setManaged(false);

        group.getChildren().addAll(horiz_guide, vert_guide);
    }

    @Override
    public void setEnabled (final boolean enabled)
    {
        super.setEnabled(enabled);
        if (!enabled)
            setVisible(false);
    }

    /** Sets the guidelines visible or not.
     *  @param visible {@code true} if guidelines must be visible, {@code false} otherwise.
     */
    public void setVisible(final boolean visible)
    {
        horiz_guide.setVisible(visible);
        vert_guide.setVisible(visible);
    }

    /** Configure tracker
     *  @param model Current model
     *  @param selected_widgets Selected widgets
     */
    public void configure(final DisplayModel model, final List<Widget> selected_widgets)
    {
        this.model = model;
        this.selected_widgets = selected_widgets;
    }

    @Override
    public Point2D constrain(double x, double y)
    {
        // System.out.println("Snap search:");
        final SnapSearch task = new SnapSearch(Arrays.asList(model), x, y);
        final SnapResult result = task.compute();
        // System.out.println("Done");

        // Editor's viewport that's used to determine size of snap lines.
        // Assume the scroll pane has been moved such that the upper left corner
        // of the viewport shows the model location x0, y0.
        // The viewport bounds will then have (minX, minY) = (-x0*zoom, -y0*zoom).
        final double zoom = toolkit.getZoom();
        final Bounds viewport = toolkit.getModelRoot().getViewportBounds();
        // System.out.println("Scroll viewport: " + viewport);

        if (result.horiz == SnapResult.INVALID)
            horiz_guide.setVisible(false);
        else
        {
            x = result.horiz;
            horiz_guide.setStartX(x);
            horiz_guide.setEndX(x);

            //  '+3':
            // Snap lines will become part of the viewport.
            // If they touch end edge of the viewport, the viewport will grow to include the snap lines.
            // resulting in a growing viewport as the mouse is moved and the snaplines are updated.
            final double top = (-viewport.getMinY()+3) / zoom,
                         bottom = top + (viewport.getHeight()-6) / zoom;
            horiz_guide.setStartY(top);
            horiz_guide.setEndY(bottom);
            horiz_guide.setVisible(true);
        }
        if (result.vert == SnapResult.INVALID)
            vert_guide.setVisible(false);
        else
        {
            y = result.vert;
            vert_guide.setStartY(y);
            vert_guide.setEndY(y);
            //  '+3': See above
            final double left = (-viewport.getMinX()+3) / zoom,
                         right = left + (viewport.getWidth()-6) / zoom;
            vert_guide.setStartX(left);
            vert_guide.setEndX(right);
            vert_guide.setVisible(true);
        }

        return new Point2D(x, y);
    }
}
