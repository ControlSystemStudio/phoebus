/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.ChoiceButtonWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeChoiceButtonClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeChoiceButtonClass extends ConverterBase<ChoiceButtonWidget>
{
    public Convert_activeChoiceButtonClass(final EdmConverter converter, final Widget parent, final Edm_activeChoiceButtonClass r)
    {
        super(converter, parent, r);

        widget.propHorizontal().setValue("horizontal".equals(r.getOrientation()));
        convertColor(r.getBgColor(), widget.propBackgroundColor());
        convertColor(r.getFgColor(), widget.propForegroundColor());
        convertColor(r.getSelectColor(), widget.propSelectedColor());
        convertFont(r.getFont(), widget.propFont());

        if (r.getAttribute("controlPv").isExistInEDL())
            widget.propPVName().setValue(convertPVName(r.getControlPv()));
    }

    @Override
    protected ChoiceButtonWidget createWidget(final EdmWidget edm)
    {
        return new ChoiceButtonWidget();
    }
}
