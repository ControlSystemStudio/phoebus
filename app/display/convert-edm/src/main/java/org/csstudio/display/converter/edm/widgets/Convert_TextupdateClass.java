/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.converter.edm.widgets;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.properties.HorizontalAlignment;
import org.csstudio.display.builder.model.properties.VerticalAlignment;
import org.csstudio.display.builder.model.widgets.TextUpdateWidget;
import org.csstudio.display.converter.edm.EdmConverter;
import org.csstudio.opibuilder.converter.model.EdmWidget;
import org.csstudio.opibuilder.converter.model.Edm_TextupdateClass;
import org.phoebus.ui.vtype.FormatOption;

/** Convert an EDM widget into Display Builder counterpart
 *  @author Kay Kasemir
 *  @author Matevz, Lei Hu, Xihui Chen et al - Original logic in Opi_.. converter
 */
@SuppressWarnings("nls")
public class Convert_TextupdateClass extends ConverterBase<TextUpdateWidget>
{
    public Convert_TextupdateClass(final EdmConverter converter, final Widget parent, final Edm_TextupdateClass r)
    {
        super(converter, parent, r);

        // Display Builder widget has no border.
        // Alarm border is shown around the widget proper.
        final int lw = r.getLineWidth();
        if (lw > 0)
        {
            widget.propX().setValue(widget.propX().getValue()+lw);
            widget.propY().setValue(widget.propY().getValue()+lw);
            widget.propWidth().setValue(widget.propWidth().getValue()-2*lw);
            widget.propHeight().setValue(widget.propHeight().getValue()-2*lw);
        }

        convertColor(r.getBgColor(), widget.propBackgroundColor());
        convertColor(r.getFgColor(), widget.propForegroundColor());
        convertFont(r.getFont(), widget.propHeight().getValue()-2, widget.propFont());
        widget.propTransparent().setValue(!r.isFill());

        widget.propPVName().setValue(convertPVName(r.getControlPv()));

        if ("right".equals(r.getFontAlign()))
            widget.propHorizontalAlignment().setValue(HorizontalAlignment.RIGHT);
        else if ("center".equals(r.getFontAlign()))
            widget.propHorizontalAlignment().setValue(HorizontalAlignment.CENTER);
        widget.propVerticalAlignment().setValue(VerticalAlignment.MIDDLE);

        if (r.getDisplayMode() != null)
        {
            // Non-default EDM mode disables units and uses fixed precision
            widget.propPrecision().setValue(r.getPrecision());
            widget.propShowUnits().setValue(false);
            if (r.getDisplayMode().equals("decimal"))
                widget.propFormat().setValue(FormatOption.DECIMAL);
            else if (r.getDisplayMode().equals("hex"))
                widget.propFormat().setValue(FormatOption.HEX);
            else if (r.getDisplayMode().equals("engineer"))
                widget.propFormat().setValue(FormatOption.ENGINEERING);
            else if (r.getDisplayMode().equals("exp"))
                widget.propFormat().setValue(FormatOption.EXPONENTIAL);
        }

    }

    @Override
    protected TextUpdateWidget createWidget(final EdmWidget edm)
    {
        return new TextUpdateWidget();
    }
}
