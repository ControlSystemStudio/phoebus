/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.editor.poly;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHeight;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propPoints;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propWidth;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propX;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propY;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.csstudio.display.builder.editor.PointConstraint;
import org.csstudio.display.builder.editor.WidgetSelectionHandler;
import org.csstudio.display.builder.editor.WidgetSelectionListener;
import org.csstudio.display.builder.editor.undo.SetWidgetPointsAction;
import org.csstudio.display.builder.editor.util.GeometryTools;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.Points;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.geometry.Point2D;
import javafx.scene.Group;

/** Bind "points" property of selected widget to {@link PointsEditor}
 *
 *  <p>Also updates widget's X, Y, Width, Height to coordinate range of points.
 *  @author Kay Kasemir
 */
public class PointsBinding implements WidgetSelectionListener, PointsEditorListener, UntypedWidgetPropertyListener
{
    private static boolean enable_scaling = true;
    private final Group parent;
    private final PointConstraint constraint;
    private final WidgetSelectionHandler selection;
    private final UndoableActionManager undo;
    private Widget widget;
    private PointsEditor editor;
    /** Flag to prevent loop when this binding is changing the widget */
    private boolean changing_widget = false;

    /** Disable or enable scaling of points as width/height of widget changes
     *  @param enabled Enable scaling?
     */
    public static void setScaling(final boolean enabled)
    {
        enable_scaling = enabled;
    }

    /** @param parent JFX {@link Group} where points editor is placed
     *  @param constraint Grid constraint
     *  @param selection Selection handler
     *  @param undo Undo manager
     */
    public PointsBinding(final Group parent, final PointConstraint constraint, final WidgetSelectionHandler selection,
                         final UndoableActionManager undo)
    {
        this.parent = parent;
        this.constraint = constraint;
        this.selection = selection;
        this.undo = undo;
        selection.addListener(this);
    }

    @Override
    public void selectionChanged(final List<Widget> widgets)
    {
        if (widgets != null  &&  widgets.size() == 1)
        {
            final Widget w = widgets.get(0);
            Optional<WidgetProperty<Points>> prop = w.checkProperty(propPoints);
            if (prop.isPresent())
            {
                createEditor(w);
                return;
            }
        }
        // Not exactly one widget with "points" -> No editor
        disposeEditor();
    }

    /** @param widget Widget for which to create editor */
    private void createEditor(final Widget widget)
    {
        disposeEditor();

        this.widget = Objects.requireNonNull(widget);

        // Turn points from widget into absolute screen coords for editor
        final Points screen_points = widget.getProperty(propPoints).getValue().clone();
        final Point2D offset = GeometryTools.getDisplayOffset(widget);
        final double x0 = widget.getProperty(propX).getValue() + offset.getX();
        final double y0 = widget.getProperty(propY).getValue() + offset.getY();
        final int N=screen_points.size();
        for (int i=0; i<N; ++i)
        {
            screen_points.setX(i, x0 + screen_points.getX(i));
            screen_points.setY(i, y0 + screen_points.getY(i));
        }

        editor = new PointsEditor(parent, constraint, screen_points, this);
        widget.getProperty(propX).addUntypedPropertyListener(this);
        widget.getProperty(propY).addUntypedPropertyListener(this);
        widget.getProperty(propWidth).addUntypedPropertyListener(this);
        widget.getProperty(propHeight).addUntypedPropertyListener(this);
        widget.getProperty(propPoints).addUntypedPropertyListener(this);
    }

    private void disposeEditor()
    {
        if (editor == null)
            return;
        widget.getProperty(propPoints).removePropertyListener(this);
        widget.getProperty(propHeight).removePropertyListener(this);
        widget.getProperty(propWidth).removePropertyListener(this);
        widget.getProperty(propY).removePropertyListener(this);
        widget.getProperty(propX).removePropertyListener(this);
        editor.dispose();
        editor = null;
        widget = null;
    }

    // WidgetPropertyListener
    @Override
    public void propertyChanged(WidgetProperty<?> property, Object old_value, Object new_value)
    {   // Ignore changes performed by this class
        if (changing_widget)
            return;

        // Delete editor since position has changed, and editor's points
        // are thus invalid
        final Widget active_widget = this.widget;
        disposeEditor();

        if (enable_scaling)
        {
            if (property.getName().equals(propWidth.getName()))
                scaleHoriz(active_widget, (Number)new_value, (Number)old_value);
            else if (property.getName().equals(propHeight.getName()))
                scaleVert(active_widget, (Number)new_value, (Number)old_value);
        }

        // Re-create editor for changed widget
        createEditor(active_widget);
    }

    private void scaleHoriz(final Widget widget, final Number new_size, final Number old_size)
    {
        final double factor = new_size.doubleValue() / old_size.doubleValue();
        final WidgetProperty<Points> prop = widget.getProperty(propPoints);
        final Points points = prop.getValue().clone();
        final int N = points.size();
        for (int i=0; i<N; ++i)
            points.setX(i, factor * points.getX(i));
        prop.setValue(points);
    }

    private void scaleVert(final Widget widget, final Number new_size, final Number old_size)
    {
        final double factor = new_size.doubleValue() / old_size.doubleValue();
        final WidgetProperty<Points> prop = widget.getProperty(propPoints);
        final Points points = prop.getValue().clone();
        final int N = points.size();
        for (int i=0; i<N; ++i)
            points.setY(i, factor * points.getY(i));
        prop.setValue(points);
    }

    // PointsEditorListener
    @Override
    public void pointsChanged(final Points screen_points)
    {
        // Update widget with edited points
        final Points points = screen_points.clone();
        final int N = points.size();
        // Widget may be inside a container
        final Point2D offset = GeometryTools.getDisplayOffset(widget);
        // Determine coordinate range of points
        double x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE;
        double x1 = 0, y1 = 0;
        for (int i=0; i<N; ++i)
        {
            final double x = points.getX(i) - offset.getX();
            final double y = points.getY(i) - offset.getY();
            x0 = Math.min(x, x0);
            y0 = Math.min(y, y0);
            x1 = Math.max(x, x1);
            y1 = Math.max(y, y1);
        }
        // Adjust points relative to x0, y0 and widget's container offset
        for (int i=0; i<N; ++i)
        {
            final double x = points.getX(i) - offset.getX();
            final double y = points.getY(i) - offset.getY();
            points.setX(i, x - x0);
            points.setY(i, y - y0);
        }

        changing_widget = true;
        try
        {
            if (N > 0)
                undo.execute(new SetWidgetPointsAction(widget.getProperty(propPoints), points,
                                                       (int) x0, (int) y0,
                                                       (int) (x1 - x0), (int) (y1 - y0)));
            else
                undo.execute(new SetWidgetPointsAction(widget.getProperty(propPoints), points));
        }
        finally
        {
            changing_widget = false;
        }
    }

    // PointsEditorListener
    @Override
    public void done()
    {
        disposeEditor();
        selection.clear();
    }
}
