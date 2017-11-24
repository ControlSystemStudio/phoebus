/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.editor.undo;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propHeight;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propWidth;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propX;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propY;

import org.csstudio.display.builder.editor.Messages;
import org.csstudio.display.builder.editor.poly.PointsBinding;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.Points;
import org.phoebus.ui.undo.UndoableAction;


/** Action to update widget points
 *  @author Kay Kasemir
 */
public class SetWidgetPointsAction extends UndoableAction
{
    private final WidgetProperty<Points> property;
    private final Points orig_points, points;
    private final int orig_x, orig_y, orig_width, orig_height,
                      x, y, width, height;

    public SetWidgetPointsAction(final WidgetProperty<Points> property,
                                 final Points points)
    {
        this(property, points,
             property.getWidget().getPropertyValue(propX),
             property.getWidget().getPropertyValue(propY),
             property.getWidget().getPropertyValue(propWidth),
             property.getWidget().getPropertyValue(propHeight));
    }

    public SetWidgetPointsAction(final WidgetProperty<Points> property,
                                 final Points points,
                                 final int x, final int y,
                                 final int width, final int height)
    {
        super(Messages.SetWidgetPoints);
        this.property = property;
        this.orig_points = property.getValue();
        this.orig_x = property.getWidget().getPropertyValue(propX);
        this.orig_y = property.getWidget().getPropertyValue(propY);
        this.orig_width = property.getWidget().getPropertyValue(propWidth);
        this.orig_height = property.getWidget().getPropertyValue(propHeight);
        this.points = points;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void run()
    {
        // Prevent PointsBinding from scaling points as width/height is adjusted
        PointsBinding.setScaling(false);
        property.setValue(points);
        property.getWidget().setPropertyValue(propX, x);
        property.getWidget().setPropertyValue(propY, y);
        property.getWidget().setPropertyValue(propWidth, width);
        property.getWidget().setPropertyValue(propHeight, height);
        PointsBinding.setScaling(true);
    }

    @Override
    public void undo()
    {
        // Prevent PointsBinding from scaling points as width/height is adjusted
        PointsBinding.setScaling(false);
        property.setValue(orig_points);
        property.getWidget().setPropertyValue(propX, orig_x);
        property.getWidget().setPropertyValue(propY, orig_y);
        property.getWidget().setPropertyValue(propWidth, orig_width);
        property.getWidget().setPropertyValue(propHeight, orig_height);
        PointsBinding.setScaling(true);
    }
}
