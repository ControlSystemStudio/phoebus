/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.widgets.ThermometerWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.scene.paint.Color;

/** Thermometer representation based on {@link org.csstudio.javafx.rtplot.RTTank}
 *
 *  <p>The tank draws tube, bulb, liquid, scale and alarm limit lines in one
 *  pass, so the liquid level always lines up with the tick marks. Compared
 *  with the stock {@link ThermometerRepresentation} this adds a configurable
 *  scale (log, format, precision, minor ticks) and alarm limit lines.
 *  Used when the {@code thermometer_scale_mode} preference is set.
 *
 *  <p>Value, range and alarm limit handling are shared with the Tank in
 *  {@link RTScaledWidgetRepresentation}. This class only maps the
 *  thermometer's appearance properties onto the tank.
 */
public class RTThermometerRepresentation extends RTScaledWidgetRepresentation<ThermometerWidget>
{
    @Override
    protected boolean isHorizontal()
    {
        return false;
    }

    @Override
    protected void configureTank()
    {
        tank.setThermometerStyle(true);
    }

    @Override
    protected void registerLookListeners()
    {
        registerScaleLookListeners();
        model_widget.propEmptyColor().addUntypedPropertyListener(lookListener);
        model_widget.propBulbSize().addUntypedPropertyListener(lookListener);
    }

    @Override
    protected void unregisterLookListeners()
    {
        unregisterScaleLookListeners();
        model_widget.propEmptyColor().removePropertyListener(lookListener);
        model_widget.propBulbSize().removePropertyListener(lookListener);
    }

    @Override
    protected void applyLookToTank()
    {
        applyScaleLook();
        // As in the stock thermometer, only the tube, the bulb and the scale
        // are drawn, so the display shows through around them
        tank.setBackground(Color.TRANSPARENT);
        tank.setEmptyColor(JFXUtil.convert(model_widget.propEmptyColor().getValue()));
        tank.setBulbSize(model_widget.propBulbSize().getValue());
    }
}
