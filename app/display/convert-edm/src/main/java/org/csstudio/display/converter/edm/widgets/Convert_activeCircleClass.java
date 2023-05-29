/*******************************************************************************
 * Copyright (c) 2019-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.BaseLEDWidget;
import org.csstudio.display.builder.model.widgets.EllipseWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeCircleClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeCircleClass extends ConverterBase<Widget>
{
    public Convert_activeCircleClass(final EdmConverter converter, final Widget parent, final Edm_activeCircleClass r)
    {
        super(converter, parent, r);

        EllipseWidget ell = (EllipseWidget) widget;

        // No 'dash' support
        //if (r.getLineStyle().isExistInEDL()  &&
        //    r.getLineStyle().get() == EdmLineStyle.DASH)

        if (r.getAttribute("lineWidth").isExistInEDL())
            ell.propLineWidth().setValue(r.getLineWidth());
        else
            ell.propLineWidth().setValue(1);
        ell.propTransparent().setValue(! r.isFill());

        if (r.isLineAlarm())
            createAlarmColor(r.getAlarmPv(), ell.propLineColor());
        else
            convertColor(r.getLineColor(), r.getAlarmPv(), ell.propLineColor());

        if (r.isFillAlarm()  &&  r.getAlarmPv() != null)
            createAlarmColor(r.getAlarmPv(), ell.propBackgroundColor());
        else
            convertColor(r.getFillColor(), r.getAlarmPv(), ell.propBackgroundColor());

        ell.propVisible().setValue(!r.isInvisible());

        widget = Convert_activeRectangleClass.convertShapeToLED(widget, r, r.isFillAlarm(), r.getFillColor(), r.getAlarmPv());

        if (widget instanceof BaseLEDWidget)
            ((BaseLEDWidget)widget).propSquare().setValue(false);
    }

    @Override
    protected EllipseWidget createWidget(final EdmWidget edm)
    {
        return new EllipseWidget();
    }
}
