/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.widgets.ProgressBarWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_activeBarClass;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_activeBarClass extends ConverterBase<ProgressBarWidget>
{
    public Convert_activeBarClass(final EdmConverter converter, final Widget parent, final Edm_activeBarClass r)
    {
        super(converter, parent, r);

        convertColor(r.getIndicatorColor(), widget.propFillColor());
        convertColor(r.getBgColor(), widget.propBackgroundColor());
        widget.propHorizontal().setValue(!"vertical".equals(r.getOrientation()));
        widget.propPVName().setValue(convertPVName(r.getIndicatorPv()));
        widget.propLimitsFromPV().setValue(r.isLimitsFromDb());
    }

    @Override
    protected ProgressBarWidget createWidget(final EdmWidget edm)
    {
        return new ProgressBarWidget();
    }
}
