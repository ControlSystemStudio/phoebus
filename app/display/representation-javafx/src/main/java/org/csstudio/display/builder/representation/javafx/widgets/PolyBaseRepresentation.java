/*******************************************************************************
 * Copyright (c) 2020 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.PolyBaseWidget;

import javafx.scene.Node;

/** Base class for Polygon and Polyline widget representations
 *  @param <JFX> JFX Widget
 *  @param <MW> Model widget
 *  @author Krisztián Löki
 */
public abstract class PolyBaseRepresentation<JFX extends Node, MW extends PolyBaseWidget> extends JFXBaseRepresentation<JFX, MW>
{
    private final UntypedWidgetPropertyListener widthChangedListener = this::widthChanged;
    private final UntypedWidgetPropertyListener heightChangedListener = this::heightChanged;
    private volatile double hScale = 1.0;
    private volatile double vScale = 1.0;
    private volatile double width = Double.NaN;
    private volatile double height = Double.NaN;

    @Override
    protected void registerListeners()
    {
        if (! toolkit.isEditMode())
        {
            attachTooltip();
        // Polyline and Polygon can't use the default x/y handling from super.registerListeners();

            model_widget.propWidth().addUntypedPropertyListener(widthChangedListener);
            model_widget.propHeight().addUntypedPropertyListener(heightChangedListener);
        }
    }

    @Override
    protected void unregisterListeners()
    {
        if (! toolkit.isEditMode())
        {
            detachTooltip();
            model_widget.propWidth().removePropertyListener(widthChangedListener);
            model_widget.propHeight().removePropertyListener(heightChangedListener);
        }
    }

    abstract protected void displayChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value);

    private void widthChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        if (Double.isNaN(width))
            width = ((Integer)old_value).doubleValue();
        hScale = ((Integer)new_value).doubleValue() / width;
        displayChanged(null, null, null);
    }

    private void heightChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        if (Double.isNaN(height))
            height = ((Integer)old_value).doubleValue();
        vScale = ((Integer)new_value).doubleValue() / height;
        displayChanged(null, null, null);
    }

    protected Double[] scalePoints()
    {
        // Change points x/y relative to widget location into
        // on-screen location
        final int x = model_widget.propX().getValue();
        final int y = model_widget.propY().getValue();
        final Double[] points = model_widget.propPoints().getValue().asDoubleArray();
        for (int i = 0; i < points.length; i += 2)
        {
            points[i] = points[i] * hScale + x;
            points[i + 1] = points[i + 1] * vScale + y;
        }

        return points;
    }
}
