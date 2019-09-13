/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.ScaledSliderWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeMotifSliderClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeMotifSliderClass extends ConverterBase<ScaledSliderWidget>
{
    public Convert_activeMotifSliderClass(final EdmConverter converter, final Widget parent, final Edm_activeMotifSliderClass r)
    {
        super(converter, parent, r);

        widget.propHorizontal().setValue(!"vertical".equals(r.getOrientation()));
        convertColor(r.getBgColor(), widget.propBackgroundColor());
        convertColor(r.getFgColor(), widget.propForegroundColor());
        convertFont(r.getFont(), widget.propFont());

        widget.propPVName().setValue(convertPVName(r.getControlPv()));
        if (! r.isLimitsFromDb())
        {
            widget.propMinimum().setValue(r.getScaleMin());
            widget.propMaximum().setValue(r.getScaleMax());
        }
    }

    @Override
    protected ScaledSliderWidget createWidget(final EdmWidget edm)
    {
        return new ScaledSliderWidget();
    }
}
