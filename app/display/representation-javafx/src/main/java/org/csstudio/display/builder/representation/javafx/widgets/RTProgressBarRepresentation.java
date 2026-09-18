/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.widgets.ProgressBarWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.scene.paint.Color;

/** Progress Bar representation based on {@link org.csstudio.javafx.rtplot.RTTank}
 *
 *  <p>Adds a numeric scale with format/precision, an optional second scale
 *  and alarm limit lines to the progress bar. Used instead of the stock
 *  {@link ProgressBarRepresentation} when the {@code progressbar_scale_mode}
 *  preference is set.
 *
 *  <p>Value, range, alarm limit and orientation handling are shared with
 *  the Tank in {@link RTScaledWidgetRepresentation}. This class only maps
 *  the progress bar's appearance properties onto the tank.
 */
public class RTProgressBarRepresentation extends RTScaledWidgetRepresentation<ProgressBarWidget>
{
    @Override
    protected void configureTank()
    {
        // Shade the empty region like the track of the stock ProgressBar
        tank.setBarTrack(true);
    }

    @Override
    protected boolean isHorizontal()
    {
        return model_widget.propHorizontal().getValue();
    }

    @Override
    protected void registerLookListeners()
    {
        registerScaleLookListeners();
        model_widget.propBackgroundColor().addUntypedPropertyListener(lookListener);
        model_widget.propInnerPadding().addUntypedPropertyListener(lookListener);
        model_widget.propHorizontal().addPropertyListener(orientationChangedListener);
    }

    @Override
    protected void unregisterLookListeners()
    {
        unregisterScaleLookListeners();
        model_widget.propBackgroundColor().removePropertyListener(lookListener);
        model_widget.propInnerPadding().removePropertyListener(lookListener);
        model_widget.propHorizontal().removePropertyListener(orientationChangedListener);
    }

    @Override
    protected void applyLookToTank()
    {
        applyScaleLook();
        // As in the JavaFX control, the background color is the color of the
        // track, and nothing is painted outside the track and the scale
        tank.setBackground(Color.TRANSPARENT);
        tank.setEmptyColor(JFXUtil.convert(model_widget.propBackgroundColor().getValue()));
        tank.setInnerPadding(model_widget.propInnerPadding().getValue());
    }
}
