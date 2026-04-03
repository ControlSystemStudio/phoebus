/*******************************************************************************
 * Copyright (c) 2015-2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.widgets.ProgressBarWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.scene.paint.Color;

/** Creates JavaFX item for the Progress Bar widget using {@link org.csstudio.javafx.rtplot.RTTank}
 *  as the rendering engine.
 *
 *  <p>This representation adds a numeric scale, tick format/precision,
 *  an optional second scale, and alarm-limit lines to the progress bar.
 *  It is selected when the {@code progressbar_scale_mode} preference is
 *  {@code true}; the stock representation ({@link ProgressBarRepresentation})
 *  is used otherwise, preserving the default JFX look.
 *
 *  <p>Shared RTTank lifecycle (value / range updates, alarm limits,
 *  orientation handling) lives in {@link RTScaledWidgetRepresentation}.
 *  This class contributes only the ProgressBar-specific appearance mapping:
 *  fill colour and a uniform background for the unfilled bar track.
 *
 *  @author Kay Kasemir
 *  @author Amanda Carpenter
 *  @author Heredie Delvalle &mdash; CLS, RTTank-based refactoring, scale support;
 *          refactored onto RTScaledWidgetRepresentation
 */
@SuppressWarnings("nls")
public class RTProgressBarRepresentation extends RTScaledWidgetRepresentation<ProgressBarWidget>
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
        model_widget.propFillColor().addUntypedPropertyListener(lookListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(lookListener);
        model_widget.propScaleVisible().addUntypedPropertyListener(lookListener);
        model_widget.propShowMinorTicks().addUntypedPropertyListener(lookListener);
        model_widget.propOppositeScaleVisible().addUntypedPropertyListener(lookListener);
        model_widget.propPerpendicularTickLabels().addUntypedPropertyListener(lookListener);
        model_widget.propBorderWidth().addUntypedPropertyListener(lookListener);
        model_widget.propLogScale().addUntypedPropertyListener(lookListener);
        model_widget.propFormat().addUntypedPropertyListener(lookListener);
        model_widget.propPrecision().addUntypedPropertyListener(lookListener);
        model_widget.propHorizontal().addPropertyListener(orientationChangedListener);
    }

    @Override
    protected void unregisterLookListeners()
    {
        model_widget.propWidth().removePropertyListener(lookListener);
        model_widget.propHeight().removePropertyListener(lookListener);
        model_widget.propFont().removePropertyListener(lookListener);
        model_widget.propFillColor().removePropertyListener(lookListener);
        model_widget.propBackgroundColor().removePropertyListener(lookListener);
        model_widget.propScaleVisible().removePropertyListener(lookListener);
        model_widget.propShowMinorTicks().removePropertyListener(lookListener);
        model_widget.propOppositeScaleVisible().removePropertyListener(lookListener);
        model_widget.propPerpendicularTickLabels().removePropertyListener(lookListener);
        model_widget.propBorderWidth().removePropertyListener(lookListener);
        model_widget.propLogScale().removePropertyListener(lookListener);
        model_widget.propFormat().removePropertyListener(lookListener);
        model_widget.propPrecision().removePropertyListener(lookListener);
        model_widget.propHorizontal().removePropertyListener(orientationChangedListener);
    }

    @Override
    protected void applyLookToTank(final double width, final double height)
    {
        final Color bg = JFXUtil.convert(model_widget.propBackgroundColor().getValue());
        tank.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
        tank.setBackground(bg);
        // Map background to empty colour so the unfilled portion blends with
        // the outer margin, giving a uniform "bar-track" appearance.
        tank.setEmptyColor(bg);
        tank.setFillColor(JFXUtil.convert(model_widget.propFillColor().getValue()));
        tank.setScaleVisible(model_widget.propScaleVisible().getValue());
        tank.setShowMinorTicks(model_widget.propShowMinorTicks().getValue());
        tank.setRightScaleVisible(model_widget.propOppositeScaleVisible().getValue());
        tank.setPerpendicularTickLabels(model_widget.propPerpendicularTickLabels().getValue());
        tank.setBorderWidth(model_widget.propBorderWidth().getValue());
        tank.setLogScale(model_widget.propLogScale().getValue());
        tank.setLabelFormat(model_widget.propFormat().getValue(),
                            model_widget.propPrecision().getValue());
    }
}
