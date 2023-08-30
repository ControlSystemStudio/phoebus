/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import static org.csstudio.display.converter.edm.Converter.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.CommonWidgetProperties;
import org.csstudio.display.builder.model.properties.Points;
import org.csstudio.display.builder.model.widgets.PolygonWidget;
import org.csstudio.display.builder.model.widgets.PolylineWidget;
import org.csstudio.display.builder.model.widgets.PolylineWidget.Arrows;
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
    // EDM has one 'line' widget with options 'close polygon' and 'fill'.
    // Display Builder has Polyline (just lines, may use arrow) and Polygon (filled).
    //
    // In either tool, the 'filled' area is always 'closed',
    // but in EDM the 'line' drawn on top of the area can remain 'open'.
    //
    // In rare cases where an EDM 'open' line on top of a 'filled' area is needed,
    // maybe including 'arrows', the EDM screen needs to be adapted by
    // placing a non-filled line for the Polyline on top of the filled Polygon.
    public Convert_activeLineClass(final EdmConverter converter, final Widget parent, final Edm_activeLineClass r)
    {
        super(converter, parent, r);
        final Points points = new Points();
        final int dx = converter.getOffsetX() + widget.propX().getValue(),
                  dy = converter.getOffsetY() + widget.propY().getValue();
        final int[] x = r.getXPoints().get(), y = r.getYPoints().get();
        final int N = Math.min(x.length,  y.length);
        if (N <= 0)
            logger.log(Level.WARNING, "EDM line without points");
        else
        {
            for (int i=0; i<N; ++i)
                points.add(x[i]-dx, y[i]-dy);
            if (r.isClosePolygon()  &&  !r.isFill())
                points.add(points.getX(0), points.getY(0));
            widget.setPropertyValue(CommonWidgetProperties.propPoints, points);
        }

        // EDM line width '0' is still creating a line of width 1
        widget.setPropertyValue(CommonWidgetProperties.propLineWidth, Math.max(1, r.getLineWidth()));

        if (widget instanceof PolygonWidget)
        {
            final PolygonWidget pg = (PolygonWidget) widget;
            if (r.isFillAlarm())
                createAlarmColor(r.getAlarmPv(), pg.propBackgroundColor());
            else
                convertColor(r.getFillColor(), r.getAlarmPv(), pg.propBackgroundColor());
        }
        else
        {
            final PolylineWidget pl = (PolylineWidget) widget;
            if ("from".equals(r.getArrows()))
                pl.propArrows().setValue(Arrows.FROM);
            else if ("to".equals(r.getArrows()))
                pl.propArrows().setValue(Arrows.TO);
            else if ("both".equals(r.getArrows()))
                pl.propArrows().setValue(Arrows.BOTH);
            pl.propArrowLength().setValue(15);
        }

        if (r.isLineAlarm())
            createAlarmColor(r.getAlarmPv(), widget.getProperty(CommonWidgetProperties.propLineColor));
        else
            convertColor(r.getLineColor(), r.getAlarmPv(), widget.getProperty(CommonWidgetProperties.propLineColor));
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
