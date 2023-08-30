/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.List;

import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.LEDWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.VEnum;
import org.epics.vtype.VType;

import javafx.scene.paint.Color;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
public class LEDRepresentation extends BaseLEDRepresentation<LEDWidget>
{
    private final UntypedWidgetPropertyListener configChangedListener = this::configChanged;

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propOffColor().addUntypedPropertyListener(configChangedListener);
        model_widget.propOnColor().addUntypedPropertyListener(configChangedListener);
        model_widget.propOffLabel().addUntypedPropertyListener(configChangedListener);
        model_widget.propOnLabel().addUntypedPropertyListener(configChangedListener);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propOffColor().removePropertyListener(configChangedListener);
        model_widget.propOnColor().removePropertyListener(configChangedListener);
        model_widget.propOffLabel().removePropertyListener(configChangedListener);
        model_widget.propOnLabel().removePropertyListener(configChangedListener);
        super.unregisterListeners();
    }

    @Override
    protected Color[] createColors()
    {
        return new Color[]
        {
            JFXUtil.convert(model_widget.propOffColor().getValue()),
            JFXUtil.convert(model_widget.propOnColor().getValue())
        };
    }

    @Override
    protected int computeColorIndex(final VType value)
    {
        if ((value instanceof VEnum)  &&
            model_widget.propLabelsFromPV().getValue())
        {
            final List<String> labels = ((VEnum) value).getDisplay().getChoices();
            if (labels.size() == 2)
            {
                model_widget.propOffLabel().setValue(labels.get(0));
                model_widget.propOnLabel().setValue(labels.get(1));
            }
        }

        int number = VTypeUtil.getValueNumber(value).intValue();
        final int bit = model_widget.propBit().getValue();
        if (bit >= 0)
            number &= (1 << bit);
        return number == 0 ? 0 : 1;
    }

    @Override
    protected String computeLabel(final int color_index)
    {
        if (color_index == 1)
            return model_widget.propOnLabel().getValue();
        return model_widget.propOffLabel().getValue();
    }

    @Override
    protected String computeLabel()
    {
        if (model_widget.propOnLabel().getValue().contentEquals(model_widget.propOffLabel().getValue()))
            return model_widget.propOnLabel().getValue();

        return "";
    }
}
