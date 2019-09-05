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
import org.csstudio.opibuilder.converter.model.Edm_activeRectangleClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Xihui Chen et al - Original logic in Opi_.. converter
 */
public class Convert_activeRectangleClass extends ConverterBase<RectangleWidget>
{
    public Convert_activeRectangleClass(final EdmConverter converter, final Widget parent, final Edm_activeRectangleClass r)
    {
        super(converter, parent, r);

        convertColor(r.getLineColor(), widget.propLineColor());
        widget.propLineWidth().setValue(r.getLineWidth());

        // No 'dash' support
        //if (r.getLineStyle().isExistInEDL()  &&
        //    r.getLineStyle().get() == EdmLineStyle.DASH)

        widget.propTransparent().setValue(! r.isFill());
        convertColor(r.getFillColor(), widget.propBackgroundColor());

        widget.propVisible().setValue(!r.isInvisible());


        // TODO See Opi_activeRectangleClass for alarm rules
    }

    @Override
    protected RectangleWidget createWidget()
    {
        return new RectangleWidget();
    }
}
