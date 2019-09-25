/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.ChildrenProperty;
import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget.Style;
import org.csstudio.display.builder.model.widgets.ScaledSliderWidget;
import org.csstudio.display.builder.model.widgets.TextUpdateWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeSliderClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeSliderClass extends ConverterBase<ScaledSliderWidget>
{
    public Convert_activeSliderClass(final EdmConverter converter, final Widget parent, final Edm_activeSliderClass r)
    {
        super(converter, parent, r);

        convertColor(r.getBgColor(), widget.propBackgroundColor());
        convertColor(r.getFgColor(), widget.propForegroundColor());
        convertFont(r.getFont(), widget.propFont());

        if (r.getAttribute("controlPv").isExistInEDL())
            widget.propPVName().setValue(convertPVName(r.getControlPv()));

        if (r.getScaleMax() == 0 && r.getScaleMin() == 0)
            widget.propLimitsFromPV().setValue(true);
        else
        {
            widget.propLimitsFromPV().setValue(r.isLimitsFromDb());
            widget.propMinimum().setValue(r.getScaleMin());
            widget.propMaximum().setValue(r.getScaleMax());
        }
        widget.propShowLoLo().setValue(false);
        widget.propShowLow().setValue(false);
        widget.propShowHigh().setValue(false);
        widget.propShowHiHi().setValue(false);

        if (r.getIndicatorPv() != null)
        {
            // Wrap slider into a group: slider, indicator
            final GroupWidget group = new GroupWidget();
            group.propName().setValue("EDM Slider/Indicator");
            group.propStyle().setValue(Style.NONE);
            group.propTransparent().setValue(true);

            final TextUpdateWidget indicator = new TextUpdateWidget();
            indicator.propName().setValue("EDM Slider Indicator");
            indicator.propPVName().setValue(convertPVName(r.getIndicatorPv()));
            indicator.propFont().setValue(widget.propFont().getValue());

            // Group uses size of original 'slider'
            group.propX().setValue(widget.propX().getValue());
            group.propY().setValue(widget.propY().getValue());
            group.propWidth().setValue(widget.propWidth().getValue());
            group.propHeight().setValue(widget.propHeight().getValue());

            // Slider moves to top of group, max. height 40
            widget.propX().setValue(0);
            widget.propY().setValue(0);
            widget.propHeight().setValue(Math.min(40, group.propHeight().getValue() - indicator.propHeight().getValue()));

            // Indicator below slider
            indicator.propX().setValue((group.propWidth().getValue() - indicator.propWidth().getValue())/2);
            indicator.propY().setValue(widget.propHeight().getValue());

            // Re-parenting
            final ChildrenProperty children = ChildrenProperty.getChildren(parent);
            children.removeChild(widget);
            children.addChild(group);
            group.runtimeChildren().addChild(widget);
            group.runtimeChildren().addChild(indicator);
        }
    }

    @Override
    protected ScaledSliderWidget createWidget(final EdmWidget edm)
    {
        return new ScaledSliderWidget();
    }
}
