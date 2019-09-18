/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.ComboWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeMenuButtonClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
public class Convert_activeMenuButtonClass extends ConverterBase<ComboWidget>
{
    public Convert_activeMenuButtonClass(final EdmConverter converter, final Widget parent, final Edm_activeMenuButtonClass t)
    {
        super(converter, parent, t);

        if (t.getControlPv() != null)
            widget.propPVName().setValue(convertPVName(t.getControlPv()));

        convertColor(t.getBgColor(), widget.propBackgroundColor());
        convertColor(t.getFgColor(), widget.propForegroundColor());
        convertFont(t.getFont(), widget.propFont());

        // Alarm sensitive border instead of FG color (usually off)
        widget.propBorderAlarmSensitive().setValue(t.isFgAlarm());

        widget.propItemsFromPV().setValue(true);
        widget.propEditable().setValue(false);
    }

    @Override
    protected ComboWidget createWidget(final EdmWidget edm)
    {
        return new ComboWidget();
    }
}
