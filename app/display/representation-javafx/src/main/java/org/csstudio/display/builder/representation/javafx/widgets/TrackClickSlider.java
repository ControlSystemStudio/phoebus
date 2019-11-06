/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
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
 *  This variant incements or decrements, similar
 *  to using the cursor keys.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TrackClickSlider extends Slider
{
    @Override
    protected Skin<?> createDefaultSkin()
    {
        final SliderSkin skin = (SliderSkin) super.createDefaultSkin();

        // SliderSkin is accessible, but the more interesting
        // com.sun.javafx.scene.control.behavior.SliderBehavior
        // is not.
        // Work around this by locating 'track',
        // then capturing mouse clicks.
        for (Node node : skin.getChildren())
            if (node.getStyleClass().contains("track"))
            {
                node.addEventFilter(MouseEvent.MOUSE_PRESSED, event ->
                {
                    if (getOrientation() == Orientation.HORIZONTAL)
                        handleTrackClick(event.getX() / node.getBoundsInLocal().getWidth());
                    else
                    {
                        final double height = node.getBoundsInLocal().getHeight();
                        handleTrackClick((height - event.getY()) / height);
                    }
                    event.consume();
                });
                break;
            }

        return skin;
    }

    private void handleTrackClick(final double click)
    {
        final double val = getValue() / (getMax() - getMin());
        if (click > val)
            increment();
        else
            decrement();
    }
}
