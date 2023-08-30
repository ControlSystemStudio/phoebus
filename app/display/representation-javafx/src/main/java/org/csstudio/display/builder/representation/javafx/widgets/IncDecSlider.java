/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.Slider;
import javafx.scene.control.skin.SliderSkin;
import javafx.scene.input.MouseEvent;

/** Slider where clicking on the track increments/decrements
 *
 *  <p>The default slider will jump to the clicked value.
 *  This variant increments or decrements, similar
 *  to using the cursor keys.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class IncDecSlider extends Slider
{
    @Override
    protected Skin<?> createDefaultSkin()
    {
        final SliderSkin skin = (SliderSkin) super.createDefaultSkin();

        // SliderSkin is accessible, but the more interesting
        // com.sun.javafx.scene.control.behavior.SliderBehavior
        // is not.
        // Work around this by locating 'track'...
        for (Node node : skin.getChildren())
            if (node.getStyleClass().contains("track"))
            {
                // Capture mouse clicks, use to inc/dec instead of jumping there
                node.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> handleTrackClick(node, event));

                // Disable mouse drag, which by default also jumps to mouse
                node.setOnMouseDragged(null);
                break;
            }

        return skin;
    }

    private void handleTrackClick(final Node track, final MouseEvent event)
    {
        // Where, in units of 0..1, was the track clicked?
        final double click;
        if (getOrientation() == Orientation.HORIZONTAL)
            click = event.getX() / track.getBoundsInLocal().getWidth();
        else
        {
            final double height = track.getBoundsInLocal().getHeight();
            click = (height - event.getY()) / height;
        }
        event.consume();

        // Is that below or above the current value in units of 0..1?
        final double val = (getValue() - getMin()) / (getMax() - getMin());
        if (click > val)
            increment();
        else
            decrement();
    }
}
