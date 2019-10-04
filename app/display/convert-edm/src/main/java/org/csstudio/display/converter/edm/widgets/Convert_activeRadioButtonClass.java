/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.RadioWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeRadioButtonClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeRadioButtonClass extends ConverterBase<RadioWidget>
{
    public Convert_activeRadioButtonClass(final EdmConverter converter, final Widget parent, final Edm_activeRadioButtonClass r)
    {
        super(converter, parent, r);

        if(r.getAttribute("controlPv").isExistInEDL())
            widget.propPVName().setValue(convertPVName(r.getControlPv()));

        convertColor(r.getFgColor(), widget.propForegroundColor());
        convertFont(r.getFont(), widget.propHeight().getValue()-2, widget.propFont());

        // If widget is higher than the font height, assume items should be vertical
        widget.propHorizontal().setValue(widget.propHeight().getValue() < 1.5*widget.propFont().getValue().getSize());
    }

    @Override
    protected RadioWidget createWidget(final EdmWidget edm)
    {
        return new RadioWidget();
    }
}
