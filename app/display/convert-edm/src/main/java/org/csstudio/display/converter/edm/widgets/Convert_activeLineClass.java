/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.Points;
import org.csstudio.display.builder.model.widgets.PolygonWidget;
import org.csstudio.display.builder.model.widgets.PolylineWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeLineClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeLineClass extends ConverterBase<Widget>
{
    public Convert_activeLineClass(final EdmConverter converter, final Widget parent, final Edm_activeLineClass r)
    {
        super(converter, parent, r);

        final Points points = new Points();
        final int dx = converter.getOffsetX() + widget.propX().getValue();
        final int dy = converter.getOffsetY() + widget.propY().getValue();
        final int[] x = r.getXPoints().get();
        final int[] y = r.getYPoints().get();
        final int N = Math.min(x.length,  y.length);
        for (int i=0; i<N; ++i)
            points.add(x[i]-dx, y[i]-dy);
        if (r.isClosePolygon()  &&  !r.isFill())
            points.add(x[0], y[0]);
        widget.setPropertyValue(CommonWidgetProperties.propPoints, points);

        if (widget instanceof PolylineWidget)
        {
            final PolylineWidget line = (PolylineWidget) widget;
            convertColor(r.getLineColor(), r.getAlarmPv(), line.propLineColor());

            line.propLineWidth().setValue(r.getLineWidth());
        }
        else
        {
            final PolygonWidget gon = (PolygonWidget) widget;
            convertColor(r.getLineColor(), gon.propLineColor());
            convertColor(r.getFillColor(), r.getAlarmPv(), gon.propBackgroundColor());
            gon.propLineWidth().setValue(r.getLineWidth());
        }

        // TODO See Opi_activeLineClass for alarm rules
    }

    @Override
    protected Widget createWidget(final EdmWidget edm)
    {
        if (((Edm_activeLineClass) edm).isFill())
            return new PolygonWidget();
        else
            return new PolylineWidget();
    }
}
