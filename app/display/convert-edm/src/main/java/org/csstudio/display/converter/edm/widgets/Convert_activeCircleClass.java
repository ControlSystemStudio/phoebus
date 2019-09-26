/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.EllipseWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeCircleClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeCircleClass extends ConverterBase<EllipseWidget>
{
    public Convert_activeCircleClass(final EdmConverter converter, final Widget parent, final Edm_activeCircleClass r)
    {
        super(converter, parent, r);

        // No 'dash' support
        //if (r.getLineStyle().isExistInEDL()  &&
        //    r.getLineStyle().get() == EdmLineStyle.DASH)

        if (r.getAttribute("lineWidth").isExistInEDL())
            widget.propLineWidth().setValue(r.getLineWidth());
        else
            widget.propLineWidth().setValue(1);
        widget.propTransparent().setValue(! r.isFill());

        if (r.isLineAlarm())
            createAlarmColor(r.getAlarmPv(), widget.propLineColor());
        else
            convertColor(r.getLineColor(), r.getAlarmPv(), widget.propLineColor());

        if (r.isFillAlarm()  &&  r.getAlarmPv() != null)
            createAlarmColor(r.getAlarmPv(), widget.propBackgroundColor());
        else
            convertColor(r.getFillColor(), r.getAlarmPv(), widget.propBackgroundColor());

        widget.propVisible().setValue(!r.isInvisible());
    }

    @Override
    protected EllipseWidget createWidget(final EdmWidget edm)
    {
        return new EllipseWidget();
    }
}
