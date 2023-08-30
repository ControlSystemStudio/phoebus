/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.ByteMonitorWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_ByteClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_ByteClass extends ConverterBase<ByteMonitorWidget>
{
    public Convert_ByteClass(final EdmConverter converter, final Widget parent, final Edm_ByteClass r)
    {
        super(converter, parent, r);

        widget.propSquare().setValue(true);
        widget.propHorizontal().setValue(r.getW() > r.getH());
        convertColor(r.getLineColor(), widget.propForegroundColor());

        if (r.getOnColor().isDynamic())
        {   // Dynamic 'on' color provides both the 'off' and 'on' color, using values 0, 1
            widget.propOffColor().setValue(evaluateDynamicColor(r.getOnColor(), 0));
            widget.propOnColor().setValue(evaluateDynamicColor(r.getOnColor(), 1));
        }
        else
        {   // Static colors
            convertColor(r.getOffColor(), widget.propOffColor());
            convertColor(r.getOnColor(), widget.propOnColor());
        }
        widget.propPVName().setValue(convertPVName(r.getControlPv()));
        widget.propBitReverse().setValue("little".equals(r.getEndian()));
        widget.propNumBits().setValue(r.getNumBits()==0?16:r.getNumBits());
        widget.propStartBit().setValue(r.getShift());
    }

    @Override
    protected ByteMonitorWidget createWidget(final EdmWidget edm)
    {
        return new ByteMonitorWidget();
    }
}
