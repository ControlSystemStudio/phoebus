/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.ArcWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeArcClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeArcClass extends ConverterBase<ArcWidget>
{
    public Convert_activeArcClass(final EdmConverter converter, final Widget parent, final Edm_activeArcClass r)
    {
        super(converter, parent, r);

        // EDM applies linewidth inside and outside of widget
        int linewidth;
        if (r.getLineWidth() != 0)
            linewidth = r.getLineWidth();
        else
            linewidth = 1;

        widget.propLineWidth().setValue(linewidth);
        widget.propX().setValue(r.getX() - converter.getOffsetX() - linewidth/2);
        widget.propY().setValue(r.getY() - converter.getOffsetY() - linewidth/2);
        widget.propWidth().setValue(r.getW()+linewidth);
        widget.propHeight().setValue(r.getH()+linewidth);

        convertColor(r.getLineColor(), r.getAlarmPv(), widget.propLineColor());
        convertColor(r.getFillColor(), r.getAlarmPv(), widget.propBackgroundColor());
        widget.propTransparent().setValue(! r.isFill());

        widget.propArcStart().setValue(r.getStartAngle());
        if (r.getAttribute("totalAngle").isExistInEDL())
            widget.propArcSize().setValue(r.getTotalAngle());
        else
            widget.propArcSize().setValue(180.0);

        // TODO See Opi_activeArcClass for alarm rules
    }

    @Override
    protected ArcWidget createWidget(final EdmWidget edm)
    {
        return new ArcWidget();
    }
}
