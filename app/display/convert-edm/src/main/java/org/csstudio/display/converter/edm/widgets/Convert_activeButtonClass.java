/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.BoolButtonWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeButtonClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeButtonClass extends ConverterBase<BoolButtonWidget>
{
    public Convert_activeButtonClass(final EdmConverter converter, final Widget parent, final Edm_activeButtonClass t)
    {
        super(converter, parent, t);

        if (t.getControlPv() != null)
            widget.propPVName().setValue(convertPVName(t.getControlPv()));
        if (t.getAttribute("controlBitsPos").isExistInEDL())
            widget.propBit().setValue(t.getControlBitsPos());

        // EDM widget has no LED
        widget.propShowLED().setValue(false);
        convertColor(t.getBgColor(), widget.propBackgroundColor());
        convertColor(t.getFgColor(), widget.propForegroundColor());
        convertColor(t.getOnColor(), widget.propOnColor());
        convertColor(t.getOffColor(), widget.propOffColor());
        convertFont(t.getFont(), widget.propFont());

        // Alarm sensitive border instead of FG color (usually off)
        widget.propBorderAlarmSensitive().setValue(t.isFgAlarm());

        widget.propLabelsFromPV().setValue(t.useLabelsFromPV());
        String on = t.getOnLabel();
        if (on == null)
            on = "";
        String off = t.getOffLabel();
        if (off == null)
            off = on;
        widget.propOnLabel().setValue(on);
        widget.propOffLabel().setValue(off);
    }

    @Override
    protected BoolButtonWidget createWidget(final EdmWidget edm)
    {
        return new BoolButtonWidget();
    }
}
