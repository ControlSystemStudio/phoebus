/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.RectangleWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeRectangleClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeRectangleClass extends ConverterBase<RectangleWidget>
{
    public Convert_activeRectangleClass(final EdmConverter converter, final Widget parent, final Edm_activeRectangleClass r)
    {
        super(converter, parent, r);

        // '0' means smallest possible line == 1
        final int linewidth = Math.max(1,  r.getLineWidth());

        // EDM applies linewidth inside and outside of widget
        widget.propLineWidth().setValue(linewidth);
        widget.propX().setValue(r.getX() - converter.getOffsetX() - linewidth/2);
        widget.propY().setValue(r.getY() - converter.getOffsetY() - linewidth/2);
        widget.propWidth().setValue(r.getW()+linewidth);
        widget.propHeight().setValue(r.getH()+linewidth);

        // No 'dash' support
        //if (r.getLineStyle().isExistInEDL()  &&
        //    r.getLineStyle().get() == EdmLineStyle.DASH)

        widget.propTransparent().setValue(! r.isFill());

        widget.propVisible().setValue(!r.isInvisible());

        if (r.isLineAlarm() && r.getAlarmPv() != null)
            createAlarmColor(r.getAlarmPv(), widget.propLineColor());
        else
            convertColor(r.getLineColor(), r.getAlarmPv(), widget.propLineColor());
        if (r.isFillAlarm() && r.getAlarmPv() != null)
            createAlarmColor(r.getAlarmPv(), widget.propBackgroundColor());
        else
            convertColor(r.getFillColor(), r.getAlarmPv(), widget.propBackgroundColor());
    }

    @Override
    protected RectangleWidget createWidget(final EdmWidget edm)
    {
        return new RectangleWidget();
    }
}
