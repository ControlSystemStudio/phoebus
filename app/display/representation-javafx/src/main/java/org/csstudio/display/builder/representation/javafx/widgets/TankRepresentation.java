/*******************************************************************************
 * Copyright (c) 2015-2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.widgets.TankWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

/** Creates JavaFX item for the Tank widget.
 *
 *  <p>All shared RTTank wiring (value updates, alarm limits,
 *  orientation handling) lives in {@link RTScaledWidgetRepresentation}.
 *  This class contributes only the Tank-specific appearance properties:
 *  background and empty colors.
 *
 *  @author Kay Kasemir
 *  @author Heredie Delvalle &mdash; CLS, alarm limits, dual scale,
 *          format/precision wiring
 */
public class TankRepresentation extends RTScaledWidgetRepresentation<TankWidget>
{
    @Override
    protected boolean isHorizontal()
    {
        return model_widget.propHorizontal().getValue();
    }

    @Override
    protected void registerLookListeners()
    {
        registerScaleLookListeners();
        model_widget.propBackground().addUntypedPropertyListener(lookListener);
        model_widget.propEmptyColor().addUntypedPropertyListener(lookListener);
        model_widget.propHorizontal().addPropertyListener(orientationChangedListener);
    }

    @Override
    protected void unregisterLookListeners()
    {
        unregisterScaleLookListeners();
        model_widget.propBackground().removePropertyListener(lookListener);
        model_widget.propEmptyColor().removePropertyListener(lookListener);
        model_widget.propHorizontal().removePropertyListener(orientationChangedListener);
    }

    @Override
    protected void applyLookToTank()
    {
        applyScaleLook();
        tank.setBackground(JFXUtil.convert(model_widget.propBackground().getValue()));
        tank.setEmptyColor(JFXUtil.convert(model_widget.propEmptyColor().getValue()));
    }
}
