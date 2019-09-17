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
import org.csstudio.display.builder.model.widgets.EllipseWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeArcClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeArcClass extends ConverterBase<Widget>
{
    // ArcWidget uses outside line for the arc,
    // EDM only used the line for the outline, not the 'wedge' toward the center.
    // If arc is close to a full circle, use EllipseWidget
    public Convert_activeArcClass(final EdmConverter converter, final Widget parent, final Edm_activeArcClass r)
    {
        super(converter, parent, r);

        // EDM applies linewidth inside and outside of widget
        int linewidth;
        if (r.getLineWidth() != 0)
            linewidth = r.getLineWidth();
        else
            linewidth = 1;

        widget.propX().setValue(r.getX() - converter.getOffsetX() - linewidth/2);
        widget.propY().setValue(r.getY() - converter.getOffsetY() - linewidth/2);
        widget.propWidth().setValue(r.getW()+linewidth);
        widget.propHeight().setValue(r.getH()+linewidth);

        if (widget instanceof ArcWidget)
        {
            final ArcWidget w = (ArcWidget) widget;
            w.propLineWidth().setValue(linewidth);
            convertColor(r.getLineColor(), r.getAlarmPv(), w.propLineColor());
            convertColor(r.getFillColor(), r.getAlarmPv(), w.propBackgroundColor());
            w.propTransparent().setValue(! r.isFill());

            w.propArcStart().setValue(r.getStartAngle());
            if (r.getAttribute("totalAngle").isExistInEDL())
                w.propArcSize().setValue(r.getTotalAngle());
            else
                w.propArcSize().setValue(180.0);
        }
        else
        {
            final EllipseWidget w = (EllipseWidget) widget;
            w.propLineWidth().setValue(linewidth);
            convertColor(r.getLineColor(), r.getAlarmPv(), w.propLineColor());
            convertColor(r.getFillColor(), r.getAlarmPv(), w.propBackgroundColor());
            w.propTransparent().setValue(! r.isFill());
        }

        // TODO See Opi_activeArcClass for alarm rules
    }

    @Override
    protected Widget createWidget(final EdmWidget edm)
    {
        final Edm_activeArcClass r = (Edm_activeArcClass) edm;
        if (r.getAttribute("totalAngle").isExistInEDL() && r.getTotalAngle() > 355)
            return new EllipseWidget();
        return new ArcWidget();
    }
}
