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
 *  background, foreground, fill and empty colours.
 *
 *  @author Kay Kasemir
 *  @author Heredie Delvalle &mdash; CLS, alarm limits, dual scale,
 *          format/precision wiring; refactored onto RTScaledWidgetRepresentation
 */
@SuppressWarnings("nls")
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
        model_widget.propWidth().addUntypedPropertyListener(lookListener);
        model_widget.propHeight().addUntypedPropertyListener(lookListener);
        model_widget.propFont().addUntypedPropertyListener(lookListener);
        model_widget.propForeground().addUntypedPropertyListener(lookListener);
        model_widget.propBackground().addUntypedPropertyListener(lookListener);
        model_widget.propFillColor().addUntypedPropertyListener(lookListener);
        model_widget.propEmptyColor().addUntypedPropertyListener(lookListener);
        model_widget.propScaleVisible().addUntypedPropertyListener(lookListener);
        model_widget.propShowMinorTicks().addUntypedPropertyListener(lookListener);
        model_widget.propPerpendicularTickLabels().addUntypedPropertyListener(lookListener);
        model_widget.propFormat().addUntypedPropertyListener(lookListener);
        model_widget.propPrecision().addUntypedPropertyListener(lookListener);
        model_widget.propOppositeScaleVisible().addUntypedPropertyListener(lookListener);
        model_widget.propBorderWidth().addUntypedPropertyListener(lookListener);
        model_widget.propLogScale().addUntypedPropertyListener(lookListener);
        model_widget.propHorizontal().addPropertyListener(orientationChangedListener);
    }

    @Override
    protected void unregisterLookListeners()
    {
        model_widget.propWidth().removePropertyListener(lookListener);
        model_widget.propHeight().removePropertyListener(lookListener);
        model_widget.propFont().removePropertyListener(lookListener);
        model_widget.propForeground().removePropertyListener(lookListener);
        model_widget.propBackground().removePropertyListener(lookListener);
        model_widget.propFillColor().removePropertyListener(lookListener);
        model_widget.propEmptyColor().removePropertyListener(lookListener);
        model_widget.propScaleVisible().removePropertyListener(lookListener);
        model_widget.propShowMinorTicks().removePropertyListener(lookListener);
        model_widget.propPerpendicularTickLabels().removePropertyListener(lookListener);
        model_widget.propFormat().removePropertyListener(lookListener);
        model_widget.propPrecision().removePropertyListener(lookListener);
        model_widget.propOppositeScaleVisible().removePropertyListener(lookListener);
        model_widget.propBorderWidth().removePropertyListener(lookListener);
        model_widget.propLogScale().removePropertyListener(lookListener);
        model_widget.propHorizontal().removePropertyListener(orientationChangedListener);
    }

    @Override
    protected void applyLookToTank(final double width, final double height)
    {
        tank.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
        tank.setBackground(JFXUtil.convert(model_widget.propBackground().getValue()));
        tank.setForeground(JFXUtil.convert(model_widget.propForeground().getValue()));
        tank.setFillColor(JFXUtil.convert(model_widget.propFillColor().getValue()));
        tank.setEmptyColor(JFXUtil.convert(model_widget.propEmptyColor().getValue()));
        tank.setScaleVisible(model_widget.propScaleVisible().getValue());
        tank.setShowMinorTicks(model_widget.propShowMinorTicks().getValue());
        tank.setPerpendicularTickLabels(model_widget.propPerpendicularTickLabels().getValue());
        tank.setLogScale(model_widget.propLogScale().getValue());
        tank.setLabelFormat(model_widget.propFormat().getValue(),
                            model_widget.propPrecision().getValue());
        tank.setRightScaleVisible(model_widget.propOppositeScaleVisible().getValue());
        tank.setBorderWidth(model_widget.propBorderWidth().getValue());
    }
}
